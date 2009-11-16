set(program openhrp-path-planner)

set(sources
  PathPlanner.cpp
  PathPlannerComp.cpp
  PathPlannerSVC_impl.cpp
  )

add_executable(${program} ${sources})

if(UNIX)
  target_link_libraries(${program}
    ${OPENRTM_LIBRARIES}
    hrpPlanner-${OPENHRP_LIBRARY_VERSION}
    hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}
    )

elseif(WIN32)
  add_definitions(-DRTC_CORBA_CXXMAPPING11)
  set_target_properties(${program} PROPERTIES DEBUG_POSTFIX d)
  target_link_libraries(${program}
    optimized hrpPlanner-${OPENHRP_LIBRARY_VERSION}
    optimized hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION} 
    debug hrpPlanner-${OPENHRP_LIBRARY_VERSION}d
    debug hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}d
    ${OPENRTM_LIBRARIES}
    )  
endif()

if(WIN32)
  install(TARGETS ${program} DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release )
endif()

install(TARGETS ${program} DESTINATION bin CONFIGURATIONS Release Debug)
