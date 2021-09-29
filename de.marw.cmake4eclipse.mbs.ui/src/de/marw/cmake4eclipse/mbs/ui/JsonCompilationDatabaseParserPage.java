/*******************************************************************************
 * Copyright (c) 2021 Martin Weber
.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake4eclipse.mbs.ui;

import org.eclipse.cdt.ui.language.settings.providers.AbstractLanguageSettingProviderOptionPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class JsonCompilationDatabaseParserPage extends AbstractLanguageSettingProviderOptionPage {

  @Override
  public void createControl(Composite parent) {
    final Composite composite = new Composite(parent, SWT.NONE);
    {
      GridLayout layout = new GridLayout();
      layout.numColumns = 1;
      layout.marginWidth = 1;
      layout.marginHeight = 1;
      layout.marginRight = 1;
      composite.setLayout(layout);
      composite.setLayoutData(new GridData(GridData.FILL_BOTH));
    }
    createLink(composite, "Configure Workspace Settings...");
    setControl(composite);
  }

  private Link createLink(Composite composite, String text) {
    Link link = new Link(composite, SWT.NONE);
    link.setFont(composite.getFont());
    link.setText("<A>" + text + "</A>"); //$NON-NLS-1$//$NON-NLS-2$
    link.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        doLinkActivated((Link) e.widget);
      }

      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        doLinkActivated((Link) e.widget);
      }
    });
    return link;
  }

  /**
   * Handle link activation.
   *
   * @param link the link
   */
  private void doLinkActivated(Link link) {
    String id = "org.eclipse.cdt.jsoncdb.core.ui.JsonCdbPreferencePage";
    PreferencesUtil.createPreferenceDialogOn(getShell(), id, new String[] { id }, null).open();
  }
}
