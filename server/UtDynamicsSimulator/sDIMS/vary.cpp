/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/**
 * vary.cpp
 * Create:Katsu Yamane, 04.04.19
 *  functions related to changing joint connectivity
 */

#include "chain.h"

int Chain::SetCharacterTorqueGiven(const char* charname, int _tg)
{
	Joint* r = FindCharacterRoot(charname);
	if(!r) return -1;
	in_create_chain = true;
	clear_data();
	// assumes that this change should not affect the character root
	set_all_torque_given(r->child, _tg);
#ifdef SEGA
	init();
#else
	init(0);
#endif
	in_create_chain = false;
	return 0;
}

int Chain::SetTorqueGiven(Joint* _joint, int _tg)
{
	if(!_joint) return -1;
	if(_joint->t_given == _tg) return 0;
	in_create_chain = true;
	clear_data();
	_joint->t_given = _tg;
	if(_tg)
	{
		_joint->n_dof = _joint->n_thrust;
		_joint->n_thrust = 0;
	}
	else
	{
		_joint->n_thrust = _joint->n_dof;
		_joint->n_dof = 0;
	}
#ifdef SEGA
	init();
#else
	init(0);
#endif
	in_create_chain = false;
	return 0;
}

int Chain::SetAllTorqueGiven(int _tg)
{
	in_create_chain = true;
	clear_data();
	set_all_torque_given(root, _tg);
#ifdef SEGA
	init();
#else
	init(0);
#endif
	in_create_chain = false;
	return 0;
}

void Chain::set_all_torque_given(Joint* cur, int _tg)
{
	if(!cur) return;
	if(cur->t_given != _tg)
	{
		cur->t_given = _tg;
		if(_tg)
		{
			cur->n_dof = cur->n_thrust;
			cur->n_thrust = 0;
		}
		else
		{
			cur->n_thrust = cur->n_dof;
			cur->n_dof = 0;
		}
	}
	set_all_torque_given(cur->brother, _tg);
	set_all_torque_given(cur->child, _tg);
}

int Chain::Connect(Joint* virtual_joint, Joint* parent_joint)
{
	if(!virtual_joint->realname) return -1;
	in_create_chain = true;
	clear_data();
	AddJoint(virtual_joint, parent_joint);
#ifdef SEGA
	init();
#else
	init(0);
#endif
	in_create_chain = false;
	do_connect = true;
	return 0;
}

int Chain::Disconnect(Joint* j)
{
	if(!j->realname) return -1;  // can only disconnect virtual joints
	in_create_chain = true;
	clear_data();
	RemoveJoint(j);
	delete j;
#ifdef SEGA
	init();
#else
	init(0);
#endif
	in_create_chain = false;
	return 0;
}

int Chain::clear_data()
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
	if(root) root->clear_data();
	return 0;
}

void Joint::clear_data()
{
	if(!this) return;
	n_root_dof = 0;
	i_value = -1;
	i_dof = -1;
	i_thrust = -1;
	i_joint = -1;
	brother->clear_data();
	child->clear_data();
}
