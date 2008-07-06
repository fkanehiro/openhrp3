
add_library(SamplePD SHARED SamplePD.cpp)
target_link_libraries(SamplePD ${OPENRTM_LIBRARIES})
set_target_properties(SamplePD PROPERTIES PREFIX "")

add_executable(SamplePDComp SamplePDComp.cpp SamplePD.cpp)
target_link_libraries(SamplePDComp ${OPENRTM_LIBRARIES})
