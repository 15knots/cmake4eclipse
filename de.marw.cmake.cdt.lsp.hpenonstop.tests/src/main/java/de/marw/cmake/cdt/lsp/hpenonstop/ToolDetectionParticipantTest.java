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

import org.junit.Test;

import de.marw.cmake.cdt.lsp.IToolDetectionParticipant;
import de.marw.cmake.cdt.lsp.ParticipantTestUtil;

/**
 * @author Martin Weber
 */
public class ToolDetectionParticipantTest {
  @Test
  public void testDetermineToolDetectionParticipant_c89() {
    IToolDetectionParticipant result = ParticipantTestUtil.determineToolDetectionParticipant("/usr/bin/c89 -C blah.c", null,
        true);
    assertNotNull(result);
    assertEquals(result.getClass(), HpeC89ToolDetectionParticipant.class);

    result = ParticipantTestUtil.determineToolDetectionParticipant("/usr/bin/c89.exe -C blah.c", null,
        true);
    assertNotNull(result);
    assertEquals(result.getClass(), HpeC89ToolDetectionParticipant.class);
  }

  @Test
  public void testDetermineToolDetectionParticipant_c99() {
    IToolDetectionParticipant result = ParticipantTestUtil.determineToolDetectionParticipant("/usr/bin/c99 -C blah.c", null,
        true);
    assertNotNull(result);
    assertEquals(result.getClass(), HpeC99ToolDetectionParticipant.class);

    result = ParticipantTestUtil.determineToolDetectionParticipant("/usr/bin/c99.exe -C blah.c", null,
        true);
    assertNotNull(result);
    assertEquals(result.getClass(), HpeC99ToolDetectionParticipant.class);
  }

  @Test
  public void testDetermineToolDetectionParticipant_c11() {
    IToolDetectionParticipant result = ParticipantTestUtil.determineToolDetectionParticipant("/usr/bin/c11 -C blah.c", null,
        true);
    assertNotNull(result);
    assertEquals(result.getClass(), HpeC11ToolDetectionParticipant.class);

    result = ParticipantTestUtil.determineToolDetectionParticipant("/usr/bin/c11.exe -C blah.c", null,
        true);
    assertNotNull(result);
    assertEquals(result.getClass(), HpeC11ToolDetectionParticipant.class);
  }
}
