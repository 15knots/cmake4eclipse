/*******************************************************************************
 * Copyright (c) 2019 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/

package de.marw.cmake.cdt.lsp;

import de.marw.cmake.cdt.internal.lsp.ParserDetection;

/**
 * Utility methods for unit testing.
 *
 * @author Martin Weber
 */
public class ParticipantTestUtil {

  private ParticipantTestUtil() {
  }

  /**
   * Determines the tool detection participant that can parse the specified command-line.
   *
   * @param line
   *          the command line to process
   * @param versionSuffixRegex
   *          the regular expression to match a version suffix in the compiler name or {@code null} to not try to detect
   *          the compiler with a version suffix
   * @param tryWindowsDetectors
   *          whether to also try the detectors for ms windows OS
   *
   * @return the detected {@link IToolDetectionParticipant} or {@code null} if none of the tool detection participants
   *         matched the tool name in the specified command-line string.
   */
  public static IToolDetectionParticipant determineToolDetectionParticipant(String line, String versionSuffixRegex,
      boolean tryWindowsDetectors) {
    ParserDetection.ParserDetectionResult result = ParserDetection.determineDetector(line, versionSuffixRegex,
        tryWindowsDetectors);
    return result == null ? null : result.getDetectorWithMethod().getToolDetectionParticipant();
  }
}
