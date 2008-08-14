
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
  CdCache.cpp
  CdScene.cpp
  CdWrapper.cpp
  Ice/IceAABB.cpp
  Ice/IceContainer.cpp
  Ice/IceIndexedTriangle.cpp
  Ice/IceMatrix3x3.cpp
  Ice/IceMatrix4x4.cpp
  Ice/IceRevisitedRadix.cpp
  Ice/IceHPoint.cpp
  Ice/IceRandom.cpp
  Ice/IcePoint.cpp
  OPC_AABBTree.cpp
  OPC_BaseModel.cpp
  OPC_Collider.cpp
  OPC_Common.cpp
  OPC_MeshInterface.cpp
  OPC_Model.cpp
  OPC_OptimizedTree.cpp
  OPC_TreeBuilders.cpp
  OPC_TreeCollider.cpp
  OPC_VolumeCollider.cpp 
  InsertCollisionPair.cpp
  TriOverlap.cpp
  )

set(headers
  config.h
  ColdetModel.h
  CdCache.h
  CdWrapper.h
  CdScene.h
  utilities.h
  OPC_PlanesTriOverlap.h
  OPC_PlanesAABBOverlap.h
  OPC_BaseModel.h
  OPC_OptimizedTree.h
  OPC_Collider.h
  OPC_TreeBuilders.h
  OPC_HybridModel.h
  OPC_Common.h
  OPC_SphereAABBOverlap.h
  OPC_AABBTree.h
  OPC_TreeCollider.h
  OPC_SphereTriOverlap.h
  OPC_TriBoxOverlap.h
  OPC_BoxBoxOverlap.h
  OPC_VolumeCollider.h
  OPC_TriTriOverlap.h
  OPC_Model.h
  OPC_Picking.h
  OPC_SweepAndPrune.h
  OPC_MeshInterface.h
  OPC_IceHook.h
  OPC_BoxPruning.h
  OPC_Settings.h
  Opcode.h
  Stdafx.h
  )

# TODO: remove this
include_directories(${CMAKE_CURRENT_SOURCE_DIR})

add_library(hrpCollision3 SHARED ${sources})

# interface version 0.0.0 - version 3.0.0 beta1
set_target_properties(hrpCollision3 PROPERTIES VERSION 0.0.0 SOVERSION 0)

if(UNIX)
  if(NOT CMAKE_BUILD_TYPE STREQUAL Debug)
    # to avoid a bug which may be caused by the optimization
    #set_source_files_properties(OPC_TreeCollider.cpp PROPERTIES COMPILE_FLAGS -O)
  endif()

  target_link_libraries(hrpCollision3 m)
elseif(WIN32)
  add_definitions(-DHRP_COLLISION_MAKE_DLL)
  set_target_properties(hrpCollision3 PROPERTIES DEBUG_POSTFIX d )
endif()

if(WIN32)
  install(TARGETS hrpCollision3
    RUNTIME DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release
    LIBRARY DESTINATION ${PROJECT_BINARY_DIR}/lib CONFIGURATIONS Release
    ARCHIVE DESTINATION ${PROJECT_BINARY_DIR}/lib CONFIGURATIONS Release
    )
endif()

install(TARGETS hrpCollision3
  RUNTIME DESTINATION bin CONFIGURATIONS Release
  LIBRARY DESTINATION lib CONFIGURATIONS Release
  ARCHIVE DESTINATION lib CONFIGURATIONS Release
  )

install(FILES ${headers} DESTINATION include/hrpCollision)

