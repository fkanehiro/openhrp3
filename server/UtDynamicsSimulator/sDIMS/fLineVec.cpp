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

#include "fLineVec.h"

ostream& operator << (ostream& ost, fLineVec& v)
{
	ost << v.v_org << v.v_dir << flush;
	return ost;
}

fVec3 fLineVec::position(double t) const
{
	fVec3 p;
	p.mul(v_dir, t);
	p += v_org;
	return p;
}

void fLineVec::position(double t, fVec3& p) const
{
	p.mul(v_dir, t);
	p += v_org;
}

int intersection(const fLineVec& lv1, const fLineVec& lv2,
				 fVec3& c1, fVec3& c2, double& d, double eps)
{
	int parallel = false;
	fVec3 p1(lv1.Org()), d1(lv1.Dir());
	fVec3 p2(lv2.Org()), d2(lv2.Dir());
	double dd11, dd12, dd22;
	double dp11, dp12, dp21, dp22;
	double f, g1, g2;
	double t1, t2;
	dd11 = d1 * d1;
	dd12 = d1 * d2;
	dd22 = d2 * d2;
	dp11 = d1 * p1;
	dp12 = d1 * p2;
	dp21 = d2 * p1;
	dp22 = d2 * p2;
	f = dd11*dd22 - dd12*dd12;
	g1 = dp12 - dp11;
	g2 = dp22 - dp21;
	if(fabs(f) < eps)
	{
		t1 = g1 / dd11;
		t2 = 0.0;
		parallel = true;
	}
	else
	{
		t1 = (dd22*g1 - dd12*g2) / f;
		t2 = (dd12*g1 - dd11*g2) / f;
	}
	lv1.position(t1, c1);
	lv2.position(t2, c2);
	d = dist(c1, c2);
	return parallel;
}

double fLineVec::distance(const fVec3& point, fVec3& pos, double* k)
{
	static fVec3 pp;
	double d2, dp, t;
	pp.sub(point, v_org);
	d2 = v_dir * v_dir;
	dp = v_dir * pp;
	t = dp / d2;
	pos.mul(t, v_dir);
	pos += v_org;
	pp.sub(point, pos);
	if(k) *k = t;
	return pp.length();
}

