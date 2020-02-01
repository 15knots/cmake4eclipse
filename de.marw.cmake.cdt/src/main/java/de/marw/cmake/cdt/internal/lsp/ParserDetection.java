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
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import de.marw.cmake.cdt.internal.CMakePlugin;
import de.marw.cmake.cdt.internal.lsp.builtins.GccBuiltinDetectionBehavior;
import de.marw.cmake.cdt.internal.lsp.builtins.MaybeGccBuiltinDetectionBehavior;
import de.marw.cmake.cdt.lsp.Arglets;
import de.marw.cmake.cdt.lsp.DefaultToolCommandlineParser;
import de.marw.cmake.cdt.lsp.DefaultToolDetectionParticipant;
import de.marw.cmake.cdt.lsp.IArglet;
import de.marw.cmake.cdt.lsp.IToolCommandlineParser;
import de.marw.cmake.cdt.lsp.IToolDetectionParticipant;
import de.marw.cmake.cdt.lsp.ResponseFileArglets;
import de.marw.cmake.cdt.lsp.builtins.IBuiltinsDetectionBehavior;

/**
 * Utility classes and methods to detect a parser for a compiler given on a
 * command-line string.
 *
 * @author Martin Weber
 *
 */
public class ParserDetection {
  private static final ILog log = CMakePlugin.getDefault().getLog();
  private static final boolean DEBUG_PARTCIPANT_DETECTION = Boolean
      .parseBoolean(Platform.getDebugOption(CMakePlugin.PLUGIN_ID + "/CECC/participant"));

  /**
   * tool detectors and their tool option parsers for each tool of interest that
   * takes part in the current build. The Matcher detects whether a command line
   * is an invocation of the tool.
   */
  private static final List<IToolDetectionParticipant> parserDetectors = new ArrayList<>(22);

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
      parserDetectors.add(new DefaultToolDetectionParticipant("cc", true, "exe", cc));
    }
    // POSIX compatible C++ compilers ===============================
    {
      final IToolCommandlineParser cxx = new DefaultToolCommandlineParser("org.eclipse.cdt.core.g++",
          new ResponseFileArglets.At(), btbGccMaybee, gcc_args);
      parserDetectors.add(new DefaultToolDetectionParticipant("c\\+\\+", true, "exe", cxx));
    }

    // GNU C compatible compilers ====
    {
      final IToolCommandlineParser gcc = new DefaultToolCommandlineParser("org.eclipse.cdt.core.gcc",
          new ResponseFileArglets.At(), btbGcc, gcc_args);
      parserDetectors.add(new DefaultToolDetectionParticipant("gcc", true, "exe", gcc));
      parserDetectors.add(new DefaultToolDetectionParticipant("clang", true, "exe", gcc));
      // cross compilers, e.g. arm-none-eabi-gcc ====
      parserDetectors.add(new DefaultToolDetectionParticipant("\\S+?-gcc", true, "exe", gcc));
    }
    // GNU C++ compatible compilers ====
    {
      final IToolCommandlineParser gxx = new DefaultToolCommandlineParser("org.eclipse.cdt.core.g++",
          new ResponseFileArglets.At(), btbGcc, gcc_args);
      parserDetectors.add(new DefaultToolDetectionParticipant("g\\+\\+", true, "exe", gxx));
      parserDetectors.add(new DefaultToolDetectionParticipant("clang\\+\\+", true, "exe", gxx));
      // cross compilers, e.g. arm-none-eabi-g++ ====
      parserDetectors.add(new DefaultToolDetectionParticipant("\\S+?-g\\+\\+", true, "exe", gxx));
    }
    {
      // cross compilers, e.g. arm-none-eabi-c++ ====
      final IToolCommandlineParser cxx = new DefaultToolCommandlineParser("org.eclipse.cdt.core.g++",
          new ResponseFileArglets.At(), btbGccMaybee, gcc_args);
      parserDetectors.add(new DefaultToolDetectionParticipant("\\S+?-c\\+\\+", true, "exe", cxx));
    }

    // ms C + C++ compiler ==========================================
    {
      final IToolCommandlineParser cl = new MsclToolCommandlineParser();
      parserDetectors.add(new DefaultToolDetectionParticipant("cl", true, "exe", cl));
    }

    // CUDA: nvcc compilers (POSIX compatible) =================================
    {
      parserDetectors.add(new NvccToolDetectionParticipant());
    }

    // compilers from extension points
    IConfigurationElement[] elements = Platform.getExtensionRegistry()
        .getConfigurationElementsFor("de.marw.cmake.cdt.lspDetectionParticipant");
    for (IConfigurationElement e : elements) {
      try {
        Object obj = e.createExecutableExtension("class");
        if (obj instanceof IToolDetectionParticipant) {
          parserDetectors.add((IToolDetectionParticipant) obj);
        }
      } catch (CoreException ex) {
        log.log(new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID, e.getNamespaceIdentifier(), ex));
      }
    }
  }

  /** Just static methods */
  private ParserDetection() {
  }

  /**
   * Gets the custom language IDs of each of the IToolDetectionParticipants.
   *
   * @see IToolCommandlineParser#getCustomLanguageIds()
   */
  public static List<String> getCustomLanguages() {
    return parserDetectors.stream().map(d -> d.getParser().getCustomLanguageIds()).filter(Objects::nonNull).
        flatMap(l -> l.stream()) .collect(Collectors.toList());
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
   * @param tryWindowsDetectors
   *          whether to also try the detectors for ms windows OS
   *
   * @return {@code null} if none of the detectors matches the tool name in the
   *         specified command-line string. Otherwise, if the tool name matches,
   *         a {@code ParserDetectionResult} holding the remaining command-line
   *         string (without the portion that matched) is returned.
   */
  public static ParserDetectionResult determineDetector(String line, String versionSuffixRegex,
      boolean tryWindowsDetectors) {
    ParserDetectionResult result;
    if (DEBUG_PARTCIPANT_DETECTION) {
      System.out.printf("> Command-line '%s'%n", line);
      System.out.printf("> Looking up detector for command '%s ...'%n", line.substring(0, Math.min(40, line.length())));
    }
    // try default detectors
    result = determineDetector0(line, versionSuffixRegex, false);
    if (result == null && tryWindowsDetectors) {
      // try with backslash as file separator on windows
      result = determineDetector0(line, versionSuffixRegex, true);
      if (result == null) {
        // try workaround for windows short file names
        final String shortPathExpanded = expandShortFileName(line);
        result = determineDetector0(shortPathExpanded, versionSuffixRegex, false);
        if (result == null) {
          // try with backslash as file separator on windows
          result = determineDetector0(shortPathExpanded, versionSuffixRegex, true);
        }
      }
    }
    if (result != null) {
      if (DEBUG_PARTCIPANT_DETECTION)
        System.out.printf("< Found detector for command '%s': %s (%s)%n", result.getCommandLine().getCommand(),
            result.getDetectorWithMethod().getToolDetectionParticipant().getParser().getClass().getSimpleName(),
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
  private static ParserDetectionResult determineDetector0(String commandLine, String versionSuffixRegex,
      boolean matchBackslash) {
    DefaultToolDetectionParticipant.MatchResult cmdline;
    // try basenames
    for (IToolDetectionParticipant pd : parserDetectors) {
      if (DEBUG_PARTCIPANT_DETECTION)
        System.out.printf("  Trying detector %s (%s)%n", pd.getParser().getClass().getSimpleName(),
            DetectorWithMethod.DetectionMethod.BASENAME);
      if ((cmdline = pd.basenameMatches(commandLine, matchBackslash)) != null) {
        return new ParserDetectionResult(new DetectorWithMethod(pd, DetectorWithMethod.DetectionMethod.BASENAME, matchBackslash),
            cmdline);
      }
    }
    if (versionSuffixRegex != null) {
      // try with version pattern
      for (IToolDetectionParticipant pd : parserDetectors) {
        if (DEBUG_PARTCIPANT_DETECTION)
          System.out.printf("  Trying detector %s (%s)%n", pd.getParser().getClass().getSimpleName(), DetectorWithMethod.DetectionMethod.WITH_VERSION);
        if ((cmdline = pd.basenameWithVersionMatches(commandLine, matchBackslash, versionSuffixRegex)) != null) {
          return new ParserDetectionResult(new DetectorWithMethod(pd, DetectorWithMethod.DetectionMethod.WITH_VERSION, matchBackslash),
              cmdline);
        }
      }
    }
    // try with extension
    for (IToolDetectionParticipant pd : parserDetectors) {
      if (DEBUG_PARTCIPANT_DETECTION)
        System.out.printf("  Trying detector %s (%s)%n", pd.getParser().getClass().getSimpleName(),
            DetectorWithMethod.DetectionMethod.WITH_EXTENSION);
      if ((cmdline = pd.basenameWithExtensionMatches(commandLine, matchBackslash)) != null) {
        return new ParserDetectionResult(new DetectorWithMethod(pd, DetectorWithMethod.DetectionMethod.WITH_EXTENSION, matchBackslash),
            cmdline);
      }
    }
    if (versionSuffixRegex != null) {
      // try with extension and version
      for (IToolDetectionParticipant pd : parserDetectors) {
        if (DEBUG_PARTCIPANT_DETECTION)
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

  // has public scope for unittest purposes
  public static class DetectorWithMethod {
    public enum DetectionMethod {
      BASENAME, WITH_VERSION, WITH_EXTENSION, WITH_VERSION_EXTENSION;
    }

    /**
     * the DefaultToolDetectionParticipant that matched the name of the tool on a given
     * command-line
     */
    private final IToolDetectionParticipant detector;
    /** describes the method that was used to match */
    private final DetectionMethod how;
    private final boolean matchBackslash;

    /**
     * @param detector
     *          the DefaultToolDetectionParticipant that matched the name of the tool on a given command-line
     * @param how
     *          describes the method that was used to match
     * @param matchBackslash
     *          whether the match is on file system paths with backslashes in the compiler argument or to match an paths
     *          with forward slashes
     */
    public DetectorWithMethod(IToolDetectionParticipant detector, DetectionMethod how, boolean matchBackslash) {
      if (detector == null)
        throw new NullPointerException("detector");
      if (how == null)
        throw new NullPointerException("how");
      this.detector = detector;
      this.how = how;
      this.matchBackslash= matchBackslash;
    }

    /**
     * Gets the DefaultToolDetectionParticipant that matched the name of the tool on a given
     * command-line.
     *
     * @return the detector, never {@code null}
     */
    public IToolDetectionParticipant getToolDetectionParticipant() {
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

  // has public scope for unittest purposes
  public static class ParserDetectionResult {

    private final DetectorWithMethod detectorWMethod;
    private final DefaultToolDetectionParticipant.MatchResult commandLine;

    /**
     * @param detectorWMethod
     *          the DefaultToolDetectionParticipant that matched the name of the tool on a given
     *          command-line
     * @param commandLine
     *          the de-composed command-line, after the matcher has matched the
     *          tool name
     */
    public ParserDetectionResult(DetectorWithMethod detectorWMethod, DefaultToolDetectionParticipant.MatchResult commandLine) {
      this.detectorWMethod = detectorWMethod;
      this.commandLine = commandLine;
    }

    /** Gets the de-composed command-line.
     */
    public DefaultToolDetectionParticipant.MatchResult getCommandLine() {
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
     * Gets the DefaultToolDetectionParticipant that matched the name of the tool on a given
     * command-line
     *
     * @return the detectorWMethod
     */
    public DetectorWithMethod getDetectorWithMethod() {
      return detectorWMethod;
    }
  }

}
