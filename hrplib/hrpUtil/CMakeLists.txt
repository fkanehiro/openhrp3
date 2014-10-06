
# Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
# All rights reserved. This program is made available under the terms of the
# Eclipse Public License v1.0 which accompanies this distribution, and is
# available at http://www.eclipse.org/legal/epl-v10.html
# Contributors:
# National Institute of Advanced Industrial Science and Technology (AIST)

# @author Shin'ichiro Nakaoka


if(PNG_JPEG_BUILD)
  ADD_SUBDIRECTORY(lpng1232)
  ADD_SUBDIRECTORY(Jpeg-6b)
  ADD_SUBDIRECTORY(zlib123)
endif()

set(sources
  EasyScanner.cpp
  Eigen3d.cpp
  Eigen4d.cpp
  MatrixSolvers.cpp
  UrlUtil.cpp
  VrmlNodes.cpp
  VrmlParser.cpp
  VrmlWriter.cpp
  TriangleMeshShaper.cpp
  Triangulator.cpp
  ImageConverter.cpp
  OnlineViewerUtil.cpp
)

set(headers
  config.h
  Referenced.h
  EasyScanner.h
  EigenTypes.h
  Tvmet3dTypes.h
  Tvmet3d.h
  Tvmet4d.h
  Tvmet2Eigen.h
  Eigen3d.h
  Eigen4d.h
  uBlasCommonTypes.h
  MatrixSolvers.h
  TimeMeasure.h
  UrlUtil.h
  VrmlNodes.h
  VrmlParser.h
  VrmlWriter.h
  TriangleMeshShaper.h
  ImageConverter.h
  OnlineViewerUtil.h
)

set(target hrpUtil-${OPENHRP_LIBRARY_VERSION})
  
add_library(${target} SHARED ${sources})

# interface version 0.0.0 - version 3.0.0 beta1
set(HRPUTIL_VERSION ${HRPSOVERSION}.0.0 )
set_target_properties(${target} PROPERTIES VERSION ${HRPUTIL_VERSION} SOVERSION ${HRPSOVERSION})

if(UNIX)
  if(NOT QNXNTO)
    target_link_libraries(${target}
      hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}
      #boost_filesystem boost_signals
      ${Boost_FILESYSTEM_LIBRARY} ${Boost_SIGNALS_LIBRARY} ${Boost_SYSTEM_LIBRARY}
      #${Boost_LIBRARIES}
      ${PNG_LIBRARY} ${JPEG_LIBRARY} ${ZLIB_LIBRARY}
      ${OMNIORB_LIBRARIES} ${LAPACK_LIBRARIES}
      )
  else(NOT QNXNTO)
    target_link_libraries(${target}
      hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}
      #boost_filesystem boost_signals
      ${Boost_LIBRARIES}
      ${PNG_LIBRARY} ${JPEG_LIBRARY} ${ZLIB_LIBRARY}
      #${OMNIORB_LIBRARIES}
      ${OPENRTM_LIBRARIES}
      ${LAPACK_LIBRARIES}
      )
  endif(NOT QNXNTO)
  if(APPLE)
  target_link_libraries(${target} boost_system)
  endif() 
elseif(WIN32)
  add_definitions(-DHRP_UTIL_MAKE_DLL)
  set_target_properties(${target} PROPERTIES LINK_FLAGS /NODEFAULTLIB:LIBCMT)
  set_target_properties(${target} PROPERTIES DEBUG_POSTFIX d )

  target_link_libraries(${target}
      optimized hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}
      debug hrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}d
      optimized ${ZLIB_LIBRARY} debug ${ZLIB_LIBRARY}d
      optimized ${PNG_LIBRARY} debug ${PNG_LIBRARY}d
      optimized ${JPEG_LIBRARY} debug ${JPEG_LIBRARY}d
      ${OMNIORB_LIBRARIES} ${LAPACK_LIBRARIES})
endif()

hrplib_install_macro(${target} ${HRPUTIL_VERSION})

install(FILES ${headers} DESTINATION ${RELATIVE_HEADERS_INSTALL_PATH}/hrpUtil)
