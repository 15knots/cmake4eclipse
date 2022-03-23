/*******************************************************************************
 * Copyright (c) 2022 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake4eclipse.mbs.console;

/**
 * Constants for the CBuildConsole extension point id.
 *
 * @author Martin Weber
 */
public interface CdtConsoleConstants {
  /** CBuildConsole extension point id for the cmake console */
  public static final String CMAKE_CONSOLE_ID = "cmake4eclipse.mbs.cmakeConsole";
  /** CBuildConsole extension point id for the compiler-builtins detection console */
  public static final String BUILTINS_DETECTION_CONSOLE_ID = "cmake4eclipse.mbs.builtinsConsole";
}
