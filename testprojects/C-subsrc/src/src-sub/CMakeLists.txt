
#----------------------
# cmake settings
#----------------------

#----------------------
# project build settings
#----------------------

include_directories( "/AN/INC LUDE/DIR")
include_directories(SYSTEM /A/SYTEM/INCLUDE/DIR)
include_directories( ../include)

add_definitions(-DFOO=1 -DBAR)
add_executable(execut2 main1.c)
target_compile_definitions(execut2 PUBLIC "OHU=LOLL Oh! wo" MAGIC=12348765)
