set(GrxUI_ON_ECLIPSE_DIR ${OpenHRP_SOURCE_DIR}/GrxUIonEclipse-project-0.9.8)

macro(copy_files_switch_rtm_ver MACRO_OPENRTM_VERSION)
  set(src_path ${GrxUI_ON_ECLIPSE_DIR}/${MACRO_OPENRTM_VERSION}/)
  set(dest_path ${GrxUI_ON_ECLIPSE_DIR}/)
  if(WIN32)
    string(REGEX REPLACE "/" "\\\\\\\\" src_path ${src_path})
    string(REGEX REPLACE "/" "\\\\\\\\" dest_path ${dest_path})
  endif()
  set(src_files
    build.properties
    grxuirc.xml
    grxuirc_win.xml
    plugin.xml
    .classpath
  )
  foreach(src_file_name ${src_files})
    if(WIN32)
      execute_process(
        COMMAND cmd /C "${COPY_COMMAND} ${src_path}${src_file_name} ${dest_path}"
        OUTPUT_VARIABLE output_var
        RESULT_VARIABLE result_var
        OUTPUT_STRIP_TRAILING_WHITESPACE )
    elseif(UNIX)
      execute_process(
        COMMAND ${COPY_COMMAND} ${src_path}${src_file_name} ${dest_path}
        OUTPUT_VARIABLE output_var
        RESULT_VARIABLE result_var
        OUTPUT_STRIP_TRAILING_WHITESPACE )
    endif()
  endforeach()

  set(src_path ${GrxUI_ON_ECLIPSE_DIR}/${MACRO_OPENRTM_VERSION}/META-INF/)
  set(dest_path ${GrxUI_ON_ECLIPSE_DIR}/META-INF/)
  if(WIN32)
    string(REGEX REPLACE "/" "\\\\\\\\" src_path ${src_path})
    string(REGEX REPLACE "/" "\\\\\\\\" dest_path ${dest_path})
  endif()
  if(WIN32)
    execute_process(
      COMMAND cmd /C "${COPY_COMMAND} ${src_path}MANIFEST.MF ${dest_path}"
      OUTPUT_VARIABLE output_var
      RESULT_VARIABLE result_var
      OUTPUT_STRIP_TRAILING_WHITESPACE )
  elseif(UNIX)
    execute_process(
      COMMAND ${COPY_COMMAND} ${src_path}MANIFEST.MF ${dest_path}
      OUTPUT_VARIABLE output_var
      RESULT_VARIABLE result_var
      OUTPUT_STRIP_TRAILING_WHITESPACE )
  endif()

  set(src_path ${GrxUI_ON_ECLIPSE_DIR}/${MACRO_OPENRTM_VERSION}/src/com/generalrobotix/ui/view/)
  set(dest_path ${GrxUI_ON_ECLIPSE_DIR}/src/com/generalrobotix/ui/view/)
  if(WIN32)
    string(REGEX REPLACE "/" "\\\\\\\\" src_path ${src_path})
    string(REGEX REPLACE "/" "\\\\\\\\" dest_path ${dest_path})
  endif()
  set(src_files GrxOpenHRPView.java )
  foreach(src_file_name ${src_files})
    if(WIN32)
      execute_process(
        COMMAND cmd /C "${COPY_COMMAND} ${src_path}${src_file_name} ${dest_path}"
        OUTPUT_VARIABLE output_var
        RESULT_VARIABLE result_var
        OUTPUT_STRIP_TRAILING_WHITESPACE )
    elseif(UNIX)
      execute_process(
        COMMAND ${COPY_COMMAND} ${src_path}${src_file_name} ${dest_path}
        OUTPUT_VARIABLE output_var
        RESULT_VARIABLE result_var
        OUTPUT_STRIP_TRAILING_WHITESPACE )
    endif()
  endforeach()
endmacro()

if( ${OPENRTM_VERSION} STREQUAL ${OPENRTM_VERSION042} )
  set(rtm_stub_jar OpenRTM-aist-0.4.1.jar)
  copy_files_switch_rtm_ver(${OPENRTM_VERSION042})
elseif(${OPENRTM_VERSION} STREQUAL ${OPENRTM_VERSION100})
  set(rtm_stub_jar openrtmstubskel.jar)
  add_subdirectory(rtmStubCorba)
  message(STATUS "OpenRTM version is ${OPENRTM_VERSION}. Generate Makefile compiles openrtmstubskel.jar.")
  copy_files_switch_rtm_ver(${OPENRTM_VERSION100})
else()
  set(rtm_stub_jar openrtmstubskel.jar)
  add_subdirectory(rtmStubCorba)
  message(STATUS "OpenRTM version is ${OPENRTM_VERSION}. Generate Makefile compiles openrtmstubskel.jar.")
  copy_files_switch_rtm_ver(${OPENRTM_VERSION100})
endif()


ADD_SUBDIRECTORY(corba)

# install(FILES vrml97.jar DESTINATION ${RELATIVE_JAVA_INSTALL_PATH})
