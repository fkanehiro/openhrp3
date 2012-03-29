/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

#include "Eigen4d.h"
#include "VrmlNodes.h"

using namespace hrp;


void hrp::calcRodrigues(Matrix44& out_R, const Vector3& axis, double q)
{
    const double sth = sin(q);
    const double vth = 1.0 - cos(q);

    double ax = axis(0);
    double ay = axis(1);
    double az = axis(2);

    const double axx = ax*ax*vth;
    const double ayy = ay*ay*vth;
    const double azz = az*az*vth;
    const double axy = ax*ay*vth;
    const double ayz = ay*az*vth;
    const double azx = az*ax*vth;

    ax *= sth;
    ay *= sth;
    az *= sth;

    out_R <<
        1.0 - azz - ayy, -az + axy,       ay + azx,        0.0,
        az + axy,        1.0 - azz - axx, -ax + ayz,       0.0,
        -ay + azx,       ax + ayz,        1.0 - ayy - axx, 0.0,
        0.0,             0.0,             0.0,             1.0;
}

/*!
  @if jp
  transformノードで指定されたrotation,translation,scaleを計算し，4x4行列に代入する。
  計算結果は第2引数に代入する。
  @endif
*/
void hrp::calcTransformMatrix(VrmlTransform* transform, Matrix44& out_T)
{
    Matrix44 R;
    const SFRotation& r = transform->rotation;
    calcRodrigues(R, Vector3(r[0], r[1], r[2]), r[3]);

    const SFVec3f& center = transform->center;

    Matrix44 SR;
    const SFRotation& so = transform->scaleOrientation;
    calcRodrigues(SR, Vector3(so[0], so[1], so[2]), so[3]);

    const SFVec3f& s = transform->scale;

    Matrix44 SinvSR;
    SinvSR <<
        s[0] * SR(0,0), s[0] * SR(1,0), s[0] * SR(2,0), 0.0,
        s[1] * SR(0,1), s[1] * SR(1,1), s[1] * SR(2,1), 0.0,
        s[2] * SR(0,2), s[2] * SR(1,2), s[2] * SR(2,2), 0.0,
        0.0,             0.0,           0.0,            1.0;

    const Vector4 c(center[0], center[1], center[2], 1.0);

    Matrix44 RSR(R * SR);

    out_T = RSR * SinvSR;

    const Vector4 c2(out_T * c);
    for(int i=0; i < 3; ++i){
        out_T(i, 3) -= c2(i);
    }
    
    for(int i=0; i < 3; ++i){
        out_T(i, 3) += transform->translation[i] + center[i];
    }
}
