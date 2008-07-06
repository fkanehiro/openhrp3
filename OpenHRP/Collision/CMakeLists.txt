
set(hrpCollision_sources
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
  CdCache.cpp
  CdScene.cpp
  CdWrapper.cpp
  )

# to avoid a bug which may be caused by the optimization
set_source_files_properties(OPC_TreeCollider.cpp PROPERTIES COMPILE_FLAGS -O)

# TODO: remove this
include_directories(${CMAKE_CURRENT_SOURCE_DIR})

add_library(hrpCollision SHARED ${hrpCollision_sources})

target_link_libraries(hrpCollision m)

INSTALL(TARGETS hrpCollision LIBRARY DESTINATION lib)
