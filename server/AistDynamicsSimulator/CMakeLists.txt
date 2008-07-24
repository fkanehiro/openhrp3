
set(program openhrp-aist-dynamics-simulator)

set(sources DynamicsSimulator_impl.cpp server.cpp)

include_directories(${PROJECT_SOURCE_DIR}/OpenHRP/Base)

add_executable(${program} ${sources})

target_link_libraries(${program} hrpBase3 hrpCorbaStubSkel3 ${OMNIORB_LIBRARIES})

if(WIN32)
install(TARGETS ${program} DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release )
endif()

install(TARGETS ${program} DESTINATION bin)

