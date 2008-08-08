
# @author Shin'ichiro Nakaoka

set(script_list
  CollisionDetector
  DynamicsSimulator
  ModelLoader
  NameServer
  )

set(grxui_script_list
  openhrp-grxui
  openhrp-jython-prompt
  openhrp-shutdown-servers
  )

if(UNIX)
  set(subbin unix)
elseif(WIN32)
  set(subbin dos)
endif()

macro(create_bootscripts DESTINATION DO_INSTALL)
  
  # This is needed. CMake's bug ?
  set(DO_INSTALL ${DO_INSTALL})

  foreach(script ${script_list} )
    configure_file(${script}.${SH_SFX}.in ${DESTINATION}/${subbin}/${script}.${SH_SFX} @ONLY)
    if(DO_INSTALL)
	  install(PROGRAMS ${DESTINATION}/${subbin}/${script}.${SH_SFX} DESTINATION ${CMAKE_INSTALL_PREFIX}/bin/${subbin})
    endif()
  endforeach()

  foreach(script ${grxui_script_list} )
    
    if(WIN32)
      set(script ${script}.${SH_SFX})
    endif()

    configure_file(${script}.in ${DESTINATION}/${script} @ONLY)
    if(DO_INSTALL)
      install(PROGRAMS ${DESTINATION}/${script} DESTINATION ${CMAKE_INSTALL_PREFIX}/bin)
    endif()
  endforeach()
 
endmacro()


set(OPENHRPHOME ${PROJECT_BINARY_DIR})
set(OPENHRP_CONFDIR_NAME .OpenHRP-${OpenHRP_MAJOR_VERSION}.${OpenHRP_MINOR_VERSION})
set(GRXUI_JAVAVM ${JAVAVM})
set(GRXUI_JYTHON_HOME ${JYTHON_HOME})
set(GRXUI_LD_LIBRARY_PATH ${JAVA_LD_LIBRARY_PATH})
if(UNIX)
  set(GRXUI_ADDITIONAL_PATH ${OPENHRPHOME}/bin:${OPENHRPHOME}/bin/${subbin})
  set(GRXUI_CLASSPATH ${PROJECT_BINARY_DIR}/java/grxui.jar:${PROJECT_BINARY_DIR}/java/openhrpstubskel.jar:${CLASSPATH})
elseif(WIN32)
  set(GRXUI_ADDITIONAL_PATH "${PROJECT_BINARY_DIR}/bin;${PROJECT_BINARY_DIR}/bin/${subbin}")
  set(GRXUI_CLASSPATH "${PROJECT_BINARY_DIR}/java/grxui.jar;${PROJECT_BINARY_DIR}/java/openhrpstubskel.jar;${CLASSPATH}")
endif()
set(GRXUI_HOME ${PROJECT_SOURCE_DIR}/java/grxui)

file(TO_NATIVE_PATH ${GRXUI_HOME} GRXUI_HOME)

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
if(UNIX)
  set(GRXUI_CLASSPATH ${OPENHRPHOME}/${RELATIVE_GRXUI_INSTALL_PATH}/grxui.jar:${OPENHRPHOME}/${RELATIVE_JAVA_INSTALL_PATH}/openhrpstubskel.jar:${CLASSPATH})
elseif(WIN32)
  set(GRXUI_CLASSPATH "${OPENHRPHOME}/${RELATIVE_GRXUI_INSTALL_PATH}/grxui.jar;${OPENHRPHOME}/${RELATIVE_JAVA_INSTALL_PATH}/openhrpstubskel.jar;${CLASSPATH}")
endif()

set(RELATIVE_JAVA_INSTALL_PATH share/${OPENHRP_INSTALL_SUB_DIR}/java)

set(GRXUI_HOME ${CMAKE_INSTALL_PREFIX}/${RELATIVE_GRXUI_INSTALL_PATH})
file(TO_NATIVE_PATH ${GRXUI_HOME} GRXUI_HOME)

create_bootscripts(${CMAKE_CURRENT_BINARY_DIR} true)
