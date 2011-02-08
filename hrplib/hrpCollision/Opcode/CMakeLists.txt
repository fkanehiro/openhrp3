set(headers
  Opcode.h
  OPC_IceHook.h
  OPC_Settings.h
  OPC_Common.h
  OPC_MeshInterface.h
  OPC_TreeBuilders.h
  OPC_AABBTree.h
  OPC_OptimizedTree.h
  OPC_BaseModel.h
  OPC_Model.h
  OPC_HybridModel.h
  OPC_Collider.h
  OPC_VolumeCollider.h
  OPC_TreeCollider.h
  OPC_RayCollider.h
  OPC_SphereCollider.h
  OPC_Picking.h
  )

install(FILES ${headers} DESTINATION ${RELATIVE_HEADERS_INSTALL_PATH}/hrpCollision/Opcode)

ADD_SUBDIRECTORY(Ice)
