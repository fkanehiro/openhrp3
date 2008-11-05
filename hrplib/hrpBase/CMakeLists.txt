
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
  uBlasCommonTypes.h
  hrpModelExportDef.h
  tvmet3d.h
  ${CMAKE_CURRENT_BINARY_DIR}/config.h
  )	


add_library(hrpBase3 SHARED ${sources})

# interface version 0.0.0 - version 3.0.0 beta1
set_target_properties(hrpBase3 PROPERTIES VERSION 0.0.0 SOVERSION 0)

if(UNIX)
  target_link_libraries(hrpBase3
    hrpUtil3 hrpCorbaStubSkel3 hrpCollision3
    ${OMNIORB_LIBRARIES} ${LAPACK_LIBRARIES}
    boost_filesystem boost_regex dl)

elseif(WIN32)
  add_definitions(-DHRPBASE_MAKE_DLL)
  target_link_libraries(hrpBase3
    optimized hrpUtil3 optimized hrpCorbaStubSkel3 optimized hrpCollision3
    debug hrpUtil3d debug hrpCorbaStubSkel3d debug hrpCollision3d
    ${OMNIORB_LIBRARIES} ${LAPACK_LIBRARIES})  
  set_target_properties(hrpBase3 PROPERTIES LINK_FLAGS /NODEFAULTLIB:LIBCMT )
  set_target_properties(hrpBase3 PROPERTIES DEBUG_POSTFIX d )
endif()

if(WIN32)
  install(TARGETS hrpBase3
    RUNTIME DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release Debug
    LIBRARY DESTINATION ${PROJECT_BINARY_DIR}/lib CONFIGURATIONS Release Debug
    ARCHIVE DESTINATION ${PROJECT_BINARY_DIR}/lib CONFIGURATIONS Release Debug
    )
endif() 

install(TARGETS hrpBase3
  RUNTIME DESTINATION bin CONFIGURATIONS Release
  LIBRARY DESTINATION lib CONFIGURATIONS Release
  ARCHIVE DESTINATION lib CONFIGURATIONS Release )

install(FILES ${headers} DESTINATION include/hrpBase)
