/**
 *
 */
package de.marw.cmake.cdt.language.settings.providers;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.marw.cmake.cdt.language.settings.providers.CompileCommandsJsonParser.ParserLookupResult;

/**
 * @author weber
 *
 */
public class ParserLookupResultTest {

  private CompileCommandsJsonParser parser;

  @Before
  public void setUp() throws Exception {
    parser = new CompileCommandsJsonParser();
  }

  /**
   * @throws java.lang.Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  /**
   * Test method for
   * {@link de.marw.cmake.cdt.language.settings.providers.CompileCommandsJsonParser.ParserLookupResult#canParse(java.lang.String)}.
   */
  @Test
  public void testCanParse() {
    String compiler = "/bin/c++";
    String args = "-DQT_CORE_LIB -I/home/self/shared/qt5-project/build/Debug"
        + " -isystem /home/self/Qt5.9.1/5.9.1/gcc_64/include/QtWidgets"
        + " -g -fPIC -std=gnu++11 -o CMakeFiles/foo.dir/foo_automoc.cpp.o"
        + " -c /home/self/shared/qt5-project/build/Debug/foo_automoc.cpp";
    String cmd = compiler + " " + args;
    ParserLookupResult result = parser.determineParserForCommandline(cmd);
    assertNotNull(result);
    assertTrue(result.canParse(cmd));
  }

  /**
   * Test method for
   * {@link de.marw.cmake.cdt.language.settings.providers.CompileCommandsJsonParser.ParserLookupResult#getReducedCommandLine()}.
   */
  @Test
  public void testGetReducedCommandLine() {
    String compiler = "/bin/c++";
    String args = "-DQT_CORE_LIB -I/home/self/shared/qt5-project/build/Debug"
        + " -isystem /home/self/Qt5.9.1/5.9.1/gcc_64/include/QtWidgets"
        + " -g -fPIC -std=gnu++11 -o CMakeFiles/foo.dir/foo_automoc.cpp.o"
        + " -c /home/self/shared/qt5-project/build/Debug/foo_automoc.cpp";
    String cmd = compiler + " " + args;
    ParserLookupResult result = parser.determineParserForCommandline(cmd);
    assertNotNull(result);
    // verify that we got a C++ parser
    assertEquals("C++", "org.eclipse.cdt.core.g++", result.parser.getLanguageId());
    assertEquals("reducedCommandLine", args, result.getReducedCommandLine());

    // test without leading path
    compiler = "c++";
    cmd = compiler + " " + args;
    result = parser.determineParserForCommandline(cmd);
    assertNotNull(result);
    // verify that we got a C++ parser
    assertEquals("C++", "org.eclipse.cdt.core.g++", result.parser.getLanguageId());
    assertEquals("reducedCommandLine", args, result.getReducedCommandLine());
  }

}
