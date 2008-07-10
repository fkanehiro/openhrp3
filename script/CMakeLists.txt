
# @author Shin'ichiro Nakaoka


if(UNIX)
  set(subbin unix)
elseif(WIN32)
  set(subbin dos)
endif()


macro(create_bootscripts DESTINATION)

  foreach(script CollisionDetector.sh DynamicsSimulator.sh ModelLoader.sh NameServer.sh)
    configure_file(${script}.in ${DESTINATION}/${subbin}/${script} @ONLY)
  endforeach()

  foreach(script openhrp-grxui openhrp-jython-prompt openhrp-shutdown-servers)
    configure_file(${script}.sh.in ${DESTINATION}/${script} @ONLY)
  endforeach()

endmacro()


set(OPENHRPHOME ${PROJECT_BINARY_DIR})
set(OPENHRP_CONFDIR_NAME .OpenHRP-${OpenHRP_MAJOR_VERSION}.${OpenHRP_MINOR_VERSION})
set(GRXUI_JAVAVM ${JAVAVM})
set(GRXUI_JYTHON_HOME ${JYTHON_HOME})
set(GRXUI_ADDITIONAL_PATH ${OPENHRPHOME}/bin:${OPENHRPHOME}/bin/${subbin})
set(GRXUI_LD_LIBRARY_PATH ${JAVA_LD_LIBRARY_PATH})
set(GRXUI_CLASSPATH ${OPENHRPHOME}/java/grxui.jar:${OPENHRPHOME}/java/openhrpstubskel.jar:${CLASSPATH})
set(GRXUI_HOME ${PROJECT_SOURCE_DIR}/client/gui)

# TODO: set a full path if the program is not in the PATH
set(OMNINAMES omniNames)
# TODO: enable the follwoing values to be edited
set(NS_PORT 2809)
set(NS_HOST localhost)

create_bootscripts(${PROJECT_BINARY_DIR}/bin)



set(OPENHRPHOME ${CMAKE_INSTALL_PREFIX})
if(NOT ${OPENHRPHOME} STREQUAL /usr OR ${OPENHRPHOME} STREQUAL /usr/local)
  set(GRXUI_ADDITIONAL_PATH ${OPENHRPHOME}/bin)
endif()
set(GRXUI_CLASSPATH ${OPENHRPHOME}/${RELATIVE_GRXUI_INSTALL_PATH}/grxui.jar:${OPENHRPHOME}/${RELATIVE_JAVA_INSTALL_PATH}/openhrpstubskel.jar:${OPENHRPHOME}/${RELATIVE_JAVA_INSTALL_PATH}/vrml97.jar:${CLASSPATH})

set(RELATIVE_JAVA_INSTALL_PATH share/${OPENHRP_INSTALL_SUB_DIR}/java)

set(GRXUI_HOME ${CMAKE_INSTALL_PREFIX}/${RELATIVE_GRXUI_INSTALL_PATH})

create_bootscripts(${CMAKE_INSTALL_PREFIX}/bin)

install(PROGRAMS ${CMAKE_CURRENT_BINARY_DIR}/openhrp-grxui DESTINATION ${CMAKE_INSTALL_PREFIX}/bin)
