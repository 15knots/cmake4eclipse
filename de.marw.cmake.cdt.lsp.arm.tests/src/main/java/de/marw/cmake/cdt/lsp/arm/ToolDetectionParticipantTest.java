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

import org.junit.Test;

import de.marw.cmake.cdt.lsp.IToolDetectionParticipant;
import de.marw.cmake.cdt.lsp.ParticipantTestUtil;

/**
 * @author Martin Weber
 */
public class ToolDetectionParticipantTest {
  @Test
  public void testDetermineToolDetectionParticipant_armcc() {
    IToolDetectionParticipant result = ParticipantTestUtil.determineToolDetectionParticipant("/usr/bin/armcc -C blah.c", null,
        true);
    assertNotNull(result);
    assertEquals(result.getClass(), ArmccToolDetectionParticipant.class);

    result = ParticipantTestUtil.determineToolDetectionParticipant("/usr/bin/armcc.exe -C blah.c", null,
        true);
    assertNotNull(result);
    assertEquals(result.getClass(), ArmccToolDetectionParticipant.class);
  }

  @Test
  public void testDetermineToolDetectionParticipant_armclang() {
    IToolDetectionParticipant result = ParticipantTestUtil.determineToolDetectionParticipant("/usr/bin/armclang -C blah.c", null,
        true);
    assertNotNull(result);
    assertEquals(result.getClass(), ArmClangToolDetectionParticipant.class);

    result = ParticipantTestUtil.determineToolDetectionParticipant("/usr/bin/armclang.exe -C blah.c", null,
        true);
    assertNotNull(result);
    assertEquals(result.getClass(), ArmClangToolDetectionParticipant.class);
  }
}
