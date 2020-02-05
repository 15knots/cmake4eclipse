/*******************************************************************************
 * Copyright (c) 2014-2019 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cdt.cmake.core.ui;

import java.util.ArrayList;
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

import de.marw.cdt.cmake.core.settings.CmakeDefine;

/**
 * Displays a table for the Cmake defines. The created table will be displayed
 * in a group, together with buttons to add, edit and delete table entries.
 *
 * @author Martin Weber
 */
/* package */class DefinesViewer {
  /** table column names */
  private static final String[] tableColumnNames = { "Name", "Type", "Value" };
  /** table column widths */
  private static final int[] tableColumnWidths = { 120, 80, 250 };

  /** the configuration description to use for variable selection dialog or {@code null} */
  private final ICConfigurationDescription cfgd;

  private TableViewer tableViewer;
  private MyViewerComparator comparator;

  /**
   * @param parent
   * @param cfgd
   *          the configuration description to use for variable selection dialog
   *          or {@code null}
   */
  public DefinesViewer(Composite parent, ICConfigurationDescription cfgd) {
    this.cfgd = cfgd;
    createEditor(parent);
    // Set the sorter for the table
    comparator = new MyViewerComparator();
    tableViewer.setComparator(comparator);
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

  private TableViewer createViewer(Composite parent) {
    TableViewer viewer = new TableViewer(parent, SWT.BORDER | SWT.H_SCROLL
        | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);

    createColumns(parent, viewer);

    final Table table = viewer.getTable();
    table.setHeaderVisible(true);
    table.setLinesVisible(true);

    viewer.setContentProvider(new CmakeDefineTableContentProvider());
    viewer.setLabelProvider(new CmakeVariableLabelProvider());

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
   * Creates the control to add/delete/edit cmake-variables to define.
   */
  private void createEditor(Composite parent) {
    final Group gr = WidgetHelper.createGroup(parent, SWT.FILL, 2,
        "CMake cache entries to &create (-D)", 2);

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
    editButtons.setLayoutData(new GridData(SWT.CENTER, SWT.BEGINNING, false,
        false));
    editButtons.setLayout(new GridLayout(1, false));
    //    editButtons.setBackground(BACKGROUND_FOR_USER_VAR);

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
      final Shell shell = tableViewer.getControl().getShell();
      AddCmakeDefineDialog dlg = new AddCmakeDefineDialog(shell,
          cfgd, (CmakeDefine) cmakeDefine);
      if (dlg.open() == Dialog.OK) {
        tableViewer.update(cmakeDefine, null); // updates the display
      }
    }
  }

  private void handleDefineDelButton(TableViewer tableViewer) {
    final IStructuredSelection selection = (IStructuredSelection) tableViewer
        .getSelection();
    final Shell shell = tableViewer.getControl().getShell();
    if (MessageDialog.openQuestion(shell, "CMake-Define deletion confirmation",
        "Are you sure to delete the selected CMake-defines?")) {
      @SuppressWarnings("unchecked")
      ArrayList<CmakeDefine> defines = (ArrayList<CmakeDefine>) tableViewer
          .getInput();
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
