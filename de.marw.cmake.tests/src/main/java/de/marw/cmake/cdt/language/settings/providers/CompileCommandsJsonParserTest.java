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

import de.marw.cmake.cdt.language.settings.providers.CompileCommandsJsonParser.ParserLookupResult;

/**
 * @author Martin Weber
 */
public class CompileCommandsJsonParserTest {

  /**
   * Test method for
   * {@link de.marw.cmake.cdt.language.settings.providers.CompileCommandsJsonParser#determineParserForCommandline(java.lang.String)}
   * .
   */
  @Test
  public void testDetermineParserForCommandline_clang() {
    ParserLookupResult result = CompileCommandsJsonParser.determineParserForCommandline("/usr/bin/clang -C blah.c");
    assertNotNull(result);
    // verify that we got a C parser
    assertEquals("C", "org.eclipse.cdt.core.gcc", result.parser.getLanguageId());
  }

  @Test
  public void testDetermineParserForCommandline_clangplusplus() {
    ParserLookupResult result = CompileCommandsJsonParser.determineParserForCommandline("/usr/bin/clang++ -C blah.c");
    assertNotNull(result);
    // verify that we got a C++ parser
    assertEquals("C++", "org.eclipse.cdt.core.g++", result.parser.getLanguageId());
  }

  @Test
  public void testDetermineParserForCommandline_clangplusplus_basename() {
    ParserLookupResult result = CompileCommandsJsonParser.determineParserForCommandline("clang++ -C blah.c");
    assertNotNull(result);
    // verify that we got a C++ parser
    assertEquals("C++", "org.eclipse.cdt.core.g++", result.parser.getLanguageId());
  }

  /**
   * Test method for
   * {@link de.marw.cmake.cdt.language.settings.providers.CompileCommandsJsonParser#determineParserForCommandline(java.lang.String)}
   */
  @Test
  @Ignore("Requires NFTS to run")
  public void testDetermineParserForCommandline_MsdosShortNames() {
    ParserLookupResult result = CompileCommandsJsonParser.determineParserForCommandline("C:\\PROGRA2\\Atmel\\AVR8-G1\\bin\\AVR-G_~1.EXE -C blah.c");
    assertNotNull(result);
    // verify that we got a C parser
    assertEquals("C", "org.eclipse.cdt.core.gcc", result.parser.getLanguageId());
  }
}
