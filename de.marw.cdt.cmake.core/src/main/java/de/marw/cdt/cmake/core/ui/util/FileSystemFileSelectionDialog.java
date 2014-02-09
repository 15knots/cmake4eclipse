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

import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.swt.widgets.Shell;

/**
 * Allows the user to browse the file system and to select a file.<br>
 * Handles the dialog of method
 * {@link AbstractCPropertyTab#getFileSystemFileDialog}.
 *
 * @author Martin Weber
 */
public class FileSystemFileSelectionDialog implements SelectionDialog {

  private final String filterPath;

  /**
   * @param filterPath
   *        the filter fath or {@code null}. See
   *        {@link org.eclipse.swt.widgets.FileDialog#setFilterPath(String)}
   */
  public FileSystemFileSelectionDialog(String filterPath) {
    this.filterPath = filterPath;
  }

  @Override
  public String getLauncherButtonText() {
    return AbstractCPropertyTab.FILESYSTEMBUTTON_NAME;
  }

  @Override
  public String getTextFromDialog(Shell shell) {
    return AbstractCPropertyTab.getFileSystemFileDialog(shell, filterPath);
  }

}
