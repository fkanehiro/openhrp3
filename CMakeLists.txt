
# Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
# All rights reserved. This program is made available under the terms of the
# Eclipse Public License v1.0 which accompanies this distribution, and is
# available at http://www.eclipse.org/legal/epl-v10.html
# Contributors:
# National Institute of Advanced Industrial Science and Technology (AIST)
# General Robotix Inc. 

# @author Shin'ichiro Nakaoka


cmake_minimum_required(VERSION 2.4)

if( NOT WIN32)
  if(COMMAND cmake_policy)
    cmake_policy(SET CMP0003 NEW)
  endif(COMMAND cmake_policy)
endif()

project(OpenHRP)

set(OPENHRP_VERSION_MAJOR 3)
set(OPENHRP_VERSION_MINOR 1)
set(OPENHRP_VERSION_MICRO 5)
set(OPENHRP_VERSION_ADD_TAIL )
set(CPACK_DEBIAN_PACKAGE_VERSION_TAIL 0)

set(CMAKE_ALLOW_LOOSE_LOOP_CONSTRUCTS true)
set(CMAKE_MODULE_PATH ${PROJECT_SOURCE_DIR}/cmake_modules/)

include(FindPkgConfig)
include(CheckIncludeFiles)
include(${CMAKE_MODULE_PATH}GetUbuntuVer.cmake)

# Enable SSE options
# include(${CMAKE_MODULE_PATH}FindSSEArchitecture.cmake)

if( CMAKE_VERBOSE_MAKEFILE )
  set(VERBOSE_FLAG -v)
  set(JAVAC_VFLAG -verbose)
endif()

option(ENABLE_INSTALL_RPATH "Enable RPATH setting for installed binary files" OFF)
option(ENABLE_INSTALL_RPATH_TO_SELF "Make the binaries that can work anywhere in the filesystem" OFF)

SET(CMAKE_SKIP_BUILD_RPATH  FALSE)

if(NOT ENABLE_INSTALL_RPATH AND NOT ENABLE_INSTALL_RPATH_TO_SELF)
  SET(CMAKE_BUILD_WITH_INSTALL_RPATH FALSE) 
  SET(CMAKE_INSTALL_RPATH "")
  SET(CMAKE_INSTALL_RPATH_USE_LINK_PATH FALSE)
else()
  SET(CMAKE_BUILD_WITH_INSTALL_RPATH TRUE) 
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
#if(NOT Boost_INCLUDE_DIR AND NOT Boost_LIBRARY_DIRS)
  if(WIN32)
    if( "$ENV{BOOST_ROOT}" STREQUAL "" )
      find_path(
        BOOST_ROOT
        NAMES boost/algorithm boost/bind boost/regex boost/thread boost/numeric
        PATHS $ENV{HOMEDRIVE}/Program Files $ENV{HOMEDRIVE}/
        PATH_SUFFIXES Boost_1_44 Boost-1.41.0 Boost-1.40.0 Boost-1.39.0 Boost-1.38.0 Boost-1.37.0 Boost-1.36.0 Boost-1.35.0
        DOC "The root directory of boost")
    else()
      set(BOOST_ROOT $ENV{BOOST_ROOT} CACHE PATH "The root directory of boost")
    endif()
    find_package(Boost 1.35.0 REQUIRED )
  elseif()
  #find_package(Boost REQUIRED)
  #find_package(Boost 1.35.0 REQUIRED COMPONENTS filesystem signals system)
  #find_package(Boost 1.35.0 REQUIRED)
    find_package(Boost 1.35.0 REQUIRED filesystem signals system regex)
  endif()
#endif()

include_directories(${Boost_INCLUDE_DIRS})
link_directories(${Boost_LIBRARY_DIRS})

# check blas and lapack
find_package(Lapack REQUIRED)

include_directories(${LAPACK_INCLUDE_DIRS})
link_directories(${LAPACK_LIBRARY_DIRS})


# eigen
find_package(Eigen)
include_directories(${EIGEN_INCLUDE_DIR})

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
    if(QNXNTO)
      set(OMNIORB_LIBRARIES ${OMNIORB_LIBRARIES} socket)
    endif(QNXNTO)
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
    include(${CMAKE_MODULE_PATH}GetOmniORBVars.cmake)
    
    if(${OMNIORB_VERSION_NUM} MATCHES "^40")
      set(OMNIORB_CFLAGS -D__WIN32__;-D__x86__;-D__NT__;-D__OSVERSION__=4;-D_CRT_SECURE_NO_DEPRECATE;-D_WIN32_WINNT=0x0400)
    else()
      set(OMNIORB_CFLAGS -D__WIN32__;-D__x86__;-D__NT__;-D__OSVERSION__=4;-D_CRT_SECURE_NO_DEPRECATE;-D_WIN32_WINNT=0x0500;-DRTC_CORBA_CXXMAPPING11)
    endif()
    set(OMNIORB_LIBRARIES_RELEASE omniORB${OMNIORB_VERSION_NUM}_rt omnithread${OMNIORB_THREAD_NUM}_rt omniDynamic${OMNIORB_VERSION_NUM}_rt )
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

if( OPENRTM_VERSION STREQUAL OPENRTM_VERSION100 OR OPENRTM_VERSION STREQUAL OPENRTM_VERSION101 OR OPENRTM_VERSION STREQUAL OPENRTM_VERSION102)
  set(OPENHRP_RTM_IDL_DIR "${OpenHRP_SOURCE_DIR}/idl/OpenRTM/${OPENRTM_VERSION100}")
elseif( OPENRTM_VERSION STREQUAL OPENRTM_VERSION110 )
  set(OPENHRP_RTM_IDL_DIR "${OpenHRP_SOURCE_DIR}/idl/OpenRTM/${OPENRTM_VERSION110}")
else()
  set(OPENHRP_RTM_IDL_DIR "${OpenHRP_SOURCE_DIR}/idl/OpenRTM/${OPENRTM_VERSION}")
endif()
if( OPENRTM_VERSION STREQUAL OPENRTM_VERSION042 )
  set(ADDITIONAL_SYMBOL "OPENRTM_VERSION_042")
endif()

if(UNIX)
  if(OPENRTM_VERSION STREQUAL OPENRTM_VERSION110)
    set(OPENRTM_IDL_DIR "${OPENRTM_DIR}/include/openrtm-1.1/rtm/idl")
  else()
    set(OPENRTM_IDL_DIR "${OPENRTM_DIR}/include/rtm/idl")
  endif()
elseif(WIN32)
  if(OPENRTM_VERSION STREQUAL OPENRTM_VERSION042)
    set(OPENRTM_IDL_DIR "${OPENHRP_RTM_IDL_DIR}")
  else()
    set(OPENRTM_IDL_DIR "${OPENRTM_DIR}/rtm/idl")
  endif()
endif()

add_definitions(${OMNIORB_CFLAGS})

# check ODE
set(ODE_DIR ${ODE_DIR} CACHE PATH "The root directory of ODE")
if(ODE_DIR)
  include_directories(${ODE_DIR}/include)
  if(UNIX)
    link_directories(${ODE_DIR}/lib/ReleaseSingleLib)
  elseif(WIN32)
    link_directories(${ODE_DIR}/lib/ReleaseSingleLib ${ODE_DIR}/lib/DebugSingleLib)
  endif()
endif()

option(COMPILE_JAVA_STUFF "Compile java stuff" ON)
if(COMPILE_JAVA_STUFF)
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
else(COMPILE_JAVA_STUFF)
  message(STATUS "Not support Java")
endif(COMPILE_JAVA_STUFF)


# jython
if(COMPILE_JAVA_STUFF)
  find_package(Jython REQUIRED)
  if(JYTHON_JAR)
    set(SYSTEM_CLASSPATH "${SYSTEM_CLASSPATH}${CPSEP}${JYTHON_JAR}")
  endif()
endif(COMPILE_JAVA_STUFF)

if(SYSTEM_CLASSPATH)
    string(REGEX REPLACE "^${CPSEP}" "" SYSTEM_CLASSPATH "${SYSTEM_CLASSPATH}" )
  message(STATUS "SYSTEM_CLASSPATH: ${SYSTEM_CLASSPATH}")
  set(CLASSPATH ${SYSTEM_CLASSPATH})
endif()

if(JAVA_LD_LIBRARY_PATH)
  message(STATUS "Additional Java LD_LIBRARY_PATH is ${JAVA_LD_LIBRARY_PATH}")
endif()

option(ENABLE_DOXYGEN "Use Doxygen" ON)
if(ENABLE_DOXYGEN)
  find_package(Doxygen)
endif(ENABLE_DOXYGEN)

if(NOT CMAKE_BUILD_TYPE)
  set(
    CMAKE_BUILD_TYPE Release CACHE STRING
    "Choose the type of build, options are: None Debug Release RelWithDebInfo MinSizeRel."
    FORCE)
endif()

set(ADDITIONAL_CXX_FLAGS_RELEASE ${ADDITIONAL_CXX_FLAGS_RELEASE} CACHE STRING "Additional c++ compiler optimization flags")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} ${EXTRA_CXX_FLAGS_RELEASE} ${ADDITIONAL_CXX_FLAGS_RELEASE}")

option(CHECK_UNRESOLVED_SYMBOLS "check unresolved symbols in the object files when creating shared libraries" OFF)
mark_as_advanced(CHECK_UNRESOLVED_SYMBOLS)

if(CHECK_UNRESOLVED_SYMBOLS)
  if(UNIX)
    list(APPEND CMAKE_SHARED_LINKER_FLAGS "-Wl,--unresolved-symbols=ignore-in-shared-libs -Wl,--warn-unresolved-symbols")
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
set(RELATIVE_DOC_INSTALL_PATH share/doc/${OPENHRP_INSTALL_SUB_DIR})

set(OPENHRP_DIR ${CMAKE_INSTALL_PREFIX})
set(OPENHRP_SHARE_DIR ${OPENHRP_DIR}/${RELATIVE_SHARE_INSTALL_PATH})



# package
set(OPENHRP_SHORT_DESCRIPTION "Platform software development of robots and simulations.")
option(ENABLE_CPACK "Enable CPack settings" OFF)

if(ENABLE_CPACK)
  set(CPACK_MODULE_PATH ${PROJECT_SOURCE_DIR}/cpack_modules/)
  set(CPACK_PACKAGE_NAME openhrp3.1)
#  set(RELATIVE_DOC_INSTALL_PATH share/doc/${CPACK_PACKAGE_NAME})
  set(CPACK_PACKAGE_VENDOR "AIST")
  set(CPACK_PACKAGE_CONTACT "Shin'ichiro Nakaoka <s.nakaoka@aist.go.jp>")
  set(CPACK_PACKAGE_DESCRIPTION_FILE "${CMAKE_CURRENT_SOURCE_DIR}/README-en.txt")
  set(CPACK_RESOURCE_FILE_LICENSE "${CMAKE_CURRENT_SOURCE_DIR}/LICENSE-en.txt")
  set(CPACK_PACKAGE_DESCRIPTION_SUMMARY "${OPENHRP_SHORT_DESCRIPTION}
 OpenHRP (Open Architecture Humanoid Robotics Platform) is an integrated 
 simulation platform for mainly a robot and consists of main dynamics 
 algorithm developed by Nakamura Lab, Graduate School of Information 
 Science and Technology Department of Mechano-Informatics in the University 
 of Tokyo, a graphical user interface developed by General Robotix,
 Inc and others by Advanced Industrial Science and Technology (AIST).")
  set(CPACK_PACKAGE_VERSION_MAJOR ${OPENHRP_VERSION_MAJOR})
  set(CPACK_PACKAGE_VERSION_MINOR ${OPENHRP_VERSION_MINOR})
  set(CPACK_PACKAGE_VERSION_PATCH ${OPENHRP_VERSION_MICRO})
  execute_process(
    COMMAND date +%Y%m%d%H%M
    OUTPUT_VARIABLE CPACK_PACKAGE_DATE
    RESULT_VARIABLE RESULT
    OUTPUT_STRIP_TRAILING_WHITESPACE)
  set(CPACK_DEBIAN_PACKAGE_ARCHITECTURE i386)
  set(CPACK_PACKAGE_VERSION ${CPACK_PACKAGE_VERSION_MAJOR}.${CPACK_PACKAGE_VERSION_MINOR}.${CPACK_PACKAGE_VERSION_PATCH}-${CPACK_PACKAGE_DATE})

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
    set(CPACK_PACKAGING_INSTALL_PREFIX "/usr" CACHE PATH "The root directory of the package")
    
    foreach(name ${DEPENDENT_BASENAMES})
      if ( depends_line )
	set(depends_line ${depends_line},${name})
      else()
	set(depends_line ${name})
      endif()
    endforeach()
    
    set(CPACK_GENERATOR DEB)
    if("${CPACK_DEBIAN_PACKAGE_ARCHITECTURE}" STREQUAL "")
	set(CPACK_DEBIAN_PACKAGE_ARCHITECTURE i386)
    else()
      set(CPACK_DEBIAN_PACKAGE_ARCHITECTURE ${CPACK_DEBIAN_PACKAGE_ARCHITECTURE})
    endif()
    set(DEBIAN_PACKAGE_SECTION "development")
    set(DEBIAN_PACKAGE_PRIORITY  "extra")
    set(CPACK_PACKAGE_FILE_NAME "${CPACK_PACKAGE_NAME}-${CPACK_PACKAGE_VERSION}${OPENHRP_VERSION_ADD_TAIL}-ubuntu${UBUNTU_VERSION}-${CPACK_DEBIAN_PACKAGE_ARCHITECTURE}")
    set(CPACK_DEBIAN_PACKAGE_CONTROL_EXTRA ${CPACK_MODULE_PATH}shlibs 
      ${CPACK_MODULE_PATH}preinst ${CPACK_MODULE_PATH}postinst ${CPACK_MODULE_PATH}postrm ${CPACK_MODULE_PATH}changelog)
    set(CPACK_DEBIAN_PACKAGE_DEPENDS "${depends_line}")
    set(CPACK_DEBIAN_PACKAGE_VERSION ${CPACK_PACKAGE_VERSION}-${CPACK_DEBIAN_PACKAGE_VERSION_TAIL})
    #  set(CPACK_STRIP_FILES "bin/MyExecutable")
    #  set(CPACK_SOURCE_STRIP_FILES "")
  endif()
  
  include(CPack)

endif(ENABLE_CPACK)

# collada stuff
option(OPT_COLLADASUPPORT "Enable COLLADA support" ON)
if( OPT_COLLADASUPPORT )
  if(UNIX)
    find_package(COLLADA_DOM 2.3 COMPONENTS 1.5)
  elseif(WIN32)
    find_path(
        COLLADA_DOM_DIR 
        NAMES collada_dom-config.cmake
        PATHS $ENV{HOMEDRIVE}/Program Files $ENV{HOMEDRIVE}/
        PATH_SUFFIXES collada_dom-2.3.1/lib/cmake/collada_dom 
        DOC "The directory containing a CMake configuration file for COLLADA_DOM")
    find_package(COLLADA_DOM 2.3 COMPONENTS 1.5)
  endif()
  if( COLLADA_DOM_FOUND )
    add_definitions(-DOPENHRP_COLLADA_FOUND)
  endif( COLLADA_DOM_FOUND )
endif( OPT_COLLADASUPPORT )

# Subdirectories
add_subdirectory(idl)
add_subdirectory(hrplib)

option(COMPILE_EXECUTABLES "Compile the executable programs, servers and scripts" ON)
if(COMPILE_EXECUTABLES)
  add_subdirectory(server)
  add_subdirectory(script)
endif()

if(COMPILE_JAVA_STUFF)
  add_subdirectory(java)
endif()

option(COMPILE_SAMPLES "Compile sample materials" ON)
if(COMPILE_SAMPLES)
  add_subdirectory(sample)
endif()

if(EXISTS ${PROJECT_SOURCE_DIR}/PatternGenerator)
  add_subdirectory(PatternGenerator)
endif()

include(${CMAKE_MODULE_PATH}ConfigurePkgConfig.cmake)

# Documents
if(UNIX)
  option(ENABLE_DOCUMENTS_GENERATE "Allow generate documents by doxygen." OFF)
  if(ENABLE_DOCUMENTS_GENERATE)
    add_subdirectory(doc)
  endif()
endif(UNIX)

if(ENABLE_CPACK)
  if(GENERATE_DEBIANPACKAGE)
    install( FILES README-ja.txt LICENSE-ja.txt HISTORY-ja.txt README-en.txt LICENSE-en.txt HISTORY-en.txt ${CPACK_MODULE_PATH}copyright
      DESTINATION ${RELATIVE_DOC_INSTALL_PATH} )
    if(EXISTS ${OpenHRP_SOURCE_DIR}/java/plugins)
      install( DIRECTORY ${OpenHRP_SOURCE_DIR}/java/plugins
        DESTINATION ${RELATIVE_JAVA_INSTALL_PATH} 
        FILES_MATCHING
    	PATTERN "*.jar"
    	PATTERN ".svn" EXCLUDE
	)
    endif()
  else()
    install( FILES README-ja.txt LICENSE-ja.txt HISTORY-ja.txt README-en.txt LICENSE-en.txt HISTORY-en.txt
      DESTINATION ${RELATIVE_DOC_INSTALL_PATH} )
  endif(GENERATE_DEBIANPACKAGE)
endif(ENABLE_CPACK)

