
set(program openhrp-aist-dynamics-simulator)

set(sources DynamicsSimulator_impl.cpp server.cpp)

add_executable(${program} ${sources})

if(UNIX)
  target_link_libraries(${program} hrpUtil3 hrpBase3 hrpCorbaStubSkel3 ${OMNIORB_LIBRARIES})
elseif(WIN32)
  set_target_properties(${program} PROPERTIES DEBUG_POSTFIX d )
  target_link_libraries(${program} optimized hrpUtil3 optimized hrpBase3 optimized hrpCorbaStubSkel3 
    debug hrpUtil3d debug hrpBase3d debug hrpCorbaStubSkel3d ${OMNIORB_LIBRARIES})  
endif()

if(WIN32)
install(TARGETS ${program} DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release)
endif()

install(TARGETS ${program} DESTINATION bin CONFIGURATIONS Release Debug)

