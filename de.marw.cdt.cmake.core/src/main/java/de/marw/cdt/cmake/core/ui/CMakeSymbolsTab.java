/*******************************************************************************
 * Copyright (c) 2014-2017 Martin Weber.
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
import org.eclipse.cdt.core.settings.model.ICMultiConfigDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import de.marw.cdt.cmake.core.CdtPlugin;
import de.marw.cdt.cmake.core.internal.settings.CMakePreferences;
import de.marw.cdt.cmake.core.internal.settings.CmakeDefine;
import de.marw.cdt.cmake.core.internal.settings.CmakeUnDefine;
import de.marw.cdt.cmake.core.internal.settings.ConfigurationManager;

/**
 * UI to control general project properties for cmake. This tab is responsible
 * for storing its values.
 *
 * @author Martin Weber
 */
public class CMakeSymbolsTab extends QuirklessAbstractCPropertyTab {

  /**  */
  private static final ILog log = CdtPlugin.getDefault().getLog();

  /** the table showing the cmake defines */
  private DefinesViewer definesViewer;
  /** the table showing the cmake undefines */
  private UnDefinesViewer undefinesViewer;

  // This page can be displayed for project
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
    usercomp.setLayout(new GridLayout(1, false));
    // cmake defines table...
    definesViewer = new DefinesViewer(usercomp);
    // cmake undefines table...
    undefinesViewer = new UnDefinesViewer(usercomp);
  }

  @Override
  protected void configSelectionChanged(ICResourceDescription lastConfig, ICResourceDescription newConfig) {
    // nothing to save here: defines & undefines are modified by the widget
    // listeners directly
    // if (lastConfig != null) {
    // saveToModel();
    // }

    if (newConfig == null)
      return;
    if (page.isMultiCfg()) {
      setAllVisible(false, null);
    } else {
      setAllVisible(true, null);

      ICConfigurationDescription cfgd = newConfig.getConfiguration();
      final ConfigurationManager configMgr = ConfigurationManager.getInstance();
      try {
        CMakePreferences prefs = configMgr.getOrLoad(cfgd);
        updateDisplay(prefs);
      } catch (CoreException ex) {
        log.log(new Status(IStatus.ERROR, CdtPlugin.PLUGIN_ID, null, ex));
      }
    }
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
      CMakePreferences srcPrefs = configMgr.getOrLoad(srcCfg);
      CMakePreferences dstPrefs = configMgr.getOrCreate(dstCfg);
      if (srcPrefs != dstPrefs) {

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
      log.log(new Status(IStatus.ERROR, CdtPlugin.PLUGIN_ID, null, ex));
    }
  }

  @Override
  protected void performOK() {
    final ICResourceDescription resDesc = getResDesc();
    if (resDesc == null)
      return;
    final ICConfigurationDescription cfgd= resDesc.getConfiguration();
    if(cfgd instanceof ICMultiConfigDescription){
      // this tab does not support editing of multiple configurations
      return;
    }
    try {
      // NB: defines & undefines are modified by the widget listeners directly
      CMakePreferences prefs = ConfigurationManager.getInstance().get(cfgd);

      // save as project settings..
      ICStorageElement storage = cfgd.getStorage(
          CMakePreferences.CFG_STORAGE_ID, true);
      prefs.saveToStorage(storage);

    } catch (CoreException ex) {
      log.log(new Status(IStatus.ERROR, CdtPlugin.PLUGIN_ID, null, ex));
    }
  }

  /*-
   * @see org.eclipse.cdt.ui.newui.AbstractCPropertyTab#performDefaults()
   */
  @Override
  protected void performDefaults() {
    final ICResourceDescription resDesc = getResDesc();
    if (resDesc == null)
      return;
    final ICConfigurationDescription cfgd= resDesc.getConfiguration();
    final CMakePreferences prefs = ConfigurationManager.getInstance().get(cfgd);
    prefs.reset();
    updateDisplay(prefs);
  }

  /**
   * Updates displayed values according to the preferences edited by this tab.
   *
   * @param cMakePreferences
   *          the CMakePreferences to display, never <code>null</code>
   */
  private void updateDisplay(CMakePreferences cMakePreferences) {
    definesViewer.setInput(cMakePreferences.getDefines());
    undefinesViewer.setInput(cMakePreferences.getUndefines());
  }

  /*-
   * @see org.eclipse.cdt.ui.newui.AbstractCPropertyTab#updateButtons()
   */
  @Override
  protected void updateButtons() {
    // never called from superclass, but abstract :-)
  }

}
