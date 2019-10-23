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

import java.util.Arrays;
import java.util.List;

import de.marw.cmake.cdt.language.settings.providers.builtins.IBuiltinsDetectionBehavior;
import de.marw.cmake.cdt.language.settings.providers.builtins.IBuiltinsOutputProcessor;

/**
 * The {link IBuiltinsDetectionBehavior} for the NVidia CUDA compiler.
 *
 * @author Martin Weber
 */
public class NvccBuiltinDetectionBehavior implements IBuiltinsDetectionBehavior {
  // -E -Xcompiler -P -Xcompiler -dM for macros, -Xcompiler -v for include paths
  private final List<String> enablingArgs = Arrays.asList("-E", "-Xcompiler", "-P", "-Xcompiler", "-dM", "-Xcompiler",
      "-v");

  @Override
  public List<String> getBuiltinsOutputEnablingArgs() {
    return enablingArgs;
  }

  @Override
  public IBuiltinsOutputProcessor createCompilerOutputProcessor() {
    return new GccOutputProcessor();
  }

  public boolean suppressErrormessage() {
    // report an error, if the compiler does not understand the arguments that enable built-in detection
    return false;
  }

  @Override
  public String getInputFileExtension(String languageId) {
    if (languageId.equals("org.eclipse.cdt.core.gcc")) {
      return "c";
    }
    if (languageId.equals("org.eclipse.cdt.core.g++")) {
      return "cpp";
    }
    if (languageId.equals("com.nvidia.cuda.toolchain.language.cuda.cu")) {
      return "cu";
    }
    return null;
  }
}
