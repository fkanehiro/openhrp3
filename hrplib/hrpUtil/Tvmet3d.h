/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */

#ifndef HRPUTIL_TVMET3D_H_INCLUDED
#define HRPUTIL_TVMET3D_H_INCLUDED

#include "Eigen3d.h"
#if defined(WIN32) || defined(_WIN32) || defined(__WIN32__) || defined(__NT__)
#pragma message( "Tvmet3d.h is obsolete. Please replace it with Eigen3d.h" )
#else
#warning Tvmet3d.h is obsolete. Please replace it with Eigen3d.h
#endif
#include "Tvmet2Eigen.h"

#endif
