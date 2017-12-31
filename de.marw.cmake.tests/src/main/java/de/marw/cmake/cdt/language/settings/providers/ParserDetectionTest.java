/*******************************************************************************
 * Copyright (c) 2016-2017 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cmake.cdt.language.settings.providers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Martin Weber
 */
public class ParserDetectionTest {

  /**
   * Test method for
   * {@link de.marw.cmake.cdt.language.settings.providers.ParserDetection#determineDetector(String, String,boolean)}
   * .
   */
  @Test
  public void testDetermineParserForCommandline_clang() {
    ParserDetection.ParserDetectionResult result = ParserDetection.determineDetector("/usr/bin/clang -C blah.c", null,
        true);
    assertNotNull(result);
    // verify that we got a C parser
    assertEquals("C", "org.eclipse.cdt.core.gcc", result.getDetectorWithMethod().getDetector().getParser().getLanguageId());
  }

  @Test
  public void testDetermineParserForCommandline_clangplusplus() {
    ParserDetection.ParserDetectionResult result = ParserDetection.determineDetector("/usr/bin/clang++ -C blah.c", null,
        true);
    assertNotNull(result);
    // verify that we got a C++ parser
    assertEquals("C++", "org.eclipse.cdt.core.g++", result.getDetectorWithMethod().getDetector().getParser().getLanguageId());
  }

  @Test
  public void testDetermineParserForCommandline_clangplusplus_basename() {
    ParserDetection.ParserDetectionResult result = ParserDetection.determineDetector("clang++ -C blah.c", null, false);
    assertNotNull(result);
    // verify that we got a C++ parser
    assertEquals("C++", "org.eclipse.cdt.core.g++", result.getDetectorWithMethod().getDetector().getParser().getLanguageId());
  }

  @Test
  @Ignore("Requires NFTS to run")
  public void testDetermineParserForCommandline_MsdosShortNames() {
    ParserDetection.ParserDetectionResult result = ParserDetection
        .determineDetector("C:\\PROGRA2\\Atmel\\AVR8-G1\\bin\\AVR-G_~1.EXE -C blah.c", null, true);
    assertNotNull(result);
    // verify that we got a C parser
    assertEquals("C", "org.eclipse.cdt.core.gcc", result.getDetectorWithMethod().getDetector().getParser().getLanguageId());
  }

  @Test
  public void testDetermineParserForCommandline_withVersion() {
    final String versionSuffixRegex = "-?\\d+(\\.\\d+)*";

    ParserDetection.ParserDetectionResult result = ParserDetection.determineDetector("/usr/bin/cc-4.1 -C blah.c",
        versionSuffixRegex, false);
    assertNotNull(result);
    // verify that we got a C parser
    assertEquals("org.eclipse.cdt.core.gcc", result.getDetectorWithMethod().getDetector().getParser().getLanguageId());

    result = ParserDetection.determineDetector("/usr/bin/cc-4.1.exe -C blah.c", versionSuffixRegex, true);
    assertNotNull(result);
    // verify that we got a C parser
    assertEquals("org.eclipse.cdt.core.gcc", result.getDetectorWithMethod().getDetector().getParser().getLanguageId());

    result = ParserDetection.determineDetector("/usr/bin/c++-4.1 -C blah.c",
        versionSuffixRegex, false);
    assertNotNull(result);
    // verify that we got a C++ parser
    assertEquals("org.eclipse.cdt.core.g++", result.getDetectorWithMethod().getDetector().getParser().getLanguageId());

    result = ParserDetection.determineDetector("/usr/bin/c++-4.1.exe -C blah.c", versionSuffixRegex, true);
    assertNotNull(result);
    // verify that we got a C++ parser
    assertEquals("org.eclipse.cdt.core.g++", result.getDetectorWithMethod().getDetector().getParser().getLanguageId());

    // clang for issue #43
    result = ParserDetection.determineDetector("/usr/local/bin/clang++40 -C blah.c", versionSuffixRegex, false);
    assertNotNull(result);
    assertEquals("org.eclipse.cdt.core.g++", result.getDetectorWithMethod().getDetector().getParser().getLanguageId());
    result = ParserDetection.determineDetector("/usr/local/bin/clang++40 -C blah.c", "40", false);
//    result = ParserDetection.determineDetector("/usr/local/bin/clang++40 -I/home/me/workspace/first/test/../utility -I/home/me/workspace/first/test/../include -I/home/me/workspace/first/test -g -std=c++1y -stdlib=libc++ -include-pch /home/me/workspace/first/build/Debug/test/catch.hpp.pch -include-pch /home/me/workspace/first/build/Debug/test/pch.hpp.pch -o CMakeFiles/first_test.test.dir/__/utility/fun.cpp.o -c /home/me/workspace/first/utility/fun.cpp",
//        "40", false);
    assertNotNull(result);
    assertEquals("org.eclipse.cdt.core.g++", result.getDetectorWithMethod().getDetector().getParser().getLanguageId());

    result = ParserDetection.determineDetector(
        "/apps/tools/cent_os72/binlinks/g++-7.1 " + "-I/apps/tools/cent_os72/thirdparty/boost/boost_1_64_0/include "
            + "-I/home/XXX/repositories/bepa/common/include -g -Wall "
            + "-c /home/XXX/repositories/bepa/common/settings/src/settings.cpp",
        versionSuffixRegex, true);
    assertNotNull(result);
    // verify that we got a C++ parser
    assertEquals("org.eclipse.cdt.core.g++", result.getDetectorWithMethod().getDetector().getParser().getLanguageId());
  }

}
