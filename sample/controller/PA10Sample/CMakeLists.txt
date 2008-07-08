
set(controller PA10Controller)

add_library(${controller} SHARED ${controller}.cpp)
target_link_libraries(${controller} ${OPENRTM_LIBRARIES})
set_target_properties(${controller} PROPERTIES PREFIX "")

add_executable(${controller}Comp ${controller}Comp.cpp ${controller}.cpp)
target_link_libraries(${controller}Comp ${OPENRTM_LIBRARIES})
