/*******************************************************************************
 * Copyright (c) 2018 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/

package de.marw.cdt.cmake.core.ui;

import org.eclipse.cdt.core.ConsoleOutputStream;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.ui.IBuildConsoleManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

/**
 * @author Martin Weber
 */
public abstract class AbstractConsole implements IConsole{

  private IConsole console;

  /**
   * Gets a console manager that is configured to the console`s display-name, the ID and icon.
   *
   * @see org.eclipse.cdt.ui.CUIPlugin#getConsoleManager(String, String, URL)
   */
  protected abstract IBuildConsoleManager getConsoleManager();

  @Override
  public ConsoleOutputStream getOutputStream() throws CoreException {
    return console.getOutputStream();
  }

  @Override
  public ConsoleOutputStream getInfoStream() throws CoreException {
    return console.getInfoStream();
  }

  @Override
  public ConsoleOutputStream getErrorStream() throws CoreException {
    return console.getErrorStream();
  }

  @Override
  public void start(IProject project) {
    IBuildConsoleManager consoleManager = getConsoleManager();
    console = consoleManager.getConsole(project);
    console.start(project);
  }

}