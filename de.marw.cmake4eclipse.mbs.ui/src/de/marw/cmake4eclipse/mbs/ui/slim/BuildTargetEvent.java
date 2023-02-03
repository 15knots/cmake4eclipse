/*******************************************************************************
 * Copyright (c) 2023 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake4eclipse.mbs.ui.slim;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IProject;

/**
 * Represents a change in the list of build targets of a project (add/remove/modify).
 *
 * @author Martin Weber
 */
class BuildTargetEvent {

  private final IProject project;
  private final List<String> targets;

  /**
   * @param project
   *                the project associated to the build targets
   * @param targets
   *                he new build targets
   */
  BuildTargetEvent(IProject project, List<String> targets) {
    this.project = Objects.requireNonNull(project);
    this.targets = Collections.unmodifiableList( Objects.requireNonNull(targets));
  }

  /**
   * Gets the project associated to the build targets.
   */
  public IProject getProject() {
    return project;
  }

  /**
   * Gets the new build targets.
   */
  public List<String> getTargets() {
    return targets;
  }
}
