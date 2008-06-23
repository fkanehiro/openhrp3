/*
 * update.cpp
 * Create: Katsu Yamane, 03.07.10
 */

#include "ik.h"

#ifdef MEASURE_TIME
#include <time.h>
double calc_jacobian_time = 0;
double solve_ik_time = 0;
double high_constraint_time = 0;
double low_constraint_time = 0;
#endif

double IK::Update(double timestep)
{
	double ret;
	CalcPosition();
#ifdef MEASURE_TIME
	clock_t t1 = clock();
#endif
	calc_jacobian();
	calc_feedback();
#ifdef MEASURE_TIME
	clock_t t2 = clock();
	calc_jacobian_time += (double)(t2 - t1) / (double)CLOCKS_PER_SEC;
#endif
	ret = solve_ik();
#ifdef MEASURE_TIME
	solve_ik_time += (double)(clock() - t2) / (double)CLOCKS_PER_SEC;
#endif
	Integrate(timestep);
	CalcPosition();
	return ret;
}

double IK::Update(double max_timestep, double min_timestep, double max_integ_error)
{
	double ret;
	double new_timestep = max_timestep;
	for(int i=0; i<2; i++)
	{
		CalcPosition();
		calc_jacobian();
		calc_feedback();
		ret = solve_ik();
		IntegrateAdaptive(new_timestep, i, min_timestep, max_integ_error);
	}
//	cerr << new_timestep << endl;
	CalcPosition();
	return ret;
}

/*
 * calc_jacobian
 */
int IK::calc_jacobian()
{
	int i;
	for(i=0; i<n_constraints; i++)
	{
		constraints[i]->calc_jacobian();
	}
	return 0;
}

int IKConstraint::calc_jacobian()
{
	J.resize(n_const, ik->NumDOF());
	J.zero();
	calc_jacobian(ik->Root());
	return 0;
}

int IKConstraint::calc_jacobian(Joint* cur)
{
	if(!cur) return 0;
	switch(cur->j_type)
	{
	case JROTATE:
		calc_jacobian_rotate(cur);
		break;
	case JSLIDE:
		calc_jacobian_slide(cur);
		break;
	case JSPHERE:
		calc_jacobian_sphere(cur);
		break;
	case JFREE:
		calc_jacobian_free(cur);
		break;
	default:
		break;
	}
	calc_jacobian(cur->child);
	calc_jacobian(cur->brother);
	return 0;
}

/*
 * calc_feedback
 */
int IK::calc_feedback()
{
	int i;
	for(i=0; i<n_constraints; i++)
	{
		constraints[i]->calc_feedback();
	}
	return 0;
}

/*
 * solve_ik
 */
int IK::copy_jacobian()
{
	int i, m;
	for(m=0; m<N_PRIORITY_TYPES; m++)
		n_const[m] = 0;
	n_all_const = 0;
	// count number of constraints
	for(i=0; i<n_constraints; i++)
	{
		constraints[i]->count_constraints();
	}
	if(n_all_const == 0) return 0;
	// 行列・ベクトルを準備
	for(m=0; m<N_PRIORITY_TYPES; m++)
	{
		if(n_const[m] > 0)
		{
			J[m].resize(n_const[m], n_dof);
			J[m].zero();
			fb[m].resize(n_const[m]);
			fb[m].zero();
			weight[m].resize(n_const[m]);
			weight[m] = 1.0;
		}
	}
	// 各拘束のヤコビアン・フィードバック速度を集めて
	// 大きな行列・ベクトルにする
	for(i=0; i<n_constraints; i++)
		constraints[i]->copy_jacobian();
	return 0;
}

double IK::solve_ik()
{
	int i, j;
	double current_max_condnum = -1.0;
	copy_jacobian();
	if(n_all_const > 0)
	{
		////
		// check rank when HIGH_IF_POSSIBLE constraints have high priority
		int n_high_const;
		fMat Jhigh;
		fMat wJhigh;
		fVec fb_high;
		fVec weight_high;
		int* is_high_const = 0;
		double cond_number = 1.0;
//		cerr << "---" << endl;
//		cerr << n_const[HIGH_PRIORITY] << " " << n_const[HIGH_IF_POSSIBLE] << " " << n_const[LOW_PRIORITY] << endl;
		if(n_const[HIGH_IF_POSSIBLE] > 0)
		{
			is_high_const = new int [n_const[HIGH_IF_POSSIBLE]];
			// initialize
			for(i=0; i<n_const[HIGH_IF_POSSIBLE]; i++)
				is_high_const[i] = true;
			// search
			int search_phase = 0;
			while(1)
			{
				n_high_const = n_const[HIGH_PRIORITY];
				for(i=0; i<n_const[HIGH_IF_POSSIBLE]; i++)
				{
					if(is_high_const[i]) n_high_const++;
				}
				Jhigh.resize(n_high_const, n_dof);
				wJhigh.resize(n_high_const, n_dof);
				fb_high.resize(n_high_const);
				weight_high.resize(n_high_const);
				if(n_const[HIGH_PRIORITY] > 0)
				{
					// set fb and J of higher priority pins
					for(i=0; i<n_const[HIGH_PRIORITY]; i++)
					{
						fb_high(i) = fb[HIGH_PRIORITY](i);
						weight_high(i) = weight[HIGH_PRIORITY](i);
						for(j=0; j<n_dof; j++)
						{
							Jhigh(i, j) = J[HIGH_PRIORITY](i, j);
							wJhigh(i, j) = Jhigh(i, j) * weight_high(i) / joint_weights(j);
						}
					}
				}
				int count = 0;
				// set fb and J of medium priority pins
				for(i=0; i<n_const[HIGH_IF_POSSIBLE]; i++)
				{
					if(is_high_const[i])
					{
						fb_high(n_const[HIGH_PRIORITY]+count) = fb[HIGH_IF_POSSIBLE](i);
						weight_high(n_const[HIGH_PRIORITY]+count) = weight[HIGH_IF_POSSIBLE](i);
						for(j=0; j<n_dof; j++)
						{
							Jhigh(n_const[HIGH_PRIORITY]+count, j) = J[HIGH_IF_POSSIBLE](i, j);
							wJhigh(n_const[HIGH_PRIORITY]+count, j) = J[HIGH_IF_POSSIBLE](i, j) * weight[HIGH_IF_POSSIBLE](i) / joint_weights(j);
						}
						count++;
					}
				}
				// singular value decomposition
				fMat U(n_high_const, n_high_const), VT(n_dof, n_dof);
				fVec s;
				int s_size;
				if(n_high_const < n_dof) s_size = n_high_const;
				else s_size = n_dof;
				s.resize(s_size);
				wJhigh.svd(U, s, VT);
				double condnum_limit = max_condnum * 100.0;
				if(s(s_size-1) > s(0)/(max_condnum*condnum_limit))
					cond_number = s(0) / s(s_size-1);
				else
					cond_number = condnum_limit;
				if(current_max_condnum < 0.0 || cond_number > current_max_condnum)
				{
					current_max_condnum = cond_number;
				}
				if(n_high_const <= n_const[HIGH_PRIORITY]) break;
				// remove some constraints
				if(cond_number > max_condnum)
				{
					int reduced = false;
					for(i=n_constraints-1; i>=0; i--)
					{
						if(constraints[i]->enabled &&
						   constraints[i]->priority == HIGH_IF_POSSIBLE &&
						   constraints[i]->i_const >= 0 &&
						   constraints[i]->GetType() == HANDLE_CONSTRAINT &&
						   is_high_const[constraints[i]->i_const])
						{
							IKHandle* h = (IKHandle*)constraints[i];
							if(search_phase ||
							   (!search_phase && h->joint->DescendantDOF() > 0))
							{
								for(j=0; j<constraints[i]->n_const; j++)
								{
									is_high_const[constraints[i]->i_const + j] = false;
								}
								constraints[i]->is_dropped = true;
//								cerr << "r" << flush;
								reduced = true;
								break;
							}
						}
					}
					if(!reduced) search_phase++;
				}
				else break;
			}
		}
		else
		{
			n_high_const = n_const[HIGH_PRIORITY];
			Jhigh.resize(n_high_const, n_dof);
			wJhigh.resize(n_high_const, n_dof);
			fb_high.resize(n_high_const);
			weight_high.resize(n_high_const);
			if(n_high_const > 0)
			{
				Jhigh.set(J[HIGH_PRIORITY]);
				fb_high.set(fb[HIGH_PRIORITY]);
				weight_high.set(weight[HIGH_PRIORITY]);
			}
		}
#if 0
		////
		// adjust feedback according to the condition number
		if(current_max_condnum > max_condnum)
			fb_high.zero();
		else
		{
			double k = (current_max_condnum-max_condnum)/(1.0-max_condnum);
			fb_high *= k;
			cerr << current_max_condnum << ", " << k << endl;
		}
		////
		////
		if(current_max_condnum < 0.0) current_max_condnum = 1.0;
#endif
		int n_low_const = n_all_const - n_high_const;
		int low_first = 0, count = 0;
		fMat Jlow(n_low_const, n_dof);
		fVec fb_low(n_low_const);
		fVec weight_low(n_low_const);
		for(i=0; i<n_const[HIGH_IF_POSSIBLE]; i++)
		{
			if(!is_high_const[i])
			{
				fb_low(count) = fb[HIGH_IF_POSSIBLE](i);
				weight_low(count) = weight[HIGH_IF_POSSIBLE](i);
				for(j=0; j<n_dof; j++)
				{
					Jlow(count, j) = J[HIGH_IF_POSSIBLE](i, j);
				}
				count++;
			}
		}
		low_first = count;
		double* p = fb_low.data() + low_first;
		double* q = fb[LOW_PRIORITY].data();
		double* r = weight_low.data() + low_first;
		double* s = weight[LOW_PRIORITY].data();
		for(i=0; i<n_const[LOW_PRIORITY]; p++, q++, r++, s++, i++)
		{
//			fb_low(low_first+i) = fb[LOW_PRIORITY](i);
			*p = *q;
			*r = *s;
			double* a = Jlow.data() + low_first + i;
			int a_row = Jlow.row();
			double* b = J[LOW_PRIORITY].data() + i;
			int b_row = J[LOW_PRIORITY].row();
			for(j=0; j<n_dof; a+=a_row, b+=b_row, j++)
			{
//				Jlow(low_first+i, j) = J[LOW_PRIORITY](i, j);
				*a = *b;
			}
		}
		if(is_high_const) delete[] is_high_const;

		fVec jvel(n_dof);   // 関節速度
		fVec jvel0(n_dof);  // 最小2乗解
		fVec fb_low_0(n_low_const), dfb(n_low_const), y(n_dof);
		fMat Jinv(n_dof, n_high_const), W(n_dof, n_dof), JW(n_low_const, n_dof);
		fVec w_error(n_low_const), w_norm(n_dof);
		// weighted
		double damping = 0.1;
//		w_error = 1.0;
		w_error.set(weight_low);
		w_norm = 1.0;
//		w_norm.set(joint_weights);
//		cerr << "joint_weights = " << joint_weights << endl;
//		cerr << "weight_high = " << weight_high << endl;
//		cerr << "weight_low = " << weight_low << endl;
#ifdef MEASURE_TIME
		clock_t t1 = clock();
#endif
		// 最小2乗解と一般解を計算
		if(n_high_const > 0)
		{
			for(i=0; i<n_dof; i++)
			{
				int a_row = wJhigh.row();
				double* a = wJhigh.data() + a_row*i;
				int b_row = Jhigh.row();
				double* b = Jhigh.data() + b_row*i;
				double* c = joint_weights.data() + i;
				double* d = weight_high.data();
				for(j=0; j<n_high_const; a++, b++, d++, j++)
				{
//					wJhigh(j, i) = Jhigh(j, i) / joint_weights(i);
					*a = *b * *d / *c;
				}
			}
			fVec w_fb_high(fb_high);
			for(i=0; i<n_high_const; i++)
				w_fb_high(i) *= weight_high(i);
			Jinv.p_inv(wJhigh);
			jvel0.mul(Jinv, w_fb_high);
			W.mul(Jinv, wJhigh);
			for(i=0; i<n_dof; i++)
			{
				double* w = W.data() + i;
				double a = joint_weights(i);
				for(j=0; j<n_dof; w+=n_dof, j++)
				{
					if(i==j) *w -= 1.0;
					*w /= -a;
				}
				jvel0(i) /= a;
			}
			JW.mul(Jlow, W);
		}
		else
		{
			jvel0.zero();
			JW.set(Jlow);
		}
#ifdef MEASURE_TIME
		clock_t t2 = clock();
		high_constraint_time += (double)(t2-t1)/(double)CLOCKS_PER_SEC;
#endif
		// 任意ベクトル項を計算
		if(n_low_const > 0)
		{
			fb_low_0.mul(Jlow, jvel0);
			dfb.sub(fb_low, fb_low_0);
			y.lineq_sr(JW, w_error, w_norm, damping, dfb);
			if(n_high_const > 0) jvel.mul(W, y);
			else jvel.set(y);
//			fVec error(n_low_const);
//			error = dfb-Jlow*jvel;
//			cerr << dfb*dfb << "->" << error*error << endl;
		}
		else
		{
			jvel.zero();
		}
		// 関節速度
		jvel += jvel0;
#ifdef MEASURE_TIME
		clock_t t3 = clock();
		low_constraint_time += (double)(t3-t2)/(double)CLOCKS_PER_SEC;
#endif
		SetJointVel(jvel);
//		cerr << fb_high - Jhigh * jvel << endl;
	}
	if(current_max_condnum < 0.0) current_max_condnum = 1.0;
	return current_max_condnum;
}

int IKConstraint::count_constraints()
{
	i_const = -1;
	if(enabled)
	{
		i_const = ik->n_const[priority];
		ik->n_const[priority] += n_const;
		ik->n_all_const += n_const;
	}
	return 0;
}

int IKConstraint::copy_jacobian()
{
	if(i_const < 0) return -1;
	int n_dof = ik->NumDOF();
	int i, j, m;
	is_dropped = false;
	for(m=0; m<IK::N_PRIORITY_TYPES; m++)
	{
		if(priority == (IK::Priority)m && enabled)
		{
			fMat& targetJ = ik->J[m];
			int target_row = targetJ.row();
			int row = J.row();
			double *a, *b;
			for(i=0; i<n_const; i++)
			{
				ik->fb[m](i_const+i) = fb(i);
				if(weight.size() == n_const)
					ik->weight[m](i_const+i) = weight(i);
				else
					ik->weight[m](i_const+i) = 1.0;
				a = targetJ.data() + i_const + i;
				b = J.data() + i;
				for(j=0; j<n_dof; a+=target_row, b+=row, j++)
				{
//					targetJ(i_const+i, j) = J(i, j);
					*a = *b;
				}
			}
			break;
		}
	}
	return 0;
}


