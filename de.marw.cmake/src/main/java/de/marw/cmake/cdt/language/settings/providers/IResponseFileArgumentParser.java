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

package de.marw.cmake.cdt.language.settings.providers;

/**
 * Parses a 'response file' tool argument and its content.
 *
 * @author Martin Weber
 */
public interface IResponseFileArgumentParser {

  /**
   * Detects whether the first argument on the given {@code argsLine} argument
   * denotes a response file and parses that file.
   *
   * @param parserHandler
   *          the handler to parse the arguments in the response-file`s content
   * @param argsLine
   *          the arguments passed to the tool, as they appear in the build
   *          output. Implementers may safely assume that the specified value
   *          does not contain leading whitespace characters, but trailing WS
   *          may occur.
   * @return the number of characters from {@code argsLine} that has been
   *         processed. Return a value of {@code zero} or less, if this tool
   *         argument parser cannot process the first argument from the input.
   */
  int process(IParserHandler parserHandler, String argsLine);

}
