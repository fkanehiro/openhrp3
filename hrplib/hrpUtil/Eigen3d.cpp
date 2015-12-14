/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */

#include <iostream>
#include "Eigen3d.h"

using namespace std;
using namespace hrp;


static const double PI = 3.14159265358979323846;


void hrp::calcRodrigues(Matrix33& out_R, const Vector3& axis, double q)
{
    // E + a_hat*sin(q) + a_hat*a_hat*(1-cos(q))
    //
    //    |  0 -az  ay|
    // =E+| az   0 -ax|*s + a_hat*a_hat*v
    //    |-ay  ax   0|
    //
    //    |  0 -az  ay|     |-az*az-ay*ay        ax*ay        az*ax|
    // =E+| az   0 -ax|*s + |       ax*ay -az*az-ax*ax        ay*az|*v
    //    |-ay  ax   0|     |       az*ax        ay*az -ax*ax-ay*ay|
    //
    //  |1-az*az*v-ay*ay*v     -az*s+ax*ay*v      ay*s+az*ax*v|
    // =|     az*s+ax*ay*v 1-az*az*v-ax*ax*v     -ax*s+ay+az*v|
    //  |    -ay*s+az*ax*v      ax*s+ay*az*v 1-ax*ax*v-ay*ay*v|
    //

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

    out_R << 1.0 - azz - ayy, -az + axy,       ay + azx,
            az + axy,        1.0 - azz - axx, -ax + ayz,
            -ay + azx,       ax + ayz,        1.0 - ayy - axx;
}


Vector3 hrp::omegaFromRot(const Matrix33& r)
{
    using ::std::numeric_limits;

    double alpha = (r(0,0) + r(1,1) + r(2,2) - 1.0) / 2.0;

    if (alpha > 1.0) {
        if (alpha > 1.0 + 1.0e-6) {
            cout << scientific << "alpha exceeded the upper limit=" << alpha << endl;
        }
        alpha = 1.0;
    }

    if(fabs(alpha - 1.0) < 1.0e-12) {   //th=0,2PI;
        return Vector3::Zero();

    } else {
        double th = acos(alpha);
        double s = sin(th);

        if (s < numeric_limits<double>::epsilon()) {   //th=PI
            return Vector3( sqrt((r(0,0)+1)*0.5)*th, sqrt((r(1,1)+1)*0.5)*th, sqrt((r(2,2)+1)*0.5)*th );
        }

        double k = -0.5 * th / s;

        return Vector3( (r(1,2) - r(2,1)) * k,
			(r(2,0) - r(0,2)) * k,
			(r(0,1) - r(1,0)) * k );
    }
}


Vector3 hrp::rpyFromRot(const Matrix33& m)
{
    double roll, pitch, yaw;
    
    if ((fabs(m(0,0))<fabs(m(2,0))) && (fabs(m(1,0))<fabs(m(2,0)))) {
	// cos(p) is nearly = 0
	double sp = -m(2,0);
	if (sp < -1.0) {
	    sp = -1;
	} else if (sp > 1.0) {
	    sp = 1;
	}
	pitch = asin(sp); // -pi/2< p < pi/2
	
	roll = atan2(sp*m(0,1)+m(1,2),  // -cp*cp*sr*cy
		     sp*m(0,2)-m(1,1)); // -cp*cp*cr*cy
	
	if (m(0,0)>0.0) { // cy > 0
	    (roll < 0.0) ? (roll += PI) : (roll -= PI);
	}
	double sr=sin(roll), cr=cos(roll);
	if (sp > 0.0) {
	    yaw = atan2(sr*m(1,1)+cr*m(1,2), //sy*sp
			sr*m(0,1)+cr*m(0,2));//cy*sp
	} else {
	    yaw = atan2(-sr*m(1,1)-cr*m(1,2),
			-sr*m(0,1)-cr*m(0,2));
	}
    } else {
	yaw = atan2(m(1,0), m(0,0));
	const double sa = sin(yaw);
	const double ca = cos(yaw);
	pitch = atan2(-m(2,0), ca*m(0,0)+sa*m(1,0));
	roll = atan2(sa*m(0,2)-ca*m(1,2), -sa*m(0,1)+ca*m(1,1));
    }
    return Vector3(roll, pitch, yaw);
}


void hrp::calcRotFromRpy(Matrix33& out_R, double r, double p, double y)
{
    const double cr = cos(r), sr = sin(r), cp = cos(p), sp = sin(p), cy = cos(y), sy = sin(y);
    out_R(0,0)= cp*cy;
    out_R(0,1)= sr*sp*cy - cr*sy;
    out_R(0,2)= cr*sp*cy + sr*sy;
    out_R(1,0)= cp*sy;
    out_R(1,1)= sr*sp*sy + cr*cy;
    out_R(1,2)= cr*sp*sy - sr*cy;
    out_R(2,0)= -sp;
    out_R(2,1)= sr*cp;
    out_R(2,2)= cr*cp;
}

bool hrp::isOrthogonalMatrix(Matrix33& m){
    Matrix33 w(m * m.transpose() - Matrix33::Identity());
    return (abs(w.array())<1.0e-12).all();
    //return all_elements( m * m.transpose() == Matrix33::Identity() );
}

