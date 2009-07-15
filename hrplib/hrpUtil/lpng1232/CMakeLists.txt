
# Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
# All rights reserved. This program is made available under the terms of the
# Eclipse Public License v1.0 which accompanies this distribution, and is
# available at http://www.eclipse.org/legal/epl-v10.html
# Contributors:
# National Institute of Advanced Industrial Science and Technology (AIST)
# General Robotix Inc. 

set(sources
  png.c
  pngerror.c
  pngget.c
  pngmem.c
  pngpread.c
  pngread.c
  pngrio.c
  pngrtran.c
  pngrutil.c
  pngset.c
  pngtrans.c
  pngwio.c
  pngwrite.c
  pngwtran.c
  pngwutil.c
  )

set(headers
  png.h
  pngconf.h
)

add_library(${PNG_LIBRARY} STATIC ${sources})

include_directories("../zlib123")
add_definitions(-DPNG_NO_MMX_CODE -D_CRT_SECURE_NO_WARNINGS)

if(WIN32)
  set_target_properties(${PNG_LIBRARY} PROPERTIES COMPILE_FLAGS -Ob1)
  set_target_properties(${PNG_LIBRARY} PROPERTIES DEBUG_POSTFIX d )
endif() 
