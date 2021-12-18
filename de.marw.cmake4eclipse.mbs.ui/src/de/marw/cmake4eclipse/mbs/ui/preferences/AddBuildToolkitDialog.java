/*******************************************************************************
 * Copyright (c) 2021 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.ui.preferences;

import org.eclipse.swt.widgets.Shell;

import de.marw.cmake4eclipse.mbs.preferences.BuildToolKitDefinition;

/**
 * The dialog used to add, duplicate or edit a build tool kit definition.
 *
 * @author Martin Weber
 */
class AddBuildToolkitDialog extends AbstractBuildToolkitDialog {
  /**
   * Creates the dialog.
   *
   * @param parentShell
   * @param toolkit   the build tool kit to edit
   */
  public AddBuildToolkitDialog(Shell parentShell, BuildToolKitDefinition toolkit) {
    super(parentShell, toolkit, "Add CMake Build Tool Kit");
  }
}
