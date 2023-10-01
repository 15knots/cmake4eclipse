/*******************************************************************************
 * Copyright (c) 2022 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake4eclipse.mbs.ui.navigator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IProject;

/**
 * Represents a container for build targets owned by a project with {@code C4ENature} in the project explorer view.
 *
 * @author Martin Weber
 */
class BuildTargetsContainer {

  private final IProject project;
  private List<NavBuildTarget> navBuildTargets = new ArrayList<>(5);

  /**
   * @param project
   */
  private BuildTargetsContainer(IProject project) {
    this.project = Objects.requireNonNull(project);
  }

  IProject getProject() {
    return project;
  }

  List<NavBuildTarget> getBuildTargets() {
    return navBuildTargets;
  }

  void updateTargets(List<String> newTargetNames) {
    navBuildTargets = new ArrayList<>(5);
    for (String name : newTargetNames) {
      navBuildTargets.add(new NavBuildTarget(this, name));
    }
  }
  /**
   * @param project
   * @param targetNames
   * @return
   */
  public static BuildTargetsContainer create(IProject project, List<String> targetNames) {
    BuildTargetsContainer container = new BuildTargetsContainer(project);
    for (String name : targetNames) {
      container.navBuildTargets.add(new NavBuildTarget(container, name));
    }
    return container;
  }
}
