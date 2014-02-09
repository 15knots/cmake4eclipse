/*******************************************************************************
 * Copyright (c) 2013 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core.internal;

import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import de.marw.cdt.cmake.core.CMakePlugin;

/**
 * Holds preferences or project settings.
 *
 * @author Martin Weber
 */
public class CMakePreferences {

  /** storage ID used to store settings or preferences with a ICConfigurationDescription */
  public static final String CFG_STORAGE_ID = CMakePlugin.PLUGIN_ID + ".cmakeSettings";

  private static final String ATTR_COMMAND = "command";

  private String command;

  /**
   * Creates a new object, initialized with all default values.
   */
  public CMakePreferences() {
    reset();
  }

  /**
   * Sets each value to its default.
   */
  public void reset() {
    setCommand("cmake");
  }

  /**
   * Initializes the configuration information from the storage element
   * specified in the argument.
   *
   * @param element
   *        A storage element containing the configuration information. If
   *        {@code null}, nothing is loaded from storage.
   */
  public void loadFromStorage(ICStorageElement element) {
    if (element == null)
      return;
    String val;
    // command
    val = element.getAttribute(ATTR_COMMAND);
    if (val != null)
      setCommand(val);
  }

  /**
   * Initializes the configuration information from the eclipse runtime
   * preferences.
   */
  public void loadFromPrefs() {
    IEclipsePreferences store = InstanceScope.INSTANCE
        .getNode(CMakePlugin.PLUGIN_ID);
    String val;
    val = store.get(ATTR_COMMAND, null);
    if (val != null)
      setCommand(val);
  }

  /**
   * Persists this configuration to project file.
   */
  public void saveToStorage(ICStorageElement element) {
//          element.setAttribute(IBuildObject.ID, id);

    element.setAttribute(ATTR_COMMAND, command);
//    element.createChild("FORGL");
  }

  /**
   * Persists this configuration to the eclipse runtime preferences.
   */
  public void saveToPrefs() {
    IEclipsePreferences store = InstanceScope.INSTANCE
        .getNode(CMakePlugin.PLUGIN_ID);

    store.put(ATTR_COMMAND, command);
  }

  /**
   * Gets the cmake command.
   */
  public final String getCommand() {
    return command;
  }

  /**
   * Sets the cmake command.
   */
  public void setCommand(String command) {
    if (command == null) {
      throw new NullPointerException("command");
    }
    this.command = command;
  }
}
