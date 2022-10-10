/*******************************************************************************
 * Copyright (c) 2022 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake4eclipse.mbs.ui.slim;

import java.util.Arrays;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IProjectType;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.ui.properties.ManagedBuilderUIImages;
import org.eclipse.cdt.managedbuilder.ui.wizards.CfgHolder;
import org.eclipse.cdt.ui.newui.CDTPrefUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Page for configuring a new project. Allows to select the build configurations and shows a button to open the standard
 * project properties page in a separate dialog.
 *
 * @author Martin Weber
 */
// copied from org.eclipse.cdt.managedbuilder.ui.wizards.CDTConfigWizardPage
class NewProjectConfigWizardPage extends WizardPage {

  private static final Image IMG_CONFIG = ManagedBuilderUIImages.get(ManagedBuilderUIImages.IMG_BUILD_CONFIG);
  private static final String TITLE = "Select Configurations";
  private static final String MESSAGE = "Select the build configurations";
  private static final String COMMENT = "Use \"Advanced settings\" button to edit project's properties.\n" + "\n"
      + "Additional configurations can be added after project creation.\n"
      + "Use \"Manage configurations\" buttons either on toolbar or on property \n" + " pages.";
  private static final String EMPTY_STR = ""; //$NON-NLS-1$

  private CheckboxTableViewer tv;
  private Composite parent;

  NewProjectConfigWizardPage() {
    super("de.marw.cmake4eclipse.mbs.ui.slim.NewProjectConfigWizardPage", TITLE, null);
  }

  @Override
  public void createControl(Composite p) {
    parent = new Composite(p, SWT.NONE);
    parent.setFont(parent.getFont());
    parent.setLayout(new GridLayout());
    parent.setLayoutData(new GridData(GridData.FILL_BOTH));

    Composite c1 = new Composite(parent, SWT.NONE);
    c1.setLayout(new GridLayout(2, false));
    c1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    setupLabel(c1, EMPTY_STR, GridData.BEGINNING);

    Composite c2 = new Composite(parent, SWT.NONE);
    c2.setLayout(new GridLayout(2, false));
    c2.setLayoutData(new GridData(GridData.FILL_BOTH));

    Table table = new Table(c2, SWT.BORDER | SWT.CHECK | SWT.V_SCROLL);
    GridData gd = new GridData(GridData.FILL_BOTH);
    table.setLayoutData(gd);

    tv = new CheckboxTableViewer(table);
    tv.setContentProvider(new IStructuredContentProvider() {
      @Override
      public Object[] getElements(Object inputElement) {
        return (Object[]) inputElement;
      }

      @Override
      public void dispose() {
      }

      @Override
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }
    });
    tv.setLabelProvider(new LabelProvider() {
      @Override
      public String getText(Object element) {
        return element == null ? EMPTY_STR : ((CfgHolder) element).getName();
      }

      @Override
      public Image getImage(Object element) {
        return IMG_CONFIG;
      }
    });
    tv.addCheckStateListener(new ICheckStateListener() {
      @Override
      public void checkStateChanged(CheckStateChangedEvent event) {
        setPageComplete(isCustomPageComplete());
        update();
      }
    });
    tv.setInput(CfgHolder.unique(getAvailableConfigurations()));
    tv.setAllChecked(true);

    Composite c = new Composite(c2, SWT.NONE);
    c.setLayoutData(new GridData(GridData.FILL_VERTICAL));
    c.setLayout(new GridLayout());

    Button b1 = new Button(c, SWT.PUSH);
    b1.setText("Select all");
    b1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    b1.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        tv.setAllChecked(true);
        setPageComplete(isCustomPageComplete());
        update();
      }
    });

    Button b2 = new Button(c, SWT.PUSH);
    b2.setText("Deselect all");
    b2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    b2.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        tv.setAllChecked(false);
        setPageComplete(isCustomPageComplete());
        update();
      }
    });

    // dummy placeholder
    new Label(c, 0).setLayoutData(new GridData(GridData.FILL_BOTH));

    Button b3 = new Button(c, SWT.PUSH);
    b3.setText("Advanced Settings...");
    b3.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    b3.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        advancedDialog();
      }
    });

    Group gr = new Group(parent, SWT.NONE);
    gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = 2;
    gr.setLayoutData(gd);
    gr.setLayout(new FillLayout());
    Label lb = new Label(gr, SWT.NONE);
    lb.setText(COMMENT);

    setControl(parent);
  }

  /**
   * Gets the IConfiguration objects defined for the "cmake4eclipse.mbs.projectType" in the plugin.xml.
   */
  /* package */ static CfgHolder[] getAvailableConfigurations() {
    IProjectType pt = ManagedBuildManager
        .getExtensionProjectType(de.marw.cmake4eclipse.mbs.internal.Activator.CMAKE4ECLIPSE_PROJECT_TYPE);
    IConfiguration[] cfgs = pt.getConfigurations();
    CfgHolder[] cfghs = CfgHolder
        .cfgs2items(Arrays.stream(cfgs).filter(cfg -> !cfg.isSystemObject()).toArray(IConfiguration[]::new));
    return CfgHolder.reorder(CfgHolder.unique(cfghs));
  }

  /**
   * Gets the IConfiguration objects defined for the "cmake4eclipse.mbs.projectType" in the plugin.xml that have been
   * enable by the user.
   *
   * @return
   */
  IConfiguration[] getCheckedConfigurations() {
    return Arrays.stream(tv.getTable().getItems()).filter(ti -> ti.getChecked())
        .map(ti -> ((CfgHolder) ti.getData()).getConfiguration()).toArray(IConfiguration[]::new);
  }

  /**
   * Returns whether this page's controls currently all contain valid values.
   *
   * @return <code>true</code> if all controls are valid, and <code>false</code> if at least one is invalid
   */
  private boolean isCustomPageComplete() {
    if (tv.getTable().getItemCount() == 0) {
      String errorMessage = "At least one configuration should be available. Project cannot be created.";
      setErrorMessage(errorMessage);
      setMessage(errorMessage);
      return false;
    }
    if (tv.getCheckedElements().length == 0) {
      String errorMessage = "At least one configuration should be selected. Please check needed configurations.";
      setErrorMessage(errorMessage);
      setMessage(errorMessage);
      return false;
    }
    setErrorMessage(null);
    setMessage(MESSAGE);
    return true;
  }

  private static Label setupLabel(Composite c, String name, int mode) {
    Label l = new Label(c, SWT.WRAP);
    l.setText(name);
    GridData gd = new GridData(mode);
    gd.verticalAlignment = SWT.TOP;
    l.setLayoutData(gd);
    Composite p = l.getParent();
    l.setFont(p.getFont());
    return l;
  }

  @Override
  public Control getControl() {
    return parent;
  }

  protected void update() {
    getWizard().getContainer().updateButtons();
    getWizard().getContainer().updateMessage();
    getWizard().getContainer().updateTitleBar();
  }

  /**
   * Edit project properties.
   */
  private void advancedDialog() {
    if (getWizard() instanceof NewProjectTemplateWizard) {
      // orig: CDTCommonProjectWizard
      NewProjectTemplateWizard nmWizard = (NewProjectTemplateWizard) getWizard();
      IProject newProject = nmWizard.getProject(false);
      if (newProject != null) {
        boolean oldManage = CDTPrefUtil.getBool(CDTPrefUtil.KEY_NOMNG);
        // disable manage configurations button
        CDTPrefUtil.setBool(CDTPrefUtil.KEY_NOMNG, true);
        try {
          int res = PreferencesUtil.createPropertyDialogOn(getWizard().getContainer().getShell(), newProject,
              "cmake4eclipse.mbs.ui.page_Build", null, null).open();
          if (res != Window.OK) {
            // if user presses cancel, remove the project.
            nmWizard.performCancel();
          }
        } finally {
          CDTPrefUtil.setBool(CDTPrefUtil.KEY_NOMNG, oldManage);
        }
      }
    }
  }
}
