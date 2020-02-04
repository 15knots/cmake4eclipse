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
package de.marw.cmake.cdt.lsp.arm;

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
  public void testDetermineToolDetectionParticipant_armcc() {
    IToolDetectionParticipant result = ParticipantTestUtil
        .determineToolDetectionParticipant("/usr/bin/armcc -I /foo/cc -C blah.c", null, true);
    assertNotNull(result);
    assertEquals(ArmccToolDetectionParticipant.class, result.getClass());

    result = ParticipantTestUtil.determineToolDetectionParticipant("armcc -I /foo/cc -C blah.c", null, true);
    assertNotNull(result);
    assertEquals(ArmccToolDetectionParticipant.class, result.getClass());

    result = ParticipantTestUtil.determineToolDetectionParticipant("/usr/bin/armcc.exe  -I /foo/cc -C blah.c", null,
        true);
    assertNotNull(result);
    assertEquals(ArmccToolDetectionParticipant.class, result.getClass());

    result = ParticipantTestUtil.determineToolDetectionParticipant("armcc.exe  -I /foo/cc -C blah.c", null, true);
    assertNotNull(result);
    assertEquals(ArmccToolDetectionParticipant.class, result.getClass());
  }

  @Test
  public void testDetermineToolDetectionParticipant_armcc_quote() {
    String[] quotes = { "\"", "'" };
    for (String quote : quotes) {
      String args = String.format(Locale.ROOT, "%1$s/usr/bin/armcc%1$s -I /foo/cc -C blah.c", quote);
      IToolDetectionParticipant result = ParticipantTestUtil.determineToolDetectionParticipant(args, null, true);
      assertNotNull("Command in quotes= " + quote, result);
      assertEquals(ArmccToolDetectionParticipant.class, result.getClass());
    }
  }

  @Test
  public void testDetermineToolDetectionParticipant_armclang() {
    IToolDetectionParticipant result = ParticipantTestUtil
        .determineToolDetectionParticipant("/usr/bin/armclang  -I /foo/clang -C blah.c", null, true);
    assertNotNull(result);
    assertEquals(ArmClangToolDetectionParticipant.class, result.getClass());

    result = ParticipantTestUtil.determineToolDetectionParticipant("armclang  -I /foo/clang -C blah.c", null, true);
    assertNotNull(result);
    assertEquals(ArmClangToolDetectionParticipant.class, result.getClass());

    result = ParticipantTestUtil.determineToolDetectionParticipant("/usr/bin/armclang.exe -I /foo/clang -C blah.c",
        null, true);
    assertNotNull(result);
    assertEquals(ArmClangToolDetectionParticipant.class, result.getClass());

    result = ParticipantTestUtil.determineToolDetectionParticipant("armclang.exe -I /foo/clang -C blah.c", null, true);
    assertNotNull(result);
    assertEquals(ArmClangToolDetectionParticipant.class, result.getClass());
  }

  @Test
  public void testDetermineToolDetectionParticipant_armclang_quote() {
    String[] quotes = { "\"", "'" };
    for (String quote : quotes) {
      String args = String.format(Locale.ROOT, "%1$s/usr/bin/armclang%1$s -I /foo/clang -C blah.c", quote);
      IToolDetectionParticipant result = ParticipantTestUtil.determineToolDetectionParticipant(args, null, true);
      assertNotNull("Command in quotes= " + quote, result);
      assertEquals(ArmClangToolDetectionParticipant.class, result.getClass());
    }
  }
}