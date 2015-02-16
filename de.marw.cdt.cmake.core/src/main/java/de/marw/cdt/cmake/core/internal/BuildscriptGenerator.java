/*******************************************************************************
 * Copyright (c) 2013 Martin Weber.
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
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator;
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator2;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
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
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;

import de.marw.cdt.cmake.core.CMakePlugin;
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
  private static final ILog log = CMakePlugin.getDefault().getLog();

  /** CBuildConsole element id */
  private static final String CMAKE_CONSOLE_ID = "de.marw.cdt.cmake.core.cmakeConsole";
  private static final boolean MULTIPLE_SOURCE_DIRS_SUPPORTED = false;

  private IProject project;
  private IProgressMonitor monitor;
  private IFolder buildFolder; // build folder - relative to the project
  private IConfiguration config;
  private IBuilder builder;

  /**   */
  public BuildscriptGenerator() {
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator2#initialize(int, org.eclipse.cdt.managedbuilder.core.IConfiguration, org.eclipse.cdt.managedbuilder.core.IBuilder, org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public void initialize(int buildKind, IConfiguration cfg, IBuilder builder,
      IProgressMonitor monitor) {
    // Save the project so we can get path and member information
    this.project = cfg.getOwner().getProject();
    // Save the monitor reference for reporting back to the user
    this.monitor = monitor;
    // Cache the build tools
    this.config = cfg;
    this.builder = builder;
    initBuildFolder();
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#initialize(org.eclipse.core.resources.IProject, org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo, org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public void initialize(IProject project, IManagedBuildInfo info,
      IProgressMonitor monitor) {
    // 15kts; seems this is never called
    // Save the project so we can get path and member information
    this.project = project;
    // Save the monitor reference for reporting back to the user
    this.monitor = monitor;
    // Cache the build tools
    config = info.getDefaultConfiguration();
    builder = config.getEditableBuilder();
    initBuildFolder();
  }

  private void initBuildFolder() {
    // set the top build dir path for the current configuration
    // TODO MWE allow user to customize the root common to all configs
    IPath buildP = new Path("build").append(config.getName());
    this.buildFolder = project.getFolder(buildP);
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
    // since CDT does weird thing with relative paths
    return buildFolder.getFullPath();
  }

  /**
   * Invoked on incremental build.
   */
  @Override
  public MultiStatus generateMakefiles(IResourceDelta delta)
      throws CoreException {
    /*
     * Let's do a sanity check right now.
     * 
     * If this is an incremental build, so if the build directory is not there,
     * then a full build is needed.
     */
    File buildDir = buildFolder.getLocation().toFile();
    final File cacheFile = new File(buildDir, "CMakeCache.txt");
    if (isGeneratorChanged() && cacheFile.exists()) {
      // The user changed the generator, remove cache file to avoid cmake's complaints..
      cacheFile.delete();
//      System.out.println("DEL "+cacheFile);
      // tell the workspace about file removal
      buildFolder.getFile("CMakeCache.txt").refreshLocal(IResource.DEPTH_ZERO,
          monitor);
    }
    final File makefile = new File(buildDir, getMakefileName());
    if (!buildDir.exists() || !cacheFile.exists() || !makefile.exists()) {
      return regenerateMakefiles();
    }

    if (false) {
      // Visit the CMakeLists.txt in the delta and detect whether to regenerate
      // normally, the cmake-generated makefiles detect changes in CMake scripts
      // and regenerate the makefiles. But this seems not to be the case, when
      // someone writes CTests in a CMakeLists.txt.
      // So regenerate the makefiles...
      final CMakelistsVisitor visitor = new CMakelistsVisitor();
      updateMonitor("Visiting CMakeLists.txt");
      delta.accept(visitor);
      // TODO detect removed/renamed source dir.. see GnuMakefileGenerator.ResourceDeltaVisitor
      if (visitor.isCmakelistsAffected()) {
        return regenerateMakefiles();
      }
    }
    MultiStatus status = new MultiStatus(CMakePlugin.PLUGIN_ID, IStatus.OK, "",
        null);
    return status;
  }

  /**
   * Invoked on full build.
   */
  @Override
  public MultiStatus regenerateMakefiles() throws CoreException {
    MultiStatus status; // Return value

    // See if the user has cancelled the build
    checkCancel();

    ICSourceEntry[] srcEntries = config.getSourceEntries();
    if (!MULTIPLE_SOURCE_DIRS_SUPPORTED && srcEntries.length > 1) {
      final String msg = "Only a single source location supported by CMake";
      updateMonitor(msg);
      status = new MultiStatus(CMakePlugin.PLUGIN_ID, IStatus.ERROR, "", null);
      status
          .add(new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID, 0, msg, null));
      return status;
    }

    if (srcEntries.length == 0) {
      // no source folders specified in project
      // Make sure there is something to build
      String msg = "No source directories in project " + project.getName();
      updateMonitor(msg);
      status = new MultiStatus(CMakePlugin.PLUGIN_ID, IStatus.INFO, "", null);
      status.add(new Status(IStatus.INFO, CMakePlugin.PLUGIN_ID,
          IManagedBuilderMakefileGenerator.NO_SOURCE_FOLDERS, msg, null));
      return status;
    } else {
      ICConfigurationDescription cfgDes = ManagedBuildManager
          .getDescriptionForConfiguration(config);
      srcEntries = CDataUtil.resolveEntries(srcEntries, cfgDes);
    }

    status = new MultiStatus(CMakePlugin.PLUGIN_ID, IStatus.OK, "", null);

    final IConsole console = CCorePlugin.getDefault().getConsole(
        CMAKE_CONSOLE_ID);
    console.start(project);

    // create makefiles, assuming each source directory contains a CMakeLists.txt
    for (int i = 0; i < srcEntries.length; i++) {
      ICSourceEntry srcEntry = srcEntries[i];
      updateMonitor("Invoking CMake for " + srcEntry.getName());
      try {
        final ConsoleOutputStream cis = console.getInfoStream();
        cis.write(SimpleDateFormat.getTimeInstance().format(new Date())
            .getBytes());
        cis.write(" **** Buildscript generation of configuration ".getBytes());
        cis.write(config.getName().getBytes());
        cis.write(" for project ".getBytes());
        cis.write(project.getName().getBytes());
        cis.write("\n".getBytes());
      } catch (IOException ignore) {
      }
      final IPath srcPath = srcEntry.getFullPath(); // project relative
      // Create the top-level directory for the build output
//      createFolder(MULTIPLE_SOURCE_DIRS_SUPPORTED ? buildFolder
//          .getFolder(srcPath) : buildFolder);
      createFolder(buildFolder);

      IPath srcDir;
      if (srcPath.isEmpty()) {
        // source folder is project folder
        srcDir = project.getLocation();
      } else {
        // source folder is a folder below project
        srcDir = project.getFolder(srcPath).getLocation();
      }

      checkCancel();
      final IPath buildDirAbs = buildFolder.getLocation();
      MultiStatus status2 = invokeCMake(srcDir, buildDirAbs, console);
      // tell the workspace that this file exists
      buildFolder.getFile("CMakeCache.txt").refreshLocal(IResource.DEPTH_ZERO,
          monitor);

      // NOTE: Commonbuilder reads getCode() to detect errors, not getSeverity()
      if (status2.getCode() == IStatus.ERROR) {
        // failed to generate
        return status2;
      }
// nutzlos:      status= status2; // return last success status
    }

    return status;
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
        folder.create(IResource.DERIVED | IResource.FORCE, true, monitor);
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
  private MultiStatus invokeCMake(IPath srcDir, IPath buildDir, IConsole console)
      throws CoreException {
    Assert.isLegal(srcDir.isAbsolute(), "srcDir");
    Assert.isLegal(buildDir.isAbsolute(), "buildDir");

    MultiStatus status; // Return value

    String errMsg = null;
    final List<String> argList = buildCommandline(srcDir);
    // extract cmake command
    final String cmd = argList.get(0);
    argList.remove(0);
    // run cmake..
    final ICommandLauncher launcher = builder.getCommandLauncher();
    launcher.showCommand(true);
    final Process proc = launcher.execute(new Path(cmd),
        argList.toArray(new String[argList.size()]), null, buildDir, monitor);
    if (proc != null) {
      try {
        // Close the input of the process since we will never write to it
        proc.getOutputStream().close();
      } catch (IOException e) {
      }
      int state = launcher.waitAndRead(console.getOutputStream(), console
          .getErrorStream(), new SubProgressMonitor(monitor,
          IProgressMonitor.UNKNOWN));
      if (state == ICommandLauncher.COMMAND_CANCELED) {
        throw new OperationCanceledException(launcher.getErrorMessage());
      }

      // check cmake exit status
      final int exitValue = proc.exitValue();
      if (exitValue == 0) {
        // success
        status = new MultiStatus(CMakePlugin.PLUGIN_ID, IStatus.OK, null, null);
      } else {
        // cmake had errors...
        errMsg = String.format(
            "%1$s exited with status %2$d. See CMake console for details.",
            cmd, exitValue);
        status = new MultiStatus(CMakePlugin.PLUGIN_ID, IStatus.ERROR, errMsg,
            null);
      }
    } else {
      // process start failed
      errMsg = launcher.getErrorMessage();
      status = new MultiStatus(CMakePlugin.PLUGIN_ID, IStatus.ERROR, errMsg,
          null);
    }

    return status;
  }

  /**
   * Build the command-line for cmake. The first argument will be the
   * cmake-command.
   * 
   * @throws CoreException
   */
  private List<String> buildCommandline(IPath srcDir) throws CoreException {
    // load project properties..
    final ICConfigurationDescription cfgd = ManagedBuildManager
        .getDescriptionForConfiguration(config);
    final CMakePreferences prefs = ConfigurationManager.getInstance()
        .getOrLoad(cfgd);

    List<String> args = new ArrayList<String>();
    /* add our defaults first */
    {
      // default for all OSes
      args.add("cmake");
      // set argument for debug or release build..
      IBuildObjectProperties buildProperties = config.getBuildProperties();
      IBuildProperty property = buildProperties
          .getProperty(ManagedBuildManager.BUILD_TYPE_PROPERTY_ID);
      if (property != null) {
        IBuildPropertyValue value = property.getValue();
        if (ManagedBuildManager.BUILD_TYPE_PROPERTY_DEBUG.equals(value.getId())) {
          args.add("-DCMAKE_BUILD_TYPE:STRING=Debug");
        } else if (ManagedBuildManager.BUILD_TYPE_PROPERTY_RELEASE.equals(value
            .getId())) {
          args.add("-DCMAKE_BUILD_TYPE:STRING=Release");
        }
      }
      // colored output during build is useless for build console (seems to affect rogress report only)
//      args.add("-DCMAKE_COLOR_MAKEFILE:BOOL=OFF");
      // speed up build output parsing by disabling progress report msgs
      args.add("CMAKE_RULE_MESSAGES:BOOL=OFF");
      // echo commands to the console during the make to give output parsers a chance
      args.add("-DCMAKE_VERBOSE_MAKEFILE:BOOL=ON");
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

    appendDefines(args, prefs.getDefines());
    appendUndefines(args, prefs.getUndefines());

    /* add settings for the operating system we are running under */
    final AbstractOsPreferences osPrefs = AbstractOsPreferences
        .extractOsPreferences(prefs);
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
  private boolean isGeneratorChanged() {
    // load project properties..
    final ICConfigurationDescription cfgd = ManagedBuildManager
        .getDescriptionForConfiguration(config);
    CMakePreferences prefs;
    try {
      prefs = ConfigurationManager.getInstance().getOrLoad(cfgd);
      AbstractOsPreferences osPrefs = AbstractOsPreferences
          .extractOsPreferences(prefs);
      return osPrefs.getGenerator() != osPrefs.getGeneratedWith();
    } catch (CoreException ex) {
      log.log(new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID, null, ex));
      return false;
    }
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
  private static void appendAbstractOsPreferences(List<String> args,
      final AbstractOsPreferences prefs) throws CoreException {
    // replace cmake command, if given
    if (!prefs.getUseDefaultCommand())
      args.set(0, prefs.getCommand());
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
  private static void appendUndefines(List<String> args,
      final List<CmakeUnDefine> undefines) {
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
  private static void appendDefines(List<String> args,
      final List<CmakeDefine> defines) throws CoreException {
    final IStringVariableManager varMgr = VariablesPlugin.getDefault()
        .getStringVariableManager();
    for (CmakeDefine def : defines) {
      final StringBuilder sb = new StringBuilder("-D");
      sb.append(def.getName());
      sb.append(':').append(def.getType().getCmakeArg());
      sb.append('=');
      String expanded = varMgr.performStringSubstitution(def.getValue(), false);
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
    final ICConfigurationDescription cfgd = ManagedBuildManager
        .getDescriptionForConfiguration(config);
    CMakePreferences prefs;
    try {
      prefs = ConfigurationManager.getInstance().getOrLoad(cfgd);
    } catch (CoreException ex) {
      // Auto-generated catch block
      ex.printStackTrace();
      return "Makefile"; // default
    }
    AbstractOsPreferences osPrefs = AbstractOsPreferences
        .extractOsPreferences(prefs);
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
    if (buildFolder.getFullPath().isPrefixOf(path))
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

}
