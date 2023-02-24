/*******************************************************************************
 * Copyright (c) 2022 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake4eclipse.mbs.ui.slim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;

import de.marw.cmake4eclipse.mbs.internal.storage.BuildTargetSerializer;
import de.marw.cmake4eclipse.mbs.internal.storage.Util;
import de.marw.cmake4eclipse.mbs.settings.CMakeSettings;

/**
 * Contributes the build target nodes to the project explorer.
 *
 * @author Martin Weber
 */
public class BuildTargetsTreeContentProvider implements ITreeContentProvider, IBuildTargetListener {

  private static final Object[] NO_CHILDREN = {};
  private StructuredViewer viewer;
  private Map<IProject, BuildTargetsContainer> prjContainerMap = new HashMap<>();

  /**
   *
   */
  public BuildTargetsTreeContentProvider() {
    BuildTargetsManager.getDefault().addListener(this);
  }

  @Override
  public void dispose() {
    BuildTargetsManager.getDefault().removeListener(this);
  }

  @Override
  public Object[] getElements(Object inputElement) {
    // We're not a root provider so this won't get called
    return null;
  }

  @Override
  public Object[] getChildren(Object parentElement) {
    if (parentElement instanceof IProject) {
      IProject prj = (IProject) parentElement;
      BuildTargetsContainer container = getBuildTargetsContainer(prj, null);
      if (container != null) {
        return new Object[] { container };
      }
    } else if (parentElement instanceof BuildTargetsContainer) {
      return ((BuildTargetsContainer) parentElement).getBuildTargets().toArray();
    }
    return NO_CHILDREN;
  }

  /**
   * Gets the cached BuildTargetsContainer.
   *
   * @param project
   *                   the project to get the container for
   * @param newTargets
   *                   the known new targets or <code>null</code> to load the targets from the
   *                   {@code ICProjectDescription}
   * @return the new BuildTargetsContainer or <code>null</code> if the project does not have {@code C4ENature} or if a
   *         CoreException was caught
   */
  private BuildTargetsContainer getBuildTargetsContainer(IProject project, List<String> newTargets) {
    if (prjContainerMap.containsKey(project)) {
      // container for project is present or project is known to NOT have a container
      return prjContainerMap.get(project);
    }

    BuildTargetsContainer conti = null;
    if (BuildTargetsManager.hasC4ENature(project)) {
      List<String> targets;
      if (newTargets != null) {
        // get targets from argument
        targets = newTargets;
      } else {
        // get targets from .cproject
        ICProjectDescription projectDescription = CoreModel.getDefault().getProjectDescription(project, false);
        try {
          ICStorageElement storage = projectDescription.getStorage(CMakeSettings.CFG_STORAGE_ID, false);
          ICStorageElement[] storeTargets = storage.getChildrenByName(CMakeSettings.ELEM_BUILD_TARGETS);
          targets = new ArrayList<>();
          if (storeTargets != null && storeTargets.length > 0) {
            Util.deserializeCollection(targets, new BuildTargetSerializer(), storeTargets[0]);
          }
        } catch (CoreException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
          return null;
        }
      }
      conti = BuildTargetsContainer.create(project, targets);
      // container for project is present now
      prjContainerMap.put(project, conti);
    } else {
      // no C4ENature: project is known to NOT have a container
      prjContainerMap.put(project, null);
    }
    return conti;
  }

  @Override
  public Object getParent(Object element) {
    if (element instanceof IProject) {
      IProject prj = (IProject) element;
      if (BuildTargetsManager.hasC4ENature(prj)) {
        return prj.getParent();
      }
    } else if (element instanceof BuildTargetsContainer) {
      return ((BuildTargetsContainer) element).getProject();
    } else if (element instanceof NavBuildTarget) {
      return ((NavBuildTarget) element).getContainer();
    }
    return null;
  }

  @Override
  public boolean hasChildren(Object parentElement) {
    if (parentElement instanceof IProject) {
      IProject prj = (IProject) parentElement;
      if (BuildTargetsManager.hasC4ENature(prj)) {
        return true;
      }
    } else if (parentElement instanceof BuildTargetsContainer) {
      BuildTargetsContainer cont = (BuildTargetsContainer) parentElement;
      return !cont.getBuildTargets().isEmpty();
    }
    return false;
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    this.viewer = (StructuredViewer) viewer;
    prjContainerMap = new HashMap<>();
  }

  // interface IBuildTargetListener
  @Override
  public void targetsChanged(BuildTargetEvent event) {
    if (viewer == null || viewer.getControl().isDisposed()) {
      return;
    }
    IProject project = event.getProject();
    BuildTargetsContainer cont = prjContainerMap.get(project);
    if (cont != null) {
      cont.updateTargets(event.getTargets());
    }
    // avoid refreshing the complete project tree..
    final Object refresh = cont == null ? project : cont;
    viewer.getControl().getDisplay().asyncExec(() -> {
      viewer.refresh(refresh);
    });
  }
}
