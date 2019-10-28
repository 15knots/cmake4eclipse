/*******************************************************************************
 * Copyright (c) 2017 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/

package de.marw.cmake.cdt.lsp;

import org.eclipse.core.runtime.IPath;

/**
 * Handles parsing of command-line arguments.
 *
 * @author Martin Weber
 */
public interface IParserHandler {

  /**
   * Parses the given String with the first parser that can handle the first
   * argument on the command-line.
   *
   * @param args
   *          the command line arguments to process
   */
  void parseArguments(String args);

  /**
   * Gets the current working directory of the compiler at the time of its
   * invocation.
   */
  IPath getCompilerWorkingDirectory();
}
