
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

# interface version 0.0.0 - version 3.0.0 beta1
set_target_properties(hrpParser3 PROPERTIES VERSION 0.0.0 SOVERSION 0)

target_link_libraries(hrpParser3 boost_signals ${PNG_LIBRARY} ${JPEG_LIBRARY})

INSTALL(TARGETS hrpParser3 LIBRARY DESTINATION lib)
