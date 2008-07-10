
set(program openhrp-model-loader)

set(sources
  ModelLoader_impl.cpp
  BodyInfo_impl.cpp
  VrmlFieldCopyUtil.cpp
  ImageConverter.cpp
  server.cpp )

include_directories(${PROJECT_SOURCE_DIR}/OpenHRP/Parser)

add_executable(${program} ${sources})

target_link_libraries(${program} hrpParser3 hrpCorbaStubSkel3 ${OMNIORB_LIBRARIES})

install(TARGETS ${program} DESTINATION bin)
