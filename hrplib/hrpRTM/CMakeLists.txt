set(sources
  SimulationExecutionContext.cpp
)

if( NOT ADDITIONAL_SYMBOL STREQUAL "")
  add_definitions(-D${ADDITIONAL_SYMBOL})
endif()

set(target hrpRTM-${OPENHRP_LIBRARY_VERSION})

add_library(${target} SHARED ${sources})

# interface version 0.0.0 - version 3.0.0 beta1
set(HRPRTM_VERSION ${HRPSOVERSION}.0.0 )
set_target_properties(${target} PROPERTIES VERSION ${HRPRTM_VERSION} SOVERSION ${HRPSOVERSION})

target_link_libraries(${target}
  ${OPENRTM_LIBRARIES} hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}
)

install(TARGETS ${target}
  RUNTIME DESTINATION bin CONFIGURATIONS Release Debug
  LIBRARY DESTINATION lib CONFIGURATIONS Release Debug
  ARCHIVE DESTINATION lib CONFIGURATIONS Release Debug
)
