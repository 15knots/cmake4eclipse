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

import de.marw.cmake.cdt.lsp.builtins.GccOutputProcessor;
import de.marw.cmake.cdt.lsp.builtins.IBuiltinsDetectionBehavior;
import de.marw.cmake.cdt.lsp.builtins.IBuiltinsOutputProcessor;

/**
 * The {link IBuiltinsDetectionBehavior} for the GNU C and GNU C++ compiler (includes clang). This implementation assumes that the 'cc'
 * command (which is the same as any POSIX compliant compiler) actually is a GNU compiler.
 *
 * @author Martin Weber
 */
public class MaybeGccBuiltinDetectionBehavior implements IBuiltinsDetectionBehavior {
  // -E -P -dM for macros, -Wp,-v for include paths
  private final List<String> enablingArgs = Arrays.asList("-E", "-P", "-dM", "-Wp,-v");

  @Override
  public List<String> getBuiltinsOutputEnablingArgs() {
    return enablingArgs;
  }

  @Override
  public IBuiltinsOutputProcessor createCompilerOutputProcessor() {
    return new GccOutputProcessor();
  }

  @Override
  public boolean suppressErrormessage() {
    // Assume 'cc' is a GNU compiler: do not report an error, if the compiler actually is
    // a POSIX compiler that does not understand the arguments that enable built-in detection
    return true;
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
