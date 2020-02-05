/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cdt.cmake.core.ui;

import java.util.EnumSet;

import de.marw.cdt.cmake.core.settings.CMakePreferences;
import de.marw.cdt.cmake.core.settings.CmakeGenerator;
import de.marw.cdt.cmake.core.settings.LinuxPreferences;

/**
 * UI to control host Linux specific project properties and preferences for
 * cmake. This tab is responsible for storing its values.
 *
 * @author Martin Weber
 */
public class LinuxPropertyTab extends AbstractOsPropertyTab<LinuxPreferences> {

  private static final EnumSet<CmakeGenerator> generators = EnumSet
      .of(CmakeGenerator.UnixMakefiles,CmakeGenerator.Ninja);

  /*-
   * @see de.marw.cdt.cmake.core.ui.AbstractOsPropertyTab#getOsPreferences(de.marw.cdt.cmake.core.internal.CMakePreferences)
   */
  @Override
  protected LinuxPreferences getOsPreferences(CMakePreferences prefs) {
    return prefs.getLinuxPreferences();
  }

  @Override
  protected EnumSet<CmakeGenerator> getAvailableGenerators() {
    return LinuxPropertyTab.generators;
  }

}
