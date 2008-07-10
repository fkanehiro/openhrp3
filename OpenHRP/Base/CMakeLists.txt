
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
	tvmet3d.cpp
	quaternion.cpp
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
	TimeMeasure.h
	World.h
	hrpModelExportDef.h
	quaternion.h
	tvmet3d.h
	tvmetCommonTypes.h
	ublasCommonTypes.h
	${CMAKE_CURRENT_BINARY_DIR}/config.h
)				


add_library(hrpBase3 SHARED ${sources})

# interface version 0.0.0 - version 3.0.0 beta1
set_target_properties(hrpBase3 PROPERTIES VERSION 0.0.0 SOVERSION 0)

target_link_libraries(
  hrpBase3 hrpCorbaStubSkel3 ${OMNIORB_LIBRARIES} ${LAPACK_LIBRARIES}
  boost_filesystem boost_regex dl)

install(TARGETS hrpBase3
	RUNTIME DESTINATION bin
	LIBRARY DESTINATION lib
	ARCHIVE DESTINATION lib)
	
install(FILES ${headers} DESTINATION include/OpenHRP/Base)





