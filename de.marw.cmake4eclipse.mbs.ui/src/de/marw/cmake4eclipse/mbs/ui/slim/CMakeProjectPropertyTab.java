/*******************************************************************************
 * Copyright (c) 2022 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.ui.slim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;
import org.eclipse.ui.dialogs.NewFolderDialog;

import de.marw.cmake4eclipse.mbs.internal.storage.BuildTargetSerializer;
import de.marw.cmake4eclipse.mbs.internal.storage.Util;
import de.marw.cmake4eclipse.mbs.settings.CMakePreferences;
import de.marw.cmake4eclipse.mbs.ui.Activator;
import de.marw.cmake4eclipse.mbs.ui.QuirklessAbstractCPropertyTab;
import de.marw.cmake4eclipse.mbs.ui.WidgetHelper;

/**
 * UI to control general project properties for cmake. This tab is responsible for storing its values.
 *
 * @author Martin Weber
 */
public class CMakeProjectPropertyTab extends QuirklessAbstractCPropertyTab {
  private static final ILog log = Activator.getDefault().getLog();

  // Widgets
  private Text t_cmakelistsFolder;

  private Text t_targets;

  @Override
  public boolean canSupportMultiCfg() {
    return false;
  }

  @Override
  protected void createControls(final Composite parent) {
    super.createControls(parent);
    usercomp.setLayout(new GridLayout(2, false));

    // CMakeLists.txt folder group
    {
      Group gr = WidgetHelper.createGroup(usercomp, SWT.FILL, 2, "CMakeLists.txt folder (relative to project root)", 2);
      setupLabel(gr, "&Folder", 1, SWT.BEGINNING);
      t_cmakelistsFolder = setupText(gr, 1, GridData.FILL_HORIZONTAL);

      // "Browse", "Create" dialog launcher buttons...
      Composite buttonBar = new Composite(gr, SWT.NONE);
      {
        buttonBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false, 3, 1));
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        buttonBar.setLayout(layout);
      }
      Button b_browseCmakelistsFolder = WidgetHelper.createButton(buttonBar, "&Browse...", true);
      b_browseCmakelistsFolder.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          FilteredResourcesSelectionDialog dialog = new FilteredResourcesSelectionDialog(t_cmakelistsFolder.getShell(),
              false, page.getProject(), IResource.FOLDER);
          dialog.setTitle("Select folder containing the top-level CMakeLists.txt file");
          dialog.open();
          IFolder folder = (IFolder) dialog.getFirstResult();
          if (folder != null) {
            // insert selected resource name
            t_cmakelistsFolder.setText(folder.getProjectRelativePath().toPortableString());
          }
        }
      });

      Button b_createOutputFolder = WidgetHelper.createButton(buttonBar, "&Create...", true);
      b_createOutputFolder.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
          boolean noLinking = preferences.getBoolean(ResourcesPlugin.PREF_DISABLE_LINKING, false);
          try {
            // do not allow users to create a linked folder for the cmakelists.txt file..
            preferences.putBoolean(ResourcesPlugin.PREF_DISABLE_LINKING, true);

            NewFolderDialog dialog = new NewFolderDialog(parent.getShell(), page.getProject());
            if (dialog.open() == Window.OK) {
              IFolder f = (IFolder) dialog.getFirstResult();
              t_cmakelistsFolder.setText(f.getProjectRelativePath().toPortableString());
            }
          } finally {
            preferences.putBoolean(ResourcesPlugin.PREF_DISABLE_LINKING, noLinking);
          }
        }
      });
    }
    // build targets group
    {
      Group gr = WidgetHelper.createGroup(usercomp, SWT.FILL, 2, "Build Targets", 2);
      t_targets = setupText(gr, 1, GridData.FILL_HORIZONTAL);
      t_targets.setToolTipText("Space-separated list of target names to show in the project explorer.");
    }
  }

  /**
   * Updates displayed values according to the preferences edited by this tab.
   */
  private void updateDisplay() {
  }

  @Override
  protected void updateData(ICResourceDescription resd) {
    if (resd == null)
      return;
    try {
      ICStorageElement storage = resd.getConfiguration().getProjectDescription()
          .getStorage(CMakePreferences.CFG_STORAGE_ID, true);
      String folder = Objects.requireNonNullElseGet(storage.getAttribute(CMakePreferences.ATTR_CMAKELISTS_FLDR),
          String::new);
      t_cmakelistsFolder.setText(folder);

      ICStorageElement[] storeTargets = storage.getChildrenByName(CMakePreferences.ELEM_BUILD_TARGETS);
      List<String> targets = new ArrayList<>();
      if (storeTargets != null && storeTargets.length > 0) {
        Util.deserializeCollection(targets, new BuildTargetSerializer(), storeTargets[0]);
      }
      String targetsTxt = targets.stream().collect(Collectors.joining(" "));
      t_targets.setText(targetsTxt);

    } catch (CoreException ex) {
      log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, null, ex));
    }
    updateDisplay();
  }

  /**
   * Invoked when project configuration changes??
   *
   * @see org.eclipse.cdt.ui.newui.AbstractCPropertyTab#performApply(org.eclipse.cdt.core.settings.model.ICResourceDescription,
   *      org.eclipse.cdt.core.settings.model.ICResourceDescription)
   */
  @Override
  protected void performApply(ICResourceDescription src, ICResourceDescription dst) {
    // save behavior as CDT has: Apply does persist settings:
    // this also solves the problem with different configuration that have been modified, apply but would
    // otherwise not be persisted on performOK()
    persist(dst);
  }

  @Override
  protected void performOK() {
    // make sure the displayed values get saved
    persist(getResDesc());
  }

  /**
   * @param resDesc
   */
  private void persist(final ICResourceDescription resDesc) {
    if (resDesc == null)
      return;

    // save project properties..
    try {
      ICProjectDescription prjDes = resDesc.getConfiguration().getProjectDescription();
      ICStorageElement storage = prjDes
          .getStorage(CMakePreferences.CFG_STORAGE_ID, true);
      storage.setAttribute(CMakePreferences.ATTR_CMAKELISTS_FLDR, t_cmakelistsFolder.getText());

      String[] targetsTxt = t_targets.getText().trim().split(" +");
      Arrays.sort(targetsTxt);
      List<String> targets = Arrays.asList(targetsTxt);
      Util.serializeCollection(CMakePreferences.ELEM_BUILD_TARGETS, storage, new BuildTargetSerializer(),
          targets);
      // update the project explorer view
      BuildTargetsManager.getDefault().notifyListeners(new BuildTargetEvent(prjDes.getProject(), targets));
    } catch (CoreException ex) {
      log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, null, ex));
    }
  }

  /*-
   * @see org.eclipse.cdt.ui.newui.AbstractCPropertyTab#performDefaults()
   */
  @Override
  protected void performDefaults() {
    t_cmakelistsFolder.setText("");
    t_targets.setText("");
    updateDisplay();
  }

  // This page can be displayed for project
  @Override
  public boolean canBeVisible() {
    return page.isForProject();
  }
}
