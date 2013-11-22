// Copyright 2013 Martin Weber

package de.marw.cdt.cmake.core.internal;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ConsoleOutputStream;
import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.core.resources.RefreshScopeManager;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICFolderDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.managedbuilder.buildproperties.IBuildProperty;
import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyValue;
import org.eclipse.cdt.managedbuilder.core.IBuildObjectProperties;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineGenerator;
import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineInfo;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.macros.BuildMacroException;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator;
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator2;
import org.eclipse.cdt.utils.EFSExtensionManager;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
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
    // MWE anscheinend gibt der Makefile-Generator das root-out-dir vor
    topBuildDir = project.getFolder(cfg.getName()).getProjectRelativePath();

    System.out.println("# in initialize(), cfg= " + cfg.getName());
    srcEntries = config.getSourceEntries();
    for (ICSourceEntry elem : srcEntries) {
      System.out.println("0 srcEntry=" + elem);
    }
    if (srcEntries.length == 0) {
      srcEntries = new ICSourceEntry[] { new CSourceEntry(Path.EMPTY, null,
          ICSettingEntry.RESOLVED | ICSettingEntry.VALUE_WORKSPACE_PATH) };
    } else {
      ICConfigurationDescription cfgDes = ManagedBuildManager
          .getDescriptionForConfiguration(config);
      srcEntries = CDataUtil.resolveEntries(srcEntries, cfgDes);
    }
    for (ICSourceEntry elem : srcEntries) {
      System.out.println("1 srcEntry=" + elem);
    }
    for (ICSourceEntry elem : srcEntries) {
      System.out.println("2 srcEntry=" + elem + ", " + elem.getName() + ", "
          + elem.getFullPath() + ", " + elem.getLocation());
    }
    // TODO Auto-generated function stub
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#getBuildWorkingDir()
   */
  @Override
  public IPath getBuildWorkingDir() {
    System.out.println("# in getBuildWorkingDir() --> " + topBuildDir);
    return topBuildDir;
  }

  /**
   * Return the configuration's top build directory as an absolute path
   */
  private IPath getTopBuildDir() {
    return getPathForResource(project).append(getBuildWorkingDir());
  }

  /**
   * Gets a path for a resource by extracting the Path field from its location
   * URI.
   *
   * @return IPath
   */
  private IPath getPathForResource(IResource resource) {
    return new Path(resource.getLocationURI().getPath());
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
     * 1. This is an incremental build, so if the top-level directory is not
     * there, then a rebuild is needed.
     */
    IFolder folder = project.getFolder(config.getName());

    // check for files generated by cmake
    if (!folder.exists() || !folder.getFile("CMakeCache.txt").exists()
        || !folder.getFile("Makefile").exists()) {
      // cmake did never run on this directory
      return regenerateMakefiles();
    }

    MultiStatus status; // Return value

    // Visit the resources in the delta and compile a list of subdirectories to regenerate
//	updateMonitor(ManagedMakeMessages.getFormattedString("MakefileGenerator.message.calc.delta", project.getName()));	//$NON-NLS-1$
//	ResourceDeltaVisitor visitor = new ResourceDeltaVisitor(this, config);
//	delta.accept(visitor);
    checkCancel();
    // TODO Auto-generated function stub
    System.out.println("# in CMakeMakefileGenerator.generateMakefiles()");
    status = new MultiStatus(CMakePlugin.PLUGIN_ID, IStatus.OK, new String(),
        null);
    return status;
  }

  /**
   * Invoked on full build.
   */
  @Override
  public MultiStatus regenerateMakefiles() throws CoreException {
    MultiStatus status; // Return value
    // TODO Auto-generated function stub
    System.out.println("# in regenerateMakefiles()");

    // See if the user has cancelled the build
    checkCancel();

    // create makefiles, assuming each source directory contains a CMakeLists.txt

    for (int i = 0; i < srcEntries.length; i++) {
      ICSourceEntry srcEntry = srcEntries[i];

      if (MULTIPLE_SOURCE_DIRS_SUPPORTED && i > 0) {
        return status = new MultiStatus(CMakePlugin.PLUGIN_ID, IStatus.ERROR,
            "Only a single source location supported by CMake", null);
      }
      final IPath srcPath = srcEntry.getFullPath(); // project relative
      // Create the top-level directory for the build output
      final IPath cfgBuildPath = createDirectory(MULTIPLE_SOURCE_DIRS_SUPPORTED ? topBuildDir
          .append(srcPath) : topBuildDir);
      final IPath srcDir = project.getFolder(srcPath).getLocation();
      final IPath buildDir = project.getFolder(cfgBuildPath).getLocation();
      checkCancel();
      invokeCMake(srcDir, buildDir, monitor);

    }
    //    ManagedBuildManager hat vile hilfmethoden

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
   *        - progress monitor in the initial state where
   *        {@link IProgressMonitor#beginTask(String, int)} has not been called
   *        yet.
   * @param console
   *        TODO
   * @param buildDir
   *        abs path
   * @param srcDir
   * @throws CoreException
   */
  private void invokeCMake(IPath srcDir, IPath buildDir,
      IProgressMonitor monitor) throws CoreException {

    Assert.isLegal(srcDir.isAbsolute(), "srcDir");
    Assert.isLegal(buildDir.isAbsolute(), "buildDir");

    Path cmd = new Path("cmake");
    List<String> argList = new ArrayList<String>();
    argList.add("-G");
    argList.add("Unix Makefiles");
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
    try {
      monitor.beginTask("Invoking_CMake " + project.getName(), 100 + 100 + 100);

      ConsoleOutputStream out = console.getOutputStream();
      out.write("invoking cmake...\n".getBytes());

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
            //TODO: the better way of handling cancel is needed
            //currently the rebuild state is set to true forcing the full rebuild
            //on the next builder invocation
            int i = 0;
//              info.getDefaultConfiguration().setRebuildState(true);
          }
        }

      } else {
        // process start faled
        errMsg = launcher.getErrorMessage();
      }

      // Report either the success or failure of our mission
      StringBuilder buf = new StringBuilder();
      if (errMsg != null && errMsg.length() > 0) {
        buf.append(errMsg).append('\n');
      } else {
        // Report a successful build
        String successMsg = "ManagedMakeMessages.getFormattedString(BUILD_FINISHED, project.getName())";
        buf.append(successMsg).append('\n');
      }

      // Write message on the console
      out.write(buf.toString().getBytes());
      out.flush();
//      epmOutputStream.close();

    } catch (IOException ex) {
      // TODO Auto-generated catch block
      ex.printStackTrace();
    } finally {
      monitor.done();
    }
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#getMakefileName()
   */
  @Override
  public String getMakefileName() {
    // TODO Auto-generated function stub
    System.out.println("# in CMakeMakefileGenerator.getMakefileName()");
    return null;
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
    // TODO Auto-generated function stub
    // Is this a generated directory ...
    IPath path = resource.getProjectRelativePath();
    System.out.println("# isGeneratedResource(): " + path);
    //TODO: fix to use builder output dir instead
    String[] configNames = ManagedBuildManager.getBuildInfo(project)
        .getConfigurationNames();
    for (String name : configNames) {
      IPath root = new Path(name);
      // It is if it is a root of the resource pathname
      if (root.isPrefixOf(path))
        return true;
    }

    return false;
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#generateDependencies()
   */
  @Override
  public void generateDependencies() throws CoreException {
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#regenerateDependencies(boolean)
   */
  @Override
  public void regenerateDependencies(boolean force) throws CoreException {
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

}
