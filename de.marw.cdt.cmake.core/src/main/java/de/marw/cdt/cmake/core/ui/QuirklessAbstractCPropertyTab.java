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

import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

/**
 * Common workarounds of quirks in AbstractCPropertyTab.
 *
 * @author Martin Weber
 */
public abstract class QuirklessAbstractCPropertyTab extends
    AbstractCPropertyTab {

  private String helpContextId;

  /**
   * Overridden because the super class always prefixes the ID with its own
   * bundle name.
   */
  @Override
  public final String getHelpContextId() {
    return helpContextId;
  }

  /**
   * Overridden because the super class always prefixes the ID with its own
   * bundle name.
   *
   * @param id
   *        the help context id, including the plugin id.
   */
  @Override
  public final void setHelpContextId(String id) {
    helpContextId = id;
  }

  @Override
  protected void createControls(Composite parent) {
    super.createControls(parent);
    PlatformUI.getWorkbench().getHelpSystem()
        .setHelp(parent, getHelpContextId());
  }
}