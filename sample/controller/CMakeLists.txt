
set(LIBRARY_OUTPUT_PATH)
set(EXECUTABLE_OUTPUT_PATH)

macro(create_simple_controller CONTROLLER_NAME)

  add_library(${CONTROLLER_NAME} SHARED ${CONTROLLER_NAME}.cpp)
  target_link_libraries(${CONTROLLER_NAME} ${OPENRTM_LIBRARIES})
  set_target_properties(${CONTROLLER_NAME} PROPERTIES PREFIX "")

  add_executable(${CONTROLLER_NAME}Comp ${CONTROLLER_NAME}Comp.cpp ${CONTROLLER_NAME}.cpp)
  target_link_libraries(${CONTROLLER_NAME}Comp ${OPENRTM_LIBRARIES})
  
  if(WIN32)
	add_definitions(${OPENRTM_DEFINITIONS})
  endif()

  set(controller_install_path ${RELATIVE_SAMPLE_INSTALL_PATH}/controller/${CONTROLLER_NAME})

  install(TARGETS ${CONTROLLER_NAME} ${CONTROLLER_NAME}Comp DESTINATION ${controller_install_path})
  
  if(WIN32)
    install(TARGETS ${CONTROLLER_NAME} ${CONTROLLER_NAME}Comp 
        DESTINATION ${CMAKE_CURRENT_SOURCE_DIR} 
        CONFIGURATIONS Release )
  endif()  

  install(PROGRAMS ${CONTROLLER_NAME}.${SH_SFX} DESTINATION ${controller_install_path})
  
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
add_subdirectory(PA10Controller)
