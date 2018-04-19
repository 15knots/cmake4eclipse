/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
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

import de.marw.cdt.cmake.core.CmakeGenerator;
import de.marw.cdt.cmake.core.settings.CMakePreferences;
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
   * @see de.marw.cdt.cmake.core.ui.AbstractOsPropertyTab#getOsPreferences(de.marw.cdt.cmake.core.settings.CMakePreferences)
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
