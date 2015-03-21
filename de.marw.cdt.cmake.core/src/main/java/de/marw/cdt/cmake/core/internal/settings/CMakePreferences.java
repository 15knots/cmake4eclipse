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

import de.marw.cdt.cmake.core.CdtPlugin;
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
  public static final String CFG_STORAGE_ID = CdtPlugin.PLUGIN_ID
      + ".settings";
  private static final String ATTR_WARN_NO_DEV = "warnNoDev";
  private static final String ATTR_DEBUG_TRYCOMPILE = "debugTryCompile";
  private static final String ATTR_DEBUG = "debugOutput";
  private static final String ATTR_TRACE = "trace";
  private static final String ATTR_WARN_UNITIALIZED = "warnUnitialized";
  private static final String ATTR_WARN_UNUSED = "warnUnused";
  /**  */
  static final String ELEM_DEFINES = "defs";
  /**  */
  static final String ELEM_UNDEFINES = "undefs";
  private static final String ELEM_OPTIONS = "options";

  private boolean warnNoDev, debugTryCompile, debugOutput, trace,
      warnUnitialized, warnUnused;

  private List<CmakeDefine> defines = new ArrayList<CmakeDefine>(0);
  private List<CmakeUnDefine> undefines = new ArrayList<CmakeUnDefine>(0);

  private LinuxPreferences linuxPreferences = new LinuxPreferences();

  private WindowsPreferences windowsPreferences = new WindowsPreferences();

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
    warnNoDev = false;
    debugTryCompile = false;
    debugOutput = false;
    trace = false;
    warnUnitialized = false;
    warnUnused = false;
    defines.clear();
    undefines.clear();

//    linuxPreferences.reset();
//    windowsPreferences.reset();
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

    final ICStorageElement[] children = parent.getChildren();
    for (ICStorageElement child : children) {
      if (ELEM_OPTIONS.equals(child.getName())) {
        // options...
        warnNoDev = Boolean.parseBoolean(child.getAttribute(ATTR_WARN_NO_DEV));
        debugTryCompile = Boolean.parseBoolean(child
            .getAttribute(ATTR_DEBUG_TRYCOMPILE));
        debugOutput = Boolean.parseBoolean(child.getAttribute(ATTR_DEBUG));
        trace = Boolean.parseBoolean(child.getAttribute(ATTR_TRACE));
        warnUnitialized = Boolean.parseBoolean(child
            .getAttribute(ATTR_WARN_UNITIALIZED));
        warnUnused = Boolean.parseBoolean(child.getAttribute(ATTR_WARN_UNUSED));
      } else if (ELEM_DEFINES.equals(child.getName())) {
        // defines...
        Util.deserializeCollection(defines, new CmakeDefineSerializer(), child);
      } else if (ELEM_UNDEFINES.equals(child.getName())) {
        // undefines...
        Util.deserializeCollection(undefines, new CmakeUndefineSerializer(),
            child);
      }
    }
    linuxPreferences.loadFromStorage(parent);
    windowsPreferences.loadFromStorage(parent);
  }

  /**
   * Persists this configuration to the project file.
   */
  public void saveToStorage(ICStorageElement parent) {
    ICStorageElement pOpts;
    ICStorageElement[] options = parent.getChildrenByName(ELEM_OPTIONS);
    if (options.length > 0) {
      pOpts = options[0];
    } else {
      pOpts = parent.createChild(ELEM_OPTIONS);
    }
    if (warnNoDev) {
      pOpts.setAttribute(ATTR_WARN_NO_DEV, String.valueOf(warnNoDev));
    } else {
      pOpts.removeAttribute(ATTR_WARN_NO_DEV);
    }
    if (debugTryCompile) {
      pOpts
          .setAttribute(ATTR_DEBUG_TRYCOMPILE, String.valueOf(debugTryCompile));
    } else {
      pOpts.removeAttribute(ATTR_DEBUG_TRYCOMPILE);
    }
    if (debugOutput) {
      pOpts.setAttribute(ATTR_DEBUG, String.valueOf(debugOutput));
    } else {
      pOpts.removeAttribute(ATTR_DEBUG);
    }
    if (trace) {
      pOpts.setAttribute(ATTR_TRACE, String.valueOf(trace));
    } else {
      pOpts.removeAttribute(ATTR_TRACE);
    }
    if (warnUnitialized) {
      pOpts
          .setAttribute(ATTR_WARN_UNITIALIZED, String.valueOf(warnUnitialized));
    } else {
      pOpts.removeAttribute(ATTR_WARN_UNITIALIZED);
    }
    if (warnUnused) {
      pOpts.setAttribute(ATTR_WARN_UNUSED, String.valueOf(warnUnused));
    } else {
      pOpts.removeAttribute(ATTR_WARN_UNUSED);
    }

    // defines...
    Util.serializeCollection(ELEM_DEFINES, parent, new CmakeDefineSerializer(),
        defines);
    // undefines...
    Util.serializeCollection(ELEM_UNDEFINES, parent,
        new CmakeUndefineSerializer(), undefines);
//    linuxPreferences.saveToStorage(parent);
//    windowsPreferences.saveToStorage(parent);
  }

  /**
   * {@code -Wno-dev}
   */
  public boolean isWarnNoDev() {
    return warnNoDev;
  }

  /**
   * {@code -Wno-dev}
   */
  public void setWarnNoDev(boolean warnNoDev) {
    this.warnNoDev = warnNoDev;
  }

  /**
   * {@code --debug-trycompile}
   */
  public boolean isDebugTryCompile() {
    return debugTryCompile;
  }

  /**
   * {@code --debug-trycompile}
   */
  public void setDebugTryCompile(boolean debugTryCompile) {
    this.debugTryCompile = debugTryCompile;
  }

  /**
   * {@code --debug-output}
   */
  public boolean isDebugOutput() {
    return debugOutput;
  }

  /**
   * {@code --debug-output}
   */
  public void setDebugOutput(boolean debugOutput) {
    this.debugOutput = debugOutput;
  }

  /**
   * {@code --trace}
   */
  public boolean isTrace() {
    return trace;
  }

  /**
   * {@code --trace}
   */
  public void setTrace(boolean trace) {
    this.trace = trace;
  }

  /**
   * {@code --warn-uninitialized}
   */
  public boolean isWarnUnitialized() {
    return warnUnitialized;
  }

  /**
   * {@code --warn-uninitialized}
   */
  public void setWarnUnitialized(boolean warnUnitialized) {
    this.warnUnitialized = warnUnitialized;
  }

  /**
   * {@code --warn-unused-vars}
   */
  public boolean isWarnUnused() {
    return warnUnused;
  }

  /**
   * {@code --warn-unused-vars}
   */
  public void setWarnUnused(boolean warnUnused) {
    this.warnUnused = warnUnused;
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

  public LinuxPreferences getLinuxPreferences() {
    return linuxPreferences;
  }

  public WindowsPreferences getWindowsPreferences() {
    return windowsPreferences;
  }

}
