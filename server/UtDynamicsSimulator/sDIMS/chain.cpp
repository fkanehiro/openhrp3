/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*
 * chain.cpp
 * Create: Katsu Yamane, Univ. of Tokyo, 03.06.18
 */

#include "chain.h"
#include <string.h>

char* CharName(const char* _name)
{
	char* ret = (char*)strrchr(_name, charname_separator);
	if(ret) return ret+1;
	return 0;
}

int Chain::SaveStatus(fVec& value, fVec& vel, fVec& acc)
{
	value.resize(n_value);
	vel.resize(n_dof);
	acc.resize(n_dof);
	GetJointValue(value);
	GetJointVel(vel);
	GetJointAcc(acc);
	return 0;
}

int Chain::SetStatus(const fVec& values, const fVec& vels, const fVec& accs)
{
	SetJointValue(values);
	SetJointVel(vels);
	SetJointAcc(accs);
	return 0;
}

/*
 * constructors and destructors
 */
Chain::Chain()
{
	root = NULL;
	n_value = 0;
	n_dof = 0;
	n_thrust = 0;
	n_joint = 0;
	in_create_chain = false;
	all_value = 0;
	all_value_dot = 0;
	all_vel = 0;
	all_vel_dot = 0;
	j_acc_p[0] = j_acc_p[1] = j_acc_p[2] = j_acc_p[3] = 0;
	j_value_dot[0] = j_value_dot[1] = j_value_dot[2] = j_value_dot[3] = 0;
	init_value = 0;
	init_vel = 0;
	do_connect = false;
}

Chain::~Chain()
{
	if(root) delete root;
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
#ifndef SEGA
	clear_scale_object_list();
#endif
}

void Chain::Clear()
{
	if(root) delete root;
	root = 0;
	n_value = 0;
	n_dof = 0;
	n_thrust = 0;
	n_joint = 0;
	if(all_value) delete[] all_value;
	all_value = 0;
	if(all_value_dot) delete[] all_value_dot;
	all_value_dot = 0;
	if(all_vel) delete[] all_vel;
	all_vel = 0;
	if(all_vel_dot) delete[] all_vel_dot;
	all_vel_dot = 0;
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
	j_acc_p[0] = j_acc_p[1] = j_acc_p[2] = j_acc_p[3] = 0;
	j_value_dot[0] = j_value_dot[1] = j_value_dot[2] = j_value_dot[3] = 0;
	init_value = 0;
	init_vel = 0;
	in_create_chain = false;
	do_connect = false;
}

Joint::Joint()
{
	name = 0;
	basename = 0;
	realname = 0;
	cur_scale = 1.0;
	clear();
}

Joint::Joint(const char* _name, JointType jt, 
			 const fVec3& rpos, const fMat33& ratt, AxisIndex ai,
			 int _t_given)
{
	name = 0;
	basename = 0;
	realname = 0;
	cur_scale = 1.0;
	clear();
	if(_name)
	{
		name = new char [strlen(_name) + 1];
		strcpy(name, _name);
		char* charname = strrchr(name, charname_separator);
		if(charname) *charname = '\0';
		basename = new char [strlen(name) + 1];
		strcpy(basename, name);
		if(charname) *charname = charname_separator;
	}
	t_given = _t_given;
	switch(jt)
	{
	case JFIXED:
		SetFixedJointType(rpos, ratt);
		break;
	case JROTATE:
		SetRotateJointType(rpos, ratt, ai);
		break;
	case JSLIDE:
		SetSlideJointType(rpos, ratt, ai);
		break;
	case JSPHERE:
		SetSphereJointType(rpos, ratt);
		break;
	case JFREE:
		SetFreeJointType(rpos, ratt);
		break;
	case JUNKNOWN:
		break;
	}
}

Joint::Joint(JointData* jdata, const char* charname)
{
	name = 0;
	basename = 0;
	realname = 0;
	cur_scale = 1.0;
	clear();
	if(jdata->name)
	{
		if(charname)
		{
			name = new char [strlen(jdata->name) + strlen(charname) + 2];
			sprintf(name, "%s%c%s", jdata->name, charname_separator, charname);
			basename = new char [strlen(jdata->name) + 1];
			strcpy(basename, jdata->name);
		}
		else
		{
			name = new char [strlen(jdata->name) + 1];
			strcpy(name, jdata->name);
			// character name included?
			char* _ch = strrchr(name, charname_separator);
			if(_ch) *_ch = '\0';
			basename = new char [strlen(name) + 1];
			strcpy(basename, name);
			if(_ch) *_ch = charname_separator;
		}
	}
	mass = jdata->mass;
	inertia.set(jdata->inertia);
	loc_com.set(jdata->com);
	t_given = jdata->t_given;
	switch(jdata->j_type)
	{
	case JFIXED:
		SetFixedJointType(jdata->rel_pos, jdata->rel_att);
		break;
	case JROTATE:
		SetRotateJointType(jdata->rel_pos, jdata->rel_att, jdata->axis_index);
		break;
	case JSLIDE:
		SetSlideJointType(jdata->rel_pos, jdata->rel_att, jdata->axis_index);
		break;
	case JSPHERE:
		SetSphereJointType(jdata->rel_pos, jdata->rel_att);
		break;
	case JFREE:
		SetFreeJointType(jdata->rel_pos, jdata->rel_att);
		break;
	default:
		break;
	}
}

void Joint::clear()
{
	if(name) delete[] name;
	if(basename) delete[] basename;
	if(realname) delete[] realname;
	name = 0;
	basename = 0;
	realname = 0;
	chain = NULL;
	parent = NULL;
	brother = NULL;
	child = NULL;
	real = NULL;
	rpos_real.zero();
	ratt_real.identity();
	j_type = JUNKNOWN;
	t_given = true;
	n_dof = 0;
	n_thrust = 0;
	n_root_dof = 0;

	q = qd = qdd = 0.0;
	axis.zero();
	init_pos.zero();
	init_att.identity();
	rel_lin_vel.zero();
	rel_ang_vel.zero();
	rel_lin_acc.zero();
	rel_ang_acc.zero();

	rel_pos.zero();
	rel_att.identity();
	mass = 0.0;
	inertia.zero();
	loc_com.zero();
	gear_ratio = 1.0;
	rotor_inertia = 0.0;
	i_value = -1;
	i_dof = -1;
	i_thrust = -1;
	i_joint = -1;

	abs_pos.zero();
	abs_att.identity();
	loc_lin_vel.zero();
	loc_ang_vel.zero();
	loc_lin_acc.zero();
	loc_ang_acc.zero();
	loc_com_acc.zero();
	loc_com_vel.zero();

	p_lin_vel.zero();
	p_ep_dot.zero();
	p_ang_vel.zero();
	p_lin_acc.zero();
	p_ang_acc.zero();

	ext_force.zero();
	ext_moment.zero();
	joint_f.zero();
	joint_n.zero();

	tau=0.0;

}

Joint::~Joint()
{
	if(name) delete[] name;
	if(basename) delete[] basename;
	if(realname) delete[] realname;
	if(brother) delete brother;
	if(child) delete child;
}

void Joint::SetJointData(JointData* jdata, const char* charname)
{
	clear();
	if(jdata->name)
	{
		name = new char [strlen(jdata->name) + 1];
		strcpy(name, jdata->name);
	}
	mass = jdata->mass;
	inertia.set(jdata->inertia);
	loc_com.set(jdata->com);
	t_given = jdata->t_given;
	switch(jdata->j_type)
	{
	case JFIXED:
		SetFixedJointType(jdata->rel_pos, jdata->rel_att);
		break;
	case JROTATE:
		SetRotateJointType(jdata->rel_pos, jdata->rel_att, jdata->axis_index);
		break;
	case JSLIDE:
		SetSlideJointType(jdata->rel_pos, jdata->rel_att, jdata->axis_index);
		break;
	case JSPHERE:
		SetSphereJointType(jdata->rel_pos, jdata->rel_att);
		break;
	case JFREE:
		SetFreeJointType(jdata->rel_pos, jdata->rel_att);
		break;
	default:
		break;
	}
}

/*
 * utilities
 */
int Chain::GetJointNameList(char**& jnames)
{
	jnames = NULL;
	if(in_create_chain)
	{
		cerr << "Chain::GetJointNameList - error: cannot be called between BeginCreateChain() and EndCreateChain()" << endl;
		return -1;
	}
	if(n_joint > 0)
	{
		jnames = new char* [n_joint];
		root->get_joint_name_list(jnames);
	}
	return n_joint;
}

void Joint::get_joint_name_list(char** jnames)
{
	if(i_joint >= 0)
	{
		jnames[i_joint] = new char [strlen(name) + 1];
		strcpy(jnames[i_joint], name);
	}
	child->get_joint_name_list(jnames);
	brother->get_joint_name_list(jnames);
}

int Chain::GetJointList(Joint**& joints)
{
	joints = NULL;
	if(in_create_chain)
	{
		cerr << "Chain::GetJointList - error: cannot be called between BeginCreateChain() and EndCreateChain()" << endl;
		return -1;
	}
	if(n_joint > 0)
	{
		joints = new Joint* [n_joint];
		root->get_joint_list(joints);
	}
	return n_joint;
}

void Joint::get_joint_list(Joint** joints)
{
	if(i_joint >= 0 && !real)
	{
		joints[i_joint] = this;
	}
	child->get_joint_list(joints);
	brother->get_joint_list(joints);
}

Joint* Chain::FindJoint(const char* n, const char* charname)
{
	return root->find_joint(n, charname);
}

Joint* Joint::find_joint(const char* n, const char* charname)
{
	if(charname)
	{
		char* mych = CharName();
		if(mych
		   && !strcmp(basename, n)
		   && !strcmp(mych, charname)) return this;
	}
	else
	{
		if(!strcmp(name, n)) return this;
	}
	Joint* ret;
	if((ret = child->find_joint(n, charname))) return ret;
	if((ret = brother->find_joint(n, charname))) return ret;
	return NULL;
}

Joint* Chain::FindJoint(int _id)
{
	return root->find_joint(_id);
}

Joint* Joint::find_joint(int _id)
{
	if(i_joint == _id) return this;
	Joint* ret;
	if((ret = child->find_joint(_id))) return ret;
	if((ret = brother->find_joint(_id))) return ret;
	return NULL;
}

Joint* Chain::FindCharacterRoot(const char* charname)
{
	if(!root) return 0;
	Joint* j;
	for(j=root->child; j; j=j->brother)
	{
		char* ch = j->CharName();
		if(ch && !strcmp(ch, charname)) return j;
	}
	return 0;
}

int Joint::DescendantDOF()
{
	return child->descendant_dof();
}

int Joint::descendant_dof()
{
	int ret1 = brother->descendant_dof();
	int ret2 = child->descendant_dof();
	return (ret1 + ret2 + n_dof);
}

int Joint::DescendantNumJoints()
{
	return (1+child->descendant_num_joints());
}

int Joint::descendant_num_joints()
{
	int ret1 = brother->descendant_num_joints();
	int ret2 = child->descendant_num_joints();
	return (ret1 + ret2 + 1);
}

int Joint::isDescendant(Joint* target)
{
	return is_descendant(child, target);
}

int Joint::is_descendant(Joint* cur, Joint* target)
{
	if(!cur) return false;
	if(cur == target) return true;
	if(is_descendant(cur->brother, target)) return true;
	if(is_descendant(cur->child, target)) return true;
	return false;
}

int Joint::isAscendant(Joint* target)
{
	Joint* p;
	for(p=this; p; p=p->parent)
	{
		if(p == target) return true;
	}
	return false;
}
