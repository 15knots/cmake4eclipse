package de.marw.cmake.cdt.ashling.util;


import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.envvar.IBuildEnvironmentVariable;
import org.eclipse.cdt.managedbuilder.envvar.IConfigurationEnvironmentVariableSupplier;
import org.eclipse.cdt.managedbuilder.envvar.IEnvironmentVariableProvider;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
import org.eclipse.core.runtime.Platform;

import de.marw.cmake.cdt.ashling.PersistentPreferences;
import de.marw.cmake.cdt.internal.Plugin;

public class EnvironmentVariableSupplier implements IConfigurationEnvironmentVariableSupplier {
	
	@Override
	public IBuildEnvironmentVariable getVariable(String variableName, IConfiguration configuration,
			IEnvironmentVariableProvider provider) {
		if (PathEnvironmentVariable.isVar(variableName)) {
			return PathEnvironmentVariable.create(configuration);
		} else {
			return null;
		}
	}

	@Override
	public IBuildEnvironmentVariable[] getVariables(IConfiguration configuration,
			IEnvironmentVariableProvider provider) {
		IBuildEnvironmentVariable path = PathEnvironmentVariable.create(configuration);
		if (path != null) {
			return new IBuildEnvironmentVariable[] { path };
		} else {
			return new IBuildEnvironmentVariable[0];
		}
	}

	private static class PathEnvironmentVariable implements IBuildEnvironmentVariable {
		
		private static String fPluginID="de.marw.cmake.cdt";
		private static String name = "PATH"; //$NON-NLS-1$
		private String path;

		private PathEnvironmentVariable(String[] paths) {
			this.path = String.join(getDelimiter(), paths);
		}

		public static PathEnvironmentVariable create(IConfiguration configuration) {
			PersistentPreferences fpPersistentPreferences=new PersistentPreferences(fPluginID);
			String buildToolPath=fpPersistentPreferences.getBuildToolPath();
			String cmakePath=fpPersistentPreferences.getCMakePath();
			String toolchainPath=fpPersistentPreferences.getToolchainPath();
			String[] paths = {resolveMacros(cmakePath, configuration), resolveMacros(toolchainPath, configuration), resolveMacros(buildToolPath, configuration)};
			return new PathEnvironmentVariable(paths);
		}

		private static String resolveMacros(String str, IConfiguration configuration) {

			String result = str;
			try {
				result = ManagedBuildManager.getBuildMacroProvider().resolveValue(str, "", " ", //$NON-NLS-1$ //$NON-NLS-2$
						IBuildMacroProvider.CONTEXT_CONFIGURATION, configuration);
			} catch (Exception e) {
				Plugin.logErrorMessage("resolveMacros " + e.getMessage());
			}
			return result;

		}

		public static boolean isVar(String name) {
			// Windows has case insensitive env var names
			return Platform.getOS().equals(Platform.OS_WIN32) ? name.equalsIgnoreCase(PathEnvironmentVariable.name)
					: name.equals(PathEnvironmentVariable.name);
		}

		@Override
		public String getDelimiter() {
			return Platform.getOS().equals(Platform.OS_WIN32) ? ";" : ":"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public int getOperation() {
			return IBuildEnvironmentVariable.ENVVAR_REMOVE;
		}

		@Override
		public String getValue() {
			return path;
		}

	}

	// ------------------------------------------------------------------------
}
