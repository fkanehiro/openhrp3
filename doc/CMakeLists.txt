set(DOXY_SFX ".doxyfile")

# configure_doxygen MACRO section
macro( configure_doxygen doc_dir_name doc_project_name
    doc_input doc_file_patterns )
  set(doc_project_name ${doc_project_name})
  set(doc_input ${doc_input})
  set(doc_file_patterns ${doc_file_patterns})
  set(doc_output_language "")
  set(doc_enabled_sections "")
  set(doc_dir_name ${doc_dir_name})

  if(GENERATE_DEBIANPACKAGE)
    set(doc_short_names "YES")
  else()
    set(doc_short_names "NO")
  endif(GENERATE_DEBIANPACKAGE)
  
  list( APPEND depends_dirs ${doc_dir_name})
  file(MAKE_DIRECTORY ${doc_dir_name})
  list( APPEND output_files ${doc_dir_name}/html/index.html)
  configure_file( ${OpenHRP_SOURCE_DIR}/doc/Doxyfile.in ${OpenHRP_SOURCE_DIR}/doc/${doc_dir_name}${DOXY_SFX} )
  add_custom_command( OUTPUT ${doc_dir_name}/html/index.html
    COMMAND doxygen ${doc_dir_name}${DOXY_SFX} 
    WORKING_DIRECTORY ${OpenHRP_SOURCE_DIR}/doc)
  # japanese section
  set(doc_output_language "Japanese")
  set(doc_enabled_sections "jp")
  set(doc_dir_name ${doc_dir_name}_${doc_enabled_sections})
  configure_file( ${OpenHRP_SOURCE_DIR}/doc/Doxyfile.in ${OpenHRP_SOURCE_DIR}/doc/${doc_dir_name}_${doc_enabled_sections}${DOXY_SFX} )
  list( APPEND depends_dirs ${doc_dir_name}_${doc_enabled_sections})
  file(MAKE_DIRECTORY ${doc_dir_name}_${doc_enabled_sections})
  list( APPEND output_files ${doc_dir_name}_${doc_enabled_sections}/html/index.html)
  add_custom_command( OUTPUT ${doc_dir_name}_${doc_enabled_sections}/html/index.html
    COMMAND doxygen ${doc_dir_name}_${doc_enabled_sections}${DOXY_SFX} 
    WORKING_DIRECTORY ${OpenHRP_SOURCE_DIR}/doc)
endmacro()


configure_doxygen( "idl" "OpenHRP3 IDL" "../idl/OpenHRP" "*.idl" )

set(lib_doc_input       "../hrplib/hrpModel \\\\
                         ../hrplib/hrpUtil \\\\
                         ../hrplib/hrpCollision")

configure_doxygen( "lib" "OpenHRP3 Library" "${lib_doc_input}" "*.cpp *.h" )


add_custom_command( OUTPUT ut/html/index.html
                    COMMAND doxygen ut${DOXY_SFX} WORKING_DIRECTORY ${OpenHRP_SOURCE_DIR}/doc)
list( APPEND depends_dirs ut)
file(MAKE_DIRECTORY ut)
list( APPEND output_files ut/html/index.html)

file(MAKE_DIRECTORY doxytag)
list( APPEND depends_dirs doxytag)

add_custom_target(doc ALL DEPENDS ${output_files} WORKING_DIRECTORY ${OpenHRP_SOURCE_DIR}/doc)

install(DIRECTORY ${depends_dirs} DESTINATION ${RELATIVE_DOC_INSTALL_PATH})
