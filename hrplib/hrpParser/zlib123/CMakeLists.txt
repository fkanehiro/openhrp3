
# Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
# All rights reserved. This program is made available under the terms of the
# Eclipse Public License v1.0 which accompanies this distribution, and is
# available at http://www.eclipse.org/legal/epl-v10.html
# Contributors:
# National Institute of Advanced Industrial Science and Technology (AIST)
# General Robotix Inc. 

# @author Shin'ichiro Nakaoka

set(sources
  adler32.c
  compress.c
  crc32.c
  gzio.c
  uncompr.c
  deflate.c
  trees.c
  zutil.c
  inflate.c
  infback.c
  inftrees.c
  inffast.c
  )

set(headers
  crc32.h
  deflate.h
  inffast.h
  inffixed.h
  inflate.h
  inftrees.h
  trees.h
  zconf.h
  zlib.h
  zutil.h
)

add_library(${ZLIB_LIBRARY} STATIC ${sources})

if(WIN32)
set_target_properties(${ZLIB_LIBRARY} PROPERTIES COMPILE_FLAGS -Ob1)
set_target_properties(${ZLIB_LIBRARY} PROPERTIES DEBUG_POSTFIX d )
endif() 

if(WIN32)
  install(
    TARGETS ${ZLIB_LIBRARY}
    RUNTIME DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release
    LIBRARY DESTINATION ${PROJECT_BINARY_DIR}/lib CONFIGURATIONS Release
    ARCHIVE DESTINATION ${PROJECT_BINARY_DIR}/lib CONFIGURATIONS Release
)
endif()

install(
  TARGETS ${ZLIB_LIBRARY}
  RUNTIME DESTINATION bin CONFIGURATIONS Release
  LIBRARY DESTINATION lib CONFIGURATIONS Release
  ARCHIVE DESTINATION lib CONFIGURATIONS Release
)

install(FILES ${headers} DESTINATION include/hrpParser)
