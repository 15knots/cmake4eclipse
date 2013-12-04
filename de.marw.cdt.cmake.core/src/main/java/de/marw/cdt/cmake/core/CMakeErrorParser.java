/*******************************************************************************
 * Copyright (c) 2013 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.IErrorParser2;
import org.eclipse.cdt.core.IErrorParser3;
import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.ProblemMarkerInfo;
import org.eclipse.cdt.core.errorparsers.AbstractErrorParser;
import org.eclipse.cdt.core.errorparsers.ErrorPattern;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

/**
 * @author Martin Weber
 */
public class CMakeErrorParser extends AbstractErrorParser implements
    IErrorParser2, IErrorParser3 {

  public static String CMAKE_PROBLEM_MARKER_ID = CMakePlugin.PLUGIN_ID
      + ".problem"; //$NON-NLS-1$

  /**
   * CMake Error:
   *
   * <pre>
   * blah Specify
   * --help for usage, or press the help button on the CMake GUI.
   * </pre>
   */
  private static final String pattern_invoke_ignore = "Specify --help for usage, .+";
  /**
   * cmake invocation error, with exit status 1:
   *
   * <pre>
   * CMake Error: The source directory "/home/gipsnich/build" does not exist.
   */
  private static final Pattern PTN_INVOKE_ERROR = Pattern
      .compile("^CMake Error:\\s*(.+)$");
  /**
   * CMakelists.txt errors...
   *
   * <pre>
   * CMake Error at CMakeLists.txt:14 (XXXproject):
   * </pre>
   */
  private static final Pattern PTN_SCRIPT_ERROR = Pattern
      .compile("^CMake Error at (.+):\\s*(\\d+)\\s+\\((.+?)\\):$");
  /**
   * CMakelists.txt warnings...
   *
   * <pre>
   * CMake Warning at CMakeLists.txt:14 (XXXproject):
   * </pre>
   */
  private static final Pattern PTN_SCRIPT_WARN = Pattern
      .compile("^CMake Warning at (.+):\\s*(\\d+)\\s+\\((.+?)\\):$");
  /**
   * CMakelists.txt developer warnings...
   *
   * <pre>
   * CMake Warning (dev) in CMakeLists.txt:
   * </pre>
   *
   * Block is terminated by a single trailing empty line!
   *
   * <pre>
   *   A logical block opening on the line
   *
   *     /home/weber/devel/src/CDT-CMake/CDT-mgmt-C-0src/CMakeLists.txt:51 (IF)
   *
   *   closes on the line
   *
   *     /home/weber/devel/src/CDT-CMake/CDT-mgmt-C-0src/CMakeLists.txt:54 (ENDIF)
   *
   *   with mis-matching arguments.
   * This warning is for project developers.  Use -Wno-dev to suppress it.
   *
   * </pre>
   */
  private static final Pattern PTN_SCRIPT_WARN_DEV = Pattern
      .compile("^CMake Warning \\(dev\\) in (.+):\\s*(\\d+)\\s+.+?:$");
  /**
   * Script error, first part of problem description
   *
   * <pre>
   *   include could not find load file:
   *
   *     CPackXX
   *
   *
   * </pre>
   */
  private static final Pattern PTN_SCRIPT_ERROR_DESCR1 = Pattern
      .compile("^ *(.+:) *$");
  /**
   * Script error/warning. No empty lines before problem description.
   *
   * <pre>
   *   Unknown CMake command "ifXXX".
   *
   *
   * </pre>
   */
  private static final Pattern PTN_SCRIPT_ERROR_DESCR2 = Pattern
      .compile("^ *(.+)(\\.|!+) *$");

  private PState pstate = PState.NONE;
  private ProblemMarkerInfo markerInfo;

  public CMakeErrorParser() {
    super(new ErrorPattern[] {});
  }

  /**
   * Overwritten to detect error messages spanning multiple lines.
   */
  @Override
  public boolean processLine(String line, ErrorParserManager epm) {
    System.err.println("# " + line);

    switch (pstate) {
    case NONE:
      Matcher matcher;
      if ((matcher = PTN_SCRIPT_ERROR.matcher(line)).matches()) {
        // "CMake Error at "
        pstate = PState.ERR_SCRIPT;
        String fileName = matcher.group(1);
        String lineNo = matcher.group(2);
        String var = matcher.group(3);
        createMarker(fileName, epm, lineNo,
            IMarkerGenerator.SEVERITY_ERROR_RESOURCE, var);
        return true; // consume line
      } else if ((matcher = PTN_SCRIPT_WARN.matcher(line)).matches()) {
        // "CMake Warning at "
        pstate = PState.ERR_SCRIPT;
        String fileName = matcher.group(1);
        String lineNo = matcher.group(2);
        String var = matcher.group(3);
        createMarker(fileName, epm, lineNo, IMarkerGenerator.SEVERITY_WARNING,
            var);
        return true; // consume line
      } else if ((matcher = PTN_SCRIPT_WARN_DEV.matcher(line)).matches()) {
        // "CMake Warning at "
        pstate = PState.ERR_SCRIPT;
        String fileName = matcher.group(1);
        String lineNo = matcher.group(2);
        createMarker(fileName, epm, lineNo, IMarkerGenerator.SEVERITY_WARNING,
            null);
        return true; // consume line
      } else if ((matcher = PTN_INVOKE_ERROR.matcher(line)).matches()) {
        pstate = PState.NONE;
        this.markerInfo = new ProblemMarkerInfo(null, -1, matcher.group(1),
            IMarkerGenerator.SEVERITY_ERROR_BUILD, null);
        markerInfo.setType(CMAKE_PROBLEM_MARKER_ID);
      }
      break;

    case ERR_SCRIPT:
      // marker is present, description is missing
      if (line.length() == 0)
        return true; // consume leading empty line
      if (line.startsWith("-- "))
        return true; // CDT or cmake sometimes reorder lines, ignore

      // extract problem description
      if ((matcher = PTN_SCRIPT_ERROR_DESCR1.matcher(line)).matches()) {
        pstate = PState.ERR_SCRIPT_DESCR_1;
        String descr = matcher.group(1);
        markerInfo.description = descr;
        return true; // consume line
      } else if ((matcher = PTN_SCRIPT_ERROR_DESCR2.matcher(line)).matches()) {
        pstate = PState.ERR_SCRIPT_END_1;
        String descr = matcher.group(1);
        markerInfo.description = descr;
        epm.addProblemMarker(markerInfo);
        return true; // consume line
      }
      break;

    case ERR_SCRIPT_DESCR_1:
      // marker is present, first part of description is present
      if (line.length() == 0)
        return true; // consume empty line
      if (line.startsWith("-- "))
        return true; // CDT or cmake sometimes reorder lines, ignore
      // append line to description
      markerInfo.description += " " + line;
      epm.addProblemMarker(markerInfo);
      pstate = PState.ERR_SCRIPT_END_1;
      return true; // consume line

    case ERR_SCRIPT_END_1:
      if (line.length() == 0) {
        pstate = PState.ERR_SCRIPT_END_2;
        return true; // consume empty line
      }
      break;

    case ERR_SCRIPT_END_2:
      if (line.length() == 0) {
        pstate = PState.NONE;
        markerInfo = null;
        return true; // consume empty line
      }
      break;
//      return true; // consume line

    case ERR_INVOKE:
      break;

    }
    // TODO Auto-generated function stub
    return false;
  }

  /**
   * Creates a problem marker.
   *
   * @param fileName
   *        the file where the problem has occurred
   * @param epm
   * @param lineNo
   *        the line number of the problem
   * @param severity
   *        the severity of the problem, see {@link IMarkerGenerator} for
   *        acceptable severity values
   * @param varName
   *        the name of the variable involved in the error or {@code null} if
   *        unknown
   */
  private void createMarker(String fileName, ErrorParserManager epm,
      String lineNo, int severity, String varName) {
    int lineNumber = Integer.parseInt(lineNo);
    // cmake reports the file relative to source entry
    final IProject project = epm.getProject();
    IConfiguration cfg = ManagedBuildManager.getBuildInfo(project, true)
        .getDefaultConfiguration();
    ICConfigurationDescription cfgDes = ManagedBuildManager
        .getDescriptionForConfiguration(cfg);
    ICSourceEntry[] srcEntriesR = cfgDes.getResolvedSourceEntries();

    ICSourceEntry[] srcEntries = cfg.getSourceEntries();
    srcEntries = CDataUtil.resolveEntries(srcEntries, cfgDes);

    IPath srcPath = srcEntries[0].getFullPath(); // project-relative path!
    IPath filePath = srcPath.append(fileName);
    IFile file2 = project.getFile(filePath);
    IFile file = epm.findFileName(filePath.toString());

    this.markerInfo = new ProblemMarkerInfo(file2, lineNumber, null, severity,
        varName);
    markerInfo.setType(CMAKE_PROBLEM_MARKER_ID);
  }

  /*-
   * @see org.eclipse.cdt.core.IErrorParser2#getProcessLineBehaviour()
   */
  @Override
  public int getProcessLineBehaviour() {
    return IErrorParser2.KEEP_LONGLINES;
  }

  ////////////////////////////////////////////////////////////////////
  // inner classes
  ////////////////////////////////////////////////////////////////////
  /**
   * Error parser state for multiline errors
   */
  private enum PState {
    /** no cmake error detected. */
    NONE,
    /** cmake invocation failed (no process) */
    ERR_INVOKE,
    /** error in CMakeLists.txt */
    ERR_SCRIPT,
    /** error in CMakeLists.txt, first part of description seen */
    ERR_SCRIPT_DESCR_1,
    /**
     * error in CMakeLists.txt, full description seen, wanting first empty line.
     * (Two empty lines mark the end of a CMake error message)
     */
    ERR_SCRIPT_END_1,
    /**
     * error in CMakeLists.txt, full description seen, wanting second empty
     * line.
     */
    ERR_SCRIPT_END_2;
  }

  /*-
   * @see org.eclipse.cdt.core.IErrorParser3#shutdown()
   */
  @Override
  public void shutdown() {
    pstate = PState.NONE;
    markerInfo = null;
  }
}
