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
package de.marw.cdt.cmake.core.cmakecache;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a simplistic subset of the parsed content of a CMake cache file
 * (CMakeCache.txt).
 *
 * @author Martin Weber
 */
public class SimpleCMakeCacheTxt {

  private String buildTool;

  /**
   * Creates a new object by parsing the specified file.
   *
   * @param file
   *        the file to parse.
   * @throws IOException
   *         if the file could not be read
   */
  public SimpleCMakeCacheTxt(File file) throws IOException {
    // parse CMakeCache.txt...
    InputStream is = null;
    try {
      is = new FileInputStream(file);
      final Set<SimpleCMakeCacheEntry> entries = new HashSet<SimpleCMakeCacheEntry>();
      new CMakeCacheFileParser().parse(is, null, entries, null);
      for (SimpleCMakeCacheEntry entry : entries) {
        if ("CMAKE_BUILD_TOOL".equals(entry.getKey()))
          buildTool = entry.getValue();
      }
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
   * Gets the name of the tool that will process the generated build scripts. In
   * most cases, this method will return the absolute file system path of the
   * tool, for example {@code /usr/bin/make}.
   *
   * @return the CMAKE_BUILD_TOOL entry from the CMakeCache.txt file or
   *         {@code null} if the file could not be parsed
   */
  public String getBuildTool() {
    return buildTool;
  }

  public String getCcTool() {
    // TODO Auto-generated stub
    return "cc";
  }

  public String getCxxTool() {
    // TODO Auto-generated stub
    return "cxx";
  }
}
