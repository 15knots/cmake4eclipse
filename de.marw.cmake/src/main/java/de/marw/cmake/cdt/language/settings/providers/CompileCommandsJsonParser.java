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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
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
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jetty.util.ajax.JSON;

import de.marw.cmake.CMakePlugin;

/**
 * A ILanguageSettingsProvider that parses the file 'compile_commands.json'
 * produced by cmake when option {@code -DCMAKE_EXPORT_COMPILE_COMMANDS=ON} is
 * given.
 *
 * @author Martin Weber
 */
public class CompileCommandsJsonParser extends LanguageSettingsSerializableProvider
    implements ILanguageSettingsProvider, ICBuildOutputParser {
  private static final ILog log = CMakePlugin.getDefault().getLog();

  /**
   * name of the session property attached to project resources. The property
   * caches the time-stamp of the last time the file
   * {@code compile_commands.json} was parsed.
   */
  private static final QualifiedName JSON_PARSED_PROP = new QualifiedName(CMakePlugin.PLUGIN_ID,
      "TS:compile_commands.json");

  private ICConfigurationDescription currentCfgDescription;

  /**
   * tool detector and their tool option parsers for each tool of interest that
   * takes part in the current build. The Matcher detects whether a command line
   * is an invocation of the tool.
   */
  private final HashMap<Matcher, IToolCommandlineParser> currentCmdlineParsers;

  /**
   * last known working tool detector and its tool option parsers or
   * {@code null}, if unknown (to speed up parsing)
   */
  private Entry<Matcher, IToolCommandlineParser> preferredCmdlineParser;

  public CompileCommandsJsonParser() {
    currentCmdlineParsers = new HashMap<Matcher, IToolCommandlineParser>(18, 1.0f);

    /** Names of known tools along with their command line argument parsers */
    final Map<String, IToolCommandlineParser> knownCmdParsers = new HashMap<String, IToolCommandlineParser>(16, 1.0f);

    final IToolArgumentParser[] posix_cc_args = { new ToolArgumentParsers.IncludePath_C_POSIX(),
        new ToolArgumentParsers.MacroDefine_C_POSIX(), new ToolArgumentParsers.MacroUndefine_C_POSIX(),
        // not defined by POSIX, but does not harm..
        new ToolArgumentParsers.SystemIncludePath_C() };
    // POSIX compatible C compilers =================================
    final ToolCommandlineParser gcc = new ToolCommandlineParser("org.eclipse.cdt.core.gcc", posix_cc_args);
    knownCmdParsers.put("cc", gcc);
    knownCmdParsers.put("cc.exe", gcc);
    knownCmdParsers.put("gcc", gcc);
    knownCmdParsers.put("gcc.exe", gcc);
    knownCmdParsers.put("clang", gcc);
    knownCmdParsers.put("clang.exe", gcc);
    // POSIX compatible C++ compilers ===============================
    final ToolCommandlineParser cpp = new ToolCommandlineParser("org.eclipse.cdt.core.g++", posix_cc_args);
    knownCmdParsers.put("c++", cpp);
    knownCmdParsers.put("c++.exe", cpp);
    knownCmdParsers.put("clang++", cpp);
    knownCmdParsers.put("clang++.exe", cpp);

    // ms C + C++ compiler ==========================================
    final IToolArgumentParser[] cl_cc_args = { new ToolArgumentParsers.IncludePath_C_CL(),
        new ToolArgumentParsers.MacroDefine_C_CL(), new ToolArgumentParsers.MacroUndefine_C_CL() };
    final ToolCommandlineParser cl = new ToolCommandlineParser("org.eclipse.cdt.core.gcc", cl_cc_args);
    knownCmdParsers.put("cl.exe", cl);

    // Intel C compilers ============================================
    final ToolCommandlineParser icc = new ToolCommandlineParser("org.eclipse.cdt.core.gcc", posix_cc_args);
    final ToolCommandlineParser icpc = new ToolCommandlineParser("org.eclipse.cdt.core.g++", posix_cc_args);

    // Linux & OS X, EDG
    knownCmdParsers.put("icc", icc);
    // OS X, clang
    knownCmdParsers.put("icl", icc);
    // Intel C++ compiler
    // Linux & OS X, EDG
    knownCmdParsers.put("icpc", icpc);
    // OS X, clang
    knownCmdParsers.put("icl++", icpc);
    // Windows C + C++, EDG
    knownCmdParsers.put("icl.exe", cl);

    // construct matchers that detect the tool name...
    final String REGEX_CMD_HEAD = "^(.*?" + Pattern.quote(File.separator) + ")(";
    final String REGEX_CMD_TAIL = ")";
    for (Entry<String, IToolCommandlineParser> entry : knownCmdParsers.entrySet()) {
      // 'cc' -> matches
      // '/bin/cc' -> matches
      // '/usr/bin/cc' -> matches
      // 'C:\program files\mingw\bin\cc' -> matches
      Matcher cmdDetector = Pattern.compile(REGEX_CMD_HEAD + Pattern.quote(entry.getKey()) + REGEX_CMD_TAIL)
          .matcher("");
      currentCmdlineParsers.put(cmdDetector, entry.getValue());
    }
  }

  /**
   * Overridden to make sure nothing is stored on disk; this provider operates
   * truly in memory.
   */
  @Override
  public IStatus serializeLanguageSettings(ICConfigurationDescription cfgDescription) {
    return Status.OK_STATUS;
  }

  /**
   * Parses the content of the 'compile_commands.json' file corresponding to the
   * specified configuration, if timestamps differ.
   *
   * @throws CoreException
   */
  private void tryParseJson() throws CoreException {

    /** cached file modification timestamp of last parse */
    Long tsCached = null;

    // If getBuilderCWD() returns a workspace relative path, it is garbled.
    // If garbled, make sure de.marw.cdt.cmake.core.internal.BuildscriptGenerator.getBuildWorkingDir()
    // returns a full, absolute path relative to the workspace.
    final IPath builderCWD = currentCfgDescription.getBuildSetting().getBuilderCWD();

    IPath jsonPath = builderCWD.append("compile_commands.json");
    final IFile jsonFileRc = ResourcesPlugin.getWorkspace().getRoot().getFile(jsonPath);

    final IPath location = jsonFileRc.getLocation();
    final IProject project = currentCfgDescription.getProjectDescription().getProject();
    if (location != null) {
      final File jsonFile = location.toFile();
      if (jsonFile.exists()) {
        // file exists on disk...
        // get cached timestamp
        tsCached = (Long) project.getSessionProperty(JSON_PARSED_PROP);
        final long tsJsonModified = jsonFile.lastModified();
        if (tsCached == null || tsCached.longValue() < tsJsonModified) {
          // must parse json file
          try {
            // parse file...
            JSON parser = new JSON();
            Reader in = new FileReader(jsonFile);
            Object parsed = parser.parse(new JSON.ReaderSource(in), false);
            if (parsed instanceof Object[]) {
              for (Object o : (Object[]) parsed) {
                if (o instanceof Map) {
                  processEntry((Map<?, ?>) o, jsonPath);
                } else {
                  // expected Map object, skipping entry.toString()
                  log.log(new Status(IStatus.WARNING, CMakePlugin.PLUGIN_ID, "'" + project.getName() + "' "
                      + "File format error: " + jsonPath.toString() + ": unexpected entry '" + o + "', skipped", null));
                }
              }
              // store timestamp as resource property
              project.setSessionProperty(JSON_PARSED_PROP, tsJsonModified);
//              System.out.println("stored cached compile_commands");
            } else {
              // file format error
              log.log(new Status(IStatus.WARNING, CMakePlugin.PLUGIN_ID, "'" + project.getName() + "' "
                  + "File format error: " + jsonPath.toString() + " does not seem to be JSON", null));
            }
          } catch (IOException ex) {
            log.log(new Status(IStatus.WARNING, CMakePlugin.PLUGIN_ID,
                "'" + project.getName() + "' " + "Failed to read file " + jsonFile, ex));
          }
        }
        return;
      }
    }
    // no json file was produced in the build
    log.log(
        new Status(IStatus.WARNING, CMakePlugin.PLUGIN_ID, "'" + jsonPath + "' " + " not created in the build", null));
  }

  /**
   * Processes an entry from a {@code compile_commands.json} file and stores a
   * {@link ICLanguageSettingEntry} for the file found in the specified map.
   *
   * @param sourceFileInfo
   *        a Map of type Map<String,String>
   * @param jsonPath
   *        the JSON file being parsed (for logging only)
   */
  private void processEntry(Map<?, ?> sourceFileInfo, IPath jsonPath) {
    if (sourceFileInfo.containsKey("file") && sourceFileInfo.containsKey("command")) {
      final String file = sourceFileInfo.get("file").toString();
      if (file != null && !file.isEmpty()) {
        final String cmdLine = sourceFileInfo.get("command").toString();
        if (cmdLine != null && !cmdLine.isEmpty()) {
          final File path = new File(file);
          final IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(path.toURI());
          if (files.length > 0) {
            if (!processCommandLineAnyDetector(files[0], cmdLine)) {
              log.log(new Status(IStatus.WARNING, CMakePlugin.PLUGIN_ID,
                  jsonPath.toString() + ": No parser for command '" + cmdLine + "', skipped", null));
            }
          }
          return;
        }
      }
    }
    // unrecognized entry, skipping
    log.log(new Status(IStatus.WARNING, CMakePlugin.PLUGIN_ID,
        "File format error: " + jsonPath.toString() + ": 'file' or 'command' missing in JSON object, skipped", null));
  }

  /**
   * Processes the command-line of an entry from a {@code compile_commands.json}
   * file by trying each parser in {@link #currentCmdlineParsers} and stores a
   * {@link ICLanguageSettingEntry} for the file found in the specified map.
   *
   * @param sourceFile
   *        the source file resource corresponding to the source file being
   *        processed by the tool
   * @param line
   *        the command line to process
   * @return {@code true} if a parser for the command-line could be found,
   *         {@code false} if no parser could be found (nothing was processed)
   */
  private boolean processCommandLineAnyDetector(IFile sourceFile, String line) {
    // try last known matching detector first...
    if (preferredCmdlineParser != null && processCommandLine(preferredCmdlineParser, sourceFile, line)) {
      return true; // could process command line
    }
    // try each tool..
    for (Entry<Matcher, IToolCommandlineParser> entry : currentCmdlineParsers.entrySet()) {
      if (processCommandLine(entry, sourceFile, line)) {
        // found a matching command-line parser
        preferredCmdlineParser = entry;
        return true; // could process command line
      }
    }
    //    System.out.println(line);
    return false; // no matching parser found
  }

  /**
   * Processes the command-line of an entry from a {@code compile_commands.json}
   * file by trying the specified detector and stores a
   * {@link ICLanguageSettingEntry} for the file found in the specified map.
   *
   * @param detectorInfo
   *        the tool detector and its tool option parsers
   * @param sourceFile
   *        the source file resource corresponding to the source file being
   *        processed by the tool
   * @param line
   *        the command line to process
   * @return {@code true} if the specified detector matches the tool given on
   *         the specified command-line, otherwise {@code false} (specified
   *         command line was processed)
   */
  private boolean processCommandLine(Entry<Matcher, IToolCommandlineParser> detectorInfo, IFile sourceFile,
      String line) {
    final Matcher cmdDetector = detectorInfo.getKey();
    cmdDetector.reset(line);
    if (cmdDetector.lookingAt()) {
      // found a matching command-line parser
      String args = line.substring(cmdDetector.end());
      args = ToolCommandlineParser.trimLeadingWS(args);
      final IToolCommandlineParser cmdlineParser = detectorInfo.getValue();
      final List<ICLanguageSettingEntry> entries = cmdlineParser.processArgs(args);
      // attach settings to sourceFile resource...
      if (entries != null && entries.size() > 0) {
        //TODO Settings speichern per Project/Configuration (ICConfigurationDescription?)
        // super speichert die global (ohne project zu berücksichtigen): NEE, tut es nicht.
        super.setSettingEntries(currentCfgDescription, sourceFile, cmdlineParser.getLanguageId(), entries);
      }
      return true; // skip other detectors
    }
    return false; // no matching detectors found
  }

  /*-
   * interface ICBuildOutputParser
   */
  @Override
  public void startup(ICConfigurationDescription cfgDescription, IWorkingDirectoryTracker cwdTracker)
      throws CoreException {
    currentCfgDescription = cfgDescription;
  }

  /**
   * Invoked for each line in the build output.
   */
  // interface ICBuildOutputParser
  @Override
  public boolean processLine(String line) {
    // nothing to do, we parse later...
    return false;
  }

  /*-
   * interface ICBuildOutputParser
   */
  @Override
  public void shutdown() {
    try {
      tryParseJson();
    } catch (CoreException ex) {
      log.log(new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID, "tryParseJson()", ex));
    }
    // release resources for garbage collector
    currentCfgDescription = null;
  }

  @Override
  public CompileCommandsJsonParser clone() throws CloneNotSupportedException {
    return (CompileCommandsJsonParser) super.clone();
  }

  ////////////////////////////////////////////////////////////////////
  // inner classes
  ////////////////////////////////////////////////////////////////////
}
