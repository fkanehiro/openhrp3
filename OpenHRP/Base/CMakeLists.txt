
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

add_library(hrpBase3 SHARED ${sources})

set_target_properties(hrpBase3 PROPERTIES VERSION ${LIBRARY_VERSION} SOVERSION ${LIBRARY_SO_VERSION})

target_link_libraries(
  hrpBase3 hrpCorbaStubSkel3 ${OMNIORB_LIBRARIES} ${LAPACK_LIBRARIES}
  boost_filesystem boost_regex dl)

INSTALL(TARGETS hrpBase3 LIBRARY DESTINATION lib)



