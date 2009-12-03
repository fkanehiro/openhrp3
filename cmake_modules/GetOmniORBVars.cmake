#  @author Takafumi Tawara

SET(OMNIORB_VERSION "OMNIORB_VERSION-NOTFOUND")
SET(OMNIORB_VERSION_NUM "OMNIORB_VERSION_NUM")

if(WIN32)
  EXECUTE_PROCESS(COMMAND cmd /c dir /B "${OMNIORB_DIR}\\THIS_IS_OMNIORB*"
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

  # omniORB version section
  if(${output_val} MATCHES "THIS_IS_OMNIORB")
    SET(OMNIORB_VERSION "${output_val}")
    # Ubuntu version number section
    STRING(REGEX REPLACE "THIS_IS_OMNIORB" ""
           OMNIORB_VERSION_NUM ${OMNIORB_VERSION})
    STRING(REGEX REPLACE "[_]" ""
           OMNIORB_VERSION_NUM ${OMNIORB_VERSION_NUM})
  endif()
endif(WIN32)
