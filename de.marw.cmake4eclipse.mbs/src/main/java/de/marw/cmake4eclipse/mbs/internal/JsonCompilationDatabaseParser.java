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
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.cdt.build.core.scannerconfig.ScannerConfigNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CommandLauncherManager;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.language.settings.providers.ICBuildOutputParser;
import org.eclipse.cdt.core.language.settings.providers.ICListenerAgent;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsEditableProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.language.settings.providers.IWorkingDirectoryTracker;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsSerializableProvider;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.parser.ExtendedScannerInfo;
import org.eclipse.cdt.core.settings.model.CIncludePathEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.jsoncdb.core.CompileCommandsJsonParser;
import org.eclipse.cdt.jsoncdb.core.ISourceFileInfoConsumer;
import org.eclipse.cdt.jsoncdb.core.ParseRequest;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

/**
 * A ILanguageSettingsProvider that parses the file 'compile_commands.json' produced by cmake and other tools.<br>
 * NOTE: This class misuses interface ICBuildOutputParser to detect when a build did finish.<br>
 * NOTE: This class misuses interface ICListenerAgent to populate the {@link #getSettingEntries setting entries} on
 * workbench startup.
 *
 * @author Martin Weber
 */
public class JsonCompilationDatabaseParser extends LanguageSettingsSerializableProvider
    implements ICListenerAgent, ICBuildOutputParser, ILanguageSettingsEditableProvider {
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

    return entries.getSettingEntries(cfgDescription, rc);
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
    // If ICBuildSetting#getBuilderCWD() returns a workspace relative path, it is garbled.
    // It returns '${workspace_loc:/my-project-name}'.
    final IPath builderCWD = cfgDescription.getBuildSetting().getBuilderCWD();
    IPath buildRoot = ResourcesPlugin.getWorkspace().getRoot().getFolder(builderCWD).getLocation();
    if (buildRoot == null) {
      String cwd = builderCWD.toString();
      cwd = CCorePlugin.getDefault().getCdtVariableManager().resolveValue(cwd.toString(), "", null, //$NON-NLS-1$
          cfgDescription);
      buildRoot = new Path(cwd);
    }
    IPath jsonPath = buildRoot.append("compile_commands.json"); //$NON-NLS-1$
    URI jsonUri = URIUtil.toURI(jsonPath);
    IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(jsonUri);
    final IFile jsonFileRc = files[0];

    CompileCommandsJsonParser parser = new CompileCommandsJsonParser(
        new ParseRequest(jsonFileRc, new SourceFileInfoConsumer(cfgDescription),
            CommandLauncherManager.getInstance().getCommandLauncher(cfgDescription), null));
    parser.parse(monitor);
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

  /**
   * Overridden to misuse this to populate the {@link #getSettingEntries setting entries} on startup. This is either
   * called when a project is loaded or when a user adds the CMake Compilation DB provider on the providers tab in the
   * UI.<br>
   * {@inheritDoc}
   */
  @Override
  public void registerListener(ICConfigurationDescription cfgDescription) {
    if (cfgDescription != null) {
      // called as per-project provider
      if (cfgDescription.isActive()) {
        final IProject project = cfgDescription.getProjectDescription().getProject();
        WorkspaceJob job = new WorkspaceJob("Parsing compilation database of project " + project.getName()) {
          @Override
          public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
            parseAndSetEntries(cfgDescription, new NullProgressMonitor());
            return Status.OK_STATUS;
          }
        };
        job.setRule(project);
        job.schedule();
      }
    } else {
      // called as workspace (shared) provider
      List<ICConfigurationDescription> configs = new ArrayList<>();
      CCorePlugin ccp = CCorePlugin.getDefault();
      // parse JSON file for any opened project that has a ScannerConfigNature...
      for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
        try {
          if (project.isOpen() && project.hasNature(ScannerConfigNature.NATURE_ID)) {
            ICProjectDescription projectDescription = ccp.getProjectDescription(project, false);
            if (projectDescription != null) {
              ICConfigurationDescription activeConfiguration = projectDescription.getActiveConfiguration();
              if (activeConfiguration instanceof ILanguageSettingsProvidersKeeper) {
                final List<ILanguageSettingsProvider> lsps = ((ILanguageSettingsProvidersKeeper) activeConfiguration)
                    .getLanguageSettingProviders();
                for (ILanguageSettingsProvider lsp : lsps) {
                  if (PROVIDER_ID.equals(lsp.getId())) {
                    configs.add(activeConfiguration);
                    break;
                  }
                }
              }
            }
          }
        } catch (CoreException ex) {
          log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "registerListener()", //$NON-NLS-1$
              ex));
        }
      }

      if (!configs.isEmpty()) {
        WorkspaceJob job = new WorkspaceJob("Parsing compilation databases") {
          @Override
          public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
            MultiStatus status = new MultiStatus(Activator.PLUGIN_ID, IStatus.OK,
                "Problem parsing compilation databases", null);
            SubMonitor subMonitor = SubMonitor.convert(monitor, configs.size());
            for (ICConfigurationDescription cfg : configs) {
              try {
                parseAndSetEntries(cfg, subMonitor.split(1));
              } catch (CoreException e) {
                IStatus s = new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR,
                    "Error in project " + cfg.getProjectDescription().getProject().getName(), e);
                status.merge(s);
              }
            }
            return status;
          }
        };
        job.schedule();
      }
    }
  }

  /*-
   * @see org.eclipse.cdt.core.language.settings.providers.ICListenerAgent#unregisterListener()
   */
  @Override
  public void unregisterListener() {
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

    public SourceFileInfoConsumer(ICConfigurationDescription currentCfgDescription) {
      this.cfgDescription = Objects.requireNonNull(currentCfgDescription);
    }

    @Override
    public void acceptSourceFileInfo(String sourceFileName, List<String> systemIncludePaths,
        Map<String, String> definedSymbols, List<String> includePaths, List<String> macroFiles,
        List<String> includeFiles) {
      ExtendedScannerInfo info = new ExtendedScannerInfo(definedSymbols,
          systemIncludePaths.stream().toArray(String[]::new), macroFiles.stream().toArray(String[]::new),
          includeFiles.stream().toArray(String[]::new), includePaths.stream().toArray(String[]::new));
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
}
