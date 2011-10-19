set(sources
  Algorithm.cpp
  Mobility.cpp
  PRM.cpp
  RRT.cpp
  PathPlanner.cpp
  TimeUtil.cpp
  Roadmap.cpp
  TGT.cpp
  RoadmapNode.cpp
  ShortcutOptimizer.cpp 
  RandomShortcutOptimizer.cpp 
  OmniWheel.cpp
  Configuration.cpp
  )

set(headers
  Algorithm.h
  Mobility.h
  PRM.h
  RRT.h
  PathPlanner.h
  TimeUtil.h
  Roadmap.h
  TGT.h
  RoadmapNode.h
  ShortcutOptimizer.h 
  RandomShortcutOptimizer.h 
  OmniWheel.h
  Configuration.h
  Optimizer.h
  exportdef.h
  )

set(target hrpPlanner-${OPENHRP_LIBRARY_VERSION})

add_library(${target} SHARED ${sources})

# interface version 0.0.0 - version 3.0.0 beta1
set(HRPPLANNER_VERSION ${HRPSOVERSION}.0.0 )
set_target_properties(${target} PROPERTIES VERSION ${HRPPLANNER_VERSION} SOVERSION ${HRPSOVERSION})

if(UNIX)
  target_link_libraries(${target}
    hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}
    hrpModel-${OPENHRP_LIBRARY_VERSION}
    hrpCollision-${OPENHRP_LIBRARY_VERSION})
elseif(WIN32)
  add_definitions(-DHRPPLANNER_MAKE_DLL)
  target_link_libraries(${target}
    optimized hrpModel-${OPENHRP_LIBRARY_VERSION}
    optimized hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}
    optimized hrpCollision-${OPENHRP_LIBRARY_VERSION}
    debug hrpModel-${OPENHRP_LIBRARY_VERSION}d
    debug hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}d
    debug hrpCollision-${OPENHRP_LIBRARY_VERSION}d
    ${OMNIORB_LIBRARIES})
  set_target_properties(${target} PROPERTIES LINK_FLAGS /NODEFAULTLIB:LIBCMT )
  set_target_properties(${target} PROPERTIES DEBUG_POSTFIX d )
endif()

hrplib_install_macro(${target} ${HRPPLANNER_VERSION})

install(FILES ${headers} DESTINATION ${RELATIVE_HEADERS_INSTALL_PATH}/hrpPlanner)
