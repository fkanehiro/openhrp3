
set(program openhrp-controller-bridge)

set(sources 
  BridgeConf.cpp
  VirtualRobotPortHandler.cpp
  VirtualRobotRTC.cpp
  Controller_impl.cpp
  main.cpp)

add_executable(${program} ${sources})

target_link_libraries(
  ${program} hrpCorbaStubSkel3 ${OMNIORB_LIBRARIES} ${OPENRTM_LIBRARIES}
  boost_filesystem boost_program_options boost_regex)
