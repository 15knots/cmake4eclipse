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

package de.marw.cmake.cdt.language.settings.providers.builtins;

import java.util.List;

/**
 * Specifies how built-in compiler macros and include path detection is handled for a specific compiler.
 *
 * @author Martin Weber
 */
public interface IBuiltinsDetectionBehavior {
  /**
   * Gets the compiler arguments that tell the compiler to output its built-in values for include search paths and
   * predefined macros. For the GNU compilers, these are {@code -E -P -dM -Wp,-v}.
   */
  List<String> getBuiltinsOutputEnablingArgs();

  /**
   * Creates a object that parses the output from built-in detection.
   */
  IBuiltinsOutputProcessor createCompilerOutputProcessor();

  /**
   * Gets whether to suppress the error-message that is printed if the compiler process exited with a non-zero exit
   * status code. Except for some special cases, most implementations, should be returned {@code false} here.
   */
  boolean suppressErrormessage();

  /**
   * Gets the filename extension for the input file. An empty input file will be created and its name will be given on
   * the command-line when the compiler is invoked for built-ins detection.
   *
   * @param languageID
   *          the language ID
   * @return the filename extension or {@code null} if no filename argument needs to be given for built-ins detection
   */
  String getInputFileExtension(String languageID);
}
