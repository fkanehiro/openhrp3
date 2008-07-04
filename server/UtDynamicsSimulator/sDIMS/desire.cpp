/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*
 * desire.cpp
 * Create: Katsu Yamane, 03.07.10
 */

#include "ik.h"

int IKDesire::calc_jacobian_rotate(Joint* cur)
{
	if(cur == joint && cur->t_given)
	{
		J(0, cur->i_dof) = 1.0;
	}
	return 0;
}

int IKDesire::calc_jacobian_slide(Joint* cur)
{
	if(cur == joint && cur->t_given)
	{
		J(0, cur->i_dof) = 1.0;
	}
	return 0;
}

int IKDesire::calc_jacobian_sphere(Joint* cur)
{
	if(cur == joint && cur->t_given)
	{
		for(int i=0; i<3; i++)
		{
			J(0, cur->i_dof+i) = cur->rel_att(0, i);
			J(1, cur->i_dof+i) = cur->rel_att(1, i);
			J(2, cur->i_dof+i) = cur->rel_att(2, i);
		}
	}
	return 0;
}

int IKDesire::calc_jacobian_free(Joint* cur)
{
	if(cur == joint && cur->t_given)
	{
		for(int i=0; i<3; i++)
		{
			J(0, cur->i_dof+i) = cur->rel_att(0, i);
			J(1, cur->i_dof+i) = cur->rel_att(1, i);
			J(2, cur->i_dof+i) = cur->rel_att(2, i);
			J(3, cur->i_dof+i+3) = cur->rel_att(0, i);
			J(4, cur->i_dof+i+3) = cur->rel_att(1, i);
			J(5, cur->i_dof+i+3) = cur->rel_att(2, i);
		}
	}
	return 0;
}

int IKDesire::calc_feedback()
{
	double q_cur;
	fVec3 fb_pos, fb_att;
	switch(joint->j_type)
	{
	case JROTATE:
	case JSLIDE:
		joint->GetJointValue(q_cur);
		fb(0) = gain * (q_des - q_cur);
		break;
	case JSPHERE:
		fb_att.rotation(att_des, joint->rel_att);
		fb_att *= gain;
		fb(0) = fb_att(0);
		fb(1) = fb_att(1);
		fb(2) = fb_att(2);
		break;
	case JFREE:
		fb_pos.sub(pos_des, joint->rel_pos);
		fb_pos *= gain;
		fb_att.rotation(att_des, joint->rel_att);
		fb_att *= gain;
		fb(0) = fb_pos(0);
		fb(1) = fb_pos(1);
		fb(2) = fb_pos(2);
		fb(3) = fb_att(0);
		fb(4) = fb_att(1);
		fb(5) = fb_att(2);
		break;
	default:
		break;
	}
	return 0;
}
