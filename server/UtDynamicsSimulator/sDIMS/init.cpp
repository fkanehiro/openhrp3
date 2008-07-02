/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*
 * init.cpp
 * Create: Katsu Yamane, Univ. of Tokyo, 03.06.18
 */

#include "chain.h"

/*
 * initialize
 */
#ifdef SEGA
int Chain::init()
#else
int Chain::init(SceneGraph* sg)
#endif
{
	// reset pointers
	if(all_value) delete[] all_value;
	if(all_value_dot) delete[] all_value_dot;
	if(all_vel) delete[] all_vel;
	if(all_vel_dot) delete[] all_vel_dot;
	if(j_acc_p[0]) delete[] j_acc_p[0];
	if(j_acc_p[1]) delete[] j_acc_p[1];
	if(j_acc_p[2]) delete[] j_acc_p[2];
	if(j_acc_p[3]) delete[] j_acc_p[3];
	if(j_value_dot[0]) delete[] j_value_dot[0];
	if(j_value_dot[1]) delete[] j_value_dot[1];
	if(j_value_dot[2]) delete[] j_value_dot[2];
	if(j_value_dot[3]) delete[] j_value_dot[3];
	if(init_value) delete[] init_value;
	if(init_vel) delete[] init_vel;
	n_value = 0;
	n_dof = 0;
	n_thrust = 0;
	n_joint = 0;
	all_value = 0;
	all_value_dot = 0;
	all_vel = 0;
	all_vel_dot = 0;
	j_acc_p[0] = j_acc_p[1] = j_acc_p[2] = j_acc_p[3] = 0;
	j_value_dot[0] = j_value_dot[1] = j_value_dot[2] = j_value_dot[3] = 0;
	init_value = 0;
	init_vel = 0;
	if(!root) return 0;
	// initialize
#ifndef SEGA
	if(sg) set_relative_positions(sg);
#endif
	root->init();
	if(n_value > 0)
	{
		all_value = new double* [n_value];
		all_value_dot = new double* [n_value];
		j_value_dot[0] = new double [n_value];
		j_value_dot[1] = new double [n_value];
		j_value_dot[2] = new double [n_value];
		j_value_dot[3] = new double [n_value];
		init_value = new double [n_value];
	}
	if(n_dof > 0)
	{
		all_vel = new double* [n_dof];
		all_vel_dot = new double* [n_dof];
		j_acc_p[0] = new double [n_dof];
		j_acc_p[1] = new double [n_dof];
		j_acc_p[2] = new double [n_dof];
		j_acc_p[3] = new double [n_dof];
		init_vel = new double [n_dof];
	}
	root->init_arrays();
	CalcPosition();
	root->init_virtual();
#ifndef SEGA
	if(sg)
	{
		init_scale(sg);
	}
	apply_scale();
#endif
	return 0;
}

#ifndef SEGA
void Chain::init_scale(SceneGraph* sg)
{
	init_scale_sub(sg->getNodes());
}

void Chain::init_scale_sub(Node* node)
{
	if(!node) return;
	char* name = 0;
	if(node->isTransformNode() && (name = node->getName()) && strstr(name, ScaleString))
	{
		char* jointname = name + strlen(ScaleString);
		float scale[3];
		((TransformNode*)node)->getScale(scale);
		scale_object _s;
		_s.set_joint_name(jointname, 0);  // jointname includes character name
		_s.scale = scale[0];
		add_scale_object(_s);
	}
	init_scale_sub(node->next());
	init_scale_sub(node->getChildNodes());
}

void Chain::ApplyGeomScale(SceneGraph* sg)
{
    apply_geom_scale(sg, root);
}

void Chain::apply_geom_scale(SceneGraph* sg, Joint* cur)
{
    if(!cur) return;
    TransformNode* mytrans = sg->findTransformNode(cur->name);
    // mytrans uses new scale to enlarge the geometry
    if(mytrans)
    {
		float new_scale = cur->cur_scale;
//        mytrans->setScale(new_scale, new_scale, new_scale);
    }
    apply_geom_scale(sg, cur->brother);
    apply_geom_scale(sg, cur->child);
}
#endif

void Joint::init()
{
	if(!this) return;
//	if(!realname)
	{
	if(parent) n_root_dof = parent->n_root_dof;
	if(t_given)
	{
		i_value = chain->n_value;
		i_dof = chain->n_dof;
		i_thrust = -1;
		n_root_dof += n_dof;
	}
	else
	{
		i_value = -1;
		i_dof = -1;
		i_thrust = chain->n_thrust;
		n_root_dof += n_thrust;
	}
	i_joint = chain->n_joint;
	chain->n_joint++;
	switch(j_type)
	{
	case JROTATE:
	case JSLIDE:
		if(t_given)
		{
			chain->n_value++;
			chain->n_dof++;
		}
		else
		{
			chain->n_thrust++;
		}
		break;
	case JSPHERE:
		if(t_given)
		{
			chain->n_value += 4; // Euler Parameters
			chain->n_dof += 3;
		}
		else
		{
			chain->n_thrust += 3;
		}
		break;
	case JFREE:
		if(t_given)
		{
			chain->n_value += 7;
			chain->n_dof += 6;
		}
		else
		{
			chain->n_thrust += 6;
		}
		break;
	case JFIXED:
		break;
	default:
		cerr << "warning: joint type not set for " << name << endl;
		break;
	}

	}
	child->init();
	brother->init();
}

void Joint::init_arrays()
{
	if(!this) return;
//	if(!realname)
	{
		
	int i;
	switch(j_type)
	{
	case JROTATE:
	case JSLIDE:
		if(t_given)
		{
			chain->all_value[i_value] = &q;
			chain->all_value_dot[i_value] = &qd;
			chain->all_vel[i_dof] = &qd;
			chain->all_vel_dot[i_dof] = &qdd;
		}
		break;
	case JSPHERE:
		if(t_given)
		{
			for(i=0; i<4; i++)
			{
				chain->all_value[i_value+i] = &rel_ep(i);
				chain->all_value_dot[i_value+i] = &p_ep_dot(i);
			}
			for(i=0; i<3; i++)
			{
				chain->all_vel[i_dof+i] = &p_ang_vel(i);
				chain->all_vel_dot[i_dof+i] = &p_ang_acc(i);
			}
		}
		break;
	case JFREE:
		if(t_given)
		{
			for(i=0; i<3; i++)
			{
				chain->all_value[i_value+i] = &rel_pos(i);
				chain->all_value_dot[i_value+i] = &p_lin_vel(i);
			}
			for(i=0; i<4; i++)
			{
				chain->all_value[i_value+3+i] = &rel_ep(i);
				chain->all_value_dot[i_value+3+i] = &p_ep_dot(i);
			}
			for(i=0; i<3; i++)
			{
				chain->all_vel[i_dof+i] = &p_lin_vel(i);
				chain->all_vel_dot[i_dof+i] = &p_lin_acc(i);
			}
			for(i=0; i<3; i++)
			{
				chain->all_vel[i_dof+3+i] = &p_ang_vel(i);
				chain->all_vel_dot[i_dof+3+i] = &p_ang_acc(i);
			}
		}
		break;
	default:
		break;
	}

	}
	child->init_arrays();
	brother->init_arrays();
}

void Joint::init_virtual()
{
	if(!this) return;
	_init_virtual();
	brother->init_virtual();
	child->init_virtual();
}

void Joint::_init_virtual()
{
	if(!realname) return;
	real = chain->FindJoint(realname);
	if(!real)
	{
		cerr << "warning: could not find real joint " << realname << " of " << name << endl;
		return;
	}
	if(real->real)
	{
		cerr << "error: real joint " << realname << " of " << name << " is also virtual" << endl;
		real = 0;
		return;
	}
	// relative position & orientation in real's frame
	fVec3 pp;
	fMat33 Rt, IR;
	Rt.tran(real->abs_att);
	pp.sub(abs_pos, real->abs_pos);
	rpos_real.mul(Rt, pp);
	ratt_real.mul(Rt, abs_att);
	// set mass, COM, inertia in virtual joint's frame
	mass = real->mass;
	pp.sub(real->loc_com, rpos_real);  // vector from origin to com, in real joint's frame
	loc_com.mul(pp, ratt_real);  // transform to local frame
	IR.mul(real->inertia, ratt_real);
	inertia.mul(tran(ratt_real), IR);
#if 0  // debug
	cerr << "-- " << name << endl;
	cerr << "rpos_real = " << rpos_real << endl;
	cerr << "ratt_real = " << ratt_real << endl;
	cerr << "mass = " << mass << endl;
	cerr << "loc_com = " << loc_com << endl;
	cerr << "inertia = " << inertia << endl;
#endif
}
