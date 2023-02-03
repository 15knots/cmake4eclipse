/*******************************************************************************
 * Copyright (c) 2022 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake4eclipse.mbs.ui.slim;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;

/**
 * Provides actions for a node of type [@code BuildTargetsContainer}.
 *
 * @author Martin Weber
 */
public class BuildTargetsContainerActionProvider extends CommonActionProvider {

  private BuildTargetAction buildTargetAction;
//  private RebuildLastTargetAction buildLastTargetAction;

  @Override
  public void init(ICommonActionExtensionSite aSite) {
    super.init(aSite);

    Shell shell = aSite.getViewSite().getShell();
    buildTargetAction = new BuildTargetAction(shell);
//    buildLastTargetAction = new RebuildLastTargetAction();

    aSite.getStructuredViewer().addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(DoubleClickEvent event) {
        IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        Object element = selection.getFirstElement();
        if (element instanceof NavBuildTarget) {
          buildTargetAction.run();
        }
      }
    });
  }

  @Override
  public void fillContextMenu(IMenuManager menu) {
    menu.add(buildTargetAction);
//    menu.add(buildLastTargetAction);
  }

  @Override
  public void setContext(ActionContext context) {
    super.setContext(context);

    if (context != null) {
      IStructuredSelection selection = (IStructuredSelection) context.getSelection();
      buildTargetAction.selectionChanged(selection);
//      buildLastTargetAction.selectionChanged(selection);
    }
  }
}
