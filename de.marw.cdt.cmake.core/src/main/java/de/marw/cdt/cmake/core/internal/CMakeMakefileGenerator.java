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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.CCorePlugin;
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
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator2;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
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
import de.marw.cdt.cmake.core.internal.settings.LinuxPreferences;
import de.marw.cdt.cmake.core.internal.settings.WindowsPreferences;

/**
 * Generates unix makefiles from CMake scripts (CMakeLists.txt).
 *
 * @author Martin Weber
 */
public class CMakeMakefileGenerator implements
    IManagedBuilderMakefileGenerator2 {

  /** CBuildConsole element id */
  private static final String CMAKE_CONSOLE_ID = "de.marw.cdt.cmake.core.cmakeConsole";
  private static final boolean MULTIPLE_SOURCE_DIRS_SUPPORTED = false;

  private IProject project;
  private IProgressMonitor monitor;
  private IPath topBuildDir; //  Build directory - relative to the project
  private IConfiguration config;
  private IBuilder builder;

  /**
   *
   */
  public CMakeMakefileGenerator() {
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

    // set the top build dir path for the current configuration
    // TODO MWE allow user to customize the root common to all configs
    final IPath binPath = new Path("build").append(cfg.getName());
    topBuildDir = project.getFolder(binPath).getProjectRelativePath();

  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#getBuildWorkingDir()
   */
  @Override
  public IPath getBuildWorkingDir() {
    return topBuildDir;
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#generateMakefiles(org.eclipse.core.resources.IResourceDelta)
   */
  @Override
  public MultiStatus generateMakefiles(IResourceDelta delta)
      throws CoreException {
    /*
     * Let's do a sanity check right now.
     *
     * This is an incremental build, so if the build directory is not there,
     * then a rebuild is needed.
     */
    IFolder folder = project.getFolder(getBuildWorkingDir());

    // check for files generated by cmake
    if (!folder.exists() || !folder.getFile("CMakeCache.txt").exists()
        || !folder.getFile("Makefile").exists()) {
      // cmake did never run on the build directory
      return regenerateMakefiles();
    }

    // Visit the CMakeLists.txt in the delta and detect whether to regenerate
    final CMakelistsVisitor visitor = new CMakelistsVisitor();
    updateMonitor("Visiting CMakeLists.txt");
    delta.accept(visitor);
    // TODO detect removed/renamed source dirs.. see GnuMakefileGenerator.ResourceDeltaVisitor
    if (visitor.isCmakelistsAffected()) {
      // normally, the cmake-generated makefiles detect changes in CMake scripts
      // and regenerate the makefiles. But this seems not to be the case, when
      // someone writes CTests in a CMakeLists.txt.
      // So regenerate the makefiles...
//      return regenerateMakefiles();
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
      status.add(new Status(IStatus.INFO, CMakePlugin.PLUGIN_ID, 0, msg, null));
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
        console
            .getInfoStream()
            .write(
                ("Buildfile generation for configuration " + config.getName() + " \n")
                    .getBytes());
      } catch (IOException ex) {
        // ignore
      }
      final IPath srcPath = srcEntry.getFullPath(); // project relative
      // Create the top-level directory for the build output
      final IPath cfgBuildPath = createFolder(MULTIPLE_SOURCE_DIRS_SUPPORTED ? topBuildDir
          .append(srcPath) : topBuildDir);
      final IPath buildDir = project.getFolder(cfgBuildPath).getLocation();

      IPath srcDir;
      if (srcPath.isEmpty()) {
        // source folder is project folder
        srcDir = project.getLocation();
      } else {
        // source folder ist folder below project
        srcDir = project.getFolder(srcPath).getLocation();
      }

      checkCancel();
      MultiStatus status2 = invokeCMake(srcDir, buildDir, console);
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
   * Return or create the folder needed for the build output. If we are creating
   * the folder, set the derived bit to true so the CM system ignores the
   * contents. If the resource exists, respect the existing derived setting.
   *
   * @param path
   *        a path, relative to the project root
   * @return the project relative path of the created folder
   */
  private IPath createFolder(IPath path) throws CoreException {
    IFolder folder = project.getFolder(path);

    if (!folder.exists()) {
      // Make sure that parent folders exist
      IPath parentPath = path.removeLastSegments(1);
      // Assume that the parent exists if the path is empty
      if (!parentPath.isEmpty()) {
        IFolder parent = project.getFolder(parentPath);
        if (!parent.exists()) {
          createFolder(parentPath);
        }
      }

      // Now make the requested folder
      try {
        folder.create(true, true, null);
      } catch (CoreException e) {
        if (e.getStatus().getCode() == IResourceStatus.PATH_OCCUPIED)
          folder.refreshLocal(IResource.DEPTH_ZERO, monitor);
        else
          throw e;
      }

      // Make sure the folder is marked as derived so it is not added to CM
      if (!folder.isDerived()) {
        folder.setDerived(true, monitor);
      }
    }

    return folder.getProjectRelativePath();
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
      String cmd = "cmake"; // default for all OSes
      args.add(cmd);
      // colored output during build is useless for build console
      args.add("-DCMAKE_COLOR_MAKEFILE:BOOL=OFF");
      // echo commands to the console during the make to give output parser a chance
      args.add("-DCMAKE_VERBOSE_MAKEFILE:BOOL=ON");

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
    }

    /* add general settings */
    {
      if (prefs.isWarnNoDev())
        args.add("-Wno-dev");
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
    }

    /* add settings for the operating system we are running under */
    final String os = Platform.getOS();
    if (Platform.OS_WIN32.equals(os)) {
      WindowsPreferences osPrefs = prefs.getWindowsPreferences();
      appendAbstractOsPreferences(args, osPrefs);
    } else {
      // fall back to linux, if OS is unknown
      LinuxPreferences osPrefs = prefs.getLinuxPreferences();
      appendAbstractOsPreferences(args, osPrefs);
    }

    // tell cmake where its script is located..
    args.add(srcDir.toOSString());

    return args;
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
    // file is generated by cmake
    return "Makefile";
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#initialize(org.eclipse.core.resources.IProject, org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo, org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public void initialize(IProject project, IManagedBuildInfo info,
      IProgressMonitor monitor) {
//    System.out.println("# in CMakeMakefileGenerator.initialize()");
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#isGeneratedResource(org.eclipse.core.resources.IResource)
   */
  @Override
  public boolean isGeneratedResource(IResource resource) {
    // Is this a generated directory ...
    IPath path = resource.getProjectRelativePath();
    IPath root = new Path("build");
    // It is if it is a root of the resource pathname
    if (root.isPrefixOf(path))
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
