
set(sources
  EasyScanner.cpp
  VrmlNodes.cpp
  VrmlParser.cpp
  VrmlWriter.cpp
  UniformedShape.cpp
  CalculateNormal.cpp
  ModelNodeSet.cpp)

set(headers
  CalculateNormal.h
  EasyScanner.h
  ModelParserConfig.h
  ModelParserExportDef.h
  UniformedShape.h
  VrmlParser.h
  ModelNodeSet.h
  ModelParserConfigImport.h
  ModelUniformConfig.h
  VrmlNodes.h
  VrmlWriter.h
)


# #include <EasyScanner.h> を #include "EasyScanner.h" にして以下はなくす
include_directories(.)

add_library(hrpParser3 SHARED ${sources})

# interface version 0.0.0 - version 3.0.0 beta1
set_target_properties(hrpParser3 PROPERTIES VERSION 0.0.0 SOVERSION 0)

if(UNIX)
  target_link_libraries(hrpParser3 boost_signals ${PNG_LIBRARY} ${JPEG_LIBRARY})
    
elseif(WIN32)
  target_link_libraries(hrpParser3 ${PNG_LIBRARY} ${JPEG_LIBRARY})

endif() 

if(WIN32)
install(TARGETS hrpParser3
	RUNTIME DESTINATION ${PROJECT_BINARY_DIR}/bin
	LIBRARY DESTINATION ${PROJECT_BINARY_DIR}/lib
	ARCHIVE DESTINATION ${PROJECT_BINARY_DIR}/lib
	CONFIGURATIONS Release )
endif()

install(TARGETS hrpParser3
	RUNTIME DESTINATION bin
	LIBRARY DESTINATION lib
	ARCHIVE DESTINATION lib)

install(FILES ${headers} DESTINATION include/OpenHRP/Parser)
