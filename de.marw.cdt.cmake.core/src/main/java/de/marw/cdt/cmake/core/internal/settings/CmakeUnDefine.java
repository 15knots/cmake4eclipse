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
 * Represents a cmake variable to undefine.
 *
 * @author Martin Weber
 */
public class CmakeUnDefine {
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
}