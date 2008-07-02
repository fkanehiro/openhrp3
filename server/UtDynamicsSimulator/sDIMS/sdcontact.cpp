/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/**
 * sdcontact.cpp
 * Create: Katsu Yamane, 04.03.28
 */

//#define USE_SLIP_FUNC

#include "sdcontact.h"

#include <fstream>
//static ofstream sd_log("sd.log");
static ofstream sd_log;

int SDContactPair::Update(double timestep, int n_contact, double** coords, double** normals, double* depths)
{
	sd_log << "Update(" << n_contact << ")" << endl;
	in_slip = false;
	if(n_contact > 0)
	{
		int i;
		if(!init_set) set_init();
		for(i=0; i<n_contact; i++)
			update(timestep, n_contact, coords[i], normals[i], depths[i]);
		if(in_slip) init_set = false;  // to reset initial pos/ori
	}
	else
	{
		init_set = false;
	}
	sd_log << "end Update" << endl;
	return 0;
}

int SDContactPair::set_init()
{
	init_set = true;
	Joint* joint1 = joints[0];
	Joint* joint2 = joints[1];
	static fVec3 pp;
	static fMat33 tr;

	pp.sub(joint2->abs_pos, joint1->abs_pos);
	tr.tran(joint1->abs_att);
	init_pos.mul(tr, pp);
	init_att.mul(tr, joint2->abs_att);
	return 0;
}

static double slip_func(double c, double v)
{
	return (1.0 - exp(-c*v));
}

int SDContactPair::update(double timestep, int n_contact, double* coord, double* normal, double depth)
{
	Joint* joint1 = joints[0];
	Joint* joint2 = joints[1];
	static fVec3 relpos1, relpos2;
	static fVec3 relnorm;
	relpos1(0) = coord[0];
	relpos1(1) = coord[1];
	relpos1(2) = coord[2];
	relpos2(0) = coord[0];
	relpos2(1) = coord[1];
	relpos2(2) = coord[2];
	relnorm(0) = normal[0];
	relnorm(1) = normal[1];
	relnorm(2) = normal[2];
	sd_log << "relpos1 = " << relpos1 << endl;
	sd_log << "relnorm = " << relnorm << endl;
	// velocity
	static fVec3 vel, vel1, vel2, force, tmp;
	double d, v, f;
	// joint1
	vel1.cross(joint1->loc_ang_vel, relpos1);
	vel1 += joint1->loc_lin_vel;
	// joint2
	static fVec3 rpos2, pp, lin2, ang2;
	static fMat33 ratt2, tr;
	pp.sub(joint2->abs_pos, joint1->abs_pos);
	tr.tran(joint1->abs_att);
	// pos/ori of joint2 in joint1 frame
	rpos2.mul(tr, pp);
	ratt2.mul(tr, joint2->abs_att);
	// lin/ang velocity of joint2 in joint1 frame
	lin2.mul(ratt2, joint2->loc_lin_vel);
	ang2.mul(ratt2, joint2->loc_ang_vel);
	// vector from joint2 frame to contact point 2, in joint1 frame
	relpos2 -= rpos2;
	// velocity of contact point 2 in joint1 frame
	vel2.cross(ang2, relpos2);
	vel2 += lin2;
	// relative velocity
	vel.sub(vel1, vel2);
	v = relnorm * vel;
	d = depth;
	sd_log << "depth = " << depth << endl;
	sd_log << "vel1 = " << vel1 << endl;
	sd_log << "joint2->loc_lin_vel = " << joint2->loc_lin_vel << endl;
	sd_log << "vel2 = " << vel2 << endl;
	sd_log << "vel = " << vel << endl;
	// average depth during timestep
//	d += 0.5 * v * timestep;
	// normal force applied to joint2, in joint1 frame
#ifdef SD_NONLINEAR
	f = (spring + damper * v) * d;
#else
	f = (spring * d + damper * v);
#endif
	if(f < 0.0) return 0;
	force.mul(f, relnorm);
	//
	// static friction
	//
	// slip info
	static fVec3 slip_vel;
	double abs_slip, slip_func_coef, w;
	slip_vel.mul(v, relnorm);
	slip_vel -= vel;  // slip_vel = vel - v*relnorm
	abs_slip = slip_vel.length();
	slip_func_coef = slip_func_coef_base / timestep;
	w = slip_func(slip_func_coef, abs_slip);
	// initial position (in joint1 frame)
	static fVec3 p_init;
//	p_init.mul(init_att, relpos2);
//	p_init += init_pos;
	p_init.add(init_pos, relpos2);
	// static friction
	static fVec3 tmp_fric;
	tmp_fric.sub(init_pos, relpos2);
	tmp_fric -= rpos2;
	tmp_fric *= slip_p;
	tmp.mul(-slip_d, slip_vel);
	tmp_fric += tmp;
	// tangential force
	double _t = tmp_fric * relnorm;
	tmp.mul(_t, relnorm);
	tmp_fric -= tmp;
//	cerr << "static fric = " << tmp_fric << endl;
	// check
	double abs_fric = tmp_fric.length();
	// slip friction
#ifdef USE_SLIP_FUNC
	if(abs_fric > f*static_fric)
	{
//		cerr << "slip" << endl;
//		cerr << abs_fric << ", " << abs_slip << endl;
		double tiny = 1e-8, _c;
		if(abs_slip > tiny)
			_c = slip_fric * f * w / abs_slip;
		else
			_c = slip_fric * f * slip_func_coef;
		tmp_fric.mul(-_c, slip_vel);
		in_slip = true;  // need? (05.09.14)
	}
#else
	double tiny = 1e-8, _c;
#if 1
	if(abs_slip > tiny)  // slip friction
	{
		_c = slip_fric * f / abs_slip;
		tmp_fric.mul(-_c, slip_vel);
		in_slip = true;  // need? (05.09.14)
	}
	else if(abs_fric > f*static_fric)  // start slipping
	{
		// parallel to the potential static friction
		_c = slip_fric * f / abs_fric;
		fVec3 fric_save(tmp_fric);
		tmp_fric.mul(_c, fric_save);
		in_slip = true;  // need? (05.09.14)
	}
#else
	if(abs_fric > f*static_fric)
	{
		if(abs_slip > tiny)  // slip friction
		{
			_c = slip_fric * f / abs_slip;
			tmp_fric.mul(-_c, slip_vel);
			in_slip = true;  // need? (05.09.14)
		}
		else
		{
			// parallel to the potential static friction
			_c = slip_fric * f / abs_fric;
			fVec3 fric_save(tmp_fric);
			tmp_fric.mul(_c, fric_save);
			in_slip = true;  // need? (05.09.14)
		}
	}
#endif
#endif
	force += tmp_fric;
	// average
	force /= (double)n_contact;
	// apply to joint1
	static fVec3 m;
	joint1->ext_force -= force;
	m.cross(relpos1, force);
	joint1->ext_moment -= m;
	// apply to joint2
//	cerr << "---" << endl;
//	cerr << "depth = " << d << endl;
//	cerr << "force = " << force << endl;
//	cerr << "relpos = " << relpos1 << endl;
//	cerr << "norm = " << relnorm << endl;
	tmp.mul(force, ratt2);
	sd_log << "ext_force = " << tmp << endl;
	joint2->ext_force += tmp;
	m.cross(relpos2, force);
	tmp.mul(m, ratt2);
	joint2->ext_moment += tmp;
	sd_log << "ext_moment = " << tmp << endl;
	return 0;
}

