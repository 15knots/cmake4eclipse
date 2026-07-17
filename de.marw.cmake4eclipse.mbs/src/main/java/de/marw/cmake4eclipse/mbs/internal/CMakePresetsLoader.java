/*******************************************************************************
 * Copyright (c) 2026 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.internal;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.marw.cmake4eclipse.mbs.settings.CmakeDefine;
import de.marw.cmake4eclipse.mbs.settings.CmakeVariableType;

/**
 * Loads and resolves CMake configure/build presets from {@code CMakePresets.json}.
 */
final class CMakePresetsLoader {
  private static final Gson gson = new Gson();
  private static final Pattern ENV_REF = Pattern.compile("\\$(p?env)\\{([^}]+)\\}");

  private CMakePresetsLoader() {
  }

  static Optional<ResolvedConfigurePreset> loadResolvedConfigurePreset(Path sourceDir, String configurationName)
      throws IOException {
    final Path file = sourceDir.resolve("CMakePresets.json");
    if (!Files.isRegularFile(file)) {
      return Optional.empty();
    }

    final JsonObject root;
    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      root = JsonParser.parseReader(reader).getAsJsonObject();
    }

    final Map<String, JsonObject> configurePresets = toPresetMap(root.getAsJsonArray("configurePresets"));
    if (configurePresets.isEmpty()) {
      return Optional.empty();
    }
    final Map<String, JsonObject> buildPresets = toPresetMap(root.getAsJsonArray("buildPresets"));

    final String configurePresetName = resolveConfigurePresetName(configurationName, buildPresets, configurePresets);
    if (configurePresetName == null) {
      return Optional.empty();
    }

    final JsonObject resolved = resolvePreset(configurePresetName, configurePresets, new HashMap<>(), new HashSet<>());
    if (resolved == null || isHidden(resolved)) {
      return Optional.empty();
    }
    final String name = getString(resolved, "name");
    final String generator = getString(resolved, "generator");
    final String binaryDir = expandBinaryDir(getString(resolved, "binaryDir"), sourceDir, name, generator);
    final List<CmakeDefine> cacheVariables = toCacheVariables(resolved.getAsJsonObject("cacheVariables"));
    final List<String> configureArguments = toConfigureArguments(resolved);

    return Optional.of(new ResolvedConfigurePreset(name, generator, binaryDir, cacheVariables, configureArguments));
  }

  private static String resolveConfigurePresetName(String configurationName, Map<String, JsonObject> buildPresets,
      Map<String, JsonObject> configurePresets) {
    JsonObject buildPreset = findNamedPreset(buildPresets, configurationName);
    if (buildPreset != null) {
      JsonObject resolvedBuildPreset = resolvePreset(getString(buildPreset, "name"), buildPresets, new HashMap<>(),
          new HashSet<>());
      if (resolvedBuildPreset == null) {
        return null;
      }
      String configurePresetName = getString(resolvedBuildPreset, "configurePreset");
      if (configurePresetName != null && configurePresets.containsKey(configurePresetName)) {
        return configurePresetName;
      }
    }
    JsonObject configurePreset = findNamedPreset(configurePresets, configurationName);
    if (configurePreset != null) {
      return getString(configurePreset, "name");
    }
    return null;
  }

  private static Map<String, JsonObject> toPresetMap(Iterable<JsonElement> array) {
    if (array == null) {
      return Collections.emptyMap();
    }
    final Map<String, JsonObject> result = new HashMap<>();
    for (JsonElement preset : array) {
      if (preset == null || !preset.isJsonObject()) {
        continue;
      }
      final JsonObject presetObj = preset.getAsJsonObject();
      final String name = getString(presetObj, "name");
      if (name != null && !name.isBlank()) {
        result.put(name, presetObj);
      }
    }
    return result;
  }

  private static JsonObject findNamedPreset(Map<String, JsonObject> presets, String name) {
    if (name == null) {
      return null;
    }
    JsonObject exact = presets.get(name);
    if (exact != null) {
      return exact;
    }
    final String needle = name.toLowerCase(Locale.ROOT);
    for (Map.Entry<String, JsonObject> entry : presets.entrySet()) {
      if (entry.getKey().toLowerCase(Locale.ROOT).equals(needle)) {
        return entry.getValue();
      }
    }
    return null;
  }

  private static JsonObject resolvePreset(String name, Map<String, JsonObject> presets, Map<String, JsonObject> cache,
      Set<String> visited) {
    if (name == null) {
      return null;
    }
    JsonObject fromCache = cache.get(name);
    if (fromCache != null) {
      return fromCache;
    }
    JsonObject self = presets.get(name);
    if (self == null) {
      return null;
    }
    if (!visited.add(name)) {
      return null;
    }

    JsonObject merged = new JsonObject();
    for (String parent : getInherits(self)) {
      JsonObject resolvedParent = resolvePreset(parent, presets, cache, visited);
      if (resolvedParent != null) {
        mergeInto(merged, resolvedParent);
      }
    }
    mergeInto(merged, self);
    visited.remove(name);
    cache.put(name, merged);
    return merged;
  }

  private static List<String> getInherits(JsonObject preset) {
    JsonElement inherits = preset.get("inherits");
    if (inherits == null || inherits.isJsonNull()) {
      return Collections.emptyList();
    }
    if (inherits.isJsonPrimitive() && inherits.getAsJsonPrimitive().isString()) {
      return List.of(inherits.getAsString());
    }
    if (inherits.isJsonArray()) {
      List<String> names = new ArrayList<>();
      for (JsonElement e : inherits.getAsJsonArray()) {
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
          names.add(e.getAsString());
        }
      }
      return names;
    }
    return Collections.emptyList();
  }

  private static void mergeInto(JsonObject target, JsonObject source) {
    for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
      String key = entry.getKey();
      JsonElement sourceValue = entry.getValue();
      JsonElement targetValue = target.get(key);
      if (targetValue != null && targetValue.isJsonObject() && sourceValue.isJsonObject()) {
        mergeInto(targetValue.getAsJsonObject(), sourceValue.getAsJsonObject());
      } else {
        target.add(key, sourceValue.deepCopy());
      }
    }
  }

  private static boolean isHidden(JsonObject preset) {
    JsonElement hidden = preset.get("hidden");
    return hidden != null && hidden.isJsonPrimitive() && hidden.getAsJsonPrimitive().isBoolean()
        && hidden.getAsBoolean();
  }

  private static String getString(JsonObject object, String member) {
    JsonElement value = object.get(member);
    if (value == null || value.isJsonNull()) {
      return null;
    }
    if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
      return value.getAsString();
    }
    return null;
  }

  private static List<CmakeDefine> toCacheVariables(JsonObject cacheVariables) {
    if (cacheVariables == null) {
      return Collections.emptyList();
    }
    List<CmakeDefine> result = new ArrayList<>();
    for (Map.Entry<String, JsonElement> entry : cacheVariables.entrySet()) {
      final String name = entry.getKey();
      final JsonElement value = entry.getValue();
      if (value == null || value.isJsonNull()) {
        continue;
      }
      if (value.isJsonObject()) {
        JsonObject object = value.getAsJsonObject();
        String text = asScalarText(object.get("value"));
        if (text == null) {
          text = gson.toJson(object.get("value"));
        }
        result.add(new CmakeDefine(name, toType(getString(object, "type")), text == null ? "" : text));
      } else {
        result.add(new CmakeDefine(name, inferType(value), asScalarText(value)));
      }
    }
    return result;
  }

  private static List<String> toConfigureArguments(JsonObject preset) {
    List<String> args = new ArrayList<>();
    JsonObject warnings = preset.getAsJsonObject("warnings");
    if (warnings != null) {
      if (getBoolean(warnings, "dev", false)) {
        args.add("-Wdev");
      }
      if (getBoolean(warnings, "deprecated", false)) {
        args.add("-Wdeprecated");
      }
      if (getBoolean(warnings, "uninitialized", false)) {
        args.add("--warn-uninitialized");
      }
      if (getBoolean(warnings, "unusedCli", false)) {
        args.add("--warn-unused-cli");
      }
    }
    JsonObject debug = preset.getAsJsonObject("debug");
    if (debug != null) {
      if (getBoolean(debug, "output", false)) {
        args.add("--debug-output");
      }
      if (getBoolean(debug, "tryCompile", false)) {
        args.add("--debug-trycompile");
      }
      if (getBoolean(debug, "find", false)) {
        args.add("--debug-find");
      }
    }
    JsonObject trace = preset.getAsJsonObject("trace");
    if (trace != null) {
      final String mode = getString(trace, "mode");
      if ("expand".equals(mode)) {
        args.add("--trace-expand");
      } else if (mode != null) {
        args.add("--trace");
      } else if (getBoolean(trace, "enable", false)) {
        args.add("--trace");
      }
    }
    return args;
  }

  private static boolean getBoolean(JsonObject object, String member, boolean defaultValue) {
    JsonElement value = object.get(member);
    if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
      return defaultValue;
    }
    return value.getAsBoolean();
  }

  private static CmakeVariableType inferType(JsonElement value) {
    if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
      return CmakeVariableType.BOOL;
    }
    return CmakeVariableType.STRING;
  }

  private static CmakeVariableType toType(String text) {
    if (text == null) {
      return CmakeVariableType.STRING;
    }
    try {
      return CmakeVariableType.valueOf(text.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return CmakeVariableType.STRING;
    }
  }

  private static String asScalarText(JsonElement value) {
    if (value == null || value.isJsonNull()) {
      return "";
    }
    if (value.isJsonPrimitive()) {
      return value.getAsJsonPrimitive().isString() ? value.getAsString() : value.getAsJsonPrimitive().toString();
    }
    return gson.toJson(value);
  }

  private static String expandBinaryDir(String binaryDir, Path sourceDir, String presetName, String generator) {
    if (binaryDir == null || binaryDir.isBlank()) {
      return binaryDir;
    }
    String expanded = binaryDir;
    expanded = expanded.replace("${sourceDir}", sourceDir.toString());
    Path parent = sourceDir.getParent();
    expanded = expanded.replace("${sourceParentDir}", parent == null ? "" : parent.toString());
    Path leaf = sourceDir.getFileName();
    expanded = expanded.replace("${sourceDirName}", leaf == null ? "" : leaf.toString());
    expanded = expanded.replace("${presetName}", presetName == null ? "" : presetName);
    expanded = expanded.replace("${generator}", generator == null ? "" : generator);
    expanded = expandEnvironmentReferences(expanded);
    return expanded;
  }

  private static String expandEnvironmentReferences(String value) {
    Matcher m = ENV_REF.matcher(value);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      String var = m.group(2);
      String replacement = System.getenv(var);
      if (replacement == null) {
        replacement = "";
      }
      m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    m.appendTail(sb);
    return sb.toString();
  }

  static final class ResolvedConfigurePreset {
    private final String name;
    private final String generator;
    private final String binaryDir;
    private final List<CmakeDefine> cacheVariables;
    private final List<String> configureArguments;

    ResolvedConfigurePreset(String name, String generator, String binaryDir, List<CmakeDefine> cacheVariables,
        List<String> configureArguments) {
      this.name = name;
      this.generator = generator;
      this.binaryDir = binaryDir;
      this.cacheVariables = new ArrayList<>(cacheVariables);
      this.configureArguments = new ArrayList<>(configureArguments);
    }

    String getName() {
      return name;
    }

    String getGenerator() {
      return generator;
    }

    String getBinaryDir() {
      return binaryDir;
    }

    List<CmakeDefine> getCacheVariables() {
      return new ArrayList<>(cacheVariables);
    }

    List<String> getConfigureArguments() {
      return new ArrayList<>(configureArguments);
    }
  }
}
