/*******************************************************************************
 * Copyright (c) 2013-2019 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core.settings;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.settings.model.ICStorageElement;

import de.marw.cdt.cmake.core.internal.Activator;
import de.marw.cdt.cmake.core.internal.storage.CmakeDefineSerializer;
import de.marw.cdt.cmake.core.internal.storage.CmakeUndefineSerializer;
import de.marw.cdt.cmake.core.internal.storage.Util;

/**
 * Holds per-configuration project settings.
 *
 * @author Martin Weber
 */
public class CMakePreferences {

  /**
   * storage ID used to store settings or preferences with a
   * ICConfigurationDescription
   */
  public static final String CFG_STORAGE_ID = Activator.PLUGIN_ID
      + ".settings";
  private static final String ATTR_WARN_NO_DEV = "warnNoDev";
  private static final String ATTR_DEBUG_TRYCOMPILE = "debugTryCompile";
  private static final String ATTR_DEBUG = "debugOutput";
  private static final String ATTR_TRACE = "trace";
  private static final String ATTR_WARN_UNITIALIZED = "warnUnitialized";
  private static final String ATTR_WARN_UNUSED = "warnUnused";
  private static final String ATTR_CLEAR_CACHE = "clearCache";
  /**  */
  static final String ELEM_DEFINES = "defs";
  /**  */
  static final String ELEM_UNDEFINES = "undefs";
  private static final String ELEM_OPTIONS = "options";
  private static final String ATTR_CACHE_FILE = "cacheEntriesFile";
  private static final String ATTR_BUILD_DIR = "buildDir";

  private boolean warnNoDev, debugTryCompile, debugOutput, trace,
      warnUnitialized, warnUnused;

  private List<CmakeDefine> defines = new ArrayList<>(0);
  private List<CmakeUnDefine> undefines = new ArrayList<>(0);
  private String buildDirectory;
  private String cacheFile;

  private LinuxPreferences linuxPreferences = new LinuxPreferences();

  private WindowsPreferences windowsPreferences = new WindowsPreferences();
  private boolean clearCache;

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
    cacheFile= null;

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
        clearCache= Boolean.parseBoolean(child.getAttribute(ATTR_CLEAR_CACHE));
        // options...
        warnNoDev = Boolean.parseBoolean(child.getAttribute(ATTR_WARN_NO_DEV));
        debugTryCompile = Boolean.parseBoolean(child
            .getAttribute(ATTR_DEBUG_TRYCOMPILE));
        debugOutput = Boolean.parseBoolean(child.getAttribute(ATTR_DEBUG));
        trace = Boolean.parseBoolean(child.getAttribute(ATTR_TRACE));
        warnUnitialized = Boolean.parseBoolean(child
            .getAttribute(ATTR_WARN_UNITIALIZED));
        warnUnused = Boolean.parseBoolean(child.getAttribute(ATTR_WARN_UNUSED));
        cacheFile = child.getAttribute(ATTR_CACHE_FILE);
        buildDirectory= parent.getAttribute(ATTR_BUILD_DIR);
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

    if (clearCache) {
      pOpts.setAttribute(ATTR_CLEAR_CACHE, String.valueOf(clearCache));
    } else {
      pOpts.removeAttribute(ATTR_CLEAR_CACHE);
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
    if (cacheFile != null) {
      pOpts.setAttribute(ATTR_CACHE_FILE, cacheFile);
    } else {
      pOpts.removeAttribute(ATTR_CACHE_FILE);
    }
    if (buildDirectory!= null) {
      parent.setAttribute(ATTR_BUILD_DIR, buildDirectory);
    } else {
      parent.removeAttribute(ATTR_CACHE_FILE);
    }

    // defines...
    Util.serializeCollection(ELEM_DEFINES, parent, new CmakeDefineSerializer(),
        defines);
    // undefines...
    Util.serializeCollection(ELEM_UNDEFINES, parent,
        new CmakeUndefineSerializer(), undefines);
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
   * Gets the list of cmake variables to define on the cmake command-line.
   *
   * @return a mutable list, never {@code null}
   */
  public List<CmakeDefine> getDefines() {
    return new ArrayList<>(defines);
  }

  /**
   * Replaces the list of cmake variables to define with the specified list.
   */
  public void setDefines(List<CmakeDefine> newDefines) {
    defines.clear();
    for (CmakeDefine def : newDefines) {
      defines.add(def.clone());
    }
  }

  /**
   * Gets the list of cmake variables to undefine on the cmake command-line.
   *
   * @return a mutable list, never {@code null}
   */
  public List<CmakeUnDefine> getUndefines() {
    return new ArrayList<>(undefines);
  }

  /**
   * Replaces the list of cmake variables to undefine with the specified list.
   */
  public void setUndefines(List<CmakeUnDefine> newDefines) {
    undefines.clear();
    for (CmakeUnDefine def : newDefines) {
      undefines.add(def.clone());
    }
  }

  public LinuxPreferences getLinuxPreferences() {
    return linuxPreferences;
  }

  public WindowsPreferences getWindowsPreferences() {
    return windowsPreferences;
  }

  /**
   * Gets the name of the file that is used to pre-populate the cmake cache.
   * {@code -C}
   *
   * @return the file name or {@code null} if the cmake cache shall not be
   *         pre-populated.
   */
  public String getCacheFile() {
    return cacheFile;
  }

  /**
   * Sets the name of the file that is used to pre-populate the cmake cache.
   * {@code -C}
   *
   * @param cacheFile
   *          the file name or {@code null} if the cmake cache shall not be
   *          pre-populated.
   */
  public void setCacheFile(String cacheFile) {
    this.cacheFile= cacheFile;
  }

  /**
   * Gets the name of the build directory.
   *
   * @return the build directory name or {@code null} if the hard-coded default shall be
   *         used.
   */
  public String getBuildDirectory() {
    return buildDirectory;
  }

  /**
   * Sets the name of the build directory.
   *
   * @param buildDirectory
   *          the build directory name or {@code null} if the hard-coded
   *          default shall be used.
   */
  public void setBuildDirectory(String buildDirectory) {
    this.buildDirectory = buildDirectory;
  }

  /** Gets whether t clear the cmake-cache before build.
   */
  public boolean isClearCache() {
    return clearCache;
  }

  /** Sets whether t clear the cmake-cache before build.
   */
  public void setClearCache(boolean clearCache) {
    this.clearCache= clearCache;
  }
}
