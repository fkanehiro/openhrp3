
set(program openhrp-controller-bridge)

set(sources 
  BridgeConf.cpp
  VirtualRobotPortHandler.cpp
  VirtualRobotRTC.cpp
  Controller_impl.cpp
  main.cpp)

add_executable(${program} ${sources})

if(UNIX)
  target_link_libraries(
    ${program} hrpCorbaStubSkel3 ${OMNIORB_LIBRARIES} ${OPENRTM_LIBRARIES}
    boost_filesystem boost_program_options boost_regex)
elseif(WIN32)
  add_definitions(${OPENRTM_DEFINITIONS})
  target_link_libraries(
    ${program} hrpCorbaStubSkel3 ${OMNIORB_LIBRARIES} ${OPENRTM_LIBRARIES})
endif()

if(WIN32)
  install(TARGETS ${program} DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release )
endif()

install(TARGETS ${program} DESTINATION bin)

