/*******************************************************************************
 * Copyright (c) 2015 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core.language.settings.providers;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;

/**
 * Various tool argument parser implementations.
 *
 * @author Martin Weber
 */
class ToolArgumentParsers {

  /**
   * nothing to instantiate
   */
  private ToolArgumentParsers() {
  }

  ////////////////////////////////////////////////////////////////////
  /**
   * A tool argument parser capable to parse a POSIX compatible C-compiler macro
   * definition argument: {@code -DNAME=value}.
   */
  static class MacroDefine_C_POSIX implements IToolArgumentParser {
    /** matches a macro name, with optional macro argument list */
    private static final String REGEX_NAME = "([\\w$]+)(?:\\(([\\w$, ]+)\\))?";

    static final MacroDefineOptionParser[] optionParsers = {
        /* quoted value, whitespace in value, w/ macro arglist */
        new MacroDefineOptionParser("-D\\s*" + REGEX_NAME
            + "((?:=)([\"'])(.+?)\\4)", 1, 5),
        /* w/ macro arglist */
        new MacroDefineOptionParser("-D\\s*" + REGEX_NAME + "((?:=)(\\S+))?",
            1, 4),
        /* quoted, whitespace in value, w/ macro arglist */
        new MacroDefineOptionParser("-D\\s*([\"'])" + REGEX_NAME
            + "((?:=)(.+?))?\\1", 2, 5),
        /* w/ macro arglist, shell escapes \' and \" in value */
        new MacroDefineOptionParser("-D\\s*" + REGEX_NAME
            + "(?:=)((\\\\([\"']))(.*?)\\2)", 1, 2), };

    /*-
     * @see de.marw.cdt.cmake.core.language.settings.providers.IToolArgumentParser#processArgs(java.lang.String)
     */
    @Override
    public int processArgument(List<ICLanguageSettingEntry> returnedEntries,
        String args) {
      for (MacroDefineOptionParser parser : optionParsers) {
        final Matcher matcher = parser.matcher;

        matcher.reset(args);
        if (matcher.lookingAt()) {
          final String name = matcher.group(parser.nameGroup);
          final String value = matcher.group(parser.valueGroup);
          final ICLanguageSettingEntry entry = CDataUtil.createCMacroEntry(
              name, value, 0);
          returnedEntries.add(entry);
          final int end = matcher.end();
          return end;
        }
      }
      return 0;// no input consumed
    }

    static class MacroDefineOptionParser {
      private final Matcher matcher;
      private final int nameGroup;
      private final int valueGroup;

      /**
       * Constructor.
       *
       * @param pattern
       *        - regular expression pattern being parsed by the parser.
       * @param nameGroup
       *        - capturing group number defining name of an entry.
       * @param valueGroup
       *        - capturing group number defining value of an entry.
       */
      public MacroDefineOptionParser(String pattern, int nameGroup,
          int valueGroup) {
        this.matcher = Pattern.compile(pattern).matcher("");
        this.nameGroup = nameGroup;
        this.valueGroup = valueGroup;
      }
    }
  }

  ////////////////////////////////////////////////////////////////////
  /**
   * A tool argument parser capable to parse a POSIX compatible C-compiler macro
   * cancel argument: {@code -UNAME}.
   */
  static class MacroUndefine_C_POSIX implements IToolArgumentParser {
    /** matches a macro name */
    private static final String REGEX_NAME = "([\\w$]+)(?:\\(([\\w$, ]+)\\))?";

    private final Matcher matcher = Pattern.compile("-U\\s*" + REGEX_NAME)
        .matcher("");

    /*-
     * @see de.marw.cdt.cmake.core.language.settings.providers.IToolArgumentParser#processArgument(java.util.List, java.lang.String)
     */
    @Override
    public int processArgument(List<ICLanguageSettingEntry> returnedEntries,
        String argsLine) {
      matcher.reset(argsLine);
      if (matcher.lookingAt()) {
        final String name = matcher.group(1);
        final ICLanguageSettingEntry entry = CDataUtil.createCMacroEntry(name,
            null, ICSettingEntry.UNDEFINED);
        returnedEntries.add(entry);
        final int end = matcher.end();
        return end;
      }
      return 0;// no input consumed
    }
  }

  ////////////////////////////////////////////////////////////////////
  /**
   * A tool argument parser capable to parse a POSIX compatible C-compiler
   * include path argument: {@code -Ipath}.
   */
  static class IncludePath_C_POSIX implements IToolArgumentParser {
    static final IncludePathOptionParser[] optionParsers = {
    /* quoted directory */
    new IncludePathOptionParser("-I\\s*([\"'])(.*?)\\1", 2),
    /* unquoted directory */
    new IncludePathOptionParser("-I\\s*([^\\s\"']*?)", 1), };

    /*-
     * @see de.marw.cdt.cmake.core.language.settings.providers.IToolArgumentParser#processArgs(java.lang.String)
     */
    @Override
    public int processArgument(List<ICLanguageSettingEntry> returnedEntries,
        String args) {
      for (IncludePathOptionParser parser : optionParsers) {
        final Matcher matcher = parser.matcher;

        matcher.reset(args);
        if (matcher.lookingAt()) {
          final String name = matcher.group(parser.nameGroup);
          final ICLanguageSettingEntry entry = CDataUtil
              .createCIncludePathEntry(name, 0);
          returnedEntries.add(entry);
          final int end = matcher.end();
          return end;
        }
      }
      return 0;// no input consumed
    }

    static class IncludePathOptionParser {
      private final Matcher matcher;
      private final int nameGroup;

      /**
       * Constructor.
       *
       * @param pattern
       *        - regular expression pattern being parsed by the parser.
       * @param nameGroup
       *        - capturing group number defining name of an entry.
       */
      public IncludePathOptionParser(String pattern, int nameGroup) {
        this.matcher = Pattern.compile(pattern).matcher("");
        this.nameGroup = nameGroup;
      }
    }
  }
  ////////////////////////////////////////////////////////////////////
}
