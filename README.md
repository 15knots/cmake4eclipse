# Introduction
This Eclipse plugin generates buildscripts for the Eclipse CDT manged build system from CMake scripts.

# Why cmake4eclipse?
Blindly invoked, CMake will generate makefiles (or other build scripts) inside the source tree, cluttering it with lots of files and directories that have to be fleed out from version control: This practice might be ok for simple hello-world-projects, but for larger projects, the CMake developers recommend _You_ to set up a separate directory for building the source.

Annoyingly, these recommended out-of-source-builds impose some tedious tasks on Your co-workers who check out the code and just want to build it:
  1. leave eclipse workbench,
  1. manually fire up a command-line shell,
  1. manually create a directory for the out-of-source-build,
  1. manually change the CWD to that directory,
  1. manually invoke cmake, telling it to generate build scripts, which kind of build scripts you want and where source source files live,
  1. re-enter eclipse workbench, configure the checked out project to use the generated buildscripts.

**Cmake4eclipse** aims to address these tasks: Co-workers can just check out the source and have all the tedious tasks automated.

# Installation Instructions
**cmake4eclipse** can be installed into an existing Eclipse installation using the "Install New Software..." dialog and entering the update site URL listed below.
## Update Site
This composite update site contains the latest release as well as selected older releases.
https://cmake4eclipsecdt.googlecode.com/git/releng/comp-update/

---


## Latest Release
1.3.1 (2015-01-21)
### Repository URL
`jar:https://googledrive.com/host/0B-QU1Qnto3huZUZ0QUdxM01pR0U/cmake4eclipsecdt-1.3.1.zip!/`
### System Requirements
CDT v 8.1.0 or higher and Eclipse v 3.8.0 (Juno) or higher

---

## Archived Releases
These releases are made available as a zipped p2 repository. To install, use the "Install New Software..." menu and paste the appropriate repository URL from below into the "Work with" input field.

### 1.3.0 (2014-12-20)
#### Repository URL
`jar:https://googledrive.com/host/0B-QU1Qnto3huZUZ0QUdxM01pR0U/cmake4eclipsecdt-1.3.0.zip!/`
#### System Requirements
CDT v 8.1.0 or higher and Eclipse v 3.8.0 (Juno) or higher

### 1.1.0 / 1.2.0.201404092022 (2014-04-09)
#### Repository URL
`jar:https://googledrive.com/host/0B-QU1Qnto3huZUZ0QUdxM01pR0U/cmake4eclipsecdt-1.1.0.zip!/`
#### System Requirements
CDT v 8.1.0 or higher
Eclipse v 3.8.0 (Juno) or higher

### 1.0.0 (2014-03-07)
#### Repository URL
`jar:https://googledrive.com/host/0B-QU1Qnto3huZUZ0QUdxM01pR0U/cmake4eclipsecdt-1.0.0.zip!/`
#### System Requirements
CDT v 8.1.0 or higher<br>
Eclipse v 3.8.0 (Juno) or higher<br>
<br>
<hr />
<h2>Older Releases</h2>
Other revisions can be found at <a href='https://googledrive.com/host/0B-QU1Qnto3huZUZ0QUdxM01pR0U/'>https://googledrive.com/host/0B-QU1Qnto3huZUZ0QUdxM01pR0U/</a> .<br>
Do not forget to add the trailing <b>!/</b> to the repository URL!<br>
