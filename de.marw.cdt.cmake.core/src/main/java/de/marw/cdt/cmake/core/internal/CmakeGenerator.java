/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core.internal;

/**
 * Represents a cmake buildscript generator including information about the
 * native tool that builds from the generated script.
 *
 * @author Martin Weber
 */
public enum CmakeGenerator {
  /*
   * Implementaion Note: Please not include generators for IDE project files,
   * such as "Eclipse CDT4 - Unix Makefiles".
   */

  // linux generators
  UnixMakefiles("Unix Makefiles"),
  // Ninja, experimental
  Ninja("Ninja", "ninja", "-k 999999"){
    @Override
    public String getNativeBuildExtraArg() {
      return "-v";
    }
  },
  // windows generators
  MinGWMakefiles("MinGW Makefiles", "mingw32-make"), MSYSMakefiles(
      "MSYS Makefiles"), NMakeMakefiles("NMake Makefiles", "nmake.exe"), NMakeMakefilesJOM(
      "NMake Makefiles JOM", "jom"), BorlandMakefiles("Borland Makefiles"), WatcomWMake(
      "Watcom WMake", "wmake");

  private final String name;
  private final String nativeBuildCommand;
  private String ignoreErrOption;

  private CmakeGenerator(String name, String nativeBuildCommand,
      String ignoreErrOption) {
    this.name = name;
    this.nativeBuildCommand = nativeBuildCommand;
    this.ignoreErrOption = ignoreErrOption;
  }

  private CmakeGenerator(String name, String nativeBuildCommand) {
    this(name, nativeBuildCommand, "-k");
  }

  private CmakeGenerator(String name) {
    this(name, "make");
  }

  /**
   * Gets the cmake argument that specifies the buildscript generator.
   *
   * @return a non-empty string, which must be a valid argument for cmake's -G
   *         option.
   */
  public String getCmakeName() {
    return name;
  }

  /**
   * Gets the native build command name.
   */
  public String getNativeBuildCommand() {
    return nativeBuildCommand;
  }

  /**
   * Gets the extra argument to pass to the native build command.
   *
   * @return a non-empty string, or {@code null} if no extra argument should be
   *         passed.
   */
  public String getNativeBuildExtraArg() {
    return null;
  }

  /**
   * Gets the native build tool´s command option to ignore build errors.
   *
   * @return
   */
  public String getIgnoreErrOption() {
    return ignoreErrOption;
  }
}
