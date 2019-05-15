/*******************************************************************************
 * Copyright (c) 2015-2019 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cmake.cdt.language.settings.providers;

import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.core.runtime.IPath;

/**
 * Converts tool arguments into LanguageSettings objects and compiler command-line arguments that affect
 * built-in-settings detection.
 *
 * @author Martin Weber
 */
public interface IToolArgumentParser {

  /**
   * Parses the next command-line argument and extracts all detected
   * LanguageSettings objects.
   *
   * @param parseContext
   *        the buffer that receives the new {@code LanguageSettings}
   * @param cwd
   *          the current working directory of the compiler at its invocation
   * @param argsLine
   *        the arguments passed to the tool, as they appear in the build
   *        output. Implementers may safely assume that the specified value does
   *        not contain leading whitespace characters, but trailing WS may
   *        occur.
   * @return the number of characters from {@code argsLine} that has been
   *         processed. Return a value of {@code zero} or less, if this tool
   *         argument parser cannot process the first argument from the input.
   */
  int processArgument(IParseContext parseContext, IPath cwd, String argsLine);

  /**
   * Gathers the results of argument parsing.
   *
   * @author Martin Weber
   */
  interface IParseContext {
    /**
     * Adds a language setting to the result.
     *
     * @param entry
     */
    void addSettingEntry(ICLanguageSettingEntry entry);

    /**
     * Adds a compiler argument that affects built-in detection to the result. For the GNU compilers, these are options
     * like {@code --sysroot} and options that specify the language's standard ({@code -std=c++17}.
     */
    void addBuiltinDetctionArgument(String argument);;
  }
}