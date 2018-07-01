/*******************************************************************************
 * Copyright (c) 2018 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/

package de.marw.cmake.cdt.language.settings.providers.builtins;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CommandLauncher;
import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import de.marw.cmake.CMakePlugin;

/**
 * Detects preprocessor macros and include paths that are built-in to a compiler.
 *
 * @author Martin Weber
 */
public class BuiltinSpecsDetector {

  private static final String MARKER_ID = CMakePlugin.PLUGIN_ID + ".CompilerBuiltinsDetectorMarker";

  private ICConfigurationDescription cfgDescription;
  private IProgressMonitor monitor;

  /** environment variables, lazily instantiated */
  private String[] envp;

  /**
   * @param cfgDescription
   * @param monitor
   *          progress monitor or <code>null</code>
   */
  public BuiltinSpecsDetector(ICConfigurationDescription cfgDescription, IProgressMonitor monitor) {
    this.cfgDescription = Objects.requireNonNull(cfgDescription);
    this.monitor = (monitor == null ? new NullProgressMonitor() : monitor);
  }

  /**
   * Run built-in detection command.
   *
   * @param languageId
   *          language id
   * @param command
   *          the compiler command (arg 0)
   * @param builtinDetectionType
   *          the compiler classification
   * @throws CoreException
   */
  public List<ICLanguageSettingEntry> run(String languageId, String command, BuiltinDetectionType builtinDetectionType)
      throws CoreException {
    Objects.requireNonNull(languageId, "languageId");
    Objects.requireNonNull(command, "command");
    Objects.requireNonNull(builtinDetectionType, "builtinDetectionType");
    final SubMonitor monitor = SubMonitor.convert(this.monitor, "Built-in settings detection for compiler " + command,
        IProgressMonitor.UNKNOWN);

    List<ICLanguageSettingEntry> entries = Collections.synchronizedList(new ArrayList<>());

    boolean silent = false;
    switch (builtinDetectionType) {
    case NONE:
      return entries;
    case GCC_MAYBE:
      // 'gcc' recognized as 'cc' by cmake: try detection, but do not report errors on failure
      silent = true;
      break;
    default:
      break;
    }

    final List<String> argList = getCompilerArguments(languageId, builtinDetectionType);
    final BuiltinsOutputProcessor bop = getCompilerOutputProcessor(entries, builtinDetectionType);

    IProject project = cfgDescription.getProjectDescription().getProject();
    ICommandLauncher launcher = new CommandLauncher();
    launcher.setProject(project);
    final Process proc = launcher.execute(new Path(command), argList.toArray(new String[argList.size()]), getEnvp(),
        cfgDescription.getBuildSetting().getBuilderCWD(), monitor);
    if (proc != null) {
      try {
        // Close the input of the process since we will never write to it
        proc.getOutputStream().close();
      } catch (IOException e) {
      }
      int state = launcher.waitAndRead(new OutputSniffer(bop), new OutputSniffer(bop), monitor);
      if (state != ICommandLauncher.COMMAND_CANCELED) {
        // check exit status
        final int exitValue = proc.exitValue();
        if (exitValue != 0 && !silent) {
          // compiler had errors...
          String errMsg = String.format("%1$s exited with status %2$d.", command, exitValue);
          createMarker(errMsg);
        }
      }
    } else {
      // process start failed
      createMarker(launcher.getErrorMessage());
    }
    return entries;
  }

  /**
   * @param entries
   *          where to place the {@code ICLanguageSettingEntry}s found during processing.
   * @param builtinDetectionType
   */
  private BuiltinsOutputProcessor getCompilerOutputProcessor(List<ICLanguageSettingEntry> entries,
      BuiltinDetectionType builtinDetectionType) {
    switch (builtinDetectionType) {
    case GCC:
    case GCC_MAYBE:
    case NVCC:
      return new GccOutputProcessor(entries);
    // case ICC:
    // case CL:
    default:
    }
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Gets the compiler-arguments corresponding to the specified language ID and BuiltinDetectionType.
   */
  private List<String> getCompilerArguments(String languageId, BuiltinDetectionType builtinDetectionType) {
    List<String> args = new ArrayList<>();
    args.addAll(getDetectionTypeArguments(builtinDetectionType));
    args.add(getInputFile(languageId));
    return args;
  }

  /**
   * Gets the compiler-arguments corresponding to the specified BuiltinDetectionType.
   */
  private List<? extends String> getDetectionTypeArguments(BuiltinDetectionType builtinDetectionType) {
    switch (builtinDetectionType) {
    case GCC:
    case GCC_MAYBE:
      return Arrays.asList("-E", "-P", "-dM", "-Wp,-v");
    case NVCC:
      return Arrays.asList("-E", "-Xcompiler", "-P", "-Xcompiler", "-dD", "-Xcompiler", "-v");
    case ICC:
      return Arrays.asList("-EP", "-dM", "-H");
    case CL:
      return Arrays.asList("/nologo", "/EP", "/dM");
    default:
      return Arrays.asList();
    }
  }

  /**
   * Get array of environment variables in format "var=value".
   */
  private String[] getEnvp() {
    if (envp == null) {
      // On POSIX (Linux, UNIX) systems reset language variables to default
      // (English)
      // with UTF-8 encoding since GNU compilers can handle only UTF-8
      // characters.
      // Include paths with locale characters will be handled properly
      // regardless
      // of the language as long as the encoding is set to UTF-8.
      // English language is set for parser because it relies on English
      // messages
      // in the output of the 'gcc -v' command.

      List<String> env = new ArrayList<>(Arrays.asList(getEnvp(cfgDescription)));
      for (Iterator<String> iterator = env.iterator(); iterator.hasNext();) {
        String var = iterator.next();
        if (var.startsWith("LANGUAGE" + '=') || var.startsWith("LC_ALL" + '=')) {
          iterator.remove();
        }
      }
      env.add("LANGUAGE" + "=en"); // override for GNU gettext //$NON-NLS-1$
      env.add("LC_ALL" + "=C.UTF-8"); // for other parts of the //$NON-NLS-1$
                                      // system libraries
      envp = env.toArray(new String[env.size()]);
    }
    return envp;
  }

  /**
   * Get environment variables from configuration as array of "var=value" suitable for using as "envp" with
   * Runtime.exec(String[] cmdarray, String[] envp, File dir)
   *
   * @param cfgDescription
   *          - configuration description.
   * @return String array of environment variables in format "var=value". Does not return {@code null}.
   */
  private static String[] getEnvp(ICConfigurationDescription cfgDescription) {
    IEnvironmentVariableManager mngr = CCorePlugin.getDefault().getBuildEnvironmentManager();
    IEnvironmentVariable[] vars = mngr.getVariables(cfgDescription, true);
    // Convert into envp strings
    Set<String> strings = new HashSet<>(vars.length);
    for (IEnvironmentVariable var : vars) {
      strings.add(var.getName() + '=' + var.getValue());
    }
    // On POSIX (Linux, UNIX) systems reset language variables to default
    // (English)
    // with UTF-8 encoding since GNU compilers can handle only UTF-8 characters.
    // Include paths with locale characters will be handled properly regardless
    // of the language as long as the encoding is set to UTF-8.
    // English language is set for parser because it relies on English messages
    // in the output of the 'gcc -v' command.
    strings.add("LANGUAGE" + "=en"); // override for GNU gettext
    strings.add("LC_ALL" + "=C.UTF-8"); // for other parts of the system
                                        // libraries

    return strings.toArray(new String[strings.size()]);
  }

  /**
   * Get path to source file which is the input for the compiler.
   *
   * @param languageId
   *          - language ID.
   * @return full path to the source file.
   */
  private String getInputFile(String languageId) {
    String specExt = getSpecFileExtension(languageId);
    String ext = ""; //$NON-NLS-1$
    if (specExt != null) {
      ext = '.' + specExt;
    }

    String specFileName = "spec" + ext;
    IPath workingLocation = CMakePlugin.getDefault().getStateLocation();
    IPath fileLocation = workingLocation.append(specFileName);

    File specFile = new java.io.File(fileLocation.toOSString());
    if (!specFile.exists()) {
      try {
        // In the typical case it is sufficient to have an empty file.
        specFile.createNewFile();
      } catch (IOException e) {
        CMakePlugin.getDefault().getLog()
            .log(new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID, "registerListener()", e));
      }
    }

    return fileLocation.toString();
  }

  /**
   * Gets the extension for the compiler input file used to detect built-in settings.
   *
   * @param languageId
   *          the ID of the compiler language
   */
  private String getSpecFileExtension(String languageId) {
    if (languageId.equals("org.eclipse.cdt.core.gcc")) {
      return "c";
    }
    if (languageId.equals("org.eclipse.cdt.core.g++")) {
      return "cpp";
    }
    if (languageId.equals("com.nvidia.cuda.toolchain.language.cuda.cu")) {
      return "cu";
    }
    return null;
  }

  private void createMarker(String message) throws CoreException {
    IMarker marker = cfgDescription.getProjectDescription().getProject().createMarker(MARKER_ID);
    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
    marker.setAttribute(IMarker.MESSAGE, message);
  }

}
