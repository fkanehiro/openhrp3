
# @author Shin'ichiro Nakaoka

include(${OPENHRP_IDL_DIR}/OpenHRP/IDL_FILE_BASENAMES.cmake)

set(idl_flags -bcxx -Wbuse_quotes -Wbh=.hh -Wbs=Sk.cpp -I${OPENRTM_IDL_DIR})

foreach(name ${IDL_FILE_BASENAMES})
  set(idl_files ${idl_files} ${OPENHRP_IDL_DIR}/OpenHRP/${name}.idl)
  set(idl_cpp_files ${idl_cpp_files} ${CMAKE_CURRENT_BINARY_DIR}/${name}Sk.cpp)
  set(idl_h_files ${idl_h_files} ${CMAKE_CURRENT_BINARY_DIR}/${name}.hh)
endforeach()

foreach(idl_basename ${IDL_FILE_BASENAMES})
  set(idl_file ${OPENHRP_IDL_DIR}/OpenHRP/${idl_basename}.idl)
  if(UNIX)
    add_custom_command(
      OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/${idl_basename}.hh ${CMAKE_CURRENT_BINARY_DIR}/${idl_basename}Sk.cpp
      COMMAND omniidl ${idl_flags} ${idl_file}
      DEPENDS ${idl_files}
      )
  elseif(WIN32)
    add_custom_command(
      OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/${idl_basename}.hh ${CMAKE_CURRENT_BINARY_DIR}/${idl_basename}Sk.cpp
      COMMAND for %%A in \("${OMNIORB_DIR}/bin/x86_win32"\) do %%~sA\\omniidl ${idl_flags} ${idl_file}
      DEPENDS ${idl_files}
      )
  endif()
endforeach()

add_library(hrpCorbaStubSkel3 STATIC ${idl_cpp_files})

if(WIN32)
  set_target_properties(hrpCorbaStubSkel3 PROPERTIES DEBUG_POSTFIX d)
endif() 

if(WIN32)
install(TARGETS hrpCorbaStubSkel3
	RUNTIME DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release Debug
	LIBRARY DESTINATION ${PROJECT_BINARY_DIR}/lib CONFIGURATIONS Release Debug
	ARCHIVE DESTINATION ${PROJECT_BINARY_DIR}/lib CONFIGURATIONS Release Debug
)
endif()

install(TARGETS hrpCorbaStubSkel3
	RUNTIME DESTINATION bin CONFIGURATIONS Release
	LIBRARY DESTINATION lib CONFIGURATIONS Release
	ARCHIVE DESTINATION lib CONFIGURATIONS Release
)

set(given_headers ORBwrap.h)

install(FILES ${given_headers} ${idl_h_files} DESTINATION include/hrpCorba)


