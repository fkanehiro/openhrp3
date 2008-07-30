
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/config.h.in ${CMAKE_CURRENT_BINARY_DIR}/config.h)

set(sources
  Sensor.cpp
  Body.cpp
  BodyCustomizerInterface.cpp
  Link.cpp
  LinkTraverse.cpp
  LinkPath.cpp
  ForwardDynamics.cpp
  ForwardDynamicsABM.cpp
  ForwardDynamicsCBM.cpp
  MatrixSolvers.cpp
  World.cpp
  ConstraintForceSolver.cpp
  ModelLoaderUtil.cpp
  )

set(headers
  Body.h
  BodyCustomizerInterface.h
  ConstraintForceSolver.h
  ForwardDynamics.h
  ForwardDynamicsABM.h
  ForwardDynamicsCBM.h
  Link.h
  LinkPath.h
  LinkTraverse.h
  MatrixSolvers.h
  ModelLoaderUtil.h
  Sensor.h
  World.h
  hrpModelExportDef.h
  tvmet3d.h
  ublasCommonTypes.h
  ${CMAKE_CURRENT_BINARY_DIR}/config.h
  )	


add_library(hrpBase3 SHARED ${sources})

# interface version 0.0.0 - version 3.0.0 beta1
set_target_properties(hrpBase3 PROPERTIES VERSION 0.0.0 SOVERSION 0)

if(UNIX)
  target_link_libraries(hrpBase3
    hrpUtil3 hrpCorbaStubSkel3
    ${OMNIORB_LIBRARIES} ${LAPACK_LIBRARIES}
    boost_filesystem boost_regex dl)

elseif(WIN32)
  add_definitions(-DHRPMODEL_MAKE_DLL)
  target_link_libraries(hrpBase3
    hrpUtil3 hrpCorbaStubSkel3
    ${OMNIORB_LIBRARIES} ${LAPACK_LIBRARIES})  
  set_target_properties(hrpBase3 PROPERTIES LINK_FLAGS /NODEFAULTLIB:LIBCMT )

endif()

if(WIN32)
  install(TARGETS hrpBase3
    RUNTIME DESTINATION ${PROJECT_BINARY_DIR}/bin
    LIBRARY DESTINATION ${PROJECT_BINARY_DIR}/lib
    ARCHIVE DESTINATION ${PROJECT_BINARY_DIR}/lib
    CONFIGURATIONS Release )
endif() 

install(TARGETS hrpBase3
  RUNTIME DESTINATION bin
  LIBRARY DESTINATION lib
  ARCHIVE DESTINATION lib)

install(FILES ${headers} DESTINATION include/OpenHRP/Base)
