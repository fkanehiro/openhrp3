
# @author Shin'ichiro Nakaoka

include(${OPENHRP_IDL_DIR}/OpenHRP/IDL_FILE_BASENAMES.cmake)

set(idl_flags_cpp ${VERBOSE_FLAG} -bcxx -Wbuse_quotes -Wbh=.hh -Wbs=Sk.cpp -I${OPENRTM_IDL_DIR})
set(idl_flags_py  ${VERBOSE_FLAG} -bpython -I${OPENRTM_IDL_DIR})

if( NOT ADDITIONAL_SYMBOL STREQUAL "")
  set(idl_flags_cpp ${idl_flags_cpp} -D ${ADDITIONAL_SYMBOL})
  set(idl_flags_py  ${idl_flags_py}  -D ${ADDITIONAL_SYMBOL})
endif()


foreach(name ${IDL_FILE_BASENAMES})
  set(idl_files ${idl_files} ${OPENHRP_IDL_DIR}/OpenHRP/${name}.idl)
  set(idl_cpp_files ${idl_cpp_files} ${CMAKE_CURRENT_BINARY_DIR}/${name}Sk.cpp)
  set(idl_h_files ${idl_h_files} ${CMAKE_CURRENT_BINARY_DIR}/${name}.hh)
  set(idl_py_files ${idl_py_files} ${CMAKE_CURRENT_BINARY_DIR}/${name}_idl.py)
endforeach()

foreach(idl_basename ${IDL_FILE_BASENAMES})
  set(idl_file ${OPENHRP_IDL_DIR}/OpenHRP/${idl_basename}.idl)
  if(UNIX)
    add_custom_command(
      OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/${idl_basename}.hh ${CMAKE_CURRENT_BINARY_DIR}/${idl_basename}Sk.cpp
      COMMAND omniidl ${idl_flags_cpp} ${idl_file}
      DEPENDS ${idl_files}
      )
    add_custom_command(
      OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/${idl_basename}_idl.py
      COMMAND omniidl ${idl_flags_py} ${idl_file}
      DEPENDS ${idl_files}
      )
  elseif(WIN32)
    add_custom_command(
      OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/${idl_basename}.hh ${CMAKE_CURRENT_BINARY_DIR}/${idl_basename}Sk.cpp
      COMMAND set PATH= "${OMNIORB_DIR}/bin/x86_win32"\;%PATH%
      COMMAND omniidl ${idl_flags_cpp} ${idl_file}
      DEPENDS ${idl_files}
      )
    add_custom_command(
      OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/${idl_basename}_idl.py
      COMMAND set PATH= "${OMNIORB_DIR}/bin/x86_win32"\;%PATH%
      COMMAND omniidl ${idl_flags_py} ${idl_file}
      DEPENDS ${idl_files}
      )
  endif()
endforeach()

set(target hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION})

add_library(${target} STATIC ${idl_cpp_files} ${idl_py_files})

if(UNIX)
  set_target_properties(${target} PROPERTIES COMPILE_FLAGS -fPIC)
endif()

if(WIN32)
  set_target_properties(${target} PROPERTIES DEBUG_POSTFIX d)
endif() 

if(WIN32)
install(TARGETS ${target}
	RUNTIME DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release Debug
	LIBRARY DESTINATION ${PROJECT_BINARY_DIR}/lib CONFIGURATIONS Release Debug
	ARCHIVE DESTINATION ${PROJECT_BINARY_DIR}/lib CONFIGURATIONS Release Debug
)
endif()

install(TARGETS ${target}
	RUNTIME DESTINATION bin CONFIGURATIONS Release Debug RelWithDebInfo
	LIBRARY DESTINATION lib CONFIGURATIONS Release Debug RelWithDebInfo
	ARCHIVE DESTINATION lib CONFIGURATIONS Release Debug RelWithDebInfo
)

set(given_headers ORBwrap.h)

add_custom_command(
  OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/OpenHRP/__init__.py
  COMMAND sed -i -e "'s@import omniORB$$@import omniORB; from OpenRTM_aist.RTM_IDL import SDOPackage_idl@'" ${CMAKE_CURRENT_BINARY_DIR}/OpenHRP/__init__.py
  DEPENDS ${idl_py_files}
  )
add_custom_target(fix_openhrp_idl ALL DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/OpenHRP/__init__.py)

install(FILES ${given_headers} ${idl_h_files} DESTINATION ${RELATIVE_HEADERS_INSTALL_PATH}/hrpCorba)
execute_process(
  COMMAND python -c "import sys; print '%s.%s'%(sys.version_info[0:2])"
  OUTPUT_VARIABLE python_version
  OUTPUT_STRIP_TRAILING_WHITESPACE)


set(idl_py_files ${idl_py_files} ${CMAKE_CURRENT_BINARY_DIR}/OpenHRP/__init__.py)
install(FILES ${idl_py_files} DESTINATION lib/python${python_version}/dist-packages/OpenHRP)
