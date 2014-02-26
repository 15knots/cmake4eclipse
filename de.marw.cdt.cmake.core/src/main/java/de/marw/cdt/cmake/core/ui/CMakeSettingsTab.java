/*******************************************************************************
 * Copyright (c) 2013 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core.ui;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IMultiConfiguration;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import de.marw.cdt.cmake.core.internal.settings.CMakePreferences;

/**
 * UI to control settings and preferences for cmake. This tab isd responsible
 * for storing its vaules.
 * 
 * @author Martin Weber
 */
public class CMakeSettingsTab extends AbstractCPropertyTab {

  // Widgets
  //1
  private Button b_useDefault;
//  private Combo c_builderType;
  private Text t_buildCmd;
  //5
  private Text t_dir;
  private Button b_dirWsp;
  private Button b_dirFile;
  private Button b_dirVars;
//  private Group group_dir;

  private IBuilder bldr;
  private IConfiguration icfg;
  private boolean canModify = true;
  /** the configuration we manage here. Initialized in {@link #updateData} */
  private ICConfigurationDescription cfgd;
  /**
   * the preferences associated with our configuration to manage. Initialized in
   * {@link #updateData}
   */
  private CMakePreferences prefs;

  @Override
  public void createControls(Composite parent) {
    super.createControls(parent);
    usercomp.setLayout(new GridLayout(1, false));

    // cmake executable group
    Group g1 = setupGroup(usercomp, "cmake executable", 3,
        GridData.FILL_HORIZONTAL);
//    setupLabel(g1, "Messages.BuilderSettingsTab_1", 1, GridData.BEGINNING);
//    c_builderType = new Combo(g1, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
//    setupControl(c_builderType, 2, GridData.FILL_HORIZONTAL);
//    c_builderType.add("Messages.BuilderSettingsTab_2");
//    c_builderType.add("Messages.BuilderSettingsTab_3");
//    c_builderType.addSelectionListener(new SelectionAdapter() {
//      @Override
//      public void widgetSelected(SelectionEvent event) {
////                          enableInternalBuilder(c_builderType.getSelectionIndex() == 1);
//        updateButtons();
//      }
//    });

    b_useDefault = setupCheck(g1, "Use cmake executable found on system &path",
        3, GridData.BEGINNING);

    setupLabel(g1, "cmake &command", 1, GridData.BEGINNING);
    t_buildCmd = setupBlock(g1, b_useDefault);
    t_buildCmd.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        if (!canModify)
          return;
        String buildCommand = t_buildCmd.getText().trim();
        if (!buildCommand.equals(prefs.getCommand())) {
          setCommand(buildCommand);
        }
      }
    });

//    // Build location group
//    group_dir = setupGroup(usercomp, Messages.BuilderSettingsTab_21, 2,
//        GridData.FILL_HORIZONTAL);
//    setupLabel(group_dir, Messages.BuilderSettingsTab_22, 1, GridData.BEGINNING);
//    t_dir = setupText(group_dir, 1, GridData.FILL_HORIZONTAL);
//    t_dir.addModifyListener(new ModifyListener() {
//      @Override
//      public void modifyText(ModifyEvent e) {
//        if (canModify)
//          setBuildPath(t_dir.getText());
//      }
//    });
//    Composite c = new Composite(group_dir, SWT.NONE);
//    setupControl(c, 2, GridData.FILL_HORIZONTAL);
//    GridLayout f = new GridLayout(4, false);
//    c.setLayout(f);
//    Label dummy = new Label(c, 0);
//    dummy.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//    b_dirWsp = setupBottomButton(c, WORKSPACEBUTTON_NAME);
//    b_dirFile = setupBottomButton(c, FILESYSTEMBUTTON_NAME);
//    b_dirVars = setupBottomButton(c, VARIABLESBUTTON_NAME);
  }

  /*-
   * @see org.eclipse.cdt.ui.newui.AbstractCPropertyTab#updateData(org.eclipse.cdt.core.settings.model.ICResourceDescription)
   */
  @Override
  protected void updateData(ICResourceDescription resd) {
    if (resd == null)
      return;

    cfgd = resd.getConfiguration();
    cfgd.get("de.marw.cdt.cmake.cfgdataprovider.id1");
    prefs = new CMakePreferences();
    // TODO (cfgd instanceof ICMultiItemsHolder)

    if (cfgd.isPreferenceConfiguration()) {
      // neccessary because CDT does not store out ICStorageElement in preferences
      prefs.loadFromPrefs();
    } else {
      try {
        ICStorageElement storage = cfgd.getStorage(
            CMakePreferences.CFG_STORAGE_ID, false);

        prefs.loadFromStorage(storage);
      } catch (CoreException ex) {
        // TODO Auto-generated catch block
        ex.printStackTrace();
      }
    }
    updateButtons();
  }

  /**
   * Invoked when project configuration changes??
   * 
   * @see org.eclipse.cdt.ui.newui.AbstractCPropertyTab#performApply(org.eclipse.cdt.core.settings.model.ICResourceDescription,
   *      org.eclipse.cdt.core.settings.model.ICResourceDescription)
   */
  @Override
  protected void performApply(ICResourceDescription src,
      ICResourceDescription dst) {
    ICConfigurationDescription srcCfg = src.getConfiguration();

    try {
      ICStorageElement srcEl = srcCfg.getStorage(
          CMakePreferences.CFG_STORAGE_ID, false);
      if (srcEl != null) {
        prefs = new CMakePreferences();
        prefs.loadFromStorage(srcEl);
        ICConfigurationDescription dstCfg = dst.getConfiguration();
        ICStorageElement dstEl = dstCfg.getStorage(
            CMakePreferences.CFG_STORAGE_ID, true);
        prefs.saveToStorage(dstEl);
      }
    } catch (CoreException ex) {
      // TODO Auto-generated catch block
      ex.printStackTrace();
    }
  }

  protected void performOK() {
    try {
      if (cfgd.isPreferenceConfiguration()) {
        // save as preferences...
        prefs.saveToPrefs();
// Unfortunately, this does NOT write our ICStorageElement :-(
//        CCorePlugin.getDefault().setPreferenceConfiguration(
//            ManagedBuildManager.CFG_DATA_PROVIDER_ID, cfgd);
      } else {
        // save as project settings..
        ICStorageElement storage = cfgd.getStorage(
            CMakePreferences.CFG_STORAGE_ID, true);
        prefs.saveToStorage(storage);
        final ICProjectDescriptionManager mgr = CCorePlugin.getDefault()
            .getProjectDescriptionManager();
        final ICProjectDescription projectDescription = cfgd
            .getProjectDescription();
        mgr.setProjectDescription(projectDescription.getProject(),
            projectDescription, true, null);
      }
    } catch (CoreException ex) {
      // TODO Auto-generated catch block
      ex.printStackTrace();
    }
    // TODO Auto-generated function stub

  }

  /*-
   * @see org.eclipse.cdt.ui.newui.AbstractCPropertyTab#performDefaults()
   */
  @Override
  protected void performDefaults() {
    prefs.reset();
    updateButtons();
  }

  /**
   * Sets up text + corresponding button. Checkbox can be implemented either by
   * Button or by TriButton
   */
  private Text setupBlock(Composite c, Control check) {
    Text t = setupText(c, 1, GridData.FILL_HORIZONTAL);
    Button b = setupButton(c, VARIABLESBUTTON_NAME, 1, GridData.END);
    b.setData(t); // to get know which text is affected
    t.setData(b); // to get know which button to enable/disable
    b.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        buttonVarPressed(event);
      }
    });
    if (check != null)
      check.setData(t);
    return t;
  }

  /*
   * Unified handler for "Variables" buttons
   */
  private void buttonVarPressed(SelectionEvent e) {
    Widget b = e.widget;
    if (b == null || b.getData() == null)
      return;
    if (b.getData() instanceof Text) {
      String x = null;
      if (b.equals(b_dirWsp)) {
        x = getWorkspaceDirDialog(usercomp.getShell(), EMPTY_STR);
        if (x != null)
          ((Text) b.getData()).setText(x);
      } else if (b.equals(b_dirFile)) {
        x = getFileSystemDirDialog(usercomp.getShell(), EMPTY_STR);
        if (x != null)
          ((Text) b.getData()).setText(x);
      } else {
        x = AbstractCPropertyTab.getVariableDialog(usercomp.getShell(),
            getResDesc().getConfiguration());
        if (x != null)
          ((Text) b.getData()).insert(x);
      }
    }
  }

  private void setCommand(String buildCommand) {
    if (cfgd instanceof IMultiConfiguration) {
      IConfiguration[] cfs = (IConfiguration[]) ((IMultiConfiguration) cfgd)
          .getItems();
      for (int i = 0; i < cfs.length; i++) {
//        IBuilder b = cfs[i].getEditableBuilder();
//        b.setCommand(buildCommand);
      }
    } else {
//      prefs.setCommand(buildCommand);
    }
  }

  // This page can be displayed for project and preferences
  @Override
  public boolean canBeVisible() {
    return page.isForProject() || page.isForPrefs();
  }

  /**
   * Overwritten to update sensivity of our checkboxes.
   */
  @Override
  public void checkPressed(SelectionEvent e) {
    checkPressed((Control) e.widget, true);
    updateButtons();
  }

  private void checkPressed(Control b, boolean needUpdate) {
    if (b == null)
      return;

    boolean val = false;
    if (b instanceof Button)
      val = ((Button) b).getSelection();

    if (b.getData() instanceof Text) {
      Text t = (Text) b.getData();
      if (b == b_useDefault) {
        val = !val;
      }
      t.setEnabled(val);
      if (t.getData() != null && t.getData() instanceof Control) {
        Control c = (Control) t.getData();
        c.setEnabled(val);
      }
    }
    // call may be used just to set text state above
    // in this case, settings update is not required
    if (!needUpdate)
      return;

//          if (b == b_useDefault) {
//                  setUseDefaultBuildCmd(!val);
//          } else if (b == b_expandVars) {
//                  if(bldr.canKeepEnvironmentVariablesInBuildfile())
//                          setKeepEnvironmentVariablesInBuildfile(!val);
//          }
  }

  /*-
   * @see org.eclipse.cdt.ui.newui.AbstractCPropertyTab#updateButtons()
   */
  @Override
  protected void updateButtons() {
//    bldr = icfg.getEditableBuilder();
    canModify = false; // avoid extra update from modifyListeners

//    AbstractCPropertyTab
//        .setTriSelection(b_useDefault, bldr.isDefaultBuildCmd());

    t_buildCmd.setText(prefs.getCommand());

    if (page.isMultiCfg()) {
//      group_dir.setVisible(false);
    } else {
//      group_dir.setVisible(true);
//      t_dir.setText(bldr.getBuildPath());
//      boolean mbOn = bldr.isManagedBuildOn();
//      t_dir.setEnabled(!mbOn);
//      b_dirVars.setEnabled(!mbOn);
//      b_dirWsp.setEnabled(!mbOn);
//      b_dirFile.setEnabled(!mbOn);
    }

//    b_useDefault.setEnabled(external);
//    t_buildCmd.setEnabled(external);
//    ((Control) t_buildCmd.getData()).setEnabled(external
//        & !b_useDefault.getSelection());

    canModify = true;
    // TODO Auto-generated function stub

  }

}
