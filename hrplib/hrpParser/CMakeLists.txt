
# Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
# All rights reserved. This program is made available under the terms of the
# Eclipse Public License v1.0 which accompanies this distribution, and is
# available at http://www.eclipse.org/legal/epl-v10.html
# Contributors:
# National Institute of Advanced Industrial Science and Technology (AIST)
# General Robotix Inc. 

# @author Shin'ichiro Nakaoka

if(PNG_JPEG_BUILD)
  ADD_SUBDIRECTORY(lpng1232)
  ADD_SUBDIRECTORY(Jpeg-6b)
  ADD_SUBDIRECTORY(zlib123)
endif()
ADD_SUBDIRECTORY(triangulator)

set(sources
  VrmlNodes.cpp
  VrmlParser.cpp
  VrmlWriter.cpp
  TriangleMeshShaper.cpp
  ImageConverter.cpp
  ModelNodeSet.cpp)

set(headers
  config.h
  VrmlNodes.h
  VrmlParser.h
  VrmlWriter.h
  TriangleMeshShaper.h
  ImageConverter.h
  ModelNodeSet.h
)

set(target hrpParser-${OPENHRP_LIBRARY_VERSION})

add_library(${target} SHARED ${sources})

# interface version 0.0.0 - version 3.0.0 beta1
set_target_properties(${target} PROPERTIES VERSION 0.0.0 SOVERSION 0)

if(UNIX)
  target_link_libraries(${target}
    hrpUtil-${OPENHRP_LIBRARY_VERSION}
    boost_signals ${PNG_LIBRARY} ${JPEG_LIBRARY} ${ZLIB_LIBRARY})
    
elseif(WIN32)
  set_target_properties(${target} PROPERTIES LINK_FLAGS /NODEFAULTLIB:LIBCMT)
  add_definitions(-DHRP_PARSER_MAKE_DLL)
  target_link_libraries(${target} 
    optimized hrpUtil-${OPENHRP_LIBRARY_VERSION}
    optimized ${PNG_LIBRARY} optimized ${JPEG_LIBRARY} optimized ${ZLIB_LIBRARY}
    debug hrpUtil-${OPENHRP_LIBRARY_VERSION}d
    debug ${PNG_LIBRARY}d debug ${JPEG_LIBRARY}d debug ${ZLIB_LIBRARY}d)
  set_target_properties(${target} PROPERTIES DEBUG_POSTFIX d)
endif() 

if(WIN32)
  install(
    TARGETS ${target}
    RUNTIME DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release Debug
    LIBRARY DESTINATION ${PROJECT_BINARY_DIR}/lib CONFIGURATIONS Release Debug
    ARCHIVE DESTINATION ${PROJECT_BINARY_DIR}/lib CONFIGURATIONS Release Debug
)
endif()

install(
  TARGETS ${target}
  RUNTIME DESTINATION bin CONFIGURATIONS Release Debug
  LIBRARY DESTINATION lib CONFIGURATIONS Release Debug
  ARCHIVE DESTINATION lib CONFIGURATIONS Release Debug
)

install(FILES ${headers} DESTINATION ${RELATIVE_HEADERS_INSTALL_PATH}/hrpParser)
