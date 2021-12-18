/*******************************************************************************
 * Copyright (c) 2014-2019 Martin Weber.
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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import de.marw.cmake4eclipse.mbs.settings.CMakePreferences;
import de.marw.cmake4eclipse.mbs.settings.ConfigurationManager;

/**
 * UI to control general project properties for cmake. This tab is responsible for storing its values.
 *
 * @author Martin Weber
 */
public class CMakeSymbolsTab extends QuirklessAbstractCPropertyTab {
  private static final ILog log = Activator.getDefault().getLog();

  /**
   * the preferences associated with our configurations to manage. Initialized in {@link #updateData}. {@code null} if
   * this tab has never been displayed so a user could have made edits.
   */
  private CMakePreferences prefs;

  /** the table showing the cmake defines */
  private DefinesViewer definesViewer;
  /** the table showing the cmake undefines */
  private UnDefinesViewer undefinesViewer;

  // This page can be displayed for project
  @Override
  public boolean canBeVisible() {
    return page.isForProject();
  }

  @Override
  public boolean canSupportMultiCfg() {
    return false;
  }

  @Override
  protected void createControls(Composite parent) {
    super.createControls(parent);
    usercomp.setLayout(new GridLayout(1, false));
    // cmake defines table...
    final ICResourceDescription resDesc = getResDesc();
    definesViewer = new DefinesViewer(usercomp, resDesc == null ? null : resDesc.getConfiguration());
    // cmake undefines table...
    undefinesViewer = new UnDefinesViewer(usercomp);
  }

  @Override
  protected void updateData(ICResourceDescription resd) {
    if (resd == null)
      return;

    if (!page.isMultiCfg()) {
      // workaround for AbstractCPropertyTab.handleTabEvent() bug, switching from multi-cfg
      // to single-cfg does not make this tab visible again...
      setAllVisible(true, null);
    } else {
      prefs = null;
      return;
    }

    final ICConfigurationDescription cfgd = resd.getConfiguration();
    try {
      prefs = ConfigurationManager.getInstance().getOrLoad(cfgd);
      updateDisplay();
    } catch (CoreException ex) {
      log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, null, ex));
    }
  }

  /**
   * Invoked when project configuration changes?? At least when apply button is pressed.
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
    if(!(srcCfg instanceof ICMultiConfigDescription)) {
      try {
        final ConfigurationManager configMgr = ConfigurationManager.getInstance();
        CMakePreferences srcPrefs = configMgr.getOrLoad(srcCfg);
        CMakePreferences dstPrefs = configMgr.getOrCreate(dstCfg);
        if (srcPrefs != dstPrefs) {
          dstPrefs.setDefines(srcPrefs.getDefines());
          dstPrefs.setUndefines(srcPrefs.getUndefines());
        }
        // To have same behavior as CDT >= 9.4 has: Apply DOES PERSIST settings:
        // this also solves the problem with different configuration that have been modified and ApplyEd
        // but would otherwise not be persisted on performOK()
        persist(dst);
      } catch (CoreException ex) {
        log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, null, ex));
      }
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

    final ICConfigurationDescription cfgd = resDesc.getConfiguration();
    try {
      // save as project settings..
      ICStorageElement storage = cfgd.getStorage(CMakePreferences.CFG_STORAGE_ID, true);
      prefs.saveToStorage(storage);
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
    prefs.reset();
    updateDisplay();
  }

  /**
   * Stores displayed values to the preferences edited by this tab.
   */
  private void saveToModel() {
    if (prefs == null)
      return;
    prefs.setDefines(definesViewer.getInput());
    prefs.setUndefines(undefinesViewer.getInput());
  }

  /**
   * Updates displayed values according to the preferences edited by this tab.
   */
  private void updateDisplay() {
    definesViewer.setInput(prefs.getDefines());
    undefinesViewer.setInput(prefs.getUndefines());
  }

}
