
add_subdirectory(controller)

install(DIRECTORY model project
  DESTINATION ${RELATIVE_SAMPLE_INSTALL_PATH}
  PATTERN ".svn" EXCLUDE)

