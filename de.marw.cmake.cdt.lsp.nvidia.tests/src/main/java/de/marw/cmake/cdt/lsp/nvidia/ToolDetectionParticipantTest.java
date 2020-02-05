/*******************************************************************************
 * Copyright (c) 2019 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake.cdt.lsp.nvidia;

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
  public void testDetermineToolDetectionParticipant() {
    IToolDetectionParticipant result = ParticipantTestUtil.determineToolDetectionParticipant("/usr/bin/nvcc -C blah.c",
        null, true);
    assertNotNull(result);
    assertEquals(NvccToolDetectionParticipant.class, result.getClass());

    result = ParticipantTestUtil.determineToolDetectionParticipant("nvcc -C blah.c", null, true);
    assertNotNull(result);
    assertEquals(NvccToolDetectionParticipant.class, result.getClass());

    result = ParticipantTestUtil.determineToolDetectionParticipant("/usr/bin/nvcc.exe -C blah.c", null, true);
    assertNotNull(result);
    assertEquals(NvccToolDetectionParticipant.class, result.getClass());

    result = ParticipantTestUtil.determineToolDetectionParticipant("nvcc.exe -C blah.c", null, true);
    assertNotNull(result);
    assertEquals(NvccToolDetectionParticipant.class, result.getClass());
  }

  @Test
  public void testDetermineToolDetectionParticipant_quote() {
    String[] quotes = { "\"", "'" };
    for (String quote : quotes) {
      String args = String.format(Locale.ROOT, "%1$s/usr/bin/nvcc%1$s -I /foo/nvcc -C blah.c", quote);
      IToolDetectionParticipant result = ParticipantTestUtil.determineToolDetectionParticipant(args, null, true);
      assertNotNull("Command in quotes= " + quote, result);
      assertEquals(NvccToolDetectionParticipant.class, result.getClass());
    }
  }
}