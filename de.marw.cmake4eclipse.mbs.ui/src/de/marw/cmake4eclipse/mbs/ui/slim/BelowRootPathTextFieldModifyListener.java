/*******************************************************************************
 * Copyright (c) 2023 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake4eclipse.mbs.ui.slim;

import java.util.Objects;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Text;

/**
 * A ModifyListener for text fields that ensures that the entered text denotes a relative
 * {@link org.eclipse.core.runtime.IPath IPath} below a given root. It checks for {@code ..}-constructs in a given
 * input. For example "foo/.." would be allowed, but ".." of "foo/../.." would be not.
 *
 * @author Martin Weber
 */
public class BelowRootPathTextFieldModifyListener implements ModifyListener {

  private final PreferencePage page;
  private final String pageErrorMessage;

  /**
   * Add a {@code BelowRootPathTextFieldModifyListener} object to the given text field.
   *
   * @param field
   *                         The text field to listen to modifications
   * @param owningPage
   *                         the preference to display an error message if the path is not below the root
   * @param pageErrorMessage
   *                         The error message to display if the path is not below the root
   */
  public static BelowRootPathTextFieldModifyListener addListener(Text field, PreferencePage owningPage,
      String pageErrorMessage) {
    Objects.requireNonNull(field, "field");
    BelowRootPathTextFieldModifyListener listener = new BelowRootPathTextFieldModifyListener(owningPage, pageErrorMessage);
    field.addModifyListener(listener);
    return listener;
  }

  /**
   * @param owningPage
   *                         the preference to display an error message if the path is not below the root
   * @param pageErrorMessage
   *                         The error message to display if the path is not below the root
   */
  private BelowRootPathTextFieldModifyListener(PreferencePage owningPage, String pageErrorMessage) {
    this.page = Objects.requireNonNull(owningPage, "owningPage");
    this.pageErrorMessage = Objects.requireNonNull(pageErrorMessage, "pageErrorMessage");
  }

  @Override
  public void modifyText(ModifyEvent evt) {
    Path path = new Path(((Text) evt.widget).getText());
    final boolean ok = !"..".equals(path.segment(0));
    page.setValid(ok);
    page.setErrorMessage(ok ? null : pageErrorMessage);
  }
}
