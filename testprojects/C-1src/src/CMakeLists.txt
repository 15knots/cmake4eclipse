# Policy
cmake_minimum_required( VERSION 2.8)
 
# Project name
project(C-1src)

#----------------------
# cmake settings
#----------------------
# include additional cmake modules...

#----------------------
# project build settings
#----------------------
ENABLE_TESTING()

MESSAGE( STATUS "CMAKE_INSTALL_PREFIX = ${CMAKE_INSTALL_PREFIX}" )

# be more CDT Error Parser friendly..
IF(CMAKE_COMPILER_IS_GNUCC)
  SET(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fmessage-length=0 ")
ENDIF(CMAKE_COMPILER_IS_GNUCC)

#set(MYPROJ_VERSION_TXT "Hello!
#  This is my project, {VERSION_MAJOR}.
#  {VERSION_MINOR}.{VERSION_PATCH}-
#  {VERSION_EXTRA}.
#  Have Fun!"
#  CACHE STRING "multiline cache variable " FORCE
#  )
MESSAGE( STATUS "MYPROJ_VERSION_TXT =  ${MYPROJ_VERSION_TXT}" )

# add the binary tree to the search path for include files
# so that we will find config.h
include_directories( "${PROJECT_BINARY_DIR}/ProjectSrc1")
add_definitions(-DC1SRC=2 -DBAR123=1)
add_executable(EXECUTABLE main.c)

