/*******************************************************************************
 * Copyright (c) 2020 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake.cdt.internal.lsp;

import static org.junit.Assert.assertEquals;

import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.Before;
import org.junit.Test;

import de.marw.cmake.cdt.lsp.Arglets.IncludeFile_GCC;
import de.marw.cmake.cdt.lsp.IArglet.IParseContext;

/**
 * @author Martin Weber
 */
public class IncludeFile_GCCTest {
  private IncludeFile_GCC testee;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    testee = new IncludeFile_GCC();
  }

  /**
   * Test method for {@link IncludeFile_GCC#processArgument(IParseContext, IPath, java.lang.String)} .
   */
  @Test
  public final void testProcessArgument() {
    final String more = " -o CMakeFiles/execut1.dir/util1.c.o -c /testprojects/C-subsrc/src/src-sub/main1.c";
    ParseContext entries;
    ICLanguageSettingEntry parsed;

    String name = "/an/Include/file.inc";
    IPath cwd = new Path("");
    // -include/an/Include/file.inc
    entries = new ParseContext();
    assertEquals(8 + name.length(), testee.processArgument(entries, cwd, "-include" + name + more));
    assertEquals("#entries", 1, entries.getSettingEntries().size());
    parsed = entries.getSettingEntries().get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_FILE, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    // -include'/an/Include/file.inc'
    entries = new ParseContext();
    assertEquals(8 + name.length() + 2, testee.processArgument(entries, cwd, "-include" + "'" + name + "'" + more));
    assertEquals("#entries", 1, entries.getSettingEntries().size());
    parsed = entries.getSettingEntries().get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_FILE, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    // -include"/an/Include/file.inc"
    entries = new ParseContext();
    assertEquals(8 + name.length() + 2, testee.processArgument(entries, cwd, "-include" + "\"" + name + "\"" + more));
    assertEquals("#entries", 1, entries.getSettingEntries().size());
    parsed = entries.getSettingEntries().get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_FILE, parsed.getKind());
    assertEquals("name", name, parsed.getName());

    // -include /an/Include/file.inc
    entries = new ParseContext();
    assertEquals(8 + name.length() + 3, testee.processArgument(entries, cwd, "-include   " + name + more));
    assertEquals("#entries", 1, entries.getSettingEntries().size());
    parsed = entries.getSettingEntries().get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_FILE, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    // -include '/an/Include/file.inc'
    entries = new ParseContext();
    assertEquals(8 + name.length() + 3 + 2,
        testee.processArgument(entries, cwd, "-include   " + "'" + name + "'" + more));
    assertEquals("#entries", 1, entries.getSettingEntries().size());
    parsed = entries.getSettingEntries().get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_FILE, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    // -include "/an/Include/file.inc"
    entries = new ParseContext();
    assertEquals(8 + name.length() + 3 + 2,
        testee.processArgument(entries, cwd, "-include   " + "\"" + name + "\"" + more));
    assertEquals("#entries", 1, entries.getSettingEntries().size());
    parsed = entries.getSettingEntries().get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_FILE, parsed.getKind());
    assertEquals("name", name, parsed.getName());

    name = (new Path("A:an\\Include/file.inc")).toOSString();
    // -includeA:an\Include/file.inc
    entries = new ParseContext();
    assertEquals(8 + name.length(), testee.processArgument(entries, cwd, "-include" + name + more));
    assertEquals("#entries", 1, entries.getSettingEntries().size());
    parsed = entries.getSettingEntries().get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_FILE, parsed.getKind());
    assertEquals("name", name, parsed.getName());
  }

  /**
   * Test method for {@link IncludeFile_GCC#processArgument(IParseContext, IPath, java.lang.String)}
   */
  @Test
  public final void testProcessArgument_WS() {
    final String more = " -o CMakeFiles/execut1.dir/util1.c.o -c /testprojects/C-subsrc/src/src-sub/main1.c";
    ParseContext entries = new ParseContext();
    ICLanguageSettingEntry parsed;

    String name = "/ye olde/In clu de/fi le.inc";
    IPath cwd = new Path("");
    // -include'/ye olde/In clu de/fi le.inc'
    entries = new ParseContext();
    assertEquals(8 + name.length() + 2, testee.processArgument(entries, cwd, "-include" + "'" + name + "'" + more));
    assertEquals("#entries", 1, entries.getSettingEntries().size());
    parsed = entries.getSettingEntries().get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_FILE, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    // -include"/ye olde/In clu de/fi le.inc"
    entries = new ParseContext();
    assertEquals(8 + name.length() + 2, testee.processArgument(entries, cwd, "-include" + "\"" + name + "\"" + more));
    assertEquals("#entries", 1, entries.getSettingEntries().size());
    parsed = entries.getSettingEntries().get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_FILE, parsed.getKind());
    assertEquals("name", name, parsed.getName());

    // -include '/ye olde/In clu de/fi le.inc'
    entries = new ParseContext();
    assertEquals(8 + name.length() + 3 + 2,
        testee.processArgument(entries, cwd, "-include   " + "'" + name + "'" + more));
    assertEquals("#entries", 1, entries.getSettingEntries().size());
    parsed = entries.getSettingEntries().get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_FILE, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    // -include "/ye olde/In clu de/fi le.inc"
    entries = new ParseContext();
    assertEquals(8 + name.length() + 3 + 2,
        testee.processArgument(entries, cwd, "-include   " + "\"" + name + "\"" + more));
    assertEquals("#entries", 1, entries.getSettingEntries().size());
    parsed = entries.getSettingEntries().get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_FILE, parsed.getKind());
    assertEquals("name", name, parsed.getName());

    name = (new Path("A:an\\In CLU  de/fi le.inc")).toOSString();
    // -include'A:an\In CLU de/fi le.inc'
    entries = new ParseContext();
    assertEquals(8 + name.length() + 2, testee.processArgument(entries, cwd, "-include" + "\"" + name + "\"" + more));
    assertEquals("#entries", 1, entries.getSettingEntries().size());
    parsed = entries.getSettingEntries().get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_FILE, parsed.getKind());
    assertEquals("name", name, parsed.getName());
  }

  /**
   * Test method for {@link IncludeFile_GCC#processArgument(IParseContext, IPath, java.lang.String)} .
   */
  @Test
  public final void testProcessArgument_RelativePath() {
    final String more = " -o CMakeFiles/execut1.dir/util1.c.o -c /testprojects/C-subsrc/src/src-sub/main1.c";
    ICLanguageSettingEntry parsed;

    String name = (new Path("a/relative/Include/file.inc")).toOSString();
    IPath cwd = new Path("/compiler/working/dir");
    ParseContext entries = new ParseContext();
    assertEquals(8 + name.length(), testee.processArgument(entries, cwd, "-include" + name + more));
    assertEquals("#entries", 1, entries.getSettingEntries().size());
    parsed = entries.getSettingEntries().get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_FILE, parsed.getKind());
    String absPath = cwd.append(name).toString();
    assertEquals("name", absPath, parsed.getName());

    name = (new Path("a\\relative\\Include\\file.inc")).toOSString();
    cwd = new Path("\\compiler\\working\\dir");
    entries = new ParseContext();
    assertEquals(8 + name.length(), testee.processArgument(entries, cwd, "-include" + name + more));
    assertEquals("#entries", 1, entries.getSettingEntries().size());
    parsed = entries.getSettingEntries().get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_FILE, parsed.getKind());
    absPath = cwd.append(name).toString();
    assertEquals("name", absPath, parsed.getName());

    name = (new Path("../../src/Include/file.inc")).toOSString();
    cwd = new Path("/compiler/working/dir");
    entries = new ParseContext();
    assertEquals(8 + name.length(), testee.processArgument(entries, cwd, "-include" + name + more));
    assertEquals("#entries", 1, entries.getSettingEntries().size());
    parsed = entries.getSettingEntries().get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_FILE, parsed.getKind());
    absPath = cwd.append(name).toString();
    assertEquals("name", absPath, parsed.getName());

    name = (new Path("..\\..\\src\\Include\\file.inc")).toOSString();
    cwd = new Path("\\compiler\\working\\dir");
    entries = new ParseContext();
    assertEquals(8 + name.length(), testee.processArgument(entries, cwd, "-include" + name + more));
    assertEquals("#entries", 1, entries.getSettingEntries().size());
    parsed = entries.getSettingEntries().get(0);
    assertEquals("kind", ICSettingEntry.INCLUDE_FILE, parsed.getKind());
    absPath = cwd.append(name).toString();
    assertEquals("name", absPath, parsed.getName());
  }
}
