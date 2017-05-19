package de.marw.cmake.cdt.language.settings.providers;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.marw.cmake.cdt.language.settings.providers.CompileCommandsJsonParser.ParserLookupResult;

public class CompileCommandsJsonParserSFNTest {

  private CompileCommandsJsonParserSFN testee;

  @Before
  public void setUp() {
    testee = new CompileCommandsJsonParserSFN();
  }

  /**
   * Test method for
   * {@link de.marw.cmake.cdt.language.settings.providers.CompileCommandsJsonParser#determineParserForCommandline(java.lang.String)}
   * .
   */
  @Test
  @Ignore("Requires NFTS to run")
  public void testDetermineParserForCommandline_MsdosSortNames() {
    ParserLookupResult result = testee.determineParserForCommandline("C:\\PROGRA2\\Atmel\\AVR8-G1\\bin\\AVR-G_~1.EXE -C blah.c");
    assertNotNull(result);
    // verify that we got a C parser
    assertEquals("C", "org.eclipse.cdt.core.gcc", result.parser.getLanguageId());
  }

}
