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

target_link_libraries(${program} ${OMNIORB_LIBRARIES} sDIMS hrpCorbaStubSkel3)

