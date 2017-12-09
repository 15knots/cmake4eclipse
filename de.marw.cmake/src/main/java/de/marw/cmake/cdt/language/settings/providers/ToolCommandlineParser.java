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

import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.core.runtime.IPath;

/**
 * Parses the build output produced by a specific tool invocation and detects
 * LanguageSettings.
 *
 * @author Martin Weber
 */
class ToolCommandlineParser implements IToolCommandlineParser {

  private final IToolArgumentParser[] argumentParsers;
  private final String languageID;

  /**
   * @param languageID
   *        the language ID of the language that the tool compiles.
   * @param argumentParsers
   *        the parsers for the command line arguments of of interest for the
   *        tool.
   * @throws NullPointerException
   *         if {@code argumentParsers} is {@code null}
   */
  public ToolCommandlineParser(String languageID, IToolArgumentParser... argumentParsers) {
    if (argumentParsers == null) {
      throw new NullPointerException("argumentParsers");
    }
    if (languageID == null) {
      throw new NullPointerException("languageID");
    }
    this.languageID = languageID;
    this.argumentParsers = argumentParsers;
  }

  @Override
  public List<ICLanguageSettingEntry> processArgs(IPath cwd, String buildOutput) {
    List<ICLanguageSettingEntry> entries = new ArrayList<>();
    while (!(buildOutput = ToolCommandlineParser.trimLeadingWS(buildOutput)).isEmpty()) {
      boolean argParsed = false;
      // try all argument parsers...
      for (IToolArgumentParser tap : argumentParsers) {
        int consumed = tap.processArgument(entries, cwd, buildOutput);
        if (consumed > 0) {
          buildOutput = buildOutput.substring(consumed);
          argParsed = true;
          break;
        }
      }

      if (!argParsed && !buildOutput.isEmpty()) {
        // tried all parsers, argument is still not parsed,
        // (blindly) advance to next whitespace
        int idx;
        if ((idx = buildOutput.indexOf(' ')) != -1) {
          buildOutput = buildOutput.substring(idx);
        } else {
          // non-option arg, may be a file name
          // for now, we just clear/skip the output
          buildOutput = "";
        }
      }
    }
    return entries;
  }

  /*-
   * @see de.marw.cmake.cdt.language.settings.providers.CmakeBuildOutputParser.IBuildOutputToolParser#getLanguageId()
   */
  @Override
  public String getLanguageId() {
    return languageID;
  }

  @Override
  public String toString() {
    return "[languageID=" + this.languageID + ", argumentParsers=" + Arrays.toString(this.argumentParsers) + "]";
  }

  /**
   * Returns a copy of the string, with leading whitespace omitted.
   *
   * @param string
   *        the string to remove whitespace from
   * @return A copy of the string with leading white space removed, or the
   *         string if it has no leading white space.
   */
  static String trimLeadingWS(String string) {
    int len = string.length();
    int st = 0;

    while ((st < len) && ( string.charAt(st) <= ' ')) {
      st++;
    }
    return st > 0 ? string.substring(st, len) : string;
  }

} // ToolOutputParser