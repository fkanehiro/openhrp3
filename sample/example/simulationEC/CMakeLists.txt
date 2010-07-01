set(program SampleSimulationECComp)

set(sources 
  SampleSimulationEC.cpp
  SampleSimulationECComp.cpp
)

add_executable(${program} ${sources})

if(UNIX)
  target_link_libraries(${program} ${OPENRTM_LIBRARIES} )
elseif(WIN32)
  set_target_properties(${program} PROPERTIES DEBUG_POSTFIX d )
  target_link_libraries(${program} ${OPENRTM_LIBRARIES} )
endif()

if(WIN32)
  install(TARGETS ${program} DESTINATION ${CMAKE_CURRENT_SOURCE_DIR} CONFIGURATIONS Release )
endif()

install(TARGETS ${program} DESTINATION ${RELATIVE_SAMPLE_INSTALL_PATH}/example/SampleSimulationEC CONFIGURATIONS Release Debug)
install(FILES rtc.conf DESTINATION ${RELATIVE_SAMPLE_INSTALL_PATH}/example/SampleSimulationEC)

if(CMAKE_INSTALL_PREFIX)
  STRING(REGEX REPLACE "\\\\" "/" CMAKE_INSTALL_PREFIX_TMP "${CMAKE_INSTALL_PREFIX}")
  STRING(REGEX REPLACE "/$" "" CMAKE_INSTALL_PREFIX_TMP "${CMAKE_INSTALL_PREFIX_TMP}")
else()
  set(CMAKE_INSTALL_PREFIX_TMP "")
endif()

if(UNIX)
  set(LOAD_PATH "${CMAKE_INSTALL_PREFIX_TMP}/lib")
  set(SO_SFX "so")
elseif(WIN32)
  set(LOAD_PATH "${CMAKE_INSTALL_PREFIX_TMP}/bin")
  set(SO_SFX "dll")
endif()

configure_file( ${CMAKE_CURRENT_SOURCE_DIR}/rtc.conf.in ${CMAKE_CURRENT_SOURCE_DIR}/rtc.conf)