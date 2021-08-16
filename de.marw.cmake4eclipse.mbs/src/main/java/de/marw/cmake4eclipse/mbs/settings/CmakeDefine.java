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
 * Represents a cmake variable to define.
 *
 * @author Martin Weber
 */
public class CmakeDefine implements Cloneable {
  private CmakeVariableType type;
  private String name;
  private String value;

  /**
   * Creates a new object with type STRING and an empty value.
   *
   * @param name
   *        the variable name, must not be empty.
   * @throws IllegalArgumentException
   *         if {@code name} is empty
   * @throws NullPointerException
   *         if {@code name} is {@code null}
   */
  public CmakeDefine(String name) {
    this(name, CmakeVariableType.STRING, "");
  }

  /**
   * Creates a new object with the specified values.
   *
   * @param name
   *        the variable name, must not be empty.
   * @param type
   *        the variable type.
   * @param value
   *        the value of the variable, may be empty
   * @throws IllegalArgumentException
   *         if {@code name} is empty
   * @throws NullPointerException
   *         if {@code name} or {@code type} is {@code null}
   */
  public CmakeDefine(String name, CmakeVariableType type, String value) {
    if (type == null) {
      throw new NullPointerException("type");
    }

    setName(name);
    setType(type);
    this.value = value == null ? "" : value;
  }

  /**
   * Gets the type.
   *
   * @return the current type.
   */
  public CmakeVariableType getType() {
    return this.type;
  }

  /**
   * Sets the type.
   *
   * @throws NullPointerException
   *         if {@code type} is {@code null}
   */
  public void setType(CmakeVariableType type) {
    if (type == null) {
      throw new NullPointerException("type");
    }
    this.type = type;
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

  /**
   * Gets the value.
   *
   * @return the current value.
   */
  public String getValue() {
    return this.value;
  }

  /**
   * Sets the value.
   *
   * @throws NullPointerException
   *         if {@code value} is {@code null}
   */
  public void setValue(String value) {
    if (value == null) {
      throw new NullPointerException("value");
    }
    this.value = value;
  }

  public String toString() {
    return name + ":" + type + "=" + value;
  }

  @Override
  public CmakeDefine clone() {
    try {
      return (CmakeDefine) super.clone();
    } catch (CloneNotSupportedException ex) { // ignore
      return null;
    }
  }
}