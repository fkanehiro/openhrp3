/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */

#ifndef HRPUTIL_TVMET3D_TYPES_H_INCLUDED
#define HRPUTIL_TVMET3D_TYPES_H_INCLUDED

#include "config.h"

//---- needed for preventing a compile error in VC++ ----
#include <iostream>

#ifdef _WIN32
#pragma warning( disable : 4251 4275 4661 )
#undef min
#undef max
#endif

//------------------------------------------------------

#ifdef __QNX__
#include <cmath>
using std::size_t;
using std::sin;
using std::cos;
using std::sqrt;
using std::fabs;
using std::acos;
using std::asin;
using std::atan2;
#endif

#include <hrpUtil/EigenTypes.h>
#warning Tvmet3dTypes.h is obsolete. Please replace it with EigenTypes.h

namespace hrp{
    inline Vector3 cross(const Vector3& v1, const Vector3& v2){
        return v1.cross(v2);
    }
    inline Matrix33 trans(const Matrix33& m) { return m.transpose(); }
    inline double dot(const Vector3& v1, const Vector3& v2) {
        return v1.dot(v2);
    }
    inline double norm2(const Vector3& v) { return v.norm(); }
};

#endif
