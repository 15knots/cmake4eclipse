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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;

/**
 * The purpose of this class is to parse a line from the compiler output when detecting built-in values and to create a
 * language settings entry out of it.
 *
 * @author Martin Weber
 */
class CompilerOutputLineProcessor {

  private final Matcher matcher;
  private final int nameGroup;
  private final int valueGroup;
  private final int kind;
  private final int extraFlag;

  /**
   * Constructor.
   *
   * @param pattern
   *          regular expression pattern being parsed by the parser.
   * @param nameGroup
   *          capturing group number defining the {@link ICSettingEntry#getName() name} of an entry.
   * @param valueGroup
   *          capturing group number defining the {@link ICSettingEntry#getValue() value} of an entry. Specify
   *          {@code -1} if no value is captured.
   * @param isIncludePath
   *          kind of language settings entries to create. Specify <code>true</code> to create a
   *          {@link ICSettingEntry#INCLUDE_PATH} entry, <code>false</code> to create a
   *          {@link ICLanguageSettingEntry#MACRO} entry
   * @param extraFlag
   *          extra-flags to add to the created language settings entry, e.g. {@link ICSettingEntry#LOCAL} or
   *          {@link ICSettingEntry#FRAMEWORKS_MAC}.
   */
  public CompilerOutputLineProcessor(String pattern, int nameGroup, int valueGroup, boolean isIncludePath,
      int extraFlag) {
    this.matcher = Pattern.compile(pattern).matcher("");
    this.nameGroup = nameGroup;
    this.valueGroup = valueGroup;
    this.kind = isIncludePath ? ICSettingEntry.INCLUDE_PATH : ICSettingEntry.MACRO;
    this.extraFlag = extraFlag;
  }

  /**
   * Processes specified compiler output line.
   *
   * @param line
   *          the compiler output line to process
   * @return the language settings entry constructed from the given output line or <code>null</code> if the line did not
   *         match any settings entry
   */
  protected ICLanguageSettingEntry process(String line) {
    matcher.reset(line);
    if (matcher.matches()) {
      final String name = matcher.group(nameGroup);
      final String value = valueGroup == -1 ? null : matcher.group(valueGroup);
      return (ICLanguageSettingEntry) CDataUtil.createEntry(kind, name, value, null,
          ICSettingEntry.BUILTIN | ICSettingEntry.READONLY | extraFlag);
    }
    return null; // no match
  }
}
