/*******************************************************************************
 * Copyright (c) 2025 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.ui.actions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.newmake.core.IMakeBuilderInfo;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.BuildAction;
import org.eclipse.ui.handlers.HandlerUtil;

import de.marw.cmake4eclipse.mbs.internal.CompileCommandsTargetLocator;
import de.marw.cmake4eclipse.mbs.nature.C4ENature;
import de.marw.cmake4eclipse.mbs.ui.Activator;

/**
 * Handler for the "Build Selected File(s)" command.<br>
 * Builds just the object file(s) that correspond to the currently selected source file(s), analogous to the
 * same-named command that CDT provides for classic Managed Build (Makefile-based) projects.
 * <p>
 * Since a cmake4eclipse project's tool-chain does not carry per-file compiler command patterns in CDT's build model
 * (cmake determines the actual compiler invocation, not CDT), the object file to build is looked up in the
 * {@code compile_commands.json} file that cmake generated for the active build configuration. A build of just that
 * target is then triggered, using the project's ordinary build mechanism (i.e. the buildscript processor that cmake
 * configured, e.g. 'make' or 'ninja').
 * </p>
 *
 * @author Aleksandrs Gumenuks
 */
public class BuildSelectedFilesHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    final List<IFile> files = getSelectedFiles(event);
    if (files.isEmpty()) {
      return null;
    }

    final Map<IProject, List<IFile>> filesByProject = new LinkedHashMap<>();
    for (IFile file : files) {
      filesByProject.computeIfAbsent(file.getProject(), p -> new ArrayList<>()).add(file);
    }

    saveDirtyEditors(filesByProject.keySet());

    Job job = new Job("Build Selected File(s)") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        MultiStatus status = new MultiStatus(Activator.PLUGIN_ID, 0, "Build Selected File(s) failed", null);
        SubMonitor subMonitor = SubMonitor.convert(monitor, filesByProject.size());
        for (Map.Entry<IProject, List<IFile>> entry : filesByProject.entrySet()) {
          try {
            buildFiles(entry.getKey(), entry.getValue(), subMonitor.newChild(1));
          } catch (CoreException ex) {
            status.add(ex.getStatus());
          }
        }
        return status.isOK() ? Status.OK_STATUS : status;
      }

      @Override
      public boolean belongsTo(Object family) {
        return ResourcesPlugin.FAMILY_MANUAL_BUILD == family;
      }
    };
    job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
    job.setUser(true);
    job.schedule();
    return null;
  }

  /**
   * Builds the object files that correspond to the given source files of one project.
   */
  private void buildFiles(IProject project, List<IFile> files, IProgressMonitor monitor) throws CoreException {
    SubMonitor subMonitor = SubMonitor.convert(monitor, files.size() + 1);

    IManagedBuildInfo info = ManagedBuildManager.getBuildInfo(project);
    if (info == null || !info.isValid()) {
      return;
    }
    IConfiguration config = info.getDefaultConfiguration();
    if (config == null) {
      return;
    }
    ICConfigurationDescription cfgDescription = ManagedBuildManager.getDescriptionForConfiguration(config);
    if (cfgDescription == null) {
      return;
    }

    // preserves selection order and avoids duplicate targets
    Set<String> targets = new LinkedHashSet<>();
    for (IFile file : files) {
      String target = CompileCommandsTargetLocator.findBuildTarget(cfgDescription, file);
      subMonitor.worked(1);
      if (target != null && !target.isEmpty()) {
        targets.add(quoteIfNeeded(target));
      } else {
        Activator.getDefault().getLog()
            .log(new Status(IStatus.WARNING, Activator.PLUGIN_ID,
                "No compile command recorded for '" + file.getFullPath()
                    + "'. Run a build for the whole project first."));
      }
    }
    if (targets.isEmpty()) {
      return;
    }

    Map<String, String> buildArgs = new LinkedHashMap<>();
    buildArgs.put(IMakeBuilderInfo.BUILD_TARGET_INCREMENTAL, String.join(" ", targets)); //$NON-NLS-1$
    project.build(IncrementalProjectBuilder.FULL_BUILD, C4ENature.BUILDER_ID, buildArgs, subMonitor.newChild(1));
  }

  private static String quoteIfNeeded(String target) {
    return target.indexOf(' ') >= 0 ? '"' + target + '"' : target;
  }

  /**
   * Gets the source files that are currently selected, or the file that is open in the active editor if the
   * selection does not contain any file (e.g. because the command was invoked while editing a file).
   */
  private static List<IFile> getSelectedFiles(ExecutionEvent event) {
    final List<IFile> files = new ArrayList<>();
    final ISelection selection = HandlerUtil.getCurrentSelection(event);
    if (selection instanceof IStructuredSelection) {
      for (Object element : ((IStructuredSelection) selection).toList()) {
        IFile file = null;
        if (element instanceof IFile) {
          file = (IFile) element;
        } else if (element instanceof IAdaptable) {
          file = ((IAdaptable) element).getAdapter(IFile.class);
        }
        if (file == null) {
          file = Platform.getAdapterManager().getAdapter(element, IFile.class);
        }
        if (file != null) {
          files.add(file);
        }
      }
    }
    if (files.isEmpty()) {
      // no file in the selection: fall back to the file open in the active editor
      IEditorPart editor = HandlerUtil.getActiveEditor(event);
      if (editor != null) {
        IEditorInput input = editor.getEditorInput();
        if (input instanceof IFileEditorInput) {
          files.add(((IFileEditorInput) input).getFile());
        }
      }
    }
    return files;
  }

  /**
   * Causes all editors of the given projects to save any modified resources, depending on the user's preference.
   */
  private static void saveDirtyEditors(Set<IProject> projects) {
    if (!BuildAction.isSaveAllSet()) {
      return;
    }
    for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
      for (IWorkbenchPage page : window.getPages()) {
        for (IEditorReference ref : page.getEditorReferences()) {
          IEditorPart editor = ref.getEditor(false);
          if (editor != null && editor.isDirty()) {
            IEditorInput input = editor.getEditorInput();
            if (input instanceof IFileEditorInput) {
              IFile file = ((IFileEditorInput) input).getFile();
              if (projects.contains(file.getProject())) {
                page.saveEditor(editor, false);
              }
            }
          }
        }
      }
    }
  }
}
