/*******************************************************************************
 * Copyright (c) 2013-2023 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;

import de.marw.cmake4eclipse.mbs.internal.Activator;
import de.marw.cmake4eclipse.mbs.internal.storage.CMakeDefineSerializer;
import de.marw.cmake4eclipse.mbs.internal.storage.CMakeUndefineSerializer;
import de.marw.cmake4eclipse.mbs.internal.storage.Util;

/**
 * Holds per-configuration project settings.
 *
 * @author Martin Weber
 */
public class CMakeSettings {

  /**
   * storage ID used to store settings or preferences with a
   * ICConfigurationDescription
   */
  public static final String CFG_STORAGE_ID = Activator.PLUGIN_ID + ".settings";
  /**
   * Attribute to store the folder of the top-level CMakeLists.txt file, used on the project level
   */
  public static final String ATTR_CMAKELISTS_FLDR= "cmakelistsFolder";
  /**
   * Element to store the build targets, used on the project level
   */
  public static final String ELEM_BUILD_TARGETS= "targets";

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
  private static final String ATTR_OTHER_ARGUMENTS = "otherArguments";
  /** the 'dirty' time stamp (in milliseconds) */
  private static final String ATTR_DIRTY_TS = "dirtyTs";

  private boolean warnNoDev, debugTryCompile, debugOutput, trace,
      warnUnitialized, warnUnused;

  private List<CmakeDefine> defines = new ArrayList<>(0);
  private List<CmakeUnDefine> undefines = new ArrayList<>(0);
  private String buildDirectory;
  private String cacheFile;
  private String otherArguments;

  private LinuxSettings linuxSettings = new LinuxSettings();

  private WindowsSettings windowsSettings = new WindowsSettings();
  private boolean clearCache;
  private long dirty_ts;

  /**
   * Creates a new object, initialized with all default values.
   */
  public CMakeSettings() {
    reset();
  }

  /** Gets the 'dirty' time stamp (in milliseconds).
   */
  public long getDirtyTs() {
    return dirty_ts;
  }

  /**
   * Sets each value to its default.
   */
  public void reset() {
    dirty_ts = System.currentTimeMillis();
    warnNoDev = false;
    debugTryCompile = false;
    debugOutput = false;
    trace = false;
    warnUnitialized = false;
    warnUnused = false;
    defines.clear();
    undefines.clear();
    buildDirectory = "_build/${ConfigName}";
    cacheFile = null;
    otherArguments = null;

//    linuxSettings.reset();
//    windowsSettings.reset();
  }

  /**
   * Initializes this object for the specified configuration.
   *
   * @param cfgd the configuration to load the preferences for. If {@code null}, nothing is loaded.
   */
  void load(ICConfigurationDescription cfgd) throws CoreException {
    if (cfgd == null)
      return;
    ICStorageElement storage = cfgd.getStorage(CMakeSettings.CFG_STORAGE_ID, true);
    buildDirectory= storage.getAttribute(ATTR_BUILD_DIR);
    dirty_ts= Long.parseLong( Objects.requireNonNullElse( storage.getAttribute(ATTR_DIRTY_TS), "0"));

    final ICStorageElement[] children = storage.getChildren();
    for (ICStorageElement child : children) {
      if (ELEM_OPTIONS.equals(child.getName())) {
        // options...
        cacheFile = child.getAttribute(ATTR_CACHE_FILE);
        otherArguments = child.getAttribute(ATTR_OTHER_ARGUMENTS);

        clearCache= Boolean.parseBoolean(child.getAttribute(ATTR_CLEAR_CACHE));
        warnNoDev = Boolean.parseBoolean(child.getAttribute(ATTR_WARN_NO_DEV));
        debugTryCompile = Boolean.parseBoolean(child.getAttribute(ATTR_DEBUG_TRYCOMPILE));
        debugOutput = Boolean.parseBoolean(child.getAttribute(ATTR_DEBUG));
        trace = Boolean.parseBoolean(child.getAttribute(ATTR_TRACE));
        warnUnitialized = Boolean.parseBoolean(child.getAttribute(ATTR_WARN_UNITIALIZED));
        warnUnused = Boolean.parseBoolean(child.getAttribute(ATTR_WARN_UNUSED));
      } else if (ELEM_DEFINES.equals(child.getName())) {
        // defines...
        Util.deserializeCollection(defines, new CMakeDefineSerializer(), child);
      } else if (ELEM_UNDEFINES.equals(child.getName())) {
        // undefines...
        Util.deserializeCollection(undefines, new CMakeUndefineSerializer(),
            child);
      }
    }
    linuxSettings.loadFromStorage(storage);
    windowsSettings.loadFromStorage(storage);
  }

  /**
   * Persists this configuration to the project file.
   */
  public void saveToStorage(ICStorageElement parent) {
    setOrRemoveAttribute(parent, ATTR_BUILD_DIR, buildDirectory);
    parent.setAttribute(ATTR_DIRTY_TS, String.valueOf(dirty_ts));

    ICStorageElement pOpts;
    ICStorageElement[] options = parent.getChildrenByName(ELEM_OPTIONS);
    if (options.length > 0) {
      pOpts = options[0];
    } else {
      pOpts = parent.createChild(ELEM_OPTIONS);
    }

    setOrRemoveAttribute(pOpts, ATTR_CACHE_FILE, cacheFile);
    setOrRemoveAttribute(pOpts, ATTR_OTHER_ARGUMENTS, otherArguments);

    // continue to load/save deprecated properties to allow users to migrate back to older versions of cmake4eclipse
    setOrRemoveAttribute(pOpts, ATTR_CLEAR_CACHE, clearCache);
    setOrRemoveAttribute(pOpts, ATTR_WARN_NO_DEV, warnNoDev);
    setOrRemoveAttribute(pOpts, ATTR_DEBUG_TRYCOMPILE, debugTryCompile);
    setOrRemoveAttribute(pOpts, ATTR_DEBUG, debugOutput);
    setOrRemoveAttribute(pOpts, ATTR_TRACE, trace);
    setOrRemoveAttribute(pOpts, ATTR_WARN_UNITIALIZED, warnUnitialized);
    setOrRemoveAttribute(pOpts, ATTR_WARN_UNUSED, warnUnused);

    // defines...
    Util.serializeCollection(ELEM_DEFINES, parent, new CMakeDefineSerializer(), defines);
    // undefines...
    Util.serializeCollection(ELEM_UNDEFINES, parent, new CMakeUndefineSerializer(), undefines);
  }

  /**
   * Sets the specified attribute in the parent element or removes it when the value is <code>false</code>.
   *
   * @param parent
   *                  the parent element for the attribute
   * @param attribute
   *                  the name of the attribute to set or remove
   * @param value
   *                  the attribute value to set
   */
  private void setOrRemoveAttribute(ICStorageElement parent, String attribute, boolean value) {
    if (value) {
      parent.setAttribute(attribute, String.valueOf(value));
    } else {
      parent.removeAttribute(ATTR_DEBUG);
    }
  }

  /**
   * Sets the specified attribute in the parent element or removes it when the value is <code>null</code>.
   *
   * @param parent
   *                  the parent element for the attribute
   * @param attribute
   *                  the name of the attribute to set or remove
   * @param value
   *                  the attribute value to set
   */
  private void setOrRemoveAttribute(ICStorageElement parent, String attribute, String value) {
    if (value != null) {
      parent.setAttribute(attribute, value);
    } else {
      parent.removeAttribute(ATTR_DEBUG);
    }
  }

  /**
   * {@code -Wno-dev}
   *
   * @deprecated no longer used
   */
  @Deprecated
  public boolean isWarnNoDev() {
    return warnNoDev;
  }

  /**
   * {@code -Wno-dev}
   *
   * @deprecated no longer used
   */
  @Deprecated
  public void setWarnNoDev(boolean warnNoDev) {
    this.warnNoDev = warnNoDev;
  }

  /**
   * {@code --debug-trycompile}
   *
   * @deprecated no longer used
   */
  @Deprecated
  public boolean isDebugTryCompile() {
    return debugTryCompile;
  }

  /**
   * {@code --debug-trycompile}
   *
   * @deprecated no longer used
   */
  @Deprecated
  public void setDebugTryCompile(boolean debugTryCompile) {
    this.debugTryCompile = debugTryCompile;
  }

  /**
   * {@code --debug-output}
   *
   * @deprecated no longer used
   */
  @Deprecated
  public boolean isDebugOutput() {
    return debugOutput;
  }

  /**
   * {@code --debug-output}
   *
   * @deprecated no longer used
   */
  @Deprecated
  public void setDebugOutput(boolean debugOutput) {
    this.debugOutput = debugOutput;
  }

  /**
   * {@code --trace}
   *
   * @deprecated no longer used
   */
  @Deprecated
  public boolean isTrace() {
    return trace;
  }

  /**
   * {@code --trace}
   *
   * @deprecated no longer used
   */
  @Deprecated
  public void setTrace(boolean trace) {
    this.trace = trace;
  }

  /**
   * {@code --warn-uninitialized}
   *
   * @deprecated no longer used
   */
  @Deprecated
  public boolean isWarnUnitialized() {
    return warnUnitialized;
  }

  /**
   * {@code --warn-uninitialized}
   *
   * @deprecated no longer used
   */
  @Deprecated
  public void setWarnUnitialized(boolean warnUnitialized) {
    this.warnUnitialized = warnUnitialized;
  }

  /**
   * {@code --warn-unused-vars}
   *
   * @deprecated no longer used
   */
  @Deprecated
  public boolean isWarnUnused() {
    return warnUnused;
  }

  /**
   * {@code --warn-unused-vars}
   *
   * @deprecated no longer used
   */
  @Deprecated
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
    if(!defines.equals(newDefines)) {
      dirty_ts= System.currentTimeMillis();
    }
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
    if(!undefines.equals(newDefines)) {
      dirty_ts= System.currentTimeMillis();
    }
    undefines.clear();
    for (CmakeUnDefine def : newDefines) {
      undefines.add(def.clone());
    }
  }

  public LinuxSettings getLinuxPreferences() {
    return linuxSettings;
  }

  public WindowsSettings getWindowsPreferences() {
    return windowsSettings;
  }

  /**
   * Gets the name of the file that is used to pre-populate the cmake cache.
   * {@code -C}
   *
   * @return the file name or {@code null} if the cmake cache shall not be
   *         pre-populated.
   */
  @Nullable
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
  public void setCacheFile(@Nullable String cacheFile) {
    if(! Objects.equals(cacheFile, this.cacheFile)) {
      dirty_ts= System.currentTimeMillis();
    }
    this.cacheFile= cacheFile;
  }

  /**
   * Gets the name of the build directory.
   *
   * @return the build directory name or {@code null} if the hard-coded default shall be
   *         used.
   */
  @Nullable public String getBuildDirectory() {
    return buildDirectory;
  }

  /**
   * Sets the name of the build directory.
   *
   * @param buildDirectory
   *          the build directory name or {@code null} if the hard-coded
   *          default shall be used.
   */
  public void setBuildDirectory(@Nullable String buildDirectory) {
    this.buildDirectory = buildDirectory;
  }

  /**
   * Gets the arbitrary arguments for cmake.
   *
   * @return the arbitrary arguments for cmake or {@code null} if none
   */
  @Nullable public String getOtherArguments() {
    return otherArguments;
  }

  /**
   * Sets the arbitrary arguments for cmake.
   *
   * @param arguments
   *          the arbitrary arguments for cmake or {@code null} if none.
   */
  public void setOtherArguments(@Nullable String arguments) {
    this.otherArguments = arguments;
  }

  /**
   * Gets whether to clear the cmake-cache before build.
   *
   * @deprecated no longer used
   */
  @Deprecated
  public boolean isClearCache() {
    return clearCache;
  }

  /** Sets whether to clear the cmake-cache before build.
   *
   * @deprecated no longer used
   */
  @Deprecated
  public void setClearCache(boolean clearCache) {
    this.clearCache= clearCache;
  }
}
