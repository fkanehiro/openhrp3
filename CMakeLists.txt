
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

set(OPENHRP_VERSION_MAJOR 3)
set(OPENHRP_VERSION_MINOR 1)
set(OPENHRP_VERSION_MICRO 0)
set(OPENHRP_VERSION_ADD_TAIL beta3)

set(CMAKE_ALLOW_LOOSE_LOOP_CONSTRUCTS true)
set(CMAKE_MODULE_PATH ${PROJECT_SOURCE_DIR}/cmake_modules/)

include(FindPkgConfig)
include(CheckIncludeFiles)
include(${CMAKE_MODULE_PATH}GetUbuntuVer.cmake)

if( CMAKE_VERBOSE_MAKEFILE )
  set(VERBOSE_FLAG -v)
  set(JAVAC_VFLAG -verbose)
endif()

option(ENABLE_INSTALL_RPATH "Enable RPATH setting for installed binary files" OFF)
option(ENABLE_INSTALL_RPATH_TO_SELF "Make the binaries that can work anywhere in the filesystem" OFF)

SET(CMAKE_SKIP_BUILD_RPATH  FALSE)
SET(CMAKE_BUILD_WITH_INSTALL_RPATH FALSE) 

if(NOT ENABLE_INSTALL_RPATH AND NOT ENABLE_INSTALL_RPATH_TO_SELF)
  SET(CMAKE_INSTALL_RPATH "")
  SET(CMAKE_INSTALL_RPATH_USE_LINK_PATH FALSE)
else()
  if(ENABLE_INSTALL_RPATH)
    SET(CMAKE_INSTALL_RPATH_USE_LINK_PATH TRUE)
  endif()
  if(ENABLE_INSTALL_RPATH_TO_SELF)
    SET(CMAKE_INSTALL_RPATH "$ORIGIN/../lib")
  endif()
endif()


# commands
if(UNIX)
  set(RMDIR rm -fr)
  set(COPY_COMMAND cp)
  set(CPSEP ":")
elseif(WIN32)
  set(RMDIR rmdir /S/Q)
  set(COPY_COMMAND copy /Y)
  set(CPSEP "\;")
endif()

# shell script file suffix
if(UNIX)
  set(SH_SFX "sh")
elseif(WIN32)
  set(SH_SFX "bat")
endif()

# CPack presetting
if(UNIX)
  option(GENERATE_DEBIANPACKAGE "When generate debian package you set ON." OFF)
endif()

# check "dlfcn.h" for using dlopen() and dlclose()
if(UNIX)
  check_include_files(dlfcn.h HAVE_DLFCN_H)
  if(NOT HAVE_DLFCN_H)
    message(FATAL_ERROR "Could not find dlfcn.h")
  endif()
endif()

# check boost
if(NOT Boost_INCLUDE_DIR AND NOT Boost_LIBRARY_DIRS)
  if(WIN32)
    if( "$ENV{BOOST_ROOT}" STREQUAL "" )
      find_path(
        BOOST_ROOT
        NAMES boost/algorithm boost/bind boost/regex boost/thread boost/numeric
        PATHS $ENV{HOMEDRIVE}/Program Files $ENV{HOMEDRIVE}/
        PATH_SUFFIXES Boost-1.41.0 Boost-1.40.0 Boost-1.39.0 Boost-1.38.0 Boost-1.37.0 Boost-1.36.0 Boost-1.35.0
        DOC "The root directory of boost")
    else()
      set(BOOST_ROOT $ENV{BOOST_ROOT} CACHE PATH "The root directory of boost")
    endif()
  endif()
  find_package(Boost REQUIRED)
  if(Boost_FOUND)
    message(STATUS "Found Boost C++ library in ${Boost_INCLUDE_DIRS}")
  endif()
endif()

include_directories(${Boost_INCLUDE_DIR})
link_directories(${Boost_LIBRARY_DIRS})



# check blas and lapack
find_package(Lapack REQUIRED)

include_directories(${LAPACK_INCLUDE_DIRS})
link_directories(${LAPACK_LIBRARY_DIRS})


# tvmet
find_package(Tvmet REQUIRED)
include_directories(${TVMET_INCLUDE_DIR})
add_definitions(-DTVMET_OPTIMIZE)


# build flag of jpeg and png library
if(UNIX)
  set(PNG_JPEG_BUILD OFF CACHE BOOL "Do you want to build png and jpeg library " )
elseif(WIN32)
  set(PNG_JPEG_BUILD ON CACHE BOOL "Do you want to build png and jpeg library " )
endif()

# jpeg
if(PNG_JPEG_BUILD)
  set(JPEG_INCLUDE_DIR "${PROJECT_SOURCE_DIR}/hrplib/hrpUtil/Jpeg-6b" ) 
  if(UNIX)
    set(JPEG_LIBRARY jpeg)
  elseif(WIN32)
    set(JPEG_LIBRARY libjpeg)
  endif()
else()
  if(UNIX)
    find_package(JPEG REQUIRED)
  elseif(WIN32)
    set(JPEG_INCLUDE_DIR CACHE PATH "Directories for searching jpeg include files" )
    set(JPEG_LIBRARY_DIR CACHE PATH "Directories for searching jpeg library files" )
    if(NOT JPEG_INCLUDE_DIR OR NOT JPEG_LIBRARY_DIR)
      message(FATAL_ERROR "JPEG library required, please specify it's location.")
    endif()
    link_directories(${JPEG_LIBRARY_DIR})
    set(JPEG_LIBRARY libjpeg)
  endif()
endif()
include_directories(${JPEG_INCLUDE_DIR})

# png
if(PNG_JPEG_BUILD)
  set(PNG_INCLUDE_DIR "${PROJECT_SOURCE_DIR}/hrplib/hrpUtil/lpng1232" ) 
  if(UNIX)
    set(PNG_LIBRARY png)
  elseif(WIN32)
    set(PNG_LIBRARY libpng)
  endif()
else()
  if(UNIX)
    find_package(PNG REQUIRED)
  elseif(WIN32)
    set(PNG_INCLUDE_DIR CACHE PATH "Directories for searching png include files" )
    set(PNG_LIBRARY_DIR CACHE PATH "Directories for searching png library files" )
    if(NOT PNG_INCLUDE_DIR OR NOT PNG_LIBRARY_DIR)
      message(FATAL_ERROR "PNG library required, please specify it's location.")
    endif()
    link_directories(${PNG_LIBRARY_DIR})
    set(PNG_LIBRARY libpng)
  endif()
endif()
include_directories(${PNG_INCLUDE_DIR})

#zlib
if(PNG_JPEG_BUILD)
  set(ZLIB_INCLUDE_DIR "${PROJECT_SOURCE_DIR}/hrplib/hrpUtil/zlib123" )
  include_directories(${ZLIB_INCLUDE_DIR})
  set(ZLIB_LIBRARY zlib)
else()
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
    include(${CMAKE_MODULE_PATH}GetOmniORBVars.cmake)
    set(OMNIORB_INCLUDE_DIRS ${OMNIORB_DIR}/include)
    set(OMNIORB_LIBRARY_DIRS ${OMNIORB_DIR}/lib/x86_win32)
    
    set(OMNIORB_CFLAGS -D__WIN32__;-D__x86__;-D__NT__;-D__OSVERSION__=4;-D_CRT_SECURE_NO_DEPRECATE)
    set(OMNIORB_LIBRARIES_RELEASE omniORB${OMNIORB_VERSION_NUM}_rt omnithread33_rt omniDynamic${OMNIORB_VERSION_NUM}_rt )
    foreach(library ${OMNIORB_LIBRARIES_RELEASE})
      list(APPEND OMNIORB_LIBRARIES optimized ${library} debug ${library}d )
    endforeach()
  endif()
endif()

include_directories(${OMNIORB_INCLUDE_DIRS})
link_directories(${OMNIORB_LIBRARY_DIRS})

set(OMNIORB_DIR ${OMNIORB_DIR} CACHE PATH "The top directory of omniORB")
set(OMNIORB_CFLAGS ${OMNIORB_CFLAGS} CACHE STRING "Compile flags for omniORB")


# OpenRTM-aist
find_package(OpenRTM REQUIRED)
set(ADDITIONAL_SYMBOL "")
include_directories(${OPENRTM_INCLUDE_DIRS})
link_directories(${OPENRTM_LIBRARY_DIRS})
if(UNIX)
  add_definitions(${OPENRTM_DEFINITIONS})
endif()

set(OPENHRP_RTM_IDL_DIR "${OpenHRP_SOURCE_DIR}/idl/OpenRTM/${OPENRTM_VERSION}")
if( OPENRTM_VERSION STREQUAL OPENRTM_VERSION042 )
  set(ADDITIONAL_SYMBOL "OPENRTM_VERSION_042")
  if(WIN32)
    list(APPEND OMNIORB_CFLAGS -D_WIN32_WINNT=0x0400)
  endif()
elseif( OPENRTM_VERSION STREQUAL OPENRTM_VERSION100 )
  if(WIN32)
    list(APPEND OMNIORB_CFLAGS -D_WIN32_WINNT=0x0500)
    set(RTM_VER_SPECIFIC_DEF -DRTC_CORBA_CXXMAPPING11)
  endif()
else()
  if(WIN32)
    list(APPEND OMNIORB_CFLAGS -D_WIN32_WINNT=0x0500)
    set(RTM_VER_SPECIFIC_DEF -DRTC_CORBA_CXXMAPPING11)
  endif()
endif()

if(UNIX)
  set(OPENRTM_IDL_DIR "${OPENRTM_DIR}/include/rtm/idl")
elseif(WIN32)
  if(OPENRTM_VERSION STREQUAL OPENRTM_VERSION042)
    set(OPENRTM_IDL_DIR "${OPENHRP_RTM_IDL_DIR}")
  else()
    set(OPENRTM_IDL_DIR "${OPENRTM_DIR}/rtm/idl")
  endif()
endif()

add_definitions(${OMNIORB_CFLAGS} ${RTM_VER_SPECIFIC_DEF})


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
  set(SYSTEM_CLASSPATH "${SYSTEM_CLASSPATH}${CPSEP}${JYTHON_JAR}")
endif()

set(JAVA3D_LIB_DIR CACHE PATH "Set the location of Java3D 'lib' directory when it is not installed in the JDK")

if(JAVA3D_LIB_DIR)
  string(REGEX REPLACE "/$" "" JAVA3D_LIB_DIR ${JAVA3D_LIB_DIR})
  foreach(jar_file j3dcore.jar j3dutils.jar vecmath.jar)
    set(SYSTEM_CLASSPATH "${SYSTEM_CLASSPATH}${CPSEP}${JAVA3D_LIB_DIR}/ext/${jar_file}")
  endforeach()
  if(UNIX)
    set(JAVA_LD_LIBRARY_PATH "${JAVA_LD_LIBRARY_PATH}${CPSEP}${JAVA3D_LIB_DIR}/i386")
  elseif(WIN32)
    set(JAVA_LD_LIBRARY_PATH "${JAVA_LD_LIBRARY_PATH}${CPSEP}${JAVA3D_LIB_DIR}/../bin")
  endif()
endif()

if( NOT $ENV{JMFHOME} )
  set(JMFHOME $ENV{JMFHOME} CACHE PATH "Set the location of Java Media Framework when it is not in the class path")
else()
  if(UNIX)
    set(jmfMidPath /java/JMF2.1.1e/unix)
  elseif(WIN32)
    set(jmfMidPath /java/JMF2.1.1e/Windows)
  endif()
  set(JMFHOME ${PROJECT_SOURCE_DIR}${jmfMidPath} CACHE PATH "Set the location of Java Media Framework when it is not in the class path")
endif()
if(JMFHOME)
  string(REGEX REPLACE "/$" "" JMFHOME ${JMFHOME})
  set(SYSTEM_CLASSPATH "${SYSTEM_CLASSPATH}${CPSEP}${JMFHOME}/lib/jmf.jar")
  set(JAVA_LD_LIBRARY_PATH "${JAVA_LD_LIBRARY_PATH}${CPSEP}${JMFHOME}/lib")
endif()


if(SYSTEM_CLASSPATH)
    string(REGEX REPLACE "^${CPSEP}" "" SYSTEM_CLASSPATH "${SYSTEM_CLASSPATH}" )
  message(STATUS "SYSTEM_CLASSPATH: ${SYSTEM_CLASSPATH}")
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

set(ADDITIONAL_CXX_FLAGS_RELEASE ${ADDITIONAL_CXX_FLAGS_RELEASE} CACHE STRING "Additional c++ compiler optimization flags")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} ${ADDITIONAL_CXX_FLAGS_RELEASE}")

option(CHECK_UNRESOLVED_SYMBOLS "check unresolved symbols in the object files when creating shared libraries" ON)
mark_as_advanced(CHECK_UNRESOLVED_SYMBOLS)

if(CHECK_UNRESOLVED_SYMBOLS)
  if(UNIX)
    list(APPEND CMAKE_SHARED_LINKER_FLAGS "-Wl,--unresolved-symbols=ignore-in-shared-libs")
  endif()
endif()

include_directories(${PROJECT_BINARY_DIR}/hrplib)
include_directories(${PROJECT_SOURCE_DIR}/hrplib)

# TODO: remove this later
# include_directories(${PROJECT_SOURCE_DIR}/OpenHRP/Corba)

set(LIBRARY_OUTPUT_PATH ${PROJECT_BINARY_DIR}/lib)
set(EXECUTABLE_OUTPUT_PATH ${PROJECT_BINARY_DIR}/bin)
mark_as_advanced(LIBRARY_OUTPUT_PATH EXECUTABLE_OUTPUT_PATH )

set(OPENHRP_IDL_DIR ${OpenHRP_SOURCE_DIR}/idl)

set(OPENHRP_INSTALL_SUB_DIR OpenHRP-${OPENHRP_VERSION_MAJOR}.${OPENHRP_VERSION_MINOR})
set(OPENHRP_LIBRARY_VERSION ${OPENHRP_VERSION_MAJOR}.${OPENHRP_VERSION_MINOR})
set(RELATIVE_HEADERS_INSTALL_PATH include/${OPENHRP_INSTALL_SUB_DIR})
set(RELATIVE_SHARE_INSTALL_PATH share/${OPENHRP_INSTALL_SUB_DIR})
set(RELATIVE_IDL_INSTALL_PATH share/${OPENHRP_INSTALL_SUB_DIR}/idl)
set(RELATIVE_JAVA_INSTALL_PATH share/${OPENHRP_INSTALL_SUB_DIR}/java)
set(RELATIVE_SAMPLE_INSTALL_PATH share/${OPENHRP_INSTALL_SUB_DIR}/sample)

set(OPENHRP_DIR ${CMAKE_INSTALL_PREFIX})
set(OPENHRP_SHARE_DIR ${OPENHRP_DIR}/${RELATIVE_SHARE_INSTALL_PATH})

# package
set(CPACK_MODULE_PATH ${PROJECT_SOURCE_DIR}/cpack_modules/)
set(CPACK_PACKAGE_NAME openhrp-aist)
set(RELATIVE_DOC_INSTALL_PATH share/doc/${CPACK_PACKAGE_NAME})
set(CPACK_PACKAGE_VENDOR "AIST")
set(CPACK_PACKAGE_CONTACT "Shin'ichiro Nakaoka <s.nakaoka@aist.go.jp>")
set(CPACK_PACKAGE_DESCRIPTION_FILE "${CMAKE_CURRENT_SOURCE_DIR}/README-en.txt")
set(CPACK_RESOURCE_FILE_LICENSE "${CMAKE_CURRENT_SOURCE_DIR}/LICENSE-en.txt")
set(CPACK_PACKAGE_DESCRIPTION_SUMMARY "Platform software development of robots and simulations.
 OpenHRP (Open Architecture Humanoid Robotics Platform) is an integrated 
 simulation platform for mainly a robot and consists of main dynamics 
 algorithm developed by Nakamura Lab, Graduate School of Information 
 Science and Technology Department of Mechano-Informatics in the University 
 of Tokyo, a graphical user interface developed by General Robotix,
 Inc and others by Advanced Industrial Science and Technology (AIST).")
set(CPACK_PACKAGE_VERSION_MAJOR ${OPENHRP_VERSION_MAJOR})
set(CPACK_PACKAGE_VERSION_MINOR ${OPENHRP_VERSION_MINOR})
set(CPACK_PACKAGE_VERSION_PATCH ${OPENHRP_VERSION_MICRO})

if(WIN32 AND NOT UNIX)
  # There is a bug in NSI that does not handle full unix paths properly. Make
  # sure there is at least one set of four (4) backlasshes.
  set(CPACK_PACKAGE_ICON "${CMake_SOURCE_DIR}/Utilities/Release\\\\InstallIcon.bmp")
  set(CPACK_NSIS_INSTALLED_ICON_NAME "bin\\\\MyExecutable.exe")
  set(CPACK_NSIS_DISPLAY_NAME "${CPACK_PACKAGE_INSTALL_DIRECTORY} My Famous Project")
  set(CPACK_NSIS_HELP_LINK "http:\\\\\\\\www.my-project-home-page.org")
  set(CPACK_NSIS_URL_INFO_ABOUT "http:\\\\\\\\www.my-personal-home-page.com")
  set(CPACK_NSIS_CONTACT "me@my-personal-home-page.com")
  set(CPACK_NSIS_MODIFY_PATH ON)
  # set(CPACK_PACKAGE_EXECUTABLES "MyExecutable" "My Executable")
  set(CPACK_PACKAGE_INSTALL_DIRECTORY "OpenHRP ${OPENHRP_VERSION_MAJOR}.${OPENHRP_VERSION_MINOR}")

else()
  include(${CPACK_MODULE_PATH}DEPENDENT${UBUNTU_VERSION_NUM}_BASENAMES.cmake)
  
  foreach(name ${DEPENDENT_BASENAMES})
    if ( depends_line )
      set(depends_line ${depends_line},${name})
    else()
      set(depends_line ${name})
    endif()
  endforeach()

  set(CPACK_GENERATOR DEB)
  set(CPACK_DEBIAN_PACKAGE_ARCHITECTURE i386)
  set(DEBIAN_PACKAGE_SECTION "development")
  set(DEBIAN_PACKAGE_PRIORITY  "extra")
  set(CPACK_PACKAGE_FILE_NAME "${CPACK_PACKAGE_NAME}-${CPACK_PACKAGE_VERSION_MAJOR}.${CPACK_PACKAGE_VERSION_MINOR}.${CPACK_PACKAGE_VERSION_PATCH}${OPENHRP_VERSION_ADD_TAIL}-ubuntu${UBUNTU_VERSION}-${CPACK_DEBIAN_PACKAGE_ARCHITECTURE}")
  set(CPACK_DEBIAN_PACKAGE_CONTROL_EXTRA ${CPACK_MODULE_PATH}shlibs 
${CPACK_MODULE_PATH}postinst ${CPACK_MODULE_PATH}postrm)
#  set(CPACK_DEBIAN_PACKAGE_ARCHITECTURE amd64)
  set(CPACK_DEBIAN_PACKAGE_DEPENDS "${depends_line}")

#  set(CPACK_STRIP_FILES "bin/MyExecutable")
#  set(CPACK_SOURCE_STRIP_FILES "")
endif()

include(CPack)

# Subdirectories
add_subdirectory(idl)
add_subdirectory(hrplib)

option(COMPILE_EXECUTABLES "Compile the executable programs, servers and scripts" ON)
if(COMPILE_EXECUTABLES)
  add_subdirectory(server)
  add_subdirectory(script)
endif()

option(COMPILE_JAVA_STUFF "Compile java stuff" ON)
if(COMPILE_JAVA_STUFF)
  add_subdirectory(java)
endif()

option(COMPILE_SAMPLES "Compile sample materials" ON)
if(COMPILE_SAMPLES)
  if( OPENRTM_VERSION STREQUAL OPENRTM_VERSION100 )
    add_subdirectory(sample)
  endif()
endif()

if(EXISTS ${PROJECT_SOURCE_DIR}/PatternGenerator)
  add_subdirectory(PatternGenerator)
endif()

# Documents
if(UNIX)
  option(ENABLE_DOCUMENTS_GENERATE "Allow generate documents by doxygen." OFF)
  if(ENABLE_DOCUMENTS_GENERATE)
    add_subdirectory(doc)
  endif()
endif(UNIX)
if(GENERATE_DEBIANPACKAGE)
install( FILES README-ja.txt LICENSE-ja.txt HISTORY-ja.txt README-en.txt LICENSE-en.txt HISTORY-en.txt ${CPACK_MODULE_PATH}copyright
         DESTINATION ${RELATIVE_DOC_INSTALL_PATH} )
else()
install( FILES README-ja.txt LICENSE-ja.txt HISTORY-ja.txt README-en.txt LICENSE-en.txt HISTORY-en.txt
         DESTINATION ${RELATIVE_DOC_INSTALL_PATH} )
endif(GENERATE_DEBIANPACKAGE)
