/*******************************************************************************
 * Copyright (c) 2016-2017 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake.cdt.internal.lsp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import de.marw.cmake.cdt.language.settings.providers.CompileCommandsJsonParser;

/**
 * @author weber
 *
 */
public class ParserLookupResultTest {

  CompileCommandsJsonParser testee = new CompileCommandsJsonParser();

  /**
   * Test method for
   * {@link de.marw.cmake.cdt.internal.lsp.ParserDetection#determineDetector(String, String,boolean)}
   */
  @Test
  public void testCanParse() {
    String compiler = "/bin/c++";
    String args = "-DQT_CORE_LIB -I/home/self/shared/qt5-project/build/Debug"
        + " -isystem /home/self/Qt5.9.1/5.9.1/gcc_64/include/QtWidgets"
        + " -g -fPIC -std=gnu++11 -o CMakeFiles/foo.dir/foo_automoc.cpp.o"
        + " -c /home/self/shared/qt5-project/build/Debug/foo_automoc.cpp";
    String cmd = compiler + " " + args;
    ParserDetection.ParserDetectionResult result = ParserDetection.determineDetector(cmd, null, false);
    assertNotNull(result);
  }

  /**
   * Test method for
   * {@link de.marw.cmake.cdt.internal.lsp.ParserDetection.ParserDetectionResult#getReducedCommandLine()}.
   */
  @Test
  public void testGetReducedCommandLine() {
    String compiler = "/bin/c++";
    String args = "-DQT_CORE_LIB -I/home/self/shared/qt5-project/build/Debug"
        + " -isystem /home/self/Qt5.9.1/5.9.1/gcc_64/include/QtWidgets"
        + " -g -fPIC -std=gnu++11 -o CMakeFiles/foo.dir/foo_automoc.cpp.o"
        + " -c /home/self/shared/qt5-project/build/Debug/foo_automoc.cpp";
    String cmd = compiler + " " + args;
    ParserDetection.ParserDetectionResult result = ParserDetection.determineDetector(cmd, null, false);
    assertNotNull(result);
    // verify that we got a C++ parser
    assertEquals("C++", "org.eclipse.cdt.core.g++", result.getDetectorWithMethod().getToolDetectionParticipant().getParser().getLanguageId("cpp"));
    assertEquals("reducedCommandLine", args, result.getReducedCommandLine());

    // test without leading path
    compiler = "c++";
    cmd = compiler + " " + args;
    result = ParserDetection.determineDetector(cmd, null, false);
    assertNotNull(result);
    // verify that we got a C++ parser
    assertEquals("C++", "org.eclipse.cdt.core.g++", result.getDetectorWithMethod().getToolDetectionParticipant().getParser().getLanguageId("cpp"));
    assertEquals("reducedCommandLine", args, result.getReducedCommandLine());
  }

  /**
   * Test method for
   * {@link de.marw.cmake.cdt.internal.lsp.ParserDetection.ParserDetectionResult#getCommandLine()}.
   */
  @Test
  public void testGetCommandLine() {
    String compiler = "/bin/c++";
    String args = "-DQT_CORE_LIB -I/home/self/shared/qt5-project/build/Debug"
        + " -isystem /home/self/Qt5.9.1/5.9.1/gcc_64/include/QtWidgets"
        + " -g -fPIC -std=gnu++11 -o CMakeFiles/foo.dir/foo_automoc.cpp.o"
        + " -c /home/self/shared/qt5-project/build/Debug/foo_automoc.cpp";
    String cmd = compiler + " " + args;
    ParserDetection.ParserDetectionResult result = ParserDetection.determineDetector(cmd, null, false);
    assertNotNull(result);
    // verify that we got a C++ parser
    assertEquals("C++", "org.eclipse.cdt.core.g++", result.getDetectorWithMethod().getToolDetectionParticipant().getParser().getLanguageId("cpp"));
    assertEquals("command", compiler, result.getCommandLine().getCommand());
    assertEquals("args", args, result.getCommandLine().getArguments());

    // test without leading path
    compiler = "c++";
    cmd = compiler + " " + args;
    result = ParserDetection.determineDetector(cmd, null, false);
    assertNotNull(result);
    // verify that we got a C++ parser
    assertEquals("C++", "org.eclipse.cdt.core.g++", result.getDetectorWithMethod().getToolDetectionParticipant().getParser().getLanguageId("cpp"));
    assertEquals("command", compiler, result.getCommandLine().getCommand());
    assertEquals("args", args, result.getCommandLine().getArguments());
  }
}
