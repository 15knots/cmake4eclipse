/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.settings;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.core.runtime.Platform;

import de.marw.cmake4eclipse.mbs.internal.storage.CMakeDefineSerializer;
import de.marw.cmake4eclipse.mbs.internal.storage.CMakeUndefineSerializer;
import de.marw.cmake4eclipse.mbs.internal.storage.Util;

/**
 * Preferences that override/augment the generic properties when running under a specific OS.
 *
 * @author Martin Weber
 *
 * @deprecated kept only to not break persistence of eclipse projects created with versions of this plugin prior to 3.0
 */
@Deprecated
public abstract class AbstractOsPreferences {
  private static final String ATTR_COMMAND = "command";
  private static final String ATTR_USE_DEFAULT_COMMAND = "use-default";
  private static final String ATTR_GENERATOR = "generator";

  private String command;
  private CmakeGenerator generator;
  private boolean useDefaultCommand;
  private List<CmakeDefine> defines = new ArrayList<>(0);
  private List<CmakeUnDefine> undefines = new ArrayList<>(0);
  private CmakeGenerator generatedWith;

  /**
   * Creates a new object, initialized with all default values.
   */
  public AbstractOsPreferences() {
    reset();
    // after startup: assume the makefiles have been generated with the saved generator
    generatedWith = generator;
  }

  /**
   * Gets the name of the storage element that store this preferences.
   */
  protected abstract String getStorageElementName();

  /**
   * Sets each value to its default.
   */
  public void reset() {
    useDefaultCommand = true;
    setCommand("cmake");
    setGenerator(CmakeGenerator.UnixMakefiles);
    defines.clear();
    undefines.clear();
  }

  /**
   * Gets the platform specific preferences object from the specified
   * preferences for the current operating system (the OS we are running under).
   * If the OS cannot be determined or its specific preferences are not
   * implemented, the platform specific preferences for Linux are returned as a
   * fall-back.
   *
   * @return the platform specific or fall-back preferences
   */
  public static AbstractOsPreferences extractOsPreferences(
      CMakePreferences prefs) {
    final String os = Platform.getOS();
    if (Platform.OS_WIN32.equals(os)) {
      return prefs.getWindowsPreferences();
    } else {
      // fall back to linux, if OS is unknown
      return prefs.getLinuxPreferences();
    }
  }

  /**
   * Gets whether to use the default cmake command.
   */
  public final boolean getUseDefaultCommand() {
    return useDefaultCommand;
  }

  /**
   * Sets whether to use the default cmake command.
   */
  public void setUseDefaultCommand(boolean useDefaultCommand) {
    this.useDefaultCommand = useDefaultCommand;
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

  /**
   * Gets the cmake buildscript generator.
   */
  public final CmakeGenerator getGenerator() {
    return generator;
  }

  /**
   * Sets the cmake build-script generator.
   */
  public void setGenerator(CmakeGenerator generator) {
    if (generator == null) {
      throw new NullPointerException("generator");
    }
    this.generator = generator;
  }

  /**
   * Gets the list of cmake variable to define on the cmake command-line.
   *
   * @return a mutable list, never {@code null}
   */
  public final List<CmakeDefine> getDefines() {
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
   * Gets the list of cmake variable to undefine on the cmake command-line.
   *
   * @return a mutable list, never {@code null}
   */
  public final List<CmakeUnDefine> getUndefines() {
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

  /**
   * Initializes the configuration information from the storage element
   * specified in the argument.
   *
   * @param parent
   *        A storage element containing the configuration information. If
   *        {@code null}, nothing is loaded from storage.
   */
  /* package */ void loadFromStorage(ICStorageElement parent) {
    if (parent == null)
      return;
    // loop to merge multiple children of the same name
    final ICStorageElement[] children = parent
        .getChildrenByName(getStorageElementName());
    for (ICStorageElement child : children) {
      loadChildFromStorage(child);
    }
  }

  private void loadChildFromStorage(ICStorageElement parent) {
    String val;
    // use default command
    val = parent.getAttribute(ATTR_USE_DEFAULT_COMMAND);
    useDefaultCommand = Boolean.parseBoolean(val);
    // command
    val = parent.getAttribute(ATTR_COMMAND);
    if (val != null)
      setCommand(val);
    // generator
    val = parent.getAttribute(ATTR_GENERATOR);
    if (val != null) {
      try {
        setGenerator(CmakeGenerator.valueOf(val));
      } catch (IllegalArgumentException ex) {
        // fall back to default generator
      }
    }

    ICStorageElement[] children = parent.getChildren();
    for (ICStorageElement child : children) {
      if (CMakePreferences.ELEM_DEFINES.equals(child.getName())) {
        // defines...
        Util.deserializeCollection(defines, new CMakeDefineSerializer(), child);
      } else if (CMakePreferences.ELEM_UNDEFINES.equals(child.getName())) {
        // undefines...
        Util.deserializeCollection(undefines, new CMakeUndefineSerializer(),
            child);
      }
    }
  }

  /**
   * Persists this configuration to the project file.
   */
  public void saveToStorage(ICStorageElement parent) {
    final String storageElementName = getStorageElementName();

    final ICStorageElement[] children = parent
        .getChildrenByName(storageElementName);
    if (children.length == 0) {
      parent = parent.createChild(storageElementName);
    } else {
      // take first child
      parent = children[0];
    }

    // use default command
    if (useDefaultCommand) {
      parent.setAttribute(ATTR_USE_DEFAULT_COMMAND,
          String.valueOf(useDefaultCommand));
    } else {
      parent.removeAttribute(ATTR_USE_DEFAULT_COMMAND);
    }
    parent.setAttribute(ATTR_COMMAND, command);
    // generator
    parent.setAttribute(ATTR_GENERATOR, generator.name());
    // defines...
    Util.serializeCollection(CMakePreferences.ELEM_DEFINES, parent,
        new CMakeDefineSerializer(), defines);
    // undefines...
    Util.serializeCollection(CMakePreferences.ELEM_UNDEFINES, parent,
        new CMakeUndefineSerializer(), undefines);
  }

  /**
   * Gets the generator that was used to generate the CMake cache file and the
   * makefiles. This property is not persisted.
   */
  public CmakeGenerator getGeneratedWith() {
    return this.generatedWith;
  }

  /**
   * Sets the generator that was used to generate the CMake cache file and the
   * makefiles.
   */
  public void setGeneratedWith(CmakeGenerator generator) {
    this.generatedWith = generator;
  }

}