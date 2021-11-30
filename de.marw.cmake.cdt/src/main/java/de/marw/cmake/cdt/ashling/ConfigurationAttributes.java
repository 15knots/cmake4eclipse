package de.marw.cmake.cdt.ashling;

import de.marw.cmake.cdt.internal.Plugin;

public interface ConfigurationAttributes{

	public static final String PREFIX = Plugin.PLUGIN_ID;
	public static final String TOOLCHAIN = "cmake.toolchain";
	public static final String BUILD_TOOLS = "cmake.buildtools";
	public static final String CMAKE = "cmake.bin";
}
