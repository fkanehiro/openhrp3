set(jarfile grxui.jar)

set(source_files com/generalrobotix/ui/*.java com/generalrobotix/ui/util/*.java com/generalrobotix/ui/item/*.java com/generalrobotix/ui/view/*.java com/generalrobotix/ui/view/tdview/*.java com/generalrobotix/ui/view/graph/*.java com/generalrobotix/ui/view/simulation/*.java com/generalrobotix/ui/view/vsensor/*.java)
set(class_files com/generalrobotix/ui/*.class com/generalrobotix/ui/util/*.class com/generalrobotix/ui/item/*.class com/generalrobotix/ui/view/*.class com/generalrobotix/ui/view/tdview/*.class com/generalrobotix/ui/view/graph/*.class com/generalrobotix/ui/view/simulation/*.class com/generalrobotix/ui/view/vsensor/*.class)

set(java_cflags -encoding EUCJIS -Xlint:unchecked -Djava.ext.dirs=JMF2.1.1e/unix/lib:Java3D/1.4.1/unix/lib/ext:Java3D/1.4.1/unix/lib/:/jre/lib/ext:/jre/lib/ -cp ${OpenHRP_BINARY_DIR}/java/openhrpstubskel.jar:vrml97.jar:${JYTHON_JAR} -sourcepath ${CMAKE_CURRENT_SOURCE_DIR} ${source_files})

add_custom_command(
    OUTPUT ${jarfile}
    COMMAND ${JAVAC} ${java_cflags}
    COMMAND ${JAR} cmf Manifest.mf ${jarfile} resources ${class_files}
    WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
)

add_custom_target(GrxuiJar ALL DEPENDS ${jarfile})
