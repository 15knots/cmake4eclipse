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
package de.marw.cmake.cdt.lsp.nvidia.builtins;

import static org.junit.Assert.assertEquals;

import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.Before;
import org.junit.Test;

import de.marw.cmake.cdt.lsp.DefaultToolCommandlineParser;
import de.marw.cmake.cdt.lsp.IToolCommandlineParser.IResult;
import de.marw.cmake.cdt.lsp.nvidia.NvccLangStdArglet;

/**
 * @author Martin Weber
 */
public class NvccBuiltins_DetectLangStdTest {

  private NvccLangStdArglet testee;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    testee = new NvccLangStdArglet();
  }

  /**
   * Test method for {@link NvccLangStdArglet#processArgument}.
   */
  @Test
  public final void testProcessArgument_std() {
    DefaultToolCommandlineParser tcp = new DefaultToolCommandlineParser("egal", null,
        null, testee);

    final String more = " -g -MMD -MT CMakeFiles/execut1.dir/util1.c.o -MF \"CMakeFiles/execut1.dir/util1.c.o.d\""
        + " -o CMakeFiles/execut1.dir/util1.c.o -c /testprojects/C-subsrc/src/src-sub/main1.c";

    final IPath cwd = new Path("");
    // --std=
    String arg = "--std c++14";
    String parsed;

    IResult result = tcp.processArgs( cwd, arg + more);
    assertEquals("#entries", 1, result.getBuiltinDetectionArgs().size());
    parsed = result.getBuiltinDetectionArgs().get(0);
    assertEquals("name", arg, parsed);
    // --std=c11
    arg = "--std c11";
    result = tcp.processArgs( cwd, arg + more);
    assertEquals("#entries", 1, result.getBuiltinDetectionArgs().size());
    parsed = result.getBuiltinDetectionArgs().get(0);
    assertEquals("name", arg, parsed);
    // --std=c1x
    arg = "--std c1x";
    result = tcp.processArgs( cwd, arg + more);
    assertEquals("#entries", 1, result.getBuiltinDetectionArgs().size());
    parsed = result.getBuiltinDetectionArgs().get(0);
    assertEquals("name", arg, parsed);
    // --std=iso9899:1999
    arg = "--std iso9899:1999";
    result = tcp.processArgs( cwd, arg + more);
    assertEquals("#entries", 1, result.getBuiltinDetectionArgs().size());
    parsed = result.getBuiltinDetectionArgs().get(0);
    assertEquals("name", arg, parsed);
  }

  /**
   * Test method for {@link NvccLangStdArglet#processArgument}.
   */
  @Test
  public final void testProcessArgument_std2() {
    final String more = " -g -MMD -MT CMakeFiles/execut1.dir/util1.c.o -MF \"CMakeFiles/execut1.dir/util1.c.o.d\""
        + " -o CMakeFiles/execut1.dir/util1.c.o -c /testprojects/C-subsrc/src/src-sub/main1.c";
    DefaultToolCommandlineParser tcp = new DefaultToolCommandlineParser("egal", null,
        null, testee);

    String parsed;

    final IPath cwd = new Path("");
    // -std=
    String arg = "-std c++14";

    IResult result = tcp.processArgs( cwd, arg + more);
    assertEquals("#entries", 1, result.getBuiltinDetectionArgs().size());
    parsed = result.getBuiltinDetectionArgs().get(0);
    assertEquals("name", arg, parsed);
    // -std=c11
    arg = "-std c11";
    result = tcp.processArgs( cwd, arg + more);
    assertEquals("#entries", 1, result.getBuiltinDetectionArgs().size());
    parsed = result.getBuiltinDetectionArgs().get(0);
    assertEquals("name", arg, parsed);
    // -std=c1x
    arg = "-std c1x";
    result = tcp.processArgs( cwd, arg + more);
    assertEquals("#entries", 1, result.getBuiltinDetectionArgs().size());
    parsed = result.getBuiltinDetectionArgs().get(0);
    assertEquals("name", arg, parsed);
    // -std=iso9899:1999
    arg = "-std iso9899:1999";
    result = tcp.processArgs( cwd, arg + more);
    assertEquals("#entries", 1, result.getBuiltinDetectionArgs().size());
    parsed = result.getBuiltinDetectionArgs().get(0);
    assertEquals("name", arg, parsed);
  }

}
