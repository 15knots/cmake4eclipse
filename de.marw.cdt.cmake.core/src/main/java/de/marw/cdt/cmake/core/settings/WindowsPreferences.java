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
package de.marw.cdt.cmake.core.settings;

import de.marw.cdt.cmake.core.CmakeGenerator;

/**
 * Preferences that override/augment the generic properties when running under
 * Windows.
 *
 * @author Martin Weber
 */
public class WindowsPreferences extends AbstractOsPreferences {

  private static final String ELEM_OS = "win32";

  /** Overridden to set a sensible generator. */
  public void reset() {
    super.reset();
    setGenerator(CmakeGenerator.MinGWMakefiles);
  }

  /**
   * @return the String "win32".
   */
  protected String getStorageElementName() {
    return ELEM_OS;
  }
}
