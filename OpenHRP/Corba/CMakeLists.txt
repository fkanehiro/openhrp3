
# @author Shin'ichiro Nakaoka

include(${OpenHRP_SOURCE_DIR}/idl/OpenHRP/IDL_FILE_BASENAMES.cmake)

set(idl_flags -bcxx -Wbuse_quotes -Wbh=.h -Wbs=Sk.cpp -I ${OPENHRP_IDL_DIR})

foreach(name ${IDL_FILE_BASENAMES})
  set(idl_files ${idl_files} ${OPENHRP_IDL_DIR}/${name}.idl)
  set(idl_cpp_files ${idl_cpp_files} ${name}Sk.cpp)
  set(idl_h_files ${idl_h_files} ${name}.h)
endforeach()

foreach(idl_basename ${IDL_FILE_BASENAMES})
  set(idl_file ${OPENHRP_IDL_DIR}/${idl_basename}.idl)
  add_custom_command(
    OUTPUT ${idl_basename}.h ${idl_basename}Sk.cpp 
    COMMAND omniidl ${idl_flags} ${idl_file}
    DEPENDS ${idl_files}
   )
endforeach()

add_library(hrpCorbaStubSkel3 STATIC ${idl_cpp_files})

install(TARGETS hrpCorbaStubSkel3
	RUNTIME DESTINATION bin
	LIBRARY DESTINATION lib
	ARCHIVE DESTINATION lib)

set(given_headers ORBwrap.h)

install(FILES ${given_headers} ${CMAKE_CURRENT_BINARY_DIR}/${idl_h_files} DESTINATION include/OpenHRP/Corba)


