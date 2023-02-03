/*******************************************************************************
 * Copyright (c) 2023 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake4eclipse.mbs.ui.slim;

/**
 * Listens for changes in the list of build targets of a project (add/remove/modify).
 *
 * @author Martin Weber
 */
public interface IBuildTargetListener {
  /**
   * Notified when a change in the list of build targets of a project (add/remove/modify) happened.
   *
   * @param event
   */
  void targetsChanged(BuildTargetEvent event);
}
