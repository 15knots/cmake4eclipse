/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.ui;

import org.eclipse.cdt.ui.newui.AbstractPage;

/**
 * Page for CMake host OS specific project settings.
 *
 * @author Martin Weber
 * @deprecated obsolete, settings must be specified as workbench preferences now
 */
@Deprecated
public class HostOSPropertyPage extends AbstractPage {

  /*-
   * @see org.eclipse.cdt.ui.newui.AbstractPage#isSingle()
   */
  @Override
  protected boolean isSingle() {
    return false;
  }
}
