if(${OPENRTM_VERSION} STREQUAL ${OPENRTM_VERSION100} OR ${OPENRTM_VERSION} STREQUAL ${OPENRTM_VERSION101} OR ${OPENRTM_VERSION} STREQUAL ${OPENRTM_VERSION102} )
  set(rtm_stub_jar OpenRTM-aist-1.0.0.jar)
elseif(${OPENRTM_VERSION} STREQUAL ${OPENRTM_VERSION110})
  set(rtm_stub_jar OpenRTM-aist-1.1.0.jar)
endif()
CONFIGURE_FILE( JoystickComponent-project/META-INF/MANIFEST.MF.in JoystickComponent-project/META-INF/MANIFEST.MF)
CONFIGURE_FILE( JoystickComponent-project/build.properties.in JoystickComponent-project/build.properties)

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
