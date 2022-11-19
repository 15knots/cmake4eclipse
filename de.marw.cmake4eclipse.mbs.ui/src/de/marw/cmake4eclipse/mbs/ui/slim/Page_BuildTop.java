/*******************************************************************************
 * Copyright (c) 2022 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.ui.slim;

import org.eclipse.cdt.ui.newui.AbstractPage;

/**
 * Page for CMake project settings in the simplified MBS UI.
 *
 * @author Martin Weber
 */
public class Page_BuildTop extends AbstractPage {
  @Override
  protected boolean isSingle() {
    return false;
  }
}
