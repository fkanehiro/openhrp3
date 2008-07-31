
# Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
# All rights reserved. This program is made available under the terms of the
# Eclipse Public License v1.0 which accompanies this distribution, and is
# available at http://www.eclipse.org/legal/epl-v10.html
# Contributors:
# National Institute of Advanced Industrial Science and Technology (AIST)
# General Robotix Inc. 

# @author Shin'ichiro Nakaoka

set(sources
  VrmlNodes.cpp
  VrmlParser.cpp
  VrmlWriter.cpp
  TriangleMeshGenerator.cpp
  NormalGenerator.cpp
  ImageConverter.cpp
  ModelNodeSet.cpp)

set(headers
  config.h
  NormalGenerator.h
  TriangleMeshGenerator.h
  VrmlParser.h
  ModelNodeSet.h
  VrmlNodes.h
  VrmlWriter.h
  ImageConverter.h
)


add_library(hrpParser3 SHARED ${sources})

# interface version 0.0.0 - version 3.0.0 beta1
set_target_properties(hrpParser3 PROPERTIES VERSION 0.0.0 SOVERSION 0)

if(UNIX)
  target_link_libraries(hrpParser3 hrpUtil3 boost_signals ${PNG_LIBRARY} ${JPEG_LIBRARY})
    
elseif(WIN32)
  set_target_properties(hrpParser3 PROPERTIES LINK_FLAGS /NODEFAULTLIB:LIBCMT)
  add_definitions(-DHRP_PARSER_MAKE_DLL)
  target_link_libraries(hrpParser3 hrpUtil3 ${PNG_LIBRARY} ${JPEG_LIBRARY} libpng zlib libjpeg )
endif() 

if(WIN32)
  install(
    TARGETS hrpParser3
    RUNTIME DESTINATION ${PROJECT_BINARY_DIR}/bin
    LIBRARY DESTINATION ${PROJECT_BINARY_DIR}/lib
    ARCHIVE DESTINATION ${PROJECT_BINARY_DIR}/lib
    CONFIGURATIONS Release )
endif()

install(
  TARGETS hrpParser3
  RUNTIME DESTINATION bin
  LIBRARY DESTINATION lib
  ARCHIVE DESTINATION lib)

install(FILES ${headers} DESTINATION include/OpenHRP/Parser)
