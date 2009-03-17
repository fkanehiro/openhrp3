set(sources
  chain.cpp
  com.cpp
  desire.cpp
  dims_clapack.cpp
  dp.cpp
  edit.cpp
  fEulerPara.cpp
  fLineVec.cpp
  fMatrix.cpp
  fMatrix3.cpp
  fMatrix4.cpp
  fk.cpp
  handle.cpp
  id.cpp
  ik.cpp
  init.cpp
  integ.cpp
  jacobi.cpp
  joint.cpp
  lcp.cpp
  lcp_pivot.cpp
  limit.cpp
  psim.cpp
  schedule.cpp
  sdcontact.cpp
  update.cpp
  update_ik.cpp
  update_lcp.cpp
  vary.cpp
  )

include_directories(.)
add_definitions(-DSEGA -DUSE_CLAPACK_INTERFACE)
add_library(sDIMS SHARED ${sources})
set_target_properties(sDIMS PROPERTIES VERSION 0.0.0 SOVERSION 0)

target_link_libraries(sDIMS ${LAPACK_LIBRARIES})

if (WIN32)
  add_definitions(-DHRPBASE_MAKE_DLL)
  set_target_properties(sDIMS PROPERTIES LINK_FLAGS /NODEFAULTLIB:MSVCRT)
  set_target_properties(sDIMS PROPERTIES LINK_FLAGS /NODEFAULTLIB:LIBCMT)
  set_target_properties(sDIMS PROPERTIES DEBUG_POSTFIX d )
endif()


if(WIN32)
install(TARGETS sDIMS DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release)
endif()

install(TARGETS sDIMS DESTINATION bin CONFIGURATIONS Release Debug)
