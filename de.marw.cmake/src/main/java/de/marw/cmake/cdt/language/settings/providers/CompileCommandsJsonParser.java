/*******************************************************************************
 * Copyright (c) 2016-2017 Martin Weber.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.build.core.scannerconfig.ScannerConfigNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.language.settings.providers.ICBuildOutputParser;
import org.eclipse.cdt.core.language.settings.providers.ICListenerAgent;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsEditableProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.language.settings.providers.IWorkingDirectoryTracker;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsSerializableProvider;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsStorage;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.ui.PreferenceConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jetty.util.ajax.JSON;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import de.marw.cmake.CMakePlugin;

/**
 * A ILanguageSettingsProvider that parses the file 'compile_commands.json'
 * produced by cmake when option {@code -DCMAKE_EXPORT_COMPILE_COMMANDS=ON} is
 * given.<br>
 * NOTE: This class misuses interface ICBuildOutputParser to detect when a build
 * did finish.<br>
 * NOTE: This class misuses interface ICListenerAgent to populate the
 * {@link #getSettingEntries setting entries} on startup.
 *
 * @author Martin Weber
 */
public class CompileCommandsJsonParser extends LanguageSettingsSerializableProvider
    implements ILanguageSettingsEditableProvider, ICListenerAgent, ICBuildOutputParser, Cloneable {
  private static final ILog log = CMakePlugin.getDefault().getLog();

  /**
   * default regex string used for version pattern matching.
   *
   * @see #isVersionPatternEnabled()
   */
  private static final String DEFALT_VERSION_PATTERN = "-?\\d+(\\.\\d+)*";
  /** storage key for version pattern */
  private static final String ATTR_PATTERN = "vPattern";
  /** storage key for version pattern enabled */
  private static final String ATTR_PATTERN_ENABLED = "vPatternEnabled";

  private static final String WORKBENCH_WILL_NOT_KNOW_ALL_MSG = "Your workbench will not know all include paths and preprocessor defines.";

  private static final String MARKER_ID = CMakePlugin.PLUGIN_ID + ".CompileCommandsJsonParserMarker";
  /**
   * Storage to keep settings entries
   */
  private PerConfigLanguageSettingsStorage storage = new PerConfigLanguageSettingsStorage();

  private ICConfigurationDescription currentCfgDescription;

  /**
   * tool detectors and their tool option parsers for each tool of interest that
   * takes part in the current build. The Matcher detects whether a command line
   * is an invocation of the tool.
   */
  private static final List<ParserDetector> parserDetectors = new ArrayList<>(22);
  /**
   * same as {@link #parserDetectors}, but for windows where cmake places a
   * backslash in the command path. Unused (<code>null</code>), when running
   * under *nix.
   */
  private static final List<ParserDetector> parserDetectorsWin32 = new ArrayList<>(16);

  static {
    /** Names of known tools along with their command line argument parsers */
    final IToolArgumentParser[] posix_cc_args = { new ToolArgumentParsers.IncludePath_C_POSIX(),
        new ToolArgumentParsers.MacroDefine_C_POSIX(), new ToolArgumentParsers.MacroUndefine_C_POSIX(),
        // not defined by POSIX, but does not harm..
        new ToolArgumentParsers.SystemIncludePath_C() };
    // POSIX compatible C compilers =================================
    final ToolCommandlineParser cc = new ToolCommandlineParser("org.eclipse.cdt.core.gcc", posix_cc_args);
    parserDetectors.add( new ParserDetector("cc", cc));
    parserDetectors.add( new ParserDetectorExt("cc","exe", cc));
    parserDetectors.add( new ParserDetector("gcc", cc));
    parserDetectors.add( new ParserDetectorExt("cc","exe", cc));
    parserDetectors.add( new ParserDetector("clang", cc));
    parserDetectors.add( new ParserDetectorExt("clang","exe", cc));

    // POSIX compatible C++ compilers ===============================
    final ToolCommandlineParser cxx = new ToolCommandlineParser("org.eclipse.cdt.core.g++", posix_cc_args);
    parserDetectors.add( new ParserDetector("c\\+\\+", cxx));
    parserDetectors.add( new ParserDetectorExt("c\\+\\+","exe", cxx));
    parserDetectors.add( new ParserDetector("g\\+\\+", cxx));
    parserDetectors.add( new ParserDetectorExt("g\\+\\+","exe", cxx));
    parserDetectors.add( new ParserDetector("clang\\+\\+", cxx));
    parserDetectors.add( new ParserDetectorExt("clang\\+\\+","exe", cxx));

    // GNU C and C++ cross compilers, e.g. arm-none-eabi-gcc.exe ====
    parserDetectors.add( new ParserDetector(".+-gcc", cc));
    parserDetectors.add( new ParserDetectorExt(".+-gcc","exe", cc));
    parserDetectors.add( new ParserDetector(".+-g\\+\\+", cxx));
    parserDetectors.add( new ParserDetectorExt(".+-g\\+\\+","exe", cxx));

    // ms C + C++ compiler ==========================================
    final IToolArgumentParser[] cl_cc_args = { new ToolArgumentParsers.IncludePath_C_CL(),
        new ToolArgumentParsers.MacroDefine_C_CL(), new ToolArgumentParsers.MacroUndefine_C_CL() };
    final ToolCommandlineParser cl = new ToolCommandlineParser("org.eclipse.cdt.core.gcc", cl_cc_args);
    parserDetectors.add(new ParserDetectorExt("cl", "exe", cl));

    // Intel C compilers ============================================
    final ToolCommandlineParser icc = new ToolCommandlineParser("org.eclipse.cdt.core.gcc", posix_cc_args);
    final ToolCommandlineParser icpc = new ToolCommandlineParser("org.eclipse.cdt.core.g++", posix_cc_args);

    // Linux & OS X, EDG
    parserDetectors.add( new ParserDetector("icc", icc));
    // OS X, clang
    parserDetectors.add( new ParserDetector("icl", icc));
    // Intel C++ compiler
    // Linux & OS X, EDG
    parserDetectors.add( new ParserDetector("icpc", icpc));
    // OS X, clang
    parserDetectors.add( new ParserDetector("icl\\+\\+", icpc));
    // Windows C + C++, EDG
    parserDetectors.add(new ParserDetectorExt("icl", "exe", cl));

    // add detectors for windows NTFS
    for (ParserDetector pd : parserDetectors) {
      if (pd instanceof ParserDetectorExt) {
        ParserDetectorExt pde = (ParserDetectorExt) pd;
        parserDetectorsWin32.add(new ParserDetectorExt(pde.basenameRegex, true, pde.extensionRegex, pde.parser));
      } else {
        parserDetectorsWin32.add(new ParserDetector(pd.basenameRegex, true, pd.parser));
      }
    }
  }

  /**
   * last known working tool detector and its tool option parsers or
   * {@code null}, if unknown (to speed up parsing)
   */
  private DetectorWithMethod lastDetector;

  public CompileCommandsJsonParser() {
  }

  /**
   * Gets whether the parser will also try to detect compiler command that have
   * a trailing version-string in their name. If enabled, this parser will also
   * try to match for example {@code gcc-4.6} or {@code gcc-4.6.exe} if none of
   * the other patterns matched.
   *
   * @return <code>true</code> version pattern matching in command names is
   *         enabled, otherwise <code>false</code>
   */
  public boolean isVersionPatternEnabled() {
    return getPropertyBool(ATTR_PATTERN_ENABLED);
  }

  /**
   * Sets whether version pattern matching is performed.
   *
   * @see #isVersionPatternEnabled()
   */
  public void setVersionPatternEnabled(boolean enabled) {
    setPropertyBool(ATTR_PATTERN_ENABLED, enabled);
  }

  /**
   * Gets the regex pattern string used for version pattern matching.
   *
   * @see #isVersionPatternEnabled()
   */
  public String getVersionPattern() {
    String val = properties.get(ATTR_PATTERN);
    if (val == null || val.isEmpty()) {
      // provide a default pattern
      val = DEFALT_VERSION_PATTERN;
    }
    return val;
  }

  /**
   * Sets the regex pattern string used for version pattern matching.
   *
   * @see #isVersionPatternEnabled()
   */
  public void setVersionPattern(String versionPattern) {
    if (versionPattern == null || versionPattern.isEmpty() || versionPattern.equals(DEFALT_VERSION_PATTERN)) {
      // do not store default pattern
      properties.remove(ATTR_PATTERN);
    } else {
      setProperty(ATTR_PATTERN, versionPattern);
    }
  }

  /**
   * {@inheritDoc} <br>
   * Note that this list is <b>unmodifiable</b>.<br>
   * Note also that <b>you can compare these lists with simple equality operator
   * ==</b>, as the lists themselves are backed by WeakHashSet<List
   * <ICLanguageSettingEntry>> where identical copies (deep comparison is used)
   * are replaced with the same one instance.
   */
  @Override
  public List<ICLanguageSettingEntry> getSettingEntries(ICConfigurationDescription cfgDescription, IResource rc,
      String languageId) {
    if (cfgDescription == null || rc == null) {
      // speed up, we do not provide global (workspace) lang settings..
      return null;
    }
    return storage.getSettingEntries(cfgDescription, rc, languageId);
  }

  /**
   * Parses the content of the 'compile_commands.json' file corresponding to the
   * specified configuration, if timestamps differ.
   *
   * @param initializingWorkbench
   *          {@code true} if the workbench is starting up. If {@code true},
   *          this method will not trigger UI update to show newly detected
   *          include paths nor will it complain if a "compile_commands.json"
   *          file does not exist.
   * @throws CoreException
   */
  private void tryParseJson(boolean initializingWorkbench) throws CoreException {

    // If getBuilderCWD() returns a workspace relative path, it is garbled.
    // If garbled, make sure
    // de.marw.cdt.cmake.core.internal.BuildscriptGenerator.getBuildWorkingDir()
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
        final long tsJsonModified = jsonFile.lastModified();

        final TimestampedLanguageSettingsStorage store = storage.getSettingsForConfig(currentCfgDescription);
        final ProjectLanguageSettingEntries projectEntries = new ProjectLanguageSettingEntries();

        if (store.lastModified < tsJsonModified) {
          // must parse json file...
          project.deleteMarkers(MARKER_ID, false, IResource.DEPTH_INFINITE);
          try {
            // parse file...
            JSON parser = new JSON();
            Reader in = new FileReader(jsonFile);
            Object parsed = parser.parse(new JSON.ReaderSource(in), false);
            if (parsed instanceof Object[]) {
              for (Object o : (Object[]) parsed) {
                if (o instanceof Map) {
                  processJsonEntry(store, projectEntries, (Map<?, ?>) o, jsonFileRc);
                } else {
                  // expected Map object, skipping entry.toString()
                  final String msg = "File format error: unexpected entry '" + o + "'. "
                      + WORKBENCH_WILL_NOT_KNOW_ALL_MSG;
                  createMarker(jsonFileRc, msg);
                }
              }
              /*
               * compile_commands.json holds entries per-file only and does not
               * contain per-project entries. For include dirs, add these
               * entries to the project resource to make them show up in the UI
               * in the includes folder...
               */
              store.addProjectLanguageSettingEntries(project, projectEntries);
              // store time-stamp
              store.lastModified = tsJsonModified;
              // System.out.println("stored cached compile_commands");

              // trigger UI update to show newly detected include paths
              // must run in UI thread
              Display.getDefault().asyncExec(new Runnable() {

                public void run() {
                  IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                  IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
                  IWorkbenchPartReference refs[] = activePage.getViewReferences();
                  for (IWorkbenchPartReference ref : refs) {
                    IWorkbenchPart part = ref.getPart(false);
                    if (part instanceof IPropertyChangeListener)
                      ((IPropertyChangeListener) part).propertyChange(
                          new PropertyChangeEvent(project, PreferenceConstants.PREF_SHOW_CU_CHILDREN, null, null));
                  }
                }
              });
            } else {
              // file format error
              final String msg = "File does not seem to be in JSON format. " + WORKBENCH_WILL_NOT_KNOW_ALL_MSG;
              createMarker(jsonFileRc, msg);
            }
          } catch (IOException ex) {
            final String msg = "Failed to read file " + jsonFile + ". " + WORKBENCH_WILL_NOT_KNOW_ALL_MSG;
            createMarker(jsonFileRc, msg);
          }
        }
        return;
      }
    }
    if (!initializingWorkbench) {
      // no json file was produced in the build
      final String msg = "File '" + jsonPath + "' was not created in the build. " + WORKBENCH_WILL_NOT_KNOW_ALL_MSG;
      createMarker(jsonFileRc, msg);
    }
  }

  /**
   * Processes an entry from a {@code compile_commands.json} file and stores a
   * {@link ICLanguageSettingEntry} for the file given the specified map.
   *
   * @param storage
   *          where to store language settings
   * @param projectEntries
   *          where to store project wide setting entries
   * @param sourceFileInfo
   *          a Map of type Map<String,String>
   * @param jsonFile
   *          the JSON file being parsed (for logging only)
   * @throws CoreException
   *           if marker creation failed
   */
  private void processJsonEntry(TimestampedLanguageSettingsStorage storage,
      ProjectLanguageSettingEntries projectEntries, Map<?, ?> sourceFileInfo, IFile jsonFile) throws CoreException {
    if (sourceFileInfo.containsKey("file") && sourceFileInfo.containsKey("command")) {
      final String file = sourceFileInfo.get("file").toString();
      if (file != null && !file.isEmpty()) {
        final String cmdLine = sourceFileInfo.get("command").toString();
        if (cmdLine != null && !cmdLine.isEmpty()) {
          final File path = new File(file);
          final IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(path.toURI());
          if (files.length > 0) {
            ParserDetectionResult pdr= fastDetermineDetector(cmdLine);
            if (pdr != null) {
              // found a matching command-line parser
              processCommandLine(storage, projectEntries, pdr.detectorWMethod.detector.parser, files[0],
                  pdr.getReducedCommandLine());
            } else {
              // no matching parser found
              String message = "No parser for command '" + cmdLine + "'. " + WORKBENCH_WILL_NOT_KNOW_ALL_MSG;
              createMarker(jsonFile, message);
            }
          }
          return;
        }
      }
    }
    // unrecognized entry, skipping
    final String msg = "File format error: " + ": 'file' or 'command' missing in JSON object. "
        + WORKBENCH_WILL_NOT_KNOW_ALL_MSG;
    createMarker(jsonFile, msg);
  }

  private static void createMarker(IFile file, String message) throws CoreException {
    IMarker marker;
    try {
      marker = file.createMarker(MARKER_ID);
    } catch (CoreException ex) {
      // resource is not (yet) known by the workbench
      file.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
      try {
        marker = file.createMarker(MARKER_ID);
      } catch (CoreException ex2) {
        // resource is not known by the workbench, use project instead of file
        marker = file.getProject().createMarker(MARKER_ID);
      }
    }
    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
    marker.setAttribute(IMarker.MESSAGE, message);
  }

  /**
   * Determines the parser detector that can parse the specified
   * command-line.<br>
   * Tries to be fast: That is, it tries the last known working detector first
   * and will perform expensive detection required under windows only if needed.
   *
   * @param line
   *          the command line to process
   *
   * @return {@code null} if none of the detectors matches the tool name in the
   *         specified command-line string. Otherwise, if the tool name matches,
   *         a {@code ParserDetectionResult} holding the remaining command-line
   *         string (without the portion that matched) is returned.
   */
  private ParserDetectionResult fastDetermineDetector(String line) {
    // try last known matching detector first...
    if (lastDetector != null) {
      String remaining = null;
      final ParserDetector detector = lastDetector.detector;
      switch (lastDetector.how) {
      case BASENAME:
        remaining = detector.basenameMatches(line);
        break;
      case WITH_EXTENSION:
        remaining = ((ParserDetectorExt) detector).basenameWithExtensionMatches(line);
        break;
      case WITH_VERSION:
        if (isVersionPatternEnabled()) {
          remaining = detector.basenameWithVersionMatches(line, getVersionPattern());
        }
        break;
      case WITH_VERSION_EXTENSION:
        if (isVersionPatternEnabled()) {
          remaining = ((ParserDetectorExt) detector).basenameWithVersionAndExtensionMatches(line, getVersionPattern());
        }
        break;
      default:
        break;
      }
      if (remaining != null) {
        return new ParserDetectionResult(lastDetector, remaining);
      } else {
        lastDetector = null; // invalidate last working detector
      }
    }

    // no working detectorfound, determine a new one...
    ParserDetectionResult result = determineDetector(line, File.separatorChar == '\\');
    if (result != null) {
      // cache last working detector
      lastDetector = result.detectorWMethod;
    }
    return result;
  }

  /**
   * Determines the parser detector that can parse the specified
   * command-line.
   *
   * @param line
   *          the command line to process
   * @param tryWindowsDectors
   *          whether to also try the detectors for ms windows OS
   *
   * @return {@code null} if none of the detectors matches the tool name in the
   *         specified command-line string. Otherwise, if the tool name matches,
   *         a {@code ParserDetectionResult} holding the remaining command-line
   *         string (without the portion that matched) is returned.
   */
  // has package scope for testing purposes
  ParserDetectionResult determineDetector(String line, boolean tryWindowsDectors) {
    ParserDetectionResult result;
    // try default detectors
    result = determineDetector(line, parserDetectors);
    if (result == null && tryWindowsDectors) {
      // try with backslash as file separator on windows
      result = determineDetector(line, parserDetectorsWin32);
      if (result == null) {
        // try workaround for windows short file names
        final String shortPathExpanded = expandShortFileName(line);
        result = determineDetector(shortPathExpanded, parserDetectorsWin32);
        if (result == null) {
          // try for cmake from mingw suite with short file names under windows
          result = determineDetector(shortPathExpanded, parserDetectors);
        }
      }
    }
    return result;
  }

  /**
   * Determines a C-compiler-command line parser that is able to parse the
   * relevant arguments in the specified command line.
   *
   * @param commandLine
   *          the command line to process
   * @param detectors
   *          the detectors to try
   *
   * @return {@code null} if none of the detectors matches the tool name in the
   *         specified command-line string. Otherwise, if the tool name matches,
   *         a {@code ParserDetectionResult} holding the remaining command-line
   *         string (without the portion that matched) is returned.
   */
  private ParserDetectionResult determineDetector(String commandLine, List<ParserDetector> detectors) {
    String remaining;
    // try basenames
    for (ParserDetector pd : detectors) {
      if ((remaining = pd.basenameMatches(commandLine)) != null) {
        return new ParserDetectionResult(new DetectorWithMethod(pd, DetectorWithMethod.DetectionMethod.BASENAME),
            remaining);
      }
    }
    if (isVersionPatternEnabled()) {
      String versionPattern = getVersionPattern();
      // try with version pattern
      for (ParserDetector pd : detectors) {
        if ((remaining = pd.basenameWithVersionMatches(commandLine, versionPattern)) != null) {
          return new ParserDetectionResult(new DetectorWithMethod(pd, DetectorWithMethod.DetectionMethod.WITH_VERSION),
              remaining);
        }
      }
    }
    // try with extension
    for (ParserDetector pd : detectors) {
      if (pd instanceof ParserDetectorExt
          && (remaining = ((ParserDetectorExt) pd).basenameWithExtensionMatches(commandLine)) != null) {
        return new ParserDetectionResult(new DetectorWithMethod(pd, DetectorWithMethod.DetectionMethod.WITH_EXTENSION),
            remaining);
      }
    }
    if (isVersionPatternEnabled()) {
      // try with extension and version
      String versionPattern = getVersionPattern();
      for (ParserDetector pd : detectors) {
        if (pd instanceof ParserDetectorExt && (remaining = ((ParserDetectorExt) pd)
            .basenameWithVersionAndExtensionMatches(commandLine, versionPattern)) != null) {
          return new ParserDetectionResult(
              new DetectorWithMethod(pd, DetectorWithMethod.DetectionMethod.WITH_VERSION_EXTENSION), remaining);
        }
      }
    }

    return null;
  }

  /**
   * Tries to convert windows short file names for the compiler executable (like
   * <code>AVR-G_~1.EXE</code>) into their long representation. This is a
   * workaround for a
   * <a href="https://gitlab.kitware.com/cmake/cmake/issues/16138">bug in CMake
   * under windows</a>.<br>
   * See <a href="https://github.com/15knots/cmake4eclipse/issues/31">issue #31
   */
  private String expandShortFileName(String commandLine) {
    String command;
    // split at first space character
    StringBuilder commandLine2 = new StringBuilder();
    int idx = commandLine.indexOf(' ');
    if (idx != -1) {
      command = commandLine.substring(0, idx);
      commandLine2.append(commandLine.substring(idx));
    } else {
      command = commandLine;
    }
    // convert to long file name and retry lookup
    try {
      command = new File(command).getCanonicalPath();
      commandLine2.insert(0, command);
      return commandLine2.toString();
    } catch (IOException e) {
      log.log(new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID, "CompileCommandsJsonParser#expandShortFileName()", e));
    }
    return null;
  }

  /**
   * Processes the command-line of an entry from a {@code compile_commands.json}
   * file by trying the specified detector and stores a
   * {@link ICLanguageSettingEntry} for the file found in the specified map.
   *
   * @param storage
   *          where to store language settings
   * @param projectEntries
   *          where to store project wide setting entries
   * @param cmdlineParser
   *          the tool detector and its tool option parsers
   * @param sourceFile
   *          the source file resource corresponding to the source file being
   *          processed by the tool
   * @param line
   *          the command line to process
   */
  private void processCommandLine(TimestampedLanguageSettingsStorage storage,
      ProjectLanguageSettingEntries projectEntries, IToolCommandlineParser cmdlineParser, IFile sourceFile,
      String line) {
    line = ToolCommandlineParser.trimLeadingWS(line);
    final List<ICLanguageSettingEntry> entries = cmdlineParser.processArgs(line);
    // attach settings to sourceFile resource...
    if (entries != null && entries.size() > 0) {
      /*
       * compile_commands.json holds entries per-file only and does not contain
       * per-project entries. For include dirs, add these entries to the project
       * resource to make them show up in the UI in the includes folder...
       */
      for (Iterator<ICLanguageSettingEntry> iter = entries.iterator(); iter.hasNext();) {
        ICLanguageSettingEntry entry = iter.next();
        if (entry.getKind() == ICSettingEntry.INCLUDE_PATH) {
          projectEntries.addSettingEntry(cmdlineParser.getLanguageId(), entry);
        }
      }
      storage.setSettingEntries(sourceFile, cmdlineParser.getLanguageId(), entries);
    }
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
      tryParseJson(false);
    } catch (CoreException ex) {
      log.log(new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID, "shutdown()", ex));
    }
    // release resources for garbage collector
    currentCfgDescription = null;
  }

  @Override
  public CompileCommandsJsonParser clone() throws CloneNotSupportedException {
    return (CompileCommandsJsonParser) super.clone();
  }

  @Override
  public CompileCommandsJsonParser cloneShallow() throws CloneNotSupportedException {
    return (CompileCommandsJsonParser) super.cloneShallow();
  }

  /**
   * Overridden to misuse this to populate the {@link #getSettingEntries setting
   * entries} on startup.<br>
   * {@inheritDoc}
   */
  @Override
  public void registerListener(ICConfigurationDescription cfgDescription) {
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    IProject[] projects = workspaceRoot.getProjects();
    CCorePlugin ccp = CCorePlugin.getDefault();
    // parse JSOn file for any opened project that has a ScannerConfigNature...
    for (IProject project : projects) {
      try {
        if (project.isOpen() && project.hasNature(ScannerConfigNature.NATURE_ID)) {
          ICProjectDescription projectDescription = ccp.getProjectDescription(project, false);
          if (projectDescription != null) {
            ICConfigurationDescription activeConfiguration = projectDescription.getActiveConfiguration();
            if (activeConfiguration instanceof ILanguageSettingsProvidersKeeper) {
              final List<ILanguageSettingsProvider> lsps = ((ILanguageSettingsProvidersKeeper) activeConfiguration)
                  .getLanguageSettingProviders();
              for (ILanguageSettingsProvider lsp : lsps) {
                if ("de.marw.cmake.cdt.language.settings.providers.CompileCommandsJsonParser".equals(lsp.getId())) {
                  currentCfgDescription = activeConfiguration;
                  tryParseJson(true);
                  break;
                }
              }
            }
          }
        }
      } catch (CoreException ex) {
        log.log(new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID, "registerListener()", ex));
      }
    }
    // release resources for garbage collector
    currentCfgDescription = null;
  }

  /*-
   * @see org.eclipse.cdt.core.language.settings.providers.ICListenerAgent#unregisterListener()
   */
  @Override
  public void unregisterListener() {
  }

  ////////////////////////////////////////////////////////////////////
  // inner classes
  ////////////////////////////////////////////////////////////////////
  private static class TimestampedLanguageSettingsStorage extends LanguageSettingsStorage {
    /** cached file modification time-stamp of last parse */
    long lastModified = 0;

    /**
     * Sets language settings entries for this storages.
     *
     * @param rc
     *          resource such as file or folder. If {@code null} the entries are
     *          considered to be being defined as default entries for resources.
     * @param languageId
     *          language id. Must not be {@code null}
     * @param entries
     *          language settings entries to set.
     */
    public void setSettingEntries(IResource rc, String languageId, List<ICLanguageSettingEntry> entries) {
      final String rcPath = rc != null ? rc.toString() : null;
      // System.out.println("#> setSettingEntries( " + rc + ", " + languageId +
      // ")");
      // System.out.println("\t" + entries);
      super.setSettingEntries(rcPath, languageId, entries);
    }

    public void addProjectLanguageSettingEntries(IProject project, ProjectLanguageSettingEntries projectEntries) {
      for (Entry<String, Set<ICLanguageSettingEntry>> entry : projectEntries.languages.entrySet()) {
        List<ICLanguageSettingEntry> lses = Arrays.asList(entry.getValue().toArray(new ICLanguageSettingEntry[0]));
        setSettingEntries(project, entry.getKey(), lses);
      }
    }

    public TimestampedLanguageSettingsStorage clone() throws CloneNotSupportedException {
      TimestampedLanguageSettingsStorage cloned = (TimestampedLanguageSettingsStorage) super.clone();
      cloned.lastModified = this.lastModified;
      return cloned;
    }
  } // TimestampedLanguageSettingsStorage

  private static class PerConfigLanguageSettingsStorage implements Cloneable {
    /**
     * Storage to keep settings entries. Key is
     * {@link ICConfigurationDescription#getId()}
     */
    private Map<String, TimestampedLanguageSettingsStorage> storages = new WeakHashMap<>();

    public List<ICLanguageSettingEntry> getSettingEntries(ICConfigurationDescription cfgDescription, IResource rc,
        String languageId) {
      // System.out.println("#< getSettingEntries( " + cfgDescription + ", " +
      // rc + ", " + languageId + ")\t");
      final LanguageSettingsStorage store = storages.get(cfgDescription.getId());
      if (store != null) {
        final String rcPath = rc.toString();
        List<ICLanguageSettingEntry> entries = store.getSettingEntries(rcPath, languageId);
        if (entries == null && languageId != null) {
          entries = store.getSettingEntries(rcPath, null);
        }
        // System.out.println("\t" + entries);
        return entries;
      }
      return null;
    }

    /**
     * Gets the settings storage for the specified configuration. Creates a new
     * settings storage, if none exists.
     *
     * @return the storages never {@code null}
     */
    public TimestampedLanguageSettingsStorage getSettingsForConfig(ICConfigurationDescription cfgDescription) {
      TimestampedLanguageSettingsStorage store = storages.get(cfgDescription.getId());
      if (store == null) {
        store = new TimestampedLanguageSettingsStorage();
        storages.put(cfgDescription.getId(), store);
      }
      return store;
    }

    public PerConfigLanguageSettingsStorage clone() throws CloneNotSupportedException {
      PerConfigLanguageSettingsStorage cloned = new PerConfigLanguageSettingsStorage();
      for (Entry<String, TimestampedLanguageSettingsStorage> entry : storages.entrySet()) {
        cloned.storages.put(entry.getKey(), entry.getValue().clone());
      }
      return cloned;
    }
  } // PerConfigLanguageSettingsStorage

  /**
   * Gathers {@code ICLanguageSettingEntry}s that are valid for a project.
   *
   * @author Martin Weber
   */
  private static class ProjectLanguageSettingEntries {
    /**
     * Storage to keep settings entries. Key is
     * {@link ICConfigurationDescription#getId()}
     */
    private final Map<String, Set<ICLanguageSettingEntry>> languages = new HashMap<>(2, 1.0f);

    /**
     * Adds a language settings entry for the current project.
     *
     * @param languageId
     *          language id. Must not be {@code null}
     * @param entry
     *          language setting entry to set.
     */
    public void addSettingEntry(String languageId, ICLanguageSettingEntry entry) {
      Set<ICLanguageSettingEntry> langEntries = languages.get(languageId);
      if (langEntries == null) {
        langEntries = new HashSet<>(8);
        languages.put(languageId, langEntries);
      }
      langEntries.add(entry);
    }

  }

  /** Responsible to match the first argument (the tool command) of a command-line.
   *
   * @author weber
   */
  static class ParserDetector {
    /** pattern part that matches linux filesystem paths */
    protected static final String REGEX_CMD_HEAD = "^(.*?/)??(";
    /** pattern part that matches win32 filesystem paths */
    protected static final String REGEX_CMD_HEAD_WIN = "^(.*?" + Pattern.quote("\\") + ")??(";
    protected static final String REGEX_CMD_TAIL = ")\\s";

    /**
     * the Matcher that matches the name of the tool (including its path, BUT WITHOUT its filename extension)
     * on a given command-line
     */
    private final Matcher toolNameMatcher;
    /**
     * the corresponding parser for the tool arguments
     */
    public final IToolCommandlineParser parser;
    protected final String basenameRegex;
    /** whether to match a Linux path (which has a forward slash) or a Windows path with backSlashes */
    protected final boolean matchBackslash;

    /** Creates a {@code ParserDetector} that matches linux paths in the tool name.
     *
     * @param basenameRegex a regular expression that matches the base name of the tool to detect
     * @param parser the corresponding parser for the tool arguments
     */
    public ParserDetector(String basenameRegex, IToolCommandlineParser parser) {
      this(basenameRegex, false, parser);
    }

    /** Creates a {@code ParserDetector}.
     *
     * @param basenameRegex a regular expression that matches the base name of the tool to detect
     * @param matchBackslash whether to match a Linux path (which has a forward slash) or a Windows path with backSlashes.
     * @param parser the corresponding parser for the tool arguments
     */
    public ParserDetector(String basenameRegex, boolean matchBackslash, IToolCommandlineParser parser) {
      this.toolNameMatcher = matchBackslash
          ? Pattern.compile(REGEX_CMD_HEAD_WIN + basenameRegex + REGEX_CMD_TAIL).matcher("")
          : Pattern.compile(REGEX_CMD_HEAD + basenameRegex + REGEX_CMD_TAIL).matcher("");
      this.basenameRegex = basenameRegex;
      this.parser = parser;
      this.matchBackslash= matchBackslash;
    }

    /**
     * Gets, whether the parser for the tool arguments can properly parse the
     * specified command-line string. If so, the remaining arguments of the
     * command-line are returned.
     *
     *@param commandLine the command line to match
     *
     * @return {@code null} if the matcher does not matches the tool name in
     *         the command-line string. Otherwise, if the tool name matches, the remaining
     *         command-line string (without the portion that matched) is returned.
     */
    public String basenameMatches(String commandLine) {
      return matcherMatches(toolNameMatcher, commandLine);
    }

    /**
     * Gets, whether the parser for the tool arguments can properly parse the
     * specified command-line string. If so, the remaining arguments of the
     * command-line are returned. This is time-consuming, since it creates a
     * Matcher object on each invocation.
     *
     * @param commandLine
     *          the command-line to match
     * @param versionRegex
     *          a regular expression that matches the version string in the name
     *          of the tool to detect.
     *
     * @return {@code null} if the matcher does not matches the tool name in the
     *         command-line string. Otherwise, if the tool name matches, the
     *         remaining command-line string (without the portion that matched)
     *         is returned.
     */
    public String basenameWithVersionMatches(String commandLine, String versionRegex) {
      Matcher matcher = Pattern.compile(REGEX_CMD_HEAD + basenameRegex + versionRegex + REGEX_CMD_TAIL)
          .matcher("");
      return matcherMatches(matcher, commandLine);
    }

    /**
     * Gets, whether the specified Matcher for the tool arguments can properly parse the
     * specified command-line string. If so, the remaining arguments of the
     * command-line are returned.
     * @param matcher the matcher that performs the mathc
     *          a regular expression that matches the version string in the name
     *          of the tool to detect.
     * @param commandLine
     *          the command-line to match
     *
     * @return {@code null} if the matcher does not matches the tool name in the
     *         command-line string. Otherwise, if the tool name matches, the
     *         remaining command-line string (without the portion that matched)
     *         is returned.
     */
    protected final String matcherMatches(Matcher matcher, String commandLine) {
      matcher.reset(commandLine);
      if (matcher.lookingAt()) {
        return commandLine.substring(matcher.end());
      }
      return null;
    }
  } // ParserDetector

  /** Same as {@link CompileCommandsJsonParser.ParserDetector}, but handles tool detection properly, if
   * tool versions are allowed and the tool name contains a filename extension.
   *
   * @author weber
   */
  static class ParserDetectorExt extends ParserDetector {
    /**
     * the Matcher that matches the name of the tool (including its path AND its filename extension)
     * on a given command-line or <code>null</code>
     */
    private final Matcher toolNameMatcherExt;
    private final String extensionRegex;

    /** a {@code ParserDetectorExt} that matches linux paths in the tool name.
     * @param basenameRegex a regular expression that matches the base name of the tool to detect.
     * @param extensionRegex  a regular expression that matches the filename extension of the tool to detect .
     * @param parser the corresponding parser for the tool arguments
     */
    public ParserDetectorExt(String basenameRegex, String extensionRegex, IToolCommandlineParser parser) {
      this(basenameRegex, false, extensionRegex, parser);
    }

    /**
     * Creates a {@code ParserDetector}.
     *
     * @param basenameRegex
     *          a regular expression that matches the base name of the tool to
     *          detect
     * @param matchBackslash
     *          whether to match a Linux path (which has a forward slash) or a
     *          Windows path with backSlashes.
     * @param extensionRegex
     *          a regular expression that matches the filename extension of the
     *          tool to detect .
     * @param parser
     *          the corresponding parser for the tool arguments
     */
    public ParserDetectorExt(String basenameRegex, boolean matchBackslash, String extensionRegex,
        IToolCommandlineParser parser) {
      super(basenameRegex, matchBackslash, parser);
      String head = matchBackslash ? REGEX_CMD_HEAD_WIN : REGEX_CMD_HEAD;
      this.toolNameMatcherExt = Pattern.compile(head
          + basenameRegex + Pattern.quote(".") + extensionRegex + REGEX_CMD_TAIL).matcher("");
      this.extensionRegex = extensionRegex;
    }

   /**
     * Gets, whether the parser for the tool arguments can properly parse the
     * specified command-line string. If so, the remaining arguments of the
     * command-line are returned.
     *
     * @param commandLine
     *          the command-line to match
     * @return {@code null} if the matcher does not matches the tool name in the
     *         command-line string. Otherwise, if the tool name matches, the
     *         remaining command-line string (without the portion that matched)
     *         is returned.
     */
    public String basenameWithExtensionMatches(String commandLine) {
      return matcherMatches(toolNameMatcherExt, commandLine);
    }

    /**
     * Gets, whether the parser for the tool arguments can properly parse the
     * specified command-line string. If so, the remaining arguments of the
     * command-line are returned. This is time-consuming, since it creates a
     * Matcher object on each invocation.
     *
     * @param commandLine
     *          the command-line to match
     * @param versionRegex
     *          a regular expression that matches the version string in the name
     *          of the tool to detect.
     *
     * @return {@code null} if the matcher does not matches the tool name in the
     *         command-line string. Otherwise, if the tool name matches, the
     *         remaining command-line string (without the portion that matched)
     *         is returned.
     */
    public String basenameWithVersionAndExtensionMatches(String commandLine, String versionRegex) {
      String head = matchBackslash ? REGEX_CMD_HEAD_WIN : REGEX_CMD_HEAD;
      Matcher matcher = Pattern
          .compile(head + basenameRegex + versionRegex + Pattern.quote(".") + extensionRegex + REGEX_CMD_TAIL)
          .matcher("");
      return matcherMatches(matcher, commandLine);
    }

  }

  // has package scope for unittest purposes
  static class DetectorWithMethod  {
    enum DetectionMethod {
      BASENAME,
      WITH_VERSION,
      WITH_EXTENSION,
      WITH_VERSION_EXTENSION;
    }

    /**
     * the ParserDetector that matched the name of the tool on a given command-line
     */
    final ParserDetector detector;
    /** describes the method that was used to match */
    final DetectionMethod how;

    /**
     * @param detector
     *          the ParserDetector that matched the name of the tool on a given
     *          command-line
     * @param how
     *          describes the method that was used to match
     */
    public DetectorWithMethod(ParserDetector detector, DetectionMethod how) {
      this.detector = detector;
      this.how = how;
    }
}
  // has package scope for unittest purposes
  static class ParserDetectionResult{

    /*package */ final DetectorWithMethod detectorWMethod;
    /**
     * the remaining arguments of the command-line, after the matcher has
     * matched the tool name
     */
    private final String reducedCommandLine;

    /**
     * @param detectorWMethod
     *          the ParserDetector that matched the name of the tool on a given
     *          command-line
     * @param reducedCommandLine
     *          the remaining arguments of the command-line, after the matcher
     *          has matched the tool name
     */
    public ParserDetectionResult(DetectorWithMethod detectorWMethod, String reducedCommandLine) {
      this.detectorWMethod = detectorWMethod;
      this.reducedCommandLine = reducedCommandLine;
    }

    /**
     * Gets the remaining arguments of the command-line, after the matcher has
     * matched the tool name (i.e. without the portion that matched).
     */
    public String getReducedCommandLine() {
      return this.reducedCommandLine;
    }
  }

}
