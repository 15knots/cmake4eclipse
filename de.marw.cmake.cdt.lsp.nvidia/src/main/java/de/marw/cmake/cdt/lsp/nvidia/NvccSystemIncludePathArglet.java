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

import org.eclipse.core.runtime.IPath;

import de.marw.cmake.cdt.lsp.Arglets.IncludePathGeneric;
import de.marw.cmake.cdt.lsp.Arglets.NameOptionMatcher;
import de.marw.cmake.cdt.lsp.IArglet;

/**
 * A tool argument parser capable to parse a nvcc-compiler system include path
 * argument: {@code -system=dir}.<br>
 * Note that nvcc seems to treat {@code -system=dir} differently from GCC
 * which`s manpage says:
 * <q>If dir begins with "=", then the "=" will be replaced by the sysroot
 * prefix; see --sysroot and -isysroot.</q>
 */
class NvccSystemIncludePathArglet extends IncludePathGeneric implements IArglet {
  static final NameOptionMatcher[] optionMatchers = {
      /* quoted directory */
      new NameOptionMatcher("-isystem=" + "([\"'])(.+?)\\1", 2),
      /* unquoted directory */
      new NameOptionMatcher("-isystem=" + "([^\\s]+)", 1), };

  /*-
   * @see de.marw.cmake.cdt.lsp.IArglet#processArgs(java.lang.String)
   */
  @Override
  public int processArgument(IParseContext parseContext, IPath cwd, String argsLine) {
    return processArgument(parseContext, cwd, argsLine, optionMatchers);
  }
}