/*******************************************************************************
 * Copyright (c) 2014-2019 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.ui;

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

import de.marw.cmake4eclipse.mbs.settings.CmakeUnDefine;
import de.marw.cmake4eclipse.mbs.ui.preferences.ViewerComparatorSortHandler;

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

  /**
   * @param parent
   */
  public UnDefinesViewer(Composite parent) {
    createEditor(parent);
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

  /**
   * Gets the list of cmake undefines that are displayed by the viewer.
   */
  @SuppressWarnings("unchecked")
  public List<CmakeUnDefine> getInput() {
    return (List<CmakeUnDefine>) tableViewer.getInput();
  }

  private static TableViewer createViewer(Composite parent) {
    TableViewer viewer = new TableViewer(parent, SWT.BORDER | SWT.H_SCROLL
        | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);

    // Set the sorter for the table
    ViewerComparatorSortHandler comparator = new ViewerComparatorSortHandler(viewer);
    viewer.setComparator(comparator);

    createColumns(viewer, comparator);

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
   * @param viewer
   */
  private static void createColumns(final TableViewer viewer, SelectionListener sortSelection) {
    for (int i = 0; i < tableColumnNames.length; i++) {
      createTableViewerColumn(viewer, tableColumnNames[i],
          tableColumnWidths[i], i, sortSelection);
    }
  }

  /**
   * Creates a table viewer column for the table.
   */
  private static TableViewerColumn createTableViewerColumn(final TableViewer viewer,
      String title, int colWidth, final int colNumber, SelectionListener sortSelection) {
    final TableViewerColumn viewerColumn = new TableViewerColumn(viewer,
        SWT.NONE);
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
   * Creates the control to add/delete/edit cmake-variables to undefine.
   */
  private void createEditor(Composite parent) {
    final Group gr = WidgetHelper.createGroup(parent, SWT.FILL, 2,
        "CMake cache entries to &remove (-U)", 2);

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
}
