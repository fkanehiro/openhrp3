#  @author Takafumi Tawara

if(PKG_CONFIG_FOUND)
  set(PACKAGE_NAME "OpenHRP3.1")
  set(OPENHRP_URL "http://www.openrtp.jp/openhrp3/")
  set(PKG_CONF_REQUIRES "")
  set(PKG_CONF_LINK_DEPEND_OPTS "''")
  if(OPENRTM_PKG_CONFIG_FOUND)
    set(PKG_CONF_REQUIRES "openrtm-aist")
  endif(OPENRTM_PKG_CONFIG_FOUND)
  set(PKG_CONF_LINK_SHARED_FILES "-lhrpCollision-${OPENHRP_LIBRARY_VERSION} " 
    "-lhrpModel-${OPENHRP_LIBRARY_VERSION} ")
  if(NOT QNXNTO)
    set(PKG_CONF_LINK_SHARED_FILES ${PKG_CONF_LINK_SHARED_FILES} "-lhrpPlanner-${OPENHRP_LIBRARY_VERSION} ")
  endif()
  set(PKG_CONF_LINK_SHARED_FILES ${PKG_CONF_LINK_SHARED_FILES} "-lhrpUtil-${OPENHRP_LIBRARY_VERSION} ")

  string(REGEX REPLACE ";" ""
     PKG_CONF_LINK_SHARED_FILES ${PKG_CONF_LINK_SHARED_FILES})

  set(PKG_CONF_LINK_STATIC_FILES "-lhrpCorbaStubSkel-${OPENHRP_LIBRARY_VERSION}" )

  set(PKG_CONF_LINK_DEPEND_DIRS)
  if(LAPACK_LIBRARY_DIRS)
    list(APPEND PKG_CONF_LINK_DEPEND_DIRS "-L${LAPACK_LIBRARY_DIRS} ")
  endif()
  if(Boost_LIBRARY_DIRS)
    list(APPEND PKG_CONF_LINK_DEPEND_DIRS "-L${Boost_LIBRARY_DIRS} ")
  endif()
  if(NOT OPENRTM_PKG_CONFIG_FOUND)
    foreach(localDir ${OPENRTM_LIBRARY_DIRS})
      list(APPEND PKG_CONF_LINK_DEPEND_DIRS "-L${localDir} ")
    endforeach()
  endif()
  string(REGEX REPLACE ";" ""
     PKG_CONF_LINK_DEPEND_DIRS ${PKG_CONF_LINK_DEPEND_DIRS})

  set(PKG_CONF_LINK_DEPEND_FILES_TEMP)
  set(PKG_CONF_LINK_DEPEND_FILES)
  foreach(libName ${LAPACK_LIBRARIES})
    list(APPEND PKG_CONF_LINK_DEPEND_FILES_TEMP ${libName})
  endforeach()
  if(NOT QNXNTO)
    #list(APPEND PKG_CONF_LINK_DEPEND_FILES_TEMP boost_filesystem-mt boost_signals-mt boost_program_options-mt boost_regex-mt)
    list(APPEND PKG_CONF_LINK_DEPEND_FILES_TEMP ${Boost_FILESYSTEM_LIBRARY_RELEASE} ${Boost_SIGNALS_LIBRARY_RELEASE} ${Boost_PROGRAM_OPTIONS_LIBRARY_RELEASE} ${Boost_REGEX_LIBRARY_RELEASE})
  else(NOT QNXNTO)
    list(APPEND PKG_CONF_LINK_DEPEND_FILES_TEMP boost_filesystem boost_signals boost_program_options boost_regex)
  endif(NOT QNXNTO)
  if(JPEG_LIBRARY)
    list(APPEND PKG_CONF_LINK_DEPEND_FILES_TEMP ${JPEG_LIBRARY})
  endif()
  if(PNG_LIBRARY)
    list(APPEND PKG_CONF_LINK_DEPEND_FILES_TEMP ${PNG_LIBRARY})
  endif(PNG_LIBRARY)
  if(PNG_JPEG_BUILD)
    if(ZLIB_LIBRARY)
      list(APPEND PKG_CONF_LINK_DEPEND_FILES_TEMP ${ZLIB_LIBRARY})
    endif(ZLIB_LIBRARY)
  else()
    list(APPEND PKG_CONF_LINK_DEPEND_FILES_TEMP z)
  endif(PNG_JPEG_BUILD)
  foreach(name ${PKG_CONF_LINK_DEPEND_FILES_TEMP})
    GET_FILENAME_COMPONENT(localVal ${name} NAME_WE)
    string(REGEX REPLACE "^lib" "" localVal ${localVal})
    list(APPEND PKG_CONF_LINK_DEPEND_FILES "-l${localVal} ")
  endforeach()
  if(NOT OPENRTM_PKG_CONFIG_FOUND)
    set(PKG_CONF_LINK_DEPEND_OPTS "")
    foreach(localFile ${OPENRTM_LIBRARIES})
      if("${localFile}" MATCHES "^-l.+")
        list(APPEND PKG_CONF_LINK_DEPEND_FILES "${localFile} ")
      else()
        list(APPEND PKG_CONF_LINK_DEPEND_OPTS "${localFile} ")
      endif()
    endforeach()
    if("${PKG_CONF_LINK_DEPEND_OPTS}" STREQUAL "")
      set(PKG_CONF_LINK_DEPEND_OPTS "''")
    else()
      string(REGEX REPLACE ";" ""
         PKG_CONF_LINK_DEPEND_OPTS ${PKG_CONF_LINK_DEPEND_OPTS})
    endif()
  endif()
  string(REGEX REPLACE ";" ""
     PKG_CONF_LINK_DEPEND_FILES ${PKG_CONF_LINK_DEPEND_FILES})

  set(PKG_CONF_DEFS "")
  set(PKG_CONF_CXXFLAG_OPTIONS "")
  if(NOT OPENRTM_PKG_CONFIG_FOUND)
    set(PKG_CONF_CXXFLAG_OPTIONS "${OPENRTM_CXX_FLAGS} ${PKG_CONF_CXXFLAG_OPTIONS}")
    string(REGEX REPLACE "\n" ""
       PKG_CONF_CXXFLAG_OPTIONS ${PKG_CONF_CXXFLAG_OPTIONS})
  endif()
  
  configure_file(openhrp3.1.pc.in openhrp3.1.pc @ONLY)
  install(FILES ${CMAKE_CURRENT_BINARY_DIR}/openhrp3.1.pc DESTINATION lib/pkgconfig)
endif(PKG_CONFIG_FOUND)
