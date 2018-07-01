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
   * 'cc, but may be a GNU C or C++ compatible compiler, (includes clang): -E -P -dM for macros, -Wp,-v
   * for include paths
   */
  GCC_MAYBE,
  /**
   * NVidia CUDA compiler: -E -Xcompiler -P -Xcompiler -dM for macros, -Xcompiler -v for include paths
   */
  NVCC,
  /**
   * Intel C or C++ compatible compiler: -EP -dM for macros, -H for include FILES. NOTE: Windows: /QdM.
   */
  ICC,
  /**
   * ms C or C++ compiler: /nologo /EP? /dM? for macros. (Reserved, if someone finds out the proper options)
   */
  CL,
  /**
   * detection of compiler built-ins is not support or not implemented yet
   */
  NONE;
}
