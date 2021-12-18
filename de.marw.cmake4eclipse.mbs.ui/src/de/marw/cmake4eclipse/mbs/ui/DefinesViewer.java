/*******************************************************************************
 * Copyright (c) 2014-2019 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.ui;

import java.util.List;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import de.marw.cmake4eclipse.mbs.settings.CmakeDefine;
import de.marw.cmake4eclipse.mbs.ui.preferences.ViewerComparatorSortHandler;

/**
 * Displays a table for the Cmake defines. The created table will be displayed in a group, together with buttons to add,
 * edit and delete table entries.
 *
 * @author Martin Weber
 */
public class DefinesViewer {
  /** table column names */
  private static final String[] tableColumnNames = { "Name", "Type", "Value" };
  /** table column widths */
  private static final int[] tableColumnWidths = { 120, 80, 250 };

  /** the configuration description to use for variable selection dialog or {@code null} */
  private final ICConfigurationDescription cfgd;

  private TableViewer tableViewer;

  /**
   * @param parent
   * @param cfgd   the configuration description to use for variable selection dialog or {@code null}
   */
  public DefinesViewer(Composite parent, ICConfigurationDescription cfgd) {
    this.cfgd = cfgd;
    createEditor(parent);
  }

  /**
   * Gets the TableViewer for the Cmake-defines
   */
  public TableViewer getTableViewer() {
    return tableViewer;
  }

  /**
   * Sets the list of cmake defines that are displayed by the viewer.
   */
  public void setInput(List<CmakeDefine> defines) {
    // Get the content for the viewer, setInput will call getElements in the
    // contentProvider
    tableViewer.setInput(defines);
  }

  /**
   * Gets the list of cmake defines that are displayed by the viewer.
   */
  @SuppressWarnings("unchecked")
  public List<CmakeDefine> getInput() {
    return (List<CmakeDefine>) tableViewer.getInput();
  }

  private static TableViewer createViewer(Composite parent) {
    TableViewer viewer = new TableViewer(parent,
        SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);

    // Set the sorter for the table
    ViewerComparatorSortHandler comparator = new ViewerComparatorSortHandler(viewer);
    viewer.setComparator(comparator);

    createColumns(viewer, comparator);

    final Table table = viewer.getTable();
    table.setHeaderVisible(true);
    table.setLinesVisible(true);

    viewer.setContentProvider(new CmakeDefineTableContentProvider());
    viewer.setLabelProvider(new CmakeVariableLabelProvider());

    // Layout the viewer
    GridData gridData = new GridData();
    gridData.verticalAlignment = GridData.FILL;
    gridData.horizontalAlignment = GridData.FILL;
//    gridData.horizontalSpan = 2;
    gridData.grabExcessHorizontalSpace = true;
    gridData.grabExcessVerticalSpace = true;
    viewer.getControl().setLayoutData(gridData);
    return viewer;
  }

  /**
   * Creates the columns for the table.
   *
   * @param viewer
   */
  private static void createColumns(final TableViewer viewer, SelectionListener sortSelection) {
    for (int i = 0; i < tableColumnNames.length; i++) {
      createTableViewerColumn(viewer, tableColumnNames[i], tableColumnWidths[i], sortSelection);
    }
  }

  /**
   * Creates a table viewer column for the table.
   */
  private static TableViewerColumn createTableViewerColumn(final TableViewer viewer, String title, int colWidth,
      SelectionListener sortSelection) {
    final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
    final TableColumn column = viewerColumn.getColumn();
    column.setText(title);
    column.setWidth(colWidth);
    column.setResizable(true);
    column.setMoveable(true);
    if (sortSelection != null) {
      column.addSelectionListener(sortSelection);
    }
    return viewerColumn;
  }

  /**
   * Creates the control to add/delete/edit cmake-variables to define.
   */
  private void createEditor(Composite parent) {
    final Group gr = WidgetHelper.createGroup(parent, SWT.FILL, 2, "CMake cache entries to &add (-D)", 2);

    tableViewer = createViewer(gr);

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

    // Buttons, vertically stacked
    Composite editButtons = new Composite(gr, SWT.NONE);
    editButtons.setLayoutData(new GridData(SWT.CENTER, SWT.BEGINNING, false, false));
    editButtons.setLayout(new GridLayout(1, false));
    // editButtons.setBackground(BACKGROUND_FOR_USER_VAR);

    Button buttonDefineAdd = WidgetHelper.createButton(editButtons, AbstractCPropertyTab.ADD_STR, true);
    final Button buttonDefineEdit = WidgetHelper.createButton(editButtons, AbstractCPropertyTab.EDIT_STR, false);
    final Button buttonDefineDel = WidgetHelper.createButton(editButtons, AbstractCPropertyTab.DEL_STR, false);

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
        // update button sensitivity..
        int sels = ((IStructuredSelection) event.getSelection()).size();
        boolean canEdit = (sels == 1);
        boolean canDel = (sels >= 1);
        buttonDefineEdit.setEnabled(canEdit);
        buttonDefineDel.setEnabled(canDel);
      }
    });

  }

  private void handleDefineAddButton(TableViewer tableViewer) {
    final Shell shell = tableViewer.getControl().getShell();
    AddCmakeDefineDialog dlg = new AddCmakeDefineDialog(shell, cfgd, null);
    if (dlg.open() == Dialog.OK) {
      CmakeDefine cmakeDefine = dlg.getCmakeDefine();
      @SuppressWarnings("unchecked")
      List<CmakeDefine> defines = (List<CmakeDefine>) tableViewer.getInput();
      defines.add(cmakeDefine);
      tableViewer.add(cmakeDefine); // updates the display
    }
  }

  private void handleDefineEditButton(TableViewer tableViewer) {
    final IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
    if (selection.size() == 1) {
      Object cmakeDefine = selection.getFirstElement();
      // edit the selected variable in-place..
      final Shell shell = tableViewer.getControl().getShell();
      AddCmakeDefineDialog dlg = new AddCmakeDefineDialog(shell, cfgd, (CmakeDefine) cmakeDefine);
      if (dlg.open() == Dialog.OK) {
        tableViewer.update(cmakeDefine, null); // updates the display
      }
    }
  }

  private void handleDefineDelButton(TableViewer tableViewer) {
    final IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
    final Shell shell = tableViewer.getControl().getShell();
    if (MessageDialog.openQuestion(shell, "CMake Cache Entry deletion confirmation",
        "Are you sure to delete the selected CMake Cache Entries?")) {
      @SuppressWarnings("unchecked")
      List<CmakeDefine> defines = (List<CmakeDefine>) tableViewer.getInput();
      defines.removeAll(selection.toList());
      tableViewer.remove(selection.toArray());// updates the display
    }
  }

  ////////////////////////////////////////////////////////////////////
  // inner classes
  ////////////////////////////////////////////////////////////////////

  /**
   * Converts the list of CmakeDefine object.
   *
   * @author Martin Weber
   */
  private static class CmakeDefineTableContentProvider implements IStructuredContentProvider {
    @Override
    public Object[] getElements(Object inputElement) {
      @SuppressWarnings("unchecked")
      final List<CmakeDefine> elems = (List<CmakeDefine>) inputElement;
      return elems.toArray();
    }

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
  } // CmakeDefineTableContentProvider

  private static class CmakeVariableLabelProvider extends BaseLabelProvider implements ITableLabelProvider {

    // interface ITableLabelProvider
    @Override
    public Image getColumnImage(Object element, int columnIndex) {
      return null;
    }

    // interface ITableLabelProvider
    @Override
    public String getColumnText(Object element, int columnIndex) {
      final CmakeDefine var = (CmakeDefine) element;
      switch (columnIndex) {
      case 0:
        return var.getName();
      case 1:
        return var.getType().name();
      case 2:
        return var.getValue();
      }
      return "";
    }
  } // CmakeVariableLabelProvider
}
