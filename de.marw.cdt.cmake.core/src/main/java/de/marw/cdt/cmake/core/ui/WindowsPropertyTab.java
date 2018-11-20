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

import java.util.EnumSet;
import java.util.Objects;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import de.marw.cdt.cmake.core.internal.Activator;
import de.marw.cdt.cmake.core.internal.CmakeGenerator;
import de.marw.cdt.cmake.core.internal.settings.CMakePreferences;
import de.marw.cdt.cmake.core.internal.settings.WindowsPreferences;

/**
 * UI to control host Windows specific project properties and preferences for
 * cmake. This tab is responsible for storing its values.
 *
 * @author Martin Weber
 */
public class WindowsPropertyTab extends
    AbstractOsPropertyTab<WindowsPreferences> {

  private static final EnumSet<CmakeGenerator> generators = EnumSet.of(
      CmakeGenerator.MinGWMakefiles, CmakeGenerator.MSYSMakefiles,
      CmakeGenerator.UnixMakefiles, CmakeGenerator.Ninja,
      CmakeGenerator.NMakeMakefiles, CmakeGenerator.NMakeMakefilesJOM,
      CmakeGenerator.BorlandMakefiles, CmakeGenerator.WatcomWMake);

  /** script file */
  private Text t_script;

  /*-
   * @see de.marw.cdt.cmake.core.ui.AbstractOsPropertyTab#getOsPreferences(de.marw.cdt.cmake.core.internal.CMakePreferences)
   */
  @Override
  protected WindowsPreferences getOsPreferences(CMakePreferences prefs) {
    return prefs.getWindowsPreferences();
  }

  @Override
  protected EnumSet<CmakeGenerator> getAvailableGenerators() {
    return WindowsPropertyTab.generators;
  }

  /* (non-Javadoc)
   * @see de.marw.cdt.cmake.core.ui.AbstractOsPropertyTab#createEnvironmentScriptControls(org.eclipse.swt.widgets.Group)
   */
  @Override
  protected void createEnvironmentScriptControls(Group parent, int horizontalSpan) {
    setupLabel(parent, "&Env. Script", 1, SWT.BEGINNING);

    t_script = setupText(parent, 1, GridData.FILL_HORIZONTAL);
    t_script.setToolTipText("Optional script to run in the same shell prior to cmake");
    // "Filesystem", "Variables" dialog launcher buttons...
    Composite buttonBar = new Composite(parent, SWT.NONE);
    {
      buttonBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false,
          horizontalSpan, 1));
      GridLayout layout;
      layout = new GridLayout(2, false);
      layout.marginHeight = 0;
      layout.marginWidth = 0;
      buttonBar.setLayout(layout);
    }
    Button b_scriptBrowseFiles = WidgetHelper.createButton(buttonBar, "Fi&le System...",
        true);
    b_scriptBrowseFiles.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        IDialogSettings settings = Activator.getDefault().getDialogSettings();
        FileDialog dialog = new FileDialog(t_script.getShell());
        dialog.setFilterPath(settings.get("cmake_dir"));
        String text = dialog.open();
        settings.put("cmake_dir", dialog.getFilterPath());
        if (text != null) {
          t_script.setText(text);
        }
      }
    });

    Button b_scriptVariables = WidgetHelper.createButton(buttonBar, "Insert Va&riable...",
        true);
    b_scriptVariables.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        final ICResourceDescription resDesc = getResDesc();
        if (resDesc == null)
          return;
        ICConfigurationDescription cfgd= resDesc.getConfiguration();
      String text = AbstractCPropertyTab.getVariableDialog(t_script.getShell(), cfgd);
        if (text != null) {
          t_script.insert(text);
        }
      }
    });
  }

  protected void saveToModel(WindowsPreferences prefs) {
    if (prefs == null)
      return;
    super.saveToModel(prefs);
    String command = t_script.getText().trim();
    prefs.setEnvSetterScript(command);
  }

  /* (non-Javadoc)
   * @see de.marw.cdt.cmake.core.ui.AbstractOsPropertyTab#updateDisplay(de.marw.cdt.cmake.core.internal.settings.AbstractOsPreferences)
   */
  @Override
  protected void updateDisplay(WindowsPreferences prefs) {
    super.updateDisplay(prefs);
    t_script.setText(Objects.toString(prefs.getEnvSetterScript(),""));
  }


}
