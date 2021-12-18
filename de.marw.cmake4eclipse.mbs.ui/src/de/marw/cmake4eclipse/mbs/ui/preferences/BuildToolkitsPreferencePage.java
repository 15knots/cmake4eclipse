/**
 *
 */
package de.marw.cmake4eclipse.mbs.ui.preferences;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.google.gson.JsonSyntaxException;

import de.marw.cmake4eclipse.mbs.preferences.BuildToolKitDefinition;
import de.marw.cmake4eclipse.mbs.preferences.PreferenceAccess;
import de.marw.cmake4eclipse.mbs.settings.CmakeGenerator;
import de.marw.cmake4eclipse.mbs.ui.Activator;
import de.marw.cmake4eclipse.mbs.ui.WidgetHelper;

/**
 * Preference page for Cmake4eclipse workbench preferences (build tools).
 */
public class BuildToolkitsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
  private TableViewer toolkitsTableViewer;
  private long overwritingBtkUid;

  public BuildToolkitsPreferencePage() {
    setDescription("Add, remove or edit build tool kit definitions.\n"
        + "A build tool kit definition guides cmake to find build tools they are not in the executable file\n"
        + "search-list ($PATH on Linux).\n" + "Mark a build tool kit as 'Overwrites' to set it into effect.");
    noDefaultButton();
  }

  @Override
  public void init(IWorkbench workbench) {
  }

  @Override
  protected Control createContents(Composite parent) {
    Control control = createEditor(parent);
    initFromPrefstore();
    return control;
  }

  /**
   * Creates the control to add/delete/edit build tool kit.
   *
   * @return
   */
  private Control createEditor(Composite parent) {
    final Group gr = WidgetHelper.createGroup(parent, SWT.FILL, 2, "Build Tool Kits", 2);

    toolkitsTableViewer = createViewer(gr);
    toolkitsTableViewer.getTable().setData(PreferenceAccess.TOOLKITS);
    // let DEL key trigger the delete dialog
    toolkitsTableViewer.getTable().addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.keyCode == SWT.DEL) {
          handleTcDelButton(toolkitsTableViewer);
        } else if (e.keyCode == SWT.INSERT) {
          handleTcAddButton(toolkitsTableViewer);
        }
      }
    });

    {
      // Buttons, vertically stacked
      Composite editButtons = new Composite(gr, SWT.NONE);
      editButtons.setLayoutData(new GridData(SWT.CENTER, SWT.BEGINNING, false, false));
      editButtons.setLayout(new GridLayout(1, false));

      final Button buttonTcAdd = WidgetHelper.createButton(editButtons, "&Add...", true);
      final Button buttonTcEdit = WidgetHelper.createButton(editButtons, "&Edit...", false);
      final Button buttonTcDuplicate = WidgetHelper.createButton(editButtons, "Dupli&cate...", false);
      final Button buttonTcDel = WidgetHelper.createButton(editButtons, "&Remove", false);

      // wire button actions...
      buttonTcAdd.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
          handleTcAddButton(toolkitsTableViewer);
        }
      });
      buttonTcEdit.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
          handleTcEditButton(toolkitsTableViewer);
        }
      });
      buttonTcDuplicate.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
          handleTcDuplicateButton(toolkitsTableViewer);
        }
      });
      buttonTcDel.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
          handleTcDelButton(toolkitsTableViewer);
        }
      });

      // enable button sensitivity based on table selection
      toolkitsTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
        @Override
        public void selectionChanged(SelectionChangedEvent event) {
          // update button sensitivity..
          int sels = ((IStructuredSelection) event.getSelection()).size();
          boolean canEdit = (sels == 1);
          boolean canDel = (sels >= 1);
          buttonTcEdit.setEnabled(canEdit);
          buttonTcDel.setEnabled(canDel);
          buttonTcDuplicate.setEnabled(canEdit);
        }
      });
    }

    return gr;
  }

  private TableViewer createViewer(Composite parent) {
    TableViewer viewer = new TableViewer(parent,
        SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);

    // Set the sorter for the table
    ViewerComparatorSortHandler comparator = new BtkViewerComparatorSortHandler(viewer);
    viewer.setComparator(comparator);

    createColumns(viewer, comparator);
    viewer.setContentProvider(new BtkTableContentProvider());

    final Table table = viewer.getTable();
    table.setHeaderVisible(true);
    table.setLinesVisible(true);

    // Layout the viewer
    GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
    viewer.getControl().setLayoutData(gridData);
    return viewer;
  }

  /**
   * Creates the columns for the table.
   *
   * @param viewer
   * @param sortSelectionListener
   */
  private void createColumns(final TableViewer viewer, SelectionListener sortSelectionListener) {
    TableViewerColumn column = createTableViewerColumn(viewer, "Overwrites", 90, sortSelectionListener);
    column.setLabelProvider(ColumnLabelProvider
        .createTextProvider(e -> ((BuildToolKitDefinition) e).getUid() == overwritingBtkUid ? "Overwrites" : ""));
    column.getColumn().setToolTipText("Whether the Build Tool Kit overwrites Default build system preferences.");
    column.setEditingSupport(new OverwritesEditingSupport(viewer));

    createTableViewerColumn(viewer, "Name", 150, sortSelectionListener)
        .setLabelProvider(ColumnLabelProvider.createTextProvider(e -> ((BuildToolKitDefinition) e).getName()));
    createTableViewerColumn(viewer, "Build System", 100, sortSelectionListener).setLabelProvider(
        ColumnLabelProvider.createTextProvider(e -> ((BuildToolKitDefinition) e).getGenerator().getCmakeName()));
    createTableViewerColumn(viewer, "$PATH", 250, sortSelectionListener)
        .setLabelProvider(ColumnLabelProvider.createTextProvider(e -> ((BuildToolKitDefinition) e).getPath()));
  }

  /**
   * Creates a table viewer column for the table.
   */
  private TableViewerColumn createTableViewerColumn(final TableViewer viewer, String title, int colWidth,
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

  private static void handleTcAddButton(TableViewer tableViewer) {
    BuildToolKitDefinition tc = new BuildToolKitDefinition(BuildToolKitDefinition.createUniqueId(), "",
        CmakeGenerator.Ninja, "${env_var:PATH}");
    AbstractBuildToolkitDialog dlg = new AddBuildToolkitDialog(tableViewer.getTable().getShell(), tc);

    if (dlg.open() == Dialog.OK) {
      @SuppressWarnings("unchecked")
      List<BuildToolKitDefinition> tcs = (List<BuildToolKitDefinition>) tableViewer.getInput();
      tcs.add(tc);
      tableViewer.add(tc); // updates the display
    }
  }

  private static void handleTcEditButton(TableViewer tableViewer) {
    final IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
    if (selection.size() == 1) {
      Object tc = selection.getFirstElement();
      AbstractBuildToolkitDialog dlg = new EditBuildToolkitDialog(tableViewer.getTable().getShell(),
          (BuildToolKitDefinition) tc);
      if (dlg.open() == Dialog.OK) {
        tableViewer.update(tc, null); // updates the display
      }
    }
  }

  private static void handleTcDuplicateButton(TableViewer tableViewer) {
    final IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
    if (selection.size() == 1) {
      @SuppressWarnings("unchecked")
      List<BuildToolKitDefinition> tcs = (List<BuildToolKitDefinition>) tableViewer.getInput();
      BuildToolKitDefinition tc = (BuildToolKitDefinition) selection.getFirstElement();
      BuildToolKitDefinition tc2 = new BuildToolKitDefinition(tc);
      tc2.setName(generateName(tc.getName(), tcs));
      AbstractBuildToolkitDialog dlg = new EditBuildToolkitDialog(tableViewer.getTable().getShell(), tc2);
      if (dlg.open() == Dialog.OK) {
        tcs.add(tc2);
        tableViewer.add(tc2); // updates the display
      }
    }
  }

  private void handleTcDelButton(TableViewer tableViewer) {
    final IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
    @SuppressWarnings("unchecked")
    List<BuildToolKitDefinition> selected = selection.toList();
    String names = selected.stream().map(BuildToolKitDefinition::getName)
        .collect(Collectors.joining("\n\t", "\n\t", ""));
    if (MessageDialog.openConfirm(getShell(), "Build Tool Kit deletion confirmation",
        "Delete the selected Build Tool Kits?" + names)) {
      @SuppressWarnings("unchecked")
      List<BuildToolKitDefinition> tcs = (List<BuildToolKitDefinition>) tableViewer.getInput();
      tcs.removeAll(selected);
      tableViewer.remove(selection.toArray());// updates the display
    }
  }

  private void initFromPrefstore() {
    IPreferenceStore store = getPreferenceStore();

    overwritingBtkUid = store.getLong(PreferenceAccess.TOOLKIT_OVERWRITES);
    String key = (String) toolkitsTableViewer.getTable().getData();
    String json = store.getString(key);
    try {
      List<BuildToolKitDefinition> entries = PreferenceAccess.toListFromJson(BuildToolKitDefinition.class, json);
      toolkitsTableViewer.setInput(entries);
    } catch (JsonSyntaxException ex) {
      // file format error
      Activator.getDefault().getLog().error("Error loading workbench preferences", ex);
    }
  }

  @Override
  public boolean performOk() {
    IPreferenceStore store = getPreferenceStore();
    boolean dirty = false;
    {
      String key = PreferenceAccess.TOOLKIT_OVERWRITES;
      if (store.getLong(key) != overwritingBtkUid) {
        dirty = true;
        store.setValue(key, overwritingBtkUid);
      }
    }
    {
      String key = (String) toolkitsTableViewer.getTable().getData();
      String oldVal = store.getString(key);
      @SuppressWarnings("unchecked")
      List<BuildToolKitDefinition> toolchains = (List<BuildToolKitDefinition>) toolkitsTableViewer.getInput();
      String newVal = PreferenceAccess.toJsonFromList(toolchains);
      if (!Objects.equals(newVal, oldVal)) {
        dirty = true;
        store.setValue(key, newVal);
      }
    }

    if (dirty) {
      store.setValue(PreferenceAccess.DIRTY_TS, System.currentTimeMillis());
    }

    return true;
  }

  /**
   * Overwritten to get the preferences of plugin "de.marw.cmake4eclipse.mbs"
   */
  @Override
  protected IPreferenceStore doGetPreferenceStore() {
    return new ScopedPreferenceStore(InstanceScope.INSTANCE, PreferenceAccess.getPreferences().name());
  }

  /**
   * Compares the given name against current names and adds the appropriate numerical suffix to ensure that it is
   * unique.
   *
   * @param name the name with which to ensure uniqueness
   * @return the unique version of the given name
   */
  private static String generateName(String name, List<BuildToolKitDefinition> toolchains) {
    if (!isDuplicateName(name, toolchains)) {
      return name;
    }

    if (name.matches(".*\\(\\d*\\)")) { //$NON-NLS-1$
      int start = name.lastIndexOf('(');
      int end = name.lastIndexOf(')');
      String stringInt = name.substring(start + 1, end);
      int numericValue = Integer.parseInt(stringInt);
      String newName = name.substring(0, start + 1) + (numericValue + 1) + ")"; //$NON-NLS-1$
      return generateName(newName, toolchains);
    } else {
      return generateName(name + " (1)", toolchains); //$NON-NLS-1$
    }
  }

  private static boolean isDuplicateName(String name, List<BuildToolKitDefinition> toolchains) {
    for (BuildToolKitDefinition tc : toolchains) {
      if (tc.getName().equals(name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @author weber
   */
  private class OverwritesEditingSupport extends EditingSupport {
    private final CheckboxCellEditor editor;

    private OverwritesEditingSupport(ColumnViewer viewer) {
      super(viewer);
      this.editor = new CheckboxCellEditor((Composite) viewer.getControl());
    }

    @Override
    protected CellEditor getCellEditor(Object element) {
      return editor;
    }

    @Override
    protected boolean canEdit(Object element) {
      return true;
    }

    @Override
    protected Object getValue(Object element) {
      return ((BuildToolKitDefinition) element).getUid() == BuildToolkitsPreferencePage.this.overwritingBtkUid;
    }

    @Override
    protected void setValue(Object element, Object value) {
      BuildToolkitsPreferencePage.this.overwritingBtkUid = Boolean.TRUE.equals(value)
          ? ((BuildToolKitDefinition) element).getUid()
          : 0L;
      getViewer().refresh();
    }
  }

  /**
   * Converts the list of BuildToolKitDefinition objects.
   *
   * @author Martin Weber
   */
  private static class BtkTableContentProvider implements IStructuredContentProvider {
    @Override
    public Object[] getElements(Object inputElement) {
      @SuppressWarnings("unchecked")
      final List<BuildToolKitDefinition> elems = (List<BuildToolKitDefinition>) inputElement;
      return elems.toArray();
    }
  } // BtkTableContentProvider

  private static class BtkViewerComparatorSortHandler extends ViewerComparatorSortHandler {

    public BtkViewerComparatorSortHandler(TableViewer tableViever) {
      super(tableViever);
    }

    @Override
    protected int doCompare(int sortColumn, String label1, String label2) {
      switch (sortColumn) {
      case 1:
        return label1.compareToIgnoreCase(label2);
      default:
        return label1.compareTo(label2);
      }
    }

    /**
     * Overwritten to cycle through the through sort directions: none, up, down.
     */
    @Override
    protected int calculateNextSortDirection(int sortDirection) {
      switch (sortDirection) {
      case SWT.NONE:
      default:
        return SWT.UP;
      case SWT.UP:
        return SWT.DOWN;
      case SWT.DOWN:
        return SWT.NONE;
      }
    }
  } // BtkViewerComparatorSortHandler
}
