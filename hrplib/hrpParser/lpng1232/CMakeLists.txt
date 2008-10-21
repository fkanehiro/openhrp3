
# Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
# All rights reserved. This program is made available under the terms of the
# Eclipse Public License v1.0 which accompanies this distribution, and is
# available at http://www.eclipse.org/legal/epl-v10.html
# Contributors:
# National Institute of Advanced Industrial Science and Technology (AIST)
# General Robotix Inc. 

# @author Shin'ichiro Nakaoka

set(sources
  png.c
  pngerror.c
  pngget.c
  pngmem.c
  pngpread.c
  pngread.c
  pngrio.c
  pngrtran.c
  pngrutil.c
  pngset.c
  pngtrans.c
  pngwio.c
  pngwrite.c
  pngwtran.c
  pngwutil.c
  )

set(headers
  png.h
  pngconf.h
)

add_library(png STATIC ${sources})

include_directories("../zlib123")
add_definitions(-DPNG_NO_MMX_CODE -D_CRT_SECURE_NO_WARNINGS)

if(WIN32)
set_target_properties(png PROPERTIES COMPILE_FLAGS -Ob1)
set_target_properties(png PROPERTIES DEBUG_POSTFIX d )
set_target_properties(png PROPERTIES OUTPUT_NAME libpng )

endif() 

if(WIN32)
  install(
    TARGETS png
    RUNTIME DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release
    LIBRARY DESTINATION ${PROJECT_BINARY_DIR}/lib CONFIGURATIONS Release
    ARCHIVE DESTINATION ${PROJECT_BINARY_DIR}/lib CONFIGURATIONS Release
)
endif()

install(
  TARGETS png
  RUNTIME DESTINATION bin CONFIGURATIONS Release
  LIBRARY DESTINATION lib CONFIGURATIONS Release
  ARCHIVE DESTINATION lib CONFIGURATIONS Release
)

install(FILES ${headers} DESTINATION include/hrpParser)
