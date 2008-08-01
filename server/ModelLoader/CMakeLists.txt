
set(program openhrp-model-loader)

set(sources
  ModelLoader_impl.cpp
  BodyInfo_impl.cpp
  UtilFunctions.cpp
  server.cpp )

add_executable(${program} ${sources})

target_link_libraries(${program} hrpUtil3 hrpParser3 hrpCorbaStubSkel3 ${OMNIORB_LIBRARIES})

if(WIN32)
  install(TARGETS ${program} DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release )
endif()

install(TARGETS ${program} DESTINATION bin CONFIGURATIONS Release)

