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

import org.eclipse.cdt.core.cdtvariables.CdtVariable;
import org.eclipse.cdt.core.cdtvariables.CdtVariableException;
import org.eclipse.cdt.core.cdtvariables.ICdtVariable;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.macros.BuildMacroException;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacro;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
import org.eclipse.cdt.managedbuilder.macros.IConfigurationBuildMacroSupplier;
import org.eclipse.cdt.managedbuilder.macros.IReservedMacroNameSupplier;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

import de.marw.cdt.cmake.core.internal.settings.CMakePreferences;
import de.marw.cdt.cmake.core.internal.settings.ConfigurationManager;
import de.marw.cdt.cmake.core.internal.settings.LinuxPreferences;
import de.marw.cdt.cmake.core.internal.settings.WindowsPreferences;

/**
 * Provides macros that are specific for the running OS.<br>
 * Macro values are resloved depending on the current operating system and the
 * active CDT build configuration. The following macros are provided:
 * <ul>
 * <li><strong>cmake_build_cmd</strong>: The buildscript processor´s command that
 * can build a CMake-generated project. Usually {@code make}.</li>
 * <li><strong>cmake_ignore_err_option</strong>: The buildscript processor´s command
 * option to ignore build errors. Usually {@code -k}.</li>
 * <li><strong>cmake_build_cmd_earg</strong> The extra argument to pass to the
 * buildscript processor.</li>
 * </ul>
 *
 * @author Martin Weber
 */
public class CmakeOsMacroSupplier implements IConfigurationBuildMacroSupplier,
    IReservedMacroNameSupplier {

  /*-
   * @see org.eclipse.cdt.managedbuilder.macros.IConfigurationBuildMacroSupplier#getMacro(java.lang.String, org.eclipse.cdt.managedbuilder.core.IConfiguration, org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider)
   */
  @Override
  public IBuildMacro getMacro(String macroName, IConfiguration configuration,
      IBuildMacroProvider provider) {
    if (!isKnownMacro(macroName))
      return null;

    // load project properties..
    final ICConfigurationDescription cfgd = ManagedBuildManager
        .getDescriptionForConfiguration(configuration);
    try {
      final CMakePreferences prefs = ConfigurationManager.getInstance()
          .getOrLoad(cfgd);
      final String os = Platform.getOS();

      if ("cmake_build_cmd".equals(macroName)) {
        String buildscriptProcessorCmd;
        if (Platform.OS_WIN32.equals(os)) {
          WindowsPreferences osPrefs = prefs.getWindowsPreferences();
          buildscriptProcessorCmd = osPrefs.getBuildscriptProcessorCommand();
          if (buildscriptProcessorCmd == null) {
            buildscriptProcessorCmd = osPrefs.getGenerator().getBuildscriptProcessorCommand();
          }
        } else {
          // fall back to linux, if OS is unknown
          LinuxPreferences osPrefs = prefs.getLinuxPreferences();
          buildscriptProcessorCmd = osPrefs.getBuildscriptProcessorCommand();
          if (buildscriptProcessorCmd == null) {
            buildscriptProcessorCmd = osPrefs.getGenerator().getBuildscriptProcessorCommand();
          }
        }
        return new CmakeBuildMacro(macroName, ICdtVariable.VALUE_TEXT,
            buildscriptProcessorCmd);
      } else {
        CmakeGenerator generator;
        if (Platform.OS_WIN32.equals(os)) {
          WindowsPreferences osPrefs = prefs.getWindowsPreferences();
          generator = osPrefs.getGenerator();
        } else {
          // fall back to linux, if OS is unknown
          LinuxPreferences osPrefs = prefs.getLinuxPreferences();
          generator = osPrefs.getGenerator();
        }

        if ("cmake_ignore_err_option".equals(macroName)) {
          return new CmakeBuildMacro(macroName, ICdtVariable.VALUE_TEXT,
              generator.getIgnoreErrOption());
        }
        if ("cmake_build_cmd_earg".equals(macroName)) {
          String extraArg = generator.getBuildscriptProcessorExtraArg();
          if (extraArg != null)
            return new CmakeBuildMacro(macroName, ICdtVariable.VALUE_TEXT,
                extraArg);
        }
      }
    } catch (CoreException ex) {
      // TODO Auto-generated catch block
    }
    return null;
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.macros.IConfigurationBuildMacroSupplier#getMacros(org.eclipse.cdt.managedbuilder.core.IConfiguration, org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider)
   */
  @Override
  public IBuildMacro[] getMacros(IConfiguration configuration,
      IBuildMacroProvider provider) {
    return new IBuildMacro[] {
        getMacro("cmake_build_cmd", configuration, provider),
        getMacro("cmake_ignore_err_option", configuration, provider),
        getMacro("cmake_build_cmd_earg", configuration, provider) };
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.macros.IReservedMacroNameSupplier#isReservedName(java.lang.String, org.eclipse.cdt.managedbuilder.core.IConfiguration)
   */
  @Override
  public boolean isReservedName(String macroName, IConfiguration configuration) {
    return isKnownMacro(macroName);
  }

  /**
   * Gets whether the macro of the specified name is known to this macro
   * supplier.
   */
  private static boolean isKnownMacro(String macroName) {
    if ("cmake_build_cmd".equals(macroName)
        || "cmake_ignore_err_option".equals(macroName)
        || "cmake_build_cmd_earg".equals(macroName))
      return true;
    return false;
  }

  ////////////////////////////////////////////////////////////////////
  // inner classes
  ////////////////////////////////////////////////////////////////////
  private static class CmakeBuildMacro extends CdtVariable implements
      IBuildMacro {

    public CmakeBuildMacro(String name, int type, String value) {
      super(name, type, value);
    }

    /*-
     * @see org.eclipse.cdt.managedbuilder.macros.IBuildMacro#getMacroValueType()
     */
    @Override
    public int getMacroValueType() {
      return super.getValueType();
    }

    /*-
     * @see org.eclipse.cdt.managedbuilder.macros.IBuildMacro#getStringListValue()
     */
    @Override
    public String[] getStringListValue() throws BuildMacroException {
      try {
        return super.getStringListValue();
      } catch (CdtVariableException e) {
        throw new BuildMacroException(e);
      }
    }

    /*-
     * @see org.eclipse.cdt.managedbuilder.macros.IBuildMacro#getStringValue()
     */
    @Override
    public String getStringValue() throws BuildMacroException {
      try {
        return super.getStringValue();
      } catch (CdtVariableException e) {
        throw new BuildMacroException(e);
      }
    }

  } // CmakeBuildMacro

}
