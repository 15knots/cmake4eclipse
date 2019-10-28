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

package de.marw.cmake.cdt.lsp.intel;

import de.marw.cmake.cdt.lsp.Arglets;
import de.marw.cmake.cdt.lsp.DefaultToolCommandlineParser;
import de.marw.cmake.cdt.lsp.DefaultToolDetectionParticipant;
import de.marw.cmake.cdt.lsp.IArglet;
import de.marw.cmake.cdt.lsp.ResponseFileArglets;

/**
 * C + C++, Windows, EDG.
 *
 * @author Martin Weber
 */
public class IclCandCppToolDetectionParticipant extends DefaultToolDetectionParticipant {

  public IclCandCppToolDetectionParticipant() {
    super("icl", true, "exe", new CandCppToolCommandlineParser());
  }

  private static class CandCppToolCommandlineParser extends DefaultToolCommandlineParser {

    private static final IArglet[] arglets = { new Arglets.IncludePath_C_POSIX(), new Arglets.MacroDefine_C_POSIX(),
        new Arglets.MacroUndefine_C_POSIX() };

    private CandCppToolCommandlineParser() {
      super(null, new ResponseFileArglets.At(), null, arglets);
    }
  }
}
