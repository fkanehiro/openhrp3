
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
    ${program}
    hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}
    ${OMNIORB_LIBRARIES} ${OPENRTM_LIBRARIES}
    boost_filesystem boost_program_options boost_regex)
elseif(WIN32)
  add_definitions(${OPENRTM_DEFINITIONS})
  set_target_properties(${program} PROPERTIES DEBUG_POSTFIX d )
  target_link_libraries(${program}
    optimized hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}
    debug hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}d 
    ${OMNIORB_LIBRARIES} ${OPENRTM_LIBRARIES})
endif()

if(WIN32)
  install(TARGETS ${program} DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release )
endif()

install(TARGETS ${program} DESTINATION bin CONFIGURATIONS Release Debug)
