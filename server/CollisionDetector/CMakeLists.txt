
set(program openhrp-collision-detector)

set(sources CollisionDetector_impl.cpp server.cpp)

add_executable(${program} ${sources})

target_link_libraries(${program} hrpCollision3 hrpCorbaStubSkel3 ${OMNIORB_LIBRARIES})

if(WIN32)
install(TARGETS ${program} DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release )
endif()

install(TARGETS ${program} DESTINATION bin)
