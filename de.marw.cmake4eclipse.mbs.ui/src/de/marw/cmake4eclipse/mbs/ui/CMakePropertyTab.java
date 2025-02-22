/*******************************************************************************
 * Copyright (c) 2013-2023 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.ui;

import java.util.Objects;
import java.util.function.Function;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICMultiConfigDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.cdt.ui.newui.AbstractPage;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;
import org.eclipse.ui.dialogs.NewFolderDialog;

import de.marw.cmake4eclipse.mbs.settings.CMakeSettings;
import de.marw.cmake4eclipse.mbs.settings.ConfigurationManager;
import de.marw.cmake4eclipse.mbs.ui.slim.BelowRootPathTextFieldModifyListener;

/**
 * UI to control general project properties for cmake. This tab is responsible
 * for storing its values.
 *
 * @author Martin Weber
 */
public class CMakePropertyTab extends QuirklessAbstractCPropertyTab {
  private static final String CONFIGURATIONS_DIFFER = " <configurations differ> ";

  private static final ILog log = Activator.getDefault().getLog();

  // Widgets
  /** pre-populate cache from file */
  private Text t_cacheFile;
  /** browse files for cache file */
  private Button b_browseCacheFile;
  /** build directory */
  private Text t_outputFolder;
  private Text t_otherArguments;
  private Button b_browseOutputFolder;
  private Button b_createOutputFolder;
  /** variables in output folder text field */
  private Button b_cmdVariablesOutput;
  private Button b_cmdVariablesOther;

  /**
   * the preferences associated with our configurations to manage. Initialized
   * in {@link #updateData}. {@code null} if this tab has never been displayed so a user could
   * have made edits.
   */
  private CMakeSettings[] prefs;

  @Override
  protected void createControls(final Composite parent) {
    super.setHelpContextId(Activator.PLUGIN_ID + ".cmake_options_tab_context");
    super.createControls(parent);
    usercomp.setLayout(new GridLayout(2, false));
//    usercomp.setBackground(BACKGROUND_FOR_USER_VAR);

    // output folder group
    {
      Group gr = WidgetHelper.createGroup(usercomp, SWT.FILL, 2, "Build output location (relative to project root)", 2);

      setupLabel(gr, "&Folder", 1, SWT.BEGINNING);
      t_outputFolder = setupText(gr, 1, GridData.FILL_HORIZONTAL);
      BelowRootPathTextFieldModifyListener.addListener(t_outputFolder, (AbstractPage) super.page,
          "Build output location must be below project root");
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

      b_cmdVariablesOutput = WidgetHelper.createButton(buttonBar, "Insert &Variable...",
          true);
      b_cmdVariablesOutput.addSelectionListener(new SelectionAdapter() {
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

    // cmake prepopulate cache group...
    {
      Group gr2 = WidgetHelper.createGroup(usercomp, SWT.FILL, 2, "Pre-load a script to populate the CMake cache entries (-C)", 2);

      setupLabel(gr2, "Fi&le", 1, SWT.BEGINNING);
      t_cacheFile = setupText(gr2, 1, GridData.FILL_HORIZONTAL);
      // "Browse..." dialog launcher buttons...
      b_browseCacheFile = WidgetHelper.createButton(gr2, "B&rowse...", true);
      b_browseCacheFile.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false, 2, 1));

      b_browseCacheFile.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          FilteredResourcesSelectionDialog dialog = new FilteredResourcesSelectionDialog(t_cacheFile.getShell(), false,
              page.getProject(), IResource.FILE);
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

    // other cmake options group
    {
      Group gr2 = WidgetHelper.createGroup(usercomp, SWT.FILL, 2, "Other CMake arguments", 1);

      t_otherArguments = new Text(gr2, SWT.MULTI | SWT.WRAP | SWT.BORDER|SWT.V_SCROLL);
      GridDataFactory.fillDefaults().grab(true, true).minSize(SWT.DEFAULT, 80).applyTo(t_otherArguments);
      t_otherArguments.setToolTipText("Specify arbitrary cmake arguments. Arguments must be separated by spaces but may "
          + "contain spaces if they are enclosed in double quotes (will be handled like a Unix shell does).");
      b_cmdVariablesOther = WidgetHelper.createButton(gr2, "Insert &Variable...", true);
      b_cmdVariablesOther.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false, 2, 1));
      b_cmdVariablesOther.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          final ICResourceDescription resDesc = getResDesc();
          if (resDesc == null)
            return;
          ICConfigurationDescription cfgd= resDesc.getConfiguration();
        String text = AbstractCPropertyTab.getVariableDialog(t_otherArguments.getShell(), cfgd);
          if (text != null) {
            t_otherArguments.insert(text);
          }
        }
      });
    } // other cmake options group
  }

  /**
   * Sets the value of the cache file entry field and whether the user can edit
   * that input field.
   *
   * @param text
   *          the text to display in the cache-file field
   */
  private void setCacheFileEditable(boolean editable, String text) {
    text= editable ? text : CONFIGURATIONS_DIFFER;
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
    text= editable ? text : CONFIGURATIONS_DIFFER;
    t_outputFolder.setText(text == null ? "" : text);
    t_outputFolder.setEditable(editable);
    t_outputFolder.setEnabled(editable);
    b_browseOutputFolder.setEnabled(editable);
    b_createOutputFolder.setEnabled(editable);
    b_cmdVariablesOutput.setEnabled(editable);
  }

  /**
   * Sets the value of the 'other options' entry field and whether the user can edit
   * that input field.
   *
   * @param text
   *          the text to display in the 'other options' field
   */
  private void setOtherOptionsEditable(boolean editable, String text) {
    text= editable ? text : CONFIGURATIONS_DIFFER;
    t_otherArguments.setText(text == null ? "" : text);
    t_otherArguments.setEditable(editable);
    t_otherArguments.setEnabled(editable);
    b_cmdVariablesOther.setEnabled(editable);
  }

  /**
   * Updates displayed values according to the preferences edited by this tab.
   */
  private void updateDisplay() {
    boolean cacheFileEditable;
    boolean buildFolderEditable;
    boolean otherArgsEditable;

    if (prefs.length > 1) {
      // we are editing multiple configurations...
      // make t_cacheFile disabled, if its settings are not the same in all configurations
      cacheFileEditable = !preferencesDiffer(CMakeSettings::getCacheFile);
      // make t_outputFolder disabled, if its settings are not the same in all configurations
      buildFolderEditable = !preferencesDiffer(CMakeSettings::getBuildDirectory);
      // make t_otherArguments disabled, if its settings are not the same in all configurations
      otherArgsEditable = !preferencesDiffer(CMakeSettings::getOtherArguments);
    } else {
      // we are editing a single configuration...
      // all buttons are in toggle mode
      cacheFileEditable = true;
      buildFolderEditable = true;
      otherArgsEditable = true;
    }

    setCacheFileEditable(cacheFileEditable, prefs[0].getCacheFile());
    setBuildFolderEditable(buildFolderEditable, prefs[0].getBuildDirectory());
    setOtherOptionsEditable(otherArgsEditable, prefs[0].getOtherArguments());
  }

  /**
   * Gets whether preferences differ when we are editing multiple configurations.
   *
   * @param getter
   *               the function to get the preference value in question from the array of preferences
   */
  private <V> boolean preferencesDiffer(Function<CMakeSettings, V> getter) {
    final V cf0 = getter.apply(prefs[0]);
    for (int i = 1; i < prefs.length; i++) {
      V cf = getter.apply(prefs[i]);
      if (!Objects.equals(cf0, cf)) {
        // configurations differ
        return true;
      }
    }
    return false;
  }

  /**
   * Stores displayed values to the preferences edited by this tab.
   *
   * @see #updateDisplay()
   */
  private void saveToModel() {
    if (prefs == null)
      return;
    if (prefs.length > 1) {
      // we are editing multiple configurations...
      for (int i = 0; i < prefs.length; i++) {
        CMakeSettings pref = prefs[i];

        if (t_cacheFile.getEditable()) {
          final String cacheFileName = t_cacheFile.getText().trim();
          pref.setCacheFile(cacheFileName.isEmpty() ? null : cacheFileName);
        }
        if (t_outputFolder.getEditable()) {
          final String dir = t_outputFolder.getText().trim();
          pref.setBuildDirectory(dir.isEmpty() ? null : dir);
        }
        if (t_otherArguments.getEditable()) {
          final String args = t_otherArguments.getText().trim();
          pref.setOtherArguments(args.isEmpty() ? null : args);
        }
      }
    } else {
      // we are editing a single configuration...
      CMakeSettings pref = prefs[0];
      String value;
      value = t_cacheFile.getText().trim();
      pref.setCacheFile(value.isEmpty() ? null : value);
      value = t_outputFolder.getText().trim();
      pref.setBuildDirectory(value);
      value = t_otherArguments.getText().trim();
      pref.setOtherArguments(value.isEmpty() ? null : value);
    }
  }

  @Override
  protected void updateData(ICResourceDescription resd) {
    if (resd == null)
      return;
    final ICConfigurationDescription cfgd= resd.getConfiguration();
    final ConfigurationManager configMgr = ConfigurationManager.getInstance();
    try {
      if (cfgd instanceof ICMultiConfigDescription) {
        // we are editing multiple configurations...
        ICConfigurationDescription[] cfgs = (ICConfigurationDescription[]) ((ICMultiConfigDescription) cfgd).getItems();

        prefs = new CMakeSettings[cfgs.length];
        for (int i = 0; i < cfgs.length; i++) {
          prefs[i] = configMgr.getOrLoad(cfgs[i]);
        }
      } else {
        // we are editing a single configuration...
        prefs = new CMakeSettings[1];
        prefs[0] = configMgr.getOrLoad(cfgd);
      }
    } catch (CoreException ex) {
      log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, null, ex));
    }
    updateDisplay();
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

    try {
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
      // save behavior as CDT has: Apply does persist settings:
      // this also solves the problem with different configuration that have been modified, apply but would
      // otherwise not be persisted on performOK()
      persist(dst);
    } catch (CoreException ex) {
      log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, null, ex));
    }
  }

  /**
   * @param srcCfg
   * @param dstCfg
   * @throws CoreException
   */
  @SuppressWarnings("deprecation")
  private static void applyConfig(ICConfigurationDescription srcCfg, ICConfigurationDescription dstCfg) throws CoreException {
    final ConfigurationManager configMgr = ConfigurationManager.getInstance();
      CMakeSettings srcPrefs = configMgr.getOrLoad(srcCfg);
      CMakeSettings dstPrefs = configMgr.getOrCreate(dstCfg);
      if (srcPrefs != dstPrefs) {
        dstPrefs.setClearCache(srcPrefs.isClearCache());
        dstPrefs.setDebugTryCompile(srcPrefs.isDebugTryCompile());
        dstPrefs.setDebugOutput(srcPrefs.isDebugOutput());
        dstPrefs.setTrace(srcPrefs.isTrace());
        dstPrefs.setWarnNoDev(srcPrefs.isWarnNoDev());
        dstPrefs.setWarnUnitialized(srcPrefs.isWarnUnitialized());
        dstPrefs.setWarnUnused(srcPrefs.isWarnUnused());
        dstPrefs.setCacheFile(srcPrefs.getCacheFile());
        dstPrefs.setBuildDirectory(srcPrefs.getBuildDirectory());
      }
  }

  @Override
  protected void performOK() {
    // make sure the displayed values get saved
    saveToModel();
    persist(getResDesc());
  }

  /**
   * @param resDesc
   */
  private void persist(final ICResourceDescription resDesc) {
    if (resDesc == null || prefs == null)
      return;

    ICConfigurationDescription cfgd = resDesc.getConfiguration();
    // save project properties..
    try {
      if (prefs.length > 1) {
        // we are editing multiple configurations...
        ICConfigurationDescription[] cfgs = (ICConfigurationDescription[]) ((ICMultiConfigDescription) cfgd).getItems();

        for (int i = 0; i < prefs.length; i++) {
          prefs[i].save(cfgs[i]);
        }
      } else {
        // we are editing a single configuration...
        prefs[0].save(cfgd);
      }
    } catch (CoreException ex) {
      log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, null, ex));
    }
  }

  /*-
   * @see org.eclipse.cdt.ui.newui.AbstractCPropertyTab#performDefaults()
   */
  @Override
  protected void performDefaults() {
    if (prefs == null)
      return;
    for (CMakeSettings pref : prefs) {
      pref.reset();
    }
    updateDisplay();
  }

  // This page can be displayed for project
  @Override
  public boolean canBeVisible() {
    return page.isForProject();
  }
}
