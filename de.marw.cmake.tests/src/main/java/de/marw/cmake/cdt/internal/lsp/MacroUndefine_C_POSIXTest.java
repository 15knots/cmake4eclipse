/*******************************************************************************
 * Copyright (c) 2015 Martin Weber.
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

import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.Before;
import org.junit.Test;

import de.marw.cmake.cdt.internal.lsp.ParseContext;
import de.marw.cmake.cdt.language.settings.providers.IToolArgumentParser.IParseContext;
import de.marw.cmake.cdt.language.settings.providers.ToolArgumentParsers.MacroUndefine_C_POSIX;

/**
 * @author Martin Weber
 */
public class MacroUndefine_C_POSIXTest {

  private MacroUndefine_C_POSIX testee;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    testee = new MacroUndefine_C_POSIX();
  }

  /**
   * Test method for
   * {@link de.marw.cmake.cdt.internal.lsp.ToolArgumentParsers.MacroUndefine_C_POSIX#processArgument(IParseContext, IPath, java.lang.String)}
   * .
   */
  @Test
  public final void testProcessArgument() {
    final String more = " -g -MMD -MT CMakeFiles/execut1.dir/util1.c.o -MF \"CMakeFiles/execut1.dir/util1.c.o.d\""
        + " -o CMakeFiles/execut1.dir/util1.c.o -c /testprojects/C-subsrc/src/src-sub/main1.c";
    ParseContext entries;
    ICLanguageSettingEntry parsed;
    final IPath cwd= new Path("");

    String name = "FOO";
    // -UFOO
    entries = new ParseContext();
    assertEquals(2 + name.length(),
        testee.processArgument(entries, cwd, "-U" + name + more));
    assertEquals("#entries", 1, entries.getSettingEntries().size());
    parsed = entries.getSettingEntries().get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("kind", ICSettingEntry.UNDEFINED, (parsed.getFlags()&ICSettingEntry.UNDEFINED));
    assertEquals("name", name, parsed.getName());
    assertEquals("value", "", parsed.getValue());
    // -U  FOO
    entries = new ParseContext();
    assertEquals(2 +2+ name.length(),
        testee.processArgument(entries, cwd, "-U  " + name + more));
    assertEquals("#entries", 1, entries.getSettingEntries().size());
    parsed = entries.getSettingEntries().get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("kind", ICSettingEntry.UNDEFINED, (parsed.getFlags()&ICSettingEntry.UNDEFINED));
    assertEquals("name", name, parsed.getName());
    assertEquals("value", "", parsed.getValue());
  }
}
