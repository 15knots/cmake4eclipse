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

import org.eclipse.swt.widgets.Shell;

/**
 * Inputs text from a selection dialog. Wrapper for the methods
 * {@code org.eclipse.cdt.ui.newui.AbstractCPropertyTab.get*Dialog}.
 *
 * @author Martin Weber
 * @see org.eclipse.cdt.ui.newui.AbstractCPropertyTab#getFileSystemDirDialog
 */
public interface SelectionDialog {

  /**
   * Gets the text to be shown on the button that fires up this selection
   * dialog.
   */
  public abstract String getLauncherButtonText();

  /**
   * Opens the dialog and, after the dialog was closed, returns the text entered
   * by the user.
   *
   * @param shell
   *        the dialog shell.
   * @return the text or {@code null}, if the dialog was cancelled.
   */
  public abstract String getTextFromDialog(Shell shell);
}
