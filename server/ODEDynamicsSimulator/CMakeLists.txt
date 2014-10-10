if(ODE_DIR)
  set(program openhrp-ode-dynamics-simulator)

  set(sources ODE_DynamicsSimulator_impl.cpp 
              server.cpp
              ODE_ModelLoaderUtil.cpp
              ODE_World.cpp
              ODE_Link.cpp)

  add_executable(${program} ${sources})
  
  if(UNIX)
    target_link_libraries(${program}
      hrpUtil-${OPENHRP_LIBRARY_VERSION}
      hrpModel-${OPENHRP_LIBRARY_VERSION}
      hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}
      ${OMNIORB_LIBRARIES}
      ode_single)
  elseif(WIN32)
    set_target_properties(${program} PROPERTIES DEBUG_POSTFIX d)
    target_link_libraries(${program}
      optimized hrpUtil-${OPENHRP_LIBRARY_VERSION}
      optimized hrpModel-${OPENHRP_LIBRARY_VERSION}
      optimized hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}
      optimized ode_single
      debug hrpUtil-${OPENHRP_LIBRARY_VERSION}d
      debug hrpModel-${OPENHRP_LIBRARY_VERSION}d
      debug hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}d
      debug ode_singled
      ${OMNIORB_LIBRARIES})  
  endif()

  if(WIN32)
  install(TARGETS ${program} DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release)
  endif()

  install(TARGETS ${program} DESTINATION bin CONFIGURATIONS Release Debug RelWithDebInfo)

endif()
