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
package de.marw.cmake.cdt.language.settings.providers;

import static org.junit.Assert.assertEquals;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.Before;
import org.junit.Test;

import de.marw.cmake.cdt.language.settings.providers.ToolArgumentParsers.LangStd_nvcc;

/**
 * @author Martin Weber
 */
public class BuiltinsDetectLangStd_nvcc_Test {

  private LangStd_nvcc testee;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    testee = new LangStd_nvcc();
  }

  /**
   * Test method for {@link ToolArgumentParsers.LangStd_nvcc#processArgument}.
   */
  @Test
  public final void testProcessArgument_std() {
    final String more = " -g -MMD -MT CMakeFiles/execut1.dir/util1.c.o -MF \"CMakeFiles/execut1.dir/util1.c.o.d\""
        + " -o CMakeFiles/execut1.dir/util1.c.o -c /testprojects/C-subsrc/src/src-sub/main1.c";
    ParseContext context;
    String parsed;

    final IPath cwd = new Path("");
    // --std=
    String arg = "--std c++14";

    context = new ParseContext();
    testee.processArgument(context, cwd, arg + more);
    assertEquals("#entries", 1, context.getBuiltinDetctionArgs().size());
    parsed = context.getBuiltinDetctionArgs().get(0);
    assertEquals("name", arg, parsed);
    // --std=c11
    context = new ParseContext();
    arg = "--std c11";
    testee.processArgument(context, cwd, arg + more);
    assertEquals("#entries", 1, context.getBuiltinDetctionArgs().size());
    parsed = context.getBuiltinDetctionArgs().get(0);
    assertEquals("name", arg, parsed);
    // --std=c1x
    context = new ParseContext();
    arg = "--std c1x";
    testee.processArgument(context, cwd, arg + more);
    assertEquals("#entries", 1, context.getBuiltinDetctionArgs().size());
    parsed = context.getBuiltinDetctionArgs().get(0);
    assertEquals("name", arg, parsed);
    // --std=iso9899:1999
    context = new ParseContext();
    arg = "--std iso9899:1999";
    testee.processArgument(context, cwd, arg + more);
    assertEquals("#entries", 1, context.getBuiltinDetctionArgs().size());
    parsed = context.getBuiltinDetctionArgs().get(0);
    assertEquals("name", arg, parsed);
  }

  /**
   * Test method for {@link ToolArgumentParsers.LangStd_nvcc#processArgument}.
   */
  @Test
  public final void testProcessArgument_std2() {
    final String more = " -g -MMD -MT CMakeFiles/execut1.dir/util1.c.o -MF \"CMakeFiles/execut1.dir/util1.c.o.d\""
        + " -o CMakeFiles/execut1.dir/util1.c.o -c /testprojects/C-subsrc/src/src-sub/main1.c";
    ParseContext context;
    String parsed;

    final IPath cwd = new Path("");
    // -std=
    String arg = "-std c++14";

    context = new ParseContext();
    testee.processArgument(context, cwd, arg + more);
    assertEquals("#entries", 1, context.getBuiltinDetctionArgs().size());
    parsed = context.getBuiltinDetctionArgs().get(0);
    assertEquals("name", arg, parsed);
    // -std=c11
    context = new ParseContext();
    arg = "-std c11";
    testee.processArgument(context, cwd, arg + more);
    assertEquals("#entries", 1, context.getBuiltinDetctionArgs().size());
    parsed = context.getBuiltinDetctionArgs().get(0);
    assertEquals("name", arg, parsed);
    // -std=c1x
    context = new ParseContext();
    arg = "-std c1x";
    testee.processArgument(context, cwd, arg + more);
    assertEquals("#entries", 1, context.getBuiltinDetctionArgs().size());
    parsed = context.getBuiltinDetctionArgs().get(0);
    assertEquals("name", arg, parsed);
    // -std=iso9899:1999
    context = new ParseContext();
    arg = "-std iso9899:1999";
    testee.processArgument(context, cwd, arg + more);
    assertEquals("#entries", 1, context.getBuiltinDetctionArgs().size());
    parsed = context.getBuiltinDetctionArgs().get(0);
    assertEquals("name", arg, parsed);
  }

}
