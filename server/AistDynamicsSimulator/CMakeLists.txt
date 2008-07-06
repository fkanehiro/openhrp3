
# set(program openhrp-aist-dynamics-simulator)
set(program DynamicsSimulator)

set(sources DynamicsSimulator_impl.cpp server.cpp)

include_directories(${PROJECT_SOURCE_DIR}/OpenHRP/Base)

add_executable(${program} ${sources})

target_link_libraries(${program} hrpBase hrpCorbaStubSkel ${OMNIORB_LIBRARIES})
