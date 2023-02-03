/*******************************************************************************
 * Copyright (c) 2023 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake4eclipse.mbs.ui.slim;

import org.eclipse.cdt.ui.newui.AbstractPage;

/**
 * Holds the 'Source Location' tab. The only need to create it is distinguishing tabs.<br>
 * NOTE: The source location is <strong>not</strong> the location of the top-level CMakeLists.txt file. It is used by
 * the project explorer view in the Includes folder to organize the include dirs.<br>
 * See issue #176.
 *
 * @author Martin Weber
 */
public class Page_SourceFolders extends AbstractPage {

  @Override
  protected boolean isSingle() {
    return true;
  }
}
