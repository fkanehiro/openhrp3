
configure_file(Config.h.in ${CMAKE_CURRENT_SOURCE_DIR}/Config.h)

set(sources
  Sensor.cpp
  Body.cpp
  BodyCustomizerInterface.cpp
  Link.cpp
  LinkTraverse.cpp
  LinkPath.cpp
  JointPath.cpp
  ForwardDynamics.cpp
  ForwardDynamicsABM.cpp
  ForwardDynamicsCBM.cpp
  World.cpp
  ConstraintForceSolver.cpp
  ModelNodeSet.cpp
  ModelLoaderUtil.cpp
  )

set(headers
  Body.h
  BodyCustomizerInterface.h
  ConstraintForceSolver.h
  ForwardDynamics.h
  ForwardDynamicsABM.h
  ForwardDynamicsCBM.h
  Link.h
  LinkTraverse.h
  LinkPath.h
  JointPath.h
  InverseKinematics.h
  ModelNodeSet.h
  ModelLoaderUtil.h
  Sensor.h
  World.h
  Config.h
  )	


set(target hrpModel-${OPENHRP_LIBRARY_VERSION})

add_library(${target} SHARED ${sources})

# interface version 0.0.0 - version 3.0.0 beta1
set_target_properties(${target} PROPERTIES VERSION 0.0.0 SOVERSION 0)


if(UNIX)
  target_link_libraries(${target}
    hrpUtil-${OPENHRP_LIBRARY_VERSION}
    hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}
    hrpCollision-${OPENHRP_LIBRARY_VERSION}
    ${OMNIORB_LIBRARIES}
    boost_filesystem boost_regex dl)

elseif(WIN32)
  add_definitions(-DHRPMODEL_MAKE_DLL)
  target_link_libraries(${target}
    optimized hrpUtil-${OPENHRP_LIBRARY_VERSION}
    optimized hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}
    optimized hrpCollision-${OPENHRP_LIBRARY_VERSION}
    debug hrpUtil-${OPENHRP_LIBRARY_VERSION}d
    debug hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}d
    debug hrpCollision-${OPENHRP_LIBRARY_VERSION}d
    ${OMNIORB_LIBRARIES})  
  set_target_properties(${target} PROPERTIES LINK_FLAGS /NODEFAULTLIB:LIBCMT )
  set_target_properties(${target} PROPERTIES DEBUG_POSTFIX d )
endif()

if(WIN32)
  install(TARGETS ${target}
    RUNTIME DESTINATION ${PROJECT_BINARY_DIR}/bin CONFIGURATIONS Release Debug
    LIBRARY DESTINATION ${PROJECT_BINARY_DIR}/lib CONFIGURATIONS Release Debug
    ARCHIVE DESTINATION ${PROJECT_BINARY_DIR}/lib CONFIGURATIONS Release Debug
    )
endif() 

install(TARGETS ${target}
  RUNTIME DESTINATION bin CONFIGURATIONS Release Debug
  LIBRARY DESTINATION lib CONFIGURATIONS Release Debug
  ARCHIVE DESTINATION lib CONFIGURATIONS Release Debug)

install(FILES ${headers} DESTINATION ${RELATIVE_HEADERS_INSTALL_PATH}/hrpModel)


# Body Customizers
set(OPENHRP_3_0_CUSTOMIZERS ${OPENHRP_3_0_CUSTOMIZERS} CACHE FILEPATH "Source files of the body customizer for OpenHRP 3.0")

if(OPENHRP_3_0_CUSTOMIZERS)

  foreach(src ${OPENHRP_3_0_CUSTOMIZERS})

    get_filename_component(customizer ${src} NAME_WE)
    add_library(${customizer} MODULE ${src})
    set_target_properties(${customizer} PROPERTIES COMPILE_FLAGS "-I${CMAKE_CURRENT_SOURCE_DIR}")
    set_target_properties(${customizer} PROPERTIES PREFIX "")
    set_target_properties(${customizer} PROPERTIES LIBRARY_OUTPUT_DIRECTORY ${PROJECT_BINARY_DIR}/customizer)

    install(TARGETS ${customizer}
      LIBRARY DESTINATION ${RELATIVE_SHARE_INSTALL_PATH}/customizer CONFIGURATIONS Release Debug)

  endforeach()
endif()


