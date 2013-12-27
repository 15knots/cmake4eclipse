/*******************************************************************************
 * Copyright (c) 2013 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core.ui;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.ui.newui.AbstractPrefPage;
import org.eclipse.core.runtime.CoreException;

import de.marw.cdt.cmake.core.CMakePlugin;

/**
 * Preference page for CMake workspace settings.
 *
 * @author Martin Weber
 */
public class CMakePrefPage extends AbstractPrefPage {

  /**  */
  static final String CFG_STORAGE_ID = CMakePlugin.PLUGIN_ID + ".cmakeSettings";

  private ICConfigurationDescription prefCfgd = null;

  @Override
  public ICResourceDescription getResDesc() {
    if (prefCfgd == null)
      try {
          prefCfgd = CCorePlugin.getDefault().getPreferenceConfiguration(
              ManagedBuildManager.CFG_DATA_PROVIDER_ID);
      } catch (CoreException e) {
        return null;
      }
    return prefCfgd.getRootFolderDescription();
  }

  /*-
   * @see org.eclipse.cdt.ui.newui.AbstractPrefPage#getHeader()
   */
  @Override
  protected String getHeader() {
    return isSingle() ? null : "CMake settings";
  }

  /*-
   * @see org.eclipse.cdt.ui.newui.AbstractPage#isSingle()
   */
  @Override
  protected boolean isSingle() {
    return true;// currently a single Tab
  }

}
