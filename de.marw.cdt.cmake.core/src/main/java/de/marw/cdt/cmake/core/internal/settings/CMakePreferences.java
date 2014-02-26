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
package de.marw.cdt.cmake.core.internal.settings;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import de.marw.cdt.cmake.core.CMakePlugin;
import de.marw.cdt.cmake.core.internal.storage.CmakeDefineSerializer;
import de.marw.cdt.cmake.core.internal.storage.CmakeUndefineSerializer;
import de.marw.cdt.cmake.core.internal.storage.Util;

/**
 * Holds preferences or project settings.
 *
 * @author Martin Weber
 */
public class CMakePreferences {

  /**
   * storage ID used to store settings or preferences with a
   * ICConfigurationDescription
   */
  public static final String CFG_STORAGE_ID = CMakePlugin.PLUGIN_ID
      + ".settings";
  private List<CmakeDefine> defines = new ArrayList<CmakeDefine>(0);
  private List<CmakeUnDefine> undefines = new ArrayList<CmakeUnDefine>(0);

  private LinuxPreferences linuxPreferences = new LinuxPreferences();

  private WindowsPreferences windowsPreferences = new WindowsPreferences();
  /**  */
  static final String ELEM_DEFINES = "defs";
  /**  */
  static final String ELEM_UNDEFINES = "undefs";

  /**
   * Creates a new object, initialized with all default values.
   */
  public CMakePreferences() {
//    reset();
  }

  /**
   * Sets each value to its default.
   */
  public void reset() {
    defines.clear();
    undefines.clear();
    linuxPreferences.reset();
    windowsPreferences.reset();
//    setCommand("cmake");
  }

  /**
   * Initializes the configuration information from the storage element
   * specified in the argument.
   *
   * @param parent
   *        A storage element containing the configuration information. If
   *        {@code null}, nothing is loaded from storage.
   */
  public void loadFromStorage(ICStorageElement parent) {
    if (parent == null)
      return;
    ICStorageElement[] children = parent.getChildren();
    for (ICStorageElement child : children) {
      if (ELEM_DEFINES.equals(child.getName())) {
        // defines...
        Util.deserializeCollection(defines, new CmakeDefineSerializer(), parent);
      } else if (ELEM_UNDEFINES.equals(child.getName())) {
        // undefines...
        Util.deserializeCollection(undefines, new CmakeUndefineSerializer(), parent);
      }
    }
    linuxPreferences.loadFromStorage(parent);
    windowsPreferences.loadFromStorage(parent);
  }

  /**
   * Persists this configuration to the project file.
   */
  public void saveToStorage(ICStorageElement parent) {
    // defines...
    Util.serializeCollection(ELEM_DEFINES, parent, new CmakeDefineSerializer(),
        defines);
    // undefines...
    Util.serializeCollection(ELEM_UNDEFINES, parent, new CmakeUndefineSerializer(),
        undefines);
    linuxPreferences.saveToStorage(parent);
//    windowsPreferences.saveToStorage(parent);
  }

  /**
   * Initializes the configuration information from the eclipse runtime
   * preferences.
   */
  public void loadFromPrefs() {
    IEclipsePreferences store = InstanceScope.INSTANCE
        .getNode(CMakePlugin.PLUGIN_ID);
//    String val;
//    val = store.get(ATTR_COMMAND, null);
//    if (val != null)
//      setCommand(val);
  }

  /**
   * Persists this configuration to the eclipse runtime preferences.
   */
  public void saveToPrefs() {
    IEclipsePreferences store = InstanceScope.INSTANCE
        .getNode(CMakePlugin.PLUGIN_ID);
//    store.put(ATTR_COMMAND, command);
  }

  public LinuxPreferences getLinuxPreferences() {
    return linuxPreferences;
  }

  public WindowsPreferences getWindowsPreferences() {
    return windowsPreferences;
  }

  /**
   * Gets the list of cmake variable to define on the cmake command-line.
   *
   * @return a mutable list, never {@code null}
   */
  public List<CmakeDefine> getDefines() {
    return defines;
  }

  /**
   * Gets the list of cmake variable to undefine on the cmake command-line.
   *
   * @return a mutable list, never {@code null}
   */
  public List<CmakeUnDefine> getUndefines() {
    return undefines;
  }

  /**
   * @return
   */
  public String getCommand() {
    return "kannwech";
  }
}
