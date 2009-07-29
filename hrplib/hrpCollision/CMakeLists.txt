
# Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
#  All rights reserved. This program is made available under the terms of the
#  Eclipse Public License v1.0 which accompanies this distribution, and is
#  available at http://www.eclipse.org/legal/epl-v10.html
#  Contributors:
#  National Institute of Advanced Industrial Science and Technology (AIST)
# 
# @author Shin'ichiro Nakaoka


set(sources
  ColdetModel.cpp
  ColdetModelPair.cpp
  CollisionPairInserter.cpp
  TriOverlap.cpp
  SSVTreeCollider.cpp
  DistFuncs.cpp
  Opcode/Ice/IceAABB.cpp
  Opcode/Ice/IceContainer.cpp
  Opcode/Ice/IceIndexedTriangle.cpp
  Opcode/Ice/IceMatrix3x3.cpp
  Opcode/Ice/IceMatrix4x4.cpp
  Opcode/Ice/IceRevisitedRadix.cpp
  Opcode/Ice/IceHPoint.cpp
  Opcode/Ice/IceRandom.cpp
  Opcode/Ice/IcePoint.cpp
  Opcode/OPC_AABBTree.cpp
  Opcode/OPC_BaseModel.cpp
  Opcode/OPC_Collider.cpp
  Opcode/OPC_Common.cpp
  Opcode/OPC_MeshInterface.cpp
  Opcode/OPC_Model.cpp
  Opcode/OPC_OptimizedTree.cpp
  Opcode/OPC_TreeBuilders.cpp
  Opcode/OPC_TreeCollider.cpp
  Opcode/OPC_VolumeCollider.cpp 
  Opcode/OPC_RayCollider.cpp
  Opcode/OPC_Picking.cpp
  )

set(headers
  config.h
  ColdetModel.h
  ColdetModelPair.h
  CollisionData.h
  )

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/Opcode)

set(target hrpCollision-${OPENHRP_LIBRARY_VERSION})

add_library(${target} SHARED ${sources})

# interface version 0.0.0 - version 3.0.0 beta1
set_target_properties(${target} PROPERTIES VERSION 0.0.0 SOVERSION 0)

if(UNIX)
  if(NOT CMAKE_BUILD_TYPE STREQUAL Debug)
    # to avoid a bug which may be caused by the optimization
    #set_source_files_properties(OPC_TreeCollider.cpp PROPERTIES COMPILE_FLAGS -O)
  endif()

  target_link_libraries(${target} m)
elseif(WIN32)
  add_definitions(-DHRP_COLLISION_MAKE_DLL)
  set_target_properties(${target} PROPERTIES DEBUG_POSTFIX d)
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

install(FILES ${headers} DESTINATION ${RELATIVE_HEADERS_INSTALL_PATH}/hrpCollision)

