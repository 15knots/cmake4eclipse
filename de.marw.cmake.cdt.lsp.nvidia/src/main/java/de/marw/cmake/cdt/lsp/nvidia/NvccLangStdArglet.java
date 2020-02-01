/*******************************************************************************
 * Copyright (c) 2020 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
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