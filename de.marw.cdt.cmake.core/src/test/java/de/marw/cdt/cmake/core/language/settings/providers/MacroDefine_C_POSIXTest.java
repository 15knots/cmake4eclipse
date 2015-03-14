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
package de.marw.cdt.cmake.core.language.settings.providers;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.junit.Before;
import org.junit.Test;

import de.marw.cdt.cmake.core.language.settings.providers.ToolArgumentParsers.MacroDefine_C_POSIX;

/**
 * @author Martin Weber
 */
public class MacroDefine_C_POSIXTest {

  private MacroDefine_C_POSIX testee;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    testee = new MacroDefine_C_POSIX();
  }

  /**
   * Test method for
   * {@link ToolArgumentParsers.MacroDefine_C_POSIX#processArgument}.
   */
  @Test
  public final void testProcessArgument() {
    final String more = " -g -MMD -MT CMakeFiles/execut1.dir/util1.c.o -MF \"CMakeFiles/execut1.dir/util1.c.o.d\""
        + " -o CMakeFiles/execut1.dir/util1.c.o -c /testprojects/C-subsrc/src/src-sub/main1.c";
    List<ICLanguageSettingEntry> entries = new ArrayList<ICLanguageSettingEntry>();
    ICLanguageSettingEntry parsed;

    // -DFOO
    String name = "FOO";
    entries.clear();
    assertEquals(5, testee.processArgument(entries, "-D" + name + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", "", parsed.getValue());
    // -D'FOO'
    entries.clear();
    assertEquals(5 + 2,
        testee.processArgument(entries, "-D" + "'" + name + "'" + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", "", parsed.getValue());
    // -D"FOO"
    entries.clear();
    assertEquals(5 + 2,
        testee.processArgument(entries, "-D" + "\"" + name + "\"" + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", "", parsed.getValue());
    // -D   FOO
    entries.clear();
    assertEquals(5 + 3, testee.processArgument(entries, "-D   " + name + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", "", parsed.getValue());
    // -D   'FOO'
    entries.clear();
    assertEquals(5 + 2 + 3,
        testee.processArgument(entries, "-D   " + "'" + name + "'" + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", "", parsed.getValue());
    // -D   "FOO"
    entries.clear();
    assertEquals(5 + 2 + 3,
        testee.processArgument(entries, "-D   " + "\"" + name + "\"" + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", "", parsed.getValue());

//  "-DBAR _"
//  "-DFO_O_B=HUHU"
//  "-DFOO$SYS=HUHU"
    fail("Not yet implemented");
  }

  /**
   * Test method for
   * {@link ToolArgumentParsers.MacroDefine_C_POSIX#processArgument}.
   */
  @Test
  public final void testProcessArgument_Values() {
    final String more = " -g -MMD -MT CMakeFiles/execut1.dir/util1.c.o -MF \"CMakeFiles/execut1.dir/util1.c.o.d\""
        + " -o CMakeFiles/execut1.dir/util1.c.o -c /testprojects/C-subsrc/src/src-sub/main1.c";
    List<ICLanguageSettingEntry> entries = new ArrayList<ICLanguageSettingEntry>();
    ICLanguageSettingEntry parsed;

    // -DFOO=noWhiteSpace
    final String name = "FOO";
    String val = "noWhiteSpace";

    entries.clear();
    assertEquals(5 + 1 + val.length(),
        testee.processArgument(entries, "-D" + name + "=" + val + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    // -D'FOO=noWhiteSpace'
    entries.clear();
    assertEquals(
        5 + 1 + 2 + val.length(),
        testee.processArgument(entries, "-D" + "'" + name + "=" + val + "'"
            + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    // -D"FOO=noWhiteSpace"
    entries.clear();
    assertEquals(
        5 + 1 + 2 + val.length(),
        testee.processArgument(entries, "-D" + "\"" + name + "=" + val + "\""
            + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    // -D   FOO=noWhiteSpace
    entries.clear();
    assertEquals(5 + 1 + 3 + val.length(),
        testee.processArgument(entries, "-D   " + name + "=" + val + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    // -D   'FOO=noWhiteSpace'
    entries.clear();
    assertEquals(
        5 + 1 + 2 + 3 + val.length(),
        testee.processArgument(entries, "-D   " + "'" + name + "=" + val + "'"
            + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    // -D   "FOO=noWhiteSpace"
    entries.clear();
    assertEquals(
        5 + 1 + 2 + 3 + val.length(),
        testee.processArgument(entries, "-D   " + "\"" + name + "=" + val
            + "\"" + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());

//    ""-D \"FOO\""
//    "D \"FOO=white sp A ce  \""
//    "-D \"FOO(a,b,c)=white sp A ce\""
  }
}
