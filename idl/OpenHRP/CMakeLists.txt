
include(IDL_FILE_BASENAMES.cmake)

set(idl_files)
foreach(name ${IDL_FILE_BASENAMES})
  list(APPEND idl_files ${name}.idl)
endforeach()

if( DEBIANPACKAGE_DEVELOP )
  install(FILES ${idl_files} DESTINATION ${RELATIVE_IDL_INSTALL_PATH}/OpenHRP)
endif()
