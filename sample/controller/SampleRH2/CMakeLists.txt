
create_simple_controller(SampleRH2)

if(UNIX)
  target_link_libraries(SampleRH2 hrpUtil-${OPENHRP_LIBRARY_VERSION})
  target_link_libraries(SampleRH2Comp hrpUtil-${OPENHRP_LIBRARY_VERSION})
elseif(WIN32)
  target_link_libraries(SampleRH2 optimized hrpUtil-${OPENHRP_LIBRARY_VERSION}
                                  debug hrpUtil-${OPENHRP_LIBRARY_VERSION}d )
  target_link_libraries(SampleRH2Comp optimized hrpUtil-${OPENHRP_LIBRARY_VERSION}
                                  debug hrpUtil-${OPENHRP_LIBRARY_VERSION}d )                               
endif()
