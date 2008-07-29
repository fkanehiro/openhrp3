
# @author Shin'ichiro Nakaoka


if(NOT TVMET_DIR)
    if(UNIX)
      find_path(
        TVMET_DIR
        NAMES include/tvmet/Vector.h
        PATHS /usr/local /usr
        DOC "the top directory of tvmet")
    elseif(WIN32)
      find_path(
        TVMET_DIR 
        NAMES include/tvmet/Vector.h
        PATHS $ENV{HOMEDRIVE}/Program Files $ENV{HOMEDRIVE}/
        PATH_SUFFIXES tvmet tvmet1.7.2 tvmet-1.7.2 
        DOC "the top directory of tvmet")  
     endif()
endif()

if(TVMET_DIR)
  string(REGEX REPLACE "/$" "" TVMET_DIR ${TVMET_DIR})
  if(EXISTS ${TVMET_DIR}/include/tvmet/Vector.h)
    set(TVMET_FOUND TRUE)
  endif()
endif()

set(TVMET_DIR ${TVMET_DIR} CACHE PATH "The top directory of tvmet")
set(TVMET_INCLUDE_DIR ${TVMET_DIR}/include)

if(TVMET_FOUND)
  if(NOT Tvmet_FIND_QUIETLY)
    message(STATUS "Found tvment headers in ${TVMET_INCLUDE_DIR}")
   endif()
else()
  if(Tvmet_FIND_REQUIRED)
    message(FATAL_ERROR "Could not find tvmet. Please set TVMET_DIR correctly")
  endif()
endif()
