/* ******************************************************************************
 * Copyright (c) 2013-2018 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core.internal;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ConsoleOutputStream;
import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.cdt.core.cdtvariables.CdtVariableException;
import org.eclipse.cdt.core.cdtvariables.ICdtVariableManager;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
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
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import de.marw.cdt.cmake.core.CdtPlugin;
import de.marw.cdt.cmake.core.internal.settings.AbstractOsPreferences;
import de.marw.cdt.cmake.core.internal.settings.CMakePreferences;
import de.marw.cdt.cmake.core.internal.settings.CmakeDefine;
import de.marw.cdt.cmake.core.internal.settings.CmakeUnDefine;
import de.marw.cdt.cmake.core.internal.settings.ConfigurationManager;

/**
 * Generates makefiles and other build scripts from CMake scripts
 * (CMakeLists.txt).
 *
 * @author Martin Weber
 */
public class BuildscriptGenerator implements IManagedBuilderMakefileGenerator2 {
  private static final ILog log = CdtPlugin.getDefault().getLog();

  /** CBuildConsole element id */
  private static final String CMAKE_CONSOLE_ID = "de.marw.cdt.cmake.core.cmakeConsole";
  /** buildscript generation error marker ID */
  private static final String MARKER_ID = CdtPlugin.PLUGIN_ID + ".BuildscriptGenerationError";

  private IProject project;
  private IProgressMonitor monitor;
  private IConfiguration config;
  private IBuilder builder;
  /** build folder - relative to the project. Lazily instantiated */
  private IFolder buildFolder;

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

  private IFolder getBuildFolder() {
    // set the top build dir path for the current configuration
    String buildDirStr = null;
    final ICConfigurationDescription cfgd = ManagedBuildManager.getDescriptionForConfiguration(config);
    try {
      CMakePreferences prefs = ConfigurationManager.getInstance().getOrLoad(cfgd);
      buildDirStr = prefs.getBuildDirectory();
    } catch (CoreException e) {
      // storage base is null; treat as bug in CDT..
      log.log(new Status(IStatus.ERROR, CdtPlugin.PLUGIN_ID,
          "falling back to hard coded build directory", e));
    }
    IPath buildP;
    if (buildDirStr == null) {
      // not configured: fall back to legacy behavior
      buildP = new Path("build").append(config.getName());
    } else {
      buildP = new Path(buildDirStr);
    }

    // Note that IPath from ICBuildSetting#getBuilderCWD() holding variables is mis-constructed,
    // i.e. ${workspace_loc:/path} gets split into 2 path segments.
    // MBS does that and we need to handle that

    // So resolve variables here and return an workspace relative path to not give CTD a chance to garble it up..
    ICdtVariableManager mngr = CCorePlugin.getDefault().getCdtVariableManager();
    try {
      String buildPathString = buildP.toString();
      buildPathString = mngr.resolveValue(buildPathString, "", "",
          ManagedBuildManager.getDescriptionForConfiguration(config));
      buildP= new Path(buildPathString);
    } catch (CdtVariableException e) {
      log.log(new Status(IStatus.ERROR, CdtPlugin.PLUGIN_ID,
          "variable expansion for build directory failed", e));
    }

    buildFolder = project.getFolder(buildP);
    return buildFolder;
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#getBuildWorkingDir()
   */
  @Override
  public IPath getBuildWorkingDir() {
    // Note that IPath from ICBuildSetting#getBuilderCWD() holding variables is mis-constructed,
    // i.e. ${workspace_loc:/path} gets split into 2 path segments, if we
    // return a relative path here.

    // So return workspace path (absolute) or absolute file system path,
    // since CDT Builder#getDefaultBuildPath() does weird thing with relative paths
    return getBuildFolder().getFullPath();
  }

  /**
   * Invoked on incremental build.
   */
  @Override
  public MultiStatus generateMakefiles(IResourceDelta delta) throws CoreException {
    return regenerateMakefiles();
  }

  /**
   * Invoked on full build.
   */
  @Override
  public MultiStatus regenerateMakefiles() throws CoreException {

    project.deleteMarkers(MARKER_ID, false, IResource.DEPTH_ZERO);

    final ICConfigurationDescription cfgDes = ManagedBuildManager.getDescriptionForConfiguration(config);

    ICSourceEntry[] srcEntries = config.getSourceEntries();
    { // do a sanity check: only one source entry allowed for project
      if (srcEntries.length == 0) {
        // no source folders specified in project
        final String msg = "No source directories configured for project";
        MultiStatus status = new MultiStatus(CdtPlugin.PLUGIN_ID, IStatus.ERROR, msg + " " + project.getName(), null);
        createErrorMarker(project, msg);
        return status;
      } else if (srcEntries.length > 1) {
        final String msg = "Only a single source directory is supported";
        MultiStatus status = new MultiStatus(CdtPlugin.PLUGIN_ID, IStatus.ERROR, msg, null);
        // if multiple projects are build, this information is lost when a new console is opened.
        // So create a problem marker to show up in the problem view
        createErrorMarker(project, msg);
        return status;
      } else {
        srcEntries = CDataUtil.resolveEntries(srcEntries, cfgDes);
      }
    }

    // load project properties..
    final CMakePreferences prefs = ConfigurationManager.getInstance().getOrLoad(cfgDes);

    // See if the user has cancelled the build
    checkCancel();

    boolean mustGenerate= false;

    final IFolder buildFolder = getBuildFolder();
    final File buildDir = buildFolder.getLocation().toFile();
    final File cacheFile = new File(buildDir, "CMakeCache.txt");
    if ((isGeneratorChanged(prefs) || prefs.isClearCache()) && cacheFile.exists()) {
      // The user changed the generator, remove cache file to avoid cmake's complaints..
      cacheFile.delete();
//      System.out.println("DEL "+cacheFile);
      // tell the workspace about file removal
      buildFolder.getFile("CMakeCache.txt").refreshLocal(IResource.DEPTH_ZERO, monitor);
    }
    final File makefile = new File(buildDir, getMakefileName());
    if (!buildDir.exists() || !cacheFile.exists() || !makefile.exists()) {
      mustGenerate= true;
    }

    if( !mustGenerate){
      return new MultiStatus(CdtPlugin.PLUGIN_ID, IStatus.OK, "", null);
    }

    // Create the top-level directory for the build output
    createFolder(buildFolder);
    /*
     * the make sure the directory REALLY exists in the file system. If it
     * doesn't, the (buggy?) ICommandLauncher instance will create will cd to
     * the current working directory, which is plainly wrong, NOTE:
     * resource.refreshLocal() does not work here.
     */
    buildDir.mkdirs();

    // See if the user has cancelled the build
    checkCancel();

    final IConsole console = CCorePlugin.getDefault().getConsole(CMAKE_CONSOLE_ID);
    console.start(project);

    // create makefile, assuming the first source directory contains a
    // CMakeLists.txt
    final ICSourceEntry srcEntry = srcEntries[0]; // project relative
    updateMonitor("Execute CMake for " + project.getName());
    try {
      final ConsoleOutputStream cis = console.getInfoStream();
      cis.write(SimpleDateFormat.getTimeInstance().format(new Date()).getBytes());
      cis.write(" Buildscript generation: ".getBytes());
      cis.write(project.getName().getBytes());
      cis.write("::".getBytes());
      cis.write(config.getName().getBytes());
      cis.write(" in ".getBytes());
      cis.write(buildDir.getAbsolutePath().getBytes());
      cis.write("\n".getBytes());
    } catch (IOException ignore) {
    }
    final IPath srcPath = srcEntry.getFullPath();
    IResource srcDir = srcPath.isEmpty() ? project : project.getFolder(srcPath);

    checkCancel();
    MultiStatus status = invokeCMake(srcDir.getLocation(), buildFolder.getLocation(), console);
    // NOTE: Commonbuilder reads getCode() to detect errors, not getSeverity()
    if (status.getCode() == IStatus.ERROR) {
      // failed to generate
      // if multiple projects are build, this information is lost when the a new console is opened.
      // So create a problem marker to show up in the problem view
      createErrorMarker(project, status.getMessage());
      return status;
    }

    return new MultiStatus(CdtPlugin.PLUGIN_ID, IStatus.OK, "", null);
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
   * @param buildDir
   *        abs. path
   * @param srcDir
   * @return a MultiStatus object, where .getCode() return the severity
   * @throws CoreException
   */
  private MultiStatus invokeCMake(IPath srcDir, IPath buildDir, IConsole console) throws CoreException {
    Assert.isLegal(srcDir.isAbsolute(), "srcDir");
    Assert.isLegal(buildDir.isAbsolute(), "buildDir");

    String errMsg;
    final List<String> argList = buildCommandline(srcDir);
    // extract cmake command
    final String cmd = argList.remove(0);
    // Set the environment
    IEnvironmentVariable[] variables = ManagedBuildManager.getEnvironmentVariableProvider().getVariables(config, true);
    String[] envp = null;
    ArrayList<String> envList = new ArrayList<>();
    if (variables != null) {
      for (int i = 0; i < variables.length; i++) {
        envList.add(variables[i].getName() + "=" + variables[i].getValue()); //$NON-NLS-1$
      }
      envp = envList.toArray(new String[envList.size()]);
    }
    // run cmake..
    final ICommandLauncher launcher = builder.getCommandLauncher();
    launcher.setProject(project); // 9.4++ versions of CDT require this for docker
    launcher.showCommand(true);
    final Process proc =
        launcher.execute(new Path(cmd), argList.toArray(new String[argList.size()]), envp, buildDir, monitor);
    if (proc != null) {
      try {
        // Close the input of the process since we will never write to it
        proc.getOutputStream().close();
      } catch (IOException e) {
      }
      int state = launcher.waitAndRead(console.getOutputStream(), console.getErrorStream(),
          new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
      if (state == ICommandLauncher.COMMAND_CANCELED) {
        throw new OperationCanceledException(launcher.getErrorMessage());
      }

      // check cmake exit status
      final int exitValue = proc.exitValue();
      if (exitValue == 0) {
        // success
        return new MultiStatus(CdtPlugin.PLUGIN_ID, IStatus.OK, null, null);
      } else {
        // cmake had errors...
        errMsg = String.format("%1$s exited with status %2$d. See CDT global build console for details.", cmd, exitValue);
        return new MultiStatus(CdtPlugin.PLUGIN_ID, IStatus.ERROR, errMsg, null);
      }
    } else {
      // process start failed
      errMsg = launcher.getErrorMessage();
      return new MultiStatus(CdtPlugin.PLUGIN_ID, IStatus.ERROR, errMsg, null);
    }
  }

  /**
   * Build the command-line for cmake. The first argument will be the
   * cmake-command.
   *
   * @throws CoreException
   */
  private List<String> buildCommandline(IPath srcDir) throws CoreException {
    // load project properties..
    final ICConfigurationDescription cfgd = ManagedBuildManager.getDescriptionForConfiguration(config);

    boolean needExportComileCommands = true;
    boolean needVerboseBuild = false;
    {
      final List<ILanguageSettingsProvider> lsps =
          ((ILanguageSettingsProvidersKeeper) cfgd).getLanguageSettingProviders();
      for (ILanguageSettingsProvider lsp : lsps) {
        if (!needVerboseBuild && ("org.eclipse.cdt.managedbuilder.core.GCCBuildCommandParser".equals(lsp.getId())
            || "de.marw.cmake.cdt.language.settings.providers.CmakeBuildOutputParser".equals(lsp.getId()))) {
          needVerboseBuild = true;
          continue;
        }
      }
    }

    final CMakePreferences prefs = ConfigurationManager.getInstance().getOrLoad(cfgd);

    List<String> args = new ArrayList<>();

    /* add our defaults first */
    {
      // default for all OSes
      args.add("cmake");
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
        // echo commands to the console during the make to give output parsers a chance
        args.add("-DCMAKE_VERBOSE_MAKEFILE:BOOL=ON");
        // speed up build output parsing by disabling progress report msgs
        args.add("-DCMAKE_RULE_MESSAGES:BOOL=OFF");
      }
      // tell cmake to write compile commands to a JSON file
      if (needExportComileCommands) {
        args.add("-DCMAKE_EXPORT_COMPILE_COMMANDS:BOOL=ON");
      }
    }

    /* add general settings */
    if (prefs.isWarnNoDev())
      args.add("-Wno-dev");
    if (prefs.isDebugTryCompile())
      args.add("--debug-trycompile");
    if (prefs.isDebugOutput())
      args.add("--debug-output");
    if (prefs.isTrace())
      args.add("--trace");
    if (prefs.isWarnUnitialized())
      args.add("--warn-unitialized");
    if (prefs.isWarnUnused())
      args.add("--warn-unused");
    if (prefs.getCacheFile() != null) {
      args.add("-C");
      args.add(prefs.getCacheFile());
    }

    appendDefines(args, prefs.getDefines());
    appendUndefines(args, prefs.getUndefines());

    /* add settings for the operating system we are running under */
    final AbstractOsPreferences osPrefs = AbstractOsPreferences.extractOsPreferences(prefs);
    appendAbstractOsPreferences(args, osPrefs);
    // TODO (does not belong here) remember last generator
    osPrefs.setGeneratedWith(osPrefs.getGenerator());

    // tell cmake where its script is located..
    args.add(srcDir.toOSString());

    return args;
  }

  /**
   * Gets whether the user changed the generator setting in the preferences.
   *
   * @return {@code true} if the user changed the generator setting in the
   *         preferences, otherwise {@code false}
   */
  private boolean isGeneratorChanged(CMakePreferences prefs) {
    AbstractOsPreferences osPrefs = AbstractOsPreferences.extractOsPreferences(prefs);
    return osPrefs.getGenerator() != osPrefs.getGeneratedWith();
  }

  /**
   * Appends arguments common to all OS preferences. The first argument in the
   * list will be replaced by the cmake command from the specified preferences,
   * if given.
   *
   * @param args
   *        the list to append cmake-arguments to.
   * @param prefs
   *        the generic OS preferences to convert and append.
   * @throws CoreException
   *         if unable to resolve the value of one or more variables
   */
  private void appendAbstractOsPreferences(List<String> args, final AbstractOsPreferences prefs)
      throws CoreException {
    // replace cmake command, if given
    if (!prefs.getUseDefaultCommand()){
      ICdtVariableManager varManager = CCorePlugin.getDefault().getCdtVariableManager();
      String cmd = varManager.resolveValue(prefs.getCommand(), "<undefined variable here, but CDT does not allow"
          + " to pass it unexpanded; thus its name is lost>"
          , null,
          ManagedBuildManager.getDescriptionForConfiguration(config));
      args.set(0, cmd);
    }
    args.add("-G");
    final CmakeGenerator generator = prefs.getGenerator();
    args.add(generator.getCmakeName());

    appendDefines(args, prefs.getDefines());
    appendUndefines(args, prefs.getUndefines());
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
   * Appends arguments for the specified cmake defines. Performs substitutions
   * on variables found in a value of each define.
   *
   * @param args
   *        the list to append cmake-arguments to.
   * @param defines
   *        the cmake defines to convert and append.
   * @throws CoreException
   *         if unable to resolve the value of one or more variables
   */
  private void appendDefines(List<String> args, final List<CmakeDefine> defines) throws CoreException {
    final ICdtVariableManager mngr = CCorePlugin.getDefault().getCdtVariableManager();
    final ICConfigurationDescription cfgd = ManagedBuildManager.getDescriptionForConfiguration(config);
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
    // load project properties..
    final ICConfigurationDescription cfgd = ManagedBuildManager.getDescriptionForConfiguration(config);
    CMakePreferences prefs;
    try {
      prefs = ConfigurationManager.getInstance().getOrLoad(cfgd);
    } catch (CoreException ex) {
      // Auto-generated catch block
      ex.printStackTrace();
      return "Makefile"; // default
    }
    AbstractOsPreferences osPrefs = AbstractOsPreferences.extractOsPreferences(prefs);
    // file generated by cmake
    return osPrefs.getGenerator().getMakefileName();
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#isGeneratedResource(org.eclipse.core.resources.IResource)
   */
  @Override
  public boolean isGeneratedResource(IResource resource) {
    // Is this a generated directory ...
    IPath path = resource.getProjectRelativePath();
    // It is if it is a root of the resource pathname
    if (getBuildFolder().getFullPath().isPrefixOf(path))
      return true;

    return false;
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
   * <code>OperationCanceledException</code>.
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
    } catch (CoreException ex) {
      // resource is not (yet) known by the workbench
      // ignore
    }
  }
}
