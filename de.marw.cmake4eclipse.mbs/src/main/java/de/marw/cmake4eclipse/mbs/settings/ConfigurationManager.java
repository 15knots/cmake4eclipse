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
 * CMakePreferences objects in order to avoid redundant de-serialization from
 * storage.
 *
 * @author Martin Weber
 */
public final class ConfigurationManager {
  private static ConfigurationManager instance;

  /** caches CMakePreferences by ICConfigurationDescription.ID */
  private WeakHashMap<String, CMakePreferences> map = new WeakHashMap<>(
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
   * Gets the {@code CMakePreferences} object associated with the specified
   * {@code ICConfigurationDescription}.
   *
   * @return the stored {@code CMakePreferences} object, or {@code null} if this
   *         object contains no mapping for the configuration description
   */
  public CMakePreferences get(ICConfigurationDescription cfgd) {
    return map.get(cfgd.getId());
  }

  /**
   * Tries to get the {@code CMakePreferences} object associated with the
   * specified {@code ICConfigurationDescription}. If no
   * {@code CMakePreferences} object is found, a new one is created.
   *
   * @return the stored {@code CMakePreferences} object, or a newly created one
   *         if this object contains no mapping for the configuration
   *         description.
   */
  public CMakePreferences getOrCreate(ICConfigurationDescription cfgd) {
    CMakePreferences pref = get(cfgd);
    if (pref == null) {
      pref = new CMakePreferences();
      map.put(cfgd.getId(), pref);
    }
    return pref;
  }

  /**
   * Tries to get the {@code CMakePreferences} object associated with the
   * specified {@code ICConfigurationDescription}. If no
   * {@code CMakePreferences} object is found, a new one is created, then loaded
   * from its storage via {@link CMakePreferences#loadFromStorage}.
   *
   * @return the stored {@code CMakePreferences} object, or a freshly loaded one
   *         if this object contains no mapping for the configuration
   *         description.
   * @throws CoreException
   *         if {@link ICConfigurationDescription#getStorage} throws a
   *         CoreException.
   */
  public CMakePreferences getOrLoad(ICConfigurationDescription cfgd)
      throws CoreException {
    CMakePreferences pref = map.get(cfgd.getId());
    if (pref == null) {
      pref = new CMakePreferences();
      pref.load(cfgd);
      map.put(cfgd.getId(), pref);
    }
    return pref;
  }
}
