
# @author Shin'ichiro Nakaoka

set(package jp.go.aist.hrp.simulator)
string(REPLACE "." "/" package_path ${package})

include(${OpenHRP_SOURCE_DIR}/idl/OpenHRP/IDL_FILE_BASENAMES.cmake)

set(idl_files)
foreach(name ${IDL_FILE_BASENAMES})
  set(idl_file ${name}.idl)
  set(src_idl_file ${OPENHRP_IDL_DIR}/${name}.idl)
  add_custom_command(
    OUTPUT ${idl_file}
    COMMAND ${NATIVE2ASCII} -encoding UTF-8 ${src_idl_file} ${idl_file}
    DEPENDS ${src_idl_file}
    )
  list(APPEND idl_files ${idl_file})
endforeach()

# TODO: '-pkgPrefix' option should be used instead of '-pkgTranslate'
set(idlj_flags -fclient -fserver -pkgTranslate OpenHRP ${package} -td src)

foreach(idl_file ${idl_files})
  get_filename_component(idl_file_basename ${idl_file} NAME_WE)
  set(compiled_flag_file ${idl_file_basename}.compiled)
  add_custom_command(
    OUTPUT ${compiled_flag_file}
    COMMAND ${IDLJ} ${idlj_flags} ${idl_file}
    COMMAND ${CMAKE_COMMAND} -E echo "" > ${compiled_flag_file}
    DEPENDS ${idl_files}
   )
 list(APPEND idl_compiled_flag_files ${compiled_flag_file})
endforeach()

set(javac_flags -target 1.5 -d . -classpath ${package_path} -sourcepath src)

if(CMAKE_BUILD_TYPE STREQUAL Debug OR CMAKE_BUILD_TYPE STREQUAL RelWithDebInfo)
  list(APPEND javac_flags -g)
endif()

set(jarfile ${OpenHRP_BINARY_DIR}/java/openhrpstubskel.jar)

add_custom_command(
  OUTPUT ${jarfile} jp
  COMMAND ${JAVAC} ${javac_flags} src/${package_path}/*.java
  COMMAND ${JAR} cf ${jarfile} jp
  DEPENDS ${idl_compiled_flag_files}
)

add_custom_target(JavaSutbSkel ALL DEPENDS ${jarfile})

# add_custom_target(clean COMMAND ${RMDIR} src jp)

# set_directory_properties(PROPERTIES ADDITIONAL_MAKE_CLEAN_FILES "src;jp")
