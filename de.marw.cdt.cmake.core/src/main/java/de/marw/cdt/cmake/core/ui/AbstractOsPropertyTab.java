/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core.ui;

import java.util.List;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import de.marw.cdt.cmake.core.CMakePlugin;
import de.marw.cdt.cmake.core.internal.settings.AbstractOsPreferences;
import de.marw.cdt.cmake.core.internal.settings.CMakePreferences;
import de.marw.cdt.cmake.core.internal.settings.CmakeDefine;
import de.marw.cdt.cmake.core.internal.settings.CmakeUnDefine;
import de.marw.cdt.cmake.core.internal.settings.ConfigurationManager;

/**
 * Generic UI to control host OS specific project properties and preferences for
 * {@code cmake}. Host OS specific properties override generic properties when
 * passed to {@code cmake} and get automatically applied if this plugin detects
 * it is running under that operating system.<br>
 * This tab and any subclass is responsible for storing its values.<br>
 *
 * @author Martin Weber
 * @param <P>
 *        the type that holds the OS specific properties.
 */
public abstract class AbstractOsPropertyTab<P extends AbstractOsPreferences>
    extends AbstractCPropertyTab {

  /**  */
  private static final ILog log = CMakePlugin.getDefault().getLog();

  /** the configuration we manage here. Initialized in {@link #updateData} */
  private ICConfigurationDescription cfgd;
  /**
   * the preferences associated with our configuration to manage. Initialized in
   * {@link #updateData}
   */
  private P prefs;

  /** 'use exec from path' checkbox */
  private Button b_cmdFromPath;
  /** cmake executable */
  private Text t_cmd;
  /** Combo that shows the generator names for cmake */
  private Combo c_generator;
  /** the table showing the cmake defines */
  private DefinesViewer definesViewer;
  /** the table showing the cmake undefines */
  private UnDefinesViewer undefinesViewer;

  /**
   */
  public AbstractOsPropertyTab() {
  }

  /**
   * Gets the OS specific preferences from the specified generic preferences.
   *
   * @return the OS specific preferences, never {@code null}.
   */
  protected abstract P getOsPreferences(CMakePreferences prefs);

  /**
   * Gets all sensible choices for cmake's '-G' option on this platform. The
   * returned array should not include generators for IDE project files, such as
   * "Eclipse CDT4 - Unix Makefiles".
   *
   * @return an array of non-empty strings, where each string must be an valid
   *         argument for cmake.
   */
  protected abstract String[] getAvailableGenerators();

  @Override
  public boolean canBeVisible() {
    return page.isForProject();
  }

  public boolean canSupportMultiCfg() {
    return false;
  }

  @Override
  protected void createControls(Composite parent) {
    super.createControls(parent);
    usercomp.setLayout(new GridLayout(2, false));
//    usercomp.setBackground(BACKGROUND_FOR_USER_VAR);

    GridLayout layout;

    // cmake executable group...
    {
      Group gr = WidgetHelper.createGroup(usercomp, SWT.FILL, 2,
          "Cmake Executable", 2);

      b_cmdFromPath = WidgetHelper.createCheckbox(gr, SWT.BEGINNING, 2,
          "Use cmake executable found on &system path");

      setupLabel(gr, "&File", 1, SWT.BEGINNING);

      t_cmd = setupText(gr, 1, GridData.FILL_HORIZONTAL);

      // "Filesystem", "Variables" dialog launcher buttons...
      Composite buttonBar = new Composite(gr, SWT.NONE);
      {
        buttonBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false,
            3, 1));
        layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        buttonBar.setLayout(layout);
      }
      final Button btnBrowseFiles = WidgetHelper.createButton(buttonBar,
          "File System...", true);
      btnBrowseFiles.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          IDialogSettings settings = CMakePlugin.getDefault()
              .getDialogSettings();
          FileDialog dialog = new FileDialog(t_cmd.getShell());
          dialog.setFilterPath(settings.get("cmake_dir"));
          String text = dialog.open();
          settings.put("cmake_dir", dialog.getFilterPath());
          if (text != null) {
            t_cmd.insert(text);
          }
        }
      });

      // control sensitivity...
      b_cmdFromPath.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
          final Button btn = (Button) event.widget;
          // adjust sensitivity...
          boolean val = !btn.getSelection();
          t_cmd.setEnabled(val);
          btnBrowseFiles.setEnabled(val);
        }
      });

    } // cmake executable group

    // makefile generator combo...
    {
      setupLabel(usercomp, "Buildscript &generator (-G):", 1, SWT.BEGINNING);
      c_generator = new Combo(usercomp, SWT.READ_ONLY | SWT.DROP_DOWN
          | SWT.BORDER);
      final GridData gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false,
          1, 1);
      gd.widthHint = 200;
      c_generator.setLayoutData(gd);

      String generatorNames[] = getAvailableGenerators();
      c_generator.setItems(generatorNames);
    } // makefile generator combo

    // cmake defines table...
    definesViewer = new DefinesViewer(usercomp);
    // cmake undefines table...
    undefinesViewer = new UnDefinesViewer(usercomp);
  }

  /**
   * Invoked when this tab is going to display. Call that 'initialize'!
   */
  @Override
  protected void updateData(ICResourceDescription resd) {
    if (resd == null)
      return;

    cfgd = resd.getConfiguration();
    final ConfigurationManager configMgr = ConfigurationManager.getInstance();
    try {
      CMakePreferences allPrefs = configMgr.getOrLoad(cfgd);
      prefs = getOsPreferences(allPrefs);
    } catch (CoreException ex) {
      log.log(new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID, null, ex));
    }

    updateDisplay();
  }

  /**
   * Updates displayed values according to the preferences edited by this tab.
   */
  private void updateDisplay() {
    t_cmd.setText(prefs.getCommand());
    b_cmdFromPath.setSelection(prefs.getUseDefaultCommand());

    String generatorName = prefs.getGeneratorName();
    int idx = c_generator.indexOf(generatorName);
    if (idx >= 0)
      c_generator.select(idx);

    definesViewer.setInput(prefs.getDefines());
    undefinesViewer.setInput(prefs.getUndefines());
  }

  /**
   * Invoked when project configuration changes?? At least when apply button is
   * pressed.
   *
   * @see org.eclipse.cdt.ui.newui.AbstractCPropertyTab#performApply(org.eclipse.cdt.core.settings.model.ICResourceDescription,
   *      org.eclipse.cdt.core.settings.model.ICResourceDescription)
   */
  @Override
  protected void performApply(ICResourceDescription src,
      ICResourceDescription dst) {
    ICConfigurationDescription srcCfg = src.getConfiguration();
    ICConfigurationDescription dstCfg = dst.getConfiguration();
    final ConfigurationManager configMgr = ConfigurationManager.getInstance();

    try {
      P srcPrefs = getOsPreferences(configMgr.getOrLoad(srcCfg));
      P dstPrefs = getOsPreferences(configMgr.getOrCreate(dstCfg));

      dstPrefs.setUseDefaultCommand(srcPrefs.getUseDefaultCommand());
      dstPrefs.setCommand(srcPrefs.getCommand());
      dstPrefs.setGeneratorName(srcPrefs.getGeneratorName());

      final List<CmakeDefine> defines = dstPrefs.getDefines();
      defines.clear();
      for (CmakeDefine def : srcPrefs.getDefines()) {
        defines.add(def.clone());
      }

      final List<CmakeUnDefine> undefines = dstPrefs.getUndefines();
      undefines.clear();
      for (CmakeUnDefine undef : srcPrefs.getUndefines()) {
        undefines.add(undef.clone());
      }
    } catch (CoreException ex) {
      log.log(new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID, null, ex));
    }
  }

  @Override
  protected void performOK() {
    if (cfgd == null)
      return; // YES, the CDT framework invokes us even if it did not call updateData()!!!
    try {
      prefs.setUseDefaultCommand(b_cmdFromPath.getSelection());
      String command = t_cmd.getText().trim();
      prefs.setCommand(command);

      int idx = c_generator.getSelectionIndex();
      if (idx >= 0) {
        String gen = c_generator.getItem(idx);
        prefs.setGeneratorName(gen);
      }
      // NB: defines & undefines are modified by the widget listeners directly

      // save as project settings..
      ICStorageElement storage = cfgd.getStorage(
          CMakePreferences.CFG_STORAGE_ID, true);
      prefs.saveToStorage(storage);
    } catch (CoreException ex) {
      log.log(new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID, null, ex));
    }
  }

  @Override
  protected void performDefaults() {
    if (cfgd == null)
      return; // YES, the CDT framework invokes us even if it did not call updateData()!!!
    prefs.reset();
    updateDisplay();
  }

  @Override
  protected void updateButtons() {
    // never called from superclass, but abstract :-)
  }
}
