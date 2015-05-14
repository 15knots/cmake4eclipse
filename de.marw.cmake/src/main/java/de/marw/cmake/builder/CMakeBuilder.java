/*******************************************************************************
 * Copyright (c) 2015 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cmake.builder;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.externaltools.internal.launchConfigurations.ExternalToolsProgramMessages;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import de.marw.cmake.CMakePlugin;

/**
 * This tool builder implementation will first run {@code cmake} to generate the
 * build scripts and then invoke the build tool specified in CMakecache.txt
 * during the build process.
 *
 * @author Martin Weber
 */
public class CMakeBuilder extends IncrementalProjectBuilder {

  class SampleDeltaVisitor implements IResourceDeltaVisitor {
    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core
     * .resources.IResourceDelta)
     */
    public boolean visit(IResourceDelta delta) throws CoreException {
      IResource resource = delta.getResource();
      switch (delta.getKind()) {
      case IResourceDelta.ADDED:
        // handle added resource
        checkXML(resource);
        break;
      case IResourceDelta.REMOVED:
        // handle removed resource
        break;
      case IResourceDelta.CHANGED:
        // handle changed resource
        checkXML(resource);
        break;
      }
      //return true to continue visiting children.
      return true;
    }
  }

  class SampleResourceVisitor implements IResourceVisitor {
    public boolean visit(IResource resource) {
      checkXML(resource);
      //return true to continue visiting children.
      return true;
    }
  }

  class XMLErrorHandler extends DefaultHandler {

    private IFile file;

    public XMLErrorHandler(IFile file) {
      this.file = file;
    }

    private void addMarker(SAXParseException e, int severity) {
      CMakeBuilder.this.addMarker(file, e.getMessage(), e.getLineNumber(),
          severity);
    }

    public void error(SAXParseException exception) throws SAXException {
      addMarker(exception, IMarker.SEVERITY_ERROR);
    }

    public void fatalError(SAXParseException exception) throws SAXException {
      addMarker(exception, IMarker.SEVERITY_ERROR);
    }

    public void warning(SAXParseException exception) throws SAXException {
      addMarker(exception, IMarker.SEVERITY_WARNING);
    }
  }

  public static final String BUILDER_ID = CMakePlugin.PLUGIN_ID
      + ".cmakeBuilder";

  private static final String MARKER_TYPE = CMakePlugin.PLUGIN_ID
      + ".xmlProblem";

  private SAXParserFactory parserFactory;

  private void addMarker(IFile file, String message, int lineNumber,
      int severity) {
    try {
      IMarker marker = file.createMarker(MARKER_TYPE);
      marker.setAttribute(IMarker.MESSAGE, message);
      marker.setAttribute(IMarker.SEVERITY, severity);
      if (lineNumber == -1) {
        lineNumber = 1;
      }
      marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
    } catch (CoreException e) {
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
   * java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
   */
  protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
      throws CoreException {
    switch (kind) {
    case IncrementalProjectBuilder.CLEAN_BUILD:
      clean(monitor);
      break;
    case IncrementalProjectBuilder.FULL_BUILD:
      clean(monitor);
      break;
    case IncrementalProjectBuilder.AUTO_BUILD:
      break;
    case IncrementalProjectBuilder.INCREMENTAL_BUILD:
      break;
    }

    final ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    final ILaunchConfigurationType type = manager
        .getLaunchConfigurationType(ExternalProcessLaunchDelegate.ID);
    /* also used as console title */
    String launchName = buildKindToText(kind) + " of project "
        + getProject().getName();
    final ILaunchConfigurationWorkingCopy config = type.newInstance(null,
        launchName);

//    config.setAttribute(ExternalProcessLaunchDelegate.ATTR_CONSOLE_TITLE,
//        launchName);
    List<String> cmdLine = Arrays.asList("cmake", "-al",
        "/home/weber/devel/src/cmake4eclipsecdt/testprojects/C-subsrc");
    config
        .setAttribute(ExternalProcessLaunchDelegate.ATTR_COMMANDLINE, cmdLine);
    config
        .setAttribute(ExternalProcessLaunchDelegate.ATTR_WORKINDIR,
            "/home/weber/devel/src/cmake4eclipsecdt/testprojects/C-subsrc/build/Debug");

    // do not show this in launch history
//    config.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
    if (false) {
      ILaunch launch = new Launch(null, "run", null);
      manager.addLaunch(launch);
      launchProcess(launch, cmdLine, null, monitor);
      cmdLine = Arrays.asList("echo", "ls", "-al");
      launchProcess(launch, cmdLine, null, monitor);
      IConsoleManager cmanager = ConsolePlugin.getDefault().getConsoleManager();
      IConsole[] existing = cmanager.getConsoles();
      for (IConsole iConsole : existing) {
        System.out.println(iConsole);
      }
      manager.removeLaunch(launch);
    } else {
      launchBuild(config, monitor);
    }
    return null;
  }

  public IProcess launchProcess(ILaunch launch, List<String> cmdLine,
      File workingDir, IProgressMonitor monitor) throws CoreException {

    String[] argv = cmdLine.toArray(new String[cmdLine.size()]);
    Process p = DebugPlugin.exec(argv, workingDir, null);
    IProcess process = null;
    if (p != null) {
// TODO      monitor.beginTask(config.getName(), IProgressMonitor.UNKNOWN);
      // add process type to process attributes
      Map<String, String> processAttributes = new HashMap<String, String>();
      processAttributes.put(IProcess.ATTR_PROCESS_TYPE, argv[0]);
      processAttributes.put(IProcess.ATTR_PROCESS_LABEL, "XXX" + argv[0]);
      processAttributes.put(IProcess.ATTR_PROCESS_TYPE, "run"); // for line trackers
      process = DebugPlugin.newProcess(launch, p, "CMake console",
          processAttributes);
    }
    if (p == null || process == null) {
      if (p != null)
        p.destroy();
      throw new CoreException(new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID,
          IExternalToolConstants.ERR_INTERNAL_ERROR,
          ExternalToolsProgramMessages.ProgramLaunchDelegate_4, null));
    }
    launch.addProcess(process);
    // wait for process to exit
    while (!process.isTerminated()) {
      try {
        if (monitor.isCanceled()) {
          process.terminate();
          break;
        }
        Thread.sleep(50);
      } catch (InterruptedException e) {
      }
    }
    return process;
  }

  private void launchBuild(ILaunchConfiguration config, IProgressMonitor monitor)
      throws CoreException {
    monitor.subTask(config.getName());
    ILaunch launch = config.launch(ILaunchManager.RUN_MODE, monitor, false,
        true);
    IProcess[] processes = launch.getProcesses();

    if (processes.length > 0) {
      // get the IProcess instance from the launch
      IProcess process = launch.getProcesses()[0];
      // get the streamsproxy from the process
      IStreamsProxy proxy = process.getStreamsProxy();
      showConsole(process);
    }
  }

  protected void clean(IProgressMonitor monitor) throws CoreException {
    // delete markers set and files created
    getProject().deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
//    launchBuild(IncrementalProjectBuilder.CLEAN_BUILD, config, null, monitor);
  }

  private String buildKindToText(int kind) {
    switch (kind) {
    case IncrementalProjectBuilder.CLEAN_BUILD:
      return "Clean build";
    case IncrementalProjectBuilder.FULL_BUILD:
      return "Full build";
    case IncrementalProjectBuilder.AUTO_BUILD:
      return "Auto build";
    case IncrementalProjectBuilder.INCREMENTAL_BUILD:
      return "Incremental build";
    default:
      return kind + " build";
    }
  }

  void checkXML(IResource resource) {
    if (resource instanceof IFile && resource.getName().endsWith(".xml")) {
      IFile file = (IFile) resource;
      deleteMarkers(file);
      XMLErrorHandler reporter = new XMLErrorHandler(file);
      try {
        getParser().parse(file.getContents(), reporter);
      } catch (Exception e1) {
      }
    }
  }

  private void deleteMarkers(IFile file) {
    try {
      file.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
    } catch (CoreException ce) {
    }
  }

  protected void fullBuild(final IProgressMonitor monitor) throws CoreException {
    try {
      getProject().accept(new SampleResourceVisitor());
    } catch (CoreException e) {
    }
  }

  private SAXParser getParser() throws ParserConfigurationException,
      SAXException {
    if (parserFactory == null) {
      parserFactory = SAXParserFactory.newInstance();
    }
    return parserFactory.newSAXParser();
  }

  protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor)
      throws CoreException {
    // the visitor does the work.
    delta.accept(new SampleDeltaVisitor());
  }

  private void showConsole(IProcess process) {
    if (process != null && process.getLaunch() != null) {
      org.eclipse.ui.console.IConsole console = DebugUITools
          .getConsole(process);
      ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);
//      IWorkbenchPage page = PlatformUI.getWorkbench()
//          .getActiveWorkbenchWindow().getActivePage();
//      IViewPart view = page.findView("org.eclipse.ui.console.ConsoleView");
//      if (view != null)
//        view.setFocus();
    }
  }

}
