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

/**
 * Classifications of compilers regarding the detection of compiler-built-in macros and include paths.
 *
 * @author Martin Weber
 *
 */
public enum BuiltinDetectionType {
  /**
   * Clearly recognized GNU C or C++ compatible compiler, (includes clang): -E -P -dM for macros, -Wp,-v for include
   * paths
   */
  GCC,
  /**
   * 'cc{@link #clone()}, but may be a GNU C or C++ compatible compiler, (includes clang): -E -P -dM for macros, -Wp,-v
   * for include paths
   */
  GCC_MAYBE,
  /**
   * NVidia CUDA compiler: -E -Xcompiler -P -Xcompiler -dD for macros, -Xcompiler -v for include paths
   */
  NVCC,
  /**
   * Intel C or C++ compatible compiler: -EP -dM for macros, -H for include paths
   */
  ICC,
  /**
   * ms C or C++ compiler: /nologo /EP /dM for macros
   */
  CL,
  /**
   * detection of compiler built-ins is not support or not implemented yet
   */
  NONE;
}
