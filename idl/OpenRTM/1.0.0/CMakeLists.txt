
include(RTM_IDL_FILE_BASENAMES.cmake)

set(rtm_idl_files)
foreach(name ${RTM_IDL_FILE_BASENAMES})
  list(APPEND rtm_idl_files ${name}.idl)
endforeach()

