/*******************************************************************************
 * Copyright (c) 2021 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import de.marw.cmake4eclipse.mbs.internal.Activator;

/**
 * Constant definitions for plug-in workbench preferences.
 */
public abstract class PreferenceAccess {
  /** preference key for the 'verbose build' boolean */
  public static final String VERBOSE_BUILD = "VERBOSE_BUILD";
  /** preference key for the 'use cmake from path' boolean */
//  public static final String CMAKE_FROM_PATH = "CMAKE_FROM_PATH";
  /** preference key for the path to the cmake executable file */
//  public static final String CMAKE_EXECUTABLE = "CMAKE_EXECUTABLE";

  /** preference key for the 'run cmake on each build' boolean */
  public static final String CMAKE_FORCE_RUN = "CMAKE_FORCE_RUN";
  /** preference key for the -Wno-dev commandline option */
  public static final String CMAKE_WARN_NO_DEV = "CMAKE_WARN_NO_DEV";
  /** preference key for the --debug-trycompile commandline option */
  public static final String CMAKE_DBG_TRY_COMPILE = "CMAKE_DBG_TRY_COMPILE";
  /** preference key for the --debug-output commandline option */
  public static final String CMAKE_DBG = "CMAKE_DBG";
  /** preference key for the --trace commandline option */
  public static final String CMAKE_TRACE = "CMAKE_TRACE";
  /** preference key for the --warn-uninitializedv commandline option */
  public static final String CMAKE_WARN_UNINITIALIZED = "CMAKE_WARN_UNINITIALIZED";
  /** preference key for the --warn-unused-vars commandline option */
  public static final String CMAKE_WARN_UNUSED = "CMAKE_WARN_UNUSED";

  /** preference key for the list of cache entries to pass to cmake */
  public static final String CMAKE_CACHE_ENTRIES = "CMAKE_CACHE_ENTRIES";
  /** preference key for the default build system to generate scripts for */
  public static final String CMAKE_GENERATOR = "CMAKE_GENERATOR";

  /** preference key for the list of defined build tool kits */
  public static final String TOOLKITS = "TOOLKITS";
  /**
   * UID of the build tool kit that overwrites the default build settings or zero if no overwrite is configured. Falls
   * back to 'no overwrite' behavior if no defined build tool kit of the given UID exists.
   */
  public static final String TOOLKIT_OVERWRITES = "TOOLKIT_OVERWRITE_UID";

  /** preference key for the 'dirty' time stamp (in milliseconds) */
  public static final String DIRTY_TS = "DIRTY_TS";

  /**
   * Returns this plugin`s preferences.
   */
  public static final IEclipsePreferences getPreferences() {
    return InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
  }

  /**
   * Converts the specified list of objects to a JSON string.
   */
  public static String toJsonFromList(List<?> cacheEntries) {
    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    return gson.toJson(cacheEntries);
  }

  /**
   * Converts the specified JSON string to a list of objects.
   *
   * @param json the JSON string to convert.
   * @param <E>  the type of the objects in the returned list
   */
  public static <E> List<E> toListFromJson(Class<E> classOfE, String json) {
    TypeToken<?> tt = TypeToken.getParameterized(List.class, classOfE);
    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    List<E> entries = gson.fromJson(json, tt.getType());
    if (entries == null) {
      entries = new ArrayList<E>();
    }
    return entries;
  }
}
