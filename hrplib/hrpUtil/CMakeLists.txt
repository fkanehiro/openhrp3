
# Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
# All rights reserved. This program is made available under the terms of the
# Eclipse Public License v1.0 which accompanies this distribution, and is
# available at http://www.eclipse.org/legal/epl-v10.html
# Contributors:
# National Institute of Advanced Industrial Science and Technology (AIST)

# @author Shin'ichiro Nakaoka


if(PNG_JPEG_BUILD)
  ADD_SUBDIRECTORY(lpng1232)
  ADD_SUBDIRECTORY(Jpeg-6b)
  ADD_SUBDIRECTORY(zlib123)
endif()

ADD_SUBDIRECTORY(triangulator)

set(sources
  EasyScanner.cpp
  Tvmet3d.cpp
  Tvmet4d.cpp
  UrlUtil.cpp
  VrmlNodes.cpp
  VrmlParser.cpp
  VrmlWriter.cpp
  TriangleMeshShaper.cpp
  Triangulator.cpp
  ImageConverter.cpp
)

set(headers
  config.h
  Referenced.h
  EasyScanner.h
  Tvmet3d.h
  Tvmet4d.h
  TimeMeasure.h
  UrlUtil.h
  VrmlNodes.h
  VrmlParser.h
  VrmlWriter.h
  TriangleMeshShaper.h
  ImageConverter.h
)

set(target hrpUtil-${OPENHRP_LIBRARY_VERSION})
  
add_library(${target} SHARED ${sources})

# interface version 0.0.0 - version 3.0.0 beta1
set_target_properties(${target} PROPERTIES VERSION 0.0.0 SOVERSION 0)

if(UNIX)
  target_link_libraries(${target}
    boost_filesystem boost_signals ${PNG_LIBRARY} ${JPEG_LIBRARY} ${ZLIB_LIBRARY})

elseif(WIN32)
  add_definitions(-DHRP_UTIL_MAKE_DLL)
  set_target_properties(${target} PROPERTIES LINK_FLAGS /NODEFAULTLIB:LIBCMT)
  set_target_properties(${target} PROPERTIES DEBUG_POSTFIX d )

    target_link_libraries(${target}
    optimized ${ZLIB_LIBRARY} debug ${ZLIB_LIBRARY}d
    optimized ${PNG_LIBRARY} debug ${PNG_LIBRARY}d
    optimized ${JPEG_LIBRARY} debug ${JPEG_LIBRARY}d)
endif()

if(WIN32)
  install(TARGETS ${target}
    RUNTIME DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release Debug
    LIBRARY DESTINATION ${PROJECT_BINARY_DIR}/lib CONFIGURATIONS Release Debug
    ARCHIVE DESTINATION ${PROJECT_BINARY_DIR}/lib CONFIGURATIONS Release Debug
)
endif() 

install(TARGETS ${target}
  RUNTIME DESTINATION bin CONFIGURATIONS Release Debug
  LIBRARY DESTINATION lib CONFIGURATIONS Release Debug
  ARCHIVE DESTINATION lib CONFIGURATIONS Release Debug
)

install(FILES ${headers} DESTINATION ${RELATIVE_HEADERS_INSTALL_PATH}/hrpUtil)
