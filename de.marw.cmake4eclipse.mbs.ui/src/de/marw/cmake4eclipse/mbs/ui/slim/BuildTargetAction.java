/*******************************************************************************
 * Copyright (c) 2022 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake4eclipse.mbs.ui.slim;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.eclipse.cdt.newmake.core.IMakeBuilderInfo;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.BuildAction;
import org.eclipse.ui.actions.SelectionListenerAction;

import de.marw.cmake4eclipse.mbs.ui.Activator;

/**
 * @author Martin Weber
 */
class BuildTargetAction extends SelectionListenerAction {

  public static final String LAST_TARGET = "lastTarget"; //$NON-NLS-1$

  private Shell shell;

  /**
   * @param shell
   */
  public BuildTargetAction(Shell shell) {
    super("Build &Target");
    this.shell = shell;
    URL url = null;
    try {
      url = new URL("platform:/plugin/" + Activator.PLUGIN_ID + "/icons/target_build.png");
      ImageDescriptor imageDescriptor = ImageDescriptor.createFromURL(url);
      setImageDescriptor(imageDescriptor);
    } catch (MalformedURLException ignore) {
    }
    setEnabled(false);
  }

  @Override
  public void run() {
    if (canBuild()) {
      NavBuildTarget[] targets = getSelectedElements().toArray(new NavBuildTarget[0]);
      buildTargets(targets);
    }
  }

  private void buildTargets(final NavBuildTarget[] targets) {
    // Setup the global build console
    CUIPlugin.getDefault().startGlobalConsole();

    saveAllResources(targets);
    Job targetJob = new Job("Building Targets") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        SubMonitor subMonitor = SubMonitor.convert(monitor, "Building Targets...", targets.length);
        try {
          for (NavBuildTarget target : targets) {
            IWorkspaceRunnable runnable = new IWorkspaceRunnable() {

              @Override
              public void run(IProgressMonitor monitor) throws CoreException {
                build(target, monitor);
              }
            };
            ResourcesPlugin.getWorkspace().run(runnable, null, IResource.NONE, subMonitor.newChild(1));
          }
          // store last target property
          BuildTargetsContainer container = targets[0].getContainer();
          container.getProject().setSessionProperty(new QualifiedName(Activator.PLUGIN_ID, LAST_TARGET),
              targets[targets.length - 1].getName());
        } catch (CoreException e) {
          return e.getStatus();
        } catch (OperationCanceledException e) {
        } finally {
          monitor.done();
        }
        return Status.OK_STATUS;
      }

      @Override
      public boolean belongsTo(Object family) {
        return ResourcesPlugin.FAMILY_MANUAL_BUILD == family;
      }
    };
    targetJob.schedule();

    // NOTE: we would like to respect the setting of prefs|C/C++|Build|build targets|background but that is non-public
    // API
    // if (!MakePreferencePage.isBuildTargetInBackground()) {
    // runWithProgressDialog(shell, targetJob);
    // }
  }

  private static void build(NavBuildTarget target, IProgressMonitor monitor) throws CoreException {
    IProject project = target.getContainer().getProject();
    ICommand[] commands = project.getDescription().getBuildSpec();
    SubMonitor subMonitor = SubMonitor.convert(monitor, commands.length);
    for (ICommand command : commands) {
      if (de.marw.cmake4eclipse.mbs.internal.Activator.BUILDER_ID.equals(command.getBuilderName())) {
        // our builder...
        final HashMap<String, String> buildArgs = new HashMap<>();
        buildArgs.put(IMakeBuilderInfo.BUILD_TARGET_INCREMENTAL, target.getName());

        project.build(IncrementalProjectBuilder.FULL_BUILD, command.getBuilderName(), buildArgs,
            subMonitor.newChild(1));
        monitor.done();
      } else {
        // other builders take the default arguments
        project.build(IncrementalProjectBuilder.FULL_BUILD, command.getBuilderName(), command.getArguments(),
            subMonitor.newChild(1));
      }
    }
  }

  /**
   * Causes all editors to save any modified resources depending on the user's preference.
   */
  static void saveAllResources(NavBuildTarget[] targets) {

    if (!BuildAction.isSaveAllSet())
      return;

    List<IProject> projects = new ArrayList<>();
    for (int i = 0; i < targets.length; ++i) {
      NavBuildTarget target = targets[i];
      IProject project = target.getContainer().getProject();
      projects.add(project);
      // Ensure we correctly save files in all referenced projects before build
      try {
        projects.addAll(Arrays.asList(project.getReferencedProjects()));
      } catch (CoreException e) {
        // Project not accessible or not open
      }
    }

    IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
    for (int i = 0; i < windows.length; i++) {
      IWorkbenchPage[] pages = windows[i].getPages();
      for (int j = 0; j < pages.length; j++) {
        IWorkbenchPage page = pages[j];
        IEditorReference[] editorReferences = page.getEditorReferences();
        for (int k = 0; k < editorReferences.length; k++) {
          IEditorPart editor = editorReferences[k].getEditor(false);
          if (editor != null && editor.isDirty()) {
            IEditorInput input = editor.getEditorInput();
            if (input instanceof IFileEditorInput) {
              IFile inputFile = ((IFileEditorInput) input).getFile();
              if (projects.contains(inputFile.getProject())) {
                page.saveEditor(editor, false);
              }
            }
          }
        }
      }
    }
  }

  @Override
  protected boolean updateSelection(IStructuredSelection selection) {
    return super.updateSelection(selection) && canBuild();
  }

  private boolean canBuild() {
    List<?> elements = getSelectedElements();
    for (Object element : elements) {
      if (!(element instanceof NavBuildTarget)) {
        return false;
      }
    }
    return elements.size() > 0;
  }

  private List<?> getSelectedElements() {
    return getStructuredSelection().toList();
  }
}
