package de.marw.cmake4eclipse.mbs.ui.slim;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;

public class EditBuildTargetsActionProvider extends CommonActionProvider {

  private EditBuildTargetsAction action;

  @Override
  public void init(ICommonActionExtensionSite aSite) {
    super.init(aSite);
    action = new EditBuildTargetsAction(aSite.getViewSite().getShell());
  }

  @Override
  public void fillContextMenu(IMenuManager menu) {
    menu.add(action);
  }

  @Override
  public void setContext(ActionContext context) {
    super.setContext(context);
    if (context != null) {
      IStructuredSelection selection = (IStructuredSelection) context.getSelection();
      action.selectionChanged(selection);
    }
  }
}
