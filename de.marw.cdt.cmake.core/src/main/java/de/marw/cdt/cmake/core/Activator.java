/*******************************************************************************
 * Copyright (c) 2013 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core;

import java.text.MessageFormat;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * @author Martin Weber
 */
public class Activator extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "de.marw.cdt.cmake.core"; //$NON-NLS-1$
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

  /**
   * Returns the string from the plugin's resource bundle, or 'key' if not
   * found.
   *
   * @param key
   *        the message key
   * @return the resource bundle message
   */
  public static String getResourceString(String key) {
//		ResourceBundle bundle = CdtPlugin.getDefault().getResourceBundle();
//		try {
//			return bundle.getString(key);
//		} catch (MissingResourceException e) {
//			return key;
//		}
    return key;
  }

  /**
   * Returns the string from the plugin's resource bundle, or 'key' if not
   * found.
   *
   * @param key
   *        the message key
   * @param args
   *        an array of substitution strings
   * @return the resource bundle message
   */
  public static String getFormattedString(String key, String[] args) {
    return MessageFormat.format(getResourceString(key), (Object[]) args);
  }
}
