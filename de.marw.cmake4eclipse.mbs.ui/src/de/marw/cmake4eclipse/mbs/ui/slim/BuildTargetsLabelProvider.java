/*******************************************************************************
 * Copyright (c) 2022 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake4eclipse.mbs.ui.slim;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import de.marw.cmake4eclipse.mbs.settings.BuildTarget;

/**
 * @author Martin Weber
 */
public class BuildTargetsLabelProvider extends LabelProvider {
  private ResourceManager resourceManager;

  /**
   */
  public BuildTargetsLabelProvider() {
    this.resourceManager = new LocalResourceManager(JFaceResources.getResources());
  }

  @Override
  public String getText(Object element) {
    if (element instanceof BuildTargetsContainer) {
      return "Build Targets";
    } else if (element instanceof NavBuildTarget) {
      BuildTarget tgt = (BuildTarget) element;
      return tgt.getName();
    }
    return super.getText(element);
  }

  @Override
  public Image getImage(Object element) {
    if (element instanceof BuildTargetsContainer || element instanceof NavBuildTarget) {
      URL url = null;
//      Bundle bundle = FrameworkUtil.getBundle(this.getClass());
      // FileLocator.find(bundle, new Path("icons/task.png"), null);
      try {
        url = new URL ("platform:/plugin/org.eclipse.cdt.managedbuilder.ui/icons/obj16/target_obj.gif");
      } catch (MalformedURLException ignore) {
      }
      ImageDescriptor imageDescriptor = ImageDescriptor.createFromURL(url);
      return resourceManager.createImage(imageDescriptor);
    }
    return null;
  }

  @Override
  public void dispose() {
      super.dispose();
      resourceManager.dispose();
  }
}
