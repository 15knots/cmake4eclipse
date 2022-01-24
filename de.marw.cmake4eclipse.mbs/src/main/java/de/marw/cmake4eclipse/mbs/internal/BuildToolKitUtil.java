/* ******************************************************************************
 * Copyright (c) 2021 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.internal;

import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import com.google.gson.JsonSyntaxException;

import de.marw.cmake4eclipse.mbs.preferences.BuildToolKitDefinition;
import de.marw.cmake4eclipse.mbs.preferences.PreferenceAccess;
import de.marw.cmake4eclipse.mbs.settings.CmakeGenerator;

/**
 * @author weber
 */
class BuildToolKitUtil {

  /**
   * @param prefs
   * @return
   */
  static CmakeGenerator getEffectiveCMakeGenerator(IEclipsePreferences prefs,
      Optional<BuildToolKitDefinition> overwritingToolkit) {
    if (overwritingToolkit.isPresent()) {
      return overwritingToolkit.map(e -> e.getGenerator()).get();
    } else {
      String genName = prefs.get(PreferenceAccess.CMAKE_GENERATOR, CmakeGenerator.Ninja.name());
      return CmakeGenerator.valueOf(genName);
    }
  }

  static Optional<BuildToolKitDefinition> getOverwritingToolkit(IEclipsePreferences prefs) throws JsonSyntaxException {
    long ovr = prefs.getLong(PreferenceAccess.TOOLKIT_OVERWRITES, 0);
    if (ovr != 0) {
      // overwrite is active
      String json = prefs.get(PreferenceAccess.TOOLKITS, null);
      List<BuildToolKitDefinition> entries = PreferenceAccess.toListFromJson(BuildToolKitDefinition.class, json);
      if (entries != null) {
        return entries.stream().filter(e -> e.getUid() == ovr).findFirst();
      }
    }
    return Optional.empty();
  }

}
