
add_subdirectory(controller)
add_subdirectory(example)

install(DIRECTORY model project
  DESTINATION ${RELATIVE_SAMPLE_INSTALL_PATH}
  PATTERN ".svn" EXCLUDE)

