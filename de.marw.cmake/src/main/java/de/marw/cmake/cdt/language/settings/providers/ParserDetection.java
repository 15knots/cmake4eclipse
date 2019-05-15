/*******************************************************************************
 * Copyright (c) 2017 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cmake.cdt.language.settings.providers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import de.marw.cmake.CMakePlugin;
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

  /**
   * tool detectors and their tool option parsers for each tool of interest that
   * takes part in the current build. The Matcher detects whether a command line
   * is an invocation of the tool.
   */
  private static final List<ParserDetector> parserDetectors = new ArrayList<>(22);
  /**
   * same as {@link #parserDetectors}, but for windows where cmake places a
   * backslash in the command path. Unused ({@code null}), when running
   * under *nix.
   */
  static final List<ParserDetector> parserDetectorsWin32 = new ArrayList<>(16);

  static {
    /** Names of known tools along with their command line argument parsers */
    final IToolArgumentParser[] gcc_args = { new ToolArgumentParsers.IncludePath_C_POSIX(),
        new ToolArgumentParsers.MacroDefine_C_POSIX(), new ToolArgumentParsers.MacroUndefine_C_POSIX(),
        // not defined by POSIX, but does not harm..
        new ToolArgumentParsers.SystemIncludePath_C(), new ToolArgumentParsers.LangStd_GCC(),
        new ToolArgumentParsers.Sysroot_GCC() };

    // POSIX compatible C compilers =================================
    {
      final ToolCommandlineParser cc = new ToolCommandlineParser("org.eclipse.cdt.core.gcc",
          new ResponseFileArgumentParsers.At(), BuiltinDetectionType.GCC_MAYBE, gcc_args);
      parserDetectors.add(new ParserDetector("cc", cc));
      parserDetectors.add(new ParserDetectorExt("cc", "exe", cc));
    }
    // POSIX compatible C++ compilers ===============================
    {
      final ToolCommandlineParser cxx = new ToolCommandlineParser("org.eclipse.cdt.core.g++",
          new ResponseFileArgumentParsers.At(), BuiltinDetectionType.GCC_MAYBE, gcc_args);
      parserDetectors.add(new ParserDetector("c\\+\\+", cxx));
      parserDetectors.add(new ParserDetectorExt("c\\+\\+", "exe", cxx));
    }

    // GNU C compatible compilers ====
    {
      final ToolCommandlineParser gcc = new ToolCommandlineParser("org.eclipse.cdt.core.gcc",
          new ResponseFileArgumentParsers.At(), BuiltinDetectionType.GCC, gcc_args);
      parserDetectors.add(new ParserDetector("gcc", gcc));
      parserDetectors.add(new ParserDetectorExt("gcc", "exe", gcc));
      parserDetectors.add(new ParserDetector("clang", gcc));
      parserDetectors.add(new ParserDetectorExt("clang", "exe", gcc));
      // cross compilers, e.g. arm-none-eabi-gcc ====
      parserDetectors.add(new ParserDetector(".+-gcc", gcc));
      parserDetectors.add(new ParserDetectorExt(".+-gcc", "exe", gcc));
    }
    // GNU C++ compatible compilers ====
    {
      final ToolCommandlineParser gxx = new ToolCommandlineParser("org.eclipse.cdt.core.g++",
          new ResponseFileArgumentParsers.At(), BuiltinDetectionType.GCC, gcc_args);
      parserDetectors.add(new ParserDetector("g\\+\\+", gxx));
      parserDetectors.add(new ParserDetectorExt("g\\+\\+", "exe", gxx));
      parserDetectors.add(new ParserDetector("clang\\+\\+", gxx));
      parserDetectors.add(new ParserDetectorExt("clang\\+\\+", "exe", gxx));
      // cross compilers, e.g. arm-none-eabi-gcc ====
      parserDetectors.add(new ParserDetector(".+-g\\+\\+", gxx));
      parserDetectors.add(new ParserDetectorExt(".+-g\\+\\+", "exe", gxx));
    }
    // ms C + C++ compiler ==========================================
    {
      final IToolArgumentParser[] cl_cc_args = { new ToolArgumentParsers.IncludePath_C_CL(),
          new ToolArgumentParsers.MacroDefine_C_CL(), new ToolArgumentParsers.MacroUndefine_C_CL() };
      final ToolCommandlineParser cl = new ToolCommandlineParser(null, new ResponseFileArgumentParsers.At(),
          BuiltinDetectionType.NONE, cl_cc_args);
      parserDetectors.add(new ParserDetectorExt("cl", "exe", cl));
    }
    // Intel C compilers ============================================
    {
      final ToolCommandlineParser icc = new ToolCommandlineParser("org.eclipse.cdt.core.gcc",
          new ResponseFileArgumentParsers.At(), BuiltinDetectionType.ICC, gcc_args);
      final ToolCommandlineParser icpc = new ToolCommandlineParser("org.eclipse.cdt.core.g++",
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
      parserDetectors.add(new ParserDetectorExt("icl", "exe", icc));
    }

    // CUDA: nvcc compilers (POSIX compatible) =================================
    {
      final IToolArgumentParser[] nvcc_args = { new ToolArgumentParsers.IncludePath_C_POSIX(),
          new ToolArgumentParsers.MacroDefine_C_POSIX(), new ToolArgumentParsers.MacroUndefine_C_POSIX(),
          new ToolArgumentParsers.SystemIncludePath_nvcc(), new ToolArgumentParsers.SystemIncludePath_C(),
          new ToolArgumentParsers.LangStd_nvcc()};

      final ToolCommandlineParser nvcc = new ToolCommandlineParser("com.nvidia.cuda.toolchain.language.cuda.cu",
          new ResponseFileArgumentParsers.At(), BuiltinDetectionType.NVCC, nvcc_args);
      parserDetectors.add(new ParserDetector("nvcc", nvcc));
      parserDetectors.add(new ParserDetectorExt("nvcc", "exe", nvcc));
    }

    // add detectors for windows NTFS
    for (ParserDetector pd : parserDetectors) {
      if (pd instanceof ParserDetectorExt) {
        ParserDetectorExt pde = (ParserDetectorExt) pd;
        parserDetectorsWin32.add(new ParserDetectorExt(pde.basenameRegex, true, pde.extensionRegex, pde.getParser()));
      } else {
        parserDetectorsWin32.add(new ParserDetector(pd.basenameRegex, true, pd.getParser()));
      }
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
    // try default detectors
    result = determineDetector(line, parserDetectors, versionSuffixRegex);
    if (result == null && tryWindowsDectors) {
      // try with backslash as file separator on windows
      result = determineDetector(line, parserDetectorsWin32, versionSuffixRegex);
      if (result == null) {
        // try workaround for windows short file names
        final String shortPathExpanded = expandShortFileName(line);
        result = determineDetector(shortPathExpanded, parserDetectorsWin32, versionSuffixRegex);
        if (result == null) {
          // try for cmake from mingw suite with short file names under windows
          result = determineDetector(shortPathExpanded, parserDetectors, versionSuffixRegex);
        }
      }
    }
    return result;
  }

  /**
   * Determines a C-compiler-command line parser that is able to parse the
   * relevant arguments in the specified command line.
   *
   * @param commandLine
   *          the command line to process
   * @param detectors
   *          the detectors to try
   * @param versionSuffixRegex
   *          the regular expression to match a version suffix in the compiler
   *          name or {@code null} to not try to detect the compiler with a
   *          version suffix
   * @return {@code null} if none of the detectors matches the tool name in the
   *         specified command-line string. Otherwise, if the tool name matches,
   *         a {@code ParserDetectionResult} holding the de-compose command-line
   *         is returned.
   */
  private static ParserDetectionResult determineDetector(String commandLine, List<ParserDetector> detectors,
      String versionSuffixRegex) {
    MatchResult cmdline;
    // try basenames
    for (ParserDetector pd : detectors) {
      if ((cmdline = pd.basenameMatches(commandLine)) != null) {
        return new ParserDetectionResult(new DetectorWithMethod(pd, DetectorWithMethod.DetectionMethod.BASENAME),
            cmdline);
      }
    }
    if (versionSuffixRegex != null) {
      // try with version pattern
      for (ParserDetector pd : detectors) {
        if ((cmdline = pd.basenameWithVersionMatches(commandLine, versionSuffixRegex)) != null) {
          return new ParserDetectionResult(new DetectorWithMethod(pd, DetectorWithMethod.DetectionMethod.WITH_VERSION),
              cmdline);
        }
      }
    }
    // try with extension
    for (ParserDetector pd : detectors) {
      if (pd instanceof ParserDetectorExt
          && (cmdline = ((ParserDetectorExt) pd).basenameWithExtensionMatches(commandLine)) != null) {
        return new ParserDetectionResult(new DetectorWithMethod(pd, DetectorWithMethod.DetectionMethod.WITH_EXTENSION),
            cmdline);
      }
    }
    if (versionSuffixRegex != null) {
      // try with extension and version
      for (ParserDetector pd : detectors) {
        if (pd instanceof ParserDetectorExt && (cmdline = ((ParserDetectorExt) pd)
            .basenameWithVersionAndExtensionMatches(commandLine, versionSuffixRegex)) != null) {
          return new ParserDetectionResult(
              new DetectorWithMethod(pd, DetectorWithMethod.DetectionMethod.WITH_VERSION_EXTENSION), cmdline);
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
    String command;
    // split at first space character
    StringBuilder commandLine2 = new StringBuilder();
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
      log.log(new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID, "CompileCommandsJsonParser#expandShortFileName()", e));
    }
    return null;
  }

  /**
   * Responsible to match the first argument (the tool command) of a
   * command-line.
   *
   * @author Martin Weber
   */
  static class ParserDetector {
    /** pattern part that matches linux filesystem paths */
    protected static final String REGEX_CMD_HEAD = "^(.*?/)??(";
    /** pattern part that matches win32 filesystem paths */
    protected static final String REGEX_CMD_HEAD_WIN = "^(.*?" + Pattern.quote("\\") + ")??(";
    protected static final String REGEX_CMD_TAIL = ")\\s";

    /**
     * the Matcher that matches the name of the tool (including its path, BUT
     * WITHOUT its filename extension) on a given command-line
     */
    private final Matcher toolNameMatcher;
    /**
     * the corresponding parser for the tool arguments
     */
    private final IToolCommandlineParser parser;
    protected final String basenameRegex;
    /**
     * whether to match a Linux path (which has a forward slash) or a Windows
     * path with backSlashes
     */
    protected final boolean matchBackslash;

    /**
     * Creates a {@code ParserDetector} that matches linux paths in the tool
     * name.
     *
     * @param basenameRegex
     *          a regular expression that matches the base name of the tool to
     *          detect
     * @param parser
     *          the corresponding parser for the tool arguments
     * @throws NullPointerException
     *           if one of the arguments is {@code null}
     */
    public ParserDetector(String basenameRegex, IToolCommandlineParser parser) {
      this(basenameRegex, false, parser);
    }

    /**
     * Creates a {@code ParserDetector}.
     *
     * @param basenameRegex
     *          a regular expression that matches the base name of the tool to
     *          detect
     * @param matchBackslash
     *          whether to match a Linux path (which has a forward slash) or a
     *          Windows path with backSlashes.
     * @param parser
     *          the corresponding parser for the tool arguments
     */
    public ParserDetector(String basenameRegex, boolean matchBackslash, IToolCommandlineParser parser) {
      this.toolNameMatcher = matchBackslash
          ? Pattern.compile(REGEX_CMD_HEAD_WIN + basenameRegex + REGEX_CMD_TAIL).matcher("")
          : Pattern.compile(REGEX_CMD_HEAD + basenameRegex + REGEX_CMD_TAIL).matcher("");
      this.basenameRegex = basenameRegex;
      this.parser = parser;
      this.matchBackslash = matchBackslash;
    }

    /**
     * Gets the IToolCommandlineParser.
     *
     * @return the parser, never {@code null}
     */
    public IToolCommandlineParser getParser() {
      return parser;
    }

    /**
     * Gets, whether the parser for the tool arguments can properly parse the
     * specified command-line string. If so, the remaining arguments of the
     * command-line are returned.
     *
     * @param commandLine
     *          the command line to match
     *
     * @return {@code null} if the matcher did not match the tool name in the
     *         command-line string. Otherwise, if the tool name matches, a
     *         MatchResult holding the de-composed command-line is returned.
     */
    public MatchResult basenameMatches(String commandLine) {
      return matcherMatches(toolNameMatcher, commandLine);
    }

    /**
     * Gets, whether the parser for the tool arguments can properly parse the
     * specified command-line string. If so, the remaining arguments of the
     * command-line are returned. This is time-consuming, since it creates a
     * Matcher object on each invocation.
     *
     * @param commandLine
     *          the command-line to match
     * @param versionRegex
     *          a regular expression that matches the version string in the name
     *          of the tool to detect.
     *
     * @return {@code null} if the matcher did not match the tool name in the
     *         command-line string. Otherwise, if the tool name matches, a
     *         MatchResult holding the de-composed command-line is returned.
     */
    public MatchResult basenameWithVersionMatches(String commandLine, String versionRegex) {
      Matcher matcher = Pattern.compile(REGEX_CMD_HEAD + basenameRegex + versionRegex + REGEX_CMD_TAIL).matcher("");
      return matcherMatches(matcher, commandLine);
    }

    /**
     * Gets, whether the specified Matcher for the tool arguments can properly
     * parse the specified command-line string. If so, the remaining arguments
     * of the command-line are returned.
     *
     * @param matcher
     *          the matcher that performs the match a regular expression that
     *          matches the version string in the name of the tool to detect.
     * @param commandLine
     *          the command-line to match
     *
     * @return {@code null} if the matcher did not match the tool name in the
     *         command-line string. Otherwise, if the tool name matches, a
     *         MatchResult holding the de-composed command-line is returned.
     */
    protected final MatchResult matcherMatches(Matcher matcher, String commandLine) {
      matcher.reset(commandLine);
      if (matcher.lookingAt()) {
        return new MatchResult(commandLine.substring(matcher.start(), matcher.end()).trim(),
            commandLine.substring(matcher.end()));
      }
      return null;
    }
  } // ParserDetector

  /**
   * Same as {@link ParserDetection.ParserDetector}, but handles tool detection
   * properly, if tool versions are allowed and the tool name contains a
   * filename extension.
   *
   * @author Martin Weber
   */
  static class ParserDetectorExt extends ParserDetector {
    /**
     * the Matcher that matches the name of the tool (including its path AND its
     * filename extension) on a given command-line or {@code null}
     */
    private final Matcher toolNameMatcherExt;
    private final String extensionRegex;

    /**
     * a {@code ParserDetectorExt} that matches linux paths in the tool name.
     *
     * @param basenameRegex
     *          a regular expression that matches the base name of the tool to
     *          detect.
     * @param extensionRegex
     *          a regular expression that matches the filename extension of the
     *          tool to detect .
     * @param parser
     *          the corresponding parser for the tool arguments
     */
    public ParserDetectorExt(String basenameRegex, String extensionRegex, IToolCommandlineParser parser) {
      this(basenameRegex, false, extensionRegex, parser);
    }

    /**
     * Creates a {@code ParserDetector}.
     *
     * @param basenameRegex
     *          a regular expression that matches the base name of the tool to
     *          detect
     * @param matchBackslash
     *          whether to match a Linux path (which has a forward slash) or a
     *          Windows path with backSlashes.
     * @param extensionRegex
     *          a regular expression that matches the filename extension of the
     *          tool to detect .
     * @param parser
     *          the corresponding parser for the tool arguments
     */
    public ParserDetectorExt(String basenameRegex, boolean matchBackslash, String extensionRegex,
        IToolCommandlineParser parser) {
      super(basenameRegex, matchBackslash, parser);
      String head = matchBackslash ? REGEX_CMD_HEAD_WIN : REGEX_CMD_HEAD;
      this.toolNameMatcherExt = Pattern
          .compile(head + basenameRegex + Pattern.quote(".") + extensionRegex + REGEX_CMD_TAIL).matcher("");
      this.extensionRegex = extensionRegex;
    }

    /**
     * Gets, whether the parser for the tool arguments can properly parse the
     * specified command-line string. If so, the remaining arguments of the
     * command-line are returned.
     *
     * @param commandLine
     *          the command-line to match
     * @return {@code null} if the matcher did not match the tool name in the
     *         command-line string. Otherwise, if the tool name matches, a
     *         MatchResult holding the de-composed command-line is returned.
     */
    public MatchResult basenameWithExtensionMatches(String commandLine) {
      return matcherMatches(toolNameMatcherExt, commandLine);
    }

    /**
     * Gets, whether the parser for the tool arguments can properly parse the
     * specified command-line string. If so, the remaining arguments of the
     * command-line are returned. This is time-consuming, since it creates a
     * Matcher object on each invocation.
     *
     * @param commandLine
     *          the command-line to match
     * @param versionRegex
     *          a regular expression that matches the version string in the name
     *          of the tool to detect.
     *
     * @return {@code null} if the matcher did not match the tool name in the
     *         command-line string. Otherwise, if the tool name matches, a
     *         MatchResult holding the de-composed command-line is returned.
     */
    public MatchResult basenameWithVersionAndExtensionMatches(String commandLine, String versionRegex) {
      String head = matchBackslash ? REGEX_CMD_HEAD_WIN : REGEX_CMD_HEAD;
      Matcher matcher = Pattern
          .compile(head + basenameRegex + versionRegex + Pattern.quote(".") + extensionRegex + REGEX_CMD_TAIL)
          .matcher("");
      return matcherMatches(matcher, commandLine);
    }

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

    /**
     * @param detector
     *          the ParserDetector that matched the name of the tool on a given
     *          command-line
     * @param how
     *          describes the method that was used to match
     */
    public DetectorWithMethod(ParserDetector detector, DetectionMethod how) {
      if (detector == null)
        throw new NullPointerException("detector");
      if (how == null)
        throw new NullPointerException("how");
      this.detector = detector;
      this.how = how;
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

  }

  /** The result of matching a commandline string.
   */
  static class MatchResult {
    private final String command;
    private final String arguments;

    /**
     * @param command
     *          the command from the command-line, without the argument string
     * @param arguments
     *          the remaining arguments from the command-line, without the
     *          command
     */
    public MatchResult(String command, String arguments) {
      this.command = command;
      this.arguments = arguments;
    }

    /**
     * Gets the command from the command-line, without the argument string.
     */
    public String getCommand() {
      return this.command;
    }

    /**
     * Gets the remaining arguments from the command-line, without the command.
     */
    public String getArguments() {
      return this.arguments;
    }
  }

  // has package scope for unittest purposes
  static class ParserDetectionResult {

    private final DetectorWithMethod detectorWMethod;
    private final MatchResult commandLine;

    /**
     * @param detectorWMethod
     *          the ParserDetector that matched the name of the tool on a given
     *          command-line
     * @param commandLine
     *          the de-composed command-line, after the matcher has matched the
     *          tool name
     */
    public ParserDetectionResult(DetectorWithMethod detectorWMethod, MatchResult commandLine) {
      this.detectorWMethod = detectorWMethod;
      this.commandLine = commandLine;
    }

    /** Gets the de-composed command-line.
     */
    public MatchResult getCommandLine() {
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
