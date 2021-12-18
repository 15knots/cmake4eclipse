/*******************************************************************************
 * Copyright (c) 2021 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.ui.preferences;

import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.google.gson.JsonSyntaxException;

import de.marw.cmake4eclipse.mbs.preferences.PreferenceAccess;
import de.marw.cmake4eclipse.mbs.settings.CmakeDefine;
import de.marw.cmake4eclipse.mbs.settings.CmakeGenerator;
import de.marw.cmake4eclipse.mbs.ui.Activator;
import de.marw.cmake4eclipse.mbs.ui.DefinesViewer;
import de.marw.cmake4eclipse.mbs.ui.WidgetHelper;

/**
 * Preference page for Cmake4eclipse workbench preferences (cmake invocation preferences).
 */
public class CMakePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  // Widgets
  /** Clear cmake-cache before build */
  private Button b_clearCache;
  private Button b_warnNoDev;
  private Button b_debugTryCompile;
  private Button b_debug;
  private Button b_trace;
  private Button b_warnUnitialized;
  private Button b_warnUnused;
  private Button b_verboseBuild;
  private Button[] persistedButtons;
  /** Combo that shows the generator names for cmake */
  private ComboViewer c_generator;

  /** the table showing the cmake defines */
  private DefinesViewer cacheEntriesViewer;

  public CMakePreferencePage() {
    setDescription("Specify how and when CMake is invoked.");
  }

  /**
   * Overwritten to get the preferences of plugin "de.marw.cmake4eclipse.mbs"
   */
  @Override
  protected IPreferenceStore doGetPreferenceStore() {
    return new ScopedPreferenceStore(InstanceScope.INSTANCE, PreferenceAccess.getPreferences().name());
  }

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  @Override
  public void init(IWorkbench workbench) {
  }

  private void initFromPrefstore() {
    persistedButtons = new Button[] { b_clearCache, b_warnNoDev, b_debugTryCompile, b_debug, b_trace, b_warnUnitialized,
        b_warnUnused, b_verboseBuild };

    IPreferenceStore store = getPreferenceStore();

    String key;
    for (Button btn : persistedButtons) {
      key = (String) btn.getData();
      btn.setSelection(store.getBoolean(key));
    }
    key = (String) c_generator.getControl().getData();
    CmakeGenerator generator = CmakeGenerator.valueOf(store.getString(key));
    c_generator.setSelection(new StructuredSelection(generator));

    key = (String) cacheEntriesViewer.getTableViewer().getTable().getData();
    String json = store.getString(key);
    try {
      List<CmakeDefine> entries = PreferenceAccess.toListFromJson(CmakeDefine.class, json);
      cacheEntriesViewer.setInput(entries);
    } catch (JsonSyntaxException ex) {
      // file format error
      Activator.getDefault().getLog().error("Error loading workbench preferences", ex);
    }
  }

  @Override
  protected void performDefaults() {
    IPreferenceStore store = getPreferenceStore();
    for (Button btn : persistedButtons) {
      String key = (String) btn.getData();
      btn.setSelection(store.getDefaultBoolean(key));
    }
//    cacheEntriesViewer.getInput().clear();
    {
      String key = (String) c_generator.getControl().getData();
      CmakeGenerator generator = CmakeGenerator.valueOf(store.getDefaultString(key));
      c_generator.setSelection(new StructuredSelection(generator));
    }
    super.performDefaults();
  }

  @Override
  public boolean performOk() {
    IPreferenceStore store = getPreferenceStore();
    boolean dirty = false;
    for (Button btn : persistedButtons) {
      String key = (String) btn.getData();
      boolean newVal = btn.getSelection();
      if (newVal != store.getBoolean(key)) {
        dirty = true;
        store.setValue(key, newVal);
      }
    }
    {
      String key = (String) c_generator.getControl().getData();
      String oldVal = store.getString(key);
      IStructuredSelection selection = (IStructuredSelection) c_generator.getSelection();
      CmakeGenerator newVal = (CmakeGenerator) selection.getFirstElement();
      if (!Objects.equals(newVal.name(), oldVal)) {
        dirty = true;
        store.setValue(key, newVal.name());
      }
    }
    {
      String key = (String) cacheEntriesViewer.getTableViewer().getTable().getData();
      String oldVal = store.getString(key);
      List<CmakeDefine> cacheEntries = cacheEntriesViewer.getInput();
      String newVal = PreferenceAccess.toJsonFromList(cacheEntries);
      if (!Objects.equals(newVal, oldVal)) {
        dirty = true;
        store.setValue(key, newVal);
      }
    }

    if (dirty) {
      store.setValue(PreferenceAccess.DIRTY_TS, System.currentTimeMillis());
    }

    return true;
  }

  @Override
  protected Control createContents(Composite parent) {
    TabFolder tabFolder = new TabFolder(parent, SWT.TOP);
    createGeneralTab(tabFolder);
    createCacheVariablesTab(tabFolder);
    initFromPrefstore();
    return tabFolder;
  }

  private void createGeneralTab(TabFolder folder) {
    TabItem tab = new TabItem(folder, SWT.NONE);
    tab.setText("General");
    Composite composite = new Composite(folder, SWT.NONE);
    composite.setLayout(new GridLayout(2, false));

    // buildscript generator...
    {
      {
        Label l = new Label(composite, SWT.NONE);
        l.setText("Default &build system (-G):");
        GridData gd = new GridData(SWT.BEGINNING);
        l.setLayoutData(gd);
      }
      c_generator = new ComboViewer(composite);
      c_generator.getControl().setData(PreferenceAccess.CMAKE_GENERATOR);
      c_generator.getCombo().setToolTipText("The default build system to generate scripts for.\n"
          + "May be overwritten by a Build Tool Kit, if any is marked to overwrite.");
      final GridData gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1);
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
      c_generator.setInput(AbstractBuildToolkitDialog.getAvailableGenerators());
    } // makefile generator combo

    // Build behavior group...
    {
      Group gr = WidgetHelper.createGroup(composite, SWT.FILL, 2, "Build-System Files", 2);
      b_clearCache = WidgetHelper.createCheckbox(gr, SWT.BEGINNING, 2, "&Force re-creation with each build");
      b_clearCache.setData(PreferenceAccess.CMAKE_FORCE_RUN);
    }

    // cmake options group...
    {
      Group gr = WidgetHelper.createGroup(composite, SWT.FILL, 2, "CMake commandline options", 2);

      b_verboseBuild = WidgetHelper.createCheckbox(gr, SWT.BEGINNING, 2,
          "Enable &verbose output from Makefile builds \t(-DCMAKE_VERBOSE_MAKEFILE)");
      b_verboseBuild.setData(PreferenceAccess.VERBOSE_BUILD);
      b_warnNoDev = WidgetHelper.createCheckbox(gr, SWT.BEGINNING, 2, "Suppress developer &warnings \t(-Wno-dev)");
      b_warnNoDev.setData(PreferenceAccess.CMAKE_WARN_NO_DEV);
      b_debugTryCompile = WidgetHelper.createCheckbox(gr, SWT.BEGINNING, 2,
          "Do not delete the tr&y_compile build tree (--debug-trycompile)");
      b_debugTryCompile.setData(PreferenceAccess.CMAKE_DBG_TRY_COMPILE);
      b_debug = WidgetHelper.createCheckbox(gr, SWT.BEGINNING, 2, "Put cmake in &debug mode \t\t(--debug-output)");
      b_debug.setData(PreferenceAccess.CMAKE_DBG);
      b_trace = WidgetHelper.createCheckbox(gr, SWT.BEGINNING, 2, "Put cmake in &trace mode \t\t(--trace)");
      b_trace.setData(PreferenceAccess.CMAKE_TRACE);
      b_warnUnitialized = WidgetHelper.createCheckbox(gr, SWT.BEGINNING, 2,
          "Warn about un&initialized values \t(--warn-uninitialized)");
      b_warnUnitialized.setData(PreferenceAccess.CMAKE_WARN_UNINITIALIZED);
      b_warnUnused = WidgetHelper.createCheckbox(gr, SWT.BEGINNING, 2,
          "Warn about un&used variables \t(--warn-unused-vars)");
      b_warnUnused.setData(PreferenceAccess.CMAKE_WARN_UNUSED);
    } // cmake options group

    tab.setControl(composite);
  }

  private void createCacheVariablesTab(TabFolder folder) {
    TabItem tab = new TabItem(folder, SWT.NONE);
    tab.setText("CMake cache entries");
    Composite composite = new Composite(folder, SWT.NONE);
    composite.setLayout(new GridLayout(1, false));
    // cmake defines table...
    cacheEntriesViewer = new DefinesViewer(composite, null);
    cacheEntriesViewer.getTableViewer().getTable().setData(PreferenceAccess.CMAKE_CACHE_ENTRIES);
    tab.setControl(composite);
  }
}