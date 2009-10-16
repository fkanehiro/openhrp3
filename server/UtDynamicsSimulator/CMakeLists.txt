add_subdirectory(sDIMS)

set(program openhrp-ut-dynamics-simulator)

set(sources DynamicsSimulator_impl.cpp 
	    server.cpp 
	    World.cpp 
	    Sensor.cpp 
	    ModelLoaderUtil.cpp)

add_executable(${program} ${sources})

add_definitions(-DSEGA -DUSE_CLAPACK_INTERFACE)
include_directories(sDIMS)
link_directories(sDIMS)

target_link_libraries(${program} ${OMNIORB_LIBRARIES} sDIMS hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION})

if(WIN32)
install(TARGETS ${program} DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release)
endif()

install(TARGETS ${program} DESTINATION ${CPACK_INSTALL_PREFIX}bin CONFIGURATIONS Release Debug)
