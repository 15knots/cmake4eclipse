/*******************************************************************************
 * Copyright (c) 2019 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/

package de.marw.cmake.cdt.language.settings.providers;

/**
 * String manipulation functions.
 *
 * @author Martin Weber
 */
public class StringUtil {

  /** Just static methods.
   *
   */
  private StringUtil() {
  }

  /**
   * Returns a copy of the string, with leading whitespace omitted.
   *
   * @param string
   *          the string to remove whitespace from
   * @return A copy of the string with leading white space removed, or the
   *         string if it has no leading white space.
   */
  public static String trimLeadingWS(String string) {
    int len = string.length();
    int st = 0;

    while ((st < len) && (string.charAt(st) <= ' ')) {
      st++;
    }
    return st > 0 ? string.substring(st, len) : string;
  }

}
