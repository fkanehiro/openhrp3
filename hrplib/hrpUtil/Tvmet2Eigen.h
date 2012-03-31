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
    inline Matrix33 VVt_prod(const Vector3& a, const Vector3& b){
        Matrix33 m;
        m << a(0) * b(0), a(0) * b(1), a(0) * b(2),
            a(1) * b(0), a(1) * b(1), a(1) * b(2),
            a(2) * b(0), a(2) * b(1), a(2) * b(2);
        return m;
    }

    void calcInverse(Matrix33& inv, const Matrix33& m){
        bool invertible;
        m.computeInverseWithCheck(inv, invertible);
        if(!invertible){
            throw std::string("Inverse matrix cannot be calculated.");
        }
    }

    inline Matrix33 inverse(const Matrix33& m){
        Matrix33 inv;
        calcInverse(inv, m);
        return inv;
    }
    
    HRP_UTIL_EXPORT inline Matrix33 inverse33(const Matrix33& m) { return inverse(m); }
};

#endif
