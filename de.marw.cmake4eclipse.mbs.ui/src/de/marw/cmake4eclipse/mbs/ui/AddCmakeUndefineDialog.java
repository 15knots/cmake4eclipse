/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.marw.cmake4eclipse.mbs.settings.CmakeUnDefine;

/**
 * The dialog used to create or edit a cmake undefine.
 *
 * @author Martin Weber
 */
public class AddCmakeUndefineDialog extends Dialog {

  private static final String DIALOG_SETTINGS_SECT = "dlg_addCmakeUndefine";

  /**
   * the variable to edit or {@code null} if a new variable is going to be
   * created.
   */
  private CmakeUnDefine editedVar;

  private Text variableName;

  /**
   * Creates a dialog. If a variable to edit is specified, it will be modified
   * in-place when the OK button is pressed. It will remain unchanged, if the
   * dialog is cancelled.
   *
   * @param parentShell
   * @param editedVar
   *        the variable to edit or {@code null} if a new variable is going to
   *        be created.
   * @see #getCmakeUndefine()
   */
  public AddCmakeUndefineDialog(Shell parentShell, CmakeUnDefine editedVar) {
    super(parentShell);
    setShellStyle(SWT.SHELL_TRIM | SWT.PRIMARY_MODAL);
    this.editedVar = editedVar;
  }

  /**
   * Gets the edited or newly created cmake define.
   *
   * @return the modified or new CmakeDefine or {@code null} if this dialog has
   *         been cancelled.
   */
  public CmakeUnDefine getCmakeUndefine() {
    return editedVar;
  }

  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    if (editedVar != null)
      shell.setText("Edit existing CMake Undefine");
    else
      shell.setText("Add new CMake Undefine");
  }

  /**
   * Overridden to read out the widgets before these get disposed.
   */
  @Override
  protected void okPressed() {
    if (editedVar != null) {
      editedVar.setName(variableName.getText().trim());
    } else {
      editedVar = new CmakeUnDefine(variableName.getText());
    }
    super.okPressed();
  }

  /**
   * Create contents of the dialog.
   *
   * @param parent
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite comp = (Composite) super.createDialogArea(parent);
    ((GridLayout) comp.getLayout()).numColumns = 2;

    Label nameLabel = new Label(comp, SWT.NONE);
    nameLabel.setText("Variable &name:");
    GridData gd_nameLabel = new GridData();
    gd_nameLabel.horizontalAlignment = SWT.LEFT;
    nameLabel.setLayoutData(gd_nameLabel);

    variableName = new Text(comp, SWT.BORDER);
    // disable OK button if variable name is empty..
    variableName.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        final boolean enable = ((Text) e.widget).getText().trim().length() > 0;
        final Button button = getButton(IDialogConstants.OK_ID);
        button.setEnabled(enable);
      }
    });
    variableName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    return comp;
  }

  /**
   * Overridden to set the sensitivity of the dialog's OK-button.
   */
  @Override
  protected Control createContents(Composite parent) {
    final Control control = super.createContents(parent);
    updateDisplay();
    return control;
  }

  /**
   * Updates displayed values according to the variable to edit.
   */
  private void updateDisplay() {
    if (editedVar == null) {
      // create a new define
      variableName.setText("");
    } else {
      variableName.setText(editedVar.getName());
    }
  }

  /*-
   * @see org.eclipse.jface.dialogs.Dialog#getDialogBoundsSettings()
   */
  @Override
  protected IDialogSettings getDialogBoundsSettings() {
    IDialogSettings settings = Activator.getDefault().getDialogSettings();
    IDialogSettings section = settings.getSection(DIALOG_SETTINGS_SECT);
    if (section == null) {
      section = settings.addNewSection(DIALOG_SETTINGS_SECT);
    }
    return section;
  }

}
