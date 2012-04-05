configure_file(openhrp-grxui-classic.in ${OpenHRP_BINARY_DIR}/bin/openhrp-grxui-classic @ONLY)

set(jarfile ${OpenHRP_BINARY_DIR}/java/grxui.jar)

set(source_files 
  com/generalrobotix/ui/*.java 
  com/generalrobotix/ui/util/*.java 
  com/generalrobotix/ui/item/*.java 
  com/generalrobotix/ui/view/*.java 
  com/generalrobotix/ui/view/tdview/*.java 
  com/generalrobotix/ui/view/graph/*.java 
  com/generalrobotix/ui/view/simulation/*.java 
  com/generalrobotix/ui/view/vsensor/*.java)

if(UNIX)
  set(java_cflags 
    -encoding EUCJIS 
    -Xlint:unchecked 
    -Djava.ext.dirs=JMF2.1.1e/unix/lib:Java3D/1.4.1/unix/lib/ext:Java3D/1.4.1/unix/lib/:/jre/lib/ext:/jre/lib/ 
    -d ${CMAKE_CURRENT_BINARY_DIR} 
    -cp ${OpenHRP_BINARY_DIR}/java/openhrpstubskel.jar:vrml97.jar:${JYTHON_JAR} 
    -sourcepath ${CMAKE_CURRENT_SOURCE_DIR} ${source_files})
elseif(WIN32)
  set(java_cflags 
    -encoding EUCJIS 
    -Xlint:unchecked 
    "-Djava.ext.dirs=JMF2.1.1e/Windows/lib\;Java3D/1.4.1/Windows/lib/ext\;Java3D/1.4.1/Windows/lib/\;${JDK_DIR}/jre/lib/ext\;${JDK_DIR}/jre/lib/" 
    -d ${CMAKE_CURRENT_BINARY_DIR} 
    -cp "${OpenHRP_BINARY_DIR}/java/openhrpstubskel.jar\;vrml97.jar\;${JYTHON_JAR}" 
    -sourcepath ${CMAKE_CURRENT_SOURCE_DIR} ${source_files})
endif()

add_custom_command(
    OUTPUT ${jarfile}
    COMMAND ${JAVAC} ${java_cflags}
    COMMAND ${JAR} cmf Manifest.mf ${jarfile} resources -C ${CMAKE_CURRENT_BINARY_DIR} com
    WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
    DEPENDS ${source_files}
)

add_custom_target(GrxuiJar ALL DEPENDS ${jarfile})

install(PROGRAMS 
  ${OpenHRP_BINARY_DIR}/bin/openhrp-grxui-classic 
  DESTINATION bin)
install(FILES ${jarfile} vrml97.jar DESTINATION ${RELATIVE_JAVA_INSTALL_PATH})
install(FILES grxuirc.xml DESTINATION ${RELATIVE_JAVA_INSTALL_PATH}/grxui)
install(DIRECTORY Java3D  JMF2.1.1e
  DESTINATION ${RELATIVE_JAVA_INSTALL_PATH}
  PATTERN .svn EXCLUDE)
install(DIRECTORY script
  DESTINATION ${RELATIVE_JAVA_INSTALL_PATH}/grxui
  PATTERN .svn EXCLUDE)
