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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.externaltools.internal.launchConfigurations.ExternalToolsProgramMessages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.model.IProcess;

import de.marw.cmake.CMakePlugin;

public class ExternalProcessLaunchDelegate implements
    ILaunchConfigurationDelegate {

  public static final String ID = CMakePlugin.PLUGIN_ID
      + ".externalProcessLaunchType";
  /** List valued type */
  public static final String ATTR_COMMANDLINE = CMakePlugin.PLUGIN_ID
      + ".cmdLine";
  public static final String ATTR_WORKINDIR = CMakePlugin.PLUGIN_ID
      + ".workingDir";

  @Override
  public void launch(ILaunchConfiguration config, String mode, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    File workingDir = null;
    String cwd = config.getAttribute(ATTR_WORKINDIR, (String) null);
    if (cwd != null) {
      workingDir = new File(cwd);
    }
    @SuppressWarnings("unchecked")
    List<String> cmd = config.getAttribute(ATTR_COMMANDLINE, (List<?>) null);
    String[] cmdline = cmd.toArray(new String[cmd.size()]);

    Process p = DebugPlugin.exec(cmdline, workingDir, null);
    IProcess process = null;

    if (p != null) {
      monitor.beginTask(config.getName(), IProgressMonitor.UNKNOWN);
      process = DebugPlugin.newProcess(launch, p, "CMake console", null);
    }
    if (p == null || process == null) {
      if (p != null)
        p.destroy();
      throw new CoreException(new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID,
          IExternalToolConstants.ERR_INTERNAL_ERROR,
          ExternalToolsProgramMessages.ProgramLaunchDelegate_4, null));
    }
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
  }
}
