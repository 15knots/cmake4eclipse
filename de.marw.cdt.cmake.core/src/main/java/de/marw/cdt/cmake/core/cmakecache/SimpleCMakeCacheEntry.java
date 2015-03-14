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
package de.marw.cdt.cmake.core.cmakecache;

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