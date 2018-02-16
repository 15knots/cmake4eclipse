/*******************************************************************************
 * Copyright (c) 2018 Martin Weber.
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.Before;
import org.junit.Test;

import de.marw.cmake.cdt.language.settings.providers.ToolArgumentParsers.SystemIncludePath_nvcc;

/**
 * @author Martin Weber
 */
public class SystemIncludePath_nvcc_Test {

  private SystemIncludePath_nvcc testee;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    testee = new SystemIncludePath_nvcc();
  }

  @Test
  public final void testProcessArgument() {
    final String more = " -g -MMD -MT CMakeFiles/execut1.dir/util1.c.o -MF \"CMakeFiles/execut1.dir/util1.c.o.d\""
        + " -o CMakeFiles/execut1.dir/util1.c.o -c /testprojects/C-subsrc/src/src-sub/main1.cu";
    List<ICLanguageSettingEntry> entries = new ArrayList<>();
    ICLanguageSettingEntry parsed;
    final IPath cwd= new Path("");

    String name = "/an/Include/Path";

    // -isystem=/an/Include/Path
    entries.clear();
    assertEquals(8 + name.length() + 1,
        testee.processArgument(entries, cwd, "-isystem" + "=" + name + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_PATH, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    // -isystem='/an/Include/Path'
    entries.clear();
    assertEquals(8 + name.length() + 1 + 2,
        testee.processArgument(entries, cwd, "-isystem" + "=" + "'" + name + "'" + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_PATH, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    // -isystem="/an/Include/Path"
    entries.clear();
    assertEquals(8 + name.length() + 1 + 2,
        testee.processArgument(entries, cwd, "-isystem" + "=" + "\"" + name + "\"" + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_PATH, parsed.getKind());
    assertEquals("name", name, parsed.getName());

    name = (new Path("A:an\\In CLU  de/Pat h")).toOSString();
    // -isystem="A:an\In CLU  de/Pat h"
    entries.clear();
    assertEquals(8 + name.length() + 1 + 2,
        testee.processArgument(entries, cwd, "-isystem" + "=" + "\"" + name + "\"" + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_PATH, parsed.getKind());
    assertEquals("name", name, parsed.getName());

    // -isystem='A:an\In CLU  de/Pat h'
    entries.clear();
    assertEquals(8 + name.length() + 1 + 2,
        testee.processArgument(entries, cwd, "-isystem" + "=" + "'" + name + "'" + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_PATH, parsed.getKind());
    assertEquals("name", name, parsed.getName());
  }

}
