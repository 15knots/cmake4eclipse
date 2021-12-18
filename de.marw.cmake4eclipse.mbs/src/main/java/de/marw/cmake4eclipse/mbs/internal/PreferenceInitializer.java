/*******************************************************************************
 * Copyright (c) 2021 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.internal;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import de.marw.cmake4eclipse.mbs.preferences.PreferenceAccess;
import de.marw.cmake4eclipse.mbs.settings.CmakeGenerator;

/**
 * Class used to initialize default workbench preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {
  @Override
  public void initializeDefaultPreferences() {
    IEclipsePreferences preferences = DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID);
    preferences.put(PreferenceAccess.CMAKE_GENERATOR, CmakeGenerator.Ninja.name());
    // "[]" is the JSON equivalent of an empty list. set here to avoid to store the empty list in the preferences store
    final String empty = "[]";
    preferences.put(PreferenceAccess.CMAKE_CACHE_ENTRIES, empty);
    preferences.put(PreferenceAccess.TOOLKITS, empty);
  }
}
