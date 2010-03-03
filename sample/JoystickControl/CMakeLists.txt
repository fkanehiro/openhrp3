install(FILES JoystickContorller.xml JoystickConnection.rtclink SimulationProject.xml
  DESTINATION ${RELATIVE_SAMPLE_INSTALL_PATH}/JoystickControl)

if(GENERATE_DEBIANPACKAGE)
  install(DIRECTORY Controller
    DESTINATION ${RELATIVE_SAMPLE_INSTALL_PATH}/JoystickControl
    PATTERN ".svn" EXCLUDE
    PATTERN "*.bat" EXCLUDE
    PATTERN "*.sln" EXCLUDE
    PATTERN "*.vsprops" EXCLUDE
    PATTERN "*.vcproj" EXCLUDE)
else()
  install(DIRECTORY Controller
    DESTINATION ${RELATIVE_SAMPLE_INSTALL_PATH}/JoystickControl
    PATTERN ".svn" EXCLUDE)
endif(GENERATE_DEBIANPACKAGE)
