
set(program openhrp-model-loader)

set(sources
  ModelLoader_impl.cpp
  BodyInfo_impl.cpp
  UtilFunctions.cpp
  server.cpp )

add_executable(${program} ${sources})

if(UNIX)
  target_link_libraries(${program} hrpParser3 hrpCorbaStubSkel3 ${OMNIORB_LIBRARIES})
elseif(WIN32)
  target_link_libraries(${program} hrpParser3 hrpCorbaStubSkel3 ${OMNIORB_LIBRARIES} libpng zlib libjpeg )
endif()

if(WIN32)
  install(TARGETS ${program} DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release )
endif()

install(TARGETS ${program} DESTINATION bin)

