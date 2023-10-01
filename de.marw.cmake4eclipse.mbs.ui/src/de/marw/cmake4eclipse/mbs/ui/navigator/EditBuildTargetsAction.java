/*******************************************************************************
 * Copyright (c) 2023 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake4eclipse.mbs.ui.navigator;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionListenerAction;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * @author Martin Weber
 */
class EditBuildTargetsAction extends SelectionListenerAction {

  private final Shell shell;

  public EditBuildTargetsAction(Shell shell) {
    super("Edit Build T&argets");
    this.shell = shell;
    URL url = null;
    try {
      url = new URL("platform:/plugin/org.eclipse.cdt.make.ui/icons/etool16/target_edit.gif");
      ImageDescriptor imageDescriptor = ImageDescriptor.createFromURL(url);
      setImageDescriptor(imageDescriptor);
    } catch (MalformedURLException ignore) {
    }
    setEnabled(false);
  }

  private Object getSelectedElement() {
    if (getStructuredSelection().size() == 1) {
      return getStructuredSelection().getFirstElement();
    }
    return null;
  }

  @Override
  public void run() {
    Object selection = getSelectedElement();
    if (selection instanceof BuildTargetsContainer || selection instanceof NavBuildTarget) {
      BuildTargetsContainer container = selection instanceof BuildTargetsContainer ? (BuildTargetsContainer) selection
          : ((NavBuildTarget) selection).getContainer();
      PreferencesUtil.createPropertyDialogOn(shell, container.getProject(),
          "cmake4eclipse.mbs.ui.page_CMakeProjectProperty", null, null).open();
    }
  }
}
