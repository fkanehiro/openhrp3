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

#  CPack Bug? Long path is over flow to /.
#  install(DIRECTORY JoystickComponent-project
#    DESTINATION ${RELATIVE_SAMPLE_INSTALL_PATH}/JoystickControl
#    PATTERN ".svn" EXCLUDE )

  install(FILES JoystickComponent-project.zip
    DESTINATION ${RELATIVE_SAMPLE_INSTALL_PATH}/JoystickControl)

else()
  install(DIRECTORY Controller JoystickComponent-project
    DESTINATION ${RELATIVE_SAMPLE_INSTALL_PATH}/JoystickControl
    PATTERN ".svn" EXCLUDE 
    PATTERN "bin" EXCLUDE )

endif(GENERATE_DEBIANPACKAGE)
