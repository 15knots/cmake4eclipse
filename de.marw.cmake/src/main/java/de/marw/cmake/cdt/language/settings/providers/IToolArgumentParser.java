/*******************************************************************************
 * Copyright (c) 2015 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cmake.cdt.language.settings.providers;

import java.util.List;

import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;

/**
 * Converts tool arguments into LanguageSettings objects.
 *
 * @author Martin Weber
 */
public interface IToolArgumentParser {

  /**
   * Parses the first tool invocation argument from the build output that this
   * object can parse and extracts all possible LanguageSettings objects.
   *
   * @param returnedEntries
   *        the buffer that receives the new {@code LanguageSettings}
   * @param argsLine
   *        the arguments passed to the tool, as they appear in the build
   *        output. Implementers may safely assume that the specified value does
   *        not contain leading whitespace characters, but trailing WS may
   *        occur.
   * @return the number of characters from {@code argsLine} that has been
   *         processed. Return a value of {@code zero} or less, if this tool
   *         argument parser cannot process the first argument from the input.
   */
  int processArgument(List<ICLanguageSettingEntry> returnedEntries,
      String argsLine);
}