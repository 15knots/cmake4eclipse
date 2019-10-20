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

package de.marw.cmake.cdt.internal.lsp.builtins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.core.runtime.Platform;

import de.marw.cmake.cdt.internal.CMakePlugin;
import de.marw.cmake.cdt.language.settings.providers.builtins.IBuiltinsOutputProcessor;

/**
 * Default implementation of IProcessingContext.
 *
 * @author Martin Weber
 */
class ProcessingContext implements IBuiltinsOutputProcessor.IProcessingContext, IBuiltinsOutputProcessor.IResult {
  private static final boolean DEBUG = Boolean
      .parseBoolean(Platform.getDebugOption(CMakePlugin.PLUGIN_ID + "/CECC/builtins/entries"));

  private final List<ICLanguageSettingEntry> entries = Collections
      .synchronizedList(new ArrayList<ICLanguageSettingEntry>());

  @Override
  public boolean addSettingEntry(ICLanguageSettingEntry entry) {
    if (entry != null) {
      if (DEBUG)
        System.out.printf("Added builtin entry: %s%n", entry);
      entries.add(entry);
      return true;
    }
    return false;
  }

  @Override
  public List<ICLanguageSettingEntry> getSettingEntries() {
    return entries;
  }

}
