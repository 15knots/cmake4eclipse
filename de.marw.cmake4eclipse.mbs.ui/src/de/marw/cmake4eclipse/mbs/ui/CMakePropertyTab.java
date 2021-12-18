/*******************************************************************************
 * Copyright (c) 2013-2019 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.ui;

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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;
import org.eclipse.ui.dialogs.NewFolderDialog;

import de.marw.cmake4eclipse.mbs.settings.CMakePreferences;
import de.marw.cmake4eclipse.mbs.settings.ConfigurationManager;

/**
 * UI to control general project properties for cmake. This tab is responsible
 * for storing its values.
 *
 * @author Martin Weber
 */
public class CMakePropertyTab extends QuirklessAbstractCPropertyTab {
  private static final ILog log = Activator.getDefault().getLog();

  // Widgets
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
   * in {@link #updateData}. {@code null} if this tab has never been displayed so a user could
   * have made edits.
   */
  private CMakePreferences[] prefs;

  @Override
  protected void createControls(final Composite parent) {
    super.createControls(parent);
    usercomp.setLayout(new GridLayout(2, false));
//    usercomp.setBackground(BACKGROUND_FOR_USER_VAR);

    // output folder group
    {
      Group gr = WidgetHelper.createGroup(usercomp, SWT.FILL, 2, "Build output location (relative to project root)", 2);

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
    if (prefs == null)
      return;
    if (prefs.length > 1) {
      // we are editing multiple configurations...
      for (int i = 0; i < prefs.length; i++) {
        CMakePreferences pref = prefs[i];

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
      final String cacheFileName = t_cacheFile.getText().trim();
      pref.setCacheFile(cacheFileName.isEmpty() ? null : cacheFileName);
      final String dir = t_outputFolder.getText().trim();
      pref.setBuildDirectory(dir.isEmpty() ? null : dir);
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
          ICStorageElement storage = cfgs[i].getStorage(CMakePreferences.CFG_STORAGE_ID, true);
          prefs[i].saveToStorage(storage);
        }
      } else {
        // we are editing a single configuration...
        ICStorageElement storage = cfgd.getStorage(CMakePreferences.CFG_STORAGE_ID, true);
        prefs[0].saveToStorage(storage);
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
}
