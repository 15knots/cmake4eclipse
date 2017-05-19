/*******************************************************************************
 * Copyright (c) 2017 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cmake.cdt.language.settings.providers;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import de.marw.cmake.CMakePlugin;

/**
 * A {@code CompileCommandsJsonParser} that also tries to convert windows short
 * file names (like <code>AVR-G_~1.EXE</code>) into their long representation.
 * This is a workaround for a
 * <a href="https://gitlab.kitware.com/cmake/cmake/issues/16138">bug in CMake
 * under windows</a>.
 *
 * @author Martin Weber
 */
public class CompileCommandsJsonParserSFN extends CompileCommandsJsonParser {

  private static final ILog log = CMakePlugin.getDefault().getLog();

  @Override
  ParserLookupResult determineParserForCommandline(String commandLine) {
    ParserLookupResult parser = super.determineParserForCommandline(commandLine);
    if (parser == null) {
      String command;
      // split at first space character
      StringBuilder commandLine2 = new StringBuilder();
      int idx = commandLine.indexOf(' ');
      if (idx != -1) {
        command = commandLine.substring(0, idx);
        commandLine2.append(commandLine.substring(idx));
      } else {
        command = commandLine;
      }
      // convert to long file name and retry lookup
      try {
        command = new File(command).getCanonicalPath();
        commandLine2.insert(0, command);
        return super.determineParserForCommandline(commandLine2.toString());
      } catch (IOException e) {
        log.log(new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID, "CompileCommandsJsonParserFSN#determineParserForCommandline()", e));
      }
    }
    return parser;
  }

}
