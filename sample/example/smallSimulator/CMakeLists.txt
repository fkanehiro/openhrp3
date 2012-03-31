set(program smallSimulator)

set(sources 
  smallSimulator.cpp
)

add_executable(${program} ${sources})

if(UNIX)
  target_link_libraries(
    ${program}
     hrpUtil-${OPENHRP_LIBRARY_VERSION}
     hrpModel-${OPENHRP_LIBRARY_VERSION})
elseif(WIN32)
  set_target_properties(${program} PROPERTIES DEBUG_POSTFIX d )
  target_link_libraries(${program}
    optimized hrpUtil-${OPENHRP_LIBRARY_VERSION}
    optimized hrpModel-${OPENHRP_LIBRARY_VERSION}
    optimized hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}
    debug hrpUtil-${OPENHRP_LIBRARY_VERSION}d
    debug hrpModel-${OPENHRP_LIBRARY_VERSION}d
    debug hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}d
    ${OMNIORB_LIBRARIES})
endif()

if(WIN32)
  install(TARGETS ${program} DESTINATION ${CMAKE_CURRENT_SOURCE_DIR} CONFIGURATIONS Release )
  configure_file( ${CMAKE_CURRENT_SOURCE_DIR}/smallSimulator.bat.in ${CMAKE_CURRENT_SOURCE_DIR}/smallSimulator.bat )
elseif(UNIX)
  configure_file( ${CMAKE_CURRENT_SOURCE_DIR}/smallSimulator.sh.in ${CMAKE_CURRENT_BINARY_DIR}/smallSimulator.sh )
endif()

install(TARGETS ${program} DESTINATION ${RELATIVE_SAMPLE_INSTALL_PATH}/example/smallSimulator CONFIGURATIONS Release Debug)
install(PROGRAMS ${CMAKE_CURRENT_BINARY_DIR}/${program}.${SH_SFX} DESTINATION ${RELATIVE_SAMPLE_INSTALL_PATH}/example/smallSimulator)