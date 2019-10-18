/*******************************************************************************
 * Copyright (c) 2016-2019 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cmake.cdt.language.settings.providers;

import java.util.Arrays;
import java.util.Objects;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;

import de.marw.cmake.cdt.internal.CMakePlugin;
import de.marw.cmake.cdt.internal.lsp.ParseContext;
import de.marw.cmake.cdt.language.settings.providers.builtins.BuiltinDetectionType;

/**
 * Parses the build output produced by a specific tool invocation and detects
 * LanguageSettings.
 *
 * @author Martin Weber
 */
public class DefaultToolCommandlineParser implements IToolCommandlineParser {
  private static final boolean DEBUG = Boolean
      .parseBoolean(Platform.getDebugOption(CMakePlugin.PLUGIN_ID + "/CECC/args"));

  private final IArglet[] argumentParsers;
  private final String languageID;
  private final IResponseFileArgumentParser responseFileArgumentParser;
  private final BuiltinDetectionType builtinDetectionType;

  /** gathers the result */
  private ParseContext result;

  private IPath cwd;

  /**
   * @param languageID
   *          the language ID of the language that the tool compiles or {@code null} if the language ID should be
   *          derived from the source file-name extension
   * @param responseFileArgumentParser
   *          the parsers for the response-file command-line argument for the tool or {@code null} if the tool does not
   *          recognize a response-file argument
   * @param builtinDetectionType
   *          the classification of how to detect compiler-built-in macros and include paths.
   * @param argumentParsers
   *          the parsers for the command line arguments of of interest for the tool
   * @throws NullPointerException
   *           if any of the {@code builtinDetectionType} or {@code argumentParsers} arguments is {@code null}
   */
  public DefaultToolCommandlineParser(String languageID, IResponseFileArgumentParser responseFileArgumentParser,
      BuiltinDetectionType builtinDetectionType, IArglet... argumentParsers) {
    this.languageID = languageID;
    this.builtinDetectionType = Objects.requireNonNull(builtinDetectionType, "builtinDetectionType");
    this.argumentParsers = Objects.requireNonNull(argumentParsers, "argumentParsers");
    this.responseFileArgumentParser = responseFileArgumentParser;
  }

  @Override
  public IResult processArgs(IPath cwd, String args) {
    this.result = new ParseContext();
    this.cwd = Objects.requireNonNull(cwd, "cwd");

    ParserHandler ph = new ParserHandler();
    ph.parseArguments(responseFileArgumentParser, args);
    return result;
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
      while (!(args = StringUtil.trimLeadingWS(args)).isEmpty()) {
        boolean argParsed = false;
        int consumed;
        // parse with first parser that can handle the first argument on the
        // command-line
        if (DEBUG)
          System.out.printf(">> Looking up parser for argument '%s ...'%n", args.substring(0, Math.min(50, args.length())));
        for (IArglet tap : argumentParsers) {
          if (DEBUG)
            System.out.printf("   Trying parser %s%n", tap.getClass().getSimpleName());
          consumed = tap.processArgument(result, cwd, args);
          if (consumed > 0) {
            if (DEBUG)
              System.out.printf("<< PARSED ARGUMENT '%s'%n", args.substring(0, consumed));
            args = args.substring(consumed);
            argParsed = true;
            break;
          }
        }

        // try response file
        if (!argParsed && responseFileArgumentParser != null) {
          if (DEBUG)
            System.out.printf("   Trying parser %s%n", responseFileArgumentParser.getClass().getSimpleName());
          consumed = responseFileArgumentParser.process(this, args);
          if (consumed > 0) {
            if (DEBUG)
              System.out.printf("<< PARSED ARGUMENT '%s'%n", args.substring(0, consumed));
            args = args.substring(consumed);
            argParsed = true;
          }
        }
        if (!argParsed && !args.isEmpty()) {
          // tried all parsers, argument is still not parsed,
          // skip argument
          if (DEBUG)
            System.out.printf("<< IGNORING ARGUMENT, no parser found for it: '%s ...'%n",
                args.substring(0, Math.min(50, args.length())));
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