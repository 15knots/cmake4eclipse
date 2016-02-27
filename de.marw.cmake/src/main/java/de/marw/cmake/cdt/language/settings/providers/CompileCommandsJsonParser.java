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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
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

  /**
   * name of the session property attached to project resources. The property
   * caches the time-stamp of the last time the file
   * {@code compile_commands.json} was parsed.
   */
  private static final QualifiedName JSON_PARSED_PROP = new QualifiedName(CMakePlugin.PLUGIN_ID,
      "TS:compile_commands.json");

  private ICConfigurationDescription currentCfgDescription;

  /**
   * parsers for each tool of interest that takes part in the current build. The
   * Matcher detects whether a command line is an invocation of the tool.
   */
  private HashMap<Matcher, IToolCommandlineParser> currentCmdlineParsers;

  public CompileCommandsJsonParser() {
    currentCmdlineParsers = new HashMap<Matcher, IToolCommandlineParser>(4, 1.0f);

    /** Names of known tools along with their command line argument parsers */
    final Map<String, IToolCommandlineParser> knownCmdParsers = new HashMap<String, IToolCommandlineParser>(4, 1.0f);

    final IToolArgumentParser[] posix_cc_args = { new ToolArgumentParsers.MacroDefine_C_POSIX(),
        new ToolArgumentParsers.MacroUndefine_C_POSIX(), new ToolArgumentParsers.IncludePath_C_POSIX(),
        // not defined by POSIX, but does not harm..
        new ToolArgumentParsers.SystemIncludePath_C(), };
    // POSIX compatible C compilers...
    final ToolCommandlineParser gcc = new ToolCommandlineParser("org.eclipse.cdt.core.gcc", posix_cc_args);
    knownCmdParsers.put("cc", gcc);
    knownCmdParsers.put("gcc", gcc);
    knownCmdParsers.put("clang", gcc);
    // POSIX compatible C++ compilers...
    final ToolCommandlineParser cpp = new ToolCommandlineParser("org.eclipse.cdt.core.g++", posix_cc_args);
    knownCmdParsers.put("c++", cpp);
    knownCmdParsers.put("clang++", cpp);
    // TODO add ms compiler and intel compilers

    final String REGEX_CMD_HEAD = "^(.*?" + Pattern.quote(File.separator) + ")(";
    final String REGEX_CMD_TAIL = ")";
    for (Entry<String, IToolCommandlineParser> entry : knownCmdParsers.entrySet()) {
      // construct a matcher that detects the tool name
      // 'cc' -> matches
      // '/bin/cc' -> matches
      // '/usr/bin/cc' -> matches
      // 'C:\program files\mingw\bin\cc' -> matches
      Matcher cmdDetector = Pattern.compile(REGEX_CMD_HEAD + Pattern.quote(entry.getKey()) + REGEX_CMD_TAIL)
          .matcher("");
      currentCmdlineParsers.put(cmdDetector, entry.getValue());
    }
  }

  private boolean processCommandLine(IFile sourceFile, String line) {
    // try each tool..
    for (Entry<Matcher, IToolCommandlineParser> entry : currentCmdlineParsers.entrySet()) {
      final Matcher cmdDetector = entry.getKey();
      cmdDetector.reset(line);
      if (cmdDetector.lookingAt()) {
        // found a matching command-line parser
        String args = line.substring(cmdDetector.end());
        args = ToolCommandlineParser.trimLeadingWS(args);
        final IToolCommandlineParser cmdlineParser = entry.getValue();
        final List<ICLanguageSettingEntry> entries = cmdlineParser.processArgs(args);
        // attach settings to sourceFile resource...
        if (entries != null && entries.size() > 0) {
          super.setSettingEntries(currentCfgDescription, sourceFile, cmdlineParser.getLanguageId(), entries);
        }
        return true; // skip other parsers
      }
    }
//    System.out.println(line);
    return false;
  }

  /**
   * Parses the content of the 'compile_commands.json' file corresponding to the
   * specified configuration, if timestamps differ.
   *
   * @param cfgd
   *        configuration
   * @return the parsed content of the file
   * @throws CoreException
   */
  private Long tryParseJson(ICConfigurationDescription cfgd) throws CoreException {

    /** cached file modification timestamp of last parse */
    Long tsCached = null;

    // If getBuilderCWD() returns a workspace relative path, it is garbled.
    // If garbled, make sure de.marw.cdt.cmake.core.internal.BuildscriptGenerator.getBuildWorkingDir()
    // returns a full, absolute path relative to the workspace.
    final IPath builderCWD = cfgd.getBuildSetting().getBuilderCWD();

    IPath jsonPath = builderCWD.append("compile_commands.json");
    final IFile jsonFileRc = ResourcesPlugin.getWorkspace().getRoot().getFile(jsonPath);

    IPath location = jsonFileRc.getLocation();
    if (location != null) {
      final File jsonFile = location.toFile();
      if (jsonFile.exists()) {
        // file exists on disk...
        IProject project = cfgd.getProjectDescription().getProject();
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
                  processEntry((Map<?, ?>) o, cfgd);
                } else {
                  // TODO expected Map object, skipping entry.toString()
                }
              }
              // store timestamp as resource property
              project.setSessionProperty(JSON_PARSED_PROP, tsJsonModified);
//              System.out.println("stored cached compile_commands");
            } else {
              // TODO file format error
            }
          } catch (IOException ex) {
            throw new CoreException(
                new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID, "Failed to read file " + jsonFile, ex));
          }
        }
      }
    } else {
      // no json file was produced in the build
      // TODO
    }
    return tsCached;
  }

  /**
   * Processes an entry from a {@code compile_commands.json} file and stores a
   * {@link ICLanguageSettingEntry} for the file found in the specified map.
   *
   * @param sourceFileInfo
   *        a Map of type Map<String,String>
   * @param cfgd
   *        configuration description for the parser.
   */
  private void processEntry(Map<?, ?> sourceFileInfo, ICConfigurationDescription cfgd) {
    if (sourceFileInfo.containsKey("file") && sourceFileInfo.containsKey("command")) {
      final String file = sourceFileInfo.get("file").toString();
      if (file != null && !file.isEmpty()) {
        final String cmdLine = sourceFileInfo.get("command").toString();
        if (cmdLine != null && !cmdLine.isEmpty()) {
          final File path = new File(file);
          final IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(path.toURI());
          if (files.length > 0) {
            processCommandLine(files[0], cmdLine);
          }
          return;
        }
      }
    }
    // TODO unrecognized entry, skipping
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
      tryParseJson(currentCfgDescription);
    } catch (CoreException ex) {
      // TODO Auto-generated catch block
      ex.printStackTrace();
    }
    // release resources for garbage collector
    currentCfgDescription = null;
  }

  @Override
  public List<ICLanguageSettingEntry> getSettingEntries(ICConfigurationDescription cfgDescription, IResource rc,
      String languageId) {
//    CDataUtil.createCMacroEntry()
//    LanguageSettingsStorage.getPooledList(List)
    // TODO Auto-generated function stub
    return super.getSettingEntries(cfgDescription, rc, languageId);
  }

  @Override
  public CompileCommandsJsonParser clone() throws CloneNotSupportedException {
    return (CompileCommandsJsonParser) super.clone();
  }

  ////////////////////////////////////////////////////////////////////
  // inner classes
  ////////////////////////////////////////////////////////////////////
}
