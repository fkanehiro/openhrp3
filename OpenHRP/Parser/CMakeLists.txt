
set(hrpParser_sources
  EasyScanner.cpp
  VrmlNodes.cpp
  VrmlParser.cpp
  VrmlWriter.cpp
  UniformedShape.cpp
  CalculateNormal.cpp
  ModelNodeSet.cpp)

# #include <EasyScanner.h> を #include "EasyScanner.h" にして以下はなくす
include_directories(.)

add_library(hrpParser SHARED ${hrpParser_sources})

target_link_libraries(hrpParser boost_signals ${PNG_LIBRARY} ${JPEG_LIBRARY})

INSTALL(TARGETS hrpParser LIBRARY DESTINATION lib)
