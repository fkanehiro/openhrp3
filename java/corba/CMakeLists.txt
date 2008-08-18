
# Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
# All rights reserved. This program is made available under the terms of the
# Eclipse Public License v1.0 which accompanies this distribution, and is
# available at http://www.eclipse.org/legal/epl-v10.html
# Contributors:
# National Institute of Advanced Industrial Science and Technology (AIST)
# General Robotix Inc. 

# @author Shin'ichiro Nakaoka


set(package jp.go.aist.hrp.simulator)
string(REPLACE "." "/" package_path ${package})

include(${OPENHRP_IDL_DIR}/IDL_FILE_BASENAMES.cmake)

set(idl_files)
foreach(name ${IDL_FILE_BASENAMES})
  set(idl_file ${CMAKE_CURRENT_BINARY_DIR}/${name}.idl)
  set(src_idl_file ${OPENHRP_IDL_DIR}/${name}.idl)
  if(UNIX) 
    add_custom_command(
      OUTPUT ${idl_file}
      COMMAND ${NATIVE2ASCII} -encoding UTF-8 ${src_idl_file} ${idl_file}
      COMMAND ${RMDIR} src
      COMMAND ${RMDIR} jp
      DEPENDS ${src_idl_file}
    )
  elseif(WIN32)
    add_custom_command(
      OUTPUT ${idl_file}
      COMMAND ${NATIVE2ASCII} -encoding UTF-8 ${src_idl_file} ${idl_file}
      COMMAND if exist src ${RMDIR} src
      COMMAND if exist jp ${RMDIR} jp
      DEPENDS ${src_idl_file}
    )
  endif() 
  list(APPEND idl_files ${idl_file})
endforeach()


# TODO: '-pkgPrefix' option should be used instead of '-pkgTranslate'
set(idlj_flags -fclient -fserver -pkgTranslate OpenHRP ${package} -td ${CMAKE_CURRENT_BINARY_DIR}/src)

foreach(idl_file ${idl_files})
  get_filename_component(idl_file_basename ${idl_file} NAME_WE)
  set(compiled_flag_file ${CMAKE_CURRENT_BINARY_DIR}/${idl_file_basename}.compiled)
  add_custom_command(
    OUTPUT ${compiled_flag_file}
    COMMAND ${IDLJ} ${idlj_flags} ${idl_file}
    COMMAND ${CMAKE_COMMAND} -E echo "" > ${compiled_flag_file}
    DEPENDS ${idl_files}
   )
 list(APPEND idl_compiled_flag_files ${compiled_flag_file})
endforeach()



set(javac_flags -target 1.5 -d ${CMAKE_CURRENT_BINARY_DIR}
  -classpath ${package_path} -sourcepath ${CMAKE_CURRENT_BINARY_DIR}/src)

if(CMAKE_BUILD_TYPE STREQUAL Debug OR CMAKE_BUILD_TYPE STREQUAL RelWithDebInfo)
  list(APPEND javac_flags -g)
endif()

set(jarfile ${OpenHRP_BINARY_DIR}/java/openhrpstubskel.jar)

if(UNIX)
  add_custom_command(
    OUTPUT ${jarfile} jp
    COMMAND ${JAVAC} ${javac_flags} ${CMAKE_CURRENT_BINARY_DIR}/src/${package_path}/*.java
    COMMAND ${JAR} cf ${jarfile} -C ${CMAKE_CURRENT_BINARY_DIR} jp
    DEPENDS ${idl_compiled_flag_files}
  )
elseif(WIN32)
  add_custom_command(
    OUTPUT ${jarfile} 
    COMMAND ${JAVAC} ${javac_flags} ${CMAKE_CURRENT_BINARY_DIR}/src/${package_path}/*.java
    COMMAND ${JAR} cf ${jarfile} -C ${CMAKE_CURRENT_BINARY_DIR} jp
    DEPENDS ${idl_compiled_flag_files}
  )
endif()

add_custom_target(JavaSutbSkel ALL DEPENDS ${jarfile})

#add_custom_target(clean COMMAND ${RMDIR} src jp)

# set_directory_properties(PROPERTIES ADDITIONAL_MAKE_CLEAN_FILES "src;jp")

install(FILES ${jarfile} DESTINATION ${RELATIVE_JAVA_INSTALL_PATH})
