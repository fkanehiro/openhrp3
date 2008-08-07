
# Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
# All rights reserved. This program is made available under the terms of the
# Eclipse Public License v1.0 which accompanies this distribution, and is
# available at http://www.eclipse.org/legal/epl-v10.html
# Contributors:
# National Institute of Advanced Industrial Science and Technology (AIST)
# General Robotix Inc. 

# @author Shin'ichiro Nakaoka


cmake_minimum_required(VERSION 2.4)

project(OpenHRP)

set(OpenHRP_MAJOR_VERSION 3)
set(OpenHRP_MINOR_VERSION 1)
set(OpenHRP_RELEASE_VERSION 0)

set(CMAKE_ALLOW_LOOSE_LOOP_CONSTRUCTS true)
set(CMAKE_MODULE_PATH ${PROJECT_SOURCE_DIR}/cmake_modules/)

include(FindPkgConfig)
include(CheckIncludeFiles)


option(ENABLE_INSTALL_RPATH "Enable RPATH setting for installed binary files" OFF)

if(ENABLE_INSTALL_RPATH)
  SET(CMAKE_SKIP_BUILD_RPATH  FALSE)
  SET(CMAKE_BUILD_WITH_INSTALL_RPATH FALSE) 
  SET(CMAKE_INSTALL_RPATH "${CMAKE_INSTALL_PREFIX}/lib")
  SET(CMAKE_INSTALL_RPATH_USE_LINK_PATH TRUE)
else()
  SET(CMAKE_SKIP_BUILD_RPATH  FALSE)
  SET(CMAKE_BUILD_WITH_INSTALL_RPATH FALSE) 
  SET(CMAKE_INSTALL_RPATH "")
  SET(CMAKE_INSTALL_RPATH_USE_LINK_PATH FALSE)
endif()


# commands
if(UNIX)
  set(RMDIR rm -fr)
elseif(WIN32)
  set(RMDIR rmdir /S/Q)
endif()

# shell script file suffix
if(UNIX)
  set(SH_SFX "sh")
elseif(WIN32)
  set(SH_SFX "bat")
endif()
    
# check "dlfcn.h" for using dlopen() and dlclose()
if(UNIX)
  check_include_files(dlfcn.h HAVE_DLFCN_H)
  if(NOT HAVE_DLFCN_H)
    message(FATAL_ERROR "Could not find dlfcn.h")
  endif()
endif()

# check boost
if(NOT Boost_INCLUDE_DIRS AND NOT Boost_LIBRARY_DIRS)
  if(WIN32)
    if( NOT $ENV{BOOST_ROOT} STREQUAL "" )
      set(BOOST_ROOT $ENV{BOOST_ROOT} )
    endif()
    set(BOOST_ROOT ${BOOST_ROOT} CACHE PATH "The root directory of boost")
  endif()
  find_package(Boost REQUIRED)
  if(Boost_FOUND)
    message(STATUS "Found Boost C++ library in ${Boost_INCLUDE_DIRS}")
  endif()
endif()  

include_directories(${Boost_INCLUDE_DIRS})
link_directories(${Boost_LIBRARY_DIRS})

set(Boost_INCLUDE_DIRS ${Boost_INCLUDE_DIRS} CACHE PATH "Directories for searching boost header files")
set(Boost_LIBRARY_DIRS ${Boost_LIBRARY_DIRS} CACHE PATH "Directories for searching boost library files")


# check blas and lapack
find_package(Lapack REQUIRED)

include_directories(${LAPACK_INCLUDE_DIRS})
link_directories(${LAPACK_LIBRARY_DIRS})


# tvmet
find_package(Tvmet REQUIRED)

include_directories(${TVMET_INCLUDE_DIR})


# jpeg
if(UNIX)
  find_package(JPEG REQUIRED)
elseif(WIN32)
  set(JPEG_INCLUDE_DIR CACHE PATH "Directories for searching jpeg include files" )
	set(JPEG_LIBRARY_DIR CACHE PATH "Directories for searching jpeg library files" )
	if(NOT JPEG_INCLUDE_DIR OR NOT JPEG_LIBRARY_DIR)
    message(FATAL_ERROR "JPEG library required, please specify it's location.")
  endif()
  set(JPEG_LIBRARY libjpeg)
  link_directories(${JPEG_LIBRARY_DIR})
endif()

include_directories(${JPEG_INCLUDE_DIR})

# png
if(UNIX)
	find_package(PNG REQUIRED)
elseif(WIN32)
	set(PNG_INCLUDE_DIR CACHE PATH "Directories for searching png include files" )
	set(PNG_LIBRARY_DIR CACHE PATH "Directories for searching png library files" )
	if(NOT PNG_INCLUDE_DIR OR NOT PNG_LIBRARY_DIR)
    message(FATAL_ERROR "PNG library required, please specify it's location.")
  endif()
  set(PNG_LIBRARY libpng)
	link_directories(${PNG_LIBRARY_DIR})
endif()

include_directories(${PNG_INCLUDE_DIR})

#zlib
if(WIN32)
	set(ZLIB_INCLUDE_DIR CACHE PATH "Directories for searching zlib include files" )
	set(ZLIB_LIBRARY_DIR CACHE PATH "Directories for searching zlib library files" )
	if(NOT ZLIB_INCLUDE_DIR OR NOT ZLIB_LIBRARY_DIR)
    message(FATAL_ERROR "ZLIB library required, please specify it's location.")
  endif()
  include_directories(${ZLIB_INCLUDE_DIR})
  link_directories(${ZLIB_LIBRARY_DIR})
  set(ZLIB_LIBRARY zlib)
endif()


# omniORB
if(UNIX)
  if(NOT OMNIORB_DIR)
    pkg_check_modules(OMNIORB REQUIRED omniORB4)
    if(OMNIORB_FOUND)
      set(OMNIORB_DIR ${OMNIORB_PREFIX})
      message(STATUS "Found omniORB ${OMNIORB_VERSION} in ${OMNIORB_PREFIX}")
    endif()
  else()
    set(OMNIORB_INCLUDE_DIRS ${OMNIORB_DIR}/include)
    set(OMNIORB_LIBRARY_DIRS ${OMNIORB_DIR}/lib)
  endif()
elseif(WIN32)
  if(NOT OMNIORB_DIR)
    if( NOT $ENV{OMNI_ROOT} STREQUAL "" )
      set(OMNIORB_DIR $ENV{OMNI_ROOT} )
      set(OMNIORB_FOUND TRUE)
    endif()
    set(OMNIORB_DIR ${OMNIORB_DIR} CACHE PATH "The root directory of omniORB")
    if(NOT OMNIORB_FOUND)
      message(FATAL_ERROR "omniORB required, please specify it's location.")
    endif()
  endif()
  if(OMNIORB_DIR)
	  set(OMNIORB_INCLUDE_DIRS ${OMNIORB_DIR}/include)
	  set(OMNIORB_LIBRARY_DIRS ${OMNIORB_DIR}/lib/x86_win32)
	  set(OMNIORB_CFLAGS -D__WIN32__ -D__x86__ )
	  set(OMNIORB_LIBRARIES_RELEASE omniORB412_rt omnithread33_rt omniDynamic412_rt )
	  foreach(library ${OMNIORB_LIBRARIES_RELEASE})
	    list(APPEND OMNIORB_LIBRARIES optimized ${library} debug ${library}d )
	  endforeach()
	endif()
endif()

include_directories(${OMNIORB_INCLUDE_DIRS})
link_directories(${OMNIORB_LIBRARY_DIRS})
add_definitions(${OMNIORB_CFLAGS})

set(OMNIORB_DIR ${OMNIORB_DIR} CACHE PATH "The top directory of omniORB")
set(OMNIORB_CFLAGS ${OMNIORB_CFLAGS} CACHE STRING "Compile flags for omniORB")


# OpenRTM-aist
find_package(OpenRTM REQUIRED)

# Version should be checked here if possible
include_directories(${OPENRTM_INCLUDE_DIRS})
link_directories(${OPENRTM_LIBRARY_DIRS})
if(UNIX)
    add_definitions(${OPENRTM_DEFINITIONS})
endif()


# check Java
if(NOT JDK_DIR)
  find_package(Java)
  if(JAVA_COMPILE)
	get_filename_component(JDK_BIN_DIR ${JAVA_COMPILE} PATH)
	string(REGEX REPLACE "/bin/?" "" JDK_DIR ${JDK_BIN_DIR})
	message(STATUS "Found Java tools in ${JDK_BIN_DIR}")
  endif()
endif()

set(JDK_DIR ${JDK_DIR} CACHE PATH "JDK path")
string(REGEX REPLACE "/$" "" JDK_DIR ${JDK_DIR})

if(NOT JDK_DIR)
  set(JAVAC javac)
  set(IDLJ idlj)
  set(JAVAVM java)
  set(JAR jar)
  set(NATIVE2ASCII native2ascii)
else()
  set(JAVAC ${JDK_DIR}/bin/javac)
  set(IDLJ ${JDK_DIR}/bin/idlj)
  set(JAVAVM ${JDK_DIR}/bin/java)
  set(JAR ${JDK_DIR}/bin/jar)
  set(NATIVE2ASCII ${JDK_DIR}/bin/native2ascii)
endif()

message(STATUS "JDK: ${JDK_DIR}")


# jython
find_package(Jython REQUIRED)
if(JYTHON_JAR)
	set(SYSTEM_CLASSPATH "${SYSTEM_CLASSPATH}:${JYTHON_JAR}")
endif()

set(JAVA3D_LIB_DIR CACHE FILEPATH "set the location of Java3D 'lib' directory when it is not installed in the JDK")

if(JAVA3D_LIB_DIR)
  string(REGEX REPLACE "/$" "" JAVA3D_LIB_DIR ${JAVA3D_LIB_DIR})
  foreach(jar_file j3dcore.jar j3dutils.jar vecmath.jar)
    set(SYSTEM_CLASSPATH "${SYSTEM_CLASSPATH}:${JAVA3D_LIB_DIR}/ext/${jar_file}")
  endforeach()
  set(JAVA_LD_LIBRARY_PATH "${JAVA_LD_LIBRARY_PATH}:${JAVA3D_LIB_DIR}/i386")
endif()

set(JMFHOME $ENV{JMFHOME} CACHE FILEPATH "set the location of Java Media Framework when it is not in the class path")
if(JMFHOME)
  string(REGEX REPLACE "/$" "" JMFHOME ${JMFHOME})
  set(SYSTEM_CLASSPATH "${SYSTEM_CLASSPATH}:${JMFHOME}/lib/jmf.jar")
  set(JAVA_LD_LIBRARY_PATH "${JAVA_LD_LIBRARY_PATH}:${JMFHOME}/lib")
endif()


if(SYSTEM_CLASSPATH)
  string(REGEX REPLACE "^:" "" SYSTEM_CLASSPATH ${SYSTEM_CLASSPATH})
  message(STATUS "Additional Java SYSTEM_CLASSPATH is ${SYSTEM_CLASSPATH}")
  set(CLASSPATH ${SYSTEM_CLASSPATH})
endif()

if(JAVA_LD_LIBRARY_PATH)
  message(STATUS "Additional Java LD_LIBRARY_PATH is ${JAVA_LD_LIBRARY_PATH}")
endif()

find_package(Doxygen)

if(NOT CMAKE_BUILD_TYPE)
  set(
    CMAKE_BUILD_TYPE Release CACHE STRING
    "Choose the type of build, options are: None Debug Release RelWithDebInfo MinSizeRel."
    FORCE)
endif()

option(CHECK_UNRESOLVED_SYMBOLS "check unresolved symbols in the object files when creating shared libraries" ON)
mark_as_advanced(CHECK_UNRESOLVED_SYMBOLS)

if(CHECK_UNRESOLVED_SYMBOLS)
  if(UNIX)
    list(APPEND CMAKE_SHARED_LINKER_FLAGS "-Wl,--unresolved-symbols=ignore-in-shared-libs")
  endif()
endif()

if(NOT CMAKE_BUILD_TYPE STREQUAL Debug)
  add_definitions(-DTVMET_OPTIMIZE)
endif()

include_directories(${OpenHRP_SOURCE_DIR})

# TODO: remove this later
# include_directories(${PROJECT_SOURCE_DIR}/OpenHRP/Corba)

set(LIBRARY_OUTPUT_PATH ${PROJECT_BINARY_DIR}/lib)
set(EXECUTABLE_OUTPUT_PATH ${PROJECT_BINARY_DIR}/bin)
mark_as_advanced(LIBRARY_OUTPUT_PATH EXECUTABLE_OUTPUT_PATH )

set(OPENHRP_IDL_DIR ${OpenHRP_SOURCE_DIR}/idl/OpenHRP)

set(OPENHRP_INSTALL_SUB_DIR OpenHRP-${OpenHRP_MAJOR_VERSION}.${OpenHRP_MINOR_VERSION})
set(RELATIVE_SHARE_INSTALL_PATH share/${OPENHRP_INSTALL_SUB_DIR})
set(RELATIVE_IDL_INSTALL_PATH share/${OPENHRP_INSTALL_SUB_DIR}/idl)
set(RELATIVE_JAVA_INSTALL_PATH share/${OPENHRP_INSTALL_SUB_DIR}/java)
set(RELATIVE_GRXUI_INSTALL_PATH share/${OPENHRP_INSTALL_SUB_DIR}/grxui)
set(RELATIVE_SAMPLE_INSTALL_PATH share/${OPENHRP_INSTALL_SUB_DIR}/sample)
set(RELATIVE_DOC_INSTALL_PATH share/doc/${OPENHRP_INSTALL_SUB_DIR})



add_subdirectory(idl/OpenHRP)
add_subdirectory(OpenHRP)
add_subdirectory(server)
add_subdirectory(java)
add_subdirectory(script)

add_subdirectory(sample)

if(EXISTS ${PROJECT_SOURCE_DIR}/PatternGenerator)
  add_subdirectory(PatternGenerator)
endif()
