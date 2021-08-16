/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.ui;

import java.util.EnumSet;

import de.marw.cmake4eclipse.mbs.settings.CMakePreferences;
import de.marw.cmake4eclipse.mbs.settings.CmakeGenerator;
import de.marw.cmake4eclipse.mbs.settings.LinuxPreferences;

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
   * @see de.marw.cmake4eclipse.mbs.ui.AbstractOsPropertyTab#getOsPreferences(de.marw.cmake4eclipse.mbs.internal.CMakePreferences)
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
