
create_simple_controller(PA10Controller)

target_link_libraries(PA10Controller hrpUtil-${OPENHRP_LIBRARY_VERSION} hrpModel-${OPENHRP_LIBRARY_VERSION})
target_link_libraries(PA10ControllerComp hrpUtil-${OPENHRP_LIBRARY_VERSION} hrpModel-${OPENHRP_LIBRARY_VERSION})