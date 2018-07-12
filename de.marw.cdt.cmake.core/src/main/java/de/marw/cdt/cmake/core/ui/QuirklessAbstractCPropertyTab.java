/*******************************************************************************
 * Copyright (c) 2014-2017 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core.ui;

import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.cdt.ui.newui.ICPropertyTab;
import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

/**
 * Common workarounds of quirks in AbstractCPropertyTab.
 *
 * @author Martin Weber
 */
public abstract class QuirklessAbstractCPropertyTab extends AbstractCPropertyTab {

  private String helpContextId;
  /** the last build configuration(s) being edited */
  private ICResourceDescription lastConfig;

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
    case ICPropertyTab.UPDATE:
      // the user wants to edit a/some different configurations...
      if (canBeVisible()) {
        final ICResourceDescription newConfig = (ICResourceDescription) data;
        if (newConfig != lastConfig) {
          final ICResourceDescription lastConfig2 = lastConfig;
          lastConfig = newConfig;
          Assert.isTrue(getResDesc() == newConfig);
          configSelectionChanged(lastConfig2, newConfig);
        }
      }
      break;
    default:
      super.handleTabEvent(kind, data);
      break;
    }
  }

  /**
   * Overridden to have a logic here that <em>I</em> can understand.
   */
  @Override
  protected void updateData(ICResourceDescription resd) {
  }

  /**
   * Notified, when the user had chosen to edit a/some different build
   * configuration(s). Also invoked when the dialog opens.
   *
   * @param lastConfig
   *          the last configuration being edited or {@code null} if the
   *          dialog is opening.
   * @param newConfig
   *          the new configuration to edit. Note that
   *          {@link AbstractCPropertyTab#getResDesc()} will return the new
   *          build configuration when this method is invoked, we just pass it
   *          to be in sync with {@link AbstractCPropertyTab#updateData}.
   *
   */
  protected abstract void configSelectionChanged(ICResourceDescription lastConfig, ICResourceDescription newConfig);

  @Override
  public void dispose() {
    lastConfig = null;
    super.dispose();
  }
}