
set(sources
  EasyScanner.cpp
  VrmlNodes.cpp
  VrmlParser.cpp
  VrmlWriter.cpp
  UniformedShape.cpp
  CalculateNormal.cpp
  ModelNodeSet.cpp)

# #include <EasyScanner.h> を #include "EasyScanner.h" にして以下はなくす
include_directories(.)

add_library(hrpParser3 SHARED ${sources})

set_target_properties(hrpParser3 PROPERTIES VERSION ${LIBRARY_VERSION} SOVERSION ${LIBRARY_SO_VERSION})

target_link_libraries(hrpParser3 boost_signals ${PNG_LIBRARY} ${JPEG_LIBRARY})

INSTALL(TARGETS hrpParser3 LIBRARY DESTINATION lib)
