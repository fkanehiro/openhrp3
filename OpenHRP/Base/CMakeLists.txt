
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/config.h.in ${CMAKE_CURRENT_BINARY_DIR}/config.h)

set(hrpBase_sources
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

add_library(hrpBase SHARED ${hrpBase_sources})

target_link_libraries(
  hrpBase hrpCorbaStubSkel ${OMNIORB_LIBRARIES} ${LAPACK_LIBRARIES}
  boost_filesystem boost_regex dl)

INSTALL(TARGETS hrpBase LIBRARY DESTINATION lib)



