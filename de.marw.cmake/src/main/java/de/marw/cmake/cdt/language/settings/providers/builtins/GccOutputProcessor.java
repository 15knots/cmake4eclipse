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

import java.util.List;

import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;

/**
 * A {link BuiltinsOutputProcessor} for the GCC/G++ compiler.
 *
 * @author Martin Weber
 *
 */
class GccOutputProcessor extends BuiltinsOutputProcessor {
  private static final CompilerOutputLineProcessor[] macros = {
      new CompilerOutputLineProcessor("#define\\s+(\\S+)\\s*(.*)", 1, 2, false, 0),
      new CompilerOutputLineProcessor("#define\\s+(\\S+)\\(.*?\\)\\s*(.*)", 1, 2, false, 0),
      };

  private static final CompilerOutputLineProcessor[] localIncludes = {
      new CompilerOutputLineProcessor(" *(\\S.*)", 1, -1, true, ICSettingEntry.LOCAL) };

  private static final CompilerOutputLineProcessor[] systemIncludes = {
      new CompilerOutputLineProcessor(" *(\\S.*)", 1, -1, true, 0) };

  private static final CompilerOutputLineProcessor[] framewotks = {
      new CompilerOutputLineProcessor(" *(\\S.*)", 1, -1, true, ICSettingEntry.FRAMEWORKS_MAC) };

  private State state = State.NONE;

  //  private static final String frameworkIndicator = "(framework directory)";

  public GccOutputProcessor(List<ICLanguageSettingEntry> entries) {
    super(entries);
  }

  @Override
  protected void processLine(String line) {

    // include paths
    if (line.equals("#include \"...\" search starts here:")) {
      state = State.EXPECTING_LOCAL_INCLUDE;
      return;
    } else if (line.equals("#include <...> search starts here:")) {
      state = State.EXPECTING_SYSTEM_INCLUDE;
      return;
    } else if (line.startsWith("End of search list.")) {
      state = State.NONE;
      return;
    } else if (line.equals("Framework search starts here:")) {
      // NOTE: need sample output of 'gcc -E -P -Wp,-v /tmp/foo.c' to implement this
      state = State.EXPECTING_FRAMEWORK;
      return;
    } else if (line.startsWith("End of framework search list.")) {
      state = State.NONE;
      return;
    }

    if (state == State.EXPECTING_LOCAL_INCLUDE) {
      for (CompilerOutputLineProcessor processor : localIncludes) {
        if (addEntry(processor.process(line)))
          return; // line matched
      }
    } else if (state == State.EXPECTING_SYSTEM_INCLUDE) {
      for (CompilerOutputLineProcessor processor : systemIncludes) {
        if (addEntry(processor.process(line)))
          return; // line matched
      }
    } else if (state == State.EXPECTING_FRAMEWORK) {
      for (CompilerOutputLineProcessor processor : framewotks) {
        if (addEntry(processor.process(line)))
          return; // line matched
      }
    } else {
      // macros
      for (CompilerOutputLineProcessor processor : macros) {
        if (addEntry(processor.process(line)))
          return; // line matched
      }
//      System.err.println("NO MATCH ON LINE: '" + line + "'");
    }
  }

  private enum State {
    NONE, EXPECTING_LOCAL_INCLUDE, EXPECTING_SYSTEM_INCLUDE, EXPECTING_FRAMEWORK
  }
}
