/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.settings;

import java.util.WeakHashMap;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.runtime.CoreException;

/**
 * Associates {@link ICConfigurationDescription} objects with our
 * CMakeSettings objects in order to avoid redundant de-serialization from
 * storage.
 *
 * @author Martin Weber
 */
public final class ConfigurationManager {
  private static ConfigurationManager instance;

  /** caches CMakeSettings by ICConfigurationDescription.ID */
  private WeakHashMap<String, CMakeSettings> map = new WeakHashMap<>(
      2);

  /**
   * Singleton constructor.
   */
  private ConfigurationManager() {
  }

  /**
   * Gets the singleton instance.
   */
  public static synchronized ConfigurationManager getInstance() {
    if (instance == null)
      instance = new ConfigurationManager();
    return instance;
  }

  /**
   * Gets the {@code CMakeSettings} object associated with the specified
   * {@code ICConfigurationDescription}.
   *
   * @return the stored {@code CMakeSettings} object, or {@code null} if this
   *         object contains no mapping for the configuration description
   */
  public CMakeSettings get(ICConfigurationDescription cfgd) {
    return map.get(cfgd.getId());
  }

  /**
   * Tries to get the {@code CMakeSettings} object associated with the
   * specified {@code ICConfigurationDescription}. If no
   * {@code CMakeSettings} object is found, a new one is created.
   *
   * @return the stored {@code CMakeSettings} object, or a newly created one
   *         if this object contains no mapping for the configuration
   *         description.
   */
  public CMakeSettings getOrCreate(ICConfigurationDescription cfgd) {
    CMakeSettings pref = get(cfgd);
    if (pref == null) {
      pref = new CMakeSettings();
      map.put(cfgd.getId(), pref);
    }
    return pref;
  }

  /**
   * Tries to get the {@code CMakeSettings} object associated with the
   * specified {@code ICConfigurationDescription}. If no
   * {@code CMakeSettings} object is found, a new one is created, then loaded
   * from its storage via {@link CMakeSettings#load}.
   *
   * @return the stored {@code CMakeSettings} object, or a freshly loaded one
   *         if this object contains no mapping for the configuration
   *         description.
   * @throws CoreException
   *         if {@link ICConfigurationDescription#getStorage} throws a
   *         CoreException.
   */
  public CMakeSettings getOrLoad(ICConfigurationDescription cfgd)
      throws CoreException {
    CMakeSettings pref = map.get(cfgd.getId());
    if (pref == null) {
      pref = new CMakeSettings();
      if (!cfgd.getProjectDescription().isCdtProjectCreating()) {
        // do not clobber default values when a project is creating
        pref.load(cfgd);
      }
      map.put(cfgd.getId(), pref);
    }
    return pref;
  }
}
