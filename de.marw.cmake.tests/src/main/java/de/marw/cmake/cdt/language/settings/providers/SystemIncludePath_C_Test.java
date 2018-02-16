/*******************************************************************************
 * Copyright (c) 2015-2018 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cmake.cdt.language.settings.providers;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.Before;
import org.junit.Test;

import de.marw.cmake.cdt.language.settings.providers.ToolArgumentParsers.SystemIncludePath_C;

/**
 * @author Martin Weber
 */
public class SystemIncludePath_C_Test {

  private SystemIncludePath_C testee;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    testee = new SystemIncludePath_C();
}

  @Test
  public final void testProcessArgument() {
    final String more = " -g -MMD -MT CMakeFiles/execut1.dir/util1.c.o -MF \"CMakeFiles/execut1.dir/util1.c.o.d\""
        + " -o CMakeFiles/execut1.dir/util1.c.o -c /testprojects/C-subsrc/src/src-sub/main1.c";
    List<ICLanguageSettingEntry> entries = new ArrayList<>();
    ICLanguageSettingEntry parsed;
    final IPath cwd= new Path("");

    String name = "/an/Include/Path";

    // -isystem   /an/Include/Path
    entries.clear();
    assertEquals(8 + name.length() + 3,
        testee.processArgument(entries, cwd, "-isystem   " + name + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_PATH, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    // -isystem   '/an/Include/Path'
    entries.clear();
    assertEquals(8 + name.length() + 3 + 2,
        testee.processArgument(entries, cwd, "-isystem   " + "'" + name + "'" + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_PATH, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    // -isystem   "/an/Include/Path"
    entries.clear();
    assertEquals(8 + name.length() + 3 + 2,
        testee.processArgument(entries, cwd, "-isystem   " + "\"" + name + "\"" + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_PATH, parsed.getKind());
    assertEquals("name", name, parsed.getName());

    name = (new Path("A:an\\Include/Path")).toOSString();
    // -isystem A:an\Include/Path
    entries.clear();
    assertEquals(8 + 1+ name.length(),
        testee.processArgument(entries, cwd, "-isystem " + name + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_PATH, parsed.getKind());
    assertEquals("name", name, parsed.getName());

    // -isystemA:an\Include/Path
    entries.clear();
    assertEquals(8 + name.length(),
        testee.processArgument(entries, cwd, "-isystem" + name + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_PATH, parsed.getKind());
    assertEquals("name", name, parsed.getName());
  }

  /**
   * Test method for
   * {@link de.marw.cmake.cdt.language.settings.providers.ToolArgumentParsers.IncludePath_C_POSIX#processArgument(java.util.List, IPath, java.lang.String)}
   */
  @Test
  public final void testProcessArgument_WS() {
    final String more = " -g -MMD -MT CMakeFiles/execut1.dir/util1.c.o -MF \"CMakeFiles/execut1.dir/util1.c.o.d\""
        + " -o CMakeFiles/execut1.dir/util1.c.o -c /testprojects/C-subsrc/src/src-sub/main1.c";
    List<ICLanguageSettingEntry> entries = new ArrayList<>();
    ICLanguageSettingEntry parsed;
    final IPath cwd= new Path("");

    String name = "/ye olde/In clu de/Pa the";
    // -isystem   '/ye olde/In clu de/Pa the'
    entries.clear();
    assertEquals(8 + name.length() + 3 + 2,
        testee.processArgument(entries, cwd, "-isystem   " + "'" + name + "'" + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_PATH, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    // -isystem   "/ye olde/In clu de/Pa the"
    entries.clear();
    assertEquals(8 + name.length() + 3 + 2,
        testee.processArgument(entries, cwd, "-isystem   " + "\"" + name + "\"" + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_PATH, parsed.getKind());
    assertEquals("name", name, parsed.getName());

    name = (new Path("A:an\\In CLU  de/Pat h")).toOSString();
    // -isystem"A:an\In CLU  de/Pat h"
    entries.clear();
    assertEquals(8 + name.length() + 2,
        testee.processArgument(entries, cwd, "-isystem" + "\"" + name + "\"" + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_PATH, parsed.getKind());
    assertEquals("name", name, parsed.getName());

    // -isystem'A:an\In CLU  de/Pat h'
    entries.clear();
    assertEquals(8 + name.length() + 2,
        testee.processArgument(entries, cwd, "-isystem" + "'" + name + "'" + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_PATH, parsed.getKind());
    assertEquals("name", name, parsed.getName());

    name = (new Path("/Inc/CLUde/Path")).toOSString();
    // -isystem/Inc/CLUde/Path
    entries.clear();
    assertEquals(8 + name.length(),
        testee.processArgument(entries, cwd, "-isystem" + name + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_PATH, parsed.getKind());
    assertEquals("name", name, parsed.getName());
 }

}
