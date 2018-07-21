/*******************************************************************************
 * Copyright (c) 2017-2018 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cmake.cdt.ui.language.settings.providers;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsManager;
import org.eclipse.cdt.ui.dialogs.AbstractCOptionPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.marw.cmake.cdt.language.settings.providers.CompileCommandsJsonParser;

/**
 * Option page for CompileCommandsJsonParse
 *
 * @author Martin Weber
 */
public class CompileCommandsJsonParserOptionPage extends AbstractCOptionPage {

  private Text pattern;
  private Button b_versionsEnabled;

  @Override
  public void performApply(IProgressMonitor monitor) throws CoreException {
    // normally should be handled by LanguageSettingsProviderTab
    final String text = pattern.getText();
    try {
      Pattern.compile(text);
      CompileCommandsJsonParser provider = (CompileCommandsJsonParser) getProvider();
      provider.setVersionPattern(text);
    } catch (PatternSyntaxException ex) {
      // BUG in CDT: core exceptions are not visible to users here
      // IStatus status = new Status(IStatus.ERROR, CMakePlugin.PLUGIN_ID,
      // IStatus.OK,
      // "invalid suffix pattern in CMAKE_EXPORT_COMPILE_COMMANDS Parser", ex);
      // throw new CoreException(status);

      throw new PatternSyntaxException(
          "invalid suffix pattern in CMAKE_EXPORT_COMPILE_COMMANDS Parser: " + ex.getDescription(), ex.getPattern(),
          ex.getIndex());
    }
  }

  @Override
  public void performDefaults() {
    // normally should be handled by LanguageSettingsProviderTab
  }

  @Override
  public void createControl(Composite parent) {
    final boolean enabled = parent.isEnabled();
    final CompileCommandsJsonParser provider = (CompileCommandsJsonParser) getProvider();

    final Composite composite = new Composite(parent, SWT.NONE);
    {
      GridLayout layout = new GridLayout();
      layout.numColumns = 2;
      layout.marginWidth = 1;
      layout.marginHeight = 1;
      layout.marginRight = 1;
      composite.setLayout(layout);
      composite.setLayoutData(new GridData(GridData.FILL_BOTH));
    }

    final Group gr = createGroup(composite, SWT.FILL, 2, "For compilers with version in name", 2);

    b_versionsEnabled = createCheckbox(gr, SWT.BEGINNING, 2, "&Also try with version suffix");
    b_versionsEnabled.setToolTipText("Can recognize gcc-12.9.2, clang++-7.5.4, ...");
    b_versionsEnabled.setEnabled(enabled);
    b_versionsEnabled.setSelection(provider.isVersionPatternEnabled());
    {
      Label label = new Label(gr, SWT.NONE);
      label.setEnabled(enabled);
      label.setText("&Suffix pattern:");
      GridData gd = new GridData(SWT.BEGINNING);
      gd.horizontalSpan = 1;
      label.setLayoutData(gd);
    }

    pattern = new Text(gr, SWT.SINGLE | SWT.BORDER);
    pattern.setToolTipText("Specify a Java regular expression pattern here");
    pattern.setEnabled(enabled && b_versionsEnabled.getSelection());
    final String compilerPattern = provider.getVersionPattern();
    pattern.setText(compilerPattern != null ? compilerPattern : "");
    {
      GridData gd = new GridData();
      gr.setLayoutData(gd);
      gd.horizontalSpan = 1;
      gd.grabExcessHorizontalSpace = true;
      gd.horizontalAlignment = SWT.FILL;
      pattern.setLayoutData(gd);
    }

    // to adjust sensitivity...
    b_versionsEnabled.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        boolean selected = ((Button) event.widget).getSelection();
        provider.setVersionPatternEnabled(selected);
        pattern.setEnabled(selected);
      }
    });

    setControl(composite);
  }

  /**
   * Creates a checkbox button.
   *
   * @param parent
   * @param horizontalAlignment
   *          how control will be positioned horizontally within a cell of the
   *          parent's grid layout, one of: SWT.BEGINNING (or SWT.LEFT),
   *          SWT.CENTER, SWT.END (or SWT.RIGHT), or SWT.FILL
   * @param horizontalSpan
   *          number of column cells in the parent's grid layout that the
   *          control will take up.
   * @param text
   *          text to display on the checkbox
   */
  private static Button createCheckbox(Composite parent, int horizontalAlignment, int horizontalSpan, String text) {
    Button b = new Button(parent, SWT.CHECK);
    b.setText(text);
    GridData gd = new GridData(horizontalAlignment, SWT.CENTER, false, false);
    gd.horizontalSpan = horizontalSpan;
    b.setLayoutData(gd);
    return b;
  }

  /**
   * Creates a group with a grid layout.
   *
   * @param parent
   * @param horizontalAlignment
   *          how control will be positioned horizontally within a cell of the
   *          parent's grid layout, one of: SWT.BEGINNING (or SWT.LEFT),
   *          SWT.CENTER, SWT.END (or SWT.RIGHT), or SWT.FILL
   * @param horizontalSpan
   *          number of column cells in the parent's grid layout that the
   *          control will take up.
   * @param text
   *          title text to display on the group
   * @param numColumns
   *          the number of columns in the grid inside the group
   */
  private static Group createGroup(Composite parent, int horizontalAlignment, int horizontalSpan, String text,
      int numColumns) {
    Group gr = new Group(parent, SWT.NONE);
    gr.setLayout(new GridLayout(numColumns, false));
    gr.setText(text);
    GridData gd = new GridData(horizontalAlignment, SWT.CENTER, true, false);
    gd.horizontalSpan = horizontalSpan;
    gr.setLayoutData(gd);
    return gr;
  }

  /**
   * Get provider being displayed on this Options Page.
   *
   * @return provider.
   */
  private static ILanguageSettingsProvider getProvider() {
    ILanguageSettingsProvider provider = LanguageSettingsManager
        .getWorkspaceProvider("de.marw.cmake.cdt.language.settings.providers.CompileCommandsJsonParser");
    return LanguageSettingsManager.getRawProvider(provider);
  }
}
