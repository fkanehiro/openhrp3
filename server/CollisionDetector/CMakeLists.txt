
set(program openhrp-collision-detector)

set(sources
  ColdetBody.cpp
  CollisionDetector_impl.cpp
  server.cpp)

add_executable(${program} ${sources})

if(UNIX)
  target_link_libraries(${program}
    hrpUtil-${OPENHRP_LIBRARY_VERSION}
    hrpCollision-${OPENHRP_LIBRARY_VERSION}
    hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}
    ${OMNIORB_LIBRARIES})
elseif(WIN32)
  set_target_properties(${program} PROPERTIES DEBUG_POSTFIX d)
  target_link_libraries(${program} 
    optimized hrpUtil-${OPENHRP_LIBRARY_VERSION}
    optimized hrpCollision-${OPENHRP_LIBRARY_VERSION}
    optimized hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION} 
    debug hrpUtil-${OPENHRP_LIBRARY_VERSION}d
    debug hrpCollision-${OPENHRP_LIBRARY_VERSION}d
    debug hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}d
    ${OMNIORB_LIBRARIES})  
endif()

if(WIN32)
install(TARGETS ${program} DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release)
endif()

install(TARGETS ${program} DESTINATION bin CONFIGURATIONS Release Debug RelWithDebInfo)
