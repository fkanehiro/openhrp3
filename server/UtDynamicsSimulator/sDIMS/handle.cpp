/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*
 * handle.cpp
 * Create: Katsu Yamane, 03.07.10
 */

#include "ik.h"

int IKHandle::calc_jacobian()
{
	int i, j, count = 0;
	int n_dof = ik->NumDOF();
	static fMat Jtemp;
	J.resize(n_const, n_dof);
	Jtemp.resize(6, n_dof);
	Jtemp.zero();
	joint->CalcJacobian(Jtemp);
	// compute other_joint's Jacobian and transform to other_joint's
	// local frame
	if(other_joint)
	{
		static fMat Jtemp_other;
		Jtemp_other.resize(6, ik->NumDOF());
		Jtemp_other.zero();
		other_joint->CalcJacobian(Jtemp_other);
		fMat33& abs_att = other_joint->abs_att;
		for(i=0; i<n_dof; i++)
		{
			double* a = Jtemp.data() + 6*i;
			double* b = Jtemp_other.data() + 6*i;
			double m0 = *a - *b;
			double m1 = *(a+1) - *(b+1);
			double m2 = *(a+2) - *(b+2);
			double m3 = *(a+3) - *(b+3);
			double m4 = *(a+4) - *(b+4);
			double m5 = *(a+5) - *(b+5);
			*a = abs_att(0,0)*m0 + abs_att(1,0)*m1 + abs_att(2,0)*m2;
			*(a+1) = abs_att(0,1)*m0 + abs_att(1,1)*m1 + abs_att(2,1)*m2;
			*(a+2) = abs_att(0,2)*m0 + abs_att(1,2)*m1 + abs_att(2,2)*m2;
			*(a+3) = abs_att(0,0)*m3 + abs_att(1,0)*m4 + abs_att(2,0)*m5;
			*(a+4) = abs_att(0,1)*m3 + abs_att(1,1)*m4 + abs_att(2,1)*m5;
			*(a+5) = abs_att(0,2)*m3 + abs_att(1,2)*m4 + abs_att(2,2)*m5;
		}
	}
	// copy to J
	for(i=0; i<6; i++)
	{
		if(const_index[i] == IK::HAVE_CONSTRAINT)
		{
			int a_row = J.row();
			double* a = J.data() + count;
			int b_row = Jtemp.row();
			double* b = Jtemp.data() + i;
			for(j=0; j<n_dof; a+=a_row, b+=b_row, j++)
			{
//				J(count, j) = Jtemp(i, j);
				*a = *b;
			}
			count++;
		}
	}
	return 0;
}

int IKHandle::calc_feedback()
{
	// compute feedback velocity
	if(n_const > 0)
	{
		static fVec3 fb_pos, fb_att;
		static fVec3 cur_pos;
		static fMat33 cur_att;
		// absolute position / orientation
		cur_att.mul(joint->abs_att, rel_att);
		cur_pos.mul(joint->abs_att, rel_pos);
		cur_pos += joint->abs_pos;
		// relative position / orientation wrt other_joint
		if(other_joint)
		{
			static fVec3 pp;
			static fMat33 rt, catt;
			rt.tran(other_joint->abs_att);
			catt.set(cur_att);
			pp.sub(cur_pos, other_joint->abs_pos);
			cur_pos.mul(rt, pp);
			cur_att.mul(rt, catt);
		}
		fb_pos.sub(abs_pos, cur_pos);
		fb_pos *= gain;
		fb_att.rotation(abs_att, cur_att);
		fb_att *= gain;
		int i, count = 0;
		for(i=0; i<3; i++)
		{
			if(const_index[i] == IK::HAVE_CONSTRAINT)
			{
				fb(count) = fb_pos(i);
				count++;
			}
		}
		for(i=0; i<3; i++)
		{
			if(const_index[i+3] == IK::HAVE_CONSTRAINT)
			{
				fb(count) = fb_att(i);
				count++;
			}
		}
	}
	return 0;
}
