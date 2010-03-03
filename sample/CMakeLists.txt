add_subdirectory(controller)
if(OPENRTM_VERSION STREQUAL OPENRTM_VERSION100)
  add_subdirectory(example)
  add_subdirectory(JoystickControl)
endif()

install(DIRECTORY model project script
  DESTINATION ${RELATIVE_SAMPLE_INSTALL_PATH}
  PATTERN ".svn" EXCLUDE PATTERN "mockup" EXCLUDE)
