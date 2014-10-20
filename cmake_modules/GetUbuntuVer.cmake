#  @author Takafumi Tawara

SET(UBUNTU_VERSION "UBUNTU_VERSION-NOTFOUND")
SET(UBUNTU_VERSION_NUM "UBUNTU_VERSION_NUM-NOTFOUND")
SET(UBUNTU_CODENAME "UBUNTU_CODENAME-NOTFOUND")

if(UNIX AND NOT APPLE AND NOT QNXNTO AND EXISTS /etc/lsb-release)
  EXECUTE_PROCESS(COMMAND cat /etc/lsb-release
                  RESULT_VARIABLE  result_val
                  OUTPUT_VARIABLE  output_val
                  ERROR_VARIABLE   error_val
                  OUTPUT_STRIP_TRAILING_WHITESPACE
                  ERROR_STRIP_TRAILING_WHITESPACE)
  if(result_val)
    MESSAGE("result_val:${result_val}")
  endif()
  if(error_val)
    MESSAGE(FATAL_ERROR "${error_val}")
  endif()

  # Ubuntu version section
  if(${output_val} MATCHES "DISTRIB_RELEASE=")
    STRING(REGEX REPLACE ".*DISTRIB_RELEASE=([^\n]+).*" "\\1"
           UBUNTU_VERSION ${output_val})
    # Ubuntu version number section
    STRING(REGEX REPLACE "[\\.]" ""
           UBUNTU_VERSION_NUM ${UBUNTU_VERSION})
  endif()

  # Ubuntu codename section
  if(${output_val} MATCHES "DISTRIB_CODENAME=")
    STRING(REGEX REPLACE ".*DISTRIB_CODENAME=([^\n]+).*" "\\1"
           UBUNTU_CODENAME ${output_val})
  endif()
endif(UNIX AND NOT APPLE AND NOT QNXNTO AND EXISTS /etc/lsb-release)


