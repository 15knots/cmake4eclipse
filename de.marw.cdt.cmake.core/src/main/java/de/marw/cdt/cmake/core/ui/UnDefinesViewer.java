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
import java.util.List;

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
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import de.marw.cdt.cmake.core.internal.settings.CmakeUnDefine;

/**
 * Displays a table for the Cmake undefines. The created table will be displayed
 * in a group, together with buttons to add, edit and delete table entries.
 * 
 * @author Martin Weber
 */
/* package */class UnDefinesViewer {
  /** table column names */
  private static final String[] tableColumnNames = { "Name" };
  /** table column widths */
  private static final int[] tableColumnWidths = { 120 };

  private TableViewer tableViewer;
  private MyViewerComparator comparator;

  /**
   * @param parent
   */
  public UnDefinesViewer(Composite parent) {
    createEditor(parent);
    // Set the sorter for the table
    comparator = new MyViewerComparator();
    tableViewer.setComparator(comparator);
  }

  /**
   * Gets the TableViewer for the Cmake-undefines
   */
  public TableViewer getTableViewer() {
    return tableViewer;
  }

  /**
   * Sets the list of cmake undefines that are displayed by the viewer.
   */
  public void setInput(List<CmakeUnDefine> list) {
    // Get the content for the viewer, setInput will call getElements in the
    // contentProvider
    tableViewer.setInput(list);
  }

  private TableViewer createViewer(Composite parent) {
    TableViewer viewer = new TableViewer(parent, SWT.BORDER | SWT.H_SCROLL
        | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);

    createColumns(parent, viewer);

    final Table table = viewer.getTable();
    table.setHeaderVisible(true);
    table.setLinesVisible(true);

    viewer.setContentProvider(new CmakeUnDefineTableContentProvider());
    viewer.setLabelProvider(new CmakeVariableLabelProvider());

//    // make the selection available to other views
//    getSite().setSelectionProvider(tableViewer);

    // Layout the viewer
    GridData gridData = new GridData();
    gridData.verticalAlignment = GridData.FILL;
//    gridData.horizontalSpan = 2;
    gridData.grabExcessHorizontalSpace = true;
    gridData.grabExcessVerticalSpace = true;
    gridData.horizontalAlignment = GridData.FILL;
    viewer.getControl().setLayoutData(gridData);
    return viewer;
  }

  /**
   * Creates the columns for the table.
   * 
   * @param parent
   * @param viewer
   */
  private void createColumns(final Composite parent, final TableViewer viewer) {
    for (int i = 0; i < tableColumnNames.length; i++) {
      createTableViewerColumn(viewer, tableColumnNames[i],
          tableColumnWidths[i], i);
    }
  }

  /**
   * Creates a table viewer column for the table.
   */
  private TableViewerColumn createTableViewerColumn(final TableViewer viewer,
      String title, int colWidth, final int colNumber) {
    final TableViewerColumn viewerColumn = new TableViewerColumn(viewer,
        SWT.NONE);
    final TableColumn column = viewerColumn.getColumn();
    column.setText(title);
    column.setWidth(colWidth);
    column.setResizable(true);
    column.setMoveable(true);
    column.addSelectionListener(createSelectionAdapter(column, colNumber));
    return viewerColumn;
  }

  /**
   * Creates a selection adapter that changes the sorting order and sorting
   * column of the table.
   */
  private SelectionAdapter createSelectionAdapter(final TableColumn column,
      final int index) {
    SelectionAdapter selectionAdapter = new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        comparator.setSortColumn(index);
        int dir = comparator.getSortDirection();
        tableViewer.getTable().setSortDirection(dir);
        tableViewer.getTable().setSortColumn(column);
        tableViewer.refresh();
      }
    };
    return selectionAdapter;
  }

  /**
   * Creates the control to add/delete/edit cmake-variables to undefine.
   */
  private void createEditor(Composite parent) {
    final Group gr = WidgetHelper.createGroup(parent, SWT.FILL, 2,
        "CMake cache entries to undefine (-U)", 2);

    tableViewer = createViewer(gr);
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

    Button buttonDefineAdd = WidgetHelper.createButton(editButtons,
        AbstractCPropertyTab.ADD_STR, true);
    final Button buttonDefineEdit = WidgetHelper.createButton(editButtons,
        AbstractCPropertyTab.EDIT_STR, false);
    final Button buttonDefineDel = WidgetHelper.createButton(editButtons,
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
        // update button sensitivity..
        int sels = ((IStructuredSelection) event.getSelection()).size();
        boolean canEdit = (sels == 1);
        boolean canDel = (sels >= 1);
        buttonDefineEdit.setEnabled(canEdit);
        buttonDefineDel.setEnabled(canDel);
      }
    });

  }

  private void handleUnDefineAddButton(TableViewer tableViewer) {
    final Shell shell = tableViewer.getControl().getShell();
    AddCmakeUndefineDialog dlg = new AddCmakeUndefineDialog(shell, null);
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
      final Shell shell = tableViewer.getControl().getShell();
      AddCmakeUndefineDialog dlg = new AddCmakeUndefineDialog(shell,
          (CmakeUnDefine) cmakeDefine);
      if (dlg.open() == Dialog.OK) {
        tableViewer.update(cmakeDefine, null); // updates the display
      }
    }
  }

  private void handleUnDefineDelButton(TableViewer tableViewer) {
    final IStructuredSelection selection = (IStructuredSelection) tableViewer
        .getSelection();
    final Shell shell = tableViewer.getControl().getShell();
    if (MessageDialog.openQuestion(shell,
        "CMake-Undefine deletion confirmation",
        "Are you sure to delete the selected CMake-undefines?")) {
      @SuppressWarnings("unchecked")
      ArrayList<String> undefines = (ArrayList<String>) tableViewer.getInput();
      undefines.removeAll(selection.toList());
      tableViewer.remove(selection.toArray());// updates the display
    }
  }

  ////////////////////////////////////////////////////////////////////
  // inner classes
  ////////////////////////////////////////////////////////////////////

  /**
   * Converts the list of CmakeUnDefine objects.
   * 
   * @author Martin Weber
   */
  private static class CmakeUnDefineTableContentProvider implements
      IStructuredContentProvider {
    @Override
    public Object[] getElements(Object inputElement) {
      @SuppressWarnings("unchecked")
      final List<CmakeUnDefine> elems = (List<CmakeUnDefine>) inputElement;
      return elems.toArray();
    }

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
  } // CmakeUnUnDefineTableContentProvider

  private static class CmakeVariableLabelProvider extends BaseLabelProvider
      implements ITableLabelProvider {

//    // LabelProvider
//    @Override
//    public String getText(Object element) {
//      return getColumnText(element, 0);
//    }

    // interface ITableLabelProvider
    @Override
    public Image getColumnImage(Object element, int columnIndex) {
      return null;
    }

    // interface ITableLabelProvider
    @Override
    public String getColumnText(Object element, int columnIndex) {
      return ((CmakeUnDefine) element).getName();
    }
  } // CmakeVariableLabelProvider

  private static class MyViewerComparator extends ViewerComparator {
    private int sortColumn;
    private boolean ascending;

    public MyViewerComparator() {
      this.sortColumn = 0;
      ascending = true;
    }

    public int getSortDirection() {
      return ascending ? SWT.UP : SWT.DOWN;
    }

    public void setSortColumn(int column) {
      if (column == this.sortColumn) {
        // Same column as last sort; toggle the direction
        ascending ^= true;
      } else {
        // New column; do an ascending sort
        this.sortColumn = column;
        ascending = true;
      }
    }

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      CmakeUnDefine v1 = (CmakeUnDefine) e1;
      CmakeUnDefine v2 = (CmakeUnDefine) e2;
      int rc = v1.getName().compareTo(v2.getName());
      // If descending order, flip the direction
      if (!ascending) {
        rc = -rc;
      }
      return rc;
    }
  } // MyViewerComparator
}
