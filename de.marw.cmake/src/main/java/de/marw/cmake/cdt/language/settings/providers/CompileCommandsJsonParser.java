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
import org.eclipse.cdt.core.AbstractExecutableExtensionBase;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.language.settings.providers.ICBuildOutputParser;
import org.eclipse.cdt.core.language.settings.providers.ICListenerAgent;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.language.settings.providers.IWorkingDirectoryTracker;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsStorage;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.ui.PreferenceConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
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
public class CompileCommandsJsonParser extends AbstractExecutableExtensionBase
    implements ILanguageSettingsProvider, ICListenerAgent, ICBuildOutputParser,
    Cloneable {
  private static final ILog log = CMakePlugin.getDefault().getLog();

  /**
   * Storage to keep settings entries
   */
  private PerConfigLanguageSettingsStorage storage =
      new PerConfigLanguageSettingsStorage();

  private ICConfigurationDescription currentCfgDescription;

  /**
   * tool detector and their tool option parsers for each tool of interest that
   * takes part in the current build. The Matcher detects whether a command line
   * is an invocation of the tool.
   */
  private static final HashMap<Matcher, IToolCommandlineParser> detectorParserMap;
  static {
    detectorParserMap =
        new HashMap<Matcher, IToolCommandlineParser>(18, 1.0f);

    /** Names of known tools along with their command line argument parsers */
    final Map<String, IToolCommandlineParser> knownCmdParsers =
        new HashMap<String, IToolCommandlineParser>(16, 1.0f);

    final IToolArgumentParser[] posix_cc_args =
        { new ToolArgumentParsers.IncludePath_C_POSIX(),
            new ToolArgumentParsers.MacroDefine_C_POSIX(),
            new ToolArgumentParsers.MacroUndefine_C_POSIX(),
            // not defined by POSIX, but does not harm..
            new ToolArgumentParsers.SystemIncludePath_C() };
    // POSIX compatible C compilers =================================
    final ToolCommandlineParser cc =
        new ToolCommandlineParser("org.eclipse.cdt.core.gcc", posix_cc_args);
    knownCmdParsers.put("cc", cc);
    knownCmdParsers.put("cc\\.exe", cc);
    knownCmdParsers.put("gcc", cc);
    knownCmdParsers.put("gcc\\.exe", cc);
    knownCmdParsers.put("clang", cc);
    knownCmdParsers.put("clang\\.exe", cc);
    // POSIX compatible C++ compilers ===============================
    final ToolCommandlineParser cxx =
        new ToolCommandlineParser("org.eclipse.cdt.core.g++", posix_cc_args);
    knownCmdParsers.put("c\\+\\+", cxx);
    knownCmdParsers.put("c\\+\\+\\.exe", cxx);
    knownCmdParsers.put("g\\+\\+", cxx);
    knownCmdParsers.put("g\\+\\+\\.exe", cxx);
    knownCmdParsers.put("clang\\+\\+", cxx);
    knownCmdParsers.put("clang\\+\\+\\.exe", cxx);
    // GNU C and C++ cross compilers, e.g. arm-none-eabi-gcc.exe ====
    knownCmdParsers.put(".+-gcc", cc);
    knownCmdParsers.put(".+-gcc\\.exe", cc);
    knownCmdParsers.put(".+-g\\+\\+", cxx);
    knownCmdParsers.put(".+-g\\+\\+\\.exe", cxx);

    // ms C + C++ compiler ==========================================
    final IToolArgumentParser[] cl_cc_args =
        { new ToolArgumentParsers.IncludePath_C_CL(),
            new ToolArgumentParsers.MacroDefine_C_CL(),
            new ToolArgumentParsers.MacroUndefine_C_CL() };
    final ToolCommandlineParser cl =
        new ToolCommandlineParser("org.eclipse.cdt.core.gcc", cl_cc_args);
    knownCmdParsers.put("cl\\.exe", cl);

    // Intel C compilers ============================================
    final ToolCommandlineParser icc =
        new ToolCommandlineParser("org.eclipse.cdt.core.gcc", posix_cc_args);
    final ToolCommandlineParser icpc =
        new ToolCommandlineParser("org.eclipse.cdt.core.g++", posix_cc_args);

    // Linux & OS X, EDG
    knownCmdParsers.put("icc", icc);
    // OS X, clang
    knownCmdParsers.put("icl", icc);
    // Intel C++ compiler
    // Linux & OS X, EDG
    knownCmdParsers.put("icpc", icpc);
    // OS X, clang
    knownCmdParsers.put("icl\\+\\+", icpc);
    // Windows C + C++, EDG
    knownCmdParsers.put("icl.exe", cl);

    // construct matchers that detect the tool name...
    final String REGEX_CMD_HEAD =
        "^(.*?" + Pattern.quote(File.separator) + ")?(";
    final String REGEX_CMD_TAIL = ")\\s";
    for (Entry<String, IToolCommandlineParser> entry : knownCmdParsers
        .entrySet()) {
      // 'cc' -> matches
      // '/bin/cc' -> matches
      // '/usr/bin/cc' -> matches
      // 'C:\program files\mingw\bin\cc' -> matches
      Matcher cmdDetector = Pattern
          .compile(
              REGEX_CMD_HEAD + entry.getKey() + REGEX_CMD_TAIL)
          .matcher("");
      detectorParserMap.put(cmdDetector, entry.getValue());
    }
  }

  /**
   * last known working tool detector and its tool option parsers or
   * {@code null}, if unknown (to speed up parsing)
   */
  private ParserLookupResult preferredCmdlineParser;

  public CompileCommandsJsonParser() {
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
  public List<ICLanguageSettingEntry> getSettingEntries(
      ICConfigurationDescription cfgDescription, IResource rc,
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
   *        {@code true} if the workbench is starting up. If {@code true}, this
   *        method will not trigger UI update to show newly detected include
   *        paths nor will it complain if a "compile_commands.json" file does
   *        not exist.
   * @throws CoreException
   */
  private void tryParseJson(boolean initializingWorkbench)
      throws CoreException {

    // If getBuilderCWD() returns a workspace relative path, it is garbled.
    // If garbled, make sure de.marw.cdt.cmake.core.internal.BuildscriptGenerator.getBuildWorkingDir()
    // returns a full, absolute path relative to the workspace.
    final IPath builderCWD =
        currentCfgDescription.getBuildSetting().getBuilderCWD();

    IPath jsonPath = builderCWD.append("compile_commands.json");
    final IFile jsonFileRc =
        ResourcesPlugin.getWorkspace().getRoot().getFile(jsonPath);

    final IPath location = jsonFileRc.getLocation();
    final IProject project =
        currentCfgDescription.getProjectDescription().getProject();
    if (location != null) {
      final File jsonFile = location.toFile();
      if (jsonFile.exists()) {
        // file exists on disk...
        final long tsJsonModified = jsonFile.lastModified();

        final TimestampedLanguageSettingsStorage store =
            storage.getSettingsForConfig(currentCfgDescription);
        final ProjectLanguageSettingEntries projectEntries =
            new ProjectLanguageSettingEntries();

        if (store.lastModified < tsJsonModified) {
          // must parse json file...
          try {
            // parse file...
            JSON parser = new JSON();
            Reader in = new FileReader(jsonFile);

            Object parsed = parser.parse(new JSON.ReaderSource(in), false);
            if (parsed instanceof Object[]) {
              for (Object o : (Object[]) parsed) {
                if (o instanceof Map) {
                  processJsonEntry(store, projectEntries, (Map<?, ?>) o,
                      jsonPath);
                } else {
                  // expected Map object, skipping entry.toString()
                  log.log(new Status(IStatus.WARNING, CMakePlugin.PLUGIN_ID,
                      "'" + project.getName() + "' " + "File format error: "
                          + jsonPath.toString() + ": unexpected entry '" + o
                          + "', skipped",
                      null));
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
//                  System.out.println("stored cached compile_commands");

              // trigger UI update to show newly detected include paths
              // must run in UI thread
              Display.getDefault().asyncExec(new Runnable() {

                public void run() {
                  IWorkbenchWindow activeWorkbenchWindow =
                      PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                  IWorkbenchPage activePage =
                      activeWorkbenchWindow.getActivePage();
                  IWorkbenchPartReference refs[] =
                      activePage.getViewReferences();
                  for (IWorkbenchPartReference ref : refs) {
                    IWorkbenchPart part = ref.getPart(false);
                    if (part instanceof IPropertyChangeListener)
                      ((IPropertyChangeListener) part)
                          .propertyChange(new PropertyChangeEvent(project,
                              PreferenceConstants.PREF_SHOW_CU_CHILDREN, null,
                              null));
                  }
                }
              });
            } else {
              // file format error
              log.log(new Status(IStatus.WARNING, CMakePlugin.PLUGIN_ID,
                  "'" + project.getName() + "' " + "File format error: "
                      + jsonPath.toString() + " does not seem to be JSON",
                  null));
            }
          } catch (IOException ex) {
            log.log(new Status(IStatus.WARNING, CMakePlugin.PLUGIN_ID, "'"
                + project.getName() + "' " + "Failed to read file " + jsonFile,
                ex));
          }
        }
        return;
      }
    }
    if (!initializingWorkbench) {
      // no json file was produced in the build
      log.log(new Status(IStatus.WARNING, CMakePlugin.PLUGIN_ID,
          "'" + jsonPath + "' " + " not created in the build", null));
    }
  }

  /**
   * Processes an entry from a {@code compile_commands.json} file and stores a
   * {@link ICLanguageSettingEntry} for the file given the specified map.
   *
   * @param storage
   *        where to store language settings
   * @param projectEntries
   *        where to store project wide setting entries
   * @param sourceFileInfo
   *        a Map of type Map<String,String>
   * @param jsonPath
   *        the JSON file being parsed (for logging only)
   */
  private void processJsonEntry(TimestampedLanguageSettingsStorage storage,
      ProjectLanguageSettingEntries projectEntries, Map<?, ?> sourceFileInfo,
      IPath jsonPath) {
    if (sourceFileInfo.containsKey("file")
        && sourceFileInfo.containsKey("command")) {
      final String file = sourceFileInfo.get("file").toString();
      if (file != null && !file.isEmpty()) {
        final String cmdLine = sourceFileInfo.get("command").toString();
        if (cmdLine != null && !cmdLine.isEmpty()) {
          final File path = new File(file);
          final IFile[] files = ResourcesPlugin.getWorkspace().getRoot()
              .findFilesForLocationURI(path.toURI());
          if (files.length > 0) {
            if (!processCommandLineAnyDetector(storage, projectEntries,
                files[0], cmdLine)) {
              log.log(new Status(IStatus.WARNING,
                  CMakePlugin.PLUGIN_ID, jsonPath.toString()
                      + ": No parser for command '" + cmdLine + "', skipped",
                  null));
            }
          }
          return;
        }
      }
    }
    // unrecognized entry, skipping
    log.log(
        new Status(IStatus.WARNING, CMakePlugin.PLUGIN_ID,
            "File format error: " + jsonPath.toString()
                + ": 'file' or 'command' missing in JSON object, skipped",
            null));
  }

  /**
   * Processes the command-line of an entry from a {@code compile_commands.json}
   * file by trying each parser in {@link #detectorParserMap} and stores a
   * {@link ICLanguageSettingEntry} for the file found in the specified map.
   *
   * @param storage
   *        where to store language settings
   * @param projectEntries
   *        where to store project wide setting entries
   * @param sourceFile
   *        the source file resource corresponding to the source file being
   *        processed by the tool
   * @param line
   *        the command line to process
   * @return {@code true} if a parser for the command-line could be found,
   *         {@code false} if no parser could be found (nothing was processed)
   */
  private boolean processCommandLineAnyDetector(
      TimestampedLanguageSettingsStorage storage,
      ProjectLanguageSettingEntries projectEntries, IFile sourceFile,
      String line) {
    // try last known matching detector first...
    if (preferredCmdlineParser == null || !preferredCmdlineParser.canParse(line)){
      // try each tool..
      preferredCmdlineParser= determineParserForCommandline(line);
    }
    if (preferredCmdlineParser == null && Platform.getOS() == Platform.OS_WIN32) {
      // try workaround
      preferredCmdlineParser= determineParserForCommandline(expandShortFileName(line));
    }
    if (preferredCmdlineParser == null) {
      return false; // no matching parser found
    }
    // found a matching command-line parser
    processCommandLine(storage, projectEntries, preferredCmdlineParser.parser, sourceFile, line);
    return true; // could process command line
  }

  /**
   * Determines a C-compiler-command line parser that is able to parse the relevant arguments
   * in the specified command line.
   *
   * @return the {@code ParserLookupResult} which describes the result of this operation
   * or {@code null} if no command line parser for the specified command-line was found.
   */
  // has package scope for unittest purposes
  ParserLookupResult determineParserForCommandline(String commandLine){
    for (Entry<Matcher, IToolCommandlineParser> detectorInfo : detectorParserMap
        .entrySet()) {
      final Matcher matcher = detectorInfo.getKey();
      matcher.reset(commandLine);
      if (matcher.lookingAt()) {
        // found a matching command-line parser
        final String newArgs = commandLine.substring(matcher.end());
        return new ParserLookupResult(matcher, detectorInfo.getValue(), newArgs);
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
      log.log(new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID,
          "CompileCommandsJsonParserFSN#determineParserForCommandline()", e));
    }
    return null;
  }

  /**
   * Processes the command-line of an entry from a {@code compile_commands.json}
   * file by trying the specified detector and stores a
   * {@link ICLanguageSettingEntry} for the file found in the specified map.
   *
   * @param storage
   *        where to store language settings
   * @param projectEntries
   *        where to store project wide setting entries
   * @param cmdlineParser
   *        the tool detector and its tool option parsers
   * @param sourceFile
   *        the source file resource corresponding to the source file being
   *        processed by the tool
   * @param line
   *        the command line to process
   */
  private void processCommandLine(TimestampedLanguageSettingsStorage storage,
      ProjectLanguageSettingEntries projectEntries,
      IToolCommandlineParser cmdlineParser, IFile sourceFile,
      String line) {
      line = ToolCommandlineParser.trimLeadingWS(line);
      final List<ICLanguageSettingEntry> entries =
          cmdlineParser.processArgs(line);
      // attach settings to sourceFile resource...
      if (entries != null && entries.size() > 0) {
        /*
         * compile_commands.json holds entries per-file only and does not
         * contain per-project entries. For include dirs, add these entries to
         * the project resource to make them show up in the UI in the includes
         * folder...
         */
        for (Iterator<ICLanguageSettingEntry> iter = entries.iterator(); iter
            .hasNext();) {
          ICLanguageSettingEntry entry = iter.next();
          if (entry.getKind() == ICSettingEntry.INCLUDE_PATH) {
            projectEntries.addSettingEntry(cmdlineParser.getLanguageId(),
                entry);
          }
        }
        storage.setSettingEntries(sourceFile, cmdlineParser.getLanguageId(),
            entries);
      }
  }

  /*-
   * interface ICBuildOutputParser
   */
  @Override
  public void startup(ICConfigurationDescription cfgDescription,
      IWorkingDirectoryTracker cwdTracker) throws CoreException {
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
      log.log(new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID, "tryParseJson()",
          ex));
    }
    // release resources for garbage collector
    currentCfgDescription = null;
  }

  @Override
  public CompileCommandsJsonParser clone() throws CloneNotSupportedException {
    return (CompileCommandsJsonParser) super.clone();
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
        if (project.isOpen()
            && project.hasNature(ScannerConfigNature.NATURE_ID)) {
          ICProjectDescription projectDescription =
              ccp.getProjectDescription(project, false);
          if (projectDescription != null) {
            ICConfigurationDescription activeConfiguration =
                projectDescription.getActiveConfiguration();
            if (activeConfiguration instanceof ILanguageSettingsProvidersKeeper) {
              final List<ILanguageSettingsProvider> lsps =
                  ((ILanguageSettingsProvidersKeeper) activeConfiguration)
                      .getLanguageSettingProviders();
              for (ILanguageSettingsProvider lsp : lsps) {
                if ("de.marw.cmake.cdt.language.settings.providers.CompileCommandsJsonParser"
                    .equals(lsp.getId())) {
                  currentCfgDescription = activeConfiguration;
                  tryParseJson(true);
                  break;
                }
              }
            }
          }
        }
      } catch (CoreException ex) {
        log.log(new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID,
            "tryParseJson()", ex));
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
  private static class TimestampedLanguageSettingsStorage
      extends LanguageSettingsStorage {
    /** cached file modification time-stamp of last parse */
    long lastModified = 0;

    /**
     * Sets language settings entries for this storages.
     *
     * @param rc
     *        resource such as file or folder. If {@code null} the entries are
     *        considered to be being defined as default entries for resources.
     * @param languageId
     *        language id. Must not be {@code null}
     * @param entries
     *        language settings entries to set.
     */
    public void setSettingEntries(IResource rc, String languageId,
        List<ICLanguageSettingEntry> entries) {
      final String rcPath = rc != null ? rc.toString() : null;
//      System.out.println("#> setSettingEntries( " + rc + ", " + languageId + ")");
//      System.out.println("\t" + entries);
      super.setSettingEntries(rcPath, languageId, entries);
    }

    public void addProjectLanguageSettingEntries(IProject project,
        ProjectLanguageSettingEntries projectEntries) {
      for (Entry<String, Set<ICLanguageSettingEntry>> entry : projectEntries.languages
          .entrySet()) {
        List<ICLanguageSettingEntry> lses = Arrays
            .asList(entry.getValue().toArray(new ICLanguageSettingEntry[0]));
        setSettingEntries(project, entry.getKey(), lses);
      }
    }

    public TimestampedLanguageSettingsStorage clone()
        throws CloneNotSupportedException {
      TimestampedLanguageSettingsStorage cloned =
          (TimestampedLanguageSettingsStorage) super.clone();
      cloned.lastModified = this.lastModified;
      return cloned;
    }
  } // TimestampedLanguageSettingsStorage

  private static class PerConfigLanguageSettingsStorage implements Cloneable {
    /**
     * Storage to keep settings entries. Key is
     * {@link ICConfigurationDescription#getId()}
     */
    private Map<String, TimestampedLanguageSettingsStorage> storages =
        new WeakHashMap<String, TimestampedLanguageSettingsStorage>();

    public List<ICLanguageSettingEntry> getSettingEntries(
        ICConfigurationDescription cfgDescription, IResource rc,
        String languageId) {
//      System.out.println("#< getSettingEntries( " + cfgDescription + ", " + rc + ", " + languageId + ")\t");
      final LanguageSettingsStorage store =
          storages.get(cfgDescription.getId());
      if (store != null) {
        final String rcPath = rc.toString();
        List<ICLanguageSettingEntry> entries =
            store.getSettingEntries(rcPath, languageId);
        if (entries == null && languageId != null) {
          entries = store.getSettingEntries(rcPath, null);
        }
//        System.out.println("\t" + entries);
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
    public TimestampedLanguageSettingsStorage getSettingsForConfig(
        ICConfigurationDescription cfgDescription) {
      TimestampedLanguageSettingsStorage store =
          storages.get(cfgDescription.getId());
      if (store == null) {
        store = new TimestampedLanguageSettingsStorage();
        storages.put(cfgDescription.getId(), store);
      }
      return store;
    }

    public PerConfigLanguageSettingsStorage clone()
        throws CloneNotSupportedException {
      PerConfigLanguageSettingsStorage cloned =
          new PerConfigLanguageSettingsStorage();
      for (Entry<String, TimestampedLanguageSettingsStorage> entry : storages
          .entrySet()) {
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
    private final Map<String, Set<ICLanguageSettingEntry>> languages =
        new HashMap<String, Set<ICLanguageSettingEntry>>(2, 1.0f);

    /**
     * Adds a language settings entry for the current project.
     *
     * @param languageId
     *        language id. Must not be {@code null}
     * @param entry
     *        language setting entry to set.
     */
    public void addSettingEntry(String languageId,
        ICLanguageSettingEntry entry) {
      Set<ICLanguageSettingEntry> langEntries = languages.get(languageId);
      if (langEntries == null) {
        langEntries = new HashSet<ICLanguageSettingEntry>(8);
        languages.put(languageId, langEntries);
      }
      langEntries.add(entry);
    }

  }

  // has package scope for unittest purposes
  static class ParserLookupResult {
    /**
     * the Matcher that matched the name of the tool on a given command-line
     */
    private final Matcher toolNameMatcher;
    /**
     * the parser for the tool arguments
     */
    public final IToolCommandlineParser parser;
    /**
     * the remaining arguments of the command-line, after the matcher has
     * matched the tool name
     */
    private String reducedCommandLine;

    /**
     * @param toolNameMatcher  a Matcher that matches the name of the tool on a given command-line
     * @param parser  the parser for the tool arguments
     * @param reducedCommandLine  the remaining arguments from the command-line, after the matcher has
     *  matched the tool name
     */
    public ParserLookupResult(Matcher toolNameMatcher, IToolCommandlineParser parser, String reducedCommandLine) {
      this.parser = parser;
      this.reducedCommandLine = reducedCommandLine;
      this.toolNameMatcher=toolNameMatcher;
    }

    /** Gets, whether the parser for the tool arguments can properly parse the
     * specified command-line string. If so, the remaining arguments of the command-line {@link #getReducedCommandLine}
     * will be updated from the specified {@code commandLine} value.
     *
     * @return {@code true} if the matcher successfully matches the tool name in
     * the command-line string, otherwise {@code false}.
     */
    public boolean canParse(String commandLine) {
      toolNameMatcher.reset(commandLine);
      if (toolNameMatcher.lookingAt()) {
        // found a matching command-line parser
        reducedCommandLine = commandLine.substring(toolNameMatcher.end());
        return true;
      }
      return false;
    }

    /**
     * Gets the remaining arguments of the command-line, after the matcher has
     * matched the tool name
     */
    public String getReducedCommandLine() {
      return this.reducedCommandLine;
    }

  }
}
