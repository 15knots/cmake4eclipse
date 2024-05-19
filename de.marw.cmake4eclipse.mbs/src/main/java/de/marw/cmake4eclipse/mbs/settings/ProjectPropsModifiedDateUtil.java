/*******************************************************************************
 * Copyright (c) 2024 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake4eclipse.mbs.settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

import de.marw.cmake4eclipse.mbs.internal.Activator;

/**
 * Utility to persist and query the time stamp of the most recent change to the project properties which affect a forced
 * invocation of cmake.
 * <p>
 * For each project with a nature of {@link de.marw.cmake4eclipse.mbs.nature.C4ENature} a file with the name of the
 * project is stored below the plug-in state area for this plug-in. The
 * {@link java.nio.file.Files#setLastModifiedTime(Path, FileTime) last modification} time stamp of the file should
 * reflect the time stamp of the most recent change to the project properties which affect a forced invocation of cmake.
 * </P>
 *
 * @author Martin Weber
 */
public class ProjectPropsModifiedDateUtil {

  private ProjectPropsModifiedDateUtil() {
  }

  /**
   * Gets the time stamp of the most recent change to the project properties.
   *
   * @param project
   *                the project
   * @return the value in milliseconds, since the epoch (1970-01-01T00:00:00Z)
   */
  public static long getLastModified(IProject project) {
    long pPropsModified = 0;
    try {
      pPropsModified = Files.getLastModifiedTime(getTimestampedFile(project.getName())).toMillis();
    } catch (IOException ignore) {
    }
    return pPropsModified;
  }

  /**
   * Persists the time stamp of the most recent change to the project properties.
   *
   * @param project
   *                  the project
   * @param timestamp
   *                  the value in milliseconds, since the epoch (1970-01-01T00:00:00Z)
   */
  public static void setLastModified(IProject project, long timestamp) {
    final Path timestampedFile = getTimestampedFile(project.getName());
    try {
      if (!Files.exists(timestampedFile)) {
        Files.createFile(timestampedFile);
      }
      Files.setLastModifiedTime(timestampedFile, FileTime.fromMillis(timestamp));
    } catch (IOException ignore) {
    }
  }

  /**
   * Gets the location of the time-stamp file in the local file system.
   *
   * @param projectName
   *                the project
   */
  public static java.nio.file.Path getTimestampedFile(String projectName) {
    IPath pPrefsDir = Activator.getDefault().getStateLocation();
    return pPrefsDir.append(projectName).addFileExtension("ts").toPath();
  }
}
