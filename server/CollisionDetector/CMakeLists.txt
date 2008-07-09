
set(program openhrp-collision-detector)

set(sources CollisionDetector_impl.cpp server.cpp)

include_directories(${PROJECT_SOURCE_DIR}/OpenHRP/Collision)

add_executable(${program} ${sources})

target_link_libraries(${program} hrpCollision3 hrpCorbaStubSkel3 ${OMNIORB_LIBRARIES})
