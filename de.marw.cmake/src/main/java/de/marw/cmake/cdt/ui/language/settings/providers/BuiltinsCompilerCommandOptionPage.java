/*******************************************************************************
 * Copyright (c) 2018 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cmake.cdt.ui.language.settings.providers;

import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsManager;
import org.eclipse.cdt.ui.dialogs.AbstractCOptionPage;
import org.eclipse.cdt.ui.language.settings.providers.AbstractLanguageSettingProviderOptionPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import de.marw.cmake.cdt.language.settings.providers.BuiltinsCompileCommandsJsonParser;

public class BuiltinsCompilerCommandOptionPage extends AbstractLanguageSettingProviderOptionPage {
  private Button b_withConsole;

  @Override
  public void createControl(Composite parent) {
    final boolean enabled = parent.isEnabled();
    final BuiltinsCompileCommandsJsonParser provider = (BuiltinsCompileCommandsJsonParser) getProvider();

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
    b_withConsole = CompileCommandsJsonParserOptionPage.createCheckbox(composite, SWT.BEGINNING, 2,
        "&Allocate console in the Console View");
    b_withConsole.setEnabled(enabled);
    b_withConsole.setSelection(provider.isWithConsole());
    b_withConsole.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        provider.setWithConsole(((Button) event.widget).getSelection());
      }
    });

    setControl(composite);
  }

  @Override
  public void performApply(IProgressMonitor monitor) throws CoreException {
  }

  @Override
  public void performDefaults() {
    b_withConsole.setSelection(false);
  }
}
