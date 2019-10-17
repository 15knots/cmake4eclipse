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

package de.marw.cmake.cdt.internal.lsp.builtins;

import java.util.List;
import java.util.Objects;

import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;

/**
 * Responsible for parsing the compiler output that is produced to detect compiler-built-in preprocessor macros and
 * include paths.
 *
 */
abstract class BuiltinsOutputProcessor {

  private final List<ICLanguageSettingEntry> entries;

  /**
   * @param entries
   *          where to place the {@code ICLanguageSettingEntry}s found during processing.
   */
  public BuiltinsOutputProcessor(List<ICLanguageSettingEntry> entries) {
    this.entries = Objects.requireNonNull(entries);
  }

  /**
   * Parsers the given line from the compiler output and places each ICLanguageSettingEntry found in the given result
   * list.
   */
  protected abstract void processLine(String line);

  /**
   * Adds a ICLanguageSettingEntry to the result list. Implemented as a convenience method which reports whether the new
   * entry was {@code null}.
   *
   * @param entry
   *          the entry to add to the result list or {@code null}.
   *
   * @return {@code false} if {@code entry} was {@code null}, otherwise {@code true}
   */
  protected boolean addEntry(ICLanguageSettingEntry entry) {
    if (entry != null) {
      entries.add(entry);
      return true;
    }
    return false;
  }

}
