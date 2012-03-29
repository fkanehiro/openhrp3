/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */

#ifndef HRPUTIL_TVMET2EIGEN_H_INCLUDED
#define HRPUTIL_TVMET2EIGEN_H_INCLUDED

namespace hrp{
    inline Vector3 cross(const Vector3& v1, const Vector3& v2){
        return v1.cross(v2);
    }
    inline Matrix33 trans(const Matrix33& m) { return m.transpose(); }
    inline double dot(const Vector3& v1, const Vector3& v2) {
        return v1.dot(v2);
    }
    inline double norm2(const Vector3& v) { return v.norm(); }
    inline Vector3 nomralize(const Vector3& v) { return v.normalized(); } 
};

#endif
