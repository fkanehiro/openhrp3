macro(compile_customizer _libname _sources)
  add_library(${_libname} SHARED ${_sources})
  if(UNIX)
    target_link_libraries(
      ${_libname}
      hrpModel-${OPENHRP_LIBRARY_VERSION})
  elseif(WIN32)
    set_target_properties(${_libname} PROPERTIES DEBUG_POSTFIX d )
    target_link_libraries(${_libname}
      optimized hrpModel-${OPENHRP_LIBRARY_VERSION}
      debug hrpModel-${OPENHRP_LIBRARY_VERSION}d
      )
  endif()
endmacro()

compile_customizer(SampleCustomizer customizer.cpp)
compile_customizer(SampleBushCustomizer bush_customizer.cpp)
set(libnames SampleCustomizer SampleBushCustomizer)

install(TARGETS ${libnames} DESTINATION ${RELATIVE_SHARE_INSTALL_PATH}/customizer CONFIGURATIONS Release Debug RelWithDebInfo)
install(FILES sample1_bush_customizer_param.conf sample_4leg_robot_bush_customizer_param.conf  DESTINATION ${RELATIVE_SHARE_INSTALL_PATH}/customizer CONFIGURATIONS Release Debug RelWithDebInfo)
