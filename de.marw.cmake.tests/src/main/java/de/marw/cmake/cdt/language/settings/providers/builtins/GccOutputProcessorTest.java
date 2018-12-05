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

package de.marw.cmake.cdt.language.settings.providers.builtins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Martin Weber
 *
 */
public class GccOutputProcessorTest {

  private List<ICLanguageSettingEntry> entries;
  private GccOutputProcessor testee;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    entries = Collections.synchronizedList(new ArrayList<ICLanguageSettingEntry>());
    testee = new GccOutputProcessor(entries);
  }

  @Test
  @Ignore
  public void testProcessLine() {
    testee.processLine("#define AAA xyz");
  }

  @Test
  public void testProcessFile() throws IOException {
    // pass resource content line-wise to the testee...
    try (InputStream is = getClass().getResourceAsStream("cbd-gcc.output.txt");
        OutputSniffer os = new OutputSniffer(testee, null)) {
      byte[] buffer = new byte[1024];
      int length;
      while ((length = is.read(buffer)) > 0) {
        os.write(buffer, 0, length);
      }
    }

    // check __GNUC__
    for (ICLanguageSettingEntry entry : entries) {
      if (entry.getKind() == ICLanguageSettingEntry.MACRO) {
        if ("__GNUC__".equals(entry.getName()))
          assertEquals("value (" + entry.getName() + ")", "4", entry.getValue() );
      }
    }

    int inc = 0;
    int macro = 0;
    for (ICLanguageSettingEntry entry : entries) {
      if (entry.getKind() == ICLanguageSettingEntry.INCLUDE_PATH) {
        inc++;
        assertTrue("path", !"".equals(entry.getName()));
      } else if (entry.getKind() == ICLanguageSettingEntry.MACRO) {
        macro++;
        assertTrue("macro", !"".equals(entry.getName()));
        assertTrue("value (" + entry.getName() + ")", entry.getValue() != null);
      }
    }
    assertEquals("# include paths", 5, inc);
    assertEquals("# macros", 238, macro);
  }
}
