[Change Log](CHANGELOG.md) | 
[Install...](#installation) | 
[![Mailing-list](https://img.shields.io/badge/Mailing-list-blue.svg)](http://groups.google.com/d/forum/cmake4eclipse-users)
[![Build Status](https://github.com/15knots/cmake4eclipse/actions/workflows/maven.yml/badge.svg)](https://github.com/15knots/cmake4eclipse/actions/workflows/maven.yml)
[![GitHub issues](https://img.shields.io/github/issues/15knots/cmake4eclipse.svg)](https://github.com/15knots/cmake4eclipse/issues)


## Abstract
This Eclipse plug-in automatically generates build-scripts for the Eclipse CDT managed build system from CMake scripts.
Its <a id="pc">*Primary claim*</a> is: Co-workers should be able to just **check out the source and build** the project. 

## Design goals
1. **Automatic** generation of build scripts. See [Primary claim](#pc): No need to **manually** invoke cmake: Cmake options are persisted in the Eclipse project settings files.
1. Cmake is a cross-platform build tool. So Eclipse projects [should be cross-platform](#pc) as feasible; without the need for co-workers to adjust Eclipse project settings just to build on **their** platform. 
1. Take the CMakeLists.txt as the source of truth.
   - Auto-detect the actual build tool to invoke: make, ninja, nmake, MinGW make, MSYS make, ...
   - Easy project configuration regarding code completion, symbol-declaration lookup and macro-value tool-tips in the source editor.
     - Feed include paths and pre-processor symbols from cmake to the CDT-Indexer (CMake Compilation DB).
     - Retrieve compiler-built-in pre-processor symbols and include paths by interrogating the actual compiler and feed these to the CDT-Indexer (CMake Compilation DB). Well, at least as the compiler supports that (GNU C and `nvcc` CUDA compilers do so).

## Quick start
 1. If you do not have any existing code, check out the [Sample Projects](https://github.com/15knots/cmake4eclipse-sample-projects), choose one and fill in your code.
 1. If you have an existing C/C++ project code, inside Eclipse, goto `Help:Help Contents`, then read the `Cmake4eclipse User Guide:Tasks` nodes.
 
---
## License
The plugin is licensed under the <a href="https://www.eclipse.org/legal/epl-2.0/">Eclipse Public License Version 2.0 ("EPL-2.0")</a>.

## System Requirements
- CDT v 10.5 or newer and Eclipse v 2021-12 or newer.
- Java 11 or newer.

## Installation
The easiest way is to drag this: <a href="http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=2318334" class="drag" title="Drag to your running Eclipse workbench to install cmake4eclipse">
<img class="img-responsive" src="https://marketplace.eclipse.org/sites/all/themes/solstice/public/images/marketplace/btn-install.png" alt="Drag to your running Eclipse workbench to install" /></a> to your running Eclipse workbench.

Alternatively, **cmake4eclipse** can be installed into Eclipse using the "Install New Software..." dialog and entering the update site URL listed below.

### Update Site
This composite update site is for use in a Eclipse workbench and contains the latest release as well as selected older releases:
https://raw.githubusercontent.com/15knots/cmake4eclipse/master/releng/comp-update/.

### p2 Repositories
Tool integrators will find each release at [cloudsmith](https://cloudsmith.io/~15knots/repos/p2-zip/packages/).
Each release is provided as a stand-alone zipped p2 repository and can be consumed in a PDE target platform. To add one
of these repositories to your target platform, add a **Software Site** and enter a URL for the location as
jar:https://dl.cloudsmith.io/public/15knots/p2-zip/raw/files/cmake4eclipse-3.0.0.zip!/ 
(note the leading `jar:` and the trailing `!/`).

If you work at a company that wants to ship cmake4eclipse with your product, please consider to mirror the zip file internally
and reference the company-internal location in your target definition. This will help to reduce the traffic on cloudsmith and
make your product builds immune to downtimes on cloudsmith.

## Debug and Build
This project uses Apache maven as its build system.
To build from a command-line, run `mvn -f ./parent/pom.xml package` in the root directory of the project source files.

There is also a run configuration for eclipse to invoke the maven build: `build cmake4eclipse`.

To debug the plug-in from Eclipse, first set the Plug-in Development Target platform of your workbench to `cdt/8.7-eclipse/4.5.2`, then run the Eclipse Application launch configuration named `cmake4eclipse`.

## References
### Projects that recommend to use this plugin to develop it using Eclipse.
- CbmRoot https://redmine.cbm.gsi.de/projects/cbmroot/wiki/DevCbmEclipse#Install-CMake-related-Eclipse-plugins
- GnuCash https://wiki.gnucash.org/wiki/Eclipse#Suggested_Plugins
- HTGS https://pages.nist.gov/HTGS/doxygen/tutorial0.html
- Kendryte https://forum.kendryte.com/topic/35/build-kendryte-using-eclipse
- Merlin http://www.accelerators.manchester.ac.uk/merlin/Quickstart/Eclipse.html
- Minres/SystemC-Quickstart https://github.com/Minres/SystemC-Quickstart
- Mixxx https://www.mixxx.org/wiki/doku.php/eclipse#step-by-step_setup
- Navit https://wiki.navit-project.org/index.php/Eclipse#Project_Setup

### IDEs with cmake4eclipse
Nsight Eclipse https://devblogs.nvidia.com/drivepx-application-development-using-nsight-eclipse-edition#attachment_10114

### Books
Advanced C++ https://books.google.de/books?id=YVG7DwAAQBAJ&pg=PA9&lpg=PA9&dq=cmake4eclipse&source=bl&ots=qgHjAfjlcA&sig=ACfU3U21IppdYpImjXxBfWD69-2NYeHB4w&hl=de&sa=X&ved=2ahUKEwimtrONqJjnAhVKK1AKHVIEC2k4ChDoATAIegQIChAB#v=onepage&q=cmake4eclipse&f=false
