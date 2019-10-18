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
package de.marw.cmake.cdt.internal.lsp;

import static org.junit.Assert.assertEquals;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.Before;
import org.junit.Test;

import de.marw.cmake.cdt.internal.lsp.ParseContext;
import de.marw.cmake.cdt.language.settings.providers.Arglets;
import de.marw.cmake.cdt.language.settings.providers.Arglets.Sysroot_GCC;

/**
 * @author Martin Weber
 */
public class BuiltinsDetectSysroot_GCC_Test {

  private Sysroot_GCC testee;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    testee = new Sysroot_GCC();
  }

  /**
   * Test method for {@link Arglets.Sysroot_GCC#processArgument}.
   */
  @Test
  public final void testProcessArgument_sysroot() {
    final String more = " -g -MMD -MT CMakeFiles/execut1.dir/util1.c.o -MF \"CMakeFiles/execut1.dir/util1.c.o.d\""
        + " -o CMakeFiles/execut1.dir/util1.c.o -c /testprojects/C-subsrc/src/src-sub/main1.c";
    ParseContext context;
    String parsed;

    final IPath cwd = new Path("");
    // --sysroot=/a/Path
    String arg = "--sysroot=/XAX/YYY";

    context = new ParseContext();
    testee.processArgument(context, cwd, arg + more);
    assertEquals("#entries", 1, context.getBuiltinDetctionArgs().size());
    parsed = context.getBuiltinDetctionArgs().get(0);
    assertEquals("name", arg, parsed);
    // --sysroot="/a/Path"
    context = new ParseContext();
    arg = "--sysroot=\"/XXX/YYY\"";
    testee.processArgument(context, cwd, arg + " " + arg + more);
    assertEquals("#entries", 1, context.getBuiltinDetctionArgs().size());
    parsed = context.getBuiltinDetctionArgs().get(0);
    assertEquals("name", arg, parsed);
  }

  /**
   * Test method for {@link Arglets.Sysroot_GCC#processArgument}.
   */
  @Test
  public final void testProcessArgument_isysroot() {
    final String more = " -g -MMD -MT CMakeFiles/execut1.dir/util1.c.o -MF \"CMakeFiles/execut1.dir/util1.c.o.d\""
        + " -o CMakeFiles/execut1.dir/util1.c.o -c /testprojects/C-subsrc/src/src-sub/main1.c";
    ParseContext context;
    String parsed;

    final IPath cwd = new Path("");
    // -isysroot=/a/Path
    String arg = "-isysroot=/XAX/YYY";

    context = new ParseContext();
    testee.processArgument(context, cwd, arg + more);
    assertEquals("#entries", 1, context.getBuiltinDetctionArgs().size());
    parsed = context.getBuiltinDetctionArgs().get(0);
    assertEquals("name", arg, parsed);
    // -isysroot="/a/Path"
    context = new ParseContext();
    arg = "-isysroot=\"/XXX/YYY\"";
    testee.processArgument(context, cwd, arg + more);
    assertEquals("#entries", 1, context.getBuiltinDetctionArgs().size());
    parsed = context.getBuiltinDetctionArgs().get(0);
    assertEquals("name", arg, parsed);
  }

  /**
   * Test method for {@link Arglets.Sysroot_GCC#processArgument}.
   */
  @Test
  public final void testProcessArgument_no_sysroot_prefix() {
    final String more = " -g -MMD -MT CMakeFiles/execut1.dir/util1.c.o -MF \"CMakeFiles/execut1.dir/util1.c.o.d\""
        + " -o CMakeFiles/execut1.dir/util1.c.o -c /testprojects/C-subsrc/src/src-sub/main1.c";
    ParseContext context;
    String parsed;

    final IPath cwd = new Path("");
    // --no-sysroot-prefix
    String arg = "--no-sysroot-prefix";

    context = new ParseContext();
    testee.processArgument(context, cwd, arg + more);
    assertEquals("#entries", 1, context.getBuiltinDetctionArgs().size());
    parsed = context.getBuiltinDetctionArgs().get(0);
    assertEquals("name", arg, parsed);
  }
}
