/*******************************************************************************
 * Copyright (c) 2021 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.preferences;

import java.util.Objects;

import de.marw.cmake4eclipse.mbs.settings.CmakeGenerator;

/**
 * Represents a cmake build tool kit. A build tool kit is comprised of a build-system type (specified through name of
 * the generator that cmake will use to generated the scripts) plus a path, that is: a list of directories where
 * executable files are looked up from.
 *
 * @author Martin Weber
 */
public class BuildToolKitDefinition {
  private static long lastUsedID;
  private long uid;
  private String name;
  private String path;
  private CmakeGenerator generator;
  private boolean externalCmake;
  private String externalCmakeFile;

  /**
   * Creates a new object with the specified values.
   *
   * @param name      the name
   * @param generator the generator to use for build-system generation
   * @param path      the path of the build tool kit
   *
   * @throws NullPointerException if {@code name} or {@code generator} is {@code null}
   */
  public BuildToolKitDefinition(long uid, String name, CmakeGenerator generator, String path) {
    setName(name);
    setGenerator(generator);
    this.path = path == null ? "" : path;
    this.uid = uid;
  }

  /**
   * Creates a new object from the specified template assigning it a new unique ID.
   */
  public BuildToolKitDefinition(BuildToolKitDefinition template) {
    this(createUniqueId(), template.name, template.generator, template.path);
    this.externalCmake = template.externalCmake;
    this.externalCmakeFile = template.externalCmakeFile;
  }

  /**
   * @return the uid
   */
  public long getUid() {
    return uid;
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
   * @throws NullPointerException if {@code name} is {@code null}
   */
  public void setName(String name) {
    this.name = Objects.requireNonNull(name, "name");
  }

  /**
   * Gets the generator to use for build-system generation.
   */
  public CmakeGenerator getGenerator() {
    return generator;
  }

  /**
   * Sets the generator to use for build-system generation
   *
   * @param generator the generator to set
   */
  public void setGenerator(CmakeGenerator generator) {
    this.generator = Objects.requireNonNull(generator, "generator");
  }

  /**
   * Gets the path where executable files are looked up from.
   *
   * @return the current path.
   */
  public String getPath() {
    return this.path;
  }

  /**
   * Sets the path executable files are looked up from.
   *
   * @throws NullPointerException if {@code path} is {@code null}
   */
  public void setPath(String value) {
    if (value == null) {
      throw new NullPointerException("path");
    }
    this.path = value;
  }

  /**
   * Sets whether to use an external cmake executable. An external executable is a file that is not picked up from the
   * path specified in the toolkit definition.
   *
   * @return <code>true</code> if an external cmake executable should be use, otherwise <code>false</code>
   * @see #getPath()
   */
  public boolean isExternalCmake() {
    return externalCmake;
  }

  /**
   * Gets whether to use an external cmake executable.
   *
   * @see #setExternalCmake(boolean)
   */
  public void setExternalCmake(boolean externalCmake) {
    this.externalCmake = externalCmake;
  }

  /**
   * Gets the absolute file system path of the external cmake executable. An external executable is a file that is not
   * picked up from the path specified in the toolkit definition.
   *
   * @return the absolute file system path of the external cmake executable or <code>null</code> if none is specified
   */
  public String getExternalCmakeFile() {
    return externalCmakeFile;
  }

  /**
   * Sets the absolute file system path of the external cmake executable
   *
   * @param the absolute file system path of the external cmake executable or <code>null</code>. Must not be
   *            <code>null</code> if {@link #isExternalCmake()} return true
   */
  public void setExternalCmakeFile(String externalCmakePath) {
    this.externalCmakeFile = externalCmakePath;
  }

  @Override
  public String toString() {
    return name + ", " + path;
  }

  /**
   * Find a unique build tool kit id.
   */
  public static long createUniqueId() {
    long id;
    for (;;) {
      id = System.currentTimeMillis();
      if (id != lastUsedID) {
        break;
      }
      try {
        Thread.sleep(1);
      } catch (InterruptedException ignore) {
      }
    }
    lastUsedID = id;
    return id;
  }
}