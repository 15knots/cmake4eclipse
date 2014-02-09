/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core.ui;

import org.eclipse.cdt.ui.newui.AbstractPage;

/**
 * Page for CMake host OS specific project settings.
 *
 * @author Martin Weber
 */
public class HostOSPropertyPage extends AbstractPage {

  /*-
   * @see org.eclipse.cdt.ui.newui.AbstractPage#isSingle()
   */
  @Override
  protected boolean isSingle() {
    return false;
  }

}
