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
 * makefile (buildscript) processor that builds from the generated script.
 *
 * @author Martin Weber
 */
public enum CmakeGenerator {
  /*
   * Implementation Note: Please not include generators for IDE project files,
   * such as "Eclipse CDT4 - Unix Makefiles".
   */

  // linux generators
  UnixMakefiles("Unix Makefiles"),
  // Ninja
  Ninja("Ninja", "ninja", "-k 0") {
    @Override
    public String getMakefileName(){
      return "build.ninja";
    }
  },
  // windows generators
  MinGWMakefiles("MinGW Makefiles", "mingw32-make"), MSYSMakefiles(
      "MSYS Makefiles"), NMakeMakefiles("NMake Makefiles", "nmake.exe"), NMakeMakefilesJOM(
      "NMake Makefiles JOM", "jom"), BorlandMakefiles("Borland Makefiles"), WatcomWMake(
      "Watcom WMake", "wmake");

  private final String name;
  private final String buildscriptProcessorCommand;
  private String ignoreErrOption;

  private CmakeGenerator(String name, String buildscriptProcessorCommand,
      String ignoreErrOption) {
    this.name = name;
    this.buildscriptProcessorCommand = buildscriptProcessorCommand;
    this.ignoreErrOption = ignoreErrOption;
  }

  private CmakeGenerator(String name, String buildscriptProcessorCommand) {
    this(name, buildscriptProcessorCommand, "-k");
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
   * Gets the name of the makefile (buildscript) processor.
   */
  public String getBuildscriptProcessorCommand() {
    return buildscriptProcessorCommand;
  }

  /**
   * Gets the name of the top-level makefile (buildscript) which is interpreted
   * by the buildscript processor.
   *
   * @return name of the makefile.
   */
  public String getMakefileName(){
    return "Makefile";
  }

  /**
   * Gets the extra argument to pass to the buildscript processor.
   *
   * @return a non-empty string, or {@code null} if no extra argument should be
   *         passed.
   */
  public String getBuildscriptProcessorExtraArg() {
    return null;
  }

  /**
   * Gets the buildscript processor´s command option to ignore build errors.
   *
   * @return the command option string or {@code null} if no option is needed.
   */
  public String getIgnoreErrOption() {
    return ignoreErrOption;
  }
}
