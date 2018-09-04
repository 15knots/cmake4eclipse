/*******************************************************************************
 * Copyright (c) 2013-2018 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core.ui;

import java.util.BitSet;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICMultiConfigDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;
import org.eclipse.ui.dialogs.NewFolderDialog;

import de.marw.cdt.cmake.core.CdtPlugin;
import de.marw.cdt.cmake.core.internal.settings.CMakePreferences;
import de.marw.cdt.cmake.core.internal.settings.ConfigurationManager;

/**
 * UI to control general project properties for cmake. This tab is responsible
 * for storing its values.
 *
 * @author Martin Weber
 */
public class CMakePropertyTab extends QuirklessAbstractCPropertyTab {

  /**  */
  private static final ILog log = CdtPlugin.getDefault().getLog();

  // Widgets
  /** Clear cmake-cache before build */
  private Button b_clearCache;
  private Button b_disableMake;
  private Button b_disableVerbose;
  private Button b_enableGenMakeTargets;
  private Button b_ignoreSingleFileTargets;
  private Button b_warnNoDev;
  private Button b_debugTryCompile;
  private Button b_debug;
  private Button b_trace;
  private Button b_warnUnitialized;
  private Button b_warnUnused;
  /** pre-populate cache from file */
  private Text t_cacheFile;
  /** browse files for cache file */
  private Button b_browseCacheFile;
  /** build directory */
  private Text t_outputFolder;
  private Button b_browseOutputFolder;
  private Button b_createOutputFolder;
  /** variables in output folder text field */
  private Button b_cmdVariables;

  /**
   * the preferences associated with our configurations to manage. Initialized
   * in {@link #updateData}
   */
  private CMakePreferences[] prefs;

  /** shared listener for check-boxes */
  private TriStateButtonListener tsl = new TriStateButtonListener();

  @Override
  protected void createControls(final Composite parent) {
    super.createControls(parent);
    usercomp.setLayout(new GridLayout(2, false));
//    usercomp.setBackground(BACKGROUND_FOR_USER_VAR);

    // output folder group
    {
      Group gr = WidgetHelper.createGroup(usercomp, SWT.FILL, 2, "Build output location (relative to project or absolute)", 2);

      setupLabel(gr, "&Folder", 1, SWT.BEGINNING);

      t_outputFolder = setupText(gr, 1, GridData.FILL_HORIZONTAL);

      // "Browse", "Create" dialog launcher buttons...
      Composite buttonBar = new Composite(gr, SWT.NONE);
      {
        buttonBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false, 3, 1));
        GridLayout layout = new GridLayout(3, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        buttonBar.setLayout(layout);
      }
      b_browseOutputFolder = WidgetHelper.createButton(buttonBar, "&Browse...", true);
      b_browseOutputFolder.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          FilteredResourcesSelectionDialog dialog = new FilteredResourcesSelectionDialog(t_cacheFile.getShell(), false,
              page.getProject(), IResource.FOLDER);
          dialog.setTitle("Select output folder");
          dialog.open();
          IFolder folder = (IFolder) dialog.getFirstResult();
          if (folder != null) {
            // insert selected resource name
            t_outputFolder.setText(folder.getProjectRelativePath().toPortableString());
          }
        }
      });
      b_createOutputFolder = WidgetHelper.createButton(buttonBar, "&Create...", true);
      b_createOutputFolder.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          NewFolderDialog dialog = new NewFolderDialog(parent.getShell(), page.getProject());
          if (dialog.open() == Window.OK) {
            IFolder f = (IFolder) dialog.getFirstResult();
            t_outputFolder.setText(f.getProjectRelativePath().toPortableString());
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
        String text = AbstractCPropertyTab.getVariableDialog(t_outputFolder.getShell(), cfgd);
          if (text != null) {
            t_outputFolder.insert(text);
          }
        }
      });
    }
    // Build behavior group...
    {
      Group gr = WidgetHelper.createGroup(usercomp, SWT.FILL, 2, "Build Behavior", 2);

      b_clearCache = WidgetHelper.createCheckbox(gr, SWT.BEGINNING, 2, "Forc&e cmake to run with each build");
      b_clearCache.setToolTipText("Useful if you are configuring a new project");
      b_clearCache.addListener(SWT.Selection, tsl);
      
      b_disableMake = WidgetHelper.createCheckbox(gr, SWT.BEGINNING, 2, "Disa&ble execution of make after cmake");
      b_disableMake.setToolTipText("Useful for configuring large projects with many small targets");
      b_disableMake.addListener(SWT.Selection, tsl);

      b_disableVerbose = WidgetHelper.createCheckbox(gr, SWT.BEGINNING, 2, "Disa&ble generation of verbose makefiles (!Warning! build output parsers may not work)");
      b_disableVerbose.setToolTipText("Useful for getting a better overview of the log output");
      b_disableVerbose.addListener(SWT.Selection, tsl);
      
      b_enableGenMakeTargets = WidgetHelper.createCheckbox(gr, SWT.BEGINNING, 2, "Gene&rate Make targets from cmake file");
      b_enableGenMakeTargets.setToolTipText("Generates targets to be dipslayed in the 'Build Target' UI");
      b_enableGenMakeTargets.addListener(SWT.Selection, tsl);
      
      // probably wrong, just copy pasted from WidgetHelper.createGroup
      Composite gr2 = new Composite(parent, SWT.NONE);
      GridLayout gl2 = new GridLayout(1, false);
      gl2.marginLeft = 20;
      gl2.marginTop = 0;
      gl2.marginBottom = 0;
      gr2.setLayout(gl2);
      GridData gd2 = new GridData(SWT.FILL, SWT.CENTER, true, false);
      gd2.horizontalSpan = 1;
      gr2.setLayoutData(gd2);
      gr2.setParent(gr);

      b_ignoreSingleFileTargets = WidgetHelper.createCheckbox(gr2, SWT.BEGINNING, 2, "Igno&re single file targets");
      b_ignoreSingleFileTargets.setToolTipText("Don't create targets that compile a single file");
      b_ignoreSingleFileTargets.addListener(SWT.Selection, tsl);
      
      b_enableGenMakeTargets.addListener(SWT.Selection, e -> {
        b_ignoreSingleFileTargets.setEnabled(b_enableGenMakeTargets.getSelection());
      });
    }

    // cmake options group...
    {
      Group gr = WidgetHelper.createGroup(usercomp, SWT.FILL, 2, "CMake commandline options", 2);

      b_warnNoDev = WidgetHelper.createCheckbox(gr, SWT.BEGINNING, 2, "Suppress developer &warnings \t(-Wno-dev)");
      b_warnNoDev.addListener(SWT.Selection, tsl);
      b_debugTryCompile = WidgetHelper.createCheckbox(gr, SWT.BEGINNING, 2,
          "Do not delete the tr&y_compile build tree (--debug-trycompile)");
      b_debugTryCompile.addListener(SWT.Selection, tsl);
      b_debug = WidgetHelper.createCheckbox(gr, SWT.BEGINNING, 2, "Put cmake in &debug mode \t\t(--debug-output)");
      b_debug.addListener(SWT.Selection, tsl);
      b_trace = WidgetHelper.createCheckbox(gr, SWT.BEGINNING, 2, "Put cmake in &trace mode \t\t(--trace)");
      b_trace.addListener(SWT.Selection, tsl);
      b_warnUnitialized = WidgetHelper.createCheckbox(gr, SWT.BEGINNING, 2,
          "Warn about un&initialized values \t(--warn-uninitialized)");
      b_warnUnitialized.addListener(SWT.Selection, tsl);
      b_warnUnused = WidgetHelper.createCheckbox(gr, SWT.BEGINNING, 2,
          "Warn about un&used variables \t(--warn-unused-vars)");
      b_warnUnused.addListener(SWT.Selection, tsl);

      // cmake prepopulate cache group...
      {
        Group gr2 = WidgetHelper.createGroup(gr, SWT.FILL, 2, "Pre-populate CMake cache entries from file (-C)", 2);

        setupLabel(gr2, "Fi&le", 1, SWT.BEGINNING);

        t_cacheFile = setupText(gr2, 1, GridData.FILL_HORIZONTAL);
        // "Browse..." dialog launcher buttons...
        b_browseCacheFile = WidgetHelper.createButton(gr2, "B&rowse...", true);
        b_browseCacheFile.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false, 2, 1));

        b_browseCacheFile.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            FilteredResourcesSelectionDialog dialog = new FilteredResourcesSelectionDialog(t_cacheFile.getShell(),
                false, page.getProject(), IResource.FILE);
            dialog.setTitle("Select file");
            dialog.setInitialPattern("c*.txt", FilteredItemsSelectionDialog.FULL_SELECTION);
            dialog.open();
            IFile file = (IFile) dialog.getFirstResult();
            if (file != null) {
              // insert selected resource name
              t_cacheFile.insert(file.getProjectRelativePath().toPortableString());
            }
          }
        });

      } // cmake prepopulate cache group
    } // cmake options group

  }

  @Override
  protected void configSelectionChanged(ICResourceDescription lastConfig, ICResourceDescription newConfig) {
    if (lastConfig != null) {
      saveToModel();
    }
    if (newConfig == null)
      return;

    ICConfigurationDescription cfgd = newConfig.getConfiguration();
    final ConfigurationManager configMgr = ConfigurationManager.getInstance();
    try {
      if (cfgd instanceof ICMultiConfigDescription) {
        // we are editing multiple configurations...
        ICConfigurationDescription[] cfgs = (ICConfigurationDescription[]) ((ICMultiConfigDescription) cfgd).getItems();

        prefs = new CMakePreferences[cfgs.length];
        for (int i = 0; i < cfgs.length; i++) {
          prefs[i] = configMgr.getOrLoad(cfgs[i]);
        }
      } else {
        // we are editing a single configuration...
        prefs = new CMakePreferences[1];
        prefs[0] = configMgr.getOrLoad(cfgd);
      }
    } catch (CoreException ex) {
      log.log(new Status(IStatus.ERROR, CdtPlugin.PLUGIN_ID, null, ex));
    }
    updateDisplay();
  }

  /**
   * Sets the value of the cache file entry field and whether the user can edit
   * that input field.
   *
   * @param text
   *          the text to display in the cache-file field
   */
  private void setCacheFileEditable(boolean editable, String text) {
    text= editable ? text : " <configurations differ> ";
    t_cacheFile.setText(text == null ? "" : text);
    t_cacheFile.setEditable(editable);
    t_cacheFile.setEnabled(editable);
    b_browseCacheFile.setEnabled(editable);
  }

  /**
   * Sets the value of the build folder entry field and whether the user can edit
   * that input field.
   *
   * @param text
   *          the text to display in the cache-file field
   */
  private void setBuildFolderEditable(boolean editable, String text) {
    text= editable ? text : " <configurations differ> ";
    t_outputFolder.setText(text == null ? "" : text);
    t_outputFolder.setEditable(editable);
    t_outputFolder.setEnabled(editable);
    b_browseOutputFolder.setEnabled(editable);
    b_createOutputFolder.setEnabled(editable);
    b_cmdVariables.setEnabled(editable);
  }

  /**
   * Updates displayed values according to the preferences edited by this tab.
   */
  private void updateDisplay() {
    boolean cacheFileEditable = true;
    boolean buildFolderEditable = true;

    if (prefs.length > 1) {
      // we are editing multiple configurations...
      /*
       * make each button tri-state, if its settings are not the same in all
       * configurations
       */
      BitSet bs = new BitSet(prefs.length);
      // b_warnNoDev...
      for (int i = 0; i < prefs.length; i++) {
        bs.set(i, prefs[i].isClearCache());
      }
      enterTristateOrToggleMode(b_clearCache, bs, prefs.length);
      
      // b_disableMake...
      for (int i = 0; i < prefs.length; i++) {
        bs.set(i, prefs[i].isMakeDisabled());
      }
      enterTristateOrToggleMode(b_disableMake, bs, prefs.length);
      
      // b_disableVerbose...
      for (int i = 0; i < prefs.length; i++) {
        bs.set(i, prefs[i].isVerboseDisabled());
      }
      enterTristateOrToggleMode(b_disableVerbose, bs, prefs.length);
      
      // b_enableGenMakeTargets...
      for (int i = 0; i < prefs.length; i++) {
        bs.set(i, prefs[i].shouldGenerateMakeTargets());
      }
      enterTristateOrToggleMode(b_enableGenMakeTargets, bs, prefs.length);

      // b_ignoreSingleFileTargets...
      for (int i = 0; i < prefs.length; i++) {
        bs.set(i, prefs[i].shouldIgnoreSingleFileTargets());
      }
      enterTristateOrToggleMode(b_ignoreSingleFileTargets, bs, prefs.length);
      
      // b_warnNoDev...
      bs.clear();
      for (int i = 0; i < prefs.length; i++) {
        bs.set(i, prefs[i].isWarnNoDev());
      }
      enterTristateOrToggleMode(b_warnNoDev, bs, prefs.length);

      // b_debugTryCompile
      bs.clear();
      for (int i = 0; i < prefs.length; i++) {
        bs.set(i, prefs[i].isDebugTryCompile());
      }
      enterTristateOrToggleMode(b_debugTryCompile, bs, prefs.length);

      // b_debug...
      bs.clear();
      for (int i = 0; i < prefs.length; i++) {
        bs.set(i, prefs[i].isDebugOutput());
      }
      enterTristateOrToggleMode(b_debug, bs, prefs.length);
      // b_trace...
      bs.clear();
      for (int i = 0; i < prefs.length; i++) {
        bs.set(i, prefs[i].isTrace());
      }
      enterTristateOrToggleMode(b_trace, bs, prefs.length);

      // b_warnUnitialized...
      bs.clear();
      for (int i = 0; i < prefs.length; i++) {
        bs.set(i, prefs[i].isWarnUnitialized());
      }
      enterTristateOrToggleMode(b_warnUnitialized, bs, prefs.length);

      // b_warnUnused...
      bs.clear();
      for (int i = 0; i < prefs.length; i++) {
        bs.set(i, prefs[i].isWarnUnused());
      }
      enterTristateOrToggleMode(b_warnUnused, bs, prefs.length);

      // t_cacheFile
      /*
       * make t_cacheFile disabled, if its settings are not the same in all
       * configurations
       */
      {
      final String cf0 = prefs[0].getCacheFile();
      for (int i = 1; i < prefs.length; i++) {
        String cf = prefs[i].getCacheFile();
        if (cf0 != null) {
          if (!cf0.equals(cf)) {
            // configurations differ
            cacheFileEditable = false;
            break;
          }
        } else if (cf != null) {
          // configurations differ
          cacheFileEditable = false;
          break;
        }
      }}
      /*
       * make t_outputFolder disabled, if its settings are not the same in all
       * configurations
       */
      {
      final String cf0 = prefs[0].getBuildDirectory();
      for (int i = 1; i < prefs.length; i++) {
        String cf = prefs[i].getBuildDirectory();
        if (cf0 != null) {
          if (!cf0.equals(cf)) {
            // configurations differ
            buildFolderEditable = false;
            break;
          }
        } else if (cf != null) {
          // configurations differ
          buildFolderEditable = false;
          break;
        }
      }}
    } else {
      // we are editing a single configuration...
      // all buttons are in toggle mode
      CMakePreferences pref = prefs[0];
      enterToggleMode(b_clearCache, pref.isClearCache());
      enterToggleMode(b_disableMake, pref.isMakeDisabled());
      enterToggleMode(b_disableVerbose, pref.isVerboseDisabled());
      enterToggleMode(b_enableGenMakeTargets, pref.shouldGenerateMakeTargets());
      b_ignoreSingleFileTargets.setEnabled(pref.shouldGenerateMakeTargets());
      enterToggleMode(b_ignoreSingleFileTargets, pref.shouldIgnoreSingleFileTargets());
      enterToggleMode(b_warnNoDev, pref.isWarnNoDev());
      enterToggleMode(b_debug, pref.isDebugOutput());
      enterToggleMode(b_trace, pref.isTrace());
      enterToggleMode(b_warnUnitialized, pref.isWarnUnitialized());
      enterToggleMode(b_warnUnused, pref.isWarnUnused());
    }

    setCacheFileEditable(cacheFileEditable, prefs[0].getCacheFile());
    String text = prefs[0].getBuildDirectory();
    setBuildFolderEditable(buildFolderEditable, text == null ? "build/${ConfigName}" : text);
  }

  /**
   * Stores displayed values to the preferences edited by this tab.
   *
   * @see #updateDisplay()
   */
  private void saveToModel() {
    if (prefs.length > 1) {
      // we are editing multiple configurations...
      for (int i = 0; i < prefs.length; i++) {
        CMakePreferences pref = prefs[i];

        if (shouldSaveButtonSelection(b_clearCache))
          pref.setClearCache(b_clearCache.getSelection());
        if (shouldSaveButtonSelection(b_disableMake))
          pref.setMakeDisabled(b_disableMake.getSelection());
        if (shouldSaveButtonSelection(b_disableVerbose))
          pref.setVerboseDisabled(b_disableVerbose.getSelection());
        if (shouldSaveButtonSelection(b_enableGenMakeTargets))
          pref.setGenerateMakeTargets(b_enableGenMakeTargets.getSelection());
        if (shouldSaveButtonSelection(b_ignoreSingleFileTargets))
          pref.setIgnoreSingleFileTargets(b_ignoreSingleFileTargets.getSelection());
        if (shouldSaveButtonSelection(b_warnNoDev))
          pref.setWarnNoDev(b_warnNoDev.getSelection());
        if (shouldSaveButtonSelection(b_debugTryCompile))
          pref.setDebugTryCompile(b_debugTryCompile.getSelection());
        if (shouldSaveButtonSelection(b_debug))
          pref.setDebugOutput(b_debug.getSelection());
        if (shouldSaveButtonSelection(b_trace))
          pref.setTrace(b_trace.getSelection());
        if (shouldSaveButtonSelection(b_warnUnitialized))
          pref.setWarnUnitialized(b_warnUnitialized.getSelection());
        if (shouldSaveButtonSelection(b_warnUnused))
          pref.setWarnUnused(b_warnUnused.getSelection());
        if (t_cacheFile.getEditable()) {
          final String cacheFileName = t_cacheFile.getText();
          pref.setCacheFile(cacheFileName.trim().isEmpty() ? null : cacheFileName);
        }
        if (t_outputFolder.getEditable()) {
          final String dir = t_outputFolder.getText();
          pref.setBuildDirectory(dir.trim().isEmpty() ? null : dir);
        }
      }
    } else {
      // we are editing a single configuration...
      CMakePreferences pref = prefs[0];
      pref.setClearCache(b_clearCache.getSelection());
      pref.setMakeDisabled(b_disableMake.getSelection());
      pref.setVerboseDisabled(b_disableVerbose.getSelection());
      pref.setGenerateMakeTargets(b_enableGenMakeTargets.getSelection());
      pref.setIgnoreSingleFileTargets(b_ignoreSingleFileTargets.getSelection());
      pref.setWarnNoDev(b_warnNoDev.getSelection());
      pref.setDebugTryCompile(b_debugTryCompile.getSelection());
      pref.setDebugOutput(b_debug.getSelection());
      pref.setTrace(b_trace.getSelection());
      pref.setWarnUnitialized(b_warnUnitialized.getSelection());
      pref.setWarnUnused(b_warnUnused.getSelection());
      final String cacheFileName = t_cacheFile.getText().trim();
      pref.setCacheFile(cacheFileName.isEmpty() ? null : cacheFileName);
      final String dir = t_outputFolder.getText().trim();
      pref.setBuildDirectory(dir.isEmpty() ? null : dir);
    }
  }

  /**
   * Switches the specified button behavior from tri-state mode to toggle mode.
   *
   * @param button
   *          the button to modify
   * @param buttonSelected
   *          the selection of the button
   */
  private static void enterToggleMode(Button button, boolean buttonSelected) {
    button.setData(null); // mark toggle mode
    button.setSelection(buttonSelected);
    button.setGrayed(false);
  }

  /**
   * Switches the specified button behavior from toggle mode to tri-state mode.
   *
   * @param button
   *          the button to modify
   */
  private static void enterTristateOrToggleMode(Button button, BitSet bs, int numBits) {
    if (needsTri(bs, numBits)) {
      enterTristateMode(button);
    } else {
      enterToggleMode(button, !bs.isEmpty());
    }
  }

  /**
   * Switches the specified button behavior to toggle mode or to tri-state mode.
   *
   * @param button
   *          the button to modify
   */
  private static void enterTristateMode(Button button) {
    button.setData(Boolean.TRUE); // mark in tri-state mode
    button.setSelection(true);
    button.setGrayed(true);
  }

  /**
   * Gets whether all bits in the bit set have the same state.
   */
  private static boolean needsTri(BitSet bs, int numBits) {
    final int card = bs.cardinality();
    return !(card == numBits || card == 0);
  }

  /**
   * Invoked when project configuration changes??
   *
   * @see org.eclipse.cdt.ui.newui.AbstractCPropertyTab#performApply(org.eclipse.cdt.core.settings.model.ICResourceDescription,
   *      org.eclipse.cdt.core.settings.model.ICResourceDescription)
   */
  @Override
  protected void performApply(ICResourceDescription src, ICResourceDescription dst) {
    // make sure the displayed values get applied
    // AFAICS, src is always == getResDesc(). so saveToModel() effectively
    // stores to src
    saveToModel();

    ICConfigurationDescription srcCfg = src.getConfiguration();
    ICConfigurationDescription dstCfg = dst.getConfiguration();

    if (srcCfg instanceof ICMultiConfigDescription) {
      ICConfigurationDescription[] srcCfgs = (ICConfigurationDescription[]) ((ICMultiConfigDescription) srcCfg)
          .getItems();
      ICConfigurationDescription[] dstCfgs = (ICConfigurationDescription[]) ((ICMultiConfigDescription) dstCfg)
          .getItems();
      for (int i = 0; i < srcCfgs.length; i++) {
        applyConfig(srcCfgs[i], dstCfgs[i]);
      }
    } else {
      applyConfig(srcCfg, dstCfg);
    }
  }

  /**
   * @param srcCfg
   * @param dstCfg
   */
  private static void applyConfig(ICConfigurationDescription srcCfg, ICConfigurationDescription dstCfg) {
    final ConfigurationManager configMgr = ConfigurationManager.getInstance();
    try {
      CMakePreferences srcPrefs = configMgr.getOrLoad(srcCfg);
      CMakePreferences dstPrefs = configMgr.getOrCreate(dstCfg);
      if (srcPrefs != dstPrefs) {
        dstPrefs.setClearCache(srcPrefs.isClearCache());
        dstPrefs.setDebugTryCompile(srcPrefs.isDebugTryCompile());
        dstPrefs.setDebugOutput(srcPrefs.isDebugOutput());
        dstPrefs.setTrace(srcPrefs.isTrace());
        dstPrefs.setWarnNoDev(srcPrefs.isWarnNoDev());
        dstPrefs.setWarnUnitialized(srcPrefs.isWarnUnitialized());
        dstPrefs.setWarnUnused(srcPrefs.isWarnUnused());
        dstPrefs.setCacheFile(srcPrefs.getCacheFile());
      }
    } catch (CoreException ex) {
      log.log(new Status(IStatus.ERROR, CdtPlugin.PLUGIN_ID, null, ex));
    }
  }

  protected void performOK() {
    final ICResourceDescription resDesc = getResDesc();
    if (resDesc == null)
      return;

    saveToModel();
    ICConfigurationDescription cfgd = resDesc.getConfiguration();
    // save project properties..
    try {
      if (prefs.length > 1) {
        // we are editing multiple configurations...
        ICConfigurationDescription[] cfgs = (ICConfigurationDescription[]) ((ICMultiConfigDescription) cfgd).getItems();

        for (int i = 0; i < prefs.length; i++) {
          ICStorageElement storage = cfgs[i].getStorage(CMakePreferences.CFG_STORAGE_ID, true);
          prefs[i].saveToStorage(storage);
        }
      } else {
        // we are editing a single configuration...
        ICStorageElement storage = cfgd.getStorage(CMakePreferences.CFG_STORAGE_ID, true);
        prefs[0].saveToStorage(storage);
      }
    } catch (CoreException ex) {
      log.log(new Status(IStatus.ERROR, CdtPlugin.PLUGIN_ID, null, ex));
    }
  }

  /**
   * Gets whether the value of the specified button should be saved.
   *
   * @param button
   *          the button to query
   */
  private static boolean shouldSaveButtonSelection(Button button) {
    if (button != null && Boolean.TRUE.equals(button.getData()) && button.getGrayed()) {
      // if button is in tri-state mode and grayed, do not save
      return false;
    }
    return true;
  }

  /*-
   * @see org.eclipse.cdt.ui.newui.AbstractCPropertyTab#performDefaults()
   */
  @Override
  protected void performDefaults() {
    for (CMakePreferences pref : prefs) {
      pref.reset();
    }
    updateDisplay();
  }

  // This page can be displayed for project
  @Override
  public boolean canBeVisible() {
    return page.isForProject();
  }

  @Override
  protected void updateButtons() {
    // never called from superclass, but abstract :-)
  }

  ////////////////////////////////////////////////////////////////////
  // inner classes
  ////////////////////////////////////////////////////////////////////
  /**
   * Adds tri-state behavior to a button when added as a SWT.Selection listener.
   *
   * @author Martin Weber
   */
  private static class TriStateButtonListener implements Listener {

    /*-
     * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
     */
    @Override
    public void handleEvent(Event event) {
      final Button btn = (Button) event.widget;
      if (btn != null && Boolean.TRUE.equals(btn.getData())) {
        // button is in tri-state mode
        if (btn.getSelection()) {
          if (!btn.getGrayed()) {
            btn.setGrayed(true);
          }
        } else {
          if (btn.getGrayed()) {
            btn.setGrayed(false);
            btn.setSelection(true);
          }
        }
      }
    }
  } // TriStateButtonListener
}
