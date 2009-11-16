
# @author Shin'ichiro Nakaoka

set(grxui_script_list
  openhrp-jython-prompt
  openhrp-shutdown-servers
  )

macro(create_bootscripts DESTINATION DO_INSTALL)
  
  # This is needed. CMake's bug ?
  set(DO_INSTALL ${DO_INSTALL})

  foreach(script ${grxui_script_list} )
    
    if(WIN32)
      set(script ${script}.${SH_SFX})
    endif()

    configure_file(${script}.in ${DESTINATION}/${script} @ONLY)
    if(DO_INSTALL)
      install(PROGRAMS ${DESTINATION}/${script} DESTINATION bin)
    endif()
  endforeach()
 
endmacro()


set(OPENHRPHOME ${PROJECT_BINARY_DIR})
set(OPENHRP_CONFDIR_NAME .OpenHRP-${OPENHRP_VERSION_MAJOR}.${OPENHRP_VERSION_MINOR})
set(GRXUI_JAVAVM ${JAVAVM})
set(GRXUI_JYTHON_HOME ${JYTHON_HOME})
set(GRXUI_LD_LIBRARY_PATH "${JAVA_LD_LIBRARY_PATH}")
set(GRXUI_ADDITIONAL_PATH ${OPENHRPHOME}/bin)
set(GRXUI_CLASSPATH ${PROJECT_BINARY_DIR}/java/grxui.jar${CPSEP}${PROJECT_BINARY_DIR}/java/openhrpstubskel.jar${CPSEP}${CLASSPATH})

# TODO: set a full path if the program is not in the PATH
if(UNIX)
  set(OMNINAMES omniNames)
elseif(WIN32)
  set(OMNINAMES ${OMNIORB_DIR}/bin/x86_win32/omniNames)
endif()

# TODO: enable the follwoing values to be edited
set(NS_PORT 2809)
set(NS_HOST localhost)

create_bootscripts(${PROJECT_BINARY_DIR}/bin false)



set(OPENHRPHOME ${CMAKE_INSTALL_PREFIX})
if(NOT ${OPENHRPHOME} STREQUAL /usr OR ${OPENHRPHOME} STREQUAL /usr/local)
  set(GRXUI_ADDITIONAL_PATH ${OPENHRPHOME}/bin)
endif()
set(GRXUI_CLASSPATH ${OPENHRPHOME}/${RELATIVE_GRXUI_INSTALL_PATH}/grxui.jar${CPSEP}${OPENHRPHOME}/${RELATIVE_JAVA_INSTALL_PATH}/openhrpstubskel.jar${CPSEP}${CLASSPATH})
set(RELATIVE_JAVA_INSTALL_PATH share/${OPENHRP_INSTALL_SUB_DIR}/java)

create_bootscripts(${CMAKE_CURRENT_BINARY_DIR} true)
