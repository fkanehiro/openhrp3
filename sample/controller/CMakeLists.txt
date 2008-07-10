
set(LIBRARY_OUTPUT_PATH)
set(EXECUTABLE_OUTPUT_PATH)

macro(create_simple_controller CONTROLLER_NAME)

  add_library(${CONTROLLER_NAME} SHARED ${CONTROLLER_NAME}.cpp)
  target_link_libraries(${CONTROLLER_NAME} ${OPENRTM_LIBRARIES})
  set_target_properties(${CONTROLLER_NAME} PROPERTIES PREFIX "")

  add_executable(${CONTROLLER_NAME}Comp ${CONTROLLER_NAME}Comp.cpp ${CONTROLLER_NAME}.cpp)
  target_link_libraries(${CONTROLLER_NAME}Comp ${OPENRTM_LIBRARIES})

  set(controller_install_path ${RELATIVE_SAMPLE_INSTALL_PATH}/controller/${CONTROLLER_NAME})

  install(TARGETS ${CONTROLLER_NAME} ${CONTROLLER_NAME}Comp DESTINATION ${controller_install_path})

  if(UNIX)
    install(PROGRAMS ${CONTROLLER_NAME}.sh DESTINATION ${controller_install_path})
  elseif(WIN32)
    install(PROGRAMS ${CONTROLLER_NAME}.bat DESTINATION ${controller_install_path})
  endif()

  install(FILES rtc.conf DESTINATION ${controller_install_path})

  if(EXISTS ${CMAKE_CURRENT_SOURCE_DIR}/etc)
    install(DIRECTORY etc DESTINATION ${controller_install_path} PATTERN ".svn" EXCLUDE)
  endif()		     

endmacro()


add_subdirectory(SamplePD)
add_subdirectory(SampleHG)
add_subdirectory(SampleLF)
add_subdirectory(SampleSV)
add_subdirectory(SampleController)
add_subdirectory(PA10Sample)
