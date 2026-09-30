/*******************************************************************************
 * Copyright (c) 2025 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.internal;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.utils.CommandLineUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Determines the build target (i.e. the name of the object file, as it would be passed to the buildscript processor
 * such as {@code make} or {@code ninja}) that corresponds to a source file, by inspecting the
 * {@code compile_commands.json} file that cmake generated for a build configuration.<br>
 * This is needed to implement the "Build Selected File(s)" command: Unlike classic Managed Build Projects, a
 * cmake4eclipse project does not carry per-file compiler command patterns in CDT's build model (the tool-chain's
 * tool is a mere placeholder, cmake determines the actual compiler invocation), so CDT's generic mechanism to build
 * an arbitrary selection of source files cannot be used here. Instead, the target name of the object file is looked
 * up in {@code compile_commands.json} and a build of just that target is triggered, using the project's ordinary
 * build mechanism (i.e. the buildscript processor that cmake configured, e.g. 'make' or 'ninja').
 *
 * @author Aleksandrs Gumenuks
 */
public class CompileCommandsTargetLocator {

  private CompileCommandsTargetLocator() {
    // static methods only
  }

  /**
   * Determines the build target that produces the specified source file, as recorded in the
   * {@code compile_commands.json} file of the given build configuration.
   *
   * @param cfgDescription
   *        the configuration description of the build configuration to investigate
   * @param sourceFile
   *        the source file to build
   * @return the name of the build target (i.e. the name of the object file, relative to the build directory) or
   *         {@code null} if no matching entry was found for {@code sourceFile} (e.g. because a build that would
   *         create {@code compile_commands.json} has never been run, or the file is excluded from the build)
   * @throws CoreException
   *         if the {@code compile_commands.json} file exists but could not be parsed
   */
  public static String findBuildTarget(ICConfigurationDescription cfgDescription, IFile sourceFile)
      throws CoreException {
    final IPath sourceLocation = sourceFile.getLocation();
    if (sourceLocation == null) {
      return null;
    }

    final IPath builderCWD = cfgDescription.getBuildSetting().getBuilderCWD();
    final String cwd = CCorePlugin.getDefault().getCdtVariableManager().resolveValue(builderCWD.toString(), "", null, //$NON-NLS-1$
        cfgDescription);
    final IFile jsonFileRc = ResourcesPlugin.getWorkspace().getRoot()
        .getFile(new Path(cwd).append("compile_commands.json")); //$NON-NLS-1$
    final IPath jsonLocation = jsonFileRc.getLocation();
    if (jsonLocation == null) {
      return null;
    }
    final File jsonFile = jsonLocation.toFile();
    if (!jsonFile.isFile()) {
      return null;
    }

    try {
      final File srcCanonical = sourceLocation.toFile().getCanonicalFile();
      final File cwdFile = new File(cwd);
      try (FileReader reader = new FileReader(jsonFile, StandardCharsets.UTF_8)) {
        CompileCommand[] entries = new Gson().fromJson(reader, CompileCommand[].class);
        if (entries != null) {
          for (CompileCommand entry : entries) {
            if (entry.file == null) {
              continue;
            }
            if (new File(entry.file).getCanonicalFile().equals(srcCanonical)) {
              String outputFile = entry.getOutputFile();
              if (outputFile != null && !outputFile.isEmpty()) {
                File dir = entry.directory != null ? new File(entry.directory) : cwdFile;
                return toBuildDirRelative(outputFile, dir);
              }
            }
          }
        }
      }
    } catch (IOException | JsonSyntaxException ex) {
      throw new CoreException(
          new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to parse file " + jsonFile, ex)); //$NON-NLS-1$
    }
    return null;
  }

  /**
   * Converts the given object file name (which may be absolute or relative to {@code dir}) to a path relative to
   * {@code dir}, using forward slashes.
   */
  private static String toBuildDirRelative(String outputFile, File dir) {
    File objFile = new File(outputFile);
    if (!objFile.isAbsolute()) {
      // already relative to 'dir' (the common case for both makefile- and ninja-generators)
      return outputFile.replace('\\', '/');
    }
    IPath dirPath = Path.fromOSString(dir.getAbsolutePath());
    IPath objPath = Path.fromOSString(objFile.getAbsolutePath());
    if (dirPath.getDevice() == null ? objPath.getDevice() == null : dirPath.getDevice().equalsIgnoreCase(objPath.getDevice())) {
      if (dirPath.isPrefixOf(objPath)) {
        return objPath.removeFirstSegments(dirPath.segmentCount()).setDevice(null).toString();
      }
    }
    return objPath.toString();
  }

  /**
   * A single entry of the {@code compile_commands.json} file, as written by cmake.
   */
  @SuppressWarnings("unused")
  private static class CompileCommand {
    String directory;
    String command;
    List<String> arguments;
    String file;
    /** only present for some generators/cmake versions */
    String output;

    /**
     * Gets the name of the object file that this compile command produces, either taken from the (optional)
     * {@code output} property or parsed from the {@code -o} command line argument.
     *
     * @return the object file name or {@code null} if it could not be determined
     */
    String getOutputFile() {
      if (output != null && !output.isEmpty()) {
        return output;
      }
      final String[] args;
      if (arguments != null && !arguments.isEmpty()) {
        args = arguments.toArray(new String[arguments.size()]);
      } else if (command != null) {
        args = CommandLineUtil.argumentsToArray(command);
      } else {
        return null;
      }
      for (int i = 0; i < args.length; i++) {
        final String arg = args[i];
        if ("-o".equals(arg)) { //$NON-NLS-1$
          if (i + 1 < args.length) {
            return args[i + 1];
          }
        } else if (arg.startsWith("-o") && arg.length() > 2) { //$NON-NLS-1$
          return arg.substring(2);
        }
      }
      return null;
    }
  }
}
