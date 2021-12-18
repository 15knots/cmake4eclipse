/*******************************************************************************
 * Copyright (c) 2013 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.internal;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * @author Martin Weber
 */
public class Activator extends Plugin {

  public static final String PLUGIN_ID = "de.marw.cmake4eclipse.mbs"; //$NON-NLS-1$
  /** extension id of the cmake-generated makefile builder */
  public static final String BUILDER_ID = Activator.PLUGIN_ID
      + "." + "genmakebuilder"; //$NON-NLS-1$

  //The shared instance.
  private static Activator plugin;

  /**
   * The constructor.
   */
  public Activator() {
  }

  /**
   * This method is called upon plug-in activation
   */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    if (!PLUGIN_ID.equals(this.getBundle().getSymbolicName()))
      throw new RuntimeException(
          "BUG: PLUGIN_ID does not match Bundle-SymbolicName");
    plugin = this;
  }

  /**
   * This method is called when the plug-in is stopped
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
    plugin = null;
  }

  /**
   * Returns the shared instance.
   */
  public static Activator getDefault() {
    return plugin;
  }
}
