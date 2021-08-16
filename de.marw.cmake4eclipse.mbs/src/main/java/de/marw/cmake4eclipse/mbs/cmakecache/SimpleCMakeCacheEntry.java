/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.cmakecache;

/**
 * Represents an entry of a CMakeCache.txt file in a simple form: Holds only
 * key-value-pairs of an entry. It does not extract any help texts nor entry
 * types.
 *
 * @author Martin Weber
 */
public class SimpleCMakeCacheEntry {
  private final String key;
  private final String value;

  /**
   * @throws IllegalArgumentException
   *         if {@code key} is empty
   * @throws NullPointerException
   *         if {@code key} is {@code null} or if {@code value} is {@code null}
   */
  public SimpleCMakeCacheEntry(String key, String value) {
    if (key == null) {
      throw new NullPointerException("key");
    }
    if (key.length() == 0) {
      throw new IllegalArgumentException("key");
    }
    if (value == null) {
      throw new NullPointerException("value");
    }

    this.value = value;
    this.key = key;
  }

  /**
   * Gets the key property.
   *
   * @return the current key property.
   */
  public String getKey() {
    return this.key;
  }

  /**
   * Gets the value.
   *
   * @return the current value.
   */
  public String getValue() {
    return this.value;
  }

  public String toString() {
    return key + "=" + value;
  }

}