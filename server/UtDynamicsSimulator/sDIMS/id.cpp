/*
 * id.cpp
 * Create: Katsu Yamane, Univ. of Tokyo, 03.08.29
 */

#include "chain.h"

void Chain::InvDyn(fVec& tau)
{
	CalcAcceleration();
	root->inv_dyn();
	root->calc_joint_force(tau);
}

void Joint::inv_dyn()
{
	if(!this) return;
	inv_dyn_1();
	child->inv_dyn();
	brother->inv_dyn();
	inv_dyn_2();
	//	cerr << name << ": force = " << joint_f << joint_n << endl;
}

void Joint::inv_dyn_1()
{
	// compute total force around com
	static fVec3 v1, v2;
	total_f.mul(mass, loc_com_acc);
	v1.mul(inertia, loc_ang_vel);
	v2.cross(loc_ang_vel, v1);
	total_n.mul(inertia, loc_ang_acc);
	total_n += v2;

	joint_f.zero();
	joint_n.zero();
}

void Joint::inv_dyn_2()
{
	static fVec3 v1;
	joint_f += total_f;
	joint_n += total_n;
	v1.cross(loc_com, joint_f);
	joint_n += v1;
	// external force/moment
	joint_f -= ext_force;
	joint_n -= ext_moment;
//	cout << name << ": joint_f = " << joint_f << ", joint_n = " << joint_n << endl;
	if(parent)
	{
		// force
		v1.mul(rel_att, joint_f);
		parent->joint_f += v1;
		// moment
		v1.mul(rel_att, joint_n);
		parent->joint_n += v1;
		static fVec3 p, f;
		f.mul(rel_att, joint_f);
		p.sub(parent->loc_com, rel_pos);
		v1.cross(p, f);
		parent->joint_n -= v1;
	}
}

void Joint::calc_joint_force(fVec& tau)
{
	if(!this) return;
	double t;
//	cerr << name << ": force = " << joint_f << joint_n << endl;
	if(i_dof >= 0)
	{
		switch(j_type)
		{
		case JROTATE:
			t = axis * joint_n;
			tau(i_dof) = t;
			break;
		case JSLIDE:
			t = axis * joint_f;
			tau(i_dof) = t;
			break;
		case JSPHERE:
			tau(i_dof+0) = joint_n(0);
			tau(i_dof+1) = joint_n(1);
			tau(i_dof+2) = joint_n(2);
			break;
		case JFREE:
			tau(i_dof+0) = joint_f(0);
			tau(i_dof+1) = joint_f(1);
			tau(i_dof+2) = joint_f(2);
			tau(i_dof+3) = joint_n(0);
			tau(i_dof+4) = joint_n(1);
			tau(i_dof+5) = joint_n(2);
			break;
		default:
			break;
		}
	}
	child->calc_joint_force(tau);
	brother->calc_joint_force(tau);
}

