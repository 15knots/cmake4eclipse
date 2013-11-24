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
import org.eclipse.cdt.core.ConsoleOutputStream;
import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
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
import org.eclipse.core.runtime.SubProgressMonitor;

import de.marw.cdt.cmake.core.CMakePlugin;

/**
 * Generates unix makefiles from CMake scripts (CMakeLists.txt).
 *
 * @author Martin Weber
 */
public class CMakeMakefileGenerator implements
    IManagedBuilderMakefileGenerator2 {

  private static final boolean MULTIPLE_SOURCE_DIRS_SUPPORTED = false;
  private IProject project;
  private IProgressMonitor monitor;
  private IPath topBuildDir; //  Build directory - relative to the project
  private IConfiguration config;
  private IBuilder builder;
  private ICSourceEntry[] srcEntries;

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
    // TODO MWE allow to customize the root common to all configs
    final IPath binPath = new Path("build").append(cfg.getName());
    topBuildDir = project.getFolder(binPath).getProjectRelativePath();

    srcEntries = config.getSourceEntries();
    if (srcEntries.length == 0) {
      // no source folders specified in project: assume project root as source folder
      srcEntries = new ICSourceEntry[] { new CSourceEntry(Path.EMPTY, null,
          ICSettingEntry.RESOLVED | ICSettingEntry.VALUE_WORKSPACE_PATH) };
    } else {
      ICConfigurationDescription cfgDes = ManagedBuildManager
          .getDescriptionForConfiguration(config);
      srcEntries = CDataUtil.resolveEntries(srcEntries, cfgDes);
    }
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
    if (visitor.isCmakelistsAffected()) {
      // normally, the cmake-generated makefiles detect changes in CMake scripts
      // and regenerate the makefiles. But this seems not to be the case, when
      // someone writes CTests in a CMakeLists.txt.
      // So regenerate the makefiles...
      return regenerateMakefiles();
    }

    MultiStatus status = new MultiStatus(CMakePlugin.PLUGIN_ID, IStatus.OK,
        new String(), null);
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

    if (!MULTIPLE_SOURCE_DIRS_SUPPORTED && srcEntries.length > 1) {
      return new MultiStatus(CMakePlugin.PLUGIN_ID, IStatus.ERROR,
          "Only a single source location supported by CMake", null);
    }

    // create makefiles, assuming each source directory contains a CMakeLists.txt
    for (int i = 0; i < srcEntries.length; i++) {
      ICSourceEntry srcEntry = srcEntries[i];

      final IPath srcPath = srcEntry.getFullPath(); // project relative
      // Create the top-level directory for the build output
      final IPath cfgBuildPath = createDirectory(MULTIPLE_SOURCE_DIRS_SUPPORTED ? topBuildDir
          .append(srcPath) : topBuildDir);
      final IPath srcDir = project.getFolder(srcPath).getLocation();
      final IPath buildDir = project.getFolder(cfgBuildPath).getLocation();
      checkCancel();
      MultiStatus status2 = invokeCMake(srcDir, buildDir, monitor);
      if (status2.getSeverity() == IStatus.ERROR) {
        // failed to generate
        return status2;
      }

    }

    status = new MultiStatus(CMakePlugin.PLUGIN_ID, IStatus.OK, "", null);
    return status;
  }

  /**
   * Return or create the folder needed for the build output. If we are creating
   * the folder, set the derived bit to true so the CM system ignores the
   * contents. If the resource exists, respect the existing derived setting.
   *
   * @param path
   *        a path, relative to the project root
   * @return the project relative pat of the created folder
   */
  private IPath createDirectory(IPath path) throws CoreException {
    IFolder folder = project.getFolder(path);

    if (!folder.exists()) {
      // Make sure that parent folders exist
      IPath parentPath = path.removeLastSegments(1);
      // Assume that the parent exists if the path is empty
      if (!parentPath.isEmpty()) {
        IFolder parent = project.getFolder(parentPath);
        if (!parent.exists()) {
          createDirectory(parentPath);
        }
      }

      // Now make the requested folder
      try {
        folder.create(true, true, null);
      } catch (CoreException e) {
        if (e.getStatus().getCode() == IResourceStatus.PATH_OCCUPIED)
          folder.refreshLocal(IResource.DEPTH_ZERO, null);
        else
          throw e;
      }

      // Make sure the folder is marked as derived so it is not added to CM
      if (!folder.isDerived()) {
        folder.setDerived(true, null);
      }
    }

    return folder.getProjectRelativePath();
  }

  /**
   * Run 'cmake -G xyz' command.
   *
   * @param monitor
   * @param console
   *        TODO
   * @param buildDir
   *        abs path
   * @param srcDir
   * @return
   * @throws CoreException
   */
  private MultiStatus invokeCMake(IPath srcDir, IPath buildDir,
      IProgressMonitor monitor) throws CoreException {
    MultiStatus status = // Return value
    new MultiStatus(CMakePlugin.PLUGIN_ID, IStatus.OK, "", null);

    Assert.isLegal(srcDir.isAbsolute(), "srcDir");
    Assert.isLegal(buildDir.isAbsolute(), "buildDir");

    Path cmd = new Path("cmake");
    List<String> argList = new ArrayList<String>();
    // generate makefiles..
    argList.add("-G");
    argList.add("Unix Makefiles");
    // colored output during build is useless for build console
    argList.add("-DCMAKE_COLOR_MAKEFILE:BOOL=OFF");
    // echo commands to the console during the make to give output parser a chance
    argList.add("-DCMAKE_VERBOSE_MAKEFILE:BOOL=ON");

    // check for debug or release build..
    {
      IBuildObjectProperties buildProperties = config.getBuildProperties();
      IBuildProperty property = buildProperties
          .getProperty(ManagedBuildManager.BUILD_TYPE_PROPERTY_ID);
      if (property != null) {
        IBuildPropertyValue value = property.getValue();
        if (ManagedBuildManager.BUILD_TYPE_PROPERTY_DEBUG.equals(value.getId())) {
          argList.add("-D");
          argList.add("CMAKE_BUILD_TYPE:STRING=Debug");
        } else if (ManagedBuildManager.BUILD_TYPE_PROPERTY_RELEASE.equals(value
            .getId())) {
          argList.add("-D");
          argList.add("CMAKE_BUILD_TYPE:STRING=Release");
        }
      }
    }
    argList.add(srcDir.toOSString());

    IConsole console = CCorePlugin.getDefault().getConsole();
    console.start(project);
    ICommandLauncher launcher = builder.getCommandLauncher();

    String errMsg = null;
    updateMonitor("Invoking CMake " + project.getName());

    ConsoleOutputStream out = console.getOutputStream();

    launcher.showCommand(true);
    Process proc = launcher.execute(cmd,
        argList.toArray(new String[argList.size()]), null, buildDir, monitor);
    if (proc != null) {
      try {
        // Close the input of the process since we will never write to it
        proc.getOutputStream().close();
      } catch (IOException e) {
      }
      int state = launcher.waitAndRead(out, console.getErrorStream(),
          new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
      if (state != ICommandLauncher.OK) {
        errMsg = launcher.getErrorMessage();

        if (state == ICommandLauncher.COMMAND_CANCELED) {
          //TODO: a better way of handling cancel is needed
          //currently the rebuild state is set to true forcing the full rebuild
          //on the next builder invocation
          config.setRebuildState(true);
          status = new MultiStatus(CMakePlugin.PLUGIN_ID, IStatus.CANCEL, "",
              null);
        }
      }

    } else {
      // process start faled
      errMsg = launcher.getErrorMessage();
    }

    // Report the failure of our mission
    if (errMsg != null && errMsg.length() > 0) {
      status = new MultiStatus(CMakePlugin.PLUGIN_ID, IStatus.ERROR, errMsg,
          null);
      StringBuilder buf = new StringBuilder();
      buf.append(errMsg).append('\n');
      // Write message on the console
      try {
        out.write(buf.toString().getBytes());
        out.flush();
      } catch (IOException ex) {
        // TODO Auto-generated catch block
        ex.printStackTrace();
      }
//      epmOutputStream.close();
    }
    return status;
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#getMakefileName()
   */
  @Override
  public String getMakefileName() {
    return "Makefile"; // file is generated by cmake
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#initialize(org.eclipse.core.resources.IProject, org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo, org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public void initialize(IProject project, IManagedBuildInfo info,
      IProgressMonitor monitor) {
    // TODO Auto-generated function stub
    System.out.println("# in CMakeMakefileGenerator.initialize()");
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
   * Check whether the build has been cancelled. Cancellation requests
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
