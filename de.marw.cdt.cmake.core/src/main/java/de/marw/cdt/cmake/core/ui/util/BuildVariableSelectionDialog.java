/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core.ui.util;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * Allows the user to select a build variable.<br>
 * Handles the dialog of method {@link AbstractCPropertyTab#getVariableDialog}.
 *
 * @author Martin Weber
 */
public class BuildVariableSelectionDialog implements SelectionDialog {

  private final ICConfigurationDescription configurationDescription;

  /**
   * @param configurationDescription
   *        the ICConfigurationDescription as "documented" at
   *        {@link AbstractCPropertyTab#getVariableDialog}.
   */
  public BuildVariableSelectionDialog(
      ICConfigurationDescription configurationDescription) {
    this.configurationDescription = configurationDescription;
  }

  @Override
  public String getLauncherButtonText() {
    return AbstractCPropertyTab.VARIABLESBUTTON_NAME;
  }

  @Override
  public String getTextFromDialog(Shell shell) {
    StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(shell);
    dialog.open();
    return dialog.getVariableExpression();
//    return AbstractCPropertyTab.getVariableDialog(shell,
//        configurationDescription);
  }

}
