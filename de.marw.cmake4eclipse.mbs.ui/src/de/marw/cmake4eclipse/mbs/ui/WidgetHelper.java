/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

/**
 * Static methods for unified creation of widgets.
 *
 * @author Martin Weber
 */
public class WidgetHelper {

  private WidgetHelper() {
    // nothing to instantiate
  }

  /**
   * Creates a button.
   *
   * @param parent
   * @param text
   *        button text
   * @param enabled
   *        whether the button should be initially enabled
   */
  public static Button createButton(Composite parent, String text, boolean enabled) {
    Button button = new Button(parent, SWT.PUSH);
    button.setText(text);
    if (!enabled)
      button.setEnabled(false);
    GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
    //    gd.minimumWidth = width;
    button.setLayoutData(gd);
    return button;
  }

  /**
   * Creates a checkbox button.
   *
   * @param parent
   * @param horizontalAlignment
   *        how control will be positioned horizontally within a cell of the
   *        parent's grid layout, one of: SWT.BEGINNING (or SWT.LEFT),
   *        SWT.CENTER, SWT.END (or SWT.RIGHT), or SWT.FILL
   * @param horizontalSpan
   *        number of column cells in the parent's grid layout that the control
   *        will take up.
   * @param text
   *        text to display on the checkbox
   */
  public static Button createCheckbox(Composite parent, int horizontalAlignment,
      int horizontalSpan, String text) {
    Button b = new Button(parent, SWT.CHECK);
    b.setText(text);
    GridData gd = new GridData(horizontalAlignment, SWT.CENTER, false, false);
    gd.horizontalSpan = horizontalSpan;
    b.setLayoutData(gd);
    return b;
  }

  /**
   * Creates a group with a grid layout.
   *
   * @param parent
   * @param horizontalAlignment
   *        how control will be positioned horizontally within a cell of the
   *        parent's grid layout, one of: SWT.BEGINNING (or SWT.LEFT),
   *        SWT.CENTER, SWT.END (or SWT.RIGHT), or SWT.FILL
   * @param horizontalSpan
   *        number of column cells in the parent's grid layout that the control
   *        will take up.
   * @param text
   *        title text to display on the group
   * @param numColumns
   *        the number of columns in the grid inside the group
   */
  public static Group createGroup(Composite parent, int horizontalAlignment,
      int horizontalSpan, String text, int numColumns) {
    Group gr = new Group(parent, SWT.NONE);
    gr.setLayout(new GridLayout(numColumns, false));
    gr.setText(text);
    GridData gd = new GridData(horizontalAlignment, SWT.CENTER, true, false);
    gd.horizontalSpan = horizontalSpan;
    gr.setLayoutData(gd);
    return gr;
  }

}
