/* ******************************************************************************
 * Copyright (c) 2013-2019 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.cdt.core.cdtvariables.CdtVariableException;
import org.eclipse.cdt.core.cdtvariables.ICdtVariableManager;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.managedbuilder.buildproperties.IBuildProperty;
import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyValue;
import org.eclipse.cdt.managedbuilder.core.IBuildObjectProperties;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator2;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jetty.util.QuotedStringTokenizer;

import com.google.gson.JsonSyntaxException;

import de.marw.cmake4eclipse.mbs.console.CdtConsoleConstants;
import de.marw.cmake4eclipse.mbs.preferences.BuildToolKitDefinition;
import de.marw.cmake4eclipse.mbs.preferences.PreferenceAccess;
import de.marw.cmake4eclipse.mbs.settings.CMakeSettings;
import de.marw.cmake4eclipse.mbs.settings.CmakeDefine;
import de.marw.cmake4eclipse.mbs.settings.CmakeGenerator;
import de.marw.cmake4eclipse.mbs.settings.CmakeUnDefine;
import de.marw.cmake4eclipse.mbs.settings.ConfigurationManager;
import de.marw.cmake4eclipse.mbs.settings.ProjectPropsModifiedDateUtil;

/**
 * Generates makefiles and other build scripts from CMake scripts
 * (CMakeLists.txt).
 *
 * @author Martin Weber
 */
public class BuildscriptGenerator implements IManagedBuilderMakefileGenerator2 {
  private static final ILog log = Activator.getDefault().getLog();

  /** buildscript generation error marker ID */
  private static final String MARKER_ID = Activator.PLUGIN_ID + ".BuildscriptGenerationError";

  private IProject project;
  private IProgressMonitor monitor;
  private IConfiguration config;
  private IBuilder builder;
  /** build path - relative to the project. Lazily instantiated */
  private IPath buildRelPath;

  /**   */
  public BuildscriptGenerator() {
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator2#initialize(int, org.eclipse.cdt.managedbuilder.core.IConfiguration, org.eclipse.cdt.managedbuilder.core.IBuilder, org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public void initialize(int buildKind, IConfiguration cfg, IBuilder builder, IProgressMonitor monitor) {
    // Save the project so we can get path and member information
    this.project = cfg.getOwner().getProject();
    // Save the monitor reference for reporting back to the user
    this.monitor = monitor;
    // Cache the build tools
    this.config = cfg;
    this.builder = builder;
    this.buildRelPath = null;
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#initialize(org.eclipse.core.resources.IProject, org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo, org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public void initialize(IProject project, IManagedBuildInfo info, IProgressMonitor monitor) {
    // 15kts; seems this is never called
    // Save the project so we can get path and member information
    this.project = project;
    // Save the monitor reference for reporting back to the user
    this.monitor = monitor;
    // Cache the build tools
    config = info.getDefaultConfiguration();
    builder = config.getEditableBuilder();
  }

  private IPath getRelBuildPath() {
    if (buildRelPath == null) {
      // set the top build dir path for the current configuration
      String buildDirStr = null;
      final ICConfigurationDescription cfgd = ManagedBuildManager.getDescriptionForConfiguration(config);
      try {
        CMakeSettings prefs = ConfigurationManager.getInstance().getOrLoad(cfgd);
        buildDirStr = prefs.getBuildDirectory();
      } catch (CoreException e) {
        // storage base is null; treat as bug in CDT..
        log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "falling back to hard coded build directory", e));
      }

      try {
        buildDirStr = CCorePlugin.getDefault().getCdtVariableManager().resolveValue(buildDirStr, "", null, cfgd);
        buildRelPath = new Path(buildDirStr);
      } catch (CdtVariableException e) {
        log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "variable expansion for build directory failed", e));
      }
    }
    return buildRelPath;
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#getBuildWorkingDir()
   */
  @Override
  public IPath getBuildWorkingDir() {
    // Note that IPath from ICBuildSetting#getBuilderCWD() holding variables is mis-constructed,
    // i.e. ${workspace_loc:/path} gets split into 2 path segments, if we return a relative path here.
    // MBS does that and we need to handle that.
    // So return a workspace relative path here
    final IPath fullPath = project.getFullPath();
    IPath buildPath = getRelBuildPath();
    if (buildPath.segmentCount() == 0) {
      return fullPath;
    }
    return fullPath.append(buildPath);
  }

  /**
   * Invoked on incremental build?
   */
  @Override
  public MultiStatus generateMakefiles(IResourceDelta delta) throws CoreException {
    CMakeListsVisitor visitor = new CMakeListsVisitor();
    // did the user modify one of the CMakeLists.txt?
    delta.accept(visitor);
    /*
     * If one of the CMakeLists.txt has changed, the generated build scripts will run cmake in order to update the
     * scripts during the build. But CDT's ICommandLauncher intermixes the stdout and stderr streams of the cmake
     * process, making it impossible to code a cmake error parser implementation of IErrorParser that works. So we force
     * to run cmake in advance to feeds its output to an error parser that WORKS.
     */
    return generateBuildscripts(visitor.hasChanges);
  }

  /**
   * Invoked on full build?
   */
  @Override
  public MultiStatus regenerateMakefiles() throws CoreException {
    return generateBuildscripts(false);
  }

  /**
   * @param forceGeneration
   *          <code>true</code> if cmake must be run, regardless whether the build-scripts have already been generated
   */
  private MultiStatus generateBuildscripts(boolean forceGeneration) throws CoreException {
    final Date startDate = new Date();
    project.deleteMarkers(MARKER_ID, false, IResource.DEPTH_ZERO);

    final ICConfigurationDescription cfgDes = ManagedBuildManager.getDescriptionForConfiguration(config);

    IPath cmakelistsPath;
    ICStorageElement storage = cfgDes.getProjectDescription().getStorage(CMakeSettings.CFG_STORAGE_ID, false);
    if (storage != null) {
      // Cmake4eclipse nature holds a path to the top-level cmakelists.txt file
      String cmakelists = storage.getAttribute(CMakeSettings.ATTR_CMAKELISTS_FLDR);
      cmakelistsPath = new Path(cmakelists);
    } else {
      // classic cmake4eclipse with MBS build system...
      // .. assumes the top-level cmakelists.txt file is below the (single) source location
      ICSourceEntry[] srcEntries = config.getSourceEntries();

      // do a sanity check..
      if (srcEntries.length == 0) {
        // no source folders specified in project
        final String msg = "No source directories configured for project";
        MultiStatus status = new MultiStatus(Activator.PLUGIN_ID, IStatus.ERROR, msg + " " + project.getName(), null);
        createErrorMarker(project, msg);
        return status;
      } else {
        srcEntries = CDataUtil.resolveEntries(srcEntries, cfgDes);
        // assume the first source directory contains a CMakeLists.txt
        cmakelistsPath = srcEntries[0].getFullPath();
      }
    }

    // See if the user has cancelled the build
    checkCancel();

    boolean mustGenerate= forceGeneration;

    final IContainer buildFolder;
    IPath buildPath = getRelBuildPath();
    if (buildPath.segmentCount() == 0) {
      buildFolder= project;
    } else {
      buildFolder = project.getFolder(buildPath);
      createFolder((IFolder) buildFolder);
    }
    // make sure we have a resource to attach session properties to
    final java.nio.file.Path buildDir = Paths.get(buildFolder.getLocationURI());

    IEclipsePreferences prefs = PreferenceAccess.getPreferences();
    try {
      final java.nio.file.Path cacheFile = buildDir.resolve( "CMakeCache.txt");
      boolean cacheFileExists = Files.exists(cacheFile);
      if (cacheFileExists) {
        ConfigurationManager.getInstance().getOrLoad(cfgDes); // migrate dirty time stamp attribute to file time stamp

        if (prefs.getLong(PreferenceAccess.DIRTY_TS, 0L) > Files.getLastModifiedTime(cacheFile).toMillis()
            || ProjectPropsModifiedDateUtil.getLastModified(project) > Files.getLastModifiedTime(cacheFile).toMillis()
            || prefs.getBoolean(PreferenceAccess.CMAKE_FORCE_RUN, false)) {
    mustGenerate = true;
    // The generator might have changed, remove cache file to avoid cmake's complaints..
    Files.delete(cacheFile);
    // also remove cache files in cmake projects that were downloaded by cmake's FetchContent call (e.g. for CPM)...
    java.nio.file.Path cpmDepsPath = buildDir.resolve("_deps");
    if( Files.exists(cpmDepsPath)) {
     Files.walkFileTree(cpmDepsPath, EnumSet.noneOf(FileVisitOption.class), 2,
        new SimpleFileVisitor<java.nio.file.Path>() {
          @Override
          public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs) throws IOException {
            if ("CMakeCache.txt".equals(file.getFileName().toString())) {
              Files.delete(file);
            }
            return FileVisitResult.CONTINUE;
          }
        });
    }
   }
      }
      if (!mustGenerate && (!cacheFileExists || !Files.exists(buildDir.resolve(getMakefileName())))) {
        mustGenerate = true;
      }
      if (!mustGenerate) {
        return new MultiStatus(Activator.PLUGIN_ID, IStatus.OK, "", null);
      }

      // Create the top-level directory for the build output
      Files.createDirectories(buildDir);
    } catch (IOException ex) {
      // if multiple projects are build, this information is lost when a new console is opened.
      // So create a problem marker to show up in the problem view
      createErrorMarker(project, ex.getMessage());
      return new MultiStatus(Activator.PLUGIN_ID, IStatus.ERROR, ex.getMessage(), ex);
    }
    updateMonitor("Execute CMake for " + project.getName());
    // See if the user has cancelled the build
    checkCancel();

    final IConsole console = CCorePlugin.getDefault().getConsole(CdtConsoleConstants.CMAKE_CONSOLE_ID);
    console.start(project);

    // create makefile
    try {
      final OutputStream cis = console.getInfoStream();
      String msg = String.format("%tT Buildscript generation: %s::%s in %s\n", startDate, project.getName(),
          config.getName(), buildDir);
      cis.write(msg.getBytes());
    } catch (IOException ignore) {
    }
    IContainer srcDir = cmakelistsPath.isEmpty() ? project : project.getFolder(cmakelistsPath);

    checkCancel();
    MultiStatus status = invokeCMake(srcDir, buildFolder.getLocation(), console);
    // NOTE: Commonbuilder reads getCode() to detect errors, not getSeverity()
    if (status.getCode() == IStatus.ERROR) {
      // failed to generate
      // if multiple projects are build, this information is lost when the a new console is opened.
      // So create a problem marker to show up in the problem view
      createErrorMarker(project, status.getMessage());
      return status;
    }

    try {
      final OutputStream cis = console.getInfoStream();
      Date endDate = new Date();
      String msg = String.format("%tT Buildscript generation finished (took %d ms)\n", endDate,
          endDate.getTime() - startDate.getTime());
      cis.write(msg.getBytes());
    } catch (IOException ignore) {
    }
    return new MultiStatus(Activator.PLUGIN_ID, IStatus.OK, "", null);
  }

  /**
   * Recursively creates the folder hierarchy needed for the build output, if
   * necessary. If the folder is created, its derived bit is set to true so the
   * CM system ignores the contents. If the resource exists, respect the
   * existing derived setting.
   *
   * @param folder
   *        a folder, somewhere below the project root
   */
  private void createFolder(IFolder folder) throws CoreException {
    if (!folder.exists()) {
      // Make sure that parent folders exist
      IContainer parent = folder.getParent();
      if (parent instanceof IFolder && !parent.exists()) {
        createFolder((IFolder) parent);
      }

      // Now make the requested folder
      try {
        folder.create(IResource.DERIVED, true, monitor);
      } catch (CoreException e) {
        if (e.getStatus().getCode() == IResourceStatus.PATH_OCCUPIED)
          folder.refreshLocal(IResource.DEPTH_ZERO, monitor);
        else
          throw e;
      }
    }
  }

  /**
   * Run 'cmake -G xyz' command.
   *
   * @param console
   *        the build console to send messages to
   * @param buildPath
   *        abs. path
   * @param srcFolder
   * @return a MultiStatus object, where .getCode() return the severity
   * @throws CoreException
   */
  private MultiStatus invokeCMake(IContainer srcFolder, IPath buildPath, IConsole console) throws CoreException {
    IEclipsePreferences prefs = PreferenceAccess.getPreferences();
    try {
      Optional<BuildToolKitDefinition> overwritingBtk = BuildToolKitUtil.getOverwritingToolkit(prefs);

      // Set the environment
      ArrayList<String> envList = buildEnvironment(console, overwritingBtk);

      final List<String> argList = buildCommandline(srcFolder.getLocation(), overwritingBtk);
      // extract cmake command
      final String cmd = argList.remove(0);
      // run cmake..
      final ICommandLauncher launcher = builder.getCommandLauncher();
      launcher.setProject(project); // 9.4++ versions of CDT require this for docker
      launcher.showCommand(true);
      final Process proc = launcher.execute(new Path(cmd), argList.toArray(new String[argList.size()]),
          envList.toArray(new String[envList.size()]), buildPath, monitor);
      if (proc != null) {
        try {
          // Close the input of the process since we will never write to it
          proc.getOutputStream().close();
        } catch (IOException e) {
        }
        CMakeErrorParser.deleteErrorMarkers(project);
        // hook in cmake error parsing
        CMakeExecutionMarkerFactory markerFactory = new CMakeExecutionMarkerFactory(srcFolder);
        // NOTE: we need 2 of this, since the output streams are not synchronized, causing loss of
        // the internal processor state
        try (CMakeErrorParser errorParserE = new CMakeErrorParser(markerFactory);
            CMakeErrorParser errorParserO = new CMakeErrorParser(markerFactory)) {
          OutputStream epe = new ParsingConsoleOutputStream(console.getErrorStream(), errorParserE);
          OutputStream epo = new ParsingConsoleOutputStream(console.getOutputStream(), errorParserO);

          int state = launcher.waitAndRead(epo, epe, monitor);
          if (state == ICommandLauncher.COMMAND_CANCELED) {
            throw new OperationCanceledException(launcher.getErrorMessage());
          }

          // check cmake exit status
          final int exitValue = proc.exitValue();
          if (exitValue == 0) {
            // success
            return new MultiStatus(Activator.PLUGIN_ID, IStatus.OK, null, null);
          } else {
            // cmake had errors...
            String msg = String.format("%1$s exited with status %2$d. See CDT global build console for details.", cmd,
                exitValue);
            return new MultiStatus(Activator.PLUGIN_ID, IStatus.ERROR, msg, null);
          }
        }
      } else {
        // process start failed
        String msg = launcher.getErrorMessage();
        return new MultiStatus(Activator.PLUGIN_ID, IStatus.ERROR, msg, null);
      }
    } catch (JsonSyntaxException ex) {
      // workbench preferences file format error
      log.error("Error loading workbench preferences", ex);
      return new MultiStatus(Activator.PLUGIN_ID, IStatus.ERROR, "Error loading workbench preferences", ex);
    }
  }

  /**
   * Build the environment to invoke cmake with.
   */
  private ArrayList<String> buildEnvironment(IConsole console, Optional<BuildToolKitDefinition> overwritingBtk)
      throws CdtVariableException, CoreException {

    IEnvironmentVariable[] variables = ManagedBuildManager.getEnvironmentVariableProvider().getVariables(config, true);
    ArrayList<String> envList = new ArrayList<>(variables.length);
    if (overwritingBtk.isEmpty()) {
      for (IEnvironmentVariable var : variables) {
        envList.add(var.getName() + "=" + var.getValue()); //$NON-NLS-1$
      }
    } else {
      // PATH is overwritten...
      Predicate<String> mustReplacePATH = n -> false;
      if (Platform.OS_WIN32.equals(Platform.getOS())) {
        // check for windows which has case-insensitive envvar names, e.g. 'pAth'
        mustReplacePATH = n -> "PATH".equalsIgnoreCase(n);
      } else {
        mustReplacePATH = n -> "PATH".equals(n);
      }
      for (IEnvironmentVariable var : variables) {
        final String name = var.getName();
        String value = var.getValue();
        if (mustReplacePATH.test(name)) {
          // replace the value of $PATH with the value specified in the overwriting build tool kit
          BuildToolKitDefinition buildToolKitDefinition = overwritingBtk.get();
          value = CCorePlugin.getDefault().getCdtVariableManager().resolveValue(buildToolKitDefinition.getPath(), "",
              var.getDelimiter(), null);
          try {
            final OutputStream cis = console.getInfoStream();
            String msg = String.format("  Using build tool kit '%s': $%s='%s'\n", buildToolKitDefinition.getName(),
                name, value);
            cis.write(msg.getBytes());
          } catch (IOException ignore) {
          }
        }
        envList.add(name + "=" + value); //$NON-NLS-1$
      }
    }
    return envList;
  }

  /**
   * Build the command-line for cmake. The first argument will be the
   * cmake-command.
   *
   * @throws CoreException
   */
  private List<String> buildCommandline(IPath srcDir, Optional<BuildToolKitDefinition> overwritingBtk)
      throws CoreException {
    // load project properties..
    final ICConfigurationDescription cfgd = ManagedBuildManager.getDescriptionForConfiguration(config);

    boolean needVerboseBuild = false;
    {
      final List<ILanguageSettingsProvider> lsps = ((ILanguageSettingsProvidersKeeper) cfgd)
          .getLanguageSettingProviders();
      // GCCBuildCommandParser wants to see gcc command lines
      needVerboseBuild = lsps.stream().map(lsp -> lsp.getId())
          .filter(id -> "org.eclipse.cdt.managedbuilder.core.GCCBuildCommandParser".equals(id)).findFirst().isPresent();
    }

    List<String> args = new ArrayList<>();
    /* add our defaults first */
    {
      args.add(0, "cmake");
      if (overwritingBtk.isPresent() && overwritingBtk.get().isExternalCmake()) {
        args.set(0, overwritingBtk.get().getExternalCmakeFile());
      }
      // set argument for debug or release build..
      IBuildObjectProperties buildProperties = config.getBuildProperties();
      IBuildProperty property = buildProperties.getProperty(ManagedBuildManager.BUILD_TYPE_PROPERTY_ID);
      if (property != null) {
        IBuildPropertyValue value = property.getValue();
        if (ManagedBuildManager.BUILD_TYPE_PROPERTY_DEBUG.equals(value.getId())) {
          args.add("-DCMAKE_BUILD_TYPE:STRING=Debug");
        } else if (ManagedBuildManager.BUILD_TYPE_PROPERTY_RELEASE.equals(value.getId())) {
          args.add("-DCMAKE_BUILD_TYPE:STRING=Release");
        }
      }
      // colored output during build is useless for build console (seems to affect progress report only)
//      args.add("-DCMAKE_COLOR_MAKEFILE:BOOL=OFF");
      if (needVerboseBuild) {
        // echo commands to the console during the build to give output parsers a chance
        args.add("-DCMAKE_VERBOSE_MAKEFILE:BOOL=ON");
        // speed up build output parsing by disabling progress report msgs
        args.add("-DCMAKE_RULE_MESSAGES:BOOL=OFF");
      }
    }

    /* add workbench preferences */
    {
      IEclipsePreferences wPrefs = PreferenceAccess.getPreferences();
      CmakeGenerator generator = BuildToolKitUtil.getEffectiveCMakeGenerator(wPrefs, overwritingBtk);
      args.add("-G");
      args.add(generator.getCmakeName());
      /* add general settings */
      if (wPrefs.getBoolean(PreferenceAccess.CMAKE_WARN_NO_DEV, false))
        args.add("-Wno-dev");
      if (wPrefs.getBoolean(PreferenceAccess.CMAKE_DBG_TRY_COMPILE, false))
        args.add("--debug-trycompile");
      if (wPrefs.getBoolean(PreferenceAccess.CMAKE_DBG, false))
        args.add("--debug-output");
      if (wPrefs.getBoolean(PreferenceAccess.CMAKE_TRACE, false))
        args.add("--trace");
      if (wPrefs.getBoolean(PreferenceAccess.CMAKE_WARN_UNINITIALIZED, false))
        args.add("--warn-uninitialized");
      if (wPrefs.getBoolean(PreferenceAccess.CMAKE_NO_WARN_UNUSED, false))
        args.add("--no-warn-unused-cli");
      if (!needVerboseBuild && wPrefs.getBoolean(PreferenceAccess.VERBOSE_BUILD, false)) {
        args.add("-DCMAKE_VERBOSE_MAKEFILE=ON");
      }

      String json = wPrefs.get(PreferenceAccess.CMAKE_CACHE_ENTRIES, "[]");
      List<CmakeDefine> entries = PreferenceAccess.toListFromJson(CmakeDefine.class, json);
      appendDefines(args, entries, null);
    }
    /* project settings... */
    {
      final CMakeSettings prefs = ConfigurationManager.getInstance().getOrLoad(cfgd);
      if (prefs.getCacheFile() != null) {
        args.add("-C");
        args.add(prefs.getCacheFile());
      }

      appendDefines(args, prefs.getDefines(), cfgd);
      appendUndefines(args, prefs.getUndefines());

      /* user specified other cmake arguments.. */
      String otherArguments = prefs.getOtherArguments();
      if (otherArguments != null) {
        final ICdtVariableManager mngr = CCorePlugin.getDefault().getCdtVariableManager();
        // handle Unix shell quoting
        QuotedStringTokenizer tokenizer = QuotedStringTokenizer.builder().delimiters(" \t\n\r\f").build();
        for (Iterator<String> iter = tokenizer.tokenize(otherArguments); iter.hasNext();) {
          final String arg = iter.next();
          try {
            args.add(mngr.resolveValue(arg, null, "", cfgd));
          } catch (CdtVariableException ex) {
            args.add(arg);
          }
        }
      }
    }

    // tell cmake to write compile commands to a JSON file
    args.add("-DCMAKE_EXPORT_COMPILE_COMMANDS:BOOL=ON");
    // tell cmake where its script is located..
    args.add(srcDir.toOSString());
    return args;
  }

  /**
   * Appends arguments for the specified cmake undefines.
   *
   * @param args
   *        the list to append cmake-arguments to.
   * @param undefines
   *        the cmake defines to convert and append.
   */
  private static void appendUndefines(List<String> args, final List<CmakeUnDefine> undefines) {
    for (CmakeUnDefine def : undefines) {
      args.add("-U" + def.getName());
    }
  }

  /**
   * Appends arguments for the specified cmake defines. Performs substitutions on variables found in a value of each
   * define.
   *
   * @param args    the list to append cmake-arguments to
   * @param defines the cmake defines to convert and append
   * @param cfgd    the project configuration to use for variable resolution or null to resolve variables from the
   *                workbench
   * @throws CoreException if unable to resolve the value of one or more variables
   */
  private void appendDefines(List<String> args, final List<CmakeDefine> defines, ICConfigurationDescription cfgd)
      throws CoreException {
    final ICdtVariableManager mngr = CCorePlugin.getDefault().getCdtVariableManager();
    for (CmakeDefine def : defines) {
      final StringBuilder sb = new StringBuilder("-D");
      sb.append(def.getName());
      sb.append(':').append(def.getType().getCmakeArg());
      sb.append('=');
      String expanded = mngr.resolveValue(def.getValue(), "", "", cfgd);
      sb.append(expanded);
      args.add(sb.toString());
    }
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#getMakefileName()
   */
  @Override
  public String getMakefileName() {
    IEclipsePreferences prefs = PreferenceAccess.getPreferences();
    try {
      CmakeGenerator generator = BuildToolKitUtil.getEffectiveCMakeGenerator(prefs,
          BuildToolKitUtil.getOverwritingToolkit(prefs));
      return generator.getMakefileName();
    } catch (JsonSyntaxException ex) {
      // file format error
      log.error("Error loading workbench preferences", ex);
    }
    return "makefile";
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#isGeneratedResource(org.eclipse.core.resources.IResource)
   */
  @Override
  public boolean isGeneratedResource(IResource resource) {
    // Is this a generated directory ...
    IPath path = resource.getProjectRelativePath();
    // It is if it is a root of the resource pathname
    return  getRelBuildPath().isPrefixOf(path);
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#generateDependencies()
   */
  @Override
  public void generateDependencies() throws CoreException {
    // nothing to do
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#regenerateDependencies(boolean)
   */
  @Override
  public void regenerateDependencies(boolean force) throws CoreException {
    // nothing to do
  }

  /**
   * Checks whether the build has been cancelled. Cancellation requests are
   * propagated to the caller by throwing
   * {@code OperationCanceledException}.
   *
   * @see org.eclipse.core.runtime.OperationCanceledException#OperationCanceledException()
   */
  protected void checkCancel() {
    if (monitor != null && monitor.isCanceled()) {
      throw new OperationCanceledException();
    }
  }

  protected void updateMonitor(String msg) {
    if (monitor != null && !monitor.isCanceled()) {
      monitor.subTask(msg);
      monitor.worked(1);
    }
  }

  private static void createErrorMarker(IProject project, String message) throws CoreException {
    try {
      IMarker marker = project.createMarker(MARKER_ID);
      marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
      marker.setAttribute(IMarker.MESSAGE, message);
      marker.setAttribute(IMarker.LOCATION, BuildscriptGenerator.class.getName());
    } catch (CoreException ex) {
      // resource is not (yet) known by the workbench
      // ignore
    }
  }

  private static class CMakeListsVisitor implements IResourceDeltaVisitor {
    private boolean hasChanges = false;

    @Override
    public boolean visit(IResourceDelta delta) {
      if (hasChanges) {
        return false; // changes were detected already
      }

      switch (delta.getKind()) {
      case IResourceDelta.CHANGED:
        IResource resource = delta.getResource();
        if (resource.getType() == IResource.FILE && !resource.isDerived(IResource.CHECK_ANCESTORS)) {
          String name = resource.getName();
          if (name.equals("CMakeLists.txt") || name.endsWith(".cmake")) {
            hasChanges= true;
            return false;
          }
        }
      }
      return true;
    }
  }
}
