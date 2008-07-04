/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*
 * integ.cpp
 * Create: Katsu Yamane, Univ. of Tokyo, 03.07.11
 */

#include "chain.h"

/**
 * estimate the integration error by comparing x1 and x2, where
 *  (1) x1 = x(t) + xdot(t)dt
 *  (2) x(t+dt/2) = x(t) + xdot(t)dt/2,
 *      x2 = x(t+dt/2) + xdot(t+dt/2)dt/2
 * the actual timestep will be equal to or smaller than the timestep parameter,
 * which will be overwritten by the actual timestep
 */
int Chain::IntegrateAdaptive(double& timestep, int step, double min_timestep, double max_integ_error)
{
	root->pre_integrate();
	int i;
	double half_step = timestep * 0.5;
	if(step == 0)
	{
		// save derivatives at t
		// save initial value/vel and compute value/vel at t+dt
		// set value/vel in the middle
		for(i=0; i<n_value; i++)
		{
			j_value_dot[0][i] = *all_value_dot[i];
			init_value[i] = *all_value[i];
			j_value_dot[1][i] = init_value[i] + j_value_dot[0][i] * timestep;
			*all_value[i] += j_value_dot[0][i] * half_step;
		}
		for(i=0; i<n_dof; i++)
		{
			j_acc_p[0][i] = *all_vel_dot[i];
			init_vel[i] = *all_vel[i];
			j_acc_p[1][i] = init_vel[i] + j_acc_p[0][i] * timestep;
			*all_vel_dot[i] += j_acc_p[0][i] * half_step;
		}
	}
	else if(step == 1)
	{
		// compute and save value/vel after timestep
		double d, value_error = 0.0, vel_error = 0.0, total_error;
		for(i=0; i<n_value; i++)
		{
			d = *all_value[i] + *all_value_dot[i] * half_step - j_value_dot[1][i];
			value_error += d*d;
		}
		for(i=0; i<n_dof; i++)
		{
			d = *all_vel[i] + *all_vel_dot[i] * half_step - j_acc_p[1][i];
			vel_error += d*d;
		}
		double new_timestep;
		// compare x1 and x2
		total_error = sqrt(value_error+vel_error) / (n_value+n_dof);
		// integrate with new timestep
		new_timestep = timestep * sqrt(max_integ_error / total_error);
		if(new_timestep < min_timestep)
			new_timestep = min_timestep;
		if(new_timestep > timestep)
			new_timestep = timestep;
		timestep = new_timestep;
		for(i=0; i<n_value; i++)
		{
			*all_value[i] = init_value[i] + j_value_dot[0][i] * timestep;
		}
		for(i=0; i<n_dof; i++)
		{
			*all_vel[i] = init_vel[i] + j_acc_p[0][i] * timestep;
		}
	}
	root->post_integrate();
	return 0;
}

int Chain::IntegrateValue(double timestep)
{
	root->pre_integrate();
	int i;
	for(i=0; i<n_value; i++)
	{
		*all_value[i] += timestep * *all_value_dot[i];
	}
	root->post_integrate();
	return 0;
}

int Chain::IntegrateVelocity(double timestep)
{
	root->pre_integrate();
	int i;
	for(i=0; i<n_dof; i++)
	{
		*all_vel[i] += timestep * *all_vel_dot[i];
	}
	root->post_integrate();
	return 0;
}

int Chain::Integrate(double timestep)
{
	root->pre_integrate();
	int i;
	for(i=0; i<n_value; i++)
	{
		*all_value[i] += timestep * *all_value_dot[i];
	}
	for(i=0; i<n_dof; i++)
	{
		*all_vel[i] += timestep * *all_vel_dot[i];
	}
	root->post_integrate();
	return 0;
}

int Chain::IntegrateRK4Value(double timestep, int step)
{
	root->pre_integrate();
	int i;
	// save derivatives
	for(i=0; i<n_value; i++)
	{
		j_value_dot[step][i] = *all_value_dot[i];
	}
	// prepare for the next step
	if(step == 0)
	{
		for(i=0; i<n_value; i++)
		{
			init_value[i] = *all_value[i];  // save init value
			*all_value[i] += j_value_dot[0][i] * timestep * 0.5;
		}
	}
	else if(step == 1)
	{
		for(i=0; i<n_value; i++)
		{
			*all_value[i] = init_value[i] + j_value_dot[1][i] * timestep * 0.5;
		}
	}
	else if(step == 2)
	{
		for(i=0; i<n_value; i++)
		{
			*all_value[i] = init_value[i] + j_value_dot[2][i] * timestep;
		}
	}
	else if(step == 3)
	{
		for(i=0; i<n_value; i++)
		{
			*all_value[i] = init_value[i] +
					(j_value_dot[0][i] + 2.0*(j_value_dot[1][i] + j_value_dot[2][i]) + j_value_dot[3][i]) * timestep / 6.0;
		}
	}
	root->post_integrate();
	return 0;
}

int Chain::IntegrateRK4Velocity(double timestep, int step)
{
	root->pre_integrate();
	int i;
	// save derivatives
	for(i=0; i<n_dof; i++)
	{
		j_acc_p[step][i] = *all_vel_dot[i];
	}
	// prepare for the next step
	if(step == 0)
	{
		for(i=0; i<n_dof; i++)
		{
			init_vel[i] = *all_vel[i];  // save init vel
			*all_vel[i] += j_acc_p[0][i] * timestep * 0.5;
		}
	}
	else if(step == 1)
	{
		for(i=0; i<n_dof; i++)
		{
			*all_vel[i] = init_vel[i] + j_acc_p[1][i] * timestep * 0.5;
		}
	}
	else if(step == 2)
	{
		for(i=0; i<n_dof; i++)
		{
			*all_vel[i] = init_vel[i] + j_acc_p[2][i] * timestep;
		}
	}
	else if(step == 3)
	{
		for(i=0; i<n_dof; i++)
		{
			*all_vel[i] = init_vel[i] +
					(j_acc_p[0][i] + 2.0*(j_acc_p[1][i] + j_acc_p[2][i]) + j_acc_p[3][i]) * timestep / 6.0;
		}
	}
	root->post_integrate();
	return 0;
}

int Chain::IntegrateRK4(double timestep, int step)
{
	root->pre_integrate();
	int i;
	double half_step = timestep * 0.5;
	// save derivatives
	for(i=0; i<n_value; i++)
	{
		j_value_dot[step][i] = *all_value_dot[i];
	}
	for(i=0; i<n_dof; i++)
	{
		j_acc_p[step][i] = *all_vel_dot[i];
	}
	// prepare for the next step
	if(step == 0)
	{
		for(i=0; i<n_value; i++)
		{
			init_value[i] = *all_value[i];  // save init value
			*all_value[i] += j_value_dot[0][i] * half_step;
		}
		for(i=0; i<n_dof; i++)
		{
			init_vel[i] = *all_vel[i];  // save init vel
			*all_vel[i] += j_acc_p[0][i] * half_step;
		}
	}
	else if(step == 1)
	{
		for(i=0; i<n_value; i++)
		{
			*all_value[i] = init_value[i] + j_value_dot[1][i] * half_step;
		}
		for(i=0; i<n_dof; i++)
		{
			*all_vel[i] = init_vel[i] + j_acc_p[1][i] * half_step;
		}
	}
	else if(step == 2)
	{
		for(i=0; i<n_value; i++)
		{
			*all_value[i] = init_value[i] + j_value_dot[2][i] * timestep;
		}
		for(i=0; i<n_dof; i++)
		{
			*all_vel[i] = init_vel[i] + j_acc_p[2][i] * timestep;
		}
	}
	else if(step == 3)
	{
		for(i=0; i<n_value; i++)
		{
			*all_value[i] = init_value[i] +
					(j_value_dot[0][i] + 2.0*(j_value_dot[1][i] + j_value_dot[2][i]) + j_value_dot[3][i]) * timestep / 6.0;
		}
		for(i=0; i<n_dof; i++)
		{
			*all_vel[i] = init_vel[i] +
					(j_acc_p[0][i] + 2.0*(j_acc_p[1][i] + j_acc_p[2][i]) + j_acc_p[3][i]) * timestep / 6.0;
		}
	}
	root->post_integrate();
	return 0;
}

int Joint::pre_integrate()
{
	if(!this) return 0;
	// compute p_lin_vel, p_ep_dot, p_ang_vel, p_lin_acc, p_ang_acc
	switch(j_type)
	{
	case JROTATE:
	case JSLIDE:
		break;
	case JSPHERE:
		p_ang_vel.mul(rel_att, rel_ang_vel);
		p_ep_dot.angvel2epdot(rel_ep, rel_ang_vel);
		p_ang_acc.mul(rel_att, rel_ang_acc);
		break;
	case JFREE:
		p_lin_vel.mul(rel_att, rel_lin_vel);
		p_ang_vel.mul(rel_att, rel_ang_vel);
		p_ep_dot.angvel2epdot(rel_ep, rel_ang_vel);
		p_lin_acc.mul(rel_att, rel_lin_acc);
		p_ang_acc.mul(rel_att, rel_ang_acc);
		break;
	default:
		break;
	}
	child->pre_integrate();
	brother->pre_integrate();
	return 0;
}

int Joint::post_integrate()
{
	if(!this) return 0;
	// compute rel_att, rel_lin_vel, rel_ang_vel,
	fMat33 ratt;
	switch(j_type)
	{
	case JROTATE:
	case JSLIDE:
		SetJointValue(q);
		SetJointVel(qd);
		break;
	case JSPHERE:
		rel_ep.unit();
		rel_att.set(rel_ep);
		ratt.tran(rel_att);
		rel_ang_vel.mul(ratt, p_ang_vel);
		break;
	case JFREE:
		rel_ep.unit();
		rel_att.set(rel_ep);
		ratt.tran(rel_att);
		rel_lin_vel.mul(ratt, p_lin_vel);
		rel_ang_vel.mul(ratt, p_ang_vel);
		break;
	default:
		break;
	}
	child->post_integrate();
	brother->post_integrate();
	return 0;
}
