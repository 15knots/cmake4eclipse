# Cmake4eclipse Change Log

## 3.0.2 (2022-02-07)
### Changes
- Closed #167: bison from <MSYS2_ROOT>\usr\bin fails when run for projects targeting mingw32 on windows.
- Add support to export and import Cmake4eclipse workbench preferences.

## 3.0.1 (2022-01-31)
### Changes
- Closed #164: NPE in BuildToolKitUtil.getEffectiveCMakeGenerator() when workspace preferences for cmake4eclipse has never been saved.

## 3.0.0 (2022-01-17)
### Changes
- Closed #157: For CDT indexer support, switch to the compilation database parser coming with CDT.
- Closed #158: Move machine specific settings from project to workbench preferences.
Compatibility Note: Eclipse projects creating with earlier version of cmake4eclipse should continue to work but
users are strongly recommended to switch project properties to use the new `CMake driven` tool chain.

#### System Requirements
- CDT v 10.5 or newer and Eclipse v 2021-12 or newer
- Java 11 or newer

## 2.1.4 (2021-03-30)
### Changes
- Closed #147: Retain order of compiler-built-in include directories.

## 2.1.3 (2020-11-17)
### Changes
- Take changes to *.cmake files into account when determining whether to re-run cmake.
- Closed #145: Add support for clang `--target` option. (Thanks to Ghaith Hachem).

## 2.1.2 (2020-07-11)
### Changes
- Closed #138: Recognize GCC/clang -include and -imacros compiler flags.

## 2.1.1 (2020-03-10)
### Changes
- Fix: Missing bundles for ARM, HPE-nonstop and Intel support in the `cmake4eclipse Extras` section in the repository.
- Raised severity level to WARNING for markers related to indexer support.
- Changed license to EPL 2.0

## 2.1.0 (2020-02-04)
### Changes
- Syntax highlighting support for NVidia and Microsoft compilers are no longer supported out-of-the box. Support is now
  handled through separate plug-ins. End users will have to install these separate plug-ins to get support for the compilers.
- Closed #128: GNU C++ CMAKE_EXPORT_COMPILE_COMMANDS Compiler Built-ins empty.
- Additional unit tests for compiler detection participants.

## 2.0.1 (2020-01-21)
### Changes
- Fix 127: Installation impossible due to dependency on bundle org.eclipse.ui.trace.
- Fix: Make builtins.DetectorConsole visible again (regression).

## 2.0.0 (2019-12-20)
### Changes
- Added a tool-chain `CMake driven` that will configure the proper builder when selected to make project configuration easier.
- Added a project type `CMake driven` to the CDT `New C Project` and `New C+ Project` wizard to ease in project creation.
- Deprecated `CMake Builder (GNU Make)`. Existing projects will still use that builder but it cannot be selected in the UI any longer.
- Added an extension point `lspDetectionParticipant`, allowing 3rd-party compiler vendors to integrate a compiler 
  for improved syntax highlighting in the CDT's C/C++ editors by simply providing an Eclipse plug-in. The 
  CMAKE_EXPORT_COMPILE_COMMANDS Parsers of this plug.in will pick-up the `lspDetectionParticipant` extensions to detect
  preprocessor macros and include directories.
- Syntax highlighting support for ARM.com and Intel compilers are no longer supported out-of-the box. Support is now
  handled through separate plug-ins that provide implementations of the `lspDetectionParticipant` extension point.  
  End users will have to install these separate plug-ins to get support for the compilers.
- Closed #119: Support for HPE non-stop compilers. Note this has not been tested yet.

## 1.17.0 (2019-09-30)
### Changes
- CMAKE_EXPORT_COMPILE_COMMANDS Parser: Allow compiler paths in quotes.
- Close #122: CMAKE_EXPORT_COMPILE_COMMANDS Parser does not recognize /some/path/arm-none-eabi-c++.

## 1.16.0 (2019-09-21)
### Changes
- Close #117: CMAKE_EXPORT_COMPILE_COMMANDS Parser: support ARM compiler toolchain.
- Make windows short file name expansion more robust.
- Add tracing support.

## 1.15.2 (2019-09-07)
### Changes
- make short file name expansion on windows more robust, log file-name on error.
- fix IndexOutOfBoundsException during built-ins detection.

## 1.15.1 (2019-07-01)
### Changes
- Fix #116: CMake errors are not detected if a project is re-build.

## 1.15.0 (2019-05-16)
### Changes
- Fix #96 and #108: Pass compiler flags that affect compiler built-ins detection to the compiler when detecting built-ins.

## 1.14.1 (2019-04-13)
### Changes
- Fix #112: cmake cache entries only saved for one configuration.
- Reviewed online help.

## 1.14.0 (2019-02-26)
### Changes
- Fix #101: ParserDetecter for MSVC cannot distinguish between C and C++.
- Fix #95: Provide programmatic access to CMake configuration.
- Fix UnsupportedOperationException at CompileCommandsJsonParser$TimestampedLanguageSettingsStorage.addBuiltinsDetector().
- Fix NPE when build is running in container.
- Fix RuntimeException: "Ignored reentrant call while viewer is busy" in settings providers on workbench startup.

## 1.13.0 (2019-01-03)
### Changes
- Fix #94: CMAKE_EXPORT_COMPILE_COMMANDS parser fails to parse inline-command-files @<<...<<
- Fix #97: Adjust documentation to newer CDT versions.
- CMAKE_EXPORT_COMPILE_COMMANDS Built-ins Parser: run detection in container, if build runs in container.
- UI/doc: Use *Cmake4eclipse* instead of just *CMake* to avoid confusion with CDT`s new cmake support.
- Add optional console view for compiler built-ins detection (#96).
- Fix: CMAKE_EXPORT_COMPILE_COMMANDS * Parser settings were not persisted for projects.
- Fix #99: CMAKE_EXPORT_COMPILE_COMMANDS Parser for MSVC does not cover "-" flags.

## 1.12.2 (2018-09-23)
### Changes
- Change related to #73 (CMake Error Parser): Deleted the old parser (which was based on CDT output handling and gave poor results) and re-wrote it from scratch.
Note that the new parser does not claim to detect *any* cmake message; more input from the community is required to detect more messages. 
- Removed deprecated CMake Build Output Parser.

## 1.12.1 (2018-07-28)
### Changes
- Fixed #86: Parsing compile_commands.json creates duplicate entries.
- Fixed #85: Poor workbench startup performance due to doing a IIndexManger.reindex().
- Fixed #84: CMAKE_EXPORT_COMPILE_COMMANDS Parser no longer detects macros specified on a command-line.
- Fixed #79: CMAKE_EXPORT_COMPILE_COMMANDS Parser does not detect compiler-built-in macros and include paths.
- Builtins-detector get no longer started in non-existing CWD.
- Plugin now requires Java 8, Eclipse 4.5 (mars) and CDT 8.7 to run.
- Reworked documentation (a little).

## 1.12.0 (2018-07-04)
### Changes
- Fixed #79: CMAKE_EXPORT_COMPILE_COMMANDS Parser does not detect compiler-built-in macros and include paths. (Forget CDT's GCC-builtin-specs-detector:-)

## 1.11.2 (2018-05-23)
### Changes
- Fixed #78: NPE when running builder 'CDT Builder' on project.
- Fixed #80: Custom build command is not used verbatim.

## 1.11.1 (2018-05-02)
### Changes
- Eliminate duplicate mnemonics in UI.
- Build in docker container: Fix NPE occurring in CDT 9.4+ with docker build launcher.
- Build in docker container: let CDT copy header files back when building in a docker container.

## 1.11.0 (2018-03-12)
### Changes
- Fixed #52: Allow to run parallel builds.
- Fail the build if build tool name cannot be determined from CMakeCache.txt.

## 1.10.0 (2018-02-14)
### Changes
- Fixed #65: Recognize nvcc as a CUDA compiler and integrate with the Nsight Eclipse Plugins.
- Merge pull request #67 from @havogt: Allow -isystem without whitespace.
- Fixed #68: Allow to parse -isystem=<path> (nvcc compiler).

## 1.9.1 (2018-01-24)
### Changes
- Fixed #64: Project is fully reindexed after each build.

## 1.9.0 (2017-12-29)
### Changes
- Enhancement: Improve diagnostic messages during build-script generation.
- Enhancement #23: Handle CDT variables instead of just eclipse core variables.
- Enhancement #58: CMAKE_EXPORT_COMPILE_COMMANDS Parser now evaluates compiler options passed in a "response file".
- Enhancement #52: Make 'CMake Make Builder' GNU-make specific and allow to build in parallel. Builder renamed to  'CMake Builder (GNU Make)'

## 1.8.0 (2017-11-18)
### Changes
- Enhancement #54: Support variable expansion in build output location.
- Fixed #55: NPE if project is of plain make-nature (no managed build).
- Fixed #51: No error message in build log, when cmake executable could not be found.
- Revised online documentation.
- Enhancement #57: Add workaround for relative include paths produced by cmake`s ninja generator.

## 1.7.0 (2017-09-26)
### Changes
- Enhancement #29: make the Output directory configurable
- Fixed #48 NPE in project configuration when OK button is clicked

## 1.6.0 (2017-09-13)
### Changes
- Enhancement #46 Populate CMake cache entries from a file
- Enhancement: Added mnemonics to UI
- Fixed: "ResourceException: The resource tree is locked for modifications" when opening project.
- Fixed #38: Build occurs in eclipse install directory.

## 1.5.0 (2017-08-26)
### Changes
- fixed #43 CMAKE_EXPORT_COMPILE_COMMANDS Parser should support different versions of compilers
- Fixed #44: CMAKE_EXPORT_COMPILE_COMMANDS Parser fails for command paths using a forward slash
- CMAKE_EXPORT_COMPILE_COMMANDS Parser show problems in Problems View instead of writing them to the application log file

## 1.4.9 (2017-07-18)
### Changes
- Fixed #36: Variable substitution for CMake executable
- Added documentation on project builders
- Fixed #41: CMAKE_EXPORT_COMPILE_COMMANDS Parser doesn't work if compiler command doesn't contain full path
- Fixed #39: Enabling build output parser does not pass necessary arguments to cmake

## 1.4.8 (2017-05-22)
### Changes
- Closed #26: CMakeFiles, cache and others generated into wrong directory
- Closed #31: CMAKE_EXPORT_COMPILE_COMMANDS Parser fails if compiler executable has a MSDOS "Short path" name. Thanks to @nolange for reporting and testing under windows
- Requires Java 7 or newer

### System Requirements
CDT v 8.7 or newer and Eclipse v 4.5 (Mars) or newer

Java 7 or newer

## 1.4.7 (2017-04-11)
### Changes
- Closed #28: CMAKE_BUILD_TOOL is deprecated in cmake version 3.0

### System Requirements
CDT v 8.1.0 or higher and Eclipse v 3.8.0 (Juno) or higher

## 1.4.6 (2017-03-20)
### Changes
- Closed #24: CMake dialog box does not save all modified configs.

### System Requirements
CDT v 8.1.0 or higher and Eclipse v 3.8.0 (Juno) or higher

## 1.4.5 (2016-11-08)
### Changes
- Closed #20: CMAKE_EXPORT_COMPILE_COMMANDS Parser treats clang++ as a C compiler.

### System Requirements
CDT v 8.1.0 or higher and Eclipse v 3.8.0 (Juno) or higher

## 1.4.4 (2016-10-05)
### Changes
- Closed #17: CMAKE_EXPORT_COMPILE_COMMANDS parser doesn't recognize g++ as a C++ compiler.

### System Requirements
CDT v 8.1.0 or higher and Eclipse v 3.8.0 (Juno) or higher

## 1.4.3 (2016-08-30)
### Changes
- Closed #16: The CMAKE_EXPORT_COMPILE_COMMANDS parser now knows about GCC cross compilers for C and C++ if the compiler executable follows the naming convention of
`<target_triplet>-gcc` or `<target_triplet>-g++`. 

### System Requirements
CDT v 8.1.0 or higher and Eclipse v 3.8.0 (Juno) or higher

## 1.4.2 (2016-05-30)
### Changes
- CMAKE_EXPORT_COMPILE_COMMANDS Parser triggers UI update to show newly detected include paths in project explorer view.
- Closed #14: Pass variables given in C++ Build->Environment to the cmake process.

### System Requirements
CDT v 8.1.0 or higher and Eclipse v 3.8.0 (Juno) or higher

## 1.4.1 (2016-03-08)
### Changes
- Closes #9: Improve support for code-completion and symbol browsing in CDT source editors.
- Closes #10: Can't install version 1.4.0 in eclipse mars.2 release 4.5.2

### System Requirements
CDT v 8.1.0 or higher and Eclipse v 3.8.0 (Juno) or higher

---
## Archived Releases (no Changelog)
These releases are made available as a zipped p2 repository. To install, use the "Install New Software..." menu and paste the appropriate repository URL from below into the "Work with" input field.

### 1.3.5 (2015-10-14)
#### System Requirements
CDT v 8.1.0 or higher and Eclipse v 3.8.0 (Juno) or higher

### 1.3.4 (2015-10-09)
#### System Requirements
CDT v 8.1.0 or higher and Eclipse v 3.8.0 (Juno) or higher

### 1.3.3 (2015-05-06)
#### System Requirements
CDT v 8.1.0 or higher and Eclipse v 3.8.0 (Juno) or higher

---
## Ancient Releases (the googlecode era)
These can be found at <a href='https://googledrive.com/host/0B-QU1Qnto3huZUZ0QUdxM01pR0U/'>https://googledrive.com/host/0B-QU1Qnto3huZUZ0QUdxM01pR0U/</a> .<br>
Do not forget to add the trailing <b>!/</b> to the repository URL!

### 1.3.2 (2015-03-27)
#### Repository URL
`jar:https://googledrive.com/host/0B-QU1Qnto3huZUZ0QUdxM01pR0U/cmake4eclipsecdt-1.3.2.zip!/`
#### System Requirements
CDT v 8.1.0 or higher and Eclipse v 3.8.0 (Juno) or higher

### 1.3.1 (2015-01-21)
#### Repository URL
`jar:https://googledrive.com/host/0B-QU1Qnto3huZUZ0QUdxM01pR0U/cmake4eclipsecdt-1.3.1.zip!/`
#### System Requirements
CDT v 8.1.0 or higher and Eclipse v 3.8.0 (Juno) or higher

### 1.3.0 (2014-12-20)
#### Repository URL
`jar:https://googledrive.com/host/0B-QU1Qnto3huZUZ0QUdxM01pR0U/cmake4eclipsecdt-1.3.0.zip!/`
#### System Requirements
CDT v 8.1.0 or higher and Eclipse v 3.8.0 (Juno) or higher

### 1.1.0 / 1.2.0.201404092022 (2014-04-09)
#### Repository URL
`jar:https://googledrive.com/host/0B-QU1Qnto3huZUZ0QUdxM01pR0U/cmake4eclipsecdt-1.1.0.zip!/`
#### System Requirements
CDT v 8.1.0 or higher and Eclipse v 3.8.0 (Juno) or higher

### 1.0.0 (2014-03-07)
#### Repository URL
`jar:https://googledrive.com/host/0B-QU1Qnto3huZUZ0QUdxM01pR0U/cmake4eclipsecdt-1.0.0.zip!/`
#### System Requirements
 CDT v 8.1.0 or higher and Eclipse v 3.8.0 (Juno) or higher

### 0.1.0-SNAPSHOT (2013-11-24) 
#### Repository URL
`jar:https://googledrive.com/host/0B-QU1Qnto3huZUZ0QUdxM01pR0U/cmake4eclipsecdt-Update-0.1.0-SNAPSHOT.zip!/`
#### System Requirements
 CDT v 8.1.0 or higher and Eclipse v 3.8.0 (Juno) or higher
 
