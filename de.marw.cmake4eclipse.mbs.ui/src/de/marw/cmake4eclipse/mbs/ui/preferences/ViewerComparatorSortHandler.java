/**
 *
 */
package de.marw.cmake4eclipse.mbs.ui.preferences;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * A generic ViewerComparator that compares the elements based on the label of a single column. It also acts as a
 * selection adapter allowing for users to changes the sorting order and sorting column of a TableViewer.
 * <p>
 * In order to keep the table sorting UI and the actual sorting of a table viewer synchronized, a single instance of
 * this class should be set as the ViewerComparator of a table viewer as well as the selection listener of the
 * TableColumn for each of the table's columns.
 *
 * @author Martin Weber
 *
 * @see TableViewer#setComparator(ViewerComparator)
 * @see TableColumn#addSelectionListener(SelectionListener)
 */
public class ViewerComparatorSortHandler extends ViewerComparator implements SelectionListener {
  private final TableViewer tv;
  private int sortColumn;
  private Function<Object, String> objectToLabel;

  public ViewerComparatorSortHandler(TableViewer tableViever) {
    this.tv = Objects.requireNonNull(tableViever);
    final Table table = tv.getTable();
    TableColumn col = table.getSortColumn();
    if (col == null) {
      setSortColumn(-1);
    } else {
      setSortColumn(Arrays.asList(table.getColumns()).indexOf(col));
    }
  }

  /**
   * Sets the column to be used for sorting.<br>
   * Calling this method multiple times with the same TableColumn object will toggle / cycle though the sort directions
   * as implemented by method {@link #calculateNextSortDirection}.
   *
   * @param column the table column to sort on or <code>null</code>
   *
   * @return the new sort direction of the table. One of <code>SWT.UP</code>, <code>SWT.DOWN</code> or
   *         <code>SWT.NONE</code>, any other value will have no effect.
   *
   */
  public int setSortColumn(TableColumn sortColumn) {
    final Table table = tv.getTable();
    TableColumn column = table.getSortColumn();
    int sd;
    if (column == sortColumn) {
      // change sort direction
      sd = calculateNextSortDirection(table.getSortDirection());
    } else {
      // user selected a different column to sort on
      table.setSortColumn(sortColumn);
      setSortColumn(Arrays.asList(table.getColumns()).indexOf(sortColumn));
      sd = SWT.UP;
    }

    table.setSortDirection(sd);
    tv.refresh();
    return table.getSortDirection();
  }

  /**
   * Sets the column to be used for sorting.
   *
   * @param column the table column index to sort
   */
  private void setSortColumn(int column) {
    this.sortColumn = column;
    ColumnLabelProvider clp = (ColumnLabelProvider) tv.getLabelProvider(column);
    if (clp != null) {
      objectToLabel = (c) -> clp.getText(c);
    } else {
      LabelProvider lp = ((LabelProvider) tv.getLabelProvider());
      objectToLabel = (c) -> lp.getText(c);
    }
  }

  /**
   * Handles the actual comparison for a given column. Clients may overwrite.
   *
   * @param sortColumn the index of the column to sort on
   * @param label1     the text of the first element in the column to be compared
   * @param label2     the text of the second element in the column to be compared
   *
   * @return a negative number if the first element is less than the second element; the value 0 if the first element is
   *         equal to the second element; and a positive number if the first element is greater than the second element
   */
  protected int doCompare(int sortColumn, String label1, String label2) {
    return label1.compareTo(label2);
  }

  @Override
  public int compare(Viewer viewer, Object row1, Object row2) {
    if (sortColumn == -1) {
      return 0; // nothing we could compare
    }
    int rc = doCompare(sortColumn, objectToLabel.apply(row1), objectToLabel.apply(row2));
    switch (tv.getTable().getSortDirection()) {
    case SWT.NONE:
    default:
      return 0;
    case SWT.UP:
      return rc;
    case SWT.DOWN:
      return -rc;
    }
  }

  /**
   * Calculates a new sort direction.<br>
   * When the user clicks the same column header multiple times, the table is going to be sorted on the same column, but
   * a different direction.<br>
   * This default implementation toggles the sort direction between SWT.UP and SWT.DOWN. Clients may overwrite.
   *
   * @param sortDirection the current sort direction. One of <code>SWT.UP</code>, <code>SWT.DOWN</code> or
   *                      <code>SWT.NONE</code>.
   *
   * @return the new sort direction. One of <code>SWT.UP</code>, <code>SWT.DOWN</code> or <code>SWT.NONE</code>, any
   *         other value will have no effect.
   */
  protected int calculateNextSortDirection(int sortDirection) {
    switch (sortDirection) {
    case SWT.NONE:
    case SWT.DOWN:
      return SWT.UP;
    case SWT.UP:
      return SWT.DOWN;
    }
    return sortDirection;
  }

  // interface SelectionListener
  @Override
  public void widgetSelected(SelectionEvent e) {
    TableColumn column = (TableColumn) e.widget;
    setSortColumn(column);
  }

  // interface SelectionListener
  @Override
  public void widgetDefaultSelected(SelectionEvent e) {
  }
} // ViewerComparatorSortHandler