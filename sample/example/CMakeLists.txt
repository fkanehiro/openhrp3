add_subdirectory(scheduler)
add_subdirectory(simulationEC)
add_subdirectory(customizer)

if(WIN32)
  add_subdirectory(move_ankle)
else()
  install(FILES move_ankle/Makefile move_ankle/move_ankle.cpp
  DESTINATION ${RELATIVE_SAMPLE_INSTALL_PATH}/example/move_ankle)
endif(WIN32)

