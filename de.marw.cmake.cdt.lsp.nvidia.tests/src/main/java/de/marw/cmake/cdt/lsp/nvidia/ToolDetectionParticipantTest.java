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

    result = ParticipantTestUtil.determineToolDetectionParticipant("/usr/bin/nvcc.exe -C blah.c", null, true);
    assertNotNull(result);
    assertEquals(NvccToolDetectionParticipant.class, result.getClass());
  }

  @Test
  public void testDetermineToolDetectionParticipant_quote() {
    String[] quotes = { "\"", "'" };
    for (String quote : quotes) {
      String args = String.format(Locale.ROOT, "%1$s/usr/bin/nvcc%1$s -I /foo/nvcc -C blah.c", quote);
      IToolDetectionParticipant result = ParticipantTestUtil.determineToolDetectionParticipant(args, null, true);
      assertNotNull("Command in quotes=" + quote, result);
      assertEquals(NvccToolDetectionParticipant.class, result.getClass());
    }
  }

}
