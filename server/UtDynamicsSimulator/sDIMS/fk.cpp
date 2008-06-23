/*
 * fk.cpp
 * Create: Katsu Yamane, Univ. of Tokyo, 03.06.19
 */

#include "chain.h"

void Chain::CalcPosition()
{
	root->calc_position();
}

void Joint::calc_position()
{
	if(!this) return;
	if(parent)
	{
#if 0
		// readable, but slower version
		abs_pos = parent->abs_pos + parent->abs_att * rel_pos;
		abs_att = parent->abs_att * rel_att;
#else
		// faster version
		abs_pos.mul(parent->abs_att, rel_pos);
		abs_pos += parent->abs_pos;
		abs_att.mul(parent->abs_att, rel_att);
#endif
//		cout << name << ": " << abs_pos << endl;
	}
	child->calc_position();
	brother->calc_position();
}

void Chain::CalcVelocity()
{
	root->calc_velocity();
}

void Joint::calc_velocity()
{
	if(!this) return;
	if(parent)
	{
		static fMat33 t_rel_att;
		static fVec3 v1, v2;
		t_rel_att.tran(rel_att);
		// compute loc_lin_vel
		v1.cross(parent->loc_ang_vel, rel_pos);
		v2.mul(t_rel_att, parent->loc_lin_vel);
		loc_lin_vel.mul(t_rel_att, v1);
		loc_lin_vel += v2;
		loc_lin_vel += rel_lin_vel;
		// compute loc_ang_vel
		loc_ang_vel.mul(t_rel_att, parent->loc_ang_vel);
		loc_ang_vel += rel_ang_vel;
		// compute loc_com_vel
		v1.cross(loc_ang_vel, loc_com);
		loc_com_vel.add(loc_lin_vel, v1);
	}
	else
	{
		loc_lin_vel.zero();
		loc_ang_vel.zero();
	}
	child->calc_velocity();
	brother->calc_velocity();
}

void Chain::CalcAcceleration()
{
	root->calc_acceleration();
}

void Joint::calc_acceleration()
{
	if(!this) return;
	if(parent)
	{
		static fMat33 t_rel_att;
		static fVec3 v1, v2, v3, v4;
		t_rel_att.tran(rel_att);
		// 並進加速度
		v1.mul(t_rel_att, parent->loc_ang_vel);
//		v1 += loc_ang_vel;
		v1 *= 2.0;
		v2.cross(v1, rel_lin_vel);
		loc_lin_acc.add(rel_lin_acc, v2);
		v2.cross(parent->loc_ang_acc, rel_pos);
		v1.add(parent->loc_lin_acc, v2);
		v2.cross(parent->loc_ang_vel, rel_pos);
		v3.cross(parent->loc_ang_vel, v2);
		v1 += v3;
		v4.mul(t_rel_att, v1);
		loc_lin_acc += v4;
		// 角加速度
//		v1.cross(loc_ang_vel, parent->rel_ang_vel);
		v1.cross(loc_ang_vel, rel_ang_vel);
		loc_ang_acc.add(rel_ang_acc, v1);
		v1.mul(t_rel_att, parent->loc_ang_acc);
		loc_ang_acc += v1;
		// 重心の並進加速度
		v1.cross(loc_ang_acc, loc_com);
		loc_com_acc.add(loc_lin_acc, v1);
		v1.cross(loc_ang_vel, loc_com);
		v2.cross(loc_ang_vel, v1);
		loc_com_acc += v2;

//		cerr << name << ": loc_lin_acc = " << loc_lin_acc << endl;
//		cerr << name << ": loc_ang_acc = " << loc_ang_acc << endl;
//		if(real)
//			cerr << name << ": acc = " << abs_att*loc_lin_acc << abs_att*loc_ang_acc << endl;
	}
	else
	{
		// ルートリンク
		// loc_lin_accには重力加速度が入っているので初期化しない
		loc_ang_acc.zero();
		loc_com_acc.zero();
	}
	child->calc_acceleration();
	brother->calc_acceleration();
}

double Chain::TotalCOM(fVec3& com, const char* chname)
{
	com.zero();
	double m = root->total_com(com, chname);
	com /= m;
	return m;
}

double Joint::total_com(fVec3& com, const char* chname)
{
	if(!this) return 0.0;
	int is_target = false;
	if(!chname)
	{
		is_target = true;
	}
	else
	{
		char* my_chname = CharName();
		if(my_chname && !strcmp(my_chname, chname))
		{
			is_target = true;
		}
	}
	fVec3 b_com, c_com;
	b_com.zero();
	c_com.zero();
	double ret = brother->total_com(b_com, chname) + child->total_com(c_com, chname);

	com.add(b_com, c_com);
	if(is_target)
	{
		static fVec3 abs_com_pos, my_com;
		ret += mass;
		abs_com_pos.mul(abs_att, loc_com);
		abs_com_pos += abs_pos;
		my_com.mul(abs_com_pos, mass);
		com += my_com;
	}
	return ret;
}

