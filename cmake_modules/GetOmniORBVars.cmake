#  @author Takafumi Tawara

SET(OMNIORB_VERSION "OMNIORB_VERSION-NOTFOUND")
SET(OMNIORB_VERSION_NUM "OMNIORB_VERSION_NUM-NOTFOUND")
SET(OMNIORB_THREAD_NUM "OMNIORB_THREAD_NUM-NOTFOUND")

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
    # omniORB version number section
    STRING(REGEX REPLACE "THIS_IS_OMNIORB" ""
           OMNIORB_VERSION_NUM ${OMNIORB_VERSION})
    STRING(REGEX REPLACE "[_]" ""
           OMNIORB_VERSION_NUM ${OMNIORB_VERSION_NUM})
  endif()

  EXECUTE_PROCESS(COMMAND cmd /c dir /B "${OMNIORB_LIBRARY_DIRS}\\omnithread*"
                  RESULT_VARIABLE  result_val
                  OUTPUT_VARIABLE  output_val
                  ERROR_VARIABLE   error_val
                  OUTPUT_STRIP_TRAILING_WHITESPACE
                  ERROR_STRIP_TRAILING_WHITESPACE)

  # omnithread number section
  if(${output_val} MATCHES "omnithread")
    SET(OMNIORB_THREAD_NUM "${output_val}")
    # omniORB version number section
    STRING(REGEX REPLACE ".*omnithread([0-9]+)_rt\\.lib.*" "\\1"
           OMNIORB_THREAD_NUM ${OMNIORB_THREAD_NUM})
  endif()
endif(WIN32)
