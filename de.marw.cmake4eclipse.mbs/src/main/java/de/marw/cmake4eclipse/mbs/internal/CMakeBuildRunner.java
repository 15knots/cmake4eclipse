/*******************************************************************************
 * Copyright (c) 2014-2018 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.ProblemMarkerInfo;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.extension.CBuildData;
import org.eclipse.cdt.managedbuilder.core.AbstractBuildRunner;
import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.ExternalBuildRunner;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IHoldsOptions;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.IOptionCategory;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.macros.IFileContextBuildMacroValues;
import org.eclipse.cdt.managedbuilder.macros.IReservedMacroNameSupplier;
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.framework.Version;

import com.google.gson.JsonSyntaxException;

import de.marw.cmake4eclipse.mbs.cmakecache.CMakeCacheFileParser;
import de.marw.cmake4eclipse.mbs.cmakecache.CMakeCacheFileParser.EntryFilter;
import de.marw.cmake4eclipse.mbs.cmakecache.SimpleCMakeCacheEntry;
import de.marw.cmake4eclipse.mbs.preferences.BuildToolKitDefinition;
import de.marw.cmake4eclipse.mbs.preferences.PreferenceAccess;
import de.marw.cmake4eclipse.mbs.settings.CmakeGenerator;

/**
 * An ExternalBuildRunner that injects the build tool command to use and some of
 * its arguments into the build. Necessary since CDT does not allow
 * {@code IConfigurationBuildMacroSupplier}s for a Builder (only for
 * tool-chains).
 *
 * @author Martin Weber
 */
public class CMakeBuildRunner extends ExternalBuildRunner {
  /** build runner error marker ID */
  private static final String MARKER_ID = Activator.PLUGIN_ID + ".BuildRunnerError";

  /** caches CMakeCacheFileInfo */
  private static final QualifiedName cacheFileInfo = new QualifiedName(Activator.PLUGIN_ID, "cmakeCacheFileInfo");

  @Override
  protected Map<String, String> getEnvironment(IBuilder builder) throws CoreException {
    Map<String, String> environment = super.getEnvironment(builder);
    IEclipsePreferences prefs = PreferenceAccess.getPreferences();
    try {
      Optional<BuildToolKitDefinition> overwritingBtk = BuildToolKitUtil.getOverwritingToolkit(prefs);

      if (!overwritingBtk.isEmpty()) {
        // PATH is overwritten...
        Predicate<String> isPATH = n -> false;
        if (Platform.OS_WIN32.equals(Platform.getOS())) {
          // check for windows which has case-insensitive envvar names, e.g. 'pAth'
          isPATH = n -> "PATH".equalsIgnoreCase(n);
        } else {
          isPATH = n -> "PATH".equals(n);
        }

        String newPath = null;
        for (Iterator<Entry<String, String>> iter = environment.entrySet().iterator(); iter.hasNext();) {
          Entry<String, String> entry = iter.next();
          String key = entry.getKey();
          if (isPATH.test(key)) {
            // replace the value of $PATH with the value specified in the overwriting build tool kit
            newPath = CCorePlugin.getDefault().getCdtVariableManager().resolveValue(overwritingBtk.get().getPath(), "",
                null, null);
            iter.remove(); // will be added later again as 'PATH'
            break;
          }
        }
        if (newPath != null) {
          // replace $PATH
          environment.put("PATH", newPath);
        }
      }
    } catch (JsonSyntaxException ex) {
      // workbench preferences file format error
      throw new CoreException(Status.error("Error loading workbench preferences", ex));
    }

    return environment;
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.core.ExternalBuildRunner#invokeBuild(int, org.eclipse.core.resources.IProject, org.eclipse.cdt.managedbuilder.core.IConfiguration, org.eclipse.cdt.managedbuilder.core.IBuilder, org.eclipse.cdt.core.resources.IConsole, org.eclipse.cdt.core.IMarkerGenerator, org.eclipse.core.resources.IncrementalProjectBuilder, org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public boolean invokeBuild(int kind, IProject project,
      IConfiguration configuration, IBuilder builder, IConsole console,
      IMarkerGenerator markerGenerator,
      IncrementalProjectBuilder projectBuilder, IProgressMonitor monitor)
      throws CoreException {
    if (!builder.isDefaultBuildCmdOnly()) {
      // either the project build settings in 'Use default build command' or the target in the 'Build Targets' view is
      // configured to not use the default: do not inject command and args from cmake4eclipse..
      return super.invokeBuild(kind, project, configuration, builder, console, markerGenerator, projectBuilder,
          monitor);
    }

    /*
     * wrap the passed-in builder into one that gets its build command, parallelism- and stop-on-first-error args from
     * the Cmake-generator. First do a sanity check.
     */
    IBuilder supa = builder;
    do {
      if (supa.getBaseId().equals("cmake4eclipse.mbs.builder")) {
        break;
      }
    } while ((supa = supa.getSuperClass()) != null);
    if (supa != null) {
      project.deleteMarkers(MARKER_ID, false, IResource.DEPTH_INFINITE);

      final ICConfigurationDescription cfgd = ManagedBuildManager
          .getDescriptionForConfiguration(configuration);
      final IPath builderCWD = cfgd.getBuildSetting().getBuilderCWD();

      if (kind == IncrementalProjectBuilder.CLEAN_BUILD) {
        // avoid calling 'rm -rf' if it is a clean build and the build dir was
        // deleted
        if (!builderCWD.toFile().exists()) {
          return true; // is clean
        }
      }

      // try to get CMAKE_MAKE_PROGRAM entry from CMakeCache.txt...
      String buildscriptProcessorCmd = getCommandFromCMakeCache(cfgd, project, markerGenerator);
      if (buildscriptProcessorCmd == null) {
        // actually this should not happen, since cmake will abort if it cannot determine
        // the build tool,.. but the variable name might change in future
        return false;
      }
      IEclipsePreferences prefs = PreferenceAccess.getPreferences();
      final CmakeGenerator generator = BuildToolKitUtil.getEffectiveCMakeGenerator(prefs,
          BuildToolKitUtil.getOverwritingToolkit(prefs));
      builder = new CmakeBuildToolInjectorBuilder(builder, buildscriptProcessorCmd, generator);
    }
    return super.invokeBuild(kind, project, configuration, builder, console,
        markerGenerator, projectBuilder, monitor);
  }

  /**
   * Gets the {@code "CMAKE_MAKE_PROGRAM"} value from the parsed content of the
   * CMake cache file (CMakeCache.txt) corresponding to the specified
   * configuration. If the cache for the parsed content is invalid, tries to
   * parse the CMakeCache.txt file first and then caches the parsed content.
   *
   * @param cfgd            configuration
   * @param project         the current project, used to create error markers
   * @param markerGenerator used to create error markers
   * @return a value for the {@code "cmake_build_cmd"} macro or {@code null}, if none could be determined
   * @throws CoreException if an IOExceptions occurs when reading the cmake cache file
   */
  private String getCommandFromCMakeCache(ICConfigurationDescription cfgd, IProject project,
      IMarkerGenerator markerGenerator) throws CoreException {
    CMakeCacheFileInfo fi = (CMakeCacheFileInfo) cfgd.getSessionProperty(cacheFileInfo);
    if (fi == null) {
      fi = new CMakeCacheFileInfo();
    }

    // If getBuilderCWD() returns a workspace relative path, it gets garbled by CDT.
    // If garbled, make sure de.marw.cmake4eclipse.mbs.internal.BuildscriptGenerator.getBuildWorkingDir()
    // returns a full, absolute path relative to the workspace.
    final IPath builderCWD = cfgd.getBuildSetting().getBuilderCWD();

    IPath location = ResourcesPlugin.getWorkspace().getRoot().getFolder(builderCWD).getLocation();
    File file = null;
    if (location != null) {
      file = location.append("CMakeCache.txt").toFile();
    }

    if (file != null && file.isFile()) {
      final long lastModified = file.lastModified();
      if (fi.cachedCmakeBuildTool == null || lastModified > fi.cmCacheFileLastModified) {
        // internally cached value is out of date, must parse CMakeCache.txt
        fi.cachedCmakeBuildTool = null; // invalidate cache

        // parse CMakeCache.txt...
        InputStream is = null;
        try {
          is = new FileInputStream(file);
          final Set<SimpleCMakeCacheEntry> entries = new HashSet<>();
          final EntryFilter filter = new EntryFilter() {
            @Override
            public boolean accept(String key) {
              return "CMAKE_MAKE_PROGRAM".equals(key);
            }
          };
          new CMakeCacheFileParser().parse(is, filter, entries, null);
          final Iterator<SimpleCMakeCacheEntry> iter = entries.iterator();
          if (iter.hasNext()) {
            // got a CMAKE_MAKE_PROGRAM entry, update internally cached value
            fi.cmCacheFileLastModified = lastModified;
            fi.cachedCmakeBuildTool = iter.next().getValue();
            cfgd.setSessionProperty(cacheFileInfo, fi);
          } else {
            // actually this should not happen, since cmake will abort if it cannot determine
            // the build tool,.. but the variable name might change in future
            final ProblemMarkerInfo pmi = new ProblemMarkerInfo(
                ResourcesPlugin.getWorkspace().getRoot().getFolder(builderCWD), 0,
                "No CMAKE_MAKE_PROGRAM entry in file CMakeCache.txt, unable to build project",
                IMarkerGenerator.SEVERITY_ERROR_BUILD, null);
            pmi.setType(MARKER_ID);
            markerGenerator.addMarker(pmi);
          }
        } catch (IOException ex) {
          throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
              "Failed to parse file " + file, ex));
        } finally {
          if (is != null) {
            try {
              is.close();
            } catch (IOException ignore) {
            }
          }
        }
      }
    } else {
      // CMakeCache.txt does not exist
      final ProblemMarkerInfo pmi = new ProblemMarkerInfo(project, 0,
          "File CMakeCache.txt does not exist, unable to build project", IMarkerGenerator.SEVERITY_ERROR_BUILD, null);
      pmi.setType(MARKER_ID);
      markerGenerator.addMarker(pmi);
    }
    return fi.cachedCmakeBuildTool;
  }

  ////////////////////////////////////////////////////////////////////
  // inner classes
  ////////////////////////////////////////////////////////////////////
  /**
   * Info about cached CMAKE_BUILD_TOOL entry parsed from from CMakeCache.txt
   *
   * @author Martin Weber
   */
  private static class CMakeCacheFileInfo {
    /**
     * cached CMAKE_BUILD_TOOL entry from CMakeCache.txt or {@code null} if CMakeCache.txt could not be parsed
     */
    private String cachedCmakeBuildTool;
    private long cmCacheFileLastModified;
  }

  /**
   * @author Martin Weber
   */
  private static class CmakeBuildToolInjectorBuilder implements IBuilder {
    private final IBuilder delegate;
    private final String cmakeBuildTool;
    private final CmakeGenerator generator;

    /**
     * @param delegate
     *        the builder we delegate most of the methods to
     * @param cmakeBuildTool
     *        the buildscript processor command to inject (e.g. 'make')
     * @param generator
     *        the cmake generator that generated the build scripts.
     */
    public CmakeBuildToolInjectorBuilder(IBuilder delegate,
        String cmakeBuildTool, CmakeGenerator generator) {
      this.delegate = delegate;
      this.cmakeBuildTool = cmakeBuildTool;
      this.generator = generator;
    }

    @Override
    public IPath getBuildCommand() {
      return new Path(cmakeBuildTool);
    }

    @Override
    public String getBuildArguments() {
      String arg0 = delegate.getBuildArguments(); // macros are expanded
      // remove placeholders required by CDT to enable the parallelism- and stop-on-error-UI (specified in plugin.xml)
      String args = arg0.replace("$<cmake4eclipse_dyn>", "");
      // Handle ignore errors option...
      if (!delegate.isStopOnError()) {
        final String ignoreErrOption = generator.getIgnoreErrOption();
        if (ignoreErrOption != null) {
          args += ignoreErrOption;
        }
      }

      // Handle parallel build cmd
      int num = delegate.getParallelizationNum();
      String arg = generator.getParallelBuildArg(num);
      if (arg != null) {
        if (!args.isEmpty())
          args += " ";
        args += arg;
      }

      return args;
    }

    @Override
    public String getArguments() {
      return delegate.getArguments();
    }

    @Override
    public String getCommand() {
      return this.delegate.getCommand();
    }

    @Override
    public String getId() {
      return this.delegate.getId();
    }

    @Override
    public String getName() {
      return this.delegate.getName();
    }

    @Override
    public String getBaseId() {
      return this.delegate.getBaseId();
    }

    @Override
    public Version getVersion() {
      return this.delegate.getVersion();
    }

    @Override
    public void setVersion(Version version) {
      this.delegate.setVersion(version);
    }

    @Override
    public String getManagedBuildRevision() {
      return this.delegate.getManagedBuildRevision();
    }

    @Override
    public IOption createOption(IOption superClass, String Id, String name,
        boolean isExtensionElement) {
      return this.delegate.createOption(superClass, Id, name,
          isExtensionElement);
    }

    @Override
    public void setBuildAttribute(String name, String value)
        throws CoreException {
      this.delegate.setBuildAttribute(name, value);
    }

    @Override
    public String getBuildAttribute(String name, String defaultValue) {
      return this.delegate.getBuildAttribute(name, defaultValue);
    }

    @Override
    public boolean isAutoBuildEnable() {
      return this.delegate.isAutoBuildEnable();
    }

    @Override
    public IPath getBuildLocation() {
      return this.delegate.getBuildLocation();
    }

    @Override
    public void setAutoBuildEnable(boolean enabled) throws CoreException {
      this.delegate.setAutoBuildEnable(enabled);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setBuildLocation(IPath location) throws CoreException {
      this.delegate.setBuildLocation(location);
    }

    @Override
    public String getAutoBuildTarget() {
      return this.delegate.getAutoBuildTarget();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setAutoBuildTarget(String target) throws CoreException {
      this.delegate.setAutoBuildTarget(target);
    }

    @Override
    public boolean isStopOnError() {
      return this.delegate.isStopOnError();
    }

    @Override
    public void setStopOnError(boolean on) throws CoreException {
      this.delegate.setStopOnError(on);
    }

    @Override
    public boolean isIncrementalBuildEnabled() {
      return this.delegate.isIncrementalBuildEnabled();
    }

    @Override
    public boolean supportsStopOnError(boolean on) {
      return this.delegate.supportsStopOnError(on);
    }

    @Override
    public void setIncrementalBuildEnable(boolean enabled) throws CoreException {
      this.delegate.setIncrementalBuildEnable(enabled);
    }

    @Override
    public int getParallelizationNum() {
      return this.delegate.getParallelizationNum();
    }

    @Override
    public void removeOption(IOption option) {
      this.delegate.removeOption(option);
    }

    @Override
    public String getIncrementalBuildTarget() {
      return this.delegate.getIncrementalBuildTarget();
    }

    @SuppressWarnings("deprecation")
    @Override
    public IOption getOption(String id) {
      return this.delegate.getOption(id);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setIncrementalBuildTarget(String target) throws CoreException {
      this.delegate.setIncrementalBuildTarget(target);
    }

    @Override
    public void setParallelizationNum(int jobs) throws CoreException {
      this.delegate.setParallelizationNum(jobs);
    }

    @Override
    public boolean isFullBuildEnabled() {
      return this.delegate.isFullBuildEnabled();
    }

    @Override
    public void setFullBuildEnable(boolean enabled) throws CoreException {
      this.delegate.setFullBuildEnable(enabled);
    }

    @Override
    public String getFullBuildTarget() {
      return this.delegate.getFullBuildTarget();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setFullBuildTarget(String target) throws CoreException {
      this.delegate.setFullBuildTarget(target);
    }

    @Override
    public boolean supportsParallelBuild() {
      return this.delegate.supportsParallelBuild();
    }

    @Override
    public String getCleanBuildTarget() {
      return this.delegate.getCleanBuildTarget();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setCleanBuildTarget(String target) throws CoreException {
      this.delegate.setCleanBuildTarget(target);
    }

    @Override
    public IOption getOptionById(String id) {
      return this.delegate.getOptionById(id);
    }

    @Override
    public boolean isParallelBuildOn() {
      return this.delegate.isParallelBuildOn();
    }

    @Override
    public boolean isCleanBuildEnabled() {
      return this.delegate.isCleanBuildEnabled();
    }

    @Override
    public void setCleanBuildEnable(boolean enabled) throws CoreException {
      this.delegate.setCleanBuildEnable(enabled);
    }

    @Override
    public void setParallelBuildOn(boolean on) throws CoreException {
      this.delegate.setParallelBuildOn(on);
    }

    @Override
    public boolean isDefaultBuildCmd() {
      return this.delegate.isDefaultBuildCmd();
    }

    @Override
    public void setUseDefaultBuildCmd(boolean on) throws CoreException {
      this.delegate.setUseDefaultBuildCmd(on);
    }

    @Override
    public IOption getOptionBySuperClassId(String id) {
      return this.delegate.getOptionBySuperClassId(id);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setBuildCommand(IPath command) throws CoreException {
      this.delegate.setBuildCommand(command);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setBuildArguments(String args) throws CoreException {
      this.delegate.setBuildArguments(args);
    }

    @Override
    public String[] getErrorParsers() {
      return this.delegate.getErrorParsers();
    }

    @Override
    public void setErrorParsers(String[] parsers) throws CoreException {
      this.delegate.setErrorParsers(parsers);
    }

    @Override
    public Map<String, String> getExpandedEnvironment() throws CoreException {
      return this.delegate.getExpandedEnvironment();
    }

    @Override
    public IOption[] getOptions() {
      return this.delegate.getOptions();
    }

    @Override
    public Map<String, String> getEnvironment() {
      return this.delegate.getEnvironment();
    }

    @Override
    public void setEnvironment(Map<String, String> env) throws CoreException {
      this.delegate.setEnvironment(env);
    }

    @Override
    public boolean appendEnvironment() {
      return this.delegate.appendEnvironment();
    }

    @Override
    public void setAppendEnvironment(boolean append) throws CoreException {
      this.delegate.setAppendEnvironment(append);
    }

    @Override
    public boolean isManagedBuildOn() {
      return this.delegate.isManagedBuildOn();
    }

    @Override
    public void setManagedBuildOn(boolean on) throws CoreException {
      this.delegate.setManagedBuildOn(on);
    }

    @Override
    public IOptionCategory[] getChildCategories() {
      return this.delegate.getChildCategories();
    }

    @Override
    public boolean supportsBuild(boolean managed) {
      return this.delegate.supportsBuild(managed);
    }

    @Override
    public void addOptionCategory(IOptionCategory category) {
      this.delegate.addOptionCategory(category);
    }

    @Override
    public IOptionCategory getOptionCategory(String id) {
      return this.delegate.getOptionCategory(id);
    }

    @Override
    public void createOptions(IHoldsOptions superClass) {
      this.delegate.createOptions(superClass);
    }

    @Override
    public IOption getOptionToSet(IOption option, boolean adjustExtension)
        throws BuildException {
      return this.delegate.getOptionToSet(option, adjustExtension);
    }

    @Override
    public boolean needsRebuild() {
      return this.delegate.needsRebuild();
    }

    @Override
    public void setRebuildState(boolean rebuild) {
      this.delegate.setRebuildState(rebuild);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IConfigurationElement getBuildFileGeneratorElement() {
      return this.delegate.getBuildFileGeneratorElement();
    }

    @Override
    public IManagedBuilderMakefileGenerator getBuildFileGenerator() {
      return this.delegate.getBuildFileGenerator();
    }

    @Override
    public String getErrorParserIds() {
      return this.delegate.getErrorParserIds();
    }

    @Override
    public String[] getErrorParserList() {
      return this.delegate.getErrorParserList();
    }

    @Override
    public IToolChain getParent() {
      return this.delegate.getParent();
    }

    @Override
    public IBuilder getSuperClass() {
      return this.delegate.getSuperClass();
    }

    @Override
    public String getUnusedChildren() {
      return this.delegate.getUnusedChildren();
    }

    @Override
    public boolean isAbstract() {
      return this.delegate.isAbstract();
    }

    @Override
    public boolean isDirty() {
      return this.delegate.isDirty();
    }

    @Override
    public boolean isExtensionElement() {
      return this.delegate.isExtensionElement();
    }

    @Override
    public void setArguments(String makeArgs) {
      this.delegate.setArguments(makeArgs);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setBuildFileGeneratorElement(IConfigurationElement element) {
      this.delegate.setBuildFileGeneratorElement(element);
    }

    @Override
    public void setCommand(String command) {
      this.delegate.setCommand(command);
    }

    @Override
    public void setDirty(boolean isDirty) {
      this.delegate.setDirty(isDirty);
    }

    @Override
    public void setErrorParserIds(String ids) {
      this.delegate.setErrorParserIds(ids);
    }

    @Override
    public void setIsAbstract(boolean b) {
      this.delegate.setIsAbstract(b);
    }

    @Override
    public String getVersionsSupported() {
      return this.delegate.getVersionsSupported();
    }

    @Override
    public String getConvertToId() {
      return this.delegate.getConvertToId();
    }

    @Override
    public void setVersionsSupported(String versionsSupported) {
      this.delegate.setVersionsSupported(versionsSupported);
    }

    @Override
    public void setConvertToId(String convertToId) {
      this.delegate.setConvertToId(convertToId);
    }

    @Override
    public IFileContextBuildMacroValues getFileContextBuildMacroValues() {
      return this.delegate.getFileContextBuildMacroValues();
    }

    @Override
    public String getBuilderVariablePattern() {
      return this.delegate.getBuilderVariablePattern();
    }

    @Override
    public String[] getReservedMacroNames() {
      return this.delegate.getReservedMacroNames();
    }

    @Override
    public IReservedMacroNameSupplier getReservedMacroNameSupplier() {
      return this.delegate.getReservedMacroNameSupplier();
    }

    @Override
    public CBuildData getBuildData() {
      return this.delegate.getBuildData();
    }

    @Override
    public boolean isCustomBuilder() {
      return this.delegate.isCustomBuilder();
    }

    @Override
    public boolean supportsCustomizedBuild() {
      return this.delegate.supportsCustomizedBuild();
    }

    @Override
    public boolean keepEnvironmentVariablesInBuildfile() {
      return this.delegate.keepEnvironmentVariablesInBuildfile();
    }

    @Override
    public void setKeepEnvironmentVariablesInBuildfile(boolean keep) {
      this.delegate.setKeepEnvironmentVariablesInBuildfile(keep);
    }

    @Override
    public boolean canKeepEnvironmentVariablesInBuildfile() {
      return this.delegate.canKeepEnvironmentVariablesInBuildfile();
    }

    @Override
    public void setBuildPath(String path) {
      this.delegate.setBuildPath(path);
    }

    @Override
    public String getBuildPath() {
      return this.delegate.getBuildPath();
    }

    @Override
    public boolean isInternalBuilder() {
      return this.delegate.isInternalBuilder();
    }

    @Override
    public boolean matches(IBuilder builder) {
      return this.delegate.matches(builder);
    }

    @Override
    public boolean isSystemObject() {
      return this.delegate.isSystemObject();
    }

    @Override
    public String getUniqueRealName() {
      return this.delegate.getUniqueRealName();
    }

    @Override
    public ICommandLauncher getCommandLauncher() {
      return this.delegate.getCommandLauncher();
    }

    @Override
    public AbstractBuildRunner getBuildRunner() throws CoreException {
      return this.delegate.getBuildRunner();
    }

    @Override
    public boolean isDefaultBuildCmdOnly() {
      return this.delegate.isDefaultBuildCmdOnly();
    }

    @Override
    public boolean isDefaultBuildArgsOnly() {
      return this.delegate.isDefaultBuildArgsOnly();
    }

    @Override
    public void setUseDefaultBuildCmdOnly(boolean on) throws CoreException {
      this.delegate.setUseDefaultBuildCmdOnly(on);
    }

    @Override
    public void setUseDefaultBuildArgsOnly(boolean on) throws CoreException {
      this.delegate.setUseDefaultBuildArgsOnly(on);
    }
  }
}
