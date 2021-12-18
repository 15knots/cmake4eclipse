/*******************************************************************************
 * Copyright (c) 2021 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake4eclipse.mbs.ui.preferences;

import java.util.Objects;

import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import de.marw.cmake4eclipse.mbs.preferences.BuildToolKitDefinition;
import de.marw.cmake4eclipse.mbs.settings.CmakeGenerator;
import de.marw.cmake4eclipse.mbs.ui.Activator;
import de.marw.cmake4eclipse.mbs.ui.WidgetHelper;

/**
 * The dialog used to add, duplicate or edit a build tool kit definition.
 */
abstract class AbstractBuildToolkitDialog extends MessageDialog {

  private static final String DIALOG_SETTINGS_SECT = "BuildToolKit";
  /**
   * the tool kit to edit
   */
  private BuildToolKitDefinition toolkit;
  private Text tkName;
  private ComboViewer tkGenerator;
  private Text tkPath;
  /** cmake executable */
  private Text tkCmakeExternalFile;

  private Button btnPathBrowseVars;
  private Button btnPathBrowseFiles;
  /** 'use cmake from path' checkbox */
  private Button tkIsExternalCmake;
  /** browse files for cmake executable */
  private Button btnCmakeBrowseFiles;
  /** variables in cmake executable text field */
  private Button btnCmakeBrowseVars;
  private ModifyListener handleOKButtonEnabled;

  public AbstractBuildToolkitDialog(Shell parentShell, BuildToolKitDefinition toolkit, String title) {
    super(parentShell, title, null, "CMake Build Tool Kit Definition", MessageDialog.NONE, 0, IDialogConstants.OK_LABEL,
        IDialogConstants.CANCEL_LABEL);
    this.toolkit = Objects.requireNonNull(toolkit, "toolkit");
    setShellStyle(SWT.SHELL_TRIM);
  }

  /**
   * Gets the edited BuildToolKitDefinition.
   *
   * @return the BuildToolKitDefinition
   */
  public BuildToolKitDefinition getBuildToolKit() {
    return toolkit;
  }

  /**
   * Overridden to read out the widgets before these get disposed.
   */
  @Override
  protected void buttonPressed(int buttonId) {
    if (buttonId == Window.OK) {
      saveToModel();
    }
    super.buttonPressed(buttonId);
  }

  /**
   * Create contents of the dialog.
   *
   * @param parent
   */
  @Override
  protected Control createCustomArea(Composite parent) {
    Composite comp = new Composite(parent, SWT.NONE);
    handleOKButtonEnabled = (e) -> {
      boolean disabled = tkName.getText().isBlank() || tkPath.getText().isBlank()
          || (tkIsExternalCmake.getSelection() && tkCmakeExternalFile.getText().isBlank());
      final Button button = getButton(IDialogConstants.OK_ID);
      button.setEnabled(!disabled);
    };

    {
      GridLayout layout = new GridLayout(2, false);
      layout.marginWidth = layout.marginHeight = 0;
      comp.setLayout(layout);
      comp.setLayoutData(new GridData(GridData.FILL_BOTH));
    }
    {
      Label label = new Label(comp, SWT.NONE);
      label.setText("&Name:");
      GridData gd_nameLabel = new GridData();
      gd_nameLabel.horizontalAlignment = SWT.LEAD;
      label.setLayoutData(gd_nameLabel);

      tkName = new Text(comp, SWT.BORDER);
      tkName.addModifyListener(handleOKButtonEnabled);
      tkName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    }
    {
      Label label = new Label(comp, SWT.NONE);
      label.setText("&Build System:");

      tkGenerator = new ComboViewer(comp);
      tkGenerator.getControl().setToolTipText("The build system to generate scripts for.");
      final GridData gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1);
      tkGenerator.getCombo().setLayoutData(gd);
      tkGenerator.setContentProvider(ArrayContentProvider.getInstance());
      tkGenerator.setLabelProvider(new LabelProvider() {
        @Override
        public String getText(Object element) {
          if (element instanceof CmakeGenerator) {
            return ((CmakeGenerator) element).getCmakeName();
          }
          return super.getText(element);
        }

      });

      tkGenerator.setInput(getAvailableGenerators());
    }
    // $PATH group...
    createPathGroup(comp);
    // cmake executable group...
    createCmakeExeutableGroup(comp);

    return comp;
  }

  // $PATH group...
  private void createPathGroup(Composite parent) {
    Group gr = WidgetHelper.createGroup(parent, SWT.FILL, 2,
        "$PATH environment variable used to locate the build tools:", 1);

    tkPath = new Text(gr, SWT.BORDER);
    tkPath.setToolTipText("The value of the $PATH environment variable used to locate the build tools.");
    GridData gd_path = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
    gd_path.widthHint = 220;
    tkPath.setLayoutData(gd_path);
    tkPath.addModifyListener(handleOKButtonEnabled);

    // "Filesystem", "Variables" dialog launcher buttons...
    Composite buttonBar = new Composite(gr, SWT.NONE);
    GridLayout gl_buttonBar = new GridLayout(2, false);
    gl_buttonBar.marginWidth = gl_buttonBar.marginHeight = 0;
    buttonBar.setLayout(gl_buttonBar);
    buttonBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 2, 1));

    btnPathBrowseFiles = new Button(buttonBar, SWT.PUSH);
    btnPathBrowseFiles.setText("&File System...");
    btnPathBrowseFiles.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    btnPathBrowseFiles.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        String text;
        IDialogSettings settings = getDialogSettings();
        DirectoryDialog dialog = new DirectoryDialog(getShell());
        dialog.setFilterPath(settings.get("dir"));
        text = dialog.open();
        settings.put("dir", dialog.getFilterPath());

        if (text != null)
          tkPath.insert(text);
      }
    });

    btnPathBrowseVars = new Button(buttonBar, SWT.PUSH);
    btnPathBrowseVars.setText("Insert &Variable...");
    btnPathBrowseVars.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    btnPathBrowseVars.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        String text = AbstractCPropertyTab.getVariableDialog(getShell(), null);
        if (text != null)
          tkPath.insert(text);
      }
    });
    createLink(buttonBar, "Configure Build Variables...",
        "org.eclipse.cdt.managedbuilder.ui.preferences.PrefPage_Vars");
  }

  // cmake executable group...
  private void createCmakeExeutableGroup(Composite parent) {
    Group gr = WidgetHelper.createGroup(parent, SWT.FILL, 2, "CMake Executable", 2);
    /*
     */
    tkIsExternalCmake = WidgetHelper.createCheckbox(gr, SWT.BEGINNING, 2,
        "Do not use cmake executable from &tool kit path");
    tkIsExternalCmake.setToolTipText("Enable if you are running windows and for example use cygwin.\n"
        + "The cygwin variant of cmake produces file paths (like /bin/cc) that are not understood\n"
        + "by the workbench, causing each build to fail.");
    // to adjust sensitivity...
    tkIsExternalCmake.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        final Button btn = (Button) event.widget;
        handleCmakeCommandEnabled(btn.getSelection());
        handleOKButtonEnabled.modifyText(null);
      }
    });
    {
      Label l = new Label(gr, SWT.NONE);
      l.setText("&CMake");
    }
    {
      tkCmakeExternalFile = new Text(gr, SWT.BORDER);
      GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
      gd.widthHint = 220;
      tkCmakeExternalFile.setLayoutData(gd);
      tkCmakeExternalFile.addModifyListener(handleOKButtonEnabled);
    }
    // "Filesystem", "Variables" dialog launcher buttons...
    Composite buttonBar = new Composite(gr, SWT.NONE);
    {
      buttonBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false, 3, 1));
      GridLayout layout = new GridLayout(2, false);
      layout.marginHeight = 0;
      layout.marginWidth = 0;
      buttonBar.setLayout(layout);
    }
    btnCmakeBrowseFiles = WidgetHelper.createButton(buttonBar, "F&ile System...", true);
    btnCmakeBrowseFiles.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        IDialogSettings settings = Activator.getDefault().getDialogSettings();
        FileDialog dialog = new FileDialog(tkCmakeExternalFile.getShell());
        dialog.setFilterPath(settings.get("cmake_dir"));
        String text = dialog.open();
        settings.put("cmake_dir", dialog.getFilterPath());
        if (text != null) {
          tkCmakeExternalFile.setText(text);
        }
      }
    });

    btnCmakeBrowseVars = WidgetHelper.createButton(buttonBar, "Insert V&ariable...", true);
    btnCmakeBrowseVars.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        String text = AbstractCPropertyTab.getVariableDialog(tkCmakeExternalFile.getShell(), null);
        if (text != null) {
          tkCmakeExternalFile.insert(text);
        }
      }
    });
  }

  private Link createLink(Composite composite, String text, final String targetPreferencePageId) {
    Link link = new Link(composite, SWT.NONE);
    link.setFont(composite.getFont());
    link.setText("<A>" + text + "</A>"); //$NON-NLS-1$//$NON-NLS-2$
    link.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        widgetDefaultSelected(e);
      }

      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        PreferencesUtil
            .createPreferenceDialogOn(getShell(), targetPreferencePageId, new String[] { targetPreferencePageId }, null)
            .open();
      }
    });
    return link;
  }

  /* package */ static CmakeGenerator[] getAvailableGenerators() {
    switch (Platform.getOS()) {
    case Platform.OS_WIN32:
      return new CmakeGenerator[] { CmakeGenerator.MinGWMakefiles, CmakeGenerator.Ninja, CmakeGenerator.MSYSMakefiles,
          CmakeGenerator.UnixMakefiles, CmakeGenerator.NMakeMakefiles, CmakeGenerator.NMakeMakefilesJOM,
          CmakeGenerator.BorlandMakefiles, CmakeGenerator.WatcomWMake };
    default:
      // fall back to linux, if OS is unknown
      return new CmakeGenerator[] { CmakeGenerator.Ninja, CmakeGenerator.UnixMakefiles
          // see https://gitlab.kitware.com/cmake/cmake/-/issues/14793
          // , CmakeGenerator.WatcomWMake
      };
    }
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
   * Changes sensitivity of controls to enter the cmake command. Necessary since Button.setSelection does not fire
   * events.
   *
   * @param enabled the new enabled state
   */
  private void handleCmakeCommandEnabled(boolean enabled) {
    tkCmakeExternalFile.setEnabled(enabled);
    btnCmakeBrowseFiles.setEnabled(enabled);
    btnCmakeBrowseVars.setEnabled(enabled);
  }

  /**
   * Updates displayed values according to the build tool kit to edit.
   */
  private void updateDisplay() {
    tkName.setText(toolkit.getName());
    CmakeGenerator generator = toolkit.getGenerator();
    tkGenerator.setSelection(new StructuredSelection(generator));
    tkPath.setText(toolkit.getPath());

    tkIsExternalCmake.setSelection(toolkit.isExternalCmake());
    tkCmakeExternalFile.setText(Objects.toString(toolkit.getExternalCmakeFile(), ""));
    // adjust sensitivity...
    handleCmakeCommandEnabled(tkIsExternalCmake.getSelection());
  }

  /**
   * Stores displayed values to the build tool kit edited by this dialog.
   *
   * @see #updateDisplay()
   */
  private void saveToModel() {
    toolkit.setName(tkName.getText());
    toolkit.setPath(tkPath.getText());
    final IStructuredSelection sel = (IStructuredSelection) tkGenerator.getSelection();
    toolkit.setGenerator((CmakeGenerator) sel.getFirstElement());
    toolkit.setExternalCmake(tkIsExternalCmake.getSelection());
    String file = tkCmakeExternalFile.getText();
    toolkit.setExternalCmakeFile(file.isBlank() ? null : file);
  }

  @Override
  protected IDialogSettings getDialogBoundsSettings() {
    return getDialogSettings();
  }

  /**
   * Gets the dialog settings that should be used for remembering the settings of of the dialog.
   *
   * @return settings the dialog settings used to store the dialog's settings.
   */
  protected IDialogSettings getDialogSettings() {
    IDialogSettings settings = PlatformUI.getDialogSettingsProvider(Activator.getDefault().getBundle())
        .getDialogSettings();
    IDialogSettings section = settings.getSection(DIALOG_SETTINGS_SECT);
    if (section == null) {
      section = settings.addNewSection(DIALOG_SETTINGS_SECT);
    }
    return section;
  }
}