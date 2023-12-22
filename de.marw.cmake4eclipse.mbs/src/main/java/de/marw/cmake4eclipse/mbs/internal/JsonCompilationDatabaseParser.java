/*******************************************************************************
 * Copyright (c) 2021 Martin Weber
.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.internal;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CommandLauncherManager;
import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.language.settings.providers.ICBuildOutputParser;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsEditableProvider;
import org.eclipse.cdt.core.language.settings.providers.IWorkingDirectoryTracker;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsSerializableProvider;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.parser.ExtendedScannerInfo;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.core.settings.model.CIncludePathEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.docker.launcher.ContainerCommandLauncher;
import org.eclipse.cdt.docker.launcher.DockerLaunchUIPlugin;
import org.eclipse.cdt.jsoncdb.core.CompileCommandsJsonParser;
import org.eclipse.cdt.jsoncdb.core.IParserPreferences;
import org.eclipse.cdt.jsoncdb.core.IParserPreferencesAccess;
import org.eclipse.cdt.jsoncdb.core.ISourceFileInfoConsumer;
import org.eclipse.cdt.jsoncdb.core.ParseRequest;
import org.eclipse.cdt.managedbuilder.buildproperties.IOptionalBuildProperties;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import de.marw.cmake4eclipse.mbs.console.CdtConsoleConstants;

/**
 * A ILanguageSettingsProvider that parses the file 'compile_commands.json' produced by cmake and other tools.<br>
 * NOTE: This class misuses interface ICBuildOutputParser to detect when a build did finish.<br>
 * NOTE: This class misuses interface ICListenerAgent to populate the {@link #getSettingEntries setting entries} on
 * workbench startup.
 *
 * @author Martin Weber
 */
public class JsonCompilationDatabaseParser extends LanguageSettingsSerializableProvider
    implements ICBuildOutputParser, ILanguageSettingsEditableProvider {
  static final String PROVIDER_ID = "de.marw.cmake4eclipse.mbs.internal.lsp.JsonCompilationDatabaseParser"; //$NON-NLS-1$

  private static final ILog log = Activator.getDefault().getLog();

  /**
   * storage to keep settings entries
   */
  private PerConfigSettingEntries entries = new PerConfigSettingEntries();

  private ICConfigurationDescription currentCfgDescription;

  @Override
  public void configureProvider(String id, String name, List<String> languages, List<ICLanguageSettingEntry> entries,
      Map<String, String> properties) {
    List<String> languages0 = Arrays.asList(
        // supported OOTB by plugin org.eclipse.cdt.jsoncdb.core..
        "org.eclipse.cdt.core.gcc", //$NON-NLS-1$
        // supported OOTB by plugin org.eclipse.cdt.jsoncdb.core..
        "org.eclipse.cdt.core.g++", //$NON-NLS-1$
        // does not harm here, but supported only if user did install plugin org.eclipse.cdt.jsoncdb.nvidia..
        // TODO we need a way to detect 3rd party plugins that provide an extension to
        // extension-point "org.eclipse.cdt.jsoncdb.core#detectionParticipant"
        "com.nvidia.cuda.toolchain.language.cuda.cu"); //$NON-NLS-1$
    if (languages != null) {
      languages0.addAll(languages);
      languages0 = languages0.stream().distinct().collect(Collectors.toList());
    }
    super.configureProvider(id, name, languages0, entries, properties);
  }

  @Override
  public List<ICLanguageSettingEntry> getSettingEntries(ICConfigurationDescription cfgDescription, IResource rc,
      String languageId) {
    if (cfgDescription == null || rc == null
        || !(rc.getType() == IResource.FILE || rc.getType() == IResource.PROJECT)) {
      // speed up, we do not provide global (workspace) lang settings..
      return null;
    }
    List<ICLanguageSettingEntry> entries2 = entries.getSettingEntries(cfgDescription, rc);
    if (entries2 == null && !entries.hasSettingEntries(cfgDescription) ) {
      // compile_commands.json file has not been parsed yet...
      try {
        if (ResourcesPlugin.getWorkspace().isTreeLocked()
            || cfgDescription.getProjectDescription().isCdtProjectCreating()) {
          // avoid ResourceException: The resource tree is locked for modifications during project creation
          return null;
        }
        parseAndSetEntries(cfgDescription, new NullProgressMonitor());
        entries2 = entries.getSettingEntries(cfgDescription, rc);
      } catch (CoreException ex) {
        log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Parsing compilation databases", //$NON-NLS-1$
            ex));
      }
    }
    return entries2;
  }

  private void setScannerInfos(ICConfigurationDescription cfgDescription,
      Map<String, ExtendedScannerInfo> infoPerResource) {
    ArrayList<ICLanguageSettingEntry> projectIncludePaths = new ArrayList<>();
    List<ICElement> changedFileSettings = new ArrayList<>();
    final ICSourceEntry[] sourceEntries = cfgDescription.getSourceEntries();

    for (Entry<String, ExtendedScannerInfo> entry : infoPerResource.entrySet()) {
      // NOTE entry.getKey is the source file in cmake notation here
      IFile file = getFileForCMakePath(entry.getKey());
      if (file != null) {
        IPath prjRelPath = file.getFullPath();
        ArrayList<ICLanguageSettingEntry> sEntries = new ArrayList<>();
        Consumer<? super CIncludePathEntry> incPathAdder = lse -> {
          sEntries.add(lse);
          /*
           * compile_commands.json holds entries per-file only and does not contain per-project or per-folder entries.
           * For include path, ALSO add these ONCE to the project resource to make them show up in the UI in the
           * includes folder...
           */
          if (!projectIncludePaths.contains(lse)) {
            projectIncludePaths.add(lse);
          }
        };

        ExtendedScannerInfo info = entry.getValue();
        // convert to ICSettingEntry objects...
        info.getDefinedSymbols()
            .forEach((k, v) -> sEntries.add(CDataUtil.createCMacroEntry(k, v, ICSettingEntry.READONLY)));
        Stream.of(info.getMacroFiles()).map(s -> CDataUtil.createCMacroFileEntry(s, ICSettingEntry.READONLY))
            .forEach(lse -> sEntries.add(lse));
        Stream.of(info.getLocalIncludePath())
            .map(s -> CDataUtil.createCIncludePathEntry(s, ICSettingEntry.LOCAL | ICSettingEntry.READONLY))
            .forEach(incPathAdder);
        Stream.of(info.getIncludePaths()).map(s -> CDataUtil.createCIncludePathEntry(s, ICSettingEntry.READONLY))
            .forEach(incPathAdder);
        Stream.of(info.getIncludeFiles()).map(s -> CDataUtil.createCIncludeFileEntry(s, ICSettingEntry.READONLY))
            .forEach(lse -> sEntries.add(lse));

        sEntries.trimToSize();
        List<ICLanguageSettingEntry> oldEntries = entries.setSettingEntries(cfgDescription, file, sEntries);
        if (oldEntries != null && !(oldEntries.size() == sEntries.size() && oldEntries.containsAll(sEntries)
            && sEntries.containsAll(oldEntries))) {
          // settings for resource changed
          if (!CDataUtil.isExcluded(prjRelPath, sourceEntries)) {
            // not excluded, we should notify the indexer
            changedFileSettings.add(CoreModel.getDefault().create(file));
          }
        }
      }
    }

    // add include paths for the project resource to populate the 'Includes' folder of the Project Explorer
    entries.setSettingEntries(cfgDescription, cfgDescription.getProjectDescription().getProject(), projectIncludePaths);

    if (!changedFileSettings.isEmpty()) {
      // settings for file resources changed: notify indexer to make opened editors update the display
      try {
        CCorePlugin.getIndexManager().update(changedFileSettings.toArray(new ICElement[changedFileSettings.size()]),
            IIndexManager.UPDATE_ALL);
      } catch (CoreException ex) {
        log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to update CDT index", //$NON-NLS-1$
            ex));
      }
    }
  }

  /**
   * Gets an IFile object that corresponds to the source file name given in CMake notation.
   *
   * @param sourceFileName the name of the source file, in CMake notation. Note that on windows, CMake writes filenames
   *                       with forward slashes (/) such as {@code H://path//to//source.c}.
   * @return a IFile object or <code>null</code>
   */
  private static IFile getFileForCMakePath(String sourceFileName) {
    org.eclipse.core.runtime.Path path = new org.eclipse.core.runtime.Path(
        new File(sourceFileName).toURI().getSchemeSpecificPart());
    IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(path);
    // TODO maybe we need to introduce a strategy here to get the workbench resource
    // Possible build scenarios:
    // 1) linux native: should be OK as is
    // 2) linux host, building in container: should be OK as is
    // 3) windows native: Path.fromOSString()?
    // 4) windows host, building in linux container: ??? needs testing on windows
    return file;
  }

  /**
   * Parses the content of the 'compile_commands.json' file corresponding to the current ICConfigurationDescription and
   * store the parsed content as ICLanguageSettingEntryS.
   *
   * @param cfgDescription
   * @param monitor
   *
   * @throws CoreException
   */
  private void parseAndSetEntries(ICConfigurationDescription cfgDescription, IProgressMonitor monitor)
      throws CoreException {
    final CCorePlugin ccp = CCorePlugin.getDefault();
    // If ICBuildSetting#getBuilderCWD() returns a workspace relative path, it is garbled.
    // It returns '${workspace_loc:/my-project-name}'. MBS Builder.getDefaultBuildPath() does that.
    final IPath builderCWD = cfgDescription.getBuildSetting().getBuilderCWD();
    if("${workspace_loc:".equals( builderCWD.segment(0))) {
      // occasionally (during project creation?) our BuildscriptGenerator did not kick in.
      // Assume no compile_commands.json file was generated, hence no need to try parsing at all...
      // This should eliminate the infamous 'Resource '/home' does not exist.' exception
      return;
    }
/*
 *     // help detecting the 'Resource '/home' does not exist.' exception cause
    if( !cfgDescription.getProjectDescription().getProject().getName().equals( builderCWD.segment(0))){
      System.err.format("CDT bug??? ICBuildSetting#getBuilderCWD() returned a path that does not match the project name!%n\t"
          + "builderCWD= '%s' vs '%s' (help detecting the 'Resource '/home' does not exist.' exception cause)%n", builderCWD, cfgDescription.getProjectDescription().getProject());
    }
 */

    final String cwd = ccp.getCdtVariableManager().resolveValue(builderCWD.toString(), "", null, //$NON-NLS-1$
        cfgDescription);
/*
    // help detecting the 'Resource '/home' does not exist.' exception cause
    if (!cfgDescription.getProjectDescription().getProject().equals(buildFolder.getProject())) {
      System.err.format(
          "CDT bug??? dtVariableManager()#resolveValue() returned a project that is not the project to build!%n\t"
              + "buildFolder.getProject()= '%s' vs '%s' (help detecting the 'Resource '/home' does not exist.' exception cause)%n",
          buildFolder.getProject(), cfgDescription.getProjectDescription().getProject());
    }
*/
    final IFile jsonFileRc = ResourcesPlugin.getWorkspace().getRoot()
        .getFile(new Path(cwd).append("compile_commands.json")); //$NON-NLS-1$

    // get the launcher that runs in docker container, if any
    IConfiguration cfg = ManagedBuildManager.getConfigurationForDescription(cfgDescription);
    IBuilder builder = cfg.getEditableBuilder();
    // IBuilder#getCommandLauncher() might return the same object for different projects causing
    // Bug 580045 - Spurious java.lang.IllegalThreadStateException: Process not Terminated in
    // o.e.c.jsoncdb.core.CompileCommandsJsonParser
    // So get a fresh one for the configuration
    ICommandLauncher launcher = CommandLauncherManager.getInstance().getCommandLauncher(cfgDescription);
    launcher.setProject(cfgDescription.getProjectDescription().getProject());

    IConsole console = null;
    IParserPreferences prefs = EclipseContextFactory
        .getServiceContext(FrameworkUtil.getBundle(IParserPreferencesAccess.class).getBundleContext())
        .get(IParserPreferencesAccess.class).getWorkspacePreferences();
    if (prefs.getAllocateConsole()) {
      console = ccp.getConsole(CdtConsoleConstants.BUILTINS_DETECTION_CONSOLE_ID);
      launcher.showCommand(true);
    }

    Map<String, String> envMap = prepareEnvironment(cfgDescription, builder);
    launcher = new EnvCommandlauncher(launcher, envMap);

    IContainerToHostPathConverter cthpc = path -> path;
    // docker: include path mapping for copied header files
    IOptionalBuildProperties props = cfg.getOptionalBuildProperties();
    if (props != null) {
      Bundle dockerBundle = Platform.getBundle(DockerLaunchUIPlugin.PLUGIN_ID); // dependency is optional
      if (dockerBundle != null) {
        boolean runsInContainer = Boolean
            .parseBoolean(props.getProperty(ContainerCommandLauncher.CONTAINER_BUILD_ENABLED));
        if (runsInContainer) {
          String connectionName = props.getProperty(ContainerCommandLauncher.CONNECTION_ID);
          String imageName = props.getProperty(ContainerCommandLauncher.IMAGE_ID);
          if (connectionName != null && !connectionName.isEmpty() && imageName != null && !imageName.isEmpty()) {
            // reverse engineered from
            // org.eclipse.cdt.docker.launcher.ContainerCommandLauncherFactory#verifyLanguageSettingEntries()
            IPath pluginPath = Platform.getStateLocation(dockerBundle);
            IPath hostDir = pluginPath.append("HEADERS") //$NON-NLS-1$
                .append(getCleanName(connectionName)).append(getCleanName(imageName));
            IProject project = cfgDescription.getProjectDescription().getProject();
            cthpc = new ContainerToHostPathConverter(hostDir, project);
          }
        }
      }
    }

    CompileCommandsJsonParser parser = new CompileCommandsJsonParser(
        new ParseRequest(jsonFileRc, new SourceFileInfoConsumer(cfgDescription, cthpc), launcher, console));
    parser.parse(monitor);
  }

  /*
   * Copied from org.eclipse.cdt.docker.launcher.ContainerCommandLauncherFactory#getCleanName()
   */
  private static String getCleanName(String name) {
    String cleanName = name.replace("unix:///", "unix_"); //$NON-NLS-1$ //$NON-NLS-2$
    cleanName = cleanName.replace("tcp://", "tcp_"); //$NON-NLS-1$ //$NON-NLS-2$
    return cleanName.replaceAll("[:/.]", "_"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private static Map<String, String> prepareEnvironment(ICConfigurationDescription cfgDes, IBuilder builder)
      throws CoreException {
    Map<String, String> envMap = new HashMap<>();
    if (builder.appendEnvironment()) {
      IEnvironmentVariableManager mngr = CCorePlugin.getDefault().getBuildEnvironmentManager();
      IEnvironmentVariable[] vars = mngr.getVariables(cfgDes, true);
      for (IEnvironmentVariable var : vars) {
        envMap.put(var.getName(), var.getValue());
      }
    }

    // Add variables from build info
    Map<String, String> builderEnv = builder.getExpandedEnvironment();
    if (builderEnv != null) {
      envMap.putAll(builderEnv);
    }

    // replace $PATH, if necessary
    BuildToolKitUtil.replacePathVarFromBuildToolKit(envMap);
    // English language is set for parser because it relies on English messages in the output of the 'gcc -v'.
    // On POSIX (Linux, UNIX) systems reset language variables to default (English) with UTF-8 encoding since GNU
    // compilers can handle only UTF-8 characters.
    // Include paths with locale characters will be handled properly regardless of the language as long as the encoding
    // is set to UTF-8.
    envMap.put("LANGUAGE", "en"); // override for GNU gettext
    envMap.put("LC_ALL", "C.UTF-8"); // for other parts of the system libraries

    return envMap;
  }

  /*-
   * interface ICBuildOutputParser
   */
  @Override
  public void startup(ICConfigurationDescription cfgDescription, IWorkingDirectoryTracker cwdTracker)
      throws CoreException {
    currentCfgDescription = cfgDescription;
  }

  /*-
   * interface ICBuildOutputParser
   */
  @Override
  public void shutdown() {
    try {
      parseAndSetEntries(currentCfgDescription, new NullProgressMonitor());
    } catch (CoreException ex) {
      log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "shutdown()", ex)); //$NON-NLS-1$
    }
    // release resources for garbage collector
    currentCfgDescription = null;
  }

  /**
   * Invoked for each line in the build output.
   */
  // interface ICBuildOutputParser
  @Override
  public boolean processLine(String line) {
    // nothing to do, we parse on shutdown...
    return false;
  }

  @Override
  public JsonCompilationDatabaseParser clone() throws CloneNotSupportedException {
    return (JsonCompilationDatabaseParser) super.clone();
  }

  @Override
  public JsonCompilationDatabaseParser cloneShallow() throws CloneNotSupportedException {
    return (JsonCompilationDatabaseParser) super.cloneShallow();
  }

  private static class PerConfigSettingEntries {

    /**
     * Storage to keep settings entries. Key is {@link ICConfigurationDescription#getId()}
     */
    private Map<String, Map<IResource, List<ICLanguageSettingEntry>>> storages = new WeakHashMap<>();
    private Object storagesLock = new Object();

    /**
     * @return the previous entries associated with the given ICConfigurationDescription and IResource or
     *         <code>null</code> if none where associated
     */
    public List<ICLanguageSettingEntry> setSettingEntries(ICConfigurationDescription cfgDescription, IResource rc,
        List<ICLanguageSettingEntry> entries) {
      Objects.requireNonNull(cfgDescription, "cfgDescription"); //$NON-NLS-1$
      synchronized (storagesLock) {
        Map<IResource, List<ICLanguageSettingEntry>> store = storages.get(cfgDescription.getId());
        if (store == null) {
          store = new HashMap<>();
          storages.put(cfgDescription.getId(), store);
        }
        return store.put(rc, entries);
      }
    }

    /**
     * @return whether this object holds ICLanguageSettingEntry for the specified configuration
     */
    public boolean hasSettingEntries(ICConfigurationDescription cfgDescription) {
      Objects.requireNonNull(cfgDescription, "cfgDescription"); //$NON-NLS-1$
      synchronized (storagesLock) {
        return storages.containsKey(cfgDescription.getId());
      }
    }

    /**
     * Gets the ICLanguageSettingEntry for the specified configuration and file.
     *
     * @return the settings, or {@code null} in none exist
     */
    public List<ICLanguageSettingEntry> getSettingEntries(ICConfigurationDescription cfgDescription, IResource rc) {
      synchronized (storagesLock) {
        Map<IResource, List<ICLanguageSettingEntry>> store = storages.get(cfgDescription.getId());
        if (store != null) {
          return store.get(rc);
        }
      }
      return null;
    }
  } // PerConfigSettingEntries

  private class SourceFileInfoConsumer implements ISourceFileInfoConsumer {
    /**
     * gathered IScannerInfo objects or <code>null</code> if no new IScannerInfo was received
     */
    private Map<String, ExtendedScannerInfo> infoPerResource = new HashMap<>();
    private boolean haveUpdates;
    private final ICConfigurationDescription cfgDescription;
    private Function<String, String> containerToHostPathConverter;

    public SourceFileInfoConsumer(ICConfigurationDescription currentCfgDescription,
        IContainerToHostPathConverter containerToHostPathConverter) {
      this.cfgDescription = Objects.requireNonNull(currentCfgDescription);
      Objects.requireNonNull(containerToHostPathConverter, "containerToHostPathConverter");
      this.containerToHostPathConverter = p -> containerToHostPathConverter.convert(p);
    }

    @Override
    public void acceptSourceFileInfo(String sourceFileName, List<String> systemIncludePaths,
        Map<String, String> definedSymbols, List<String> includePaths, List<String> macroFiles,
        List<String> includeFiles) {
      ExtendedScannerInfo info = new ExtendedScannerInfo(definedSymbols,
          systemIncludePaths.stream().map(containerToHostPathConverter).toArray(String[]::new),
          macroFiles.stream().toArray(String[]::new),
          includeFiles.stream().map(containerToHostPathConverter).toArray(String[]::new),
          includePaths.stream().map(containerToHostPathConverter).toArray(String[]::new));
      infoPerResource.put(sourceFileName, info);
      haveUpdates = true;
    }

    @Override
    public void shutdown() {
      if (haveUpdates) {
        // we received updates
        JsonCompilationDatabaseParser.this.setScannerInfos(cfgDescription, infoPerResource);
        infoPerResource = null;
        haveUpdates = false;
      }
    }
  } // IndexerInfoConsumer

  /**
   * An {@code ICommandLauncher} that passes the environment variables used during the project build phase to the
   * compilers that perform the built-in detection.
   *
   * @author Martin Weber
   */
  private static class EnvCommandlauncher implements ICommandLauncher {
    private final ICommandLauncher delegate;
    private Map<String, String> envMap;

    EnvCommandlauncher(ICommandLauncher delegate, Map<String, String> envMap) {
      this.delegate = Objects.requireNonNull(delegate);
      this.envMap = envMap;
    }

    @Override
    public Process execute(IPath commandPath, String[] args, String[] env, IPath workingDirectory,
        IProgressMonitor monitor) throws CoreException {
      if (env != null) {
        for (String envStr : env) {
          // Split "ENV=value" and put in envMap
          int pos = envStr.indexOf('=');
          if (pos < 0)
            pos = envStr.length();
          String key = envStr.substring(0, pos);
          String value = envStr.substring(pos + 1);
          envMap.put(key, value);
        }
      }
      String[] effEnv = envMap.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new);

      return delegate.execute(commandPath, args, effEnv, workingDirectory, monitor);
    }

    @Override
    public void setProject(IProject project) {
      delegate.setProject(project);
    }

    @Override
    public IProject getProject() {
      return delegate.getProject();
    }

    @Override
    public void showCommand(boolean show) {
      delegate.showCommand(show);
    }

    @Override
    public String getErrorMessage() {
      return delegate.getErrorMessage();
    }

    @Override
    public void setErrorMessage(String error) {
      delegate.setErrorMessage(error);
    }

    @Override
    public String[] getCommandArgs() {
      return delegate.getCommandArgs();
    }

    @Override
    public Properties getEnvironment() {
      return delegate.getEnvironment();
    }

    @Override
    public String getCommandLine() {
      return delegate.getCommandLine();
    }

    @Override
    public int waitAndRead(OutputStream out, OutputStream err) {
      return delegate.waitAndRead(out, err);
    }

    @Override
    public int waitAndRead(OutputStream output, OutputStream err, IProgressMonitor monitor) {
      return delegate.waitAndRead(output, err, monitor);
    }
  } // EnvCommandlauncher

  /**
   * @author Martin Weber
   */
  private interface IContainerToHostPathConverter {
    /**
     * Converts a file system path inside a container to a local path that contains the header files that are copied out
     * of the container.
     *
     * @param pathInContainer the path inside the container
     * @return the converted path
     */
    String convert(String pathInContainer);
  }

  private static class ContainerToHostPathConverter implements IContainerToHostPathConverter {
    private final IPath hostDir;
    private IPath projectRoot;

    /**
     * @param hostDir
     * @param project
     */
    public ContainerToHostPathConverter(IPath hostDir, IProject project) {
      this.hostDir = Objects.requireNonNull(hostDir);
      Objects.requireNonNull(project, "project");
      projectRoot = project.getLocation();
    }

    @Override
    public String convert(String pathInContainer) {
      if (projectRoot.isPrefixOf(new Path(pathInContainer))) {
        // got a path below project root
        return pathInContainer;
      }
      return hostDir.append(pathInContainer).toString();
    }
  }
}
