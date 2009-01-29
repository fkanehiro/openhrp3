set(sources
  SimulationExecutionContext.cpp
)

add_library(hrpRTM SHARED ${sources})

# interface version 0.0.0 - version 3.0.0 beta1
set_target_properties(hrpRTM PROPERTIES VERSION 0.0.0 SOVERSION 0)

target_link_libraries(hrpRTM
  ${OPENRTM_LIBRARIES} hrpCorbaStubSkel3
)

install(TARGETS hrpRTM
  RUNTIME DESTINATION bin CONFIGURATIONS Release Debug
  LIBRARY DESTINATION lib CONFIGURATIONS Release Debug
  ARCHIVE DESTINATION lib CONFIGURATIONS Release Debug
)


