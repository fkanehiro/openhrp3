
# @author Takafumi Tawara

set(package jp.go.aist.openrtm)
string(REPLACE "." "/" package_path ${package})
include(${OPENHRP_RTM_IDL_DIR}/RTM_IDL_FILE_BASENAMES.cmake)

set(idlj_files)
foreach(name ${RTM_IDL_FILE_BASENAMES})
  set(idlj_file ${CMAKE_CURRENT_BINARY_DIR}/${name}.idl)
  set(src_idlj_file ${OPENHRP_RTM_IDL_DIR}/${name}.idl)

  if(UNIX) 
    add_custom_command(
      OUTPUT ${idlj_file}
      COMMAND ${NATIVE2ASCII} -encoding UTF-8 ${src_idlj_file} ${idlj_file}
      COMMAND if [ -e src ]; then ${RMDIR} src\; fi
      COMMAND if [ -e jp ]; then ${RMDIR} jp\; fi
      COMMAND if [ -e RTC ]; then ${RMDIR} RTC\; fi
      COMMAND if [ -e OpenRTM ]; then ${RMDIR} OpenRTM\; fi
      COMMAND if [ -e RTM ]; then ${RMDIR} RTM\; fi
      COMMAND if [ -e _SDOPackage ]; then ${RMDIR} _SDOPackage\; fi
      DEPENDS ${src_idlj_file}
    )
  elseif(WIN32)
    add_custom_command(
      OUTPUT ${idlj_file}
      COMMAND ${NATIVE2ASCII} -encoding UTF-8 ${src_idlj_file} ${idlj_file}
      COMMAND if exist src ${RMDIR} src
      COMMAND if exist jp ${RMDIR} jp
      COMMAND if exist RTC ${RMDIR} RTC
      COMMAND if exist OpenRTM ${RMDIR} OpenRTM
      COMMAND if exist RTM ${RMDIR} RTM
      COMMAND if exist _SDOPackage ${RMDIR} _SDOPackage
      DEPENDS ${src_idlj_file}
    )
  endif() 
  list(APPEND idlj_files ${idlj_file})
endforeach()

set(idlj_flags ${VERBOSE_FLAG} -fclient -fserver -td ${CMAKE_CURRENT_BINARY_DIR}/src -i ${OPENHRP_RTM_IDL_DIR} )
foreach(idlj_file ${idlj_files})
  get_filename_component(idl_file_basename ${idlj_file} NAME_WE)
  set(compiled_flag_file ${CMAKE_CURRENT_BINARY_DIR}/${idl_file_basename}.compiled)
  add_custom_command(
    OUTPUT ${compiled_flag_file}
    COMMAND ${IDLJ} ${idlj_flags} -pkgTranslate ${idl_file_basename} ${package}.${idl_file_basename} ${idlj_file}
    COMMAND ${IDLJ} ${idlj_flags} ${idlj_file}
    COMMAND ${CMAKE_COMMAND} -E echo "" > ${compiled_flag_file}
    DEPENDS ${idlj_files}
   )
  list(APPEND idl_compiled_flag_files ${compiled_flag_file})
endforeach()

set(jarDirs OpenRTM RTM RTC _SDOPackage jp)

if(UNIX)
set(javac_flags ${JAVAC_VFLAG} -target 1.5 -d ${CMAKE_CURRENT_BINARY_DIR}
  -classpath ${package_path} -sourcepath ${CMAKE_CURRENT_BINARY_DIR}/src)
elseif(WIN32)
set(javac_flags ${JAVAC_VFLAG} -target 1.5 -d ${CMAKE_CURRENT_BINARY_DIR}
  -classpath "${package_path}" -sourcepath ${CMAKE_CURRENT_BINARY_DIR}/src)
endif()

if(CMAKE_BUILD_TYPE STREQUAL Debug OR CMAKE_BUILD_TYPE STREQUAL RelWithDebInfo)
  list(APPEND javac_flags -g)
endif()

set(jarfile ${OpenHRP_BINARY_DIR}/java/${rtm_stub_jar})

if(UNIX)
  add_custom_command(
    OUTPUT ${jarfile}
    COMMAND ${JAVAC} ${javac_flags} src/${package_path}/*/*.java
    COMMAND ${JAVAC} ${javac_flags} src/*/*.java
    COMMAND ${JAR} cf ${jarfile} -C ${CMAKE_CURRENT_BINARY_DIR} ${jarDirs}
    COMMAND cp -fu ${jarfile} ${GrxUI_ON_ECLIPSE_DIR}/
    DEPENDS ${idl_compiled_flag_files}
  )
elseif(WIN32)
  STRING( REPLACE "/" "\\" win32_jarfile "${jarfile}")
  STRING( REPLACE "/" "\\" win32_GrxUIonEclipseDir "${GrxUI_ON_ECLIPSE_DIR}/" )
  STRING( REPLACE "/" "\\" win32_CmakeCurrentBinaryDir "${CMAKE_CURRENT_BINARY_DIR}/" )
  add_custom_command(
    OUTPUT ${jarfile}
    COMMAND del \"${win32_CmakeCurrentBinaryDir}src.lst\"
    COMMAND for /R \"${win32_CmakeCurrentBinaryDir}src\" %%i in \(*.java\) do echo %%i >> \"${win32_CmakeCurrentBinaryDir}src.lst\"
    COMMAND ${JAVAC} ${javac_flags} @${CMAKE_CURRENT_BINARY_DIR}/src.lst
    COMMAND ${JAR} cf ${jarfile} -C ${CMAKE_CURRENT_BINARY_DIR} ${jarDirs}
    COMMAND copy /Y /D ${win32_jarfile} ${win32_GrxUIonEclipseDir}
    COMMAND del \"${win32_CmakeCurrentBinaryDir}src.lst\"
    DEPENDS ${idl_compiled_flag_files}
  )
endif()

add_custom_target(OpenRTMStubSkel ALL DEPENDS ${jarfile})

install(FILES ${jarfile} DESTINATION ${RELATIVE_JAVA_INSTALL_PATH})
