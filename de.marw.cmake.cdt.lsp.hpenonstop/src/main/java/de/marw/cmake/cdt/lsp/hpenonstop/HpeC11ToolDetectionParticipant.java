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

package de.marw.cmake.cdt.lsp.hpenonstop;

import de.marw.cmake.cdt.lsp.Arglets;
import de.marw.cmake.cdt.lsp.DefaultToolCommandlineParser;
import de.marw.cmake.cdt.lsp.DefaultToolDetectionParticipant;
import de.marw.cmake.cdt.lsp.IArglet;
import de.marw.cmake.cdt.lsp.ResponseFileArglets;

/**
 * HPE NonStop c11 C & C++.
 *
 * @author Martin Weber
 */
public class HpeC11ToolDetectionParticipant extends DefaultToolDetectionParticipant {

  public HpeC11ToolDetectionParticipant() {
    super("c11", true, "exe", new ToolCommandlineParser());
  }

  private static class ToolCommandlineParser extends DefaultToolCommandlineParser {

    private static final IArglet[] arglets = {
        new Arglets.IncludePath_C_POSIX(),
        new Arglets.MacroDefine_C_POSIX(), new Arglets.MacroUndefine_C_POSIX()
    };

    private ToolCommandlineParser() {
      super(null, new ResponseFileArglets.At(), null, arglets);
    }
  }
}
