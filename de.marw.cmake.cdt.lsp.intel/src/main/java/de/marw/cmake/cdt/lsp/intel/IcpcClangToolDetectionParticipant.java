/*******************************************************************************
 * Copyright (c) 2019 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake.cdt.lsp.intel;

import de.marw.cmake.cdt.lsp.DefaultToolDetectionParticipant;

/**
 * C++, OS X, clang.
 *
 * @author Martin Weber
 */
public class IcpcClangToolDetectionParticipant extends DefaultToolDetectionParticipant {

  public IcpcClangToolDetectionParticipant() {
    super("icl\\+\\+", IntelCppToolCommandlineParser.INSTANCE);
  }
}
