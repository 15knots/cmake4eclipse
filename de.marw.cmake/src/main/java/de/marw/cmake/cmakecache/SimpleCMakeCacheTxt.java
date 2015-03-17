/*******************************************************************************
 * Copyright (c) 2015 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cmake.cmakecache;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a simplistic subset of the parsed content of a CMake cache file
 * (CMakeCache.txt).
 *
 * @author Martin Weber
 */
public class SimpleCMakeCacheTxt {

  private String buildTool;
  private List<String> tools;
  private List<String> commands;

  /**
   * Creates a new object by parsing the specified file.
   *
   * @param file
   *        the file to parse.
   * @throws IOException
   *         if the file could not be read
   */
  public SimpleCMakeCacheTxt(File file) throws IOException {
    ArrayList<String> tools = new ArrayList<String>();
    ArrayList<String> commands = new ArrayList<String>();

    // parse CMakeCache.txt...
    InputStream is = null;
    try {
      is = new FileInputStream(file);
      final Set<SimpleCMakeCacheEntry> entries = new HashSet<SimpleCMakeCacheEntry>();
      new CMakeCacheFileParser().parse(is, null, entries, null);
      for (SimpleCMakeCacheEntry entry : entries) {
        final String toolKey = entry.getKey();
        final String tool = entry.getValue();
        if ("CMAKE_BUILD_TOOL".equals(toolKey)) {
          buildTool = tool;
        } else if ("CMAKE_COMMAND".equals(toolKey)) {
          commands.add(tool);
        } else if ("CMAKE_CPACK_COMMAND".equals(toolKey)) {
          commands.add(tool);
        } else if ("CMAKE_CTEST_COMMAND".equals(toolKey)) {
          commands.add(tool);
        } else if ("CMAKE_C_COMPILER".equals(toolKey)) {
          tools.add(tool);
        } else if ("CMAKE_CXX_COMPILER".equals(toolKey)) {
          tools.add(tool);
        }
      }
      this.tools = Collections.unmodifiableList(tools);
      this.commands = Collections.unmodifiableList(commands);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException ignore) {
        }
      }
    }
  }

  /**
   * Gets the name of the tool that processes the generated build scripts. In
   * most cases, this method will return the absolute file system path of the
   * tool, such as {@code /usr/bin/make}.
   *
   * @return the CMAKE_BUILD_TOOL entry from the CMakeCache.txt file or
   *         {@code null} if the file could not be parsed
   */
  public String getBuildTool() {
    return buildTool;
  }

  /**
   * Gets the tools that process the source files to binary files (compilers,
   * linkers). In most cases, this method will return the absolute file system
   * paths of a tool, for example {@code /usr/bin/cc}.
   */
  public List<String> getTools() {
    return tools;
  }

  /**
   * Gets the tools provided by CMake itself (cmake, cpack, ctest). In most
   * cases, this method will return the absolute file system paths of a tool.
   */
  public List<String> getCmakeCommands() {
    return commands;
  }
}
