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
package de.marw.cmake.cdt.language.settings.providers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.language.settings.providers.ICBuildOutputParser;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.IWorkingDirectoryTracker;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsSerializableProvider;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import de.marw.cmake.CMakePlugin;
import de.marw.cmake.cmakecache.SimpleCMakeCacheTxt;

/**
 * A build output parser capable to parse the output of the build tool for which
 * cmake has generated the build scripts. Since cmake does a fairly good job to
 * determine the absolute file system location of the tools to invoke, this
 * parser does know exactly to associate a line of the build output with the
 * tool which is invoked.
 *
 * @author Martin Weber
 */
public class CmakeBuildOutputParser extends
    LanguageSettingsSerializableProvider implements ILanguageSettingsProvider,
    ICBuildOutputParser {

  /**
   * regex to match leading chars in a build output line. The full regex to
   * match a tool command is constructed as<br>
   * {@code REGEX_CMD_HEAD}&lt;tool command>{@code REGEX_CMD_TAIL}
   */
  private static final String REGEX_CMD_HEAD = "^(.*?)(";
  /** regex to match trailing chars in a build output line */
  private static final String REGEX_CMD_TAIL = ")";

  private ICConfigurationDescription currentCfgDescription;
  private IProject currentProject;

  /**
   * parsers for each tool of interest that takes part in the current build. The
   * Matcher detects whether a line of the build output is an invocation of the
   * tool.
   */
  private HashMap<Matcher, IBuildOutputToolParser> currentBuildOutputToolParsers;

  private static final IBuildOutputToolParser skipOutput = new IgnoringToolParser();
  /**
   * Names of known tools along with their command line argument parsers.
   */
  private static Map<String, IBuildOutputToolParser> knownCmdParsers = new HashMap<String, IBuildOutputToolParser>(
      4, 1.0f);
  static {
    IToolArgumentParser[] posix_cc_args = {
        new ToolArgumentParsers.MacroDefine_C_POSIX(),
        new ToolArgumentParsers.MacroUndefine_C_POSIX(),
        new ToolArgumentParsers.IncludePath_C_POSIX(),
        // not defined by POSIX, but does not harm..
        new ToolArgumentParsers.SystemIncludePath_C(), };
    // POSIX compatible C compilers...
    BuildOutputToolParser gcc = new BuildOutputToolParser(
        "org.eclipse.cdt.core.gcc", posix_cc_args);
    knownCmdParsers.put("cc", gcc);
    knownCmdParsers.put("clang", gcc);
    // POSIX compatible C++ compilers...
    BuildOutputToolParser gpp = new BuildOutputToolParser(
        "org.eclipse.cdt.core.g++", posix_cc_args);
    knownCmdParsers.put("c++", gpp);
    knownCmdParsers.put("clang++", gpp);
  }

  public CmakeBuildOutputParser() {
  }

  /**
   * Gets the parsed content of the CMake cache file (CMakeCache.txt)
   * corresponding to the specified configuration. If the cache for the parsed
   * content is invalid, tries to parse the CMakeCache.txt file first and then
   * caches the parsed content.
   *
   * @param cfgd
   *        configuration
   * @return the parsed content of the CMake cache file
   * @throws CoreException
   */
  private static SimpleCMakeCacheTxt getParsedCMakeCache(
      ICConfigurationDescription cfgd) throws CoreException {
    SimpleCMakeCacheTxt cmCache = null;

    // If getBuilderCWD() returns a workspace relative path, it is garbled.
    // If garbled, make sure de.marw.cdt.cmake.core.internal.BuildscriptGenerator.getBuildWorkingDir()
    // returns a full, absolute path relative to the workspace.
    final IPath builderCWD = cfgd.getBuildSetting().getBuilderCWD();

    final IFile cmakeCache = ResourcesPlugin.getWorkspace().getRoot()
        .getFile(builderCWD.append("CMakeCache.txt"));
    cmCache = (SimpleCMakeCacheTxt) cmakeCache
        .getSessionProperty(CMakePlugin.CMAKECACHE_PARSED_PROP);
//    System.out.println("have cached CMakeCache: " + (cmCache != null));
    if (cmCache == null) { // must parse CMakeCache.txt

      IPath location = cmakeCache.getLocation();
      if (location == null) {
        return null; // fall back to built-in from generator
      }
      final File file = location.toFile();

      try {
        // parse CMakeCache.txt...
        cmCache = new SimpleCMakeCacheTxt(file);
        // store parsed cache as resource property
        cmakeCache.setSessionProperty(CMakePlugin.CMAKECACHE_PARSED_PROP,
            cmCache);
//        System.out.println("stored cached CMakeCache");
      } catch (IOException ex) {
        throw new CoreException(new Status(IStatus.ERROR,
            CMakePlugin.PLUGIN_ID, "Failed to read file " + file, ex));
      }
    }
    return cmCache;
  }

  /*-
   * interface ICBuildOutputParser
   */
  @Override
  public void startup(ICConfigurationDescription cfgDescription,
      IWorkingDirectoryTracker cwdTracker) throws CoreException {
//    System.out.println("STARTUP Parser " + cfgDescription + ": CWD="
//        + cwdTracker.getWorkingDirectoryURI());
    this.currentCfgDescription = cfgDescription;
    this.currentProject = cfgDescription.getProjectDescription().getProject();

    SimpleCMakeCacheTxt parsedCMakeCache = getParsedCMakeCache(cfgDescription);

    // determine the parsers for the tool invocations of interest
    currentBuildOutputToolParsers = new HashMap<Matcher, IBuildOutputToolParser>(
        4, 1.0f);
    // compilers and linkers...
    for (String toolPath : parsedCMakeCache.getTools()) {
      final String toolName = new File(toolPath).getName();
      for (Entry<String, IBuildOutputToolParser> entry : knownCmdParsers
          .entrySet()) {
        if (toolName.equals(entry.getKey())) {
          Matcher cmdDetector = Pattern.compile(
              REGEX_CMD_HEAD + Pattern.quote(toolPath) + REGEX_CMD_TAIL)
              .matcher("");
          currentBuildOutputToolParsers.put(cmdDetector, entry.getValue());
          break;
        }
      }
    }
    // cmake, cpack, ctest. Only for skipping the output...
    for (String toolPath : parsedCMakeCache.getCmakeCommands()) {
      Matcher cmdDetector = Pattern.compile(
          REGEX_CMD_HEAD + Pattern.quote(toolPath) + REGEX_CMD_TAIL)
          .matcher("");
      currentBuildOutputToolParsers.put(cmdDetector, skipOutput);
    }
    // add parser for the build tool itself (make, ninja, etc.)
    Matcher cmdDetector = Pattern.compile(
        REGEX_CMD_HEAD + Pattern.quote(parsedCMakeCache.getBuildTool())
            + REGEX_CMD_TAIL).matcher("");
    currentBuildOutputToolParsers.put(cmdDetector, skipOutput);
  }

  /*-
   * interface ICBuildOutputParser
   */
  @Override
  public boolean processLine(String line) {
    // try each tool..
    for (Entry<Matcher, IBuildOutputToolParser> entry : currentBuildOutputToolParsers
        .entrySet()) {
      final Matcher cmdDetector = entry.getKey();
      cmdDetector.reset(line);
      if (cmdDetector.lookingAt()) {
        // found a matching tool parser
        String args = line.substring(cmdDetector.end());
        args = trimLeadingWS(args);
        final IBuildOutputToolParser botp = entry.getValue();
        final List<ICLanguageSettingEntry> entries = botp.processArgs(args);
        // attach settings to project...
        if (entries != null && entries.size()>0) {
          super.setSettingEntries(currentCfgDescription, currentProject,
              botp.getLanguageId(), entries);
        }
        return true; // skip other build output parsers
      }
    }
//    System.out.println(line);
    return false;
  }

  /*-
   * interface ICBuildOutputParser
   */
  @Override
  public void shutdown() {
    // release resources for garbage collector
    currentCfgDescription = null;
    currentBuildOutputToolParsers = null;
    currentProject = null;
    //    cwdTracker = null;
  }

  /**
   * Returns a copy of the string, with leading whitespace omitted.
   *
   * @param string
   *        the string to remove whitespace from
   * @return A copy of the string with leading white space removed, or the
   *         string if it has no leading white space.
   */
  private static String trimLeadingWS(String string) {
    int len = string.length();
    int st = 0;

    while ((st < len) && (string.charAt(st) <= ' ')) {
      st++;
    }
    return st > 0 ? string.substring(st, len) : string;
  }

  @Override
  public List<ICLanguageSettingEntry> getSettingEntries(
      ICConfigurationDescription cfgDescription, IResource rc, String languageId) {
    // TODO Auto-generated function stub
    return super.getSettingEntries(cfgDescription, rc, languageId);
  }

  ////////////////////////////////////////////////////////////////////
  // inner classes
  ////////////////////////////////////////////////////////////////////
  /**
   * Parses the build output produced by a specific tool invocation and detects
   * LanguageSettings.
   *
   * @author Martin Weber
   */
  private static interface IBuildOutputToolParser {
    /**
     * Parses all arguments given to the tool from build output.
     *
     * @return the language setting entries produced or {@code null} or an empty
     *         list if no entries where produced
     */
    public List<ICLanguageSettingEntry> processArgs(String args);

    /**
     * Gets the language ID of the language that the tool compiles.
     *
     * @return the language ID, {@code null} is allowed if this parser does not
     *         produce any {@link ICLanguageSettingEntry language settings
     *         entries}
     */
    public String getLanguageId();
  }

  /**
   * Parses the build output produced by a specific tool invocation and detects
   * LanguageSettings.
   *
   * @author Martin Weber
   */
  private static class BuildOutputToolParser implements IBuildOutputToolParser {

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
    public BuildOutputToolParser(String languageID,
        IToolArgumentParser... argumentParsers) {
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
    public List<ICLanguageSettingEntry> processArgs(String buildOutput) {
      List<ICLanguageSettingEntry> entries = new ArrayList<ICLanguageSettingEntry>();
      while (!(buildOutput = trimLeadingWS(buildOutput)).isEmpty()) {
        boolean argParsed = false;
        // try all argument parsers...
        for (IToolArgumentParser tap : argumentParsers) {
          int consumed = tap.processArgument(entries, buildOutput);
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
      return "[languageID=" + this.languageID + ", argumentParsers="
          + Arrays.toString(this.argumentParsers) + "]";
    }

  } // ToolOutputParser

  private static class IgnoringToolParser implements IBuildOutputToolParser {
    @Override
    public List<ICLanguageSettingEntry> processArgs(String args) {
      return null;
    }

    /*-
     * @see de.marw.cmake.cdt.language.settings.providers.CmakeBuildOutputParser.IBuildOutputToolParser#getLanguageId()
     */
    @Override
    public String getLanguageId() {
      return null;
    }

    @Override
    public String toString() {
      return "IgnoringToolParser";
    }

  }
}
