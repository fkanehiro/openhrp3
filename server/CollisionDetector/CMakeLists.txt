
# set(program openhrp-collision-detector)
set(program CollisionDetector)

set(sources CollisionDetector_impl.cpp server.cpp)

include_directories(${PROJECT_SOURCE_DIR}/OpenHRP/Collision)

add_executable(${program} ${sources})

target_link_libraries(${program} hrpCollision hrpCorbaStubSkel ${OMNIORB_LIBRARIES})
