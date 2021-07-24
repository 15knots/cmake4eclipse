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
 * Linux.
 *
 * @author Martin Weber
 */
public class LinuxPreferences extends AbstractOsPreferences {

  private static final String ELEM_OS = "linux";

  /**
   * Creates a new object, initialized with all default values.
   */
  public LinuxPreferences() {
  }

  /**
   * @return the String "linux".
   */
  protected String getStorageElementName() {
    return ELEM_OS;
  }
}
