/*******************************************************************************
 * Copyright (c) 2013-2018 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.console;

import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.cdt.ui.IBuildConsoleManager;

/**
 * A console for cmake invocations.
 *
 * @author Martin Weber
 */
public class CMakeConsole extends AbstractConsole {

  private static final String CONSOLE_CONTEXT_MENU_ID = "CMakeConsole"; //$NON-NLS-1$

  @Override
  protected IBuildConsoleManager getConsoleManager() {
    return CUIPlugin.getDefault().getConsoleManager("CMake Console", CONSOLE_CONTEXT_MENU_ID);
  }

}
