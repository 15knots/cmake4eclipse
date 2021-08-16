/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.settings;

/**
 * Represents a cmake variable to undefine.
 *
 * @author Martin Weber
 */
public class CmakeUnDefine implements Cloneable {
  private String name;

  /**
   * Creates a new object.
   *
   * @param name
   *        the variable name, must not be empty.
   * @throws IllegalArgumentException
   *         if {@code name} is empty
   * @throws NullPointerException
   *         if {@code name} is {@code null}
   */
  public CmakeUnDefine(String name) {
    setName(name);
  }

  /**
   * Gets the name property.
   *
   * @return the current name property.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Sets the name property.
   *
   * @throws IllegalArgumentException
   *         if {@code name} is empty
   * @throws NullPointerException
   *         if {@code name} is {@code null}
   */
  public void setName(String name) {
    if (name == null) {
      throw new NullPointerException("name");
    }
    if (name.length() == 0) {
      throw new IllegalArgumentException("name");
    }
    this.name = name;
  }

  public String toString() {
    return name;
  }

  /*-
   * @see java.lang.Object#clone()
   */
  public CmakeUnDefine clone() {
    try {
      return (CmakeUnDefine) super.clone();
    } catch (CloneNotSupportedException ex) { // ignore
      return null;
    }
  }
}