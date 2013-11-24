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
package de.marw.cdt.cmake.core.ui;

import org.eclipse.cdt.core.ConsoleOutputStream;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.cdt.ui.IBuildConsoleManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

/**
 * @author Martin Weber
 */
public class CMakeConsole implements IConsole {

  private static final String CONSOLE_CONTEXT_MENU_ID = "CMakeConsole"; //$NON-NLS-1$

  private IProject project;
  private IBuildConsoleManager fConsoleManager;

  /**
   */
  public CMakeConsole() {
  }

  /**
   * Starts the console for a given project.
   *
   * @param project
   *        - the project to start the console.
   */
  @Override
  public void start(IProject project) {
    this.project = project;
    fConsoleManager = CUIPlugin.getDefault().getConsoleManager("CMake Console",
        CONSOLE_CONTEXT_MENU_ID);
    fConsoleManager.getConsole(project).start(project);
  }

  /*-
   * @see org.eclipse.cdt.core.resources.IConsole#getOutputStream()
   */
  @Override
  public ConsoleOutputStream getOutputStream() throws CoreException {
    return fConsoleManager.getConsole(project).getOutputStream();
  }

  /*-
   * @see org.eclipse.cdt.core.resources.IConsole#getInfoStream()
   */
  @Override
  public ConsoleOutputStream getInfoStream() throws CoreException {
    return fConsoleManager.getConsole(project).getInfoStream();
  }

  /*-
   * @see org.eclipse.cdt.core.resources.IConsole#getErrorStream()
   */
  @Override
  public ConsoleOutputStream getErrorStream() throws CoreException {
    return fConsoleManager.getConsole(project).getErrorStream();
  }

}
