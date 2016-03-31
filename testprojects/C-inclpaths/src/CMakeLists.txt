# add the binary tree to the search path for include files
# so that we will find config.h
configure_file( config.h.in ${CMAKE_CURRENT_BINARY_DIR}/config.h)
include_directories( ${CMAKE_CURRENT_BINARY_DIR})

add_executable(main main.c)
