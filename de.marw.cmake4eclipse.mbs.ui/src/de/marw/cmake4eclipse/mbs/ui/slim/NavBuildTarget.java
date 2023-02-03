/*******************************************************************************
 * Copyright (c) 2022 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake4eclipse.mbs.ui.slim;

import java.util.Objects;

import de.marw.cmake4eclipse.mbs.settings.BuildTarget;

/**
 * Represents a build target owned by a project with {@code C4ENature} in the project explorer view.
 *
 * @author Martin Weber
 */
class NavBuildTarget extends BuildTarget {

  private final BuildTargetsContainer container;
  /**
   * @param container
   * @param name
   */
  NavBuildTarget(BuildTargetsContainer container, String name) {
    super(name);
    this.container = Objects.requireNonNull(container, "container");
  }

  BuildTargetsContainer getContainer() {
    return container;
  }
}
