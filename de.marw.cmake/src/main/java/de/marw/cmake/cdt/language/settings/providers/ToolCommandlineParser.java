/*******************************************************************************
 * Copyright (c) 2016 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cmake.cdt.language.settings.providers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.core.runtime.IPath;

import de.marw.cmake.cdt.language.settings.providers.builtins.BuiltinDetectionType;

/**
 * Parses the build output produced by a specific tool invocation and detects
 * LanguageSettings.
 *
 * @author Martin Weber
 */
class ToolCommandlineParser implements IToolCommandlineParser {

  private final IToolArgumentParser[] argumentParsers;
  private final String languageID;
  private final IResponseFileArgumentParser responseFileArgumentParser;
  private final BuiltinDetectionType builtinDetectionType;

  /** gathers all entries */
  private List<ICLanguageSettingEntry> entries;

  private IPath cwd;

  /**
   * @param languageID
   *          the language ID of the language that the tool compiles
   * @param responseFileArgumentParser
   *          the parsers for the response-file command-line argument for the
   *          tool of {@code null} if the tool does not recognize a
   *          response-file argument
   * @param builtinDetectionType
   *          the classification of how to detect compiler-built-in macros and
   *          include paths.
   * @param argumentParsers
   *          the parsers for the command line arguments of of interest for the
   *          tool
   * @throws NullPointerException
   *           if any of the arguments is {@code null}
   */
  public ToolCommandlineParser(String languageID, IResponseFileArgumentParser responseFileArgumentParser,
      BuiltinDetectionType builtinDetectionType, IToolArgumentParser... argumentParsers) {
    this.languageID = Objects.requireNonNull(languageID, "languageID");
    this.builtinDetectionType = Objects.requireNonNull(builtinDetectionType, "builtinDetectionType");
    this.argumentParsers = Objects.requireNonNull(argumentParsers, "argumentParsers");
    this.responseFileArgumentParser = responseFileArgumentParser;
  }

  @Override
  public List<ICLanguageSettingEntry> processArgs(IPath cwd, String args) {
    this.entries = new ArrayList<>();
    this.cwd = Objects.requireNonNull(cwd, "cwd");

    ParserHandler ph = new ParserHandler();
    ph.parseArguments(responseFileArgumentParser, args);
    return entries;
  }

  @Override
  public String getLanguageId() {
    return languageID;
  }

  /** Gets the {@code BuiltinDetectionType}.
   */
  @Override
  public BuiltinDetectionType getBuiltinDetectionType() {
    return builtinDetectionType;
  }

  @Override
  public String toString() {
    return "[languageID=" + this.languageID + ", argumentParsers=" + Arrays.toString(this.argumentParsers) + "]";
  }

  /**
   * Returns a copy of the string, with leading whitespace omitted.
   *
   * @param string
   *          the string to remove whitespace from
   * @return A copy of the string with leading white space removed, or the
   *         string if it has no leading white space.
   */
  /* package */static String trimLeadingWS(String string) {
    int len = string.length();
    int st = 0;

    while ((st < len) && (string.charAt(st) <= ' ')) {
      st++;
    }
    return st > 0 ? string.substring(st, len) : string;
  }

  /**
   * @param buildOutput
   *          the command line arguments to process
   * @return the number of characters consumed
   */
  private static int skipArgument(String buildOutput) {
    int consumed;

    // (blindly) advance to next whitespace
    if ((consumed = buildOutput.indexOf(' ')) != -1) {
      return consumed;
    } else {
      // non-option arg, may be a file name
      // for now, we just clear/skip the output
      return buildOutput.length();
    }
  }

  /**
   * Handles parsing of command-line arguments.
   *
   * @author Martin Weber
   */
  private class ParserHandler implements IParserHandler {

    /**
     * @param responseFileArgumentParser
     * @param args
     *          the command line arguments to process
     */
    private void parseArguments(IResponseFileArgumentParser responseFileArgumentParser, String args) {
      // eat buildOutput string argument by argument..
      while (!(args = ToolCommandlineParser.trimLeadingWS(args)).isEmpty()) {
        boolean argParsed = false;
        int consumed;
        // parse with first parser that can handle the first argument on the
        // command-line
        for (IToolArgumentParser tap : argumentParsers) {
          consumed = tap.processArgument(entries, cwd, args);
          if (consumed > 0) {
            args = args.substring(consumed);
            argParsed = true;
          }
        }

        // try response file
        if (responseFileArgumentParser != null) {
          consumed = responseFileArgumentParser.process(this, args);
          if (consumed > 0) {
            args = args.substring(consumed);
            argParsed = true;
          }
        }
        if (!argParsed && !args.isEmpty()) {
          // tried all parsers, argument is still not parsed,
          // skip argument
          consumed = skipArgument(args);
          if (consumed > 0) {
            args = args.substring(consumed);
          }
        }
      }
    }

    /**
     * Parses the given String with the first parser that can handle the first
     * argument on the command-line.
     *
     * @param args
     *          the command line arguments to process
     */
    @Override
    public void parseArguments(String args) {
      parseArguments(null, args);
    }

    /*
     * (non-Javadoc)
     *
     * @see de.marw.cmake.cdt.language.settings.providers.IParserHandler#
     * getCompilerWorkingDirectory()
     */
    @Override
    public IPath getCompilerWorkingDirectory() {
      return cwd;
    }

  } // ParserHandler
} // ToolOutputParser