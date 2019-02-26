[Release Notes](#release-notes) | 
[Install...](#installation) | 
[![Mailing-list](https://img.shields.io/badge/Mailing-list-blue.svg)](http://groups.google.com/d/forum/cmake4eclipse-users)
[![Build Status](https://travis-ci.org/15knots/cmake4eclipse.svg?branch=master)](https://travis-ci.org/15knots/cmake4eclipse)
[![GitHub issues](https://img.shields.io/github/issues/15knots/cmake4eclipse.svg)](https://github.com/15knots/cmake4eclipse/issues)


## Abstract
This Eclipse plugin automatically generates build-scripts for the Eclipse CDT managed build system from CMake scripts.
Its <a id="pc">*Primary claim*</a> is: Co-workers should be able to just **check out the source and build** the project. 

## Design goals
1. **Automatic** generation of build scripts. See [Primary claim](#pc): No need to **manually** invoke cmake: Cmake options are persisted in the Eclipse project settings files.
1. Cmake is a cross-platform build tool. So Eclipse projects [should be cross-platform](#pc) as feasible; without the need for co-workers to adjust Eclipse project settings just to build on **their** platform. 
1. Take the CMakeLists.txt as the source of truth.
   - Auto-detect the actual build tool to invoke: make, ninja, nmake, MinGW make, MSYS make, ...
   - Easy project configuration regarding code completion, symbol-declaration lookup and macro-value tool-tips in the source editor.
     - Feed include paths and pre-processor symbols from cmake to the CDT-Indexer (CMAKE_EXPORT_COMPILE_COMMANDS Parser).
      - Retrieve compiler-built-in pre-processor symbols and include paths by interrogating the actual compiler and feed these to the CDT-Indexer (CMAKE_EXPORT_COMPILE_COMMANDS Compiler Built-ins). Well, at least as the compiler supports that (`nvcc` CUDA complier does so).
## Screenshots
Screenshots can be found at the <a href="https://marketplace.eclipse.org/content/cmake4eclipse#group-screenshots" title="Screenshots">Eclipse Marketplace</a>.

## Quick start
 1. If you do not have any existing code, check out the [Sample Projects](https://github.com/15knots/cmake4eclipse-sample-projects), chose one and fill in your code.
 1. If you have an existing C/C++ project code, inside Eclipse, goto `Help:Help Contents`, then read the `Cmake4eclipse User Guide:Getting Started` node to adjust your project settings.
 
---
## License
The plugin is licensed under the <a href="http://www.eclipse.org/legal/epl-v10.html">Eclipse Public License Version 1.0 ("EPL")</a>.

## System Requirements
CDT v 8.7 or newer and Eclipse v 4.5 (Mars) or newer

Java 8 or newer

## Installation
The easiest way is to drag this: <a href="http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=2318334" class="drag" title="Drag to your running Eclipse workbench to install cmake4eclipse">
<img class="img-responsive" src="https://marketplace.eclipse.org/sites/all/themes/solstice/public/images/marketplace/btn-install.png" alt="Drag to your running Eclipse workbench to install" /></a> to your running Eclipse workbench.

Alternatively, **cmake4eclipse** can be installed into Eclipse using the "Install New Software..." dialog and entering the update site URL listed below.

### Update Site
This composite update site is for use in a Eclipse workbench and contains the latest release as well as selected older releases:
https://raw.githubusercontent.com/15knots/cmake4eclipse/master/releng/comp-update/.

### p2 Repositories
Tool integrators will find each release at [bintray](https://bintray.com/15knots/p2-zip/cmake4eclipse#files).
Each release is provided as a standalone zipped p2 repository and can be consumed in a PDE target platform. To add one
of these repositories to your target platform, add a **Software Site** and enter a URL for the location as
jar:https://dl.bintray.com/15knots/p2-zip/cmake4eclipse-1.12.1.zip!/ (note the leading `jar:` and the trailing
`!/`).


## Debug and Build
This project uses Apache maven as its build system.
To build from a command-line, run `mvn -f ./parent/pom.xml package` in the root directory of the project source files.

There is a run configuration for eclipse to invoke maven `build cmake4eclipse` plus a launch configuration to debug the plugin: `cmake4eclipse`.

---
# Release Notes
## 1.1?.0 (Unreleased)
### Changes
- Feature #91: ?

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
 