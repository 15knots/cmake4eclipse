/*******************************************************************************
 * Copyright (c) 2015 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cmake.builder;

import java.util.Iterator;
import org.eclipse.core.commands.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

public class AddRemoveCMakeBuildNatureHandler extends AbstractHandler {

  private ISelection selection;

  public Object execute(ExecutionEvent event) throws ExecutionException {
    // TODO Auto-generated method stub
    ISelection selection = HandlerUtil.getCurrentSelection(event);
    //
    if (selection instanceof IStructuredSelection) {
      for (Iterator<?> it = ((IStructuredSelection) selection).iterator(); it
          .hasNext();) {
        Object element = it.next();
        IProject project = null;
        if (element instanceof IProject) {
          project = (IProject) element;
        } else if (element instanceof IAdaptable) {
          project = (IProject) ((IAdaptable) element)
              .getAdapter(IProject.class);
        }
        if (project != null) {
          try {
            toggleNature(project);
          } catch (CoreException e) {
            //TODO log something
            throw new ExecutionException("Failed to toggle nature", e);
          }
        }
      }
    }

    return null;
  }

  /**
   * Toggles sample nature on a project
   * 
   * @param project
   *        to have sample nature added or removed
   */
  private void toggleNature(IProject project) throws CoreException {
    IProjectDescription description = project.getDescription();
    String[] natures = description.getNatureIds();

    for (int i = 0; i < natures.length; ++i) {
      if (CMakeBuildNature.NATURE_ID.equals(natures[i])) {
        // Remove the nature
        String[] newNatures = new String[natures.length - 1];
        System.arraycopy(natures, 0, newNatures, 0, i);
        System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
        description.setNatureIds(newNatures);
        project.setDescription(description, null);
        return;
      }
    }

    // Add the nature
    String[] newNatures = new String[natures.length + 1];
    System.arraycopy(natures, 0, newNatures, 0, natures.length);
    newNatures[natures.length] = CMakeBuildNature.NATURE_ID;
    description.setNatureIds(newNatures);
    project.setDescription(description, null);
  }

}