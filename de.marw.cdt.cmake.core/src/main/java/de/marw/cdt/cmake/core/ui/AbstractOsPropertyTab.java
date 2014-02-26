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
import org.eclipse.cdt.core.settings.model.ICMultiConfigDescription;
import org.eclipse.cdt.core.settings.model.ICMultiItemsHolder;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import de.marw.cdt.cmake.core.CMakePlugin;
import de.marw.cdt.cmake.core.internal.settings.AbstractOsPreferences;
import de.marw.cdt.cmake.core.internal.settings.CMakePreferences;
import de.marw.cdt.cmake.core.internal.settings.CmakeDefine;
import de.marw.cdt.cmake.core.internal.settings.CmakeUnDefine;

/**
 * Generic UI to control host OS specific project properties and preferences for
 * {@code cmake}. Host OS specific properties override generic properties when
 * passed to {@code cmake} and get automatically applied if this plugin detects
 * it is running under that operating system.<br>
 * This tab and any subclass is responsible for storing its values.<br>
 *
 * @author Martin Weber
 * @param <P>
 *        the type that holds the OS specific properties.
 */
public abstract class AbstractOsPropertyTab<P extends AbstractOsPreferences>
    extends AbstractCPropertyTab {

  /**  */
  private static final ILog log = CMakePlugin.getDefault().getLog();

  private boolean canModify = true;

  /** the configuration we manage here. Initialized in {@link #updateData} */
  private ICConfigurationDescription cfgd;
  /**
   * the preferences associated with our configuration to manage. Initialized in
   * {@link #updateData}
   */
  private P prefs;

  /** 'use exec from path' checkbox */
  private Button b_cmdFromPath;
  /** cmake executable */
  private Text t_cmd;
  /** Combo that shows the generator names for cmake */
  private Combo c_generator;
  /** the table showing the cmake defines */
  private DefinesViewer definesViewer;
  /** the table showing the cmake undefines */
  private UnDefinesViewer undefinesViewer;

  /**
   *
   */
  public AbstractOsPropertyTab() {
  }

  /**
   * Gets the OS specific preferences from the specified generic preferences.
   *
   * @return the OS specific preferences, never {@code null}.
   */
  protected abstract P getOsPreferences(CMakePreferences prefs);

  /**
   * Gets all sensible choices for cmake's '-G' option on this platform. The
   * returned array should not include generators for IDE project files, such as
   * "Eclipse CDT4 - Unix Makefiles".
   *
   * @return an array of non-empty strings, where each string must be an valid
   *         argument for cmake.
   */
  protected abstract String[] getAvailableGenerators();

  @Override
  public void createControls(Composite parent) {
    super.createControls(parent);
    usercomp.setLayout(new GridLayout(2, false));
    GridLayout layout;

    // cmake executable group...
    {
      Group gr = createGroup(usercomp, 2, "Cmake Executable", SWT.FILL, 2);

      b_cmdFromPath = createCheckbox(gr,
          "Use cmake executable found on &system path", SWT.BEGINNING, 2);

      setupLabel(gr, "&File", 1, SWT.BEGINNING);

      t_cmd = setupText(gr, 1, GridData.FILL_HORIZONTAL);

      // "Filesystem", "Variables" dialog launcher buttons...
      Composite buttonBar = new Composite(gr, SWT.NONE);
      {
        buttonBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false,
            3, 1));
        layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        buttonBar.setLayout(layout);
        //        buttonBar.setBackground(BACKGROUND_FOR_USER_VAR);
      }
      final Button btnBrowseFiles = createButton(buttonBar, "File System...",
          true);
      btnBrowseFiles.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          FileDialog dialog = new FileDialog(t_cmd.getShell());
          String text = dialog.open();
          if (text != null)
            t_cmd.insert(text);
        }
      });

      // control sensitivity...
      b_cmdFromPath.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
          final Button btn = (Button) event.widget;
          // adjust sensitivity...
          boolean val = !btn.getSelection();
          t_cmd.setEnabled(val);
          btnBrowseFiles.setEnabled(val);
        }
      });

    } // cmake executable group

    // makefile generator combo...
    {
      setupLabel(usercomp, "Buildscript &generator (-G):", 1, SWT.BEGINNING);
      c_generator = new Combo(usercomp, SWT.READ_ONLY | SWT.DROP_DOWN
          | SWT.BORDER);
      final GridData gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false,
          1, 1);
      gd.widthHint = 200;
      c_generator.setLayoutData(gd);

      String generatorNames[] = getAvailableGenerators();
      c_generator.setItems(generatorNames);
    } // makefile generator combo

    // cmake defines table...
    createCmakeDefinesEditor(usercomp);
    // cmake undefines table...
    createCmakeUnDefinesEditor(usercomp);
  }

  /**
   * Creates the control to add/delete/edit cmake-variables to define.
   */
  private void createCmakeDefinesEditor(Composite parent) {
    final Group gr = createGroup(usercomp, 2,
        "Cmake cache entries to define (-D)", SWT.FILL, 2);

    definesViewer = new DefinesViewer(gr);

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
        } else if (e.keyCode == SWT.INSERT) {
          handleDefineAddButton(tableViewer);
        }
      }
    });
    // TODO multicfgtest
//    tableViewer.getTable().setEnabled(false);

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
        // disable edit & del when multiple configurations are affected
        boolean isMulti = (cfgd instanceof ICMultiConfigDescription) ? true
            : false;

        // update button sensitivity..
        int sels = ((IStructuredSelection) event.getSelection()).size();
        boolean canEdit = !isMulti && (sels == 1);
        boolean canDel = !isMulti && (sels >= 1);
        buttonDefineEdit.setEnabled(canEdit);
        buttonDefineDel.setEnabled(canDel);
      }
    });
  }

  /**
   * Creates the control to add/delete/edit cmake-variables to undefine.
   */
  private void createCmakeUnDefinesEditor(Composite parent) {
    final Group gr = createGroup(usercomp, 2,
        "Cmake cache entries to undefine (-U)", SWT.FILL, 2);

    undefinesViewer = new UnDefinesViewer(gr);

    final TableViewer tableViewer = undefinesViewer.getTableViewer();
    // let double click trigger the edit dialog
    tableViewer.addDoubleClickListener(new IDoubleClickListener() {

      @Override
      public void doubleClick(DoubleClickEvent event) {
        handleUnDefineEditButton(tableViewer);
      }
    });
    // let DEL key trigger the delete dialog
    tableViewer.getTable().addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.keyCode == SWT.DEL) {
          handleUnDefineDelButton(tableViewer);
        } else if (e.keyCode == SWT.INSERT) {
          handleUnDefineAddButton(tableViewer);
        }
      }
    });

    // Buttons, vertically stacked
    Composite editButtons = new Composite(gr, SWT.NONE);
    editButtons.setLayoutData(new GridData(SWT.CENTER, SWT.BEGINNING, false,
        false));
    editButtons.setLayout(new GridLayout(1, false));

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
        handleUnDefineAddButton(tableViewer);
      }
    });
    buttonDefineEdit.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        handleUnDefineEditButton(tableViewer);
      }
    });
    buttonDefineDel.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        handleUnDefineDelButton(tableViewer);
      }
    });

    // enable button sensitivity based on table selection
    tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        // disable edit & del when multiple configurations are affected
        boolean isMulti = (cfgd instanceof ICMultiConfigDescription) ? true
            : false;

        // update button sensitivity..
        int sels = ((IStructuredSelection) event.getSelection()).size();
        boolean canEdit = !isMulti && (sels == 1);
        boolean canDel = !isMulti && (sels >= 1);
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
    GridData gd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
    //    gd.minimumWidth = width;
    button.setLayoutData(gd);
    return button;
  }

  /**
   * Creates a checkbox.
   *
   * @param parent
   * @param text
   *        text to display on checkbox
   * @param horizontalAlignment
   *        how control will be positioned horizontally within a cell, one of:
   *        SWT.BEGINNING (or SWT.LEFT), SWT.CENTER, SWT.END (or SWT.RIGHT), or
   *        SWT.FILL
   */
  private Button createCheckbox(Composite parent, String text,
      int horizontalAlignment, int horizontalSpan) {
    Button b = new Button(parent, SWT.CHECK);
    b.setText(text);
    GridData gd = new GridData(horizontalAlignment, SWT.CENTER, false, false);
    gd.horizontalSpan = horizontalSpan;
    b.setLayoutData(gd);
    return b;
  }

  private Group createGroup(Composite parent, int numColumns, String text,
      int horizontalAlignment, int horizontalSpan) {
    Group gr = new Group(usercomp, SWT.NONE);
    gr.setLayout(new GridLayout(numColumns, false));
    gr.setText(text);
    GridData gd = new GridData(horizontalAlignment, SWT.CENTER, true, false);
    gd.horizontalSpan = horizontalSpan;
    gr.setLayoutData(gd);
    return gr;
  }

  private void handleDefineAddButton(TableViewer tableViewer) {
    AddCmakeDefineDialog dlg = new AddCmakeDefineDialog(usercomp.getShell(),
        null);
    if (dlg.open() == Dialog.OK) {
      CmakeDefine cmakeDefine = dlg.getCmakeDefine();
      @SuppressWarnings("unchecked")
      ArrayList<CmakeDefine> defines = (ArrayList<CmakeDefine>) tableViewer
          .getInput();
      defines.add(cmakeDefine);
      tableViewer.add(cmakeDefine); // updates the display
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
        tableViewer.update(cmakeDefine, null); // updates the display
      }
    }
  }

  private void handleDefineDelButton(TableViewer tableViewer) {
    final IStructuredSelection selection = (IStructuredSelection) tableViewer
        .getSelection();
    if (MessageDialog.openQuestion(usercomp.getShell(),
        "Cmake-Define deletion confirmation",
        "Are you sure to delete the selected Cmake-defines?")) {
      if (cfgd != null) {
        if (cfgd instanceof ICMultiItemsHolder) {
          ICConfigurationDescription[] cfs = (ICConfigurationDescription[]) ((ICMultiItemsHolder) cfgd)
              .getItems();
          for (int k = 0; k < cfs.length; k++) {
            // TODO                                                    fUserSup.deleteMacro(macros[i].getName(), cfs[k]);
          }
        } else {
          @SuppressWarnings("unchecked")
          ArrayList<CmakeDefine> defines = (ArrayList<CmakeDefine>) tableViewer
              .getInput();
          defines.removeAll(selection.toList());
          tableViewer.remove(selection.toArray());// updates the display
        }
      }
    }
  }

  private void handleUnDefineAddButton(TableViewer tableViewer) {
    AddCmakeUndefineDialog dlg = new AddCmakeUndefineDialog(
        usercomp.getShell(), null);
    if (dlg.open() == Dialog.OK) {
      CmakeUnDefine cmakeDefine = dlg.getCmakeUndefine();
      @SuppressWarnings("unchecked")
      ArrayList<CmakeUnDefine> undefines = (ArrayList<CmakeUnDefine>) tableViewer
          .getInput();
      undefines.add(cmakeDefine);
      tableViewer.add(cmakeDefine); // updates the display
    }
  }

  private void handleUnDefineEditButton(TableViewer tableViewer) {
    final IStructuredSelection selection = (IStructuredSelection) tableViewer
        .getSelection();
    if (selection.size() == 1) {
      Object cmakeDefine = selection.getFirstElement();
      // edit the selected variable in-place..
      AddCmakeUndefineDialog dlg = new AddCmakeUndefineDialog(
          usercomp.getShell(), (CmakeUnDefine) cmakeDefine);
      if (dlg.open() == Dialog.OK) {
        tableViewer.update(cmakeDefine, null); // updates the display
      }
    }
  }

  private void handleUnDefineDelButton(TableViewer tableViewer) {
    final IStructuredSelection selection = (IStructuredSelection) tableViewer
        .getSelection();
    if (MessageDialog.openQuestion(usercomp.getShell(),
        "Cmake-Undefine deletion confirmation",
        "Are you sure to delete the selected Cmake-undefines?")) {
      @SuppressWarnings("unchecked")
      ArrayList<String> undefines = (ArrayList<String>) tableViewer.getInput();
      undefines.removeAll(selection.toList());
      tableViewer.remove(selection.toArray());// updates the display
    }
  }

  /**
   * Invoked when this tab is going to display. Call that 'initialize'!
   */
  @Override
  protected void updateData(ICResourceDescription resd) {
    if (resd == null)
      return;

    cfgd = resd.getConfiguration();
    CMakePreferences allPrefs = new CMakePreferences();
    if (cfgd instanceof ICMultiConfigDescription) {
      ICConfigurationDescription[] cfs = (ICConfigurationDescription[]) ((ICMultiConfigDescription) cfgd)
          .getItems();
      for (int i = 0; i < cfs.length; i++) {
// TODO
      }
      // disable table viewers when multiple configurations are affected
    }
    try {
      ICStorageElement storage = cfgd.getStorage(
          CMakePreferences.CFG_STORAGE_ID, false);
//      allPrefs.loadFromStorage(storage);
      prefs = getOsPreferences(allPrefs);
      prefs.loadFromStorage(storage);
    } catch (CoreException ex) {
      log.log(new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID, null,ex));
    }
    updateDisplay();
  }

  /**
   * Updates displayed values according to the preferences edited by this tab.
   */
  private void updateDisplay() {
    canModify = false; // avoid extra update from modifyListeners
    try {
      if (cfgd instanceof ICMultiConfigDescription) {
        // TODO multicfg test
        definesViewer.getTableViewer().getTable().setEnabled(false);
        //      group_cmd.setVisible(false);
      } else {
        t_cmd.setText(prefs.getCommand());
        b_cmdFromPath.setSelection(prefs.getUseDefaultCommand());

        String generatorName = prefs.getGeneratorName();
        int idx = c_generator.indexOf(generatorName);
        if (idx >= 0)
          c_generator.select(idx);

        definesViewer.setInput(prefs.getDefines());
        undefinesViewer.setInput(prefs.getUndefines());
      }
    } finally {
      canModify = true;
    }
  }

  /**
   * Invoked when project configuration changes?? At least when apply button is
   * pressed.
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
        CMakePreferences prefs = new CMakePreferences();
        prefs.loadFromStorage(srcEl);
        ICConfigurationDescription dstCfg = dst.getConfiguration();
        ICStorageElement dstEl = dstCfg.getStorage(
            CMakePreferences.CFG_STORAGE_ID, true);
        prefs.saveToStorage(dstEl);
      }
    } catch (CoreException ex) {
      log.log(new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID, null,ex));
    }
  }

  @Override
  protected void performOK() {
    try {
      int idx = c_generator.getSelectionIndex();
      if (idx >= 0) {
        String gen = c_generator.getItem(idx);
        prefs.setGeneratorName(gen);
      }
      String command = t_cmd.getText().trim();
      prefs.setCommand(command);
      // NB: defines & undefines are modified by the widget listeners directly

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
    } catch (CoreException ex) {
      log.log(new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID, null,ex));
    }
  }

  @Override
  protected void performDefaults() {
    prefs.reset();
    updateDisplay();
  }

  @Override
  public boolean canBeVisible() {
    return page.isForProject();
  }

  @Override
  protected void updateButtons() {
    // never called from superclass, but abstract :-)
  }
}