/*******************************************************************************
 * Copyright (c) 2019 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/

package de.marw.cmake.cdt.internal.lsp;

import org.eclipse.core.runtime.IPath;

import de.marw.cmake.cdt.lsp.DefaultToolCommandlineParser;
import de.marw.cmake.cdt.lsp.IArglet;
import de.marw.cmake.cdt.lsp.IToolCommandlineParser;
import de.marw.cmake.cdt.lsp.ResponseFileArglets;
import de.marw.cmake.cdt.lsp.Arglets.IncludePathGeneric;
import de.marw.cmake.cdt.lsp.Arglets.MacroDefineGeneric;
import de.marw.cmake.cdt.lsp.Arglets.MacroUndefineGeneric;
import de.marw.cmake.cdt.lsp.Arglets.NameOptionMatcher;
import de.marw.cmake.cdt.lsp.Arglets.NameValueOptionMatcher;

/**
 * An {@link IToolCommandlineParser} for the microsoft C and C++ compiler (cl).
 *
 * @author Martin Weber
 */
class MsclToolCommandlineParser extends DefaultToolCommandlineParser {

  private static final IArglet[] arglets = { new IncludePath_C_CL(), new MacroDefine_C_CL(), new MacroUndefine_C_CL() };

  public MsclToolCommandlineParser() {
    super(null, new ResponseFileArglets.At(), null, arglets);
  }

  /**
   * Overridden to get the language ID from the file name extension.
   */
  @Override
  public String getLanguageId(String sourceFileExtension) {
    return super.determineLanguageId(sourceFileExtension);
  }

  ////////////////////////////////////////////////////////////////////
  /** matches a macro name, with optional macro parameter list */
  private static final String REGEX_MACRO_NAME = "([\\w$]+)(?:\\([\\w$, ]*?\\))?";
  /**
   * matches a macro name, skipping leading whitespace. Name in matcher group 1
   */
  private static final String REGEX_MACRO_NAME_SKIP_LEADING_WS = "\\s*" + REGEX_MACRO_NAME;
  /** matches an include path with quoted directory. Name in matcher group 2 */
  private static final String REGEX_INCLUDEPATH_QUOTED_DIR = "\\s*([\"'])(.+?)\\1";
  /**
   * matches an include path with unquoted directory. Name in matcher group 1
   */
  private static final String REGEX_INCLUDEPATH_UNQUOTED_DIR = "\\s*([^\\s]+)";

  /**
   * A tool argument parser capable to parse a cl (Microsoft c compiler) compatible C-compiler include path argument:
   * {@code /Ipath}.
   */
  public static class IncludePath_C_CL extends IncludePathGeneric implements IArglet {
    private static final NameOptionMatcher[] optionMatchers = {
        /* quoted directory */
        new NameOptionMatcher("[-/]I" + REGEX_INCLUDEPATH_QUOTED_DIR, 2),
        /* unquoted directory */
        new NameOptionMatcher("[-/]I" + REGEX_INCLUDEPATH_UNQUOTED_DIR, 1), };

    /*-
     * @see de.marw.cmake.cdt.lsp.IArglet#processArgs(java.lang.String)
     */
    @Override
    public int processArgument(IParseContext parseContext, IPath cwd, String argsLine) {
      return processArgument(parseContext, cwd, argsLine, optionMatchers);
    }
  }

  /**
   * A tool argument parser capable to parse a cl (Microsoft c compiler) compatible C-compiler macro definition
   * argument: {@code /DNAME=value}.
   */
  public static class MacroDefine_C_CL extends MacroDefineGeneric implements IArglet {

    private static final NameValueOptionMatcher[] optionMatchers = {
        /* quoted value, whitespace in value, w/ macro arglist */
        new NameValueOptionMatcher("[-/]D" + REGEX_MACRO_NAME_SKIP_LEADING_WS + "((?:=)([\"'])(.+?)\\4)", 1, 5),
        /* w/ macro arglist */
        new NameValueOptionMatcher("[-/]D" + REGEX_MACRO_NAME_SKIP_LEADING_WS + "((?:=)(\\S+))?", 1, 3),
        /* quoted name, whitespace in value, w/ macro arglist */
        new NameValueOptionMatcher("[-/]D" + REGEX_MACRO_NAME_SKIP_LEADING_WS + "((?:=)(.+?))?\\1", 2, 5),
        /* w/ macro arglist, shell escapes \' and \" in value */
        new NameValueOptionMatcher("[-/]D" + REGEX_MACRO_NAME_SKIP_LEADING_WS + "(?:=)((\\\\([\"']))(.*?)\\2)", 1,
            2), };

    /*-
     * @see de.marw.cmake.cdt.lsp.IArglet#processArgs(java.lang.String)
     */
    @Override
    public int processArgument(IParseContext parseContext, IPath cwd, String argsLine) {
      return processArgument(parseContext, argsLine, optionMatchers);
    }
  }

  /**
   * A tool argument parser capable to parse a cl (Microsoft c compiler) compatible C-compiler macro cancel argument:
   * {@code /UNAME}.
   */
  public static class MacroUndefine_C_CL extends MacroUndefineGeneric implements IArglet {

    private static final NameOptionMatcher optionMatcher = new NameOptionMatcher(
        "[-/]U" + REGEX_MACRO_NAME_SKIP_LEADING_WS, 1);

    /*-
     * @see de.marw.cmake.cdt.lsp.IArglet#processArgument(java.util.List, java.lang.String)
     */
    @Override
    public int processArgument(IParseContext parseContext, IPath cwd, String argsLine) {
      return processArgument(parseContext, argsLine, optionMatcher);
    }
  }
}
