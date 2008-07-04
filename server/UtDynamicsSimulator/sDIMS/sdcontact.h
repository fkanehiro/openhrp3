/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/**
 * sdcontact.h
 * Create: Katsu Yamane, 04.03.28
 */

#ifndef __SDCONTACT_H__
#define __SDCONTACT_H__

//#include <colinfo.h>
#include "chain.h"
#include <vector>

#define SD_NONLINEAR

class SDContactPair
{
public:
	SDContactPair(Joint* _jnt1, Joint* _jnt2,
				  double _spring = 1e5, double _damper = 10.0,
				  double _static_fric = 0.0, double _slip_fric = 0.0,
				  double _slip_p = 2000.0, double _slip_d = 700.0,
				  double _slip_func_coef_base = 0.1) {
		joints[0] = _jnt1;
		joints[1] = _jnt2;
		spring = _spring;
		damper = _damper;
		static_fric = _static_fric;
		slip_fric = _slip_fric;
		slip_p = _slip_p;
		slip_d = _slip_d;
		slip_func_coef_base = _slip_func_coef_base;
		in_slip = false;
	}
	~SDContactPair() {
	}

	void SetSpring(double _spring) {
		spring = _spring;
	}
	void SetDamper(double _damper) {
		damper = _damper;
	}
	void SetStaticFric(double _static_fric) {
		static_fric = _static_fric;
	}
	void SetSlipFric(double _slip_fric) {
		slip_fric = _slip_fric;
	}
	double StaticFric() {
		return static_fric;
	}
	double SlipFric() {
		return slip_fric;
	}

	Joint* GetJoint(int index) {
		return joints[index];
	}

	void Clear() {
		coords.clear();
		normals.clear();
		depths.clear();
	}

	void AddPoint(double* coord, double* normal, double depth) {
		coords.push_back(fVec3(coord[0], coord[1], coord[2]));
		normals.push_back(fVec3(normal[0], normal[1], normal[2]));
		depths.push_back(depth);
	}

	int NumPoints() {
		return coords.size();
	}
	const fVec3& Coord(int index) {
		return coords[index];
	}
	const fVec3& Normal(int index) {
		return normals[index];
	}
	double Depth(int index) {
		return depths[index];
	}
	
	// compute external forces of links in contact
	int Update(double timestep, int n_contact, double** coords, double** normals, double* depths);

protected:
	int set_init();
	int update(double timestep, int n_contact, double* coord, double* normal, double depth);

	// pair
	Joint* joints[2];

	std::vector<fVec3> coords;
	std::vector<fVec3> normals;
	std::vector<double> depths;

	// spring-damper model parameters
	double spring;
	double damper;
	// friction parameters
	double static_fric;
	double slip_fric;
	double slip_p;
	double slip_d;
	double slip_func_coef_base;
	// initial position/orientation of joint2 in joint1 frame
	int init_set;
	fVec3 init_pos;
	fMat33 init_att;
	// slipping?
	int in_slip;
};

#endif
