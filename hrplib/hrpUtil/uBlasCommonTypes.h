/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

#ifndef OPENHRP_UBLAS_COMMON_TYPES_H_INCLUDED
#define OPENHRP_UBLAS_COMMON_TYPES_H_INCLUDED

#include <hrpUtil/EigenTypes.h>
#if defined(WIN32) || defined(_WIN32) || defined(__WIN32__) || defined(__NT__)
#pragma message( "uBlasCommonTypes.h is obsolete. Please replace it with EigenTypes.h" )
#else
#warning uBlasCommonTypes.h is obsolete. Please replace it with EigenTypes.h
#endif
#endif
