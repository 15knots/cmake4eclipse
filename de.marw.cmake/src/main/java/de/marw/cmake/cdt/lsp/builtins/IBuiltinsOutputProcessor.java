/*******************************************************************************
 * Copyright (c) 2018-2019 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/

package de.marw.cmake.cdt.language.settings.providers.builtins;

import java.util.List;

import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;

/**
 * Responsible for parsing the output that is produced when a compiler is invoked to detect its-built-in preprocessor
 * macros and include paths.
 */
public interface IBuiltinsOutputProcessor {

  /**
   * Parsers the given line from the compiler output and places each ICLanguageSettingEntry found in the given
   * {@code IProcessingContext}.
   *
   * @param line
   *          a line from the compiler output to parse
   * @param processingContext
   *          the buffer that receives the new {@code LanguageSetting} entries
   */
  void processLine(String line, IProcessingContext processingContext);

  /**
   * Gathers the results of argument parsing.
   *
   * @author Martin Weber
   */
  public interface IProcessingContext {
    /**
     * Adds a ICLanguageSettingEntry to the result list. For convenience this method reports whether the new entry was
     * {@code null}.
     *
     * @param entry
     *          the entry to add to the result list or {@code null}.
     *
     * @return {@code false} if {@code entry} was {@code null}, otherwise {@code true}
     */
    boolean addSettingEntry(ICLanguageSettingEntry entry);
  } // IProcessingContext

  /**
   * The result of processing the complete compiler output.
   *
   * @author Martin Weber
   *
   * @see IBuiltinsOutputProcessor#processLine(String, IProcessingContext)
   */
  public interface IResult {
    /**
     * Gets the language setting entries produced during processing.
     *
     * @return the language setting entries
     */
    List<ICLanguageSettingEntry> getSettingEntries();
  } // IResult
}
