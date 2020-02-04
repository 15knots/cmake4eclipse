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
package de.marw.cmake.cdt.lsp.hpenonstop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Locale;

import org.junit.Test;

import de.marw.cmake.cdt.lsp.IToolDetectionParticipant;
import de.marw.cmake.cdt.lsp.ParticipantTestUtil;

/**
 * @author Martin Weber
 */
public class ToolDetectionParticipantTest {
  @Test
  public void testDetermineToolDetectionParticipant_c89() {
    IToolDetectionParticipant result = ParticipantTestUtil.determineToolDetectionParticipant("/usr/bin/c89 -C blah.c",
        null, true);
    assertNotNull(result);
    assertEquals(HpeC89ToolDetectionParticipant.class, result.getClass());

    result = ParticipantTestUtil.determineToolDetectionParticipant("c89 -C blah.c", null, true);
    assertNotNull(result);
    assertEquals(HpeC89ToolDetectionParticipant.class, result.getClass());

    result = ParticipantTestUtil.determineToolDetectionParticipant("/usr/bin/c89.exe -C blah.c", null, true);
    assertNotNull(result);
    assertEquals(HpeC89ToolDetectionParticipant.class, result.getClass());

    result = ParticipantTestUtil.determineToolDetectionParticipant("c89.exe -C blah.c", null, true);
    assertNotNull(result);
    assertEquals(HpeC89ToolDetectionParticipant.class, result.getClass());
  }

  @Test
  public void testDetermineToolDetectionParticipant_c89_quote() {
    String[] quotes = { "\"", "'" };
    for (String quote : quotes) {
      String args = String.format(Locale.ROOT, "%1$s/usr/bin/c89%1$s -I /foo/c89 -C blah.c", quote);
      IToolDetectionParticipant result = ParticipantTestUtil.determineToolDetectionParticipant(args, null, true);
      assertNotNull("Command in quotes= " + quote, result);
      assertEquals(HpeC89ToolDetectionParticipant.class, result.getClass());
    }
  }

  @Test
  public void testDetermineToolDetectionParticipant_c99() {
    IToolDetectionParticipant result = ParticipantTestUtil.determineToolDetectionParticipant("/usr/bin/c99 -C blah.c",
        null, true);
    assertNotNull(result);
    assertEquals(HpeC99ToolDetectionParticipant.class, result.getClass());

    result = ParticipantTestUtil.determineToolDetectionParticipant("c99 -C blah.c", null, true);
    assertNotNull(result);
    assertEquals(HpeC99ToolDetectionParticipant.class, result.getClass());

    result = ParticipantTestUtil.determineToolDetectionParticipant("/usr/bin/c99.exe -C blah.c", null, true);
    assertNotNull(result);
    assertEquals(HpeC99ToolDetectionParticipant.class, result.getClass());

    result = ParticipantTestUtil.determineToolDetectionParticipant("c99.exe -C blah.c", null, true);
    assertNotNull(result);
    assertEquals(HpeC99ToolDetectionParticipant.class, result.getClass());
  }

  @Test
  public void testDetermineToolDetectionParticipant_c99_quote() {
    String[] quotes = { "\"", "'" };
    for (String quote : quotes) {
      String args = String.format(Locale.ROOT, "%1$s/usr/bin/c99%1$s -I /foo/c99 -C blah.c", quote);
      IToolDetectionParticipant result = ParticipantTestUtil.determineToolDetectionParticipant(args, null, true);
      assertNotNull("Command in quotes= " + quote, result);
      assertEquals(HpeC99ToolDetectionParticipant.class, result.getClass());
    }
  }

  @Test
  public void testDetermineToolDetectionParticipant_c11() {
    IToolDetectionParticipant result = ParticipantTestUtil.determineToolDetectionParticipant("/usr/bin/c11 -C blah.c",
        null, true);
    assertNotNull(result);
    assertEquals(HpeC11ToolDetectionParticipant.class, result.getClass());

    result = ParticipantTestUtil.determineToolDetectionParticipant("c11 -C blah.c", null, true);
    assertNotNull(result);
    assertEquals(HpeC11ToolDetectionParticipant.class, result.getClass());

    result = ParticipantTestUtil.determineToolDetectionParticipant("/usr/bin/c11.exe -C blah.c", null, true);
    assertNotNull(result);
    assertEquals(HpeC11ToolDetectionParticipant.class, result.getClass());

    result = ParticipantTestUtil.determineToolDetectionParticipant("c11.exe -C blah.c", null, true);
    assertNotNull(result);
    assertEquals(HpeC11ToolDetectionParticipant.class, result.getClass());
  }

  @Test
  public void testDetermineToolDetectionParticipant_c11_quote() {
    String[] quotes = { "\"", "'" };
    for (String quote : quotes) {
      String args = String.format(Locale.ROOT, "%1$s/usr/bin/c11%1$s -I /foo/c11 -C blah.c", quote);
      IToolDetectionParticipant result = ParticipantTestUtil.determineToolDetectionParticipant(args, null, true);
      assertNotNull("Command in quotes= " + quote, result);
      assertEquals(HpeC11ToolDetectionParticipant.class, result.getClass());
    }
  }
}
