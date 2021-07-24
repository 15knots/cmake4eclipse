/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cdt.cmake.core.settings;

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
