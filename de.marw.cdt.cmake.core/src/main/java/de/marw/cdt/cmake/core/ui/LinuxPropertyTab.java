/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core.ui;

import java.util.ArrayList;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICMultiItemsHolder;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IMultiConfiguration;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import de.marw.cdt.cmake.core.internal.CMakePreferences;
import de.marw.cdt.cmake.core.internal.CmakeDefine;
import de.marw.cdt.cmake.core.internal.CmakeVariableType;
import de.marw.cdt.cmake.core.ui.util.BuildVariableSelectionDialog;
import de.marw.cdt.cmake.core.ui.util.FileSystemFileSelectionDialog;
import de.marw.cdt.cmake.core.ui.util.SelectionDialog;

/**
 * UI to control host OS specific project properties and preferences for cmake.
 * Host OS specific properties override generic properties and get automatically
 * applied if the plugin is running under that operating system.<br>
 * This tab is responsible for storing its values.<br>
 *
 * @author Martin Weber
 */
public class LinuxPropertyTab extends AbstractCPropertyTab {

  private boolean canModify = true;
  /** the configuration we manage here. Initialized in {@link #updateData} */
  private ICConfigurationDescription cfgd;
  /**
   * the preferences associated with our configuration to manage. Initialized in
   * {@link #updateData}
   */
  private CMakePreferences prefs;

  // Widgets
  /** 'use exec from path' checkbox */
  private Button b_cmdFromPath;
  /** cmake executable */
  private Text t_cmd;

  /** Combo that shows the generator names for cmake */
  private Combo c_generator;
  private ArrayList<CmakeDefine> defines;

  /**
   *
   */
  public LinuxPropertyTab() {
    super();
    defines = new ArrayList<CmakeDefine>();
    // TODO get values from project props..
    defines.add(new CmakeDefine("test"));
    defines.add(new CmakeDefine("KEKS", CmakeVariableType.BOOL, "true"));

  }

  @Override
  public void createControls(Composite parent) {
    super.createControls(parent);
    usercomp.setLayout(new GridLayout(1, false));
    GridLayout layout;

    // cmake executable group...
    {
      Group gr = setupGroup(usercomp, "cmake executable", 1,
          GridData.FILL_HORIZONTAL);

      b_cmdFromPath = setupCheck(gr,
          "Use cmake executable found on &system path", 1, SWT.BEGINNING);
      // the control that is en-/disabled by the b_cmdFromPath checkbox..
      Composite c = new Composite(gr, SWT.NONE);
      c.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      b_cmdFromPath.setData(c); // to know which control to enable/disable
//      c.setBackground(BACKGROUND_FOR_USER_VAR);

      layout = new GridLayout(3, false);
      layout.marginHeight = 0;
      layout.marginWidth = 0;
      c.setLayout(layout);

      Label label = setupLabel(c, "&Cmake", 1, SWT.BEGINNING);
      label.setToolTipText(ADD_STR);
      t_cmd = setupText(c, 2, GridData.FILL_HORIZONTAL);

      t_cmd.addModifyListener(new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {
          if (!canModify)
            return;
          String buildCommand = t_cmd.getText().trim();
          if (!buildCommand.equals(prefs.getCommand())) {
            setCommand(buildCommand);
          }
        }
      });

      // "Filesystem", "Variables" dialog launcher buttons...
      Composite buttonBar = new Composite(c, SWT.NONE);
      {
        buttonBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false,
            3, 1));
        layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        buttonBar.setLayout(layout);
      }
      setupDialogLauncherButton(buttonBar, new FileSystemFileSelectionDialog(
          null), t_cmd);
      setupDialogLauncherButton(buttonBar, new BuildVariableSelectionDialog(
          cfgd), t_cmd);
    }
    // makefile generator combo...
    {
      setupLabel(usercomp, "Buildscript &generator (-G)", 1, SWT.BEGINNING);
      String generatorNames[] = getAvailableGenerators();
      c_generator = new Combo(usercomp, SWT.READ_ONLY | SWT.DROP_DOWN
          | SWT.BORDER);
      setupControl(c_generator, 1, GridData.FILL_HORIZONTAL);
//      c_generator.setEnabled(page.isForProject());

      for (String gen : generatorNames) {
        c_generator.add(gen);
      }
      c_generator.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
          updateButtons();
        }
      });
    }
    // cmake variables table...
    {
      setupCmakeDefinesEditor(usercomp);
    }
  }

  /**
   * Creates the control to add/delete/edit cmake-variables to define.
   */
  private void setupCmakeDefinesEditor(Composite usercomp) {
    Group gr = setupGroup(usercomp, "Cmake cache entries to add (-D)", 2,
        GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);

    final DefinesViewer definesViewer = new DefinesViewer(gr);
definesViewer.setInput(defines);

    final TableViewer tableViewer = definesViewer.getTableViewer();
    // let double click trigger the edit dialog
    tableViewer.addDoubleClickListener(new IDoubleClickListener() {

      @Override
      public void doubleClick(DoubleClickEvent event) {
          handleDefineEditButton(tableViewer);
      }
    });
    // let DEL key trigger the delete dialog
    tableViewer.getTable().addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.keyCode == SWT.DEL) {
          handleDefineDelButton(tableViewer);
        }
      }
    });

    // Buttons, vertically stacked
    Composite editButtons = new Composite(gr, SWT.NONE);
    editButtons.setLayoutData(new GridData(SWT.CENTER, SWT.BEGINNING, false,
        false));
    editButtons.setLayout(new GridLayout(1, false));
//    editButtons.setBackground(BACKGROUND_FOR_USER_VAR);

    Button buttonDefineAdd = createButton(editButtons,
        AbstractCPropertyTab.ADD_STR, true);
    final Button buttonDefineEdit = createButton(editButtons,
        AbstractCPropertyTab.EDIT_STR, false);
    final Button buttonDefineDel = createButton(editButtons,
        AbstractCPropertyTab.DEL_STR, false);

    // wire button actions...
    buttonDefineAdd.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        handleDefineAddButton(tableViewer);
      }
    });
    buttonDefineEdit.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        handleDefineEditButton(tableViewer);
      }
    });
    buttonDefineDel.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        handleDefineDelButton(tableViewer);
      }
    });

    // enable button sensitivity based on table selection
    tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        // udate button sensitivity..
        int sels = ((IStructuredSelection) event.getSelection()).size();
        boolean canEdit = (sels == 1);
        boolean canDel = (sels >= 1);
        buttonDefineEdit.setEnabled(canEdit);
        buttonDefineDel.setEnabled(canDel);
      }
    });

  }

  /**
   * @param parent
   * @param text
   *        button text
   * @param enabled
   *        whether the button should be enabled
   */
  private Button createButton(Composite parent, String text, boolean enabled) {
    Button button = new Button(parent, SWT.PUSH);
    button.setText(text);
    if (!enabled)
      button.setEnabled(false);
    GridData gdb = new GridData(SWT.CENTER, SWT.CENTER, false, false);
//    gdb.horizontalAlignment = SWT.FILL;
//    gdb.minimumWidth = width;
    button.setLayoutData(gdb);
    return button;
  }

  private void handleDefineAddButton(TableViewer tableViewer) {
    AddCmakeDefineDialog dlg = new AddCmakeDefineDialog(usercomp.getShell(),
        null);
    if (dlg.open() == Dialog.OK) {
      CmakeDefine cmakeDefine = dlg.getCmakeDefine();
      defines.add(cmakeDefine);
      tableViewer.add(cmakeDefine);
//      tableViewer.refresh(cmakeDefine,false); // update the display
    }
  }

  private void handleDefineEditButton(TableViewer tableViewer) {
    final IStructuredSelection selection = (IStructuredSelection) tableViewer
        .getSelection();
    if (selection.size() == 1) {
      Object cmakeDefine = selection.getFirstElement();
      // edit the selected variable in-place..
      AddCmakeDefineDialog dlg = new AddCmakeDefineDialog(usercomp.getShell(),
          (CmakeDefine) cmakeDefine);
      if (dlg.open() == Dialog.OK) {
        tableViewer.update(cmakeDefine,null); // update the display
      }
    }
  }

  private void handleDefineDelButton(TableViewer tableViewer) {
//    ICdtVariable macros[] = getSelectedUserMacros();
//    if(macros != null && macros.length > 0){
            if(MessageDialog.openQuestion(usercomp.getShell(),
                            "Cmake-Define deletion confirmation",
                            "Are you sure to delete the selected Cmake-defines?")){
                            if (cfgd != null) {
                                    if (page.isMultiCfg() && cfgd instanceof ICMultiItemsHolder) {
                                            ICConfigurationDescription[] cfs = (ICConfigurationDescription[])((ICMultiItemsHolder)cfgd).getItems();
                                            for (int k=0; k<cfs.length; k++)
                                                    fUserSup.deleteMacro(macros[i].getName(), cfs[k]);
                                            replaceMacros();
                                    }
                                    else
                                      defines.remove();
                                            fUserSup.deleteMacro(macros[i].getName(), cfgd);
                            }
                    }
            }
    }
  }

  /**
   * Get all sensible choices for cmake's '-G' option on this platform. The
   * returned array should not include generators for IDE project files, such as
   * "Eclipse CDT4 - Unix Makefiles".
   *
   * @return an array of non-empty strings, where each string must be an valid
   *         argument for cmake.
   */
  protected String[] getAvailableGenerators() {
    return new String[] { "Unix Makefiles" };
  }

  /**
   * Creates a button that fires up a selection dialog.
   *
   * @param buttonParent
   * @param selectionDialog
   *        the dialog to fire up when the button is pressed
   * @param receivingWidget
   *        the Text widget wher to append text
   */
  private Button setupDialogLauncherButton(final Composite buttonParent,
      final SelectionDialog selectionDialog, Text receivingWidget) {
    if (buttonParent == null || selectionDialog == null
        || receivingWidget == null) {
      throw new NullPointerException();
    }

    final Button b = setupButton(buttonParent,
        selectionDialog.getLauncherButtonText(), 1, SWT.CENTER);
    b.setData(receivingWidget); // to get know which Text is affected

    b.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent evt) {
        Widget b = evt.widget;
        if (b == null)
          return;
        if (b.getData() instanceof Text) {
          // launch dialog, insert input text into receiving Widget..
          String x = selectionDialog.getTextFromDialog(buttonParent.getShell());
          if (x != null)
            ((Text) b.getData()).insert(x);
        }
      }
    });
    return b;
  }

  /*-
   * @see org.eclipse.cdt.ui.newui.AbstractCPropertyTab#updateData(org.eclipse.cdt.core.settings.model.ICResourceDescription)
   */
  @Override
  protected void updateData(ICResourceDescription resd) {
    if (resd == null)
      return;

    cfgd = resd.getConfiguration();
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

  private void setCommand(String buildCommand) {
    if (cfgd instanceof IMultiConfiguration) {
      IConfiguration[] cfs = (IConfiguration[]) ((IMultiConfiguration) cfgd)
          .getItems();
      for (int i = 0; i < cfs.length; i++) {
//        IBuilder b = cfs[i].getEditableBuilder();
//        b.setCommand(buildCommand);
      }
    } else {
      prefs.setCommand(buildCommand);
    }
  }

  // This page can be displayed for project and preferences
  @Override
  public boolean canBeVisible() {
    return page.isForProject();// || page.isForPrefs();
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
    if (b instanceof Button) {
      val = ((Button) b).getSelection();
      if (b == b_cmdFromPath) {
        // 'use exec from path' checkbox
        val = !val;
      }
    }
    // adjust sensitivity...
    final Object slave_control = b.getData();
    if (slave_control instanceof Control) {
      // enable/disable the associated Control
      ((Control) slave_control).setEnabled(val);
    }
  }

  /*-
   * @see org.eclipse.cdt.ui.newui.AbstractCPropertyTab#updateButtons()
   */
  @Override
  protected void updateButtons() {
//    bldr = icfg.getEditableBuilder();
    canModify = false; // avoid extra update from modifyListeners

    if (page.isMultiCfg()) {
//      group_cmd.setVisible(false);
    } else {
      t_cmd.setText(prefs.getCommand());
      // TODO
//      b_cmdFromPath.setEnabled(canModify);
//
//      final String[] gens = c_generator.getItems();
//      for (int i = 0; i < gens.length; i++) {
//        if (gens[i].equals(prefs.linux.getGenerator())) {
//          c_generator.select(i);
//          break;
//        }
//      }
    }
    canModify = true;
    // TODO Auto-generated function stub

  }

}
