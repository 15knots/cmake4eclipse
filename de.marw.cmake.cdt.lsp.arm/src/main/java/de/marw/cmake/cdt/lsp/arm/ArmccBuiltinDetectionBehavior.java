/*******************************************************************************
 * Copyright (c) 2019 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake.cdt.lsp.arm;

import java.util.Arrays;
import java.util.List;

import de.marw.cmake.cdt.lsp.builtins.GccOutputProcessor;
import de.marw.cmake.cdt.lsp.builtins.IBuiltinsDetectionBehavior;
import de.marw.cmake.cdt.lsp.builtins.IBuiltinsOutputProcessor;

/**
 * The {link IBuiltinsDetectionBehavior} for the ARM C or C++ version 5 compiler.
 *
 * @author Martin Weber
 */
class ArmccBuiltinDetectionBehavior implements IBuiltinsDetectionBehavior {
  // --list_macros for macros
  private final List<String> enablingArgs = Arrays.asList("--list_macros");

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
    return null;
  }
}
