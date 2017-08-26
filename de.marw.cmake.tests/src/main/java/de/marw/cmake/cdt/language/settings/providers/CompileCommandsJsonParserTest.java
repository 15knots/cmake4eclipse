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

import de.marw.cmake.cdt.language.settings.providers.CompileCommandsJsonParser.ParserDetectionResult;

/**
 * @author Martin Weber
 */
public class CompileCommandsJsonParserTest {

  CompileCommandsJsonParser testee= new CompileCommandsJsonParser();

  /**
   * Test method for
   * {@link de.marw.cmake.cdt.language.settings.providers.CompileCommandsJsonParser#determineDetector(String, boolean)}
   * .
   */
  @Test
  public void testDetermineParserForCommandline_clang() {
    ParserDetectionResult result = testee.determineDetector("/usr/bin/clang -C blah.c", true);
    assertNotNull(result);
    // verify that we got a C parser
    assertEquals("C", "org.eclipse.cdt.core.gcc", result.detectorWMethod.detector.parser.getLanguageId());
  }

  @Test
  public void testDetermineParserForCommandline_clangplusplus() {
    ParserDetectionResult result = testee.determineDetector("/usr/bin/clang++ -C blah.c", true);
    assertNotNull(result);
    // verify that we got a C++ parser
    assertEquals("C++", "org.eclipse.cdt.core.g++", result.detectorWMethod.detector.parser.getLanguageId());
  }

  @Test
  public void testDetermineParserForCommandline_clangplusplus_basename() {
    ParserDetectionResult result = testee.determineDetector("clang++ -C blah.c", false);
    assertNotNull(result);
    // verify that we got a C++ parser
    assertEquals("C++", "org.eclipse.cdt.core.g++", result.detectorWMethod.detector.parser.getLanguageId());
  }

  @Test
  @Ignore("Requires NFTS to run")
  public void testDetermineParserForCommandline_MsdosShortNames() {
    ParserDetectionResult result = testee.determineDetector("C:\\PROGRA2\\Atmel\\AVR8-G1\\bin\\AVR-G_~1.EXE -C blah.c", true);
    assertNotNull(result);
    // verify that we got a C parser
    assertEquals("C", "org.eclipse.cdt.core.gcc", result.detectorWMethod.detector.parser.getLanguageId());
  }

  @Test
  public void testDetermineParserForCommandline_withVersion() {
    testee.setVersionPatternEnabled(true);
    testee.setVersionPattern("-?\\d+(\\.\\d+)*");
    ParserDetectionResult result = testee.determineDetector("/usr/bin/cc-4.1 -C blah.c", false);
    assertNotNull(result);
    // verify that we got a C++ parser
    assertEquals("C", "org.eclipse.cdt.core.gcc", result.detectorWMethod.detector.parser.getLanguageId());

    result = testee.determineDetector("/usr/bin/cc-4.1.exe -C blah.c", true);
    assertNotNull(result);
    // verify that we got a C++ parser
    assertEquals("C", "org.eclipse.cdt.core.gcc", result.detectorWMethod.detector.parser.getLanguageId());
}

}
