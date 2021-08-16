/*******************************************************************************
 * Copyright (c) 2014-2019 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.ui;

import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.cdt.ui.newui.ICPropertyTab;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

/**
 * Common workarounds of quirks in AbstractCPropertyTab.
 *
 * @author Martin Weber
 */
public abstract class QuirklessAbstractCPropertyTab extends AbstractCPropertyTab {

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
   *          the help context id, including the plugin id.
   */
  @Override
  public final void setHelpContextId(String id) {
    helpContextId = id;
  }

  @Override
  protected void createControls(Composite parent) {
    super.createControls(parent);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, getHelpContextId());
  }

  /**
   * Overridden to have a logic here that <em>I</em> can understand.
   */
  @Override
  public void handleTabEvent(int kind, Object data) {
    switch (kind) {
    case ICPropertyTab.DEFAULTS:
      if (canBeVisible()) {
        performDefaults();
      }
      break;
    default:
      super.handleTabEvent(kind, data);
      break;
    }
  }

  /**
   * Makes the UI display the specified new settings.<br>
   * Overridden to have documentation. This documentation is reversed engineered from existing CDT code;
   * AbstractCPropertyTab.java lacks documentation.
   *
   * @param resd
   *          the setting to display
   */
  @Override
  protected abstract void updateData(ICResourceDescription resd);

  @Override
  protected void updateButtons() {
    // never called from superclass, but abstract :-)
  }
}