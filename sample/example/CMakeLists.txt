add_subdirectory(scheduler)
add_subdirectory(simulationEC)
add_subdirectory(customizer)
add_subdirectory(move_ankle)
add_subdirectory(clap)
add_subdirectory(viewSimulator)
find_package(LibXml2)
if (LIBXML2_FOUND)
  add_subdirectory(smallSimulator)
endif()


