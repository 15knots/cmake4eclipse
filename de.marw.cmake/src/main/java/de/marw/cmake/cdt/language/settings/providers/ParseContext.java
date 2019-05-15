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

package de.marw.cmake.cdt.language.settings.providers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;

/**
 * Default implementation of IParseContext.
 *
 * @author Martin Weber
 */
class ParseContext implements IToolArgumentParser.IParseContext, IToolCommandlineParser.IResult {
  private final List<ICLanguageSettingEntry> entries = new ArrayList<>();
  private final List<String> args = new ArrayList<>();

  @Override
  public void addSettingEntry(ICLanguageSettingEntry entry) {
    entries.add(entry);
  }

  @Override
  public void addBuiltinDetctionArgument(String argument) {
    args.add(argument);
  }

  @Override
  public List<ICLanguageSettingEntry> getSettingEntries() {
    return entries;
  }

  @Override
  public List<String> getBuiltinDetctionArgs() {
    return args;
  }

}
