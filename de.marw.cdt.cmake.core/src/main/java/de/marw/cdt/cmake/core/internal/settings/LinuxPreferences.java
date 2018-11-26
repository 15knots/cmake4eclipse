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
package de.marw.cdt.cmake.core.internal.settings;


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
