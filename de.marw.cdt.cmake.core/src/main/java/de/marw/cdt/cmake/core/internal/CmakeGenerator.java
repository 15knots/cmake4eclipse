/*******************************************************************************
 * Copyright (c) 2014-2018 Martin Weber.
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
 * Represents a cmake build-script generator including information about the
 * makefile (build-script) processor that builds from the generated script.
 *
 * @author Martin Weber
 */
public enum CmakeGenerator {
  /*
   * Implementation Note: Please do not include generators for IDE project files,
   * such as "Eclipse CDT4 - Unix Makefiles".
   */

  // linux generators
  UnixMakefiles("Unix Makefiles"),
  // Ninja
  Ninja("Ninja", "-k 0") {
    @Override
    public String getMakefileName(){
      return "build.ninja";
    }
    @Override
    public String getParallelBuildArg(int parallelizationNum) {
      if (parallelizationNum == 1)
        return "-j 1"; // No parallel
      else
        return "-j " + parallelizationNum; // Unlimited or User specified
    }
  },
  // windows generators
  NMakeMakefilesJOM("NMake Makefiles JOM") {
    @Override
    public String getParallelBuildArg(int parallelizationNum) {
      if (parallelizationNum == 1)
        return null; // No parallel
      else
        return "-j " + parallelizationNum; // Unlimited or User specified
    }
  },
  MinGWMakefiles("MinGW Makefiles"), MSYSMakefiles("MSYS Makefiles"), NMakeMakefiles("NMake Makefiles") {
        @Override
        public String getParallelBuildArg(int parallelizationNum) {
          return null; // parallel build not supported
        }
      },
  BorlandMakefiles("Borland Makefiles") {
    @Override
    public String getParallelBuildArg(int parallelizationNum) {
      return null; // parallel build not supported
    }
  },
  WatcomWMake("Watcom WMake") {
    @Override
    public String getParallelBuildArg(int parallelizationNum) {
      return null; // parallel build not supported
    }
  };

  private final String name;
  private String ignoreErrOption;

  private CmakeGenerator(String name,
      String ignoreErrOption) {
    this.name = name;
    this.ignoreErrOption = ignoreErrOption;
  }

  private CmakeGenerator(String name) {
    this(name, "-k");
  }

  /**
   * Gets the cmake argument that specifies the build-script generator.
   *
   * @return a non-empty string, which must be a valid argument for cmake's -G
   *         option.
   */
  public String getCmakeName() {
    return name;
  }

  /**
   * Gets the name of the top-level makefile (build-script) which is interpreted
   * by the build-script processor.
   *
   * @return name of the makefile.
   */
  public String getMakefileName(){
    return "Makefile";
  }

  /**
   * Gets the extra argument to pass to the build-script processor.
   *
   * @return a non-empty string, or {@code null} if no extra argument should be
   *         passed.
   */
  public String getBuildscriptProcessorExtraArg() {
    return null;
  }

  /**
   * Gets the build-script processor´s command argument(s) to ignore build errors.
   *
   * @return the command option string or {@code null} if no argument is needed.
   */
  public String getIgnoreErrOption() {
    return ignoreErrOption;
  }

  /**
   * Gets the build-script processor´s command argument(s) to run parallel jobs
   * in the build. This default implementation returns the argument as specified
   * for the {@code GNU make} tool.
   *
   * @param parallelizationNum
   *          the number of parallel jobs to run. This is encoded as follows:
   *          <ul>
   *          <li>No parallel: <b>1</b></li>
   *          <li>Unlimited: <b>Integer.MAX_VALUE</b></li>
   *          <li>User specified: <b>>0</b> (positive number)</li>
   *          </ul>
   * @return the command option string or {@code null} if no argument is needed.
   */
  public String getParallelBuildArg(int parallelizationNum) {
    if (parallelizationNum == 1)
      return null; // No parallel
    else if (parallelizationNum == Integer.MAX_VALUE)
      return "-j"; // Unlimited
    else
      return "-j " + parallelizationNum; // User specified
  }
}
