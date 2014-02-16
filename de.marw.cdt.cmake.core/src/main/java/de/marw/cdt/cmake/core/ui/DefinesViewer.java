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

import java.util.List;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import de.marw.cdt.cmake.core.internal.CmakeDefine;

/**
 * Displays a table for the Cmake defines.
 *
 * @author Martin Weber
 */
/* package */class DefinesViewer {
  /** table column names */
  private static final String[] tableColumnNames = { "Name", "Type", "Value" };
  /** table column widths */
  private static final int[] tableColumnWidths = { 100, 100, 250 };

  private TableViewer viewer;
  private MyViewerComparator comparator;

  /**
   * @param parent
   */
  public DefinesViewer(Composite parent) {
    createViewer(parent);
    // Set the sorter for the table
    comparator = new MyViewerComparator();
    viewer.setComparator(comparator);
  }

  /**
   * Gets the TableViewer for the Cmake-defines
   */
  public TableViewer getTableViewer() {
    return viewer;
  }

  /** Sets the list of cmake defines that are displayed by the viewer.
   * @param input
   */
  public void setInput(List<CmakeDefine> defines){
    // Get the content for the viewer, setInput will call getElements in the
    // contentProvider
    viewer.setInput(defines);
  }

  private void createViewer(Composite parent) {
    viewer = new TableViewer(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL
        | SWT.MULTI | SWT.FULL_SELECTION);

    createColumns(parent, viewer);

    final Table table = viewer.getTable();
    table.setHeaderVisible(true);
    table.setLinesVisible(true);

    viewer.setContentProvider(new CmakeDefineTableContentProvider());
    viewer.setLabelProvider(new CmakeVariableLabelProvider());

//    // make the selection available to other views
//    getSite().setSelectionProvider(viewer);

    // Layout the viewer
    GridData gridData = new GridData();
    gridData.verticalAlignment = GridData.FILL;
//    gridData.horizontalSpan = 2;
    gridData.grabExcessHorizontalSpace = true;
    gridData.grabExcessVerticalSpace = true;
    gridData.horizontalAlignment = GridData.FILL;
    viewer.getControl().setLayoutData(gridData);
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
        viewer.getTable().setSortDirection(dir);
        viewer.getTable().setSortColumn(column);
        viewer.refresh();
      }
    };
    return selectionAdapter;
  }

  ////////////////////////////////////////////////////////////////////
  // inner classes
  ////////////////////////////////////////////////////////////////////

  /**
   * Converts the list of CmakeDefine object.
   *
   * @author Martin Weber
   */
  private static class CmakeDefineTableContentProvider implements
      IStructuredContentProvider {
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
      CmakeDefine v1 = (CmakeDefine) e1;
      CmakeDefine v2 = (CmakeDefine) e2;
      int rc = 0;
      switch (sortColumn) {
      case 0:
        rc = v1.getName().compareTo(v2.getName());
        break;
      case 1:
        rc = v1.getType().name().compareTo(v2.getType().name());
        break;
      case 2:
        rc = v1.getValue().compareTo(v2.getValue());
        break;
      default:
        rc = 0;
      }
      // If descending order, flip the direction
      if (!ascending) {
        rc = -rc;
      }
      return rc;
    }
  } // MyViewerComparator
}
