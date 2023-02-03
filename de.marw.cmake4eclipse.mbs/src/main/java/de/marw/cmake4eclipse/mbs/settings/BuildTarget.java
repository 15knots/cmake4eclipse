/*******************************************************************************
 * Copyright (c) 2022 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake4eclipse.mbs.settings;

import java.util.Objects;

/**
 * Represents a build target owned by a project with {@code C4ENature}.
 *
 * @author Martin Weber
 */
public class BuildTarget {

  private String name;

  public BuildTarget(String name) {
    this.name = Objects.requireNonNull(name, "name");
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return "BuildTarget [" + name + "]";
  }
}