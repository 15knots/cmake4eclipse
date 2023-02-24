/*******************************************************************************
 * Copyright (c) 2022 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.ui.slim;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.managedbuilder.buildproperties.IBuildProperty;
import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedProject;
import org.eclipse.cdt.managedbuilder.core.IProjectType;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.ui.wizards.CfgHolder;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.tools.templates.core.IGenerator;
import org.eclipse.tools.templates.ui.TemplateWizard;
import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import de.marw.cmake4eclipse.mbs.internal.storage.BuildTargetSerializer;
import de.marw.cmake4eclipse.mbs.internal.storage.Util;
import de.marw.cmake4eclipse.mbs.nature.C4ENature;
import de.marw.cmake4eclipse.mbs.settings.CMakeSettings;
import de.marw.cmake4eclipse.mbs.ui.Activator;

/**
 * Wizard for creating a new project. Shows the standard page for project creation and a page to configure the project.
 *
 * @author Martin Weber
 */
// NOTE: we have to extend class TemplateWizard to make this wizard show up in
// the templates section of the 'New C/C++ Project' wizard
public class NewProjectTemplateWizard extends TemplateWizard implements IGenerator {

  private WizardNewProjectCreationPage mainPage;
  private NewProjectConfigWizardPage configPage;

  private IProject newProject;

  private boolean existingPath = false;
  private String lastProjectName;
  private URI lastProjectLocation = null;

  @Override
  public void addPages() {
    mainPage = new WizardNewProjectCreationPage("de.marw.cmake4eclipse.mbs.ui.new_cmake4eclipse_project");
    mainPage.setTitle("C/C++ Project");
    mainPage.setDescription("Create a new cmake4eclipse project.");
    super.addPage(mainPage);

    configPage = new NewProjectConfigWizardPage();
    super.addPage(configPage);
  }

  @Override
  public boolean performCancel() {
    clearProject();
    return true;
  }

  @Override
  public boolean performFinish() {
    // create project if it is not created yet
    if (getProject(true) == null)
      return false;
    try {
      setCreated();
    } catch (CoreException e) {
      e.printStackTrace();
      return false;
    }
    selectAndReveal(newProject);
    return true;
  }

  /**
   * Create and return the project specified by the wizard.
   *
   * @param onFinish
   *                 true if the method is called when finish is pressed, false otherwise. If onFinish is false, the
   *                 project is temporary and can be removed if cancel is pressed.
   * @return the newly created project
   */
  /* package */ IProject getProject(boolean onFinish) {
    if (newProject != null && isChanged())
      clearProject();
    if (newProject == null) {
      existingPath = false;
      try {
        IFileStore fs;
        URI p = getProjectLocation();
        if (p == null) {
          fs = EFS.getStore(ResourcesPlugin.getWorkspace().getRoot().getLocationURI());
          fs = fs.getChild(mainPage.getProjectName());
        } else
          fs = EFS.getStore(p);
        IFileInfo f = fs.fetchInfo();
        if (f.exists() && f.isDirectory()) {
          if (fs.getChild(".project").fetchInfo().exists()) { //$NON-NLS-1$
            if (!MessageDialog.openConfirm(getShell(), "Project already exists on disk",
                "Existing project settings will be overridden.\n"
                    + "Import feature can be used instead to preserve old settings.\n" + "OK to override ?"))
              return null;
          }
          existingPath = true;
        }
      } catch (CoreException e) {
        CUIPlugin.log(e.getStatus());
      }
      lastProjectName = mainPage.getProjectName();
      lastProjectLocation = getProjectLocation();
      // start creation process
      invokeRunnable(getRunnable(onFinish));
    }
    return newProject;
  }

  /**
   * First stage of creating the project. Only used internally.
   *
   * @param name
   *                 name of the project
   * @param location
   *                 location URI for the project
   * @param monitor
   *                 progress monitor
   * @return the project
   * @throws CoreException
   *                       if project creation fails for any reason
   */
  private IProject createIProject(final String name, final URI location, IProgressMonitor monitor)
      throws CoreException {
    SubMonitor subMonitor = SubMonitor.convert(monitor, "Creating project", 100);
    if (newProject != null)
      return newProject;

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceRoot root = workspace.getRoot();
    final IProject newProjectHandle = root.getProject(name);

    if (!newProjectHandle.exists()) {
      IProjectDescription description = workspace.newProjectDescription(newProjectHandle.getName());
      if (location != null) {
        description.setLocationURI(location);
      }
      newProject = CCorePlugin.getDefault().createCDTProject(description, newProjectHandle, subMonitor.split(50));
    } else {
      IWorkspaceRunnable runnable = monitor1 -> newProjectHandle.refreshLocal(IResource.DEPTH_INFINITE, monitor1);
      workspace.run(runnable, root, IWorkspace.AVOID_UPDATE, subMonitor.split(50));
      newProject = newProjectHandle;
    }

    // Open the project if we have to
    if (!newProject.isOpen()) {
      newProject.open(subMonitor.split(25));
    }
    IProgressMonitor continueCreationMonitor = subMonitor.split(25);
    C4ENature.addNature(newProject, SubMonitor.convert(continueCreationMonitor, "Add Project Nature", 1).split(1));

    return newProject;
  }

  private void createProject(IProject project, boolean onFinish, IProgressMonitor monitor) throws CoreException {
    SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
    setProjectDescription(project, onFinish, subMonitor.split(70));
    subMonitor.worked(30);
  }

  private void setProjectDescription(IProject project, boolean onFinish, IProgressMonitor monitor)
      throws CoreException {
    SubMonitor subMonitor = SubMonitor.convert(monitor, 3);
    // Sanity checks..
    IConfiguration[] cfgs = configPage.getCheckedConfigurations();
    if (cfgs == null || cfgs.length == 0)
      cfgs = CfgHolder.items2cfgs(NewProjectConfigWizardPage.getAvailableConfigurations());
    if (cfgs == null || cfgs.length == 0 || cfgs[0] == null) {
      throw new CoreException(
          new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Cannot create managed project with NULL configuration"));
    }

    ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
    ICProjectDescription projectDescr = mngr.createProjectDescription(project, false, !onFinish);
    subMonitor.worked(1);
    ICStorageElement storage = projectDescr.getStorage(CMakeSettings.CFG_STORAGE_ID, true);

    String cmakelists = storage.getAttribute(CMakeSettings.ATTR_CMAKELISTS_FLDR);
    ICSourceEntry[] sourceEntries = null;
    if (cmakelists == null) {
      // scan for top-level CMakeLists.txt file..
      cmakelists = scanForCmakelists(project.getLocation().toFile()).toString();
      storage.setAttribute(CMakeSettings.ATTR_CMAKELISTS_FLDR, cmakelists);
      if (!cmakelists.isEmpty()) {
        sourceEntries = new ICSourceEntry[] { new CSourceEntry(cmakelists, null, 0) };
      }
    }
    // add well known build targets (sorted)...
    List<String> targets = Arrays.asList("all", "clean", "help", "test");
    Util.serializeCollection(CMakeSettings.ELEM_BUILD_TARGETS, storage, new BuildTargetSerializer(), targets);

    IProjectType pt = ManagedBuildManager
        .getExtensionProjectType(de.marw.cmake4eclipse.mbs.internal.Activator.CMAKE4ECLIPSE_PROJECT_TYPE);
    ManagedBuildManager.createBuildInfo(project);
    IManagedProject mProj;
    try {
      mProj = ManagedBuildManager.createManagedProject(project, pt);
    } catch (BuildException e) {
      // gets never thrown
      e.printStackTrace();
      return;
    }
    subMonitor.worked(1);

    IConfiguration active = null;
    SubMonitor cfgMonitor = SubMonitor.convert(subMonitor.split(1), cfgs.length);
    // create build configurations..
    for (IConfiguration cfg : cfgs) {
      IConfiguration iconfig = ManagedBuildManager.createConfigurationForProject(projectDescr, mProj, cfg,
          de.marw.cmake4eclipse.mbs.internal.Activator.CMAKE4ECLIPSE_BUILD_SYSTEM_ID);
      iconfig.setSourceEntries(sourceEntries);

      IBuilder bld = iconfig.getEditableBuilder();
      if (bld != null) {
        bld.setManagedBuildOn(true);
      }

      IBuildProperty b = iconfig.getBuildProperties().getProperty(ManagedBuildManager.BUILD_TYPE_PROPERTY_ID);
      if (active == null && b != null && b.getValue() != null
          && ManagedBuildManager.BUILD_TYPE_PROPERTY_DEBUG.equals(b.getValue().getId())) {
        active = iconfig;
      }
      cfgMonitor.worked(1);
    }
    // activate DEBUG configuration...
    if (active != null) {
      ICConfigurationDescription conf = ManagedBuildManager.getDescriptionForConfiguration(active);
      projectDescr.setActiveConfiguration(conf);
    }

    mngr.setProjectDescription(project, projectDescr);
  }

  /**
   * Scans the file system for a top-level {@code CMakeLists.txt} file.
   *
   * @param projectRootDir
   *                       abs path to project root
   * @return relative path to the top-level {@code CMakeLists.txt} file, never <code>null</code>
   */
  private static File scanForCmakelists(File projectRootDir) {
    List<Path> files = new ArrayList<>();
    Path rootPath = projectRootDir.toPath();
    try {
      Files.walkFileTree(rootPath, EnumSet.noneOf(FileVisitOption.class), 4,
          new SimpleFileVisitor<java.nio.file.Path>() {
            @Override
            public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs) throws IOException {
              if ("CMakeLists.txt".equals(file.getFileName().toString())) {
                files.add(file.getParent());
                return FileVisitResult.SKIP_SUBTREE;
              }
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException e) {
      // ignore, we assign project root as default
    }

    Path shortestPath = files.stream().min(Comparator.comparing(Path::getNameCount)).orElse(rootPath);
    shortestPath = rootPath.relativize(shortestPath);
    return shortestPath.toFile();
  }

 /**
   * Remove created project either after error or if user returned back from config page.
   */
  private void clearProject() {
    if (lastProjectName == null)
      return;
    try {
      ResourcesPlugin.getWorkspace().getRoot().getProject(lastProjectName).delete(!existingPath, true, null);
    } catch (CoreException ignore) {
    }
    newProject = null;
    lastProjectName = null;
    lastProjectLocation = null;
  }

  /**
   * @return true if user has changed settings since project creation
   */
  private boolean isChanged() {
    if (!mainPage.getProjectName().equals(lastProjectName))
      return true;

    URI projectLocation = getProjectLocation();
    if (projectLocation == null) {
      if (lastProjectLocation != null) {
        return true;
      }
    } else if (!projectLocation.equals(lastProjectLocation)) {
      return true;
    }

    return false;
  }

  private URI getProjectLocation() {
    return mainPage.useDefaults() ? null : mainPage.getLocationURI();
  }

  private IRunnableWithProgress getRunnable(final boolean onFinish) {
    return imonitor -> {
      final Exception except[] = new Exception[1];
      getShell().getDisplay().syncExec(() -> {
        IRunnableWithProgress op = new WorkspaceModifyDelegatingOperation(monitor -> {

          SubMonitor subMonitor = SubMonitor.convert(monitor,
              CUIPlugin.getResourceString("Cmake4eclipse Project Wizard"), 100);
          subMonitor.worked(10);
          try {
            newProject = createIProject(lastProjectName, lastProjectLocation, subMonitor.split(40));
            if (newProject != null) {
              createProject(newProject, onFinish, subMonitor.split(40));
            }
            subMonitor.worked(10);
          } catch (CoreException e) {
            Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR, "Project creation failed.",
                e);
            ErrorDialog.openError(getShell(), mainPage.getTitle(), "Project creation failed.", status);
          }
        });
        try {
          getContainer().run(false, true, op);
        } catch (InvocationTargetException e1) {
          except[0] = e1;
        } catch (InterruptedException e2) {
          except[0] = e2;
        }
      });
      if (except[0] != null) {
        if (except[0] instanceof InvocationTargetException) {
          throw (InvocationTargetException) except[0];
        }
        if (except[0] instanceof InterruptedException) {
          throw (InterruptedException) except[0];
        }
        throw new InvocationTargetException(except[0]);
      }
    };
  }

  private boolean invokeRunnable(IRunnableWithProgress runnable) {
    IRunnableWithProgress op = new WorkspaceModifyDelegatingOperation(runnable);
    try {
      getContainer().run(true, true, op);
    } catch (InvocationTargetException e) {
      CUIPlugin.errorDialog(getShell(), "Error Creating Project", "Project cannot be created", e.getTargetException(),
          true);
      clearProject();
      return false;
    } catch (InterruptedException e) {
      clearProject();
      return false;
    }
    return true;
  }

  private boolean setCreated() throws CoreException {
    ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
    ICProjectDescription des = mngr.getProjectDescription(newProject, false);

    if (des == null) {
      return false;
    }

    if (des.isCdtProjectCreating()) {
      des = mngr.getProjectDescription(newProject, true);
      des.setCdtProjectCreated();
      mngr.setProjectDescription(newProject, des, false, null);
      return true;
    }
    return false;
  }

  @Override
  protected IGenerator getGenerator() {
    return this;
  }

  @Override
  public void generate(IProgressMonitor monitor) throws CoreException {
    // Nothing to do, the performFinish already did it
  }
}
