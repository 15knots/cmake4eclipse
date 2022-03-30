/* ******************************************************************************
 * Copyright (c) 2021 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.internal;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.cdtvariables.CdtVariableException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
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

  /**
   * Replaces the PATH entry in the specified map with the value from the overwriting build tool kit, if any is
   * overwriting.
   *
   * @throws CdtVariableException if expansion of the PATH variable fails
   *
   * @see BuildToolKitDefinition#getPath()
   */
  static void replacePathVarFromBuildToolKit(Map<String, String> environment)
      throws CdtVariableException, CoreException {
    IEclipsePreferences prefs = PreferenceAccess.getPreferences();
    try {
      Optional<BuildToolKitDefinition> overwritingBtk = BuildToolKitUtil.getOverwritingToolkit(prefs);
      // replace $PATH, if necessary
      if (overwritingBtk.isPresent()) {
        BuildToolKitDefinition buildToolKitDefinition = overwritingBtk.get();
        // PATH is overwritten...
        Predicate<String> isPATH = n -> false;
        if (Platform.OS_WIN32.equals(Platform.getOS())) {
          // windows which has case-insensitive envvar names, e.g. 'pAth'
          isPATH = n -> "PATH".equalsIgnoreCase(n);
        } else {
          isPATH = n -> "PATH".equals(n);
        }

        for (Iterator<Entry<String, String>> iter = environment.entrySet().iterator(); iter.hasNext();) {
          Entry<String, String> entry = iter.next();
          String key = entry.getKey();
          if (isPATH.test(key)) {
            // replace the value of $PATH with the value specified in the overwriting build tool kit
            String newPath = CCorePlugin.getDefault().getCdtVariableManager()
                .resolveValue(buildToolKitDefinition.getPath(), "", null, null);
            entry.setValue(newPath);
            break;
          }
        }
      }
    } catch (JsonSyntaxException ex) {
      // workbench preferences file format error
      throw new CoreException(Status.error("Error loading workbench preferences", ex));
    }
  }
}
