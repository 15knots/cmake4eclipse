/*******************************************************************************
 * Copyright (c) 2013 Martin Weber.
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
 * Page for CMake project settings.
 *
 * @author Martin Weber
 */
public class CMakePropertyPage extends AbstractPage {

//  /*-
//   * @see org.eclipse.cdt.ui.newui.AbstractPrefPage#getHeader()
//   */
//  @Override
//  protected String getHeader() {
//    return isSingle() ? null : "CMake settings";
//  }

  /*-
   * @see org.eclipse.cdt.ui.newui.AbstractPage#isSingle()
   */
  @Override
  protected boolean isSingle() {
    return true;// currently a single Tab
  }

}
