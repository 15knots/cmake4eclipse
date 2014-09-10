/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
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

import de.marw.cdt.cmake.core.internal.CmakeGenerator;
import de.marw.cdt.cmake.core.internal.storage.CmakeDefineSerializer;
import de.marw.cdt.cmake.core.internal.storage.CmakeUndefineSerializer;
import de.marw.cdt.cmake.core.internal.storage.Util;

/**
 * Preferences that override/augment the generic properties when running under a
 * specific OS.
 *
 * @author Martin Weber
 */
public abstract class AbstractOsPreferences {
  private static final String ATTR_COMMAND = "command";
  private static final String ATTR_USE_DEFAULT_COMMAND = "use-default";
  private static final String ATTR_GENERATOR = "generator";
  private static final String ATTR_BUILD_COMMAND = "build_command";

  private String command;
  private CmakeGenerator generator;
  private String buildscriptProcessorCmd;
  private boolean useDefaultCommand;
  private List<CmakeDefine> defines = new ArrayList<CmakeDefine>(0);
  private List<CmakeUnDefine> undefines = new ArrayList<CmakeUnDefine>(0);

  /**
   * Creates a new object, initialized with all default values.
   */
  public AbstractOsPreferences() {
    reset();
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
    setBuildscriptProcessorCommand(null);
    defines.clear();
    undefines.clear();
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
   * Gets the buildscript processor name.
   *
   * @return the buildscript processor or {@code null} if the build command
   *         matching the chosen generator should be used.
   */
  public String getBuildscriptProcessorCommand() {
    return buildscriptProcessorCmd;
  }

  /**
   * Sets the buildscript processor name.
   *
   * @param buildscriptProcessorCommand
   *        the buildscript processor. If {@code null} or an empty string, the
   *        build command matching the chosen generator should be used.
   */
  public void setBuildscriptProcessorCommand(String buildscriptProcessorCommand) {
    if ("".equals(buildscriptProcessorCommand))
      buildscriptProcessorCommand = null;
    this.buildscriptProcessorCmd = buildscriptProcessorCommand;
  }

  /**
   * Gets the list of cmake variable to define on the cmake command-line.
   *
   * @return a mutable list, never {@code null}
   */
  public final List<CmakeDefine> getDefines() {
    return defines;
  }

  /**
   * Gets the list of cmake variable to undefine on the cmake command-line.
   *
   * @return a mutable list, never {@code null}
   */
  public final List<CmakeUnDefine> getUndefines() {
    return undefines;
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
    val = parent.getAttribute(ATTR_BUILD_COMMAND);
//    if (val != null)
    setBuildscriptProcessorCommand(val);

    ICStorageElement[] children = parent.getChildren();
    for (ICStorageElement child : children) {
      if (CMakePreferences.ELEM_DEFINES.equals(child.getName())) {
        // defines...
        Util.deserializeCollection(defines, new CmakeDefineSerializer(), child);
      } else if (CMakePreferences.ELEM_UNDEFINES.equals(child.getName())) {
        // undefines...
        Util.deserializeCollection(undefines, new CmakeUndefineSerializer(),
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
    if (buildscriptProcessorCmd != null) {
      parent.setAttribute(ATTR_BUILD_COMMAND, buildscriptProcessorCmd);
    } else {
      parent.removeAttribute(ATTR_BUILD_COMMAND);
    }
    // defines...
    Util.serializeCollection(CMakePreferences.ELEM_DEFINES, parent,
        new CmakeDefineSerializer(), defines);
    // undefines...
    Util.serializeCollection(CMakePreferences.ELEM_UNDEFINES, parent,
        new CmakeUndefineSerializer(), undefines);
  }

}