/*******************************************************************************
 * Copyright (c) 2023 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake4eclipse.mbs.ui.slim;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ListenerList;

import de.marw.cmake4eclipse.mbs.nature.C4ENature;

/**
 * Manages {@code BuildTargetEvent} listeners.
 *
 * @author Martin Weber
 */
public class BuildTargetsManager {
  private static BuildTargetsManager instance;

  private final ListenerList<IBuildTargetListener> listeners = new ListenerList<>(ListenerList.IDENTITY);

  /**
   * Gets the singleton {@code BuildTargetsManager} object.
   */
  public static BuildTargetsManager getDefault() {
    if (instance == null) {
      instance = new BuildTargetsManager();
    }
    return instance;
  }

  public void notifyListeners(BuildTargetEvent event) {
    for (Object listener : listeners.getListeners()) {
      ((IBuildTargetListener) listener).targetsChanged(event);
    }
  }

  public void addListener(IBuildTargetListener listener) {
    listeners.add(listener);
  }

  public void removeListener(IBuildTargetListener listener) {
    listeners.remove(listeners);
  }

  /**
   * Gets whether the specified element is an instance of {@code IProject} and has our project nature.
   *
   * @param project
   */
  public static boolean hasC4ENature(Object project) {
    if (project instanceof IProject) {
      try {
        if (((IProject) project).hasNature(C4ENature.NATURE_ID)) {
          return true;
        }
      } catch (CoreException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return false;
  }
}
