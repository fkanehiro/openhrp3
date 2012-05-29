set(headers
  IcePreprocessor.h
  IceTypes.h
  IceFPU.h
  IceMemoryMacros.h
  IceUtils.h
  IceContainer.h
  IcePairs.h
  IceRevisitedRadix.h
  IceRandom.h
  IceAxes.h
  IcePoint.h
  IceHPoint.h
  IceMatrix3x3.h
  IceMatrix4x4.h
  IcePlane.h
  IceRay.h
  IceIndexedTriangle.h
  IceAABB.h
  IceBoundingSphere.h
  IceOBB.h
  )

install(FILES ${headers} DESTINATION ${RELATIVE_HEADERS_INSTALL_PATH}/hrpCollision/Opcode/Ice)
