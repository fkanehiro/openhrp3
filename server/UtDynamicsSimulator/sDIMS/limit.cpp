/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/**
 * limit.cpp
 *
 */

#include <ik.h>

int IKScalarJointLimit::calc_jacobian_rotate(Joint* cur)
{
	if(cur == joint && cur->j_type == JROTATE && cur->t_given)
	{
		J(0, cur->i_dof) = 1.0;
	}
	return 0;
}

int IKScalarJointLimit::calc_jacobian_slide(Joint* cur)
{
	if(cur == joint && cur->j_type == JSLIDE && cur->t_given)
	{
		J(0, cur->i_dof) = 1.0;
	}
	return 0;
}

int IKScalarJointLimit::calc_feedback()
{
	if(active && joint->n_dof == 1)
	{
		enabled = true;
		double q_cur;
		joint->GetJointValue(q_cur);
		if(min_limit && q_cur < q_min)
		{
//			cerr << joint->name << " is below min (" << q_cur << " < " << q_min << ")" << endl;
			fb(0) = gain * (q_min - q_cur);
		}
		else if(max_limit && q_cur > q_max)
		{
//			cerr << joint->name << " is above max (" << q_cur << " > " << q_max << ")" << endl;
			fb(0) = gain * (q_max - q_cur);
		}
	}
	else
	{
		enabled = false;
	}
	return 0;
}

void IKScalarJointLimit::SetCharacterScale(double _scale, const char* charname)
{
}

