/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*
 * joint.cpp
 * Create: Katsu Yamane, Univ. of Tokyo, 03.10.21
 */

#include "chain.h"

double Joint::TotalMass()
{
	if(!child) return mass;
	return (mass + child->total_mass());
}

double Joint::total_mass()
{
	if(!child) return mass;
	double b = 0.0;
	if(brother) b = brother->total_mass();
	return mass + b + child->total_mass();
}

void Joint::SetRotateJointType(const fVec3& rpos, const fMat33& ratt, AxisIndex ai)
{
	j_type = JROTATE;
	if(t_given) n_dof = 1;
	else n_thrust = 1;
	init_pos.set(rpos);
	init_att.set(ratt);
	rel_pos.set(rpos);
	rel_att.set(ratt);
	axis.zero();
	if(ai != AXIS_NULL) axis(ai) = 1.0;
}

void Joint::SetSlideJointType(const fVec3& rpos, const fMat33& ratt, AxisIndex ai)
{
	j_type = JSLIDE;
	if(t_given) n_dof = 1;
	else n_thrust = 1;
	init_pos.set(rpos);
	init_att.set(ratt);
	rel_pos.set(rpos);
	rel_att.set(ratt);
	axis.zero();
	if(ai != AXIS_NULL) axis(ai) = 1.0;
}

void Joint::SetFixedJointType(const fVec3& rpos, const fMat33& ratt)
{
	j_type = JFIXED;
	n_dof = 0;
	n_thrust = 0;
	init_pos.set(rpos);
	init_att.set(ratt);
	rel_pos.set(rpos);
	rel_att.set(ratt);
	rel_ep.set(rel_att);
}

void Joint::SetSphereJointType(const fVec3& rpos, const fMat33& ratt)
{
	j_type = JSPHERE;
	if(t_given) n_dof = 3;
	else n_thrust = 3;
	init_pos.set(rpos);
	init_att.set(ratt);
	rel_pos.set(rpos);
	rel_att.set(ratt);
	rel_ep.set(rel_att);
}

void Joint::SetFreeJointType(const fVec3& rpos, const fMat33& ratt)
{
	j_type = JFREE;
	if(t_given) n_dof = 6;
	else n_thrust = 6;
	init_pos.set(rpos);
	init_att.set(ratt);
	rel_pos.set(rpos);
	rel_att.set(ratt);
	rel_ep.set(rel_att);
}

void Joint::UpdateJointType()
{
	n_dof = 0;
	n_thrust = 0;
	switch(j_type)
	{
	case JROTATE:
	case JSLIDE:
		if(t_given) n_dof = 1;
		else n_thrust = 1;
		break;
	case JSPHERE:
		if(t_given) n_dof = 3;
		else n_thrust = 3;
		break;
	case JFREE:
		if(t_given) n_dof = 6;
		else n_thrust = 6;
		break;
	case JFIXED:
		break;
	default:
		break;
	}
	init_pos.set(rel_pos);
	init_att.set(rel_att);
	rel_ep.set(rel_att);
}

/*
 * get joint values, vels, accs
 */
int Chain::GetJointValue(fVec& values)
{
	values.resize(n_value);
	root->get_joint_value(values);
	return 0;
}

int Joint::get_joint_value(fVec& values)
{
	if(!this) return 0;
	static fVec3 p;
	static fMat33 r;
	static fEulerPara ep;
	if(i_value >= 0)
	{
		switch(j_type)
		{
		case JROTATE:
		case JSLIDE:
			GetJointValue(values(i_value));
			break;
		case JSPHERE:
			GetJointValue(r);
			ep.set(r);
			values(i_value) = ep(0);
			values(i_value+1) = ep(1);
			values(i_value+2) = ep(2);
			values(i_value+3) = ep(3);
			break;
		case JFREE:
			GetJointValue(p, r);
			ep.set(r);
			values(i_value) = p(0);
			values(i_value+1) = p(1);
			values(i_value+2) = p(2);
			values(i_value+3) = ep(0);
			values(i_value+4) = ep(1);
			values(i_value+5) = ep(2);
			values(i_value+6) = ep(3);
			break;
		default:
			break;
		}
	}
	child->get_joint_value(values);
	brother->get_joint_value(values);
	return 0;
}

int Chain::GetJointVel(fVec& vels)
{
	vels.resize(n_dof);
	root->get_joint_vel(vels);
	return 0;
}

int Joint::get_joint_vel(fVec& vels)
{
	if(!this) return 0;
	static fVec3 pd, rd;
	if(i_dof >= 0)
	{
		switch(j_type)
		{
		case JROTATE:
		case JSLIDE:
			GetJointVel(vels(i_dof));
			break;
		case JSPHERE:
			GetJointVel(rd);
			vels(i_dof) = rd(0);
			vels(i_dof+1) = rd(1);
			vels(i_dof+2) = rd(2);
			break;
		case JFREE:
			GetJointVel(pd, rd);
			vels(i_dof) = pd(0);
			vels(i_dof+1) = pd(1);
			vels(i_dof+2) = pd(2);
			vels(i_dof+3) = rd(0);
			vels(i_dof+4) = rd(1);
			vels(i_dof+5) = rd(2);
			break;
		default:
			break;
		}
	}
	brother->get_joint_vel(vels);
	child->get_joint_vel(vels);
	return 0;
}

int Chain::GetJointAcc(fVec& accs)
{
	accs.resize(n_dof);
	root->get_joint_acc(accs);
	return 0;
}

int Joint::get_joint_acc(fVec& accs)
{
	if(!this) return 0;
	static fVec3 pdd, rdd;
	if(i_dof >= 0)
	{
		switch(j_type)
		{
		case JROTATE:
		case JSLIDE:
			GetJointAcc(accs(i_dof));
			break;
		case JSPHERE:
			GetJointAcc(rdd);
			accs(i_dof) = rdd(0);
			accs(i_dof+1) = rdd(1);
			accs(i_dof+2) = rdd(2);
			break;
		case JFREE:
			GetJointAcc(pdd, rdd);
			accs(i_dof) = pdd(0);
			accs(i_dof+1) = pdd(1);
			accs(i_dof+2) = pdd(2);
			accs(i_dof+3) = rdd(0);
			accs(i_dof+4) = rdd(1);
			accs(i_dof+5) = rdd(2);
			break;
		default:
			break;
		}
	}
	brother->get_joint_acc(accs);
	child->get_joint_acc(accs);
	return 0;
}

int Joint::GetJointValue(double& _q)
{
	if(j_type == JROTATE || j_type == JSLIDE)
	{
		_q = q;
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::GetJointVel(double& _qd)
{
	if(j_type == JROTATE || j_type == JSLIDE)
	{
		_qd = qd;
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::GetJointAcc(double& _qdd)
{
	if(j_type == JROTATE || j_type == JSLIDE)
	{
		_qdd = qdd;
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::GetJointValue(fMat33& r)
{
	if(j_type == JSPHERE || j_type == JFREE)
	{
		r.set(rel_att);
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::GetJointVel(fVec3& rd)
{
	if(j_type == JSPHERE || j_type == JFREE)
	{
		rd.set(rel_ang_vel);
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::GetJointAcc(fVec3& rdd)
{
	if(j_type == JSPHERE || j_type == JFREE)
	{
		rdd.set(rel_ang_acc);
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::GetJointValue(fVec3& p, fMat33& r)
{
	if(j_type == JFREE || j_type == JFIXED)
	{
		p.set(rel_pos);
		r.set(rel_att);
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::GetJointVel(fVec3& pd, fVec3& rd)
{
	if(j_type == JFREE)
	{
		pd.set(rel_lin_vel);
		rd.set(rel_ang_vel);
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::GetJointAcc(fVec3& pdd, fVec3& rdd)
{
	if(j_type == JFREE)
	{
		pdd.set(rel_lin_acc);
		rdd.set(rel_ang_acc);
	}
	else
	{
		return -1;
	}
	return 0;
}

/*
 * set joint position/orientation
 */
int Joint::SetJointPosition(const fVec3& p)
{
	rel_pos.set(p);
	return 0;
}

int Joint::SetJointOrientation(const fMat33& r)
{
	rel_att.set(r);
	rel_ep.set(r);
	return 0;
}

/*
 * compute relative from absolute position/orientation
 * call CalcPosition() first
 */
int Chain::set_abs_position_orientation(Joint* jnt, const fVec3& abs_pos, const fMat33& abs_att)
{
	if(!jnt->parent) return -1;
	fVec3 p_pos(jnt->parent->abs_pos);
	fMat33 p_att(jnt->parent->abs_att);
	static fVec3 pp, r_pos;
	static fMat33 tR, r_att;
	pp.sub(abs_pos, p_pos);
	tR.tran(p_att);
	r_pos.mul(tR, pp);
	r_att.mul(tR, abs_att);
	jnt->SetJointPosition(r_pos);
	jnt->SetJointOrientation(r_att);
	return 0;
}

/*
 * set joint values, vels, accs
 */
int Chain::SetJointValue(const fVec& values)
{
	if(values.size() != n_value)
	{
		cerr << "Chain::SetJointValue: error - size of the argument should be n_value (" << n_value << ")" << endl;
		return -1;
	}
	root->set_joint_value(values);
	return 0;
}

int Joint::set_joint_value(const fVec& values)
{
	if(!this) return 0;
	static fVec3 p;
	static fEulerPara ep;
	if(i_value >= 0)
	{
		switch(j_type)
		{
		case JROTATE:
		case JSLIDE:
			SetJointValue(values(i_value));
			break;
		case JSPHERE:
			ep.set(values(i_value), values(i_value+1), values(i_value+2), values(i_value+3));
			SetJointValue(ep);
			break;
		case JFREE:
			p.set(values(i_value), values(i_value+1), values(i_value+2));
			ep.set(values(i_value+3), values(i_value+4), values(i_value+5), values(i_value+6));
			SetJointValue(p, ep);
			break;
		default:
			break;
		}
	}
	child->set_joint_value(values);
	brother->set_joint_value(values);
	return 0;
}

int Chain::SetJointVel(const fVec& vels)
{
	if(vels.size() != n_dof)
	{
		cerr << "Chain::SetJointVel: error - size of the argument should be n_dof (" << n_dof << ")" << endl;
		return -1;
	}
	root->set_joint_vel(vels);
	return 0;
}

int Joint::set_joint_vel(const fVec& vels)
{
	if(!this) return 0;
	static fVec3 pd;
	static fVec3 rd;
	if(i_dof >= 0)
	{
		switch(j_type)
		{
		case JROTATE:
		case JSLIDE:
			SetJointVel(vels(i_dof));
			break;
		case JSPHERE:
			rd.set(vels(i_dof), vels(i_dof+1), vels(i_dof+2));
			SetJointVel(rd);
			break;
		case JFREE:
			pd.set(vels(i_dof), vels(i_dof+1), vels(i_dof+2));
			rd.set(vels(i_dof+3), vels(i_dof+4), vels(i_dof+5));
			SetJointVel(pd, rd);
			break;
		default:
			break;
		}
	}
	child->set_joint_vel(vels);
	brother->set_joint_vel(vels);
	return 0;
}

int Chain::SetJointAcc(const fVec& accs)
{
	if(accs.size() != n_dof)
	{
		cerr << "Chain::SetJointAcc: error - size of the argument should be n_dof (" << n_dof << ")" << endl;
		return -1;
	}
	root->set_joint_acc(accs);
	return 0;
}

int Joint::set_joint_acc(const fVec& accs)
{
	if(!this) return 0;
	static fVec3 pdd;
	static fVec3 rdd;
	if(i_dof >= 0)
	{
		switch(j_type)
		{
		case JROTATE:
		case JSLIDE:
			SetJointAcc(accs(i_dof));
			break;
		case JSPHERE:
			rdd.set(accs(i_dof), accs(i_dof+1), accs(i_dof+2));
			SetJointAcc(rdd);
			break;
		case JFREE:
			pdd.set(accs(i_dof), accs(i_dof+1), accs(i_dof+2));
			rdd.set(accs(i_dof+3), accs(i_dof+4), accs(i_dof+5));
			SetJointAcc(pdd, rdd);
			break;
		default:
			break;
		}
	}
	child->set_joint_acc(accs);
	brother->set_joint_acc(accs);
	return 0;
}

int Joint::SetJointValue(double _q)
{
	q = _q;
	if(j_type == JROTATE)
	{
		static fMat33 tmp;
		tmp.rot2mat(axis, q);
		rel_att.mul(init_att, tmp);
		rel_ep.set(rel_att);
	}
	else if(j_type == JSLIDE)
	{
		static fVec3 tmp, tmp1;
		tmp.mul(q, axis);
		tmp1.mul(rel_att, tmp);
		rel_pos.add(tmp1, init_pos);
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::SetJointVel(double _qd)
{
	qd = _qd;
	if(j_type == JROTATE)
	{
		rel_ang_vel.mul(qd, axis);
	}
	else if(j_type == JSLIDE)
	{
		rel_lin_vel.mul(qd, axis);
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::SetJointAcc(double _qdd)
{
	qdd = _qdd;
	if(j_type == JROTATE)
	{
		rel_ang_acc.mul(qdd, axis);
	}
	else if(j_type == JSLIDE)
	{
		rel_lin_acc.mul(qdd, axis);
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::SetJointValue(const fMat33& r)
{
	if(j_type == JSPHERE || j_type == JFREE)
	{
		rel_att.set(r);
		rel_ep.set(rel_att);
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::SetJointValue(const fEulerPara& ep)
{
	if(j_type == JSPHERE || j_type == JFREE)
	{
		rel_ep.set(ep);
		rel_att.set(rel_ep);
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::SetJointVel(const fVec3& rd)
{
	if(j_type == JSPHERE || j_type == JFREE)
	{
		rel_ang_vel.set(rd);
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::SetJointAcc(const fVec3& rdd)
{
	if(j_type == JSPHERE || j_type == JFREE)
	{
		rel_ang_acc.set(rdd);
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::SetJointAcc(double ax, double ay, double az)
{
	if(j_type == JSPHERE || j_type == JFREE)
	{
		rel_ang_acc(0) = ax;
		rel_ang_acc(1) = ay;
		rel_ang_acc(2) = az;
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::SetJointValue(const fVec3& p, const fMat33& r)
{
	if(j_type == JFREE || j_type == JFIXED)
	{
		rel_pos.set(p);
		rel_att.set(r);
		rel_ep.set(rel_att);
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::SetJointValue(const fVec3& p, const fEulerPara& ep)
{
	if(j_type == JFREE || j_type == JFIXED)
	{
		rel_pos.set(p);
		rel_ep.set(ep);
		rel_att.set(rel_ep);
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::SetJointVel(const fVec3& pd, const fVec3& rd)
{
	if(j_type == JFREE)
	{
		rel_lin_vel.set(pd);
		rel_ang_vel.set(rd);
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::SetJointAcc(const fVec3& pdd, const fVec3& rdd)
{
	if(j_type == JFREE)
	{
		rel_lin_acc.set(pdd);
		rel_ang_acc.set(rdd);
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::SetJointAcc(double lx, double ly, double lz, double ax, double ay, double az)
{
	if(j_type == JFREE)
	{
		rel_lin_acc(0) = lx;
		rel_lin_acc(1) = ly;
		rel_lin_acc(2) = lz;
		rel_ang_acc(0) = ax;
		rel_ang_acc(1) = ay;
		rel_ang_acc(2) = az;
	}
	else
	{
		return -1;
	}
	return 0;
}

/*
 * set joint froce/torque
 */
int Chain::SetJointForce(const fVec& forces)
{
	if(forces.size() != n_dof)
	{
		cerr << "Chain::SetJointForce: error - size of the argument should be n_dof (" << n_dof << ")" << endl;
		return -1;
	}
	root->set_joint_force(forces);
	return 0;
}

int Joint::set_joint_force(const fVec& forces)
{
	if(!this) return 0;
	if(i_dof >= 0)
	{
		switch(j_type)
		{
		case JROTATE:
		case JSLIDE:
			SetJointForce(forces(i_dof));
			break;
		case JSPHERE:
			SetJointForce(fVec3(forces(i_dof), forces(i_dof+1), forces(i_dof+2)));
			break;
		case JFREE:
			SetJointForce(fVec3(forces(i_dof), forces(i_dof+1), forces(i_dof+2)),
						  fVec3(forces(i_dof+3), forces(i_dof+4), forces(i_dof+5)));
			break;
		default:
			break;
		}
	}
	child->set_joint_force(forces);
	brother->set_joint_force(forces);
	return 0;
}

int Joint::SetJointForce(double _tau)
{
	if(j_type == JROTATE || j_type == JSLIDE)
	{
		tau = _tau;
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::GetJointForce(double& _tau)
{
	if(j_type == JROTATE || j_type == JSLIDE)
	{
		_tau = tau;
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::SetJointForce(const fVec3& _n3)
{
	if(j_type == JSPHERE)
	{
		tau_n.set(_n3);
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::GetJointForce(fVec3& _n3)
{
	if(j_type == JSPHERE)
	{
		_n3.set(tau_n);
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::SetJointForce(const fVec3& _f3, const fVec3& _n3)
{
	if(j_type == JFREE)
	{
		tau_f.set(_f3);
		tau_n.set(_n3);
	}
	else
	{
		return -1;
	}
	return 0;
}

int Joint::GetJointForce(fVec3& _f3, fVec3& _n3)
{
	if(j_type == JFREE)
	{
		_f3.set(tau_f);
		_n3.set(tau_n);
	}
	else
	{
		return -1;
	}
	return 0;
}

/*
 * get joint force/torque
 */
int Chain::GetJointForce(fVec& forces)
{
	if(forces.size() != n_dof)
	{
		cerr << "Chain::GetJointForce: error - size of the argument should be n_dof (" << n_dof << ")" << endl;
		return -1;
	}
	root->get_joint_force(forces);
	return 0;
}

int Joint::get_joint_force(fVec& forces)
{
	if(!this) return 0;
	if(i_dof >= 0)
	{
		switch(j_type)
		{
		case JROTATE:
		case JSLIDE:
			forces(i_dof) = tau;
			break;
		case JSPHERE:
			forces(i_dof) = tau_n(0);
			forces(i_dof+1) = tau_n(1);
			forces(i_dof+2) = tau_n(2);
			break;
		case JFREE:
			forces(i_dof) = tau_f(0);
			forces(i_dof+1) = tau_f(1);
			forces(i_dof+2) = tau_f(2);
			forces(i_dof+3) = tau_n(0);
			forces(i_dof+4) = tau_n(1);
			forces(i_dof+5) = tau_n(2);
			break;
		default:
			break;
		}
	}
	child->get_joint_force(forces);
	brother->get_joint_force(forces);
	return 0;
}

/*
 * clear joint force/torque and external force/moment
 */
int Chain::ClearJointForce()
{
	root->clear_joint_force();
	return 0;
}

int Joint::clear_joint_force()
{
	if(!this) return 0;
	tau = 0.0;
	tau_f.zero();
	tau_n.zero();
	brother->clear_joint_force();
	child->clear_joint_force();
	return 0;
}

int Chain::ClearExtForce()
{
	root->clear_ext_force();
	return 0;
}

int Joint::clear_ext_force()
{
	if(!this) return 0;
	ext_force.zero();
	ext_moment.zero();
	brother->clear_ext_force();
	child->clear_ext_force();
	return 0;
}
