/*******************************************************************************
 * Copyright (c) 2014-2018 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core.ui;

import java.util.EnumSet;
import java.util.List;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICMultiConfigDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import de.marw.cdt.cmake.core.internal.Activator;
import de.marw.cdt.cmake.core.internal.CmakeGenerator;
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
    extends QuirklessAbstractCPropertyTab {

  /**  */
  private static final ILog log = Activator.getDefault().getLog();

  /**
   * the preferences associated with our configuration to manage. Initialized in
   * {@link #configSelectionChanged}
   */
  private P prefs;

  /** 'use exec from path' checkbox */
  private Button b_cmdFromPath;
  /** cmake executable */
  private Text t_cmd;
  /** browse files for cmake executable */
  private Button b_cmdBrowseFiles;
  /** variables in cmake executable text field */
  private Button b_cmdVariables;
  /** Combo that shows the generator names for cmake */
  private ComboViewer c_generator;

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
   * Gets all sensible choices for cmake's generator option on this platform.
   *
   * @return a non-empty set, never {@code null}.
   */
  protected abstract EnumSet<CmakeGenerator> getAvailableGenerators();

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


    // cmake executable group...
    {
      GridLayout layout;
      final int horizontalSpan = 3;
      Group gr = WidgetHelper.createGroup(usercomp, SWT.FILL, 2,
          "CMake Executable", 2);

      b_cmdFromPath = WidgetHelper.createCheckbox(gr, SWT.BEGINNING, 2,
          "Use cmake executable found on &system path");

      setupLabel(gr, "&File", 1, SWT.BEGINNING);

      t_cmd = setupText(gr, 1, GridData.FILL_HORIZONTAL);

      // "Filesystem", "Variables" dialog launcher buttons...
      Composite buttonBar = new Composite(gr, SWT.NONE);
      {
        buttonBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false,
            horizontalSpan, 1));
        layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        buttonBar.setLayout(layout);
      }
      b_cmdBrowseFiles = WidgetHelper.createButton(buttonBar, "F&ile System...",
          true);
      b_cmdBrowseFiles.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          IDialogSettings settings = Activator.getDefault().getDialogSettings();
          FileDialog dialog = new FileDialog(t_cmd.getShell());
          dialog.setFilterPath(settings.get("cmake_dir"));
          String text = dialog.open();
          settings.put("cmake_dir", dialog.getFilterPath());
          if (text != null) {
            t_cmd.setText(text);
          }
        }
      });

      b_cmdVariables = WidgetHelper.createButton(buttonBar, "Insert &Variable...",
          true);
      b_cmdVariables.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          final ICResourceDescription resDesc = getResDesc();
          if (resDesc == null)
            return;
          ICConfigurationDescription cfgd= resDesc.getConfiguration();
        String text = AbstractCPropertyTab.getVariableDialog(t_cmd.getShell(), cfgd);
          if (text != null) {
            t_cmd.insert(text);
          }
        }
      });
      // to adjust sensitivity...
      b_cmdFromPath.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
          final Button btn = (Button) event.widget;
          handleCommandEnabled(!btn.getSelection());
        }
      });

      createEnvironmentScriptControls(gr, horizontalSpan);
    } // cmake executable group

    // makefile generator combo...
    {
      setupLabel(usercomp, "Buildscript &generator (-G):", 1, SWT.BEGINNING);
      c_generator = new ComboViewer(usercomp);
      final GridData gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false,
          1, 1);
      gd.widthHint = 200;
      c_generator.getCombo().setLayoutData(gd);
      c_generator.setContentProvider(ArrayContentProvider.getInstance());
      c_generator.setLabelProvider(new LabelProvider() {
        @Override
        public String getText(Object element) {
          if (element instanceof CmakeGenerator) {
            return ((CmakeGenerator) element).getCmakeName();
          }
          return super.getText(element);
        }

      });
      final EnumSet<CmakeGenerator> generators = getAvailableGenerators();
      c_generator.setInput(generators);
      if (generators.size() == 1)
        c_generator.getCombo().setEnabled(false);
    } // makefile generator combo

    // cmake defines table...
    final ICResourceDescription resDesc = getResDesc();
    definesViewer = new DefinesViewer(usercomp, resDesc == null ? null : resDesc.getConfiguration());
    // cmake undefines table...
    undefinesViewer = new UnDefinesViewer(usercomp);
  }

  /**
   * Creates the controls to specify an extra script to run in the same shell prior to cmake. The default implementation
   * does nothing.
   *
   * @param parent
   *          the parent control
   * @param horizontalSpan
   *          the max. number of column cells that the control may take up
   */
  protected void createEnvironmentScriptControls(Group parent, int horizontalSpan) {

  }

  @Override
  protected void configSelectionChanged(ICResourceDescription lastConfig, ICResourceDescription newConfig) {
    if (lastConfig != null) {
      saveToModel(prefs);
    }
    if (newConfig == null)
      return;

    if (page.isMultiCfg()) {
      // we are editing multiple configurations...
      setAllVisible(false, null);
      return;
    } else {
      setAllVisible(true, null);

      ICConfigurationDescription cfgd = newConfig.getConfiguration();
      final ConfigurationManager configMgr = ConfigurationManager.getInstance();
      try {
        CMakePreferences allPrefs = configMgr.getOrLoad(cfgd);
        prefs = getOsPreferences(allPrefs);
      } catch (CoreException ex) {
        log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, null, ex));
      }

      updateDisplay(prefs);
    }
  }

  /**
   * Updates displayed values according to the preferences edited by this tab.
   *
   * @param prefs
   *          the preferences associated with our configuration to manage.
   *
   */
  protected void updateDisplay(P prefs) {
    t_cmd.setText(prefs.getCommand());
    b_cmdFromPath.setSelection(prefs.getUseDefaultCommand());
    // adjust sensitivity...
    handleCommandEnabled(!prefs.getUseDefaultCommand());

    CmakeGenerator generator = prefs.getGenerator();
    c_generator.setSelection(new StructuredSelection(generator));

    definesViewer.setInput(prefs.getDefines());
    undefinesViewer.setInput(prefs.getUndefines());
  }

  /**
   * Stores displayed values to the preferences edited by this tab.
   *
   * @param prefs
   *          the preferences associated with our configuration to manage or <code>null</code>.
   *
   * @see #updateDisplay
   */
  protected void saveToModel(P prefs) {
    if (prefs == null)
      return;
    prefs.setUseDefaultCommand(b_cmdFromPath.getSelection());
    String command = t_cmd.getText().trim();
    prefs.setCommand(command);

    final IStructuredSelection sel = (IStructuredSelection) c_generator
        .getSelection();
    prefs.setGenerator((CmakeGenerator) sel.getFirstElement());
    // NB: defines & undefines are modified by the widget listeners directly
  }

  /**
   * Changes sensitivity of controls to enter the cmake command. Necessary since
   * Button.setSelection does not fire events.
   *
   * @param enabled
   *        the new enabled state
   */
  private void handleCommandEnabled(boolean enabled) {
    t_cmd.setEnabled(enabled);
    b_cmdBrowseFiles.setEnabled(enabled);
    b_cmdVariables.setEnabled(enabled);
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
    // make sure the displayed values get applied
    saveToModel(prefs);

    ICConfigurationDescription srcCfg = src.getConfiguration();
    ICConfigurationDescription dstCfg = dst.getConfiguration();
    final ConfigurationManager configMgr = ConfigurationManager.getInstance();

    try {
      P srcPrefs = getOsPreferences(configMgr.getOrLoad(srcCfg));
      P dstPrefs = getOsPreferences(configMgr.getOrCreate(dstCfg));
      if (srcPrefs != dstPrefs) {
        dstPrefs.setUseDefaultCommand(srcPrefs.getUseDefaultCommand());
        dstPrefs.setCommand(srcPrefs.getCommand());
        dstPrefs.setGenerator(srcPrefs.getGenerator());

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
      }
    } catch (CoreException ex) {
      log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, null, ex));
    }
  }

  @Override
  protected void performOK() {
    final ICResourceDescription resDesc = getResDesc();
    if (resDesc == null)
      return;

    ICConfigurationDescription cfgd= resDesc.getConfiguration();
    if(cfgd instanceof ICMultiConfigDescription){
      // this tab does not support editing of multiple configurations
      return;
    }
    saveToModel(prefs);
    try {
      // save as project settings..
      ICStorageElement storage = cfgd.getStorage(
          CMakePreferences.CFG_STORAGE_ID, true);
      prefs.saveToStorage(storage);
    } catch (CoreException ex) {
      log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, null, ex));
    }
  }

  @Override
  protected void performDefaults() {
    prefs.reset();
    updateDisplay(prefs);
  }

  @Override
  protected void updateButtons() {
    // never called from superclass, but abstract :-)
  }
}
