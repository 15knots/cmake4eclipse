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

/**
 * The {link IBuiltinsDetectionBehavior} for the GNU C and GNU C++ compiler (includes clang). This implementation is for
 * the 'gcc' and 'g++' command.
 *
 * @author Martin Weber
 */
public class GccBuiltinDetectionBehavior extends MaybeGccBuiltinDetectionBehavior {
  public boolean suppressErrormessage() {
    // report an error, if the compiler does not understand the arguments that enable built-in detection
    return false;
  }
}
