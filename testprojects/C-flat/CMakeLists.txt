# Policy
cmake_minimum_required( VERSION 2.8)
 
# Project name
project(C-flat)

#----------------------
# cmake settings
#----------------------
# include additional cmake modules...

#----------------------
# project build settings
#----------------------
ENABLE_TESTING()

# be more CDT Error Parser friendly..
IF(CMAKE_COMPILER_IS_GNUCC)
  SET(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fmessage-length=0 ")
ENDIF(CMAKE_COMPILER_IS_GNUCC)

# add the binary tree to the search path for include files
# so that we will find config.h
include_directories( "${PROJECT_BINARY_DIR}")

add_executable(EXECUTABLE main.c)
