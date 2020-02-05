/*******************************************************************************
 * Copyright (c) 2020 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake.cdt.lsp.nvidia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;

import de.marw.cmake.cdt.lsp.Arglets.BuiltinDetctionArgsGeneric;
import de.marw.cmake.cdt.lsp.IArglet;

/**
 * A tool argument parser capable to parse a nvcc option to specify the language standard {@code --std=xxx}.
 */
public class NvccLangStdArglet extends BuiltinDetctionArgsGeneric implements IArglet {
  private static final Matcher[] optionMatchers = { Pattern.compile("--std \\S+").matcher(""),
      Pattern.compile("-std \\S+").matcher(""), };

  /*-
   * @see de.marw.cmake.cdt.lsp.IArglet#processArgs(java.lang.String)
   */
  @Override
  public int processArgument(IParseContext parseContext, IPath cwd, String argsLine) {
    return processArgument(parseContext, argsLine, optionMatchers);
  }
}