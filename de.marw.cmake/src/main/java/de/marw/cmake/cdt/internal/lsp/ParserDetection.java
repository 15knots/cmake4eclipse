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
import de.marw.cmake.cdt.internal.lsp.builtins.ArmccBuiltinDetectionBehavior;
import de.marw.cmake.cdt.internal.lsp.builtins.GccBuiltinDetectionBehavior;
import de.marw.cmake.cdt.internal.lsp.builtins.MaybeGccBuiltinDetectionBehavior;
import de.marw.cmake.cdt.internal.lsp.builtins.NvccBuiltinDetectionBehavior;
import de.marw.cmake.cdt.language.settings.providers.IArglet;
import de.marw.cmake.cdt.language.settings.providers.IToolCommandlineParser;
import de.marw.cmake.cdt.language.settings.providers.IToolDetectionParticiant;
import de.marw.cmake.cdt.language.settings.providers.DefaultToolDetectionParticiant;
import de.marw.cmake.cdt.language.settings.providers.ResponseFileArglets;
import de.marw.cmake.cdt.language.settings.providers.builtins.IBuiltinsDetectionBehavior;
import de.marw.cmake.cdt.language.settings.providers.Arglets;
import de.marw.cmake.cdt.language.settings.providers.DefaultToolCommandlineParser;

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
  private static final List<DefaultToolDetectionParticiant> parserDetectors = new ArrayList<>(22);

  static {
    /** Names of known tools along with their command line argument parsers */
    final IArglet[] gcc_args = { new Arglets.IncludePath_C_POSIX(),
        new Arglets.MacroDefine_C_POSIX(), new Arglets.MacroUndefine_C_POSIX(),
        // not defined by POSIX, but does not harm..
        new Arglets.SystemIncludePath_C(), new Arglets.LangStd_GCC(),
        new Arglets.Sysroot_GCC() };

    IBuiltinsDetectionBehavior btbGccMaybee= new MaybeGccBuiltinDetectionBehavior();
    IBuiltinsDetectionBehavior btbGcc= new GccBuiltinDetectionBehavior();

    // POSIX compatible C compilers =================================
    {
      final IToolCommandlineParser cc = new DefaultToolCommandlineParser("org.eclipse.cdt.core.gcc",
          new ResponseFileArglets.At(), btbGccMaybee, gcc_args);
      parserDetectors.add(new DefaultToolDetectionParticiant("cc", true, "exe", cc));
    }
    // POSIX compatible C++ compilers ===============================
    {
      final IToolCommandlineParser cxx = new DefaultToolCommandlineParser("org.eclipse.cdt.core.g++",
          new ResponseFileArglets.At(), btbGccMaybee, gcc_args);
      parserDetectors.add(new DefaultToolDetectionParticiant("c\\+\\+", true, "exe", cxx));
    }

    // GNU C compatible compilers ====
    {
      final IToolCommandlineParser gcc = new DefaultToolCommandlineParser("org.eclipse.cdt.core.gcc",
          new ResponseFileArglets.At(), btbGcc, gcc_args);
      parserDetectors.add(new DefaultToolDetectionParticiant("gcc", true, "exe", gcc));
      parserDetectors.add(new DefaultToolDetectionParticiant("clang", true, "exe", gcc));
      // cross compilers, e.g. arm-none-eabi-gcc ====
      parserDetectors.add(new DefaultToolDetectionParticiant(".+-gcc", true, "exe", gcc));
    }
    // GNU C++ compatible compilers ====
    {
      final IToolCommandlineParser gxx = new DefaultToolCommandlineParser("org.eclipse.cdt.core.g++",
          new ResponseFileArglets.At(), btbGcc, gcc_args);
      parserDetectors.add(new DefaultToolDetectionParticiant("g\\+\\+", true, "exe", gxx));
      parserDetectors.add(new DefaultToolDetectionParticiant("clang\\+\\+", true, "exe", gxx));
      // cross compilers, e.g. arm-none-eabi-g++ ====
      parserDetectors.add(new DefaultToolDetectionParticiant(".+-g\\+\\+", true, "exe", gxx));
    }
    {
      // cross compilers, e.g. arm-none-eabi-c++ ====
      final IToolCommandlineParser cxx = new DefaultToolCommandlineParser("org.eclipse.cdt.core.g++",
          new ResponseFileArglets.At(), btbGccMaybee, gcc_args);
      parserDetectors.add(new DefaultToolDetectionParticiant(".+-c\\+\\+", true, "exe", cxx));
    }

    // ms C + C++ compiler ==========================================
    {
      final IArglet[] cl_cc_args = { new Arglets.IncludePath_C_CL(),
          new Arglets.MacroDefine_C_CL(), new Arglets.MacroUndefine_C_CL() };
      final IToolCommandlineParser cl = new DefaultToolCommandlineParser(null, new ResponseFileArglets.At(),
          null, cl_cc_args);
      parserDetectors.add(new DefaultToolDetectionParticiant("cl", true, "exe", cl));
    }
    // Intel C compilers ============================================
    {
      // for the recod: builtin detection: -EP -dM for macros, -H for include FILES. NOTE: Windows: /QdM.
      final IToolCommandlineParser icc = new DefaultToolCommandlineParser("org.eclipse.cdt.core.gcc",
          new ResponseFileArglets.At(), null, gcc_args);
      final IToolCommandlineParser icpc = new DefaultToolCommandlineParser("org.eclipse.cdt.core.g++",
          new ResponseFileArglets.At(), null, gcc_args);
      // Linux & OS X, EDG
      parserDetectors.add(new DefaultToolDetectionParticiant("icc", icc));
      // OS X, clang
      parserDetectors.add(new DefaultToolDetectionParticiant("icl", icc));
      // Intel C++ compiler
      // Linux & OS X, EDG
      parserDetectors.add(new DefaultToolDetectionParticiant("icpc", icpc));
      // OS X, clang
      parserDetectors.add(new DefaultToolDetectionParticiant("icl\\+\\+", icpc));
      // Windows C + C++, EDG
      parserDetectors.add(new DefaultToolDetectionParticiant("icl", true, "exe", icc));
    }

    // CUDA: nvcc compilers (POSIX compatible) =================================
    {
      final IArglet[] nvcc_args = { new Arglets.IncludePath_C_POSIX(),
          new Arglets.MacroDefine_C_POSIX(), new Arglets.MacroUndefine_C_POSIX(),
          new Arglets.SystemIncludePath_nvcc(), new Arglets.SystemIncludePath_C(),
          new Arglets.LangStd_nvcc()};

      final IToolCommandlineParser nvcc = new DefaultToolCommandlineParser("com.nvidia.cuda.toolchain.language.cuda.cu",
          new ResponseFileArglets.At(), new NvccBuiltinDetectionBehavior(), nvcc_args);
      parserDetectors.add(new DefaultToolDetectionParticiant("nvcc", true, "exe", nvcc));
    }

    // ARM.com armclang compiler ====
    {
      final IArglet[] armclang_args = { new Arglets.IncludePath_C_POSIX(),
          new Arglets.MacroDefine_C_POSIX(), new Arglets.MacroUndefine_C_POSIX(), };
      final IToolCommandlineParser armclang = new DefaultToolCommandlineParser(null, new ResponseFileArglets.At(),
          null, armclang_args);
      parserDetectors.add(new DefaultToolDetectionParticiant("armclang", true, "exe", armclang));
    }
    // ARM.com armcc compiler ====
    {
      final IArglet[] armcc_args = { new Arglets.IncludePath_C_POSIX(),
          new Arglets.MacroDefine_C_POSIX(), new Arglets.MacroUndefine_C_POSIX(),
          new Arglets.SystemIncludePath_armcc() };
      final IToolCommandlineParser armcc = new DefaultToolCommandlineParser(null, null,
          new ArmccBuiltinDetectionBehavior(), armcc_args);
      parserDetectors.add(new DefaultToolDetectionParticiant("armcc", true, "exe", armcc));
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
  private static ParserDetectionResult determineDetector(String commandLine, List<DefaultToolDetectionParticiant> detectors,
      String versionSuffixRegex, boolean matchBackslash) {
    DefaultToolDetectionParticiant.MatchResult cmdline;
    // try basenames
    for (IToolDetectionParticiant pd : detectors) {
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
      for (IToolDetectionParticiant pd : detectors) {
        if (DEBUG_PARSER_DETECTION)
          System.out.printf("  Trying detector %s (%s)%n", pd.getParser().getClass().getSimpleName(), DetectorWithMethod.DetectionMethod.WITH_VERSION);
        if ((cmdline = pd.basenameWithVersionMatches(commandLine, matchBackslash, versionSuffixRegex)) != null) {
          return new ParserDetectionResult(new DetectorWithMethod(pd, DetectorWithMethod.DetectionMethod.WITH_VERSION, matchBackslash),
              cmdline);
        }
      }
    }
    // try with extension
    for (IToolDetectionParticiant pd : detectors) {
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
      for (IToolDetectionParticiant pd : detectors) {
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
     * the DefaultToolDetectionParticiant that matched the name of the tool on a given
     * command-line
     */
    private final IToolDetectionParticiant detector;
    /** describes the method that was used to match */
    private final DetectionMethod how;
    private final boolean matchBackslash;

    /**
     * @param detector
     *          the DefaultToolDetectionParticiant that matched the name of the tool on a given command-line
     * @param how
     *          describes the method that was used to match
     * @param matchBackslash
     *          whether the match is on file system paths with backslashes in the compiler argument or to match an paths
     *          with forward slashes
     */
    public DetectorWithMethod(IToolDetectionParticiant detector, DetectionMethod how, boolean matchBackslash) {
      if (detector == null)
        throw new NullPointerException("detector");
      if (how == null)
        throw new NullPointerException("how");
      this.detector = detector;
      this.how = how;
      this.matchBackslash= matchBackslash;
    }

    /**
     * Gets the DefaultToolDetectionParticiant that matched the name of the tool on a given
     * command-line.
     *
     * @return the detector, never {@code null}
     */
    public IToolDetectionParticiant getDetector() {
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
    private final DefaultToolDetectionParticiant.MatchResult commandLine;

    /**
     * @param detectorWMethod
     *          the DefaultToolDetectionParticiant that matched the name of the tool on a given
     *          command-line
     * @param commandLine
     *          the de-composed command-line, after the matcher has matched the
     *          tool name
     */
    public ParserDetectionResult(DetectorWithMethod detectorWMethod, DefaultToolDetectionParticiant.MatchResult commandLine) {
      this.detectorWMethod = detectorWMethod;
      this.commandLine = commandLine;
    }

    /** Gets the de-composed command-line.
     */
    public DefaultToolDetectionParticiant.MatchResult getCommandLine() {
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
     * Gets the DefaultToolDetectionParticiant that matched the name of the tool on a given
     * command-line
     *
     * @return the detectorWMethod
     */
    public DetectorWithMethod getDetectorWithMethod() {
      return detectorWMethod;
    }
  }

}
