/*******************************************************************************
 * Copyright (c) 2013 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core.internal;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;

/**
 * Detects whether one or more CMakeLists.txt files are affected by the resource
 * delta.
 */
/* package */class CMakelistsVisitor implements IResourceVisitor,
    IResourceDeltaVisitor {
  private boolean cmakelistsAffected = false;

  /*
   * @see
   * org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core
   * .resources.IResourceDelta)
   */
  @Override
  public boolean visit(IResourceDelta delta) throws CoreException {
    final IResource resource = delta.getResource();
    if (resource.isDerived())
      return true;
    switch (delta.getKind()) {
    case IResourceDelta.ADDED: // handle added resource
    case IResourceDelta.REMOVED: // handle removed resource
    case IResourceDelta.CHANGED: // handle changed resource
      cmakelistsAffected |= checkCMakeLists(resource);
      break;
    }
    //return true to continue visiting children.
    return cmakelistsAffected ? false : true;
  }

  @Override
  public boolean visit(IResource resource) {
    cmakelistsAffected |= checkCMakeLists(resource);
    //return true to continue visiting children.
    return cmakelistsAffected ? false : true;
  }

  /**
   * Gets whether a CMakeLists.txt file is affected by the resource delta.
   */
  public boolean isCmakelistsAffected() {
    return this.cmakelistsAffected;
  }

  /**
   * Checks for CMakeLists.txt files.
   *
   * @param resource
   * @return {@code true} if the resource is a CMakeLists.txt file, otherwise
   *         {@code false}
   */
  private static boolean checkCMakeLists(IResource resource) {
//    System.out.println("# in CMakeBuilder.checkCMakeLists(): "
//        + resource.getProjectRelativePath());
    if (resource instanceof IFile
        && resource.getName().equals("CMakeLists.txt")) {
      return true; // a CMakeLists.txt is affected
    }
    return false;
  }

}