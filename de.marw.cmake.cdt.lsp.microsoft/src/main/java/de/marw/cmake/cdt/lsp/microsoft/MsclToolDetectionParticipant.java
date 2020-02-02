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

package de.marw.cmake.cdt.lsp.microsoft;

import de.marw.cmake.cdt.lsp.DefaultToolDetectionParticipant;

/**
 * Microsoft C and C++ compiler (cl).
 *
 * @author Martin Weber
 */
public class MsclToolDetectionParticipant extends DefaultToolDetectionParticipant {

  public MsclToolDetectionParticipant() {
    super("cl", true, "exe", new MsclToolCommandlineParser());
  }
}
