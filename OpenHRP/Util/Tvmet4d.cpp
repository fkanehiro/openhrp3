/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */

#include "Tvmet4d.h"

using namespace tvmet;
using namespace OpenHRP;


void OpenHRP::calcRodrigues(Matrix44& out_R, const Vector3& axis, double q)
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

    out_R =
        1.0 - azz - ayy, -az + axy,       ay + azx,        0.0,
        az + axy,        1.0 - azz - axx, -ax + ayz,       0.0,
        -ay + azx,       ax + ayz,        1.0 - ayy - axx, 0.0,
        0.0,             0.0,             0.0,             1.0;
}


void OpenHRP::calcHomogeneousInverse(Matrix44& inv, const Matrix44& m)
{
  using ::std::numeric_limits;

  double det =
      m(0,0)*(m(1,1)*m(2,2)-m(1,2)*m(2,1)) - m(0,1)*(m(1,0)*m(2,2)-m(1,2)*m(2,0)) + m(0,2)*(m(1,0)*m(2,1)-m(1,1)*m(2,0));
  
  if( fabs(det) < numeric_limits<double>::epsilon()){
      throw "Iverse matrix cannot be calculated.";
  }

  inv(0,0) =  (m(1,1)*m(2,2)-m(1,2)*m(2,1)) / det;
  inv(0,1) = -(m(0,1)*m(2,2)-m(0,2)*m(2,1)) / det;
  inv(0,2) =  (m(1,0)*m(2,1)-m(1,1)*m(2,0)) / det;
  inv(1,0) = -(m(1,0)*m(2,2)-m(1,2)*m(2,0)) / det;
  inv(1,1) =  (m(0,0)*m(2,2)-m(0,2)*m(2,0)) / det;
  inv(1,2) = -(m(0,0)*m(1,2)-m(0,2)*m(1,0)) / det;
  inv(2,0) =  (m(0,1)*m(1,2)-m(0,2)*m(1,1)) / det;
  inv(2,1) = -(m(0,0)*m(2,1)-m(0,1)*m(2,0)) / det;
  inv(2,2) =  (m(0,0)*m(1,1)-m(0,1)*m(1,0)) / det;

  inv(3,0) = inv(3,1) = inv(3,2) = 0.0;
  inv(3,3) = 1.0;

  inv(0,3) = -inv(0,0) * m(0,3) - inv(0,1) * m(1,3) - inv(0,2) * m(2,3);
  inv(1,3) = -inv(1,0) * m(0,3) - inv(1,1) * m(1,3) - inv(1,2) * m(2,3);
  inv(2,3) = -inv(2,0) * m(0,3) - inv(2,1) * m(1,3) - inv(2,2) * m(2,3);
}
