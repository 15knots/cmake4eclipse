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

package de.marw.cmake.cdt.lsp.nvidia;

import java.util.Collections;
import java.util.List;

import de.marw.cmake.cdt.lsp.Arglets;
import de.marw.cmake.cdt.lsp.DefaultToolCommandlineParser;
import de.marw.cmake.cdt.lsp.DefaultToolDetectionParticipant;
import de.marw.cmake.cdt.lsp.IArglet;
import de.marw.cmake.cdt.lsp.ResponseFileArglets;

/**
 * CUDA: nvcc compilers (POSIX compatible).
 *
 * @author Martin Weber
 */
public class NvccToolDetectionParticipant extends DefaultToolDetectionParticipant {
  static final String COM_NVIDIA_CUDA_LANGUAGE_ID = "com.nvidia.cuda.toolchain.language.cuda.cu";

  public NvccToolDetectionParticipant() {
    super("nvcc", true, "exe", new ToolCommandlineParser());
  }

  private static class ToolCommandlineParser extends DefaultToolCommandlineParser {

    private static final IArglet[] arglets = { new Arglets.IncludePath_C_POSIX(), new Arglets.MacroDefine_C_POSIX(),
        new Arglets.MacroUndefine_C_POSIX(), new NvccSystemIncludePathArglet(), new Arglets.SystemIncludePath_C(),
        new NvccLangStdArglet() };

    private ToolCommandlineParser() {
      super(COM_NVIDIA_CUDA_LANGUAGE_ID, new ResponseFileArglets.At(), new NvccBuiltinDetectionBehavior(), arglets);
    }

    @Override
    public List<String> getCustomLanguageIds() {
      return Collections.singletonList(COM_NVIDIA_CUDA_LANGUAGE_ID);
    }
  }
}
