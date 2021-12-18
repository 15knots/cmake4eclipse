/*******************************************************************************
 * Copyright (c) 2014-2017 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.ui;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.marw.cmake4eclipse.mbs.settings.CmakeDefine;
import de.marw.cmake4eclipse.mbs.settings.CmakeVariableType;

/**
 * The dialog used to create or edit a CMake Cache Entry.
 *
 * @author Martin Weber
 */
public class AddCmakeDefineDialog extends Dialog {
  /**
   * the variable to edit or {@code null} if a new variable is going to be
   * created.
   */
  private CmakeDefine editedVar;
  /** the configuration description to use for variable selection dialog or {@code null} */
  private final ICConfigurationDescription cfgd;

  private Text variableName;
  private Combo typeSelector;
  private Text variableValue;
  private Button btnBrowseVars;
  private Button btnBrowseFiles;

  private static final String DIALOG_SETTINGS_SECT = "dlg_addCmakeDefine";

  /** String representation of each {@code CmakeVariableType}, by ordinal */
  private static final String[] typeNames;

  static {
    CmakeVariableType[] variableTypes = CmakeVariableType.values();
    typeNames = new String[variableTypes.length];
    for (int i = 0; i < variableTypes.length; i++) {
      typeNames[i] = variableTypes[i].name();
    }
  }

  /**
   * Creates a dialog. If a variable to edit is specified, it will be modified
   * in-place when the OK button is pressed. It will remain unchanged, if the
   * dialog is cancelled.
   *
   * @param parentShell
   * @param editedVar
   *          the variable to edit or {@code null} if a new variable is going to
   *          be created.
   * @param cfgd
   *          the configuration description to use for variable selection dialog
   *          or {@code null}
   */
  public AddCmakeDefineDialog(Shell parentShell, ICConfigurationDescription cfgd, CmakeDefine editedVar) {
    super(parentShell);
    setShellStyle(SWT.SHELL_TRIM);
    this.cfgd = cfgd;
    this.editedVar = editedVar;
  }

  /**
   * Gets the edited or newly created CMake Cache Entry.
   *
   * @return the modified or new CmakeDefine or {@code null} if this dialog has
   *         been cancelled.
   */
  public CmakeDefine getCmakeDefine() {
    return editedVar;
  }

  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    if (editedVar != null)
      shell.setText("Edit existing CMake Cache Entry");
    else
      shell.setText("Add new CMake Cache Entry");
  }

  /**
   * Overridden to read out the widgets before these get disposed.
   */
  @Override
  protected void okPressed() {
    if (editedVar != null) {
      editedVar.setName(variableName.getText().trim());
      editedVar.setType(indexToType(typeSelector.getSelectionIndex()));
      editedVar.setValue(variableValue.getText());
    } else {
      editedVar = new CmakeDefine(variableName.getText(),
          indexToType(typeSelector.getSelectionIndex()),
          variableValue.getText());
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
    ((GridLayout) comp.getLayout()).numColumns = 3;

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
    variableName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,
        2, 1));

    Label typeLabel = new Label(comp, SWT.NONE);
    typeLabel.setText("&Type:");

    typeSelector = new Combo(comp, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
    GridData gd = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    gd.widthHint = 100;
    typeSelector.setLayoutData(gd);
    typeSelector.setItems(typeNames);

    Label lblvalue = new Label(comp, SWT.NONE);
    lblvalue.setText("&Value:");

    variableValue = new Text(comp, SWT.BORDER);
    GridData gd_variableValue = new GridData(SWT.FILL, SWT.CENTER, true, false,
        2, 1);
    gd_variableValue.widthHint = 220;
    variableValue.setLayoutData(gd_variableValue);

    Label lbldummy = new Label(comp, SWT.NONE);
    lbldummy.setEnabled(false);

    // "Filesystem", "Variables" dialog launcher buttons...
    Composite buttonBar = new Composite(comp, SWT.NONE);
    GridLayout gl_buttonBar = new GridLayout(2, false);
    gl_buttonBar.marginLeft = 10;
    gl_buttonBar.marginWidth = 0;
    gl_buttonBar.marginHeight = 0;
    buttonBar.setLayout(gl_buttonBar);
    buttonBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 2,
        1));

    btnBrowseFiles = new Button(buttonBar, SWT.PUSH);
    btnBrowseFiles.setText("File System...");
    btnBrowseFiles.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
        false, 1, 1));
    btnBrowseFiles.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        String text;
        IDialogSettings settings = getDialogBoundsSettings();

        int sel = typeSelector.getSelectionIndex();
        if (CmakeVariableType.PATH == indexToType(sel)) {
          DirectoryDialog dialog = new DirectoryDialog(getShell());
          dialog.setFilterPath(settings.get("dir"));
          text = dialog.open();
          settings.put("dir", dialog.getFilterPath());
        } else {
          FileDialog dialog = new FileDialog(getShell());
          dialog.setFilterPath(settings.get("file"));
          text = dialog.open();
          settings.put("file", dialog.getFilterPath());
        }

        if (text != null)
          variableValue.insert(text);
      }
    });

    btnBrowseVars = new Button(buttonBar, SWT.PUSH);
    btnBrowseVars.setText("Insert Variable...");
    btnBrowseVars.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
        false, 1, 1));
    btnBrowseVars.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        String text = AbstractCPropertyTab.getVariableDialog(getShell(), cfgd);
        if (text != null)
          variableValue.insert(text);
      }
    });

    // to control sensitivity of buttons...
    typeSelector.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        int sel = ((Combo) e.widget).getSelectionIndex();
        adjustBrowseButtons(indexToType(sel));
      }
    });

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
      typeSelector.select(3); // default to STRING type
    } else {
      variableName.setText(editedVar.getName());
      typeSelector.select(editedVar.getType().ordinal());
      variableValue.setText(editedVar.getValue());
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

  /**
   * Converts an index in {@link AddCmakeDefineDialog#typeNames} to a Cmake
   * variable type.
   *
   * @param selectionIndex
   */
  private static CmakeVariableType indexToType(int selectionIndex) {
    return CmakeVariableType.values()[selectionIndex];
  }

  /**
   * Sets button enabledment based on cmake variable type.
   */
  private void adjustBrowseButtons(CmakeVariableType type) {
    switch (type) {
    case BOOL:
      btnBrowseFiles.setEnabled(false);
      btnBrowseVars.setEnabled(false);
      break;
    case FILEPATH:
    case PATH:
      btnBrowseFiles.setEnabled(true);
      btnBrowseVars.setEnabled(true);
      break;
    case STRING:
    case INTERNAL:
      btnBrowseFiles.setEnabled(false);
      btnBrowseVars.setEnabled(true);
      break;
    default:
      break;
    }
  }
}
