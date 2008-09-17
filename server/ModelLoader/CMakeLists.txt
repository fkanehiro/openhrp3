
# Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
# All rights reserved. This program is made available under the terms of the
# Eclipse Public License v1.0 which accompanies this distribution, and is
# available at http://www.eclipse.org/legal/epl-v10.html
# Contributors:
# National Institute of Advanced Industrial Science and Technology (AIST)

# @author Shin'ichiro Nakaoka

set(program openhrp-model-loader)

set(sources
  ShapeSetInfo_impl.cpp
  BodyInfo_impl.cpp
  ModelLoader_impl.cpp
  UtilFunctions.cpp
  server.cpp )

add_executable(${program} ${sources})

if(UNIX)
  target_link_libraries(${program} hrpUtil3 hrpParser3 hrpCorbaStubSkel3 ${OMNIORB_LIBRARIES} boost_filesystem)
elseif(WIN32)
  set_target_properties(${program} PROPERTIES DEBUG_POSTFIX d )
  target_link_libraries(${program} optimized hrpUtil3 optimized hrpParser3 optimized hrpCorbaStubSkel3 
    debug hrpUtil3d debug hrpParser3d debug hrpCorbaStubSkel3d ${OMNIORB_LIBRARIES})  
endif()

if(WIN32)
  install(TARGETS ${program} DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release )
endif()

install(TARGETS ${program} DESTINATION bin CONFIGURATIONS Release)

