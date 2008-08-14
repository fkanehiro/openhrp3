
# Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
# All rights reserved. This program is made available under the terms of the
# Eclipse Public License v1.0 which accompanies this distribution, and is
# available at http://www.eclipse.org/legal/epl-v10.html
# Contributors:
# National Institute of Advanced Industrial Science and Technology (AIST)
# General Robotix Inc. 

# @author Shin'ichiro Nakaoka

set(sources
  EasyScanner.cpp
  Tvmet3d.cpp
  Tvmet4d.cpp
)

set(headers
  config.h
  EasyScanner.h
  Tvmet3d.h
  Tvmet4d.h
  TimeMeasure.h
)
  
add_library(hrpUtil3 SHARED ${sources})

# interface version 0.0.0 - version 3.0.0 beta1
set_target_properties(hrpUtil3 PROPERTIES VERSION 0.0.0 SOVERSION 0)

if(WIN32)
  add_definitions(-DHRP_UTIL_MAKE_DLL)
  set_target_properties(hrpUtil3 PROPERTIES LINK_FLAGS /NODEFAULTLIB:LIBCMT)
  set_target_properties(hrpUtil3 PROPERTIES DEBUG_POSTFIX d )
endif()

if(WIN32)
  install(TARGETS hrpUtil3
    RUNTIME DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release
    LIBRARY DESTINATION ${PROJECT_BINARY_DIR}/lib CONFIGURATIONS Release
    ARCHIVE DESTINATION ${PROJECT_BINARY_DIR}/lib CONFIGURATIONS Release
)
endif() 

install(TARGETS hrpUtil3
  RUNTIME DESTINATION bin CONFIGURATIONS Release
  LIBRARY DESTINATION lib CONFIGURATIONS Release
  ARCHIVE DESTINATION lib CONFIGURATIONS Release
)

install(FILES ${headers} DESTINATION include/hrpUtil)
