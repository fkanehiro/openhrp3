set(libname SampleCustomizer)

set(sources customizer.cpp)

add_library(${libname} SHARED ${sources})

if(UNIX)
  target_link_libraries(
    ${libname}
     hrpModel-${OPENHRP_LIBRARY_VERSION})
elseif(WIN32)
  set_target_properties(${libname} PROPERTIES DEBUG_POSTFIX d )
  target_link_libraries(${libname}
    optimized hrpModel-${OPENHRP_LIBRARY_VERSION}
    debug hrpModel-${OPENHRP_LIBRARY_VERSION}d
    )
endif()

install(TARGETS ${libname} DESTINATION ${RELATIVE_SHARE_INSTALL_PATH}/customizer CONFIGURATIONS Release Debug)