/*******************************************************************************
 * Copyright (c) 2022 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake4eclipse.mbs.internal;

import java.util.Optional;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;

import com.google.gson.JsonSyntaxException;

import de.marw.cmake4eclipse.mbs.preferences.BuildToolKitDefinition;
import de.marw.cmake4eclipse.mbs.preferences.PreferenceAccess;

/**
 * Resolves to the value of the path property of the currently overwriting build tool kit. If no overwriting build tool
 * kit is present, resloves to the value of the system environment variable {@code PATH}.
 * 
 * @author Martin Weber
 */
public class BTKPathDynamicVariableResolver implements IDynamicVariableResolver {

  @Override
  public String resolveValue(IDynamicVariable variable, String argument) throws CoreException {
    IEclipsePreferences prefs = PreferenceAccess.getPreferences();
    try {
      Optional<BuildToolKitDefinition> overwritingBtk = BuildToolKitUtil.getOverwritingToolkit(prefs);

      if (!overwritingBtk.isEmpty()) {
        // PATH is overwritten...
        // replace the value of $PATH with the value specified in the overwriting build tool kit
        String newPath = CCorePlugin.getDefault().getCdtVariableManager().resolveValue(overwritingBtk.get().getPath(),
            "", null, null);
        return newPath;
      } else {
        return System.getenv("PATH");
      }
    } catch (JsonSyntaxException ex) {
      // workbench preferences file format error
      throw new CoreException(Status.error("Error loading workbench preferences", ex));
    }
  }
}
