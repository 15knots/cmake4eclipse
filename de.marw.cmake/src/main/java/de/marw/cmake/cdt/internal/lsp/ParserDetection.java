/*******************************************************************************
 * Copyright (c) 2017-2019 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cmake.cdt.internal.lsp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import de.marw.cmake.cdt.internal.CMakePlugin;
import de.marw.cmake.cdt.language.settings.providers.IToolArgumentParser;
import de.marw.cmake.cdt.language.settings.providers.IToolCommandlineParser;
import de.marw.cmake.cdt.language.settings.providers.ParserDetector;
import de.marw.cmake.cdt.language.settings.providers.ResponseFileArgumentParsers;
import de.marw.cmake.cdt.language.settings.providers.ToolArgumentParsers;
import de.marw.cmake.cdt.language.settings.providers.ToolCommandlineParser;
import de.marw.cmake.cdt.language.settings.providers.builtins.BuiltinDetectionType;

/**
 * Utility classes and methods to detect a parser for a compiler given on a
 * command-line string.
 *
 * @author Martin Weber
 *
 */
class ParserDetection {
  private static final ILog log = CMakePlugin.getDefault().getLog();
  private static final boolean DEBUG_PARSER_DETECTION = Boolean
      .parseBoolean(Platform.getDebugOption(CMakePlugin.PLUGIN_ID + "/CECC/parser"));

  /**
   * tool detectors and their tool option parsers for each tool of interest that
   * takes part in the current build. The Matcher detects whether a command line
   * is an invocation of the tool.
   */
  private static final List<ParserDetector> parserDetectors = new ArrayList<>(22);

  static {
    /** Names of known tools along with their command line argument parsers */
    final IToolArgumentParser[] gcc_args = { new ToolArgumentParsers.IncludePath_C_POSIX(),
        new ToolArgumentParsers.MacroDefine_C_POSIX(), new ToolArgumentParsers.MacroUndefine_C_POSIX(),
        // not defined by POSIX, but does not harm..
        new ToolArgumentParsers.SystemIncludePath_C(), new ToolArgumentParsers.LangStd_GCC(),
        new ToolArgumentParsers.Sysroot_GCC() };

    // POSIX compatible C compilers =================================
    {
      final IToolCommandlineParser cc = new ToolCommandlineParser("org.eclipse.cdt.core.gcc",
          new ResponseFileArgumentParsers.At(), BuiltinDetectionType.GCC_MAYBE, gcc_args);
      parserDetectors.add(new ParserDetector("cc", true, "exe", cc));
    }
    // POSIX compatible C++ compilers ===============================
    {
      final IToolCommandlineParser cxx = new ToolCommandlineParser("org.eclipse.cdt.core.g++",
          new ResponseFileArgumentParsers.At(), BuiltinDetectionType.GCC_MAYBE, gcc_args);
      parserDetectors.add(new ParserDetector("c\\+\\+", true, "exe", cxx));
    }

    // GNU C compatible compilers ====
    {
      final IToolCommandlineParser gcc = new ToolCommandlineParser("org.eclipse.cdt.core.gcc",
          new ResponseFileArgumentParsers.At(), BuiltinDetectionType.GCC, gcc_args);
      parserDetectors.add(new ParserDetector("gcc", true, "exe", gcc));
      parserDetectors.add(new ParserDetector("clang", true, "exe", gcc));
      // cross compilers, e.g. arm-none-eabi-gcc ====
      parserDetectors.add(new ParserDetector(".+-gcc", true, "exe", gcc));
    }
    // GNU C++ compatible compilers ====
    {
      final IToolCommandlineParser gxx = new ToolCommandlineParser("org.eclipse.cdt.core.g++",
          new ResponseFileArgumentParsers.At(), BuiltinDetectionType.GCC, gcc_args);
      parserDetectors.add(new ParserDetector("g\\+\\+", true, "exe", gxx));
      parserDetectors.add(new ParserDetector("clang\\+\\+", true, "exe", gxx));
      // cross compilers, e.g. arm-none-eabi-g++ ====
      parserDetectors.add(new ParserDetector(".+-g\\+\\+", true, "exe", gxx));
    }
    {
      // cross compilers, e.g. arm-none-eabi-c++ ====
      final IToolCommandlineParser cxx = new ToolCommandlineParser("org.eclipse.cdt.core.g++",
          new ResponseFileArgumentParsers.At(), BuiltinDetectionType.GCC_MAYBE, gcc_args);
      parserDetectors.add(new ParserDetector(".+-c\\+\\+", true, "exe", cxx));
    }

    // ms C + C++ compiler ==========================================
    {
      final IToolArgumentParser[] cl_cc_args = { new ToolArgumentParsers.IncludePath_C_CL(),
          new ToolArgumentParsers.MacroDefine_C_CL(), new ToolArgumentParsers.MacroUndefine_C_CL() };
      final IToolCommandlineParser cl = new ToolCommandlineParser(null, new ResponseFileArgumentParsers.At(),
          BuiltinDetectionType.NONE, cl_cc_args);
      parserDetectors.add(new ParserDetector("cl", true, "exe", cl));
    }
    // Intel C compilers ============================================
    {
      final IToolCommandlineParser icc = new ToolCommandlineParser("org.eclipse.cdt.core.gcc",
          new ResponseFileArgumentParsers.At(), BuiltinDetectionType.ICC, gcc_args);
      final IToolCommandlineParser icpc = new ToolCommandlineParser("org.eclipse.cdt.core.g++",
          new ResponseFileArgumentParsers.At(), BuiltinDetectionType.ICC, gcc_args);
      // Linux & OS X, EDG
      parserDetectors.add(new ParserDetector("icc", icc));
      // OS X, clang
      parserDetectors.add(new ParserDetector("icl", icc));
      // Intel C++ compiler
      // Linux & OS X, EDG
      parserDetectors.add(new ParserDetector("icpc", icpc));
      // OS X, clang
      parserDetectors.add(new ParserDetector("icl\\+\\+", icpc));
      // Windows C + C++, EDG
      parserDetectors.add(new ParserDetector("icl", true, "exe", icc));
    }

    // CUDA: nvcc compilers (POSIX compatible) =================================
    {
      final IToolArgumentParser[] nvcc_args = { new ToolArgumentParsers.IncludePath_C_POSIX(),
          new ToolArgumentParsers.MacroDefine_C_POSIX(), new ToolArgumentParsers.MacroUndefine_C_POSIX(),
          new ToolArgumentParsers.SystemIncludePath_nvcc(), new ToolArgumentParsers.SystemIncludePath_C(),
          new ToolArgumentParsers.LangStd_nvcc()};

      final IToolCommandlineParser nvcc = new ToolCommandlineParser("com.nvidia.cuda.toolchain.language.cuda.cu",
          new ResponseFileArgumentParsers.At(), BuiltinDetectionType.NVCC, nvcc_args);
      parserDetectors.add(new ParserDetector("nvcc", true, "exe", nvcc));
    }

    // ARM.com armclang compiler ====
    {
      final IToolArgumentParser[] armclang_args = { new ToolArgumentParsers.IncludePath_C_POSIX(),
          new ToolArgumentParsers.MacroDefine_C_POSIX(), new ToolArgumentParsers.MacroUndefine_C_POSIX(), };
      final IToolCommandlineParser armclang = new ToolCommandlineParser(null, new ResponseFileArgumentParsers.At(),
          BuiltinDetectionType.NONE, armclang_args);
      parserDetectors.add(new ParserDetector("armclang", true, "exe", armclang));
    }
    // ARM.com armcc compiler ====
    {
      final IToolArgumentParser[] armcc_args = { new ToolArgumentParsers.IncludePath_C_POSIX(),
          new ToolArgumentParsers.MacroDefine_C_POSIX(), new ToolArgumentParsers.MacroUndefine_C_POSIX(),
          new ToolArgumentParsers.SystemIncludePath_armcc() };
      final IToolCommandlineParser armcc = new ToolCommandlineParser(null, null,
          BuiltinDetectionType.ARMCC, armcc_args);
      parserDetectors.add(new ParserDetector("armcc", true, "exe", armcc));
    }
  }

  /** Just static methods */
  private ParserDetection() {
  }

  /**
   * Determines the parser detector that can parse the specified command-line.
   *
   * @param line
   *          the command line to process
   * @param versionSuffixRegex
   *          the regular expression to match a version suffix in the compiler
   *          name or {@code null} to not try to detect the compiler with a
   *          version suffix
   * @param tryWindowsDectors
   *          whether to also try the detectors for ms windows OS
   *
   * @return {@code null} if none of the detectors matches the tool name in the
   *         specified command-line string. Otherwise, if the tool name matches,
   *         a {@code ParserDetectionResult} holding the remaining command-line
   *         string (without the portion that matched) is returned.
   */
  public static ParserDetectionResult determineDetector(String line, String versionSuffixRegex,
      boolean tryWindowsDectors) {
    ParserDetectionResult result;
    if (DEBUG_PARSER_DETECTION) {
      System.out.printf("> Command-line '%s'%n", line);
      System.out.printf("> Looking up detector for command '%s ...'%n", line.substring(0, Math.min(40, line.length())));
    }
    // try default detectors
    result = determineDetector(line, parserDetectors, versionSuffixRegex, false);
    if (result == null && tryWindowsDectors) {
      // try with backslash as file separator on windows
      result = determineDetector(line, parserDetectors, versionSuffixRegex, true);
      if (result == null) {
        // try workaround for windows short file names
        final String shortPathExpanded = expandShortFileName(line);
        result = determineDetector(shortPathExpanded, parserDetectors, versionSuffixRegex, false);
        if (result == null) {
          // try with backslash as file separator on windows
          result = determineDetector(shortPathExpanded, parserDetectors, versionSuffixRegex, true);
        }
      }
    }
    if (result != null) {
      if (DEBUG_PARSER_DETECTION)
        System.out.printf("< Found detector for command '%s': %s (%s)%n", result.getCommandLine().getCommand(),
            result.getDetectorWithMethod().getDetector().getParser().getClass().getSimpleName(),
            result.getDetectorWithMethod().getHow());
    }
    return result;
  }

  /**
   * Determines a C-compiler-command line parser that is able to parse the relevant arguments in the specified command
   * line.
   *
   * @param commandLine
   *          the command line to process
   * @param detectors
   *          the detectors to try
   * @param versionSuffixRegex
   *          the regular expression to match a version suffix in the compiler name or {@code null} to not try to detect
   *          the compiler with a version suffix
   * @param matchBackslash
   *          whether to match on file system paths with backslashes in the compiler argument or to match an paths with
   *          forward slashes
   * @return {@code null} if none of the detectors matches the tool name in the specified command-line string.
   *         Otherwise, if the tool name matches, a {@code ParserDetectionResult} holding the de-compose command-line is
   *         returned.
   */
  private static ParserDetectionResult determineDetector(String commandLine, List<ParserDetector> detectors,
      String versionSuffixRegex, boolean matchBackslash) {
    ParserDetector.MatchResult cmdline;
    // try basenames
    for (ParserDetector pd : detectors) {
      if (DEBUG_PARSER_DETECTION)
        System.out.printf("  Trying detector %s (%s)%n", pd.getParser().getClass().getSimpleName(),
            DetectorWithMethod.DetectionMethod.BASENAME);
      if ((cmdline = pd.basenameMatches(commandLine, matchBackslash)) != null) {
        return new ParserDetectionResult(new DetectorWithMethod(pd, DetectorWithMethod.DetectionMethod.BASENAME, matchBackslash),
            cmdline);
      }
    }
    if (versionSuffixRegex != null) {
      // try with version pattern
      for (ParserDetector pd : detectors) {
        if (DEBUG_PARSER_DETECTION)
          System.out.printf("  Trying detector %s (%s)%n", pd.getParser().getClass().getSimpleName(), DetectorWithMethod.DetectionMethod.WITH_VERSION);
        if ((cmdline = pd.basenameWithVersionMatches(commandLine, matchBackslash, versionSuffixRegex)) != null) {
          return new ParserDetectionResult(new DetectorWithMethod(pd, DetectorWithMethod.DetectionMethod.WITH_VERSION, matchBackslash),
              cmdline);
        }
      }
    }
    // try with extension
    for (ParserDetector pd : detectors) {
      if (DEBUG_PARSER_DETECTION)
        System.out.printf("  Trying detector %s (%s)%n", pd.getParser().getClass().getSimpleName(),
            DetectorWithMethod.DetectionMethod.WITH_EXTENSION);
      if ((cmdline = pd.basenameWithExtensionMatches(commandLine, matchBackslash)) != null) {
        return new ParserDetectionResult(new DetectorWithMethod(pd, DetectorWithMethod.DetectionMethod.WITH_EXTENSION, matchBackslash),
            cmdline);
      }
    }
    if (versionSuffixRegex != null) {
      // try with extension and version
      for (ParserDetector pd : detectors) {
        if (DEBUG_PARSER_DETECTION)
          System.out.printf("  Trying detector %s (%s)%n", pd.getParser().getClass().getSimpleName() + " ("
              + DetectorWithMethod.DetectionMethod.WITH_VERSION_EXTENSION);
        if ((cmdline = pd.basenameWithVersionAndExtensionMatches(commandLine, matchBackslash,
            versionSuffixRegex)) != null) {
          return new ParserDetectionResult(
              new DetectorWithMethod(pd, DetectorWithMethod.DetectionMethod.WITH_VERSION_EXTENSION, matchBackslash), cmdline);
        }
      }
    }

    return null;
  }

  /**
   * Tries to convert windows short file names for the compiler executable (like
   * {@code AVR-G_~1.EXE}) into their long representation. This is a
   * workaround for a
   * <a href="https://gitlab.kitware.com/cmake/cmake/issues/16138">bug in CMake
   * under windows</a>.<br>
   * See <a href="https://github.com/15knots/cmake4eclipse/issues/31">issue #31
   */
  private static String expandShortFileName(String commandLine) {
    if (commandLine.indexOf('~', 6) == -1) {
      // not a short file name
      return commandLine;
    }
    String command;
    StringBuilder commandLine2 = new StringBuilder();
    // split at first space character
    int idx = commandLine.indexOf(' ');
    if (idx != -1) {
      command = commandLine.substring(0, idx);
      commandLine2.append(commandLine.substring(idx));
    } else {
      command = commandLine;
    }
    // convert to long file name and retry lookup
    try {
      command = new File(command).getCanonicalPath();
      commandLine2.insert(0, command);
      return commandLine2.toString();
    } catch (IOException e) {
      log.log(new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID, command, e));
    }
    return commandLine;
  }

  // has package scope for unittest purposes
  static class DetectorWithMethod {
    enum DetectionMethod {
      BASENAME, WITH_VERSION, WITH_EXTENSION, WITH_VERSION_EXTENSION;
    }

    /**
     * the ParserDetector that matched the name of the tool on a given
     * command-line
     */
    private final ParserDetector detector;
    /** describes the method that was used to match */
    private final DetectionMethod how;
    private final boolean matchBackslash;

    /**
     * @param detector
     *          the ParserDetector that matched the name of the tool on a given command-line
     * @param how
     *          describes the method that was used to match
     * @param matchBackslash
     *          whether the match is on file system paths with backslashes in the compiler argument or to match an paths
     *          with forward slashes
     */
    public DetectorWithMethod(ParserDetector detector, DetectionMethod how, boolean matchBackslash) {
      if (detector == null)
        throw new NullPointerException("detector");
      if (how == null)
        throw new NullPointerException("how");
      this.detector = detector;
      this.how = how;
      this.matchBackslash= matchBackslash;
    }

    /**
     * Gets the ParserDetector that matched the name of the tool on a given
     * command-line.
     *
     * @return the detector, never {@code null}
     */
    public ParserDetector getDetector() {
      return detector;
    }

    /**
     * Gets the method that was used to match.
     *
     * @return the detection method, never {@code null}
     */
    public DetectionMethod getHow() {
      return how;
    }

    /**
     * @return the matchBackslash
     */
    public boolean isMatchBackslash() {
      return matchBackslash;
    }

  }

  // has package scope for unittest purposes
  static class ParserDetectionResult {

    private final DetectorWithMethod detectorWMethod;
    private final ParserDetector.MatchResult commandLine;

    /**
     * @param detectorWMethod
     *          the ParserDetector that matched the name of the tool on a given
     *          command-line
     * @param commandLine
     *          the de-composed command-line, after the matcher has matched the
     *          tool name
     */
    public ParserDetectionResult(DetectorWithMethod detectorWMethod, ParserDetector.MatchResult commandLine) {
      this.detectorWMethod = detectorWMethod;
      this.commandLine = commandLine;
    }

    /** Gets the de-composed command-line.
     */
    public ParserDetector.MatchResult getCommandLine() {
      return commandLine;
    }

    /**
     * Gets the remaining arguments of the command-line, after the matcher has
     * matched the tool name (i.e. without the command).
     */
    public String getReducedCommandLine() {
      return this.commandLine.getArguments();
    }

    /**
     * Gets the ParserDetector that matched the name of the tool on a given
     * command-line
     *
     * @return the detectorWMethod
     */
    public DetectorWithMethod getDetectorWithMethod() {
      return detectorWMethod;
    }
  }

}
