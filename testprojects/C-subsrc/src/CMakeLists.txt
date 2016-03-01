# Policy
cmake_minimum_required( VERSION 2.8) 

# Project name
project(C-subsrc)

#----------------------
# cmake settings
#----------------------

#----------------------
# project build settings
#----------------------
#ENABLE_TESTING()

# be more CDT Error Parser friendly..
IF(CMAKE_COMPILER_IS_GNUCC)
  SET(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fmessage-length=0 ")
ENDIF(CMAKE_COMPILER_IS_GNUCC)


include_directories( "${PROJECT_BINARY_DIR}")

add_executable(execut1 util1.c main.c)

add_subdirectory(src-sub)

