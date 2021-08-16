/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.settings;

/**
 * The type identifier of a cmake variable.
 *
 * @author Martin Weber
 */
public enum CmakeVariableType {
  /** Boolean ON/OFF checkbox */
  BOOL {
    @Override
    public String getCmakeArg() {
      return "BOOL";
    }
  },
  /** File chooser dialog */
  FILEPATH {
    @Override
    public String getCmakeArg() {
      return "FILEPATH";
    }
  },
  /** Directory chooser dialog */
  PATH {
    @Override
    public String getCmakeArg() {
      return "PATH";
    }
  },
  /** Arbitrary string */
  STRING {
    @Override
    public String getCmakeArg() {
      return "STRING";
    }
  },
  /** No GUI entry (used for persistent variables) */
  INTERNAL {
    @Override
    public String getCmakeArg() {
      return "INTERNAL";
    }
  };

  /**
   * Gets the type as a valid commandline argument for cmake.
   */
  public abstract String getCmakeArg();
}