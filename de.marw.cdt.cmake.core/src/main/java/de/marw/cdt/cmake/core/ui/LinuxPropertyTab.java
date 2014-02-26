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

import de.marw.cdt.cmake.core.internal.settings.CMakePreferences;
import de.marw.cdt.cmake.core.internal.settings.LinuxPreferences;

/**
 * UI to control host Linux specific project properties and preferences for
 * cmake. This tab is responsible for storing its values.
 * 
 * @author Martin Weber
 */
public class LinuxPropertyTab extends AbstractOsPropertyTab<LinuxPreferences> {

  /*-
   * @see de.marw.cdt.cmake.core.ui.AbstractOsPropertyTab#getOsPreferences(de.marw.cdt.cmake.core.internal.CMakePreferences)
   */
  @Override
  protected LinuxPreferences getOsPreferences(CMakePreferences prefs) {
    return prefs.getLinuxPreferences();
  }

  @Override
  protected String[] getAvailableGenerators() {
    return new String[] { "Unix Makefiles" };
  }

}
