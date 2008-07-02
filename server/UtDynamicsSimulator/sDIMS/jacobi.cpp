/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*
 * jacobi.cpp
 * Create: Katsu Yamane, Univ. of Tokyo, 03.10.21
 */

#include <assert.h>
#include "chain.h"

double Chain::ComJacobian(fMat& J, fVec3& com, const char* charname)
{
	double m;
	J.resize(3, n_dof);
	J.zero();
	com.zero();
	m = root->com_jacobian(J, com, charname);
	com /= m;
	J /= m;
	return m;
}

double Joint::com_jacobian(fMat& J, fVec3& com, const char* charname)
{
	if(!this) return 0.0;
	double b_mass, c_mass;
	fVec3 b_com(0,0,0), c_com(0,0,0);
//	cerr << name << ": " << i_chain_dof << endl;
	c_mass = child->com_jacobian(J, c_com, charname);
	// check if this joint should be included
	int is_target = false;
	if(!charname)
	{
		is_target = true;
	}
	else
	{
		char* my_chname = CharName();
		if(my_chname && !strcmp(my_chname, charname))
		{
			is_target = true;
		}
	}
	// add myself
	if(is_target)
	{
		static fVec3 abs_com_pos, my_com;
		abs_com_pos.mul(abs_att, loc_com);
		abs_com_pos += abs_pos;
		c_mass += mass;
		my_com.mul(abs_com_pos, mass);
		c_com += my_com;

		if(n_dof > 0)
		{
			int i;
			// compute Jacobian
			static fVec3 ms; // vector from child com to joint origin, minus s
			static fMat33 msX; // cross matrix of vector ms
			static fVec3 msXaxis;
			ms = c_com - (c_mass * abs_pos);
			msX.cross(ms);
			static fMat33 msXatt;
			msXatt.mul(msX, abs_att);
			msXatt *= -1.0;
			switch(j_type)
			{
			case JROTATE:
				msXaxis.mul(msXatt, axis);
				J(0, i_dof) = msXaxis(0);
				J(1, i_dof) = msXaxis(1);
				J(2, i_dof) = msXaxis(2);
				break;
			case JSLIDE:
			  //				msXaxis.mul(msX, axis);
				msXaxis.mul(abs_att, axis);
				msXaxis *= c_mass;
				J(0, i_dof) = msXaxis(0);
				J(1, i_dof) = msXaxis(1);
				J(2, i_dof) = msXaxis(2);
				break;
			case JSPHERE:
				for(i=0; i<3; i++)
				{
					J(0, i_dof+i) = msXatt(0, i);
					J(1, i_dof+i) = msXatt(1, i);
					J(2, i_dof+i) = msXatt(2, i);
				}
				break;
			case JFREE:
				for(i=0; i<3; i++)
				{
					J(0, i_dof+i) = c_mass * abs_att(0, i);
					J(1, i_dof+i) = c_mass * abs_att(1, i);
					J(2, i_dof+i) = c_mass * abs_att(2, i);
					J(0, i_dof+3+i) = msXatt(0, i);
					J(1, i_dof+3+i) = msXatt(1, i);
					J(2, i_dof+3+i) = msXatt(2, i);
				}
				break;
			}
		}
	}
	b_mass = brother->com_jacobian(J, b_com, charname);
	com.add(c_com, b_com);
	return c_mass + b_mass;
}

int Joint::CalcJacobian(fMat& J)
{
	J.resize(6, chain->n_dof);
	J.zero();
	calc_jacobian(J, this);
	return 0;
}

int Joint::calc_jacobian(fMat& J, Joint* target)
{
	if(!this) return 0;
	switch(j_type)
	{
	case JROTATE:
		if(t_given) calc_jacobian_rotate(J, target);
		break;
	case JSLIDE:
		if(t_given) calc_jacobian_slide(J, target);
		break;
	case JSPHERE:
		if(t_given) calc_jacobian_sphere(J, target);
		break;
	case JFREE:
		if(t_given) calc_jacobian_free(J, target);
		break;
	default:
		break;
	}
	parent->calc_jacobian(J, target);
	return 0;
}

int Joint::calc_jacobian_rotate(fMat& J, Joint* target)
{
	static fVec3 axis0, pp, lin;
	axis0.mul(abs_att, axis);
	pp.sub(target->abs_pos, abs_pos);
	lin.cross(axis0, pp);
	double* a = J.data() + 6*i_dof;
	*a = lin(0);
	*(++a) = lin(1);
	*(++a) = lin(2);
	*(++a) = axis0(0);
	*(++a) = axis0(1);
	*(++a) = axis0(2);
/*	J(0, i_dof) = lin(0);
	J(1, i_dof) = lin(1);
	J(2, i_dof) = lin(2);
	J(3, i_dof) = axis0(0);
	J(4, i_dof) = axis0(1);
	J(5, i_dof) = axis0(2);
*/	return 0;
}

int Joint::calc_jacobian_slide(fMat& J, Joint* target)
{
	double* a = J.data() + 6*i_dof;
	static fVec3 axis0;
	axis0.mul(abs_att, axis);
	*a = axis0(0);
	*(++a) = axis0(1);
	*(++a) = axis0(2);
	*(++a) = 0.0;
	*(++a) = 0.0;
	*(++a) = 0.0;
/*	J(0, i_dof) = axis0(0);
	J(1, i_dof) = axis0(1);
	J(2, i_dof) = axis0(2);
	J(3, i_dof) = 0.0;
	J(4, i_dof) = 0.0;
	J(5, i_dof) = 0.0;
*/	return 0;
}

int Joint::calc_jacobian_sphere(fMat& J, Joint* target)
{
	static fVec3 axis, axis0, pp, lin;
	double* a = J.data() + 6*i_dof;
	axis.zero();
	for(int i=0; i<3; a+=6, i++)
	{
		axis(i) = 1.0;
		axis0.mul(abs_att, axis);
		pp.sub(target->abs_pos, abs_pos);
		lin.cross(axis0, pp);
		*a = lin(0);
		*(a+1) = lin(1);
		*(a+2) = lin(2);
		*(a+3) = axis0(0);
		*(a+4) = axis0(1);
		*(a+5) = axis0(2);
/*		J(0, i_dof+i) = lin(0);
		J(1, i_dof+i) = lin(1);
		J(2, i_dof+i) = lin(2);
		J(3, i_dof+i) = axis0(0);
		J(4, i_dof+i) = axis0(1);
		J(5, i_dof+i) = axis0(2);
*/		axis(i) = 0.0;
	}
	return 0;
}

int Joint::calc_jacobian_free(fMat& J, Joint* target)
{
	double* a = J.data() + 6*i_dof;
	static fVec3 axis, axis0, pp, lin;
	int i;
	axis.zero();
	for(i=0; i<3; a+=6, i++)
	{
		axis(i) = 1.0;
		axis0.mul(abs_att, axis);
		pp.sub(target->abs_pos, abs_pos);
		lin.cross(axis0, pp);
		*a = axis0(0);
		*(a+1) = axis0(1);
		*(a+2) = axis0(2);
		*(a+3) = 0.0;
		*(a+4) = 0.0;
		*(a+5) = 0.0;
		*(a+18) = lin(0);
		*(a+19) = lin(1);
		*(a+20) = lin(2);
		*(a+21) = axis0(0);
		*(a+22) = axis0(1);
		*(a+23) = axis0(2);
/*		J(0, i_dof+i) = axis0(0);
		J(1, i_dof+i) = axis0(1);
		J(2, i_dof+i) = axis0(2);
		J(3, i_dof+i) = 0.0;
		J(4, i_dof+i) = 0.0;
		J(5, i_dof+i) = 0.0;
		J(0, i_dof+3+i) = lin(0);
		J(1, i_dof+3+i) = lin(1);
		J(2, i_dof+3+i) = lin(2);
		J(3, i_dof+3+i) = axis0(0);
		J(4, i_dof+3+i) = axis0(1);
		J(5, i_dof+3+i) = axis0(2);
*/		axis(i) = 0.0;
	}
	return 0;
}

int Joint::CalcJdot(fVec& jdot)
{
	static fVec save_acc, zero_acc;
	static fVec3 space_acc;
	save_acc.resize(chain->n_dof);
	zero_acc.resize(chain->n_dof);
	jdot.resize(6);
	zero_acc.zero();

	// save gravity and joint accelerations
	space_acc.set(chain->root->loc_lin_acc);
	chain->GetJointAcc(save_acc);
	// clear gravity and joint accelerations
	chain->root->loc_lin_acc.zero();
	chain->SetJointAcc(zero_acc);
	// update acceleration
	chain->CalcAcceleration();
	// jdot obtained
	jdot(0) = loc_lin_acc(0);
	jdot(1) = loc_lin_acc(1);
	jdot(2) = loc_lin_acc(2);
	jdot(3) = loc_ang_acc(0);
	jdot(4) = loc_ang_acc(1);
	jdot(5) = loc_ang_acc(2);
	// restore original accelerations
	chain->root->loc_lin_acc.set(space_acc);
	chain->SetJointAcc(save_acc);
	chain->CalcAcceleration();
	return 0;
}

/*
 * 2nd-order derivatives
 */
// J: array of n_dof fMat's
int Joint::CalcJacobian2(fMat* J)
{
	int i;
	for(i=0; i<chain->n_dof; i++)
	{
		J[i].resize(6, chain->n_dof);
		J[i].zero();
	}
	calc_jacobian_2(J, this);
	return 0;
}

// first layer
// compute J[*](1...6, i_dof)
int Joint::calc_jacobian_2(fMat* J, Joint* target)
{
	if(!this) return 0;
	if(j_type == JROTATE)
	{
		target->calc_jacobian_2_rotate_sub(J, target, this);
	}
	else if(j_type == JSLIDE)
	{
		target->calc_jacobian_2_slide_sub(J, target, this);
	}
	else if(j_type == JSPHERE)
	{
		target->calc_jacobian_2_sphere_sub(J, target, this);
	}
	else if(j_type == JFREE)
	{
		target->calc_jacobian_2_free_sub(J, target, this);
	}
	parent->calc_jacobian_2(J, target);
	return 0;
}

// main computation
// compute J[i_dof](1...6, j1->i_dof)
int Joint::calc_jacobian_2_rotate_sub(fMat* J, Joint* target, Joint* j1)
{
	if(!this) return 0;
	assert(j1->j_type == JROTATE);
	static fVec3 x(1, 0, 0), y(0, 1, 0), z(0, 0, 1);
	if(j_type == JROTATE)
	{
		calc_jacobian_2_rotate_rotate(J, target, j1, j1->axis, j1->i_dof, axis, i_dof);
	}
	else if(j_type == JSLIDE)
	{
		calc_jacobian_2_rotate_slide(J, target, j1, j1->axis, j1->i_dof, axis, i_dof);
	}
	else if(j_type == JSPHERE)
	{
		calc_jacobian_2_rotate_rotate(J, target, j1, j1->axis, j1->i_dof, x, i_dof);
		calc_jacobian_2_rotate_rotate(J, target, j1, j1->axis, j1->i_dof, y, i_dof+1);
		calc_jacobian_2_rotate_rotate(J, target, j1, j1->axis, j1->i_dof, z, i_dof+2);
	}
	else if(j_type == JFREE)
	{
		calc_jacobian_2_rotate_slide(J, target, j1, j1->axis, j1->i_dof, x, i_dof);
		calc_jacobian_2_rotate_slide(J, target, j1, j1->axis, j1->i_dof, y, i_dof+1);
		calc_jacobian_2_rotate_slide(J, target, j1, j1->axis, j1->i_dof, z, i_dof+2);
		calc_jacobian_2_rotate_rotate(J, target, j1, j1->axis, j1->i_dof, x, i_dof+3);
		calc_jacobian_2_rotate_rotate(J, target, j1, j1->axis, j1->i_dof, y, i_dof+4);
		calc_jacobian_2_rotate_rotate(J, target, j1, j1->axis, j1->i_dof, z, i_dof+5);
	}
	parent->calc_jacobian_2_rotate_sub(J, target, j1);
	return 0;
}

int Joint::calc_jacobian_2_slide_sub(fMat* J, Joint* target, Joint* j1)
{
	if(!this) return 0;
	assert(j1->j_type == JSLIDE);
	static fVec3 x(1, 0, 0), y(0, 1, 0), z(0, 0, 1);
	if(j_type == JROTATE)
	{
		calc_jacobian_2_slide_rotate(J, target, j1, j1->axis, j1->i_dof, axis, i_dof);
	}
	else if(j_type == JSLIDE)
	{
	}
	else if(j_type == JSPHERE)
	{
		calc_jacobian_2_slide_rotate(J, target, j1, j1->axis, j1->i_dof, x, i_dof);
		calc_jacobian_2_slide_rotate(J, target, j1, j1->axis, j1->i_dof, y, i_dof+1);
		calc_jacobian_2_slide_rotate(J, target, j1, j1->axis, j1->i_dof, z, i_dof+2);
	}
	else if(j_type == JFREE)
	{
		calc_jacobian_2_slide_rotate(J, target, j1, j1->axis, j1->i_dof, x, i_dof+3);
		calc_jacobian_2_slide_rotate(J, target, j1, j1->axis, j1->i_dof, y, i_dof+4);
		calc_jacobian_2_slide_rotate(J, target, j1, j1->axis, j1->i_dof, z, i_dof+5);
	}
	parent->calc_jacobian_2_slide_sub(J, target, j1);
	return 0;
}

int Joint::calc_jacobian_2_sphere_sub(fMat* J, Joint* target, Joint* j1)
{
	if(!this) return 0;
	assert(j1->j_type == JSPHERE);
	static fVec3 x(1, 0, 0), y(0, 1, 0), z(0, 0, 1);
	if(j_type == JROTATE)
	{
		calc_jacobian_2_rotate_rotate(J, target, j1, x, j1->i_dof, axis, i_dof);
		calc_jacobian_2_rotate_rotate(J, target, j1, y, j1->i_dof+1, axis, i_dof);
		calc_jacobian_2_rotate_rotate(J, target, j1, z, j1->i_dof+2, axis, i_dof);
	}
	else if(j_type == JSLIDE)
	{
		calc_jacobian_2_rotate_slide(J, target, j1, x, j1->i_dof, axis, i_dof);
		calc_jacobian_2_rotate_slide(J, target, j1, y, j1->i_dof+1, axis, i_dof);
		calc_jacobian_2_rotate_slide(J, target, j1, z, j1->i_dof+2, axis, i_dof);
	}
	else if(j_type == JSPHERE)
	{
		// my x axis
		calc_jacobian_2_rotate_rotate(J, target, j1, x, j1->i_dof, x, i_dof);
		calc_jacobian_2_rotate_rotate(J, target, j1, y, j1->i_dof+1, x, i_dof);
		calc_jacobian_2_rotate_rotate(J, target, j1, z, j1->i_dof+2, x, i_dof);
		// my y axis
		calc_jacobian_2_rotate_rotate(J, target, j1, x, j1->i_dof, y, i_dof+1);
		calc_jacobian_2_rotate_rotate(J, target, j1, y, j1->i_dof+1, y, i_dof+1);
		calc_jacobian_2_rotate_rotate(J, target, j1, z, j1->i_dof+2, y, i_dof+1);
		// my z axis
		calc_jacobian_2_rotate_rotate(J, target, j1, x, j1->i_dof, z, i_dof+2);
		calc_jacobian_2_rotate_rotate(J, target, j1, y, j1->i_dof+1, z, i_dof+2);
		calc_jacobian_2_rotate_rotate(J, target, j1, z, j1->i_dof+2, z, i_dof+2);
	}
	else if(j_type == JFREE)
	{
		// my x trans
		calc_jacobian_2_rotate_slide(J, target, j1, x, j1->i_dof, x, i_dof);
		calc_jacobian_2_rotate_slide(J, target, j1, y, j1->i_dof+1, x, i_dof);
		calc_jacobian_2_rotate_slide(J, target, j1, z, j1->i_dof+2, x, i_dof);
		// my y trans
		calc_jacobian_2_rotate_slide(J, target, j1, x, j1->i_dof, y, i_dof+1);
		calc_jacobian_2_rotate_slide(J, target, j1, y, j1->i_dof+1, y, i_dof+1);
		calc_jacobian_2_rotate_slide(J, target, j1, z, j1->i_dof+2, y, i_dof+1);
		// my z trans
		calc_jacobian_2_rotate_slide(J, target, j1, x, j1->i_dof, z, i_dof+2);
		calc_jacobian_2_rotate_slide(J, target, j1, y, j1->i_dof+1, z, i_dof+2);
		calc_jacobian_2_rotate_slide(J, target, j1, z, j1->i_dof+2, z, i_dof+2);
		// my x rot
		calc_jacobian_2_rotate_rotate(J, target, j1, x, j1->i_dof, x, i_dof+3);
		calc_jacobian_2_rotate_rotate(J, target, j1, y, j1->i_dof+1, x, i_dof+3);
		calc_jacobian_2_rotate_rotate(J, target, j1, z, j1->i_dof+2, x, i_dof+3);
		// my y rot
		calc_jacobian_2_rotate_rotate(J, target, j1, x, j1->i_dof, y, i_dof+4);
		calc_jacobian_2_rotate_rotate(J, target, j1, y, j1->i_dof+1, y, i_dof+4);
		calc_jacobian_2_rotate_rotate(J, target, j1, z, j1->i_dof+2, y, i_dof+4);
		// my z rot
		calc_jacobian_2_rotate_rotate(J, target, j1, x, j1->i_dof, z, i_dof+5);
		calc_jacobian_2_rotate_rotate(J, target, j1, y, j1->i_dof+1, z, i_dof+5);
		calc_jacobian_2_rotate_rotate(J, target, j1, z, j1->i_dof+2, z, i_dof+5);
	}
	parent->calc_jacobian_2_sphere_sub(J, target, j1);
	return 0;
}

int Joint::calc_jacobian_2_free_sub(fMat* J, Joint* target, Joint* j1)
{
	if(!this) return 0;
	assert(j1->j_type == JFREE);
	static fVec3 x(1, 0, 0), y(0, 1, 0), z(0, 0, 1);
	if(j_type == JROTATE)
	{
		calc_jacobian_2_slide_rotate(J, target, j1, x, j1->i_dof, axis, i_dof);
		calc_jacobian_2_slide_rotate(J, target, j1, y, j1->i_dof+1, axis, i_dof);
		calc_jacobian_2_slide_rotate(J, target, j1, z, j1->i_dof+2, axis, i_dof);
		calc_jacobian_2_rotate_rotate(J, target, j1, x, j1->i_dof+3, axis, i_dof);
		calc_jacobian_2_rotate_rotate(J, target, j1, y, j1->i_dof+4, axis, i_dof);
		calc_jacobian_2_rotate_rotate(J, target, j1, z, j1->i_dof+5, axis, i_dof);
	}
	else if(j_type == JSLIDE)
	{
		calc_jacobian_2_rotate_slide(J, target, j1, x, j1->i_dof+3, axis, i_dof);
		calc_jacobian_2_rotate_slide(J, target, j1, y, j1->i_dof+4, axis, i_dof);
		calc_jacobian_2_rotate_slide(J, target, j1, z, j1->i_dof+5, axis, i_dof);
	}
	else if(j_type == JSPHERE)
	{
		// my x rot
		calc_jacobian_2_slide_rotate(J, target, j1, x, j1->i_dof, x, i_dof);
		calc_jacobian_2_slide_rotate(J, target, j1, y, j1->i_dof+1, x, i_dof);
		calc_jacobian_2_slide_rotate(J, target, j1, z, j1->i_dof+2, x, i_dof);
		calc_jacobian_2_rotate_rotate(J, target, j1, x, j1->i_dof+3, x, i_dof);
		calc_jacobian_2_rotate_rotate(J, target, j1, y, j1->i_dof+4, x, i_dof);
		calc_jacobian_2_rotate_rotate(J, target, j1, z, j1->i_dof+5, x, i_dof);
		// my y rot
		calc_jacobian_2_slide_rotate(J, target, j1, x, j1->i_dof, y, i_dof+1);
		calc_jacobian_2_slide_rotate(J, target, j1, y, j1->i_dof+1, y, i_dof+1);
		calc_jacobian_2_slide_rotate(J, target, j1, z, j1->i_dof+2, y, i_dof+1);
		calc_jacobian_2_rotate_rotate(J, target, j1, x, j1->i_dof+3, y, i_dof+1);
		calc_jacobian_2_rotate_rotate(J, target, j1, y, j1->i_dof+4, y, i_dof+1);
		calc_jacobian_2_rotate_rotate(J, target, j1, z, j1->i_dof+5, y, i_dof+1);
		// my z rot
		calc_jacobian_2_slide_rotate(J, target, j1, x, j1->i_dof, z, i_dof+2);
		calc_jacobian_2_slide_rotate(J, target, j1, y, j1->i_dof+1, z, i_dof+2);
		calc_jacobian_2_slide_rotate(J, target, j1, z, j1->i_dof+2, z, i_dof+2);
		calc_jacobian_2_rotate_rotate(J, target, j1, x, j1->i_dof+3, z, i_dof+2);
		calc_jacobian_2_rotate_rotate(J, target, j1, y, j1->i_dof+4, z, i_dof+2);
		calc_jacobian_2_rotate_rotate(J, target, j1, z, j1->i_dof+5, z, i_dof+2);
	}
	else if(j_type == JFREE)
	{
		// my x trans
		calc_jacobian_2_rotate_slide(J, target, j1, x, j1->i_dof+3, x, i_dof);
		calc_jacobian_2_rotate_slide(J, target, j1, y, j1->i_dof+4, x, i_dof);
		calc_jacobian_2_rotate_slide(J, target, j1, z, j1->i_dof+5, x, i_dof);
		// my y trans
		calc_jacobian_2_rotate_slide(J, target, j1, x, j1->i_dof+3, y, i_dof+1);
		calc_jacobian_2_rotate_slide(J, target, j1, y, j1->i_dof+4, y, i_dof+1);
		calc_jacobian_2_rotate_slide(J, target, j1, z, j1->i_dof+5, y, i_dof+1);
		// my z trans
		calc_jacobian_2_rotate_slide(J, target, j1, x, j1->i_dof+3, z, i_dof+2);
		calc_jacobian_2_rotate_slide(J, target, j1, y, j1->i_dof+4, z, i_dof+2);
		calc_jacobian_2_rotate_slide(J, target, j1, z, j1->i_dof+5, z, i_dof+2);
		// my x rot
		calc_jacobian_2_slide_rotate(J, target, j1, x, j1->i_dof, x, i_dof+3);
		calc_jacobian_2_slide_rotate(J, target, j1, y, j1->i_dof+1, x, i_dof+3);
		calc_jacobian_2_slide_rotate(J, target, j1, z, j1->i_dof+2, x, i_dof+3);
		calc_jacobian_2_rotate_rotate(J, target, j1, x, j1->i_dof+3, x, i_dof+3);
		calc_jacobian_2_rotate_rotate(J, target, j1, y, j1->i_dof+4, x, i_dof+3);
		calc_jacobian_2_rotate_rotate(J, target, j1, z, j1->i_dof+5, x, i_dof+3);
		// my y rot
		calc_jacobian_2_slide_rotate(J, target, j1, x, j1->i_dof, y, i_dof+4);
		calc_jacobian_2_slide_rotate(J, target, j1, y, j1->i_dof+1, y, i_dof+4);
		calc_jacobian_2_slide_rotate(J, target, j1, z, j1->i_dof+2, y, i_dof+4);
		calc_jacobian_2_rotate_rotate(J, target, j1, x, j1->i_dof+3, y, i_dof+4);
		calc_jacobian_2_rotate_rotate(J, target, j1, y, j1->i_dof+4, y, i_dof+4);
		calc_jacobian_2_rotate_rotate(J, target, j1, z, j1->i_dof+5, y, i_dof+4);
		// my z rot
		calc_jacobian_2_slide_rotate(J, target, j1, x, j1->i_dof, z, i_dof+5);
		calc_jacobian_2_slide_rotate(J, target, j1, y, j1->i_dof+1, z, i_dof+5);
		calc_jacobian_2_slide_rotate(J, target, j1, z, j1->i_dof+2, z, i_dof+5);
		calc_jacobian_2_rotate_rotate(J, target, j1, x, j1->i_dof+3, z, i_dof+5);
		calc_jacobian_2_rotate_rotate(J, target, j1, y, j1->i_dof+4, z, i_dof+5);
		calc_jacobian_2_rotate_rotate(J, target, j1, z, j1->i_dof+5, z, i_dof+5);
	}
	parent->calc_jacobian_2_free_sub(J, target, j1);
	return 0;
}

int Joint::calc_jacobian_2_rotate_rotate(fMat* J, Joint* target, Joint* jk, const fVec3& k_axis, int k_index, const fVec3& loc_axis, int loc_index)
{
	static fVec3 out1, out2;
	// 1st term
	static fVec3 jk_axis, my_axis, d_jk_axis, dp, dJ;
	jk_axis.mul(jk->abs_att, k_axis);
	my_axis.mul(abs_att, loc_axis);
	if(this != jk && isAscendant(jk))
	{
		d_jk_axis.zero();
	}
	else
	{
		d_jk_axis.cross(my_axis, jk_axis);
	}
	dp.sub(target->abs_pos, jk->abs_pos);
	out1.cross(d_jk_axis, dp);
	// 2nd term
	static fVec3 Jt, Jk;
	dp.sub(target->abs_pos, abs_pos);
	Jt.cross(my_axis, dp);
	if(this != jk && isAscendant(jk))
	{
		Jk.zero();
	}
	else
	{
		dp.sub(jk->abs_pos, abs_pos);
		Jk.cross(my_axis, dp);
	}
	dJ.sub(Jt, Jk);
	out2.cross(jk_axis, dJ);
	// set
	J[loc_index](0, k_index) = out1(0) + out2(0);
	J[loc_index](1, k_index) = out1(1) + out2(1);
	J[loc_index](2, k_index) = out1(2) + out2(2);
	J[loc_index](3, k_index) = d_jk_axis(0);
	J[loc_index](4, k_index) = d_jk_axis(1);
	J[loc_index](5, k_index) = d_jk_axis(2);
	return 0;
}

int Joint::calc_jacobian_2_slide_rotate(fMat* J, Joint* target, Joint* jk, const fVec3& k_axis, int k_index, const fVec3& loc_axis, int loc_index)
{
	static fVec3 jk_axis, my_axis, d_jk_axis;
	jk_axis.mul(jk->abs_att, k_axis);
	my_axis.mul(abs_att, loc_axis);
//	if(isAscendant(jk))
	if(this != jk && isAscendant(jk))
	{
		d_jk_axis.zero();
	}
	else
	{
		d_jk_axis.cross(my_axis, jk_axis);
	}
	J[loc_index](0, k_index) = d_jk_axis(0);
	J[loc_index](1, k_index) = d_jk_axis(1);
	J[loc_index](2, k_index) = d_jk_axis(2);
	return 0;
}

int Joint::calc_jacobian_2_rotate_slide(fMat* J, Joint* target, Joint* jk, const fVec3& k_axis, int k_index, const fVec3& loc_axis, int loc_index)
{
	if(isAscendant(jk))
	{
		static fVec3 jk_axis, my_axis, out1;
		jk_axis.mul(jk->abs_att, k_axis);
		my_axis.mul(abs_att, loc_axis);
		out1.cross(jk_axis, my_axis);
		J[loc_index](0, k_index) = out1(0);
		J[loc_index](1, k_index) = out1(1);
		J[loc_index](2, k_index) = out1(2);
	}
	return 0;
}


