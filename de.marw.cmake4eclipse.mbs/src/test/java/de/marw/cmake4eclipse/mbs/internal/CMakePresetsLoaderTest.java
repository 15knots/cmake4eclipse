/*******************************************************************************
 * Copyright (c) 2026 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.Test;

import de.marw.cmake4eclipse.mbs.settings.CmakeVariableType;

public class CMakePresetsLoaderTest {

  @Test
  public void testResolvesBuildPresetToConfigurePreset() throws Exception {
    Path sourceDir = Files.createTempDirectory("c4e-preset-test");
    try {
      String json = "{\n"
          + "  \"version\": 4,\n"
          + "  \"configurePresets\": [\n"
          + "    {\n"
          + "      \"name\": \"base\",\n"
          + "      \"generator\": \"Ninja\",\n"
          + "      \"cacheVariables\": {\n"
          + "        \"CMAKE_C_STANDARD\": \"11\"\n"
          + "      }\n"
          + "    },\n"
          + "    {\n"
          + "      \"name\": \"debug\",\n"
          + "      \"inherits\": \"base\",\n"
          + "      \"binaryDir\": \"${sourceDir}/out/${presetName}\",\n"
          + "      \"cacheVariables\": {\n"
          + "        \"ENABLE_TESTS\": true,\n"
          + "        \"TOOLCHAIN_FILE\": {\"type\": \"FILEPATH\", \"value\": \"toolchain.cmake\"}\n"
          + "      },\n"
          + "      \"debug\": {\"output\": true}\n"
          + "    }\n"
          + "  ],\n"
          + "  \"buildPresets\": [\n"
          + "    {\"name\": \"Debug\", \"configurePreset\": \"debug\"}\n"
          + "  ]\n"
          + "}\n";
      Files.writeString(sourceDir.resolve("CMakePresets.json"), json);

      Optional<CMakePresetsLoader.ResolvedConfigurePreset> preset = CMakePresetsLoader
          .loadResolvedConfigurePreset(sourceDir, "Debug");
      assertTrue(preset.isPresent());
      CMakePresetsLoader.ResolvedConfigurePreset value = preset.get();
      assertEquals("debug", value.getName());
      assertEquals("Ninja", value.getGenerator());
      assertEquals(sourceDir.resolve("out/debug").normalize().toString(), value.getBinaryDir());
      assertEquals(3, value.getCacheVariables().size());
      assertTrue(value.getCacheVariables().stream().anyMatch(v -> "CMAKE_C_STANDARD".equals(v.getName())));
      assertTrue(value.getCacheVariables().stream().anyMatch(v -> "ENABLE_TESTS".equals(v.getName())
          && v.getType() == CmakeVariableType.BOOL && "true".equals(v.getValue())));
      assertTrue(value.getCacheVariables().stream().anyMatch(v -> "TOOLCHAIN_FILE".equals(v.getName())
          && v.getType() == CmakeVariableType.FILEPATH));
      assertTrue(value.getConfigureArguments().contains("--debug-output"));
    } finally {
      Files.deleteIfExists(sourceDir.resolve("CMakePresets.json"));
      Files.deleteIfExists(sourceDir);
    }
  }

  @Test
  public void testIgnoresMissingPresetFile() throws Exception {
    Path sourceDir = Files.createTempDirectory("c4e-preset-empty");
    try {
      Optional<CMakePresetsLoader.ResolvedConfigurePreset> preset = CMakePresetsLoader
          .loadResolvedConfigurePreset(sourceDir, "Debug");
      assertNotNull(preset);
      assertFalse(preset.isPresent());
    } finally {
      Files.deleteIfExists(sourceDir);
    }
  }

  @Test
  public void testMatchesConfigurePresetByConfigurationName() throws Exception {
    Path sourceDir = Files.createTempDirectory("c4e-preset-config-name");
    try {
      String json = "{\n"
          + "  \"version\": 4,\n"
          + "  \"configurePresets\": [\n"
          + "    {\n"
          + "      \"name\": \"Release\",\n"
          + "      \"generator\": \"Ninja Multi-Config\",\n"
          + "      \"cacheVariables\": {\n"
          + "        \"CMAKE_BUILD_TYPE\": \"Release\"\n"
          + "      }\n"
          + "    }\n"
          + "  ]\n"
          + "}\n";
      Files.writeString(sourceDir.resolve("CMakePresets.json"), json);

      Optional<CMakePresetsLoader.ResolvedConfigurePreset> preset = CMakePresetsLoader
          .loadResolvedConfigurePreset(sourceDir, "release");
      assertTrue(preset.isPresent());
      assertEquals("Release", preset.get().getName());
      assertTrue(preset.get().getCacheVariables().stream().anyMatch(v -> "CMAKE_BUILD_TYPE".equals(v.getName())));
    } finally {
      Files.deleteIfExists(sourceDir.resolve("CMakePresets.json"));
      Files.deleteIfExists(sourceDir);
    }
  }
}
