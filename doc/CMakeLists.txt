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

configure_file( ${OpenHRP_SOURCE_DIR}/doc/Doxyfile.in ${OpenHRP_SOURCE_DIR}/doc/${doc_dir_name}${DOXY_SFX} )
add_custom_command( OUTPUT ${doc_dir_name}
                    COMMAND doxygen ${doc_dir_name}${DOXY_SFX} )
# japanese section
set(doc_output_language "Japanese")
set(doc_enabled_sections "jp")
set(doc_dir_name ${doc_dir_name}_${doc_enabled_sections})
configure_file( ${OpenHRP_SOURCE_DIR}/doc/Doxyfile.in ${OpenHRP_SOURCE_DIR}/doc/${doc_dir_name}_${doc_enabled_sections}${DOXY_SFX} )
add_custom_command( OUTPUT ${doc_dir_name}_${doc_enabled_sections}
                    COMMAND doxygen ${doc_dir_name}_${doc_enabled_sections}${DOXY_SFX} )
endmacro()



configure_doxygen( "idl" "OpenHRP3 IDL" "../idl/OpenHRP" "*.idl" )
list( APPEND depends_dirs idl idl_jp)

set(lib_doc_input "../server/AistDynamicsSimulator \\\\
                         ../server/CollisionDetector \\\\
                         ../server/ControllerBridge \\\\
                         ../server/ModelLoader \\\\
                         ../server/PathPlanner" )

configure_doxygen( "lib" "OpenHRP3 Library" "${lib_doc_input}" "*.cpp *.h" )
list( APPEND depends_dirs lib lib_jp)

add_custom_command( OUTPUT ut
                    COMMAND doxygen ut${DOXY_SFX} )
list( APPEND depends_dirs ut)

add_custom_target(doc ALL DEPENDS ${depends_dirs})

install( DIRECTORY ${depends_dirs} DESTINATION ${RELATIVE_DOC_INSTALL_PATH})
