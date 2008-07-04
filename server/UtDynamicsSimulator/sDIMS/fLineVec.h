/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*
 * fLineVec.h
 * Create: Katsu Yamane, 03.04.11
 */

#ifndef __F_LINEVEC_H__
#define __F_LINEVEC_H__

#include "fMatrix3.h"

class fLineVec
{
public:
	fLineVec() {
		v_org = 0;
		v_dir = 0;
		temp = 0;
	}
	fLineVec(const fLineVec& v) {
		v_org = v.v_org;
		v_dir = v.v_dir;
		temp = 0;
	}
	fLineVec(const fVec3& v1, const fVec3& v2) {
		v_org = v1;
		v_dir = v2;
		temp = 0;
	}
	fLineVec(double v1, double v2, double v3, double v4, double v5, double v6) {
		v_org(0) = v1;
		v_org(1) = v2;
		v_org(2) = v3;
		v_dir(0) = v4;
		v_dir(1) = v5;
		v_dir(2) = v6;
	}
	~fLineVec() {
	}

	fLineVec operator = (const fLineVec& vec) {
		v_org.set(vec.v_org);
		v_dir.set(vec.v_dir);
		return *this;
	}
	void operator = (double d) {
		v_org = d;
		v_dir = d;
	}

	friend fVec3& Org(fLineVec& v) {
		return v.v_org;
	}
	friend fVec3& Dir(fLineVec& v) {
		return v.v_dir;
	}
	fVec3& Org() {
		return v_org;
	}
	fVec3& Dir() {
		return v_dir;
	}
	const fVec3& Org() const {
		return v_org;
	}
	const fVec3& Dir() const {
		return v_dir;
	}
	
	void Org(fVec3& v) {
		v_org.set(v);
	}
	void Dir(fVec3& v) {
		v_dir.set(v);
	}
	
	void set(const fLineVec& vec) {
		v_org.set(vec.v_org);
		v_dir.set(vec.v_dir);
	}
	void set(const fVec3& _org, const fVec3& _dir) {
		v_org.set(_org);
		v_dir.set(_dir);
	}

	friend ostream& operator << (ostream& ost, fLineVec& v);

	double* pOrg() {
		return v_org.data();
	}
	double* pDir() {
		return v_dir.data();
	}

	/*
	 * computes p = org + t*dir
	 */
	fVec3 position(double t) const;
	void position(double t, fVec3& p) const;

	/*
	 * compute the nearest points and distance
	 * if the lines are parallel, computes the projection of
	 * lv2.Org() onto lv1 and returns -1
	 */	
	friend int intersection(const fLineVec& lv1, const fLineVec& lv2,
							fVec3& c1, fVec3& c2, double& d, double eps=1e-8);

	/*
	 * distance from a point
	 */
	double distance(const fVec3& point, fVec3& pos, double* k = 0);
	
protected:
	fVec3 v_org;
	fVec3 v_dir;
	double temp;
};

#endif
