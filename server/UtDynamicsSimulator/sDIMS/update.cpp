/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*
 * update.cpp
 * Create: Katsu Yamane, 04.02.25
 */

#include "psim.h"
#include <limits>

//#define PSIM_TEST

//// memo: previous method (using inverse) is in version 1.61 

#ifdef VERBOSE
#include <fstream>
std::ofstream update_log("update.log");
#endif

#ifdef TIMING_CHECK
static double update_start_time = 0.0;
#endif

#ifdef PSIM_TEST
double max_condition_number = -1.0;
Joint* max_condition_number_joint = 0;
double max_sigma_ratio = -1.0;
Joint* max_sigma_ratio_joint = 0;
fVec condition_numbers;
fVec sigma_ratios;
double total_gamma_error = 0.0;
#endif

int pSim::Update()
{
#ifdef VERBOSE
	update_log << "Update no contact" << endl;
#endif
#ifdef TIMING_CHECK
	update_start_time = MPI_Wtime();
#endif
#ifdef PSIM_TEST
	max_condition_number = -1.0;
	max_sigma_ratio = -1.0;
	max_condition_number_joint = 0;
	max_sigma_ratio_joint = 0;
	condition_numbers.resize(n_dof);
	sigma_ratios.resize(n_dof);
	condition_numbers.zero();
	sigma_ratios.zero();
	total_gamma_error = 0.0;
#endif
	/**** assembly ****/
	// position-dependent variables
	update_position();
#if 1
	if(do_connect)
	{
		//
		// do collision computation if needed
		//
		update_collision();
		// don't forget to recompute link velocities
		CalcVelocity();
		do_connect = false;
	}
#endif
	// velocity-dependent variables
	update_velocity();

#ifdef TIMING_CHECK
	cerr << "[" << rank << "] disassembly t = " << MPI_Wtime()-update_start_time << endl;
#endif
	/**** disassembly ****/
	disassembly();
	
#ifdef PSIM_TEST
	cerr << "--- max condition number = " << max_condition_number << " at " << max_condition_number_joint->name << endl;
	cerr << "--- max sigma ratio = " << max_sigma_ratio << " at " << max_sigma_ratio_joint->name << endl;
	cerr << "--- total_gamma_error = " << total_gamma_error << endl;
//	cerr << "condition_numbers = " << tran(condition_numbers) << endl;
//	cerr << "sigma_ratios = " << tran(sigma_ratios) << endl;
#endif
#ifdef USE_MPI
	// scatter results
	scatter_acc();
#endif
	return 0;
}

#ifdef USE_MPI
void pSim::scatter_acc()
{
	int i;
	for(i=0; i<size; i++)
	{
		MPI_Bcast(MPI_BOTTOM, 1, all_acc_types[i], i, MPI_COMM_WORLD);
	}
	subchains->scatter_acc();
}

void pSubChain::scatter_acc()
{
	if(!this) return;
	if(last_joint && last_joint->t_given)
	{
		switch(last_joint->j_type)
		{
		case JROTATE:
		case JSLIDE:
			last_joint->SetJointAcc(acc_final(0));
//			cerr << last_joint->name << ": " << acc_final(0) << endl;
			break;
		case JSPHERE:
			last_joint->SetJointAcc(acc_final(0), acc_final(1), acc_final(2));
//			cerr << last_joint->name << ": " << tran(acc_final) << endl;
			break;
		case JFREE:
			last_joint->SetJointAcc(acc_final(0), acc_final(1), acc_final(2),
									acc_final(3), acc_final(4), acc_final(5));
//			cerr << last_joint->name << ": " << tran(acc_final) << endl;
			break;
		}
#if 0
		double* sendbuf;
		sendbuf = last_pjoints[0]->f_final.data();
		MPI_Bcast(sendbuf, 6, MPI_DOUBLE, rank, MPI_COMM_WORLD);
		sendbuf = last_pjoints[1]->f_final.data();
		MPI_Bcast(sendbuf, 6, MPI_DOUBLE, rank, MPI_COMM_WORLD);
#endif
	}
	if(children[0]) children[0]->scatter_acc();
	if(children[1] && children[0] != children[1]) children[1]->scatter_acc();
}
#endif

/**
 * position-dependent
 */
void pSim::update_position()
{
	int i;
	for(i=0; i<n_joint; i++)
	{
#ifdef USE_MPI
		if(joint_info[i].pjoints[0]->subchain && rank == joint_info[i].pjoints[0]->subchain->rank)
#endif
			joint_info[i].pjoints[0]->calc_jacobian();
#ifdef USE_MPI
		if(joint_info[i].pjoints[1]->subchain && rank == joint_info[i].pjoints[1]->subchain->rank)
#endif
			joint_info[i].pjoints[1]->calc_jacobian();
	}
	// should come after computing Jacobians of all pjoints
	subchains->calc_inertia();
}

void pJoint::calc_jacobian()
{
	if(parent_side)
	{
		static fVec3 rel_pos;
		static fMat33 rel_att;
		if(link_side == joint->real)
		{
			rel_pos.set(joint->rpos_real);
			rel_att.set(joint->ratt_real);
		}
		else
		{
			rel_pos.set(joint->rel_pos);
			rel_att.set(joint->rel_att);
		}
		static fMat33 tcross, tmpT;
		tcross.cross(rel_pos);
		tmpT.mul(tcross, rel_att);
		J(0,0) = rel_att(0,0), J(0,1) = rel_att(1,0), J(0,2) = rel_att(2,0);
		J(1,0) = rel_att(0,1), J(1,1) = rel_att(1,1), J(1,2) = rel_att(2,1);
		J(2,0) = rel_att(0,2), J(2,1) = rel_att(1,2), J(2,2) = rel_att(2,2);
		J(0,3) = tmpT(0,0), J(0,4) = tmpT(1,0), J(0,5) = tmpT(2,0);
		J(1,3) = tmpT(0,1), J(1,4) = tmpT(1,1), J(1,5) = tmpT(2,1);
		J(2,3) = tmpT(0,2), J(2,4) = tmpT(1,2), J(2,5) = tmpT(2,2);
		J(3,0) = 0.0, J(3,1) = 0.0, J(3,2) = 0.0;
		J(4,0) = 0.0, J(4,1) = 0.0, J(4,2) = 0.0;
		J(5,0) = 0.0, J(5,1) = 0.0, J(5,2) = 0.0;
		J(3,3) = rel_att(0,0), J(3,4) = rel_att(1,0), J(3,5) = rel_att(2,0);
		J(4,3) = rel_att(0,1), J(4,4) = rel_att(1,1), J(4,5) = rel_att(2,1);
		J(5,3) = rel_att(0,2), J(5,4) = rel_att(1,2), J(5,5) = rel_att(2,2);
//		update_log << joint->name << ": J = " << J << endl;
	}
}

void pSubChain::calc_inertia()
{
	if(!this) return;
	if(!last_joint)  // single link
	{
		calc_inertia_leaf();
	}
	else  // have last joint
	{
		// process children first
		children[0]->calc_inertia();
		if(children[1] != children[0])
		{
			children[1]->calc_inertia();
		}
		calc_inertia_body();
	}
}

#ifdef USE_MPI
void pSubChain::recv_inertia()
{
	// recv from subchain's rank
	MPI_Status status;
#if 0
	int i, j;
	for(i=0; i<n_outer_joints; i++)
	{
		for(j=i; j<n_outer_joints; j++)
		{
			double* buf = Lambda[i][j].data();
			MPI_Recv(buf, 36, MPI_DOUBLE, rank, PSIM_TAG_LAMBDA, MPI_COMM_WORLD, &status);
			if(i != j)
			{
				Lambda[j][i].tran(Lambda[i][j]);
			}
		}
	}
#else
#ifdef TIMING_CHECK
	double time1 = MPI_Wtime();
#endif
	MPI_Recv(MPI_BOTTOM, 1, parent_lambda_type, rank, PSIM_TAG_LAMBDA, MPI_COMM_WORLD, &status);
#ifdef TIMING_CHECK
	double time2 = MPI_Wtime();
	sim->inertia_wait_time += time2-time1;
#endif
#endif
}

void pSubChain::send_inertia(int dest)
{
	// send to dest
#if 0
	int i, j;
	for(i=0; i<n_outer_joints; i++)
	{
		for(j=i; j<n_outer_joints; j++)
		{
			double* buf = Lambda[i][j].data();
			MPI_Send(buf, 36, MPI_DOUBLE, dest, PSIM_TAG_LAMBDA, MPI_COMM_WORLD);
		}
	}
#else
	MPI_Send(MPI_BOTTOM, 1, parent_lambda_type, dest, PSIM_TAG_LAMBDA, MPI_COMM_WORLD);
#endif
}
#endif

void pSubChain::calc_inertia_leaf()
{
#ifdef USE_MPI
	if(sim->rank != rank) return;
#endif
	int i, j;
	// Lambda
//	update_log << links[0]->joint->name << ": calc_inertia_leaf" << endl;
	for(i=0; i<n_outer_joints; i++)
	{
		static fMat JM(6, 6);
		if(outer_joints[i]->parent_side) // link is parent side -> compute JM
		{
			JM.mul(outer_joints[i]->J, outer_joints[i]->plink->Minv);
		}
		else // link is child side -> J is identity
		{
			JM.set(outer_joints[i]->plink->Minv);
		}
		for(j=i; j<n_outer_joints; j++)  // half only
		{
			static fMat tJ(6, 6);
			if(outer_joints[j]->parent_side)
			{
				tJ.tran(outer_joints[j]->J);
				Lambda[i][j].mul(JM, tJ);
			}
			else
			{
				Lambda[i][j].set(JM);
			}
//			update_log << "Lambda[" << i << "][" << j << "] = " << Lambda[i][j] << endl;
			if(i != j)  // copy transpose
				Lambda[j][i].tran(Lambda[i][j]);
		}
	}
#ifdef USE_MPI
	if(parent && sim->rank != parent->rank)
	{
//		cerr << sim->rank << ": link (" << links[0]->joint->name << "): send_inertia to " << parent->rank << endl;
		send_inertia(parent->rank);
	}
#endif
}

void pSubChain::calc_inertia_body()
{
	// for space
//	if(!children[1]) return;
#ifdef USE_MPI
	if(sim->rank != rank) return;
	if(children[0] && sim->rank != children[0]->rank)
	{
//		cerr << sim->rank << ": joint (" << last_joint->name << "): recv_inertia from " << children[0]->rank << endl;
		children[0]->recv_inertia();
	}
	if(children[1] && children[0] != children[1] && sim->rank != children[1]->rank)
	{
//		cerr << sim->rank << ": joint (" << last_joint->name << "): recv_inertia from " << children[1]->rank << endl;
		children[1]->recv_inertia();
	}
#endif
	int i, j;
//	cerr << "---- " << last_joint->name << ": calc_inertia_body" << endl;
	// P
	if(children[1])
	{
		P.add(children[0]->Lambda[last_index[0]][last_index[0]], children[1]->Lambda[last_index[1]][last_index[1]]);
	}
	else
	{
		P.set(children[0]->Lambda[last_index[0]][last_index[0]]);
	}
//	cerr << "Lambda[0] = " << children[0]->Lambda[last_index[0]][last_index[0]] << endl;
//	cerr << "Lambda[1] = " << children[1]->Lambda[last_index[1]][last_index[1]] << endl;
#ifdef PSIM_TEST
	{
		fMat U1(6,6), V1T(6,6), U2(6,6), V2T(6,6);
		fVec sigma1(6), sigma2(6);
		children[0]->Lambda[last_index[0]][last_index[0]].svd(U1, sigma1, V1T);
		children[1]->Lambda[last_index[1]][last_index[1]].svd(U2, sigma2, V2T);
		double s1 = sigma1.length(), s2 = sigma2.length();
		if(s1 > 1e-8 && s2 > 1e-8)
		{
			double ratio = (s1 < s2) ? s2/s1 : s1/s2;
			if(max_sigma_ratio < 0.0 || ratio > max_sigma_ratio)
			{
				max_sigma_ratio = ratio;
				max_sigma_ratio_joint = last_joint;
			}
			sigma_ratios(last_joint->i_dof) =  ratio;
//			cerr << last_joint->name << ": " << s1 << ", " << s2 << " -> " << ratio << endl;
		}
	}
#endif
	if(children[0] == children[1])
	{
		P -= children[0]->Lambda[last_index[0]][last_index[1]];
		P -= children[0]->Lambda[last_index[1]][last_index[0]];
	}
	// P should be symmetric
//	P += tran(P);
//	P *= 0.5;
	P.symmetric();
#ifndef USE_DCA
	// Gamma
	if(n_const > 0)
	{
		for(i=0; i<n_const; i++)
		{
			for(j=0; j<n_const; j++)
				Gamma(i, j) = P(const_index[i], const_index[j]);
		}
		if(children[0] != children[1])
		{
			// Gamma is symmetric, positive-definite
			if(Gamma_inv.inv_posv(Gamma))
				Gamma_inv.inv_svd(Gamma);
//			Gamma_inv.inv_porfs(Gamma);
#ifdef PSIM_TEST
			fMat U(n_const, n_const), VT(n_const, n_const);
			fVec sigma(n_const);
			Gamma.svd(U, sigma, VT);
			double cn = sigma(0) / sigma(n_const-1);
			if(max_condition_number < 0.0 || cn > max_condition_number)
			{
				max_condition_number = cn;
				max_condition_number_joint = last_joint;
			}
			condition_numbers(last_joint->i_dof) = cn;
//			cerr << "condition_number = " << cn << endl;
//			fMat Gamma_inv2(n_const, n_const);
//			Gamma_inv2.inv_svd(Gamma, 2000);
//			cerr << last_joint->name << ": " << cn << endl;
//			fMat I(n_const, n_const);
//			I.identity();
//			cerr << last_joint->name << ": " << I - Gamma*Gamma_inv << endl;
//			cerr << last_joint->name << ": " << Gamma_inv - Gamma_inv2 << endl;
#endif
		}
		else
		{
			// Gamma may be singular
			Gamma_inv.inv_svd(Gamma);
//			cerr << Gamma_inv * Gamma << endl;
		}
	}
	// W, IW
	W.zero();
	for(i=0; i<n_const; i++)
	{
		for(int j=0; j<n_const; j++)
		{
			W(const_index[i], const_index[j]) = -Gamma_inv(i, j);
		}
	}
#else // #ifndef USE_DCA
	static fMat SV, VSV;
	SV.resize(n_dof, 6);
	VSV.resize(n_dof, 6);
	Vhat.inv_posv(P);
	if(n_dof > 0)
	{
		for(i=0; i<n_dof; i++)
		{
			for(int j=0; j<n_dof; j++)
			{
				SVS(i,j) = Vhat(joint_index[i], joint_index[j]);
			}
			for(int j=0; j<6; j++)
			{
				SV(i,j) = Vhat(joint_index[i], j);
			}
		}
//		cerr << "SVS = " << SVS << endl;
		VSV.lineq(SVS, SV);
//		cerr << "VSV = " << VSV << endl;
		W.mul(tran(SV), VSV);
//		W *= -1.0;
	}
	else
	{
		W.zero();
	}
	W -= Vhat;
#endif
//	cerr << "P = " << P << endl;
//	cerr << "W = " << W << endl;
	IW.mul(P, W);
	for(i=0; i<6; i++)
	{
		IW(i, i) += 1.0;
	}
//	cerr << "IW = " << IW << endl;
	// Lambda
	if(n_const == 0)
	{
		for(i=0; i<n_outer_joints; i++)
		{
			int org_i = outer_joints_origin[i];
			int index_i = outer_joints_index[i];
			for(j=i; j<n_outer_joints; j++)
			{
				int org_j = outer_joints_origin[j];
				int index_j = outer_joints_index[j];
				if(children[org_i] == children[org_j])
				{
					Lambda[i][j].set(children[org_i]->Lambda[index_i][index_j]);
				}
				if(i != j)
				{
					Lambda[j][i].tran(Lambda[i][j]);
				}
			}
		}
	}
	else
	{
		for(i=0; i<n_outer_joints; i++)
		{
			int org_i = outer_joints_origin[i];
			int index_i = outer_joints_index[i];
			fMat& Lambda_i = children[org_i]->Lambda[index_i][last_index[org_i]];
#ifndef USE_DCA
			int m, n;
			static fMat LKi, KLj, GKLj;
			LKi.resize(6, n_const);
			KLj.resize(n_const, 6);
			GKLj.resize(n_const, 6);
			for(m=0; m<6; m++)
			{
				for(n=0; n<n_const; n++)
					LKi(m, n) = Lambda_i(m, const_index[n]);
			}
			for(j=i; j<n_outer_joints; j++)
			{
				int org_j = outer_joints_origin[j];
				int index_j = outer_joints_index[j];
				fMat& Lambda_j = children[org_j]->Lambda[last_index[org_j]][index_j];
				for(m=0; m<n_const; m++)
				{
					for(n=0; n<6; n++)
						KLj(m, n) = Lambda_j(const_index[m], n);
				}
				GKLj.mul(Gamma_inv, KLj);
//				GKLj.lineq_posv(Gamma, KLj);
				Lambda[i][j].mul(LKi, GKLj);
				if(children[org_i] == children[org_j])
				{
					Lambda[i][j] -= children[org_i]->Lambda[index_i][index_j];
					Lambda[i][j] *= -1.0;
				}
				if(i != j)
				{
					Lambda[j][i].tran(Lambda[i][j]);
				}
				else
				{
					Lambda[i][j].symmetric();
				}
			}
#else  // #ifndef USE_DCA
			for(j=i; j<n_outer_joints; j++)
			{
				int org_j = outer_joints_origin[j];
				int index_j = outer_joints_index[j];
				fMat& Lambda_j = children[org_j]->Lambda[last_index[org_j]][index_j];
				static fMat WL(6,6);
				WL.mul(W, Lambda_j);
				WL *= -1.0;
				Lambda[i][j].mul(Lambda_i, WL);
				if(children[org_i] == children[org_j])
				{
					Lambda[i][j] -= children[org_i]->Lambda[index_i][index_j];
					Lambda[i][j] *= -1.0;
				}
				if(i != j)
				{
					Lambda[j][i].tran(Lambda[i][j]);
				}
				else
				{
					Lambda[i][j].symmetric();
				}
			}
#endif
		}
	}
#ifdef USE_MPI
	if(parent && sim->rank != parent->rank)
	{
//		cerr << sim->rank << ": joint (" << last_joint->name << "): send_inertia to " << parent->rank << endl;
		send_inertia(parent->rank);
	}
#endif
}

/**
 * collision
 */
void pSim::update_collision()
{
	calc_dvel();
	col_disassembly();
}

void pSim::calc_dvel()
{
	int i;
	for(i=0; i<n_joint; i++)
	{
		joint_info[i].pjoints[0]->calc_dvel();
		joint_info[i].pjoints[1]->calc_dvel();
	}
	subchains->calc_dvel();
}

void pJoint::calc_dvel()
{
	if(parent_side)
	{
		static fVec v(6);
		v(0) = link_side->loc_lin_vel(0);
		v(1) = link_side->loc_lin_vel(1);
		v(2) = link_side->loc_lin_vel(2);
		v(3) = link_side->loc_ang_vel(0);
		v(4) = link_side->loc_ang_vel(1);
		v(5) = link_side->loc_ang_vel(2);
		dvel.mul(J, v);
	}
	else
	{
		dvel(0) = joint->loc_lin_vel(0);
		dvel(1) = joint->loc_lin_vel(1);
		dvel(2) = joint->loc_lin_vel(2);
		dvel(3) = joint->loc_ang_vel(0);
		dvel(4) = joint->loc_ang_vel(1);
		dvel(5) = joint->loc_ang_vel(2);
	}
}

void pSubChain::calc_dvel()
{
	if(!this) return;
	if(!last_joint)  // single link
	{
		calc_dvel_leaf();
	}
	else  // have last joint
	{
		// process children first
		children[0]->calc_dvel();
		if(children[1] != children[0]) children[1]->calc_dvel();
		calc_dvel_body();
	}
}

void pSubChain::calc_dvel_leaf()
{
	int i;
	for(i=0; i<n_outer_joints; i++)
	{
		vel_temp[i].set(outer_joints[i]->dvel);
	}
}

void pSubChain::calc_dvel_body()
{
	if(!children[1]) return;
	int i, j;
	// compute f_temp
	static fVec dv6(6), dv;
	static fMat PK;
	PK.resize(6, n_dof);
	dv.resize(n_const);
	for(i=0; i<6; i++)
	{
		for(j=0; j<n_dof; j++)
			PK(i, j) = P(i, joint_index[j]);
	}
	// + child_side - parent_side ?
	dv6.sub(children[0]->vel_temp[last_index[0]], children[1]->vel_temp[last_index[1]]);
	// actually we could save some computation by
	// selecting const rows first
	for(i=0; i<n_const; i++)
		dv(i) = -dv6(const_index[i]);
#ifndef USE_DCA  // TODO: allow dca
	colf_temp.mul(Gamma_inv, dv);
#endif
	// compute new velocity at all outer joints
	static fVec f(6);
	f.zero();
	for(i=0; i<n_const; i++)
		f(const_index[i]) = colf_temp(i);
	for(i=0; i<n_outer_joints; i++)
	{
		int org = outer_joints_origin[i];
		int index = outer_joints_index[i];
		int ilast = last_index[org];
		vel_temp[i].mul(children[org]->Lambda[index][ilast], f);
		if(org == 1)
		{
			vel_temp[i] *= -1.0;
		}
		vel_temp[i] += children[org]->vel_temp[index];
	}
}

void pSim::col_disassembly()
{
	subchains->col_disassembly();
}

void pSubChain::col_disassembly()
{
	if(!this) return;
	if(last_joint)
	{
		col_disassembly_body();
		// process children last
		children[0]->col_disassembly();
		if(children[1] != children[0]) children[1]->col_disassembly();
	}
}

void pSubChain::col_disassembly_body()
{
	// for space
	if(!children[1]) return;
	int i;
	// compute final constraint force
	static fVec KLf, Lf(6), Lf_temp[2], v(6), colf, colf_final(6);
	KLf.resize(n_const);
	Lf_temp[0].resize(6);
	Lf_temp[1].resize(6);
	colf.resize(n_const);
	Lf_temp[0].zero();
	Lf_temp[1].zero();
	for(i=0; i<n_outer_joints; i++)
	{
		int org = outer_joints_origin[i];
		int index = outer_joints_index[i];
		fMat& Lambda_i = children[org]->Lambda[last_index[org]][index];
		// first multiply and increment
		v.mul(Lambda_i, children[org]->outer_joints[index]->colf_final);
		Lf_temp[org] += v;
	}
	Lf.sub(Lf_temp[0], Lf_temp[1]);
	// then extract rows
	for(i=0; i<n_const; i++)
		KLf(i) = Lf(const_index[i]);
#ifndef USE_DCA  // TODO: allow DCA
	colf.mul(Gamma_inv, KLf);
#endif
	colf_temp -= colf;
	colf_final.zero();
	for(i=0; i<n_const; i++)
		colf_final(const_index[i]) = colf_temp(i);
	last_pjoints[0]->colf_final.set(colf_final);
	last_pjoints[1]->colf_final.neg(colf_final);
	// compute link velocities
	last_pjoints[0]->vel_final.mul(children[0]->Lambda[last_index[0]][last_index[0]], last_pjoints[0]->colf_final);
	last_pjoints[0]->vel_final += Lf_temp[0];
	last_pjoints[0]->vel_final += children[0]->vel_temp[last_index[0]];
	if(children[1])
	{
		last_pjoints[1]->vel_final.mul(children[1]->Lambda[last_index[1]][last_index[1]], last_pjoints[1]->colf_final);
		last_pjoints[1]->vel_final += Lf_temp[1];
		last_pjoints[1]->vel_final += children[1]->vel_temp[last_index[1]];
	}
	if(children[0] == children[1])
	{
		v.mul(children[1]->Lambda[last_index[0]][last_index[1]], last_pjoints[1]->colf_final);
		last_pjoints[0]->vel_final += v;
		v.mul(children[0]->Lambda[last_index[1]][last_index[0]], last_pjoints[0]->colf_final);
		last_pjoints[1]->vel_final += v;
	}
	// compute joint velocity
	v.sub(last_pjoints[0]->vel_final, last_pjoints[1]->vel_final);
//	cerr << last_joint->name << ": v = " << tran(v) << endl;
	switch(last_joint->j_type)
	{
	case JROTATE:
	case JSLIDE:
		last_joint->SetJointVel(v(axis));
		break;
	case JSPHERE:
		last_joint->SetJointVel(fVec3(v(3), v(4), v(5)));
		break;
	case JFREE:
		last_joint->SetJointVel(fVec3(v(0), v(1), v(2)), fVec3(v(3), v(4), v(5)));
		break;
	default:
		break;
	}
}

/**
 * velocity-dependent
 */
void pSim::update_velocity()
{
	int i;
	for(i=0; i<n_joint; i++)
	{
#ifdef USE_MPI
		if(joint_info[i].pjoints[0]->subchain && rank == joint_info[i].pjoints[0]->subchain->rank)
#endif
		{
			joint_info[i].pjoints[0]->calc_jdot();
		}
#ifdef USE_MPI
		if(joint_info[i].pjoints[1]->subchain && rank == joint_info[i].pjoints[1]->subchain->rank)
#endif
		{
			joint_info[i].pjoints[1]->calc_jdot();
		}
		if(joint_info[i].plink
#ifdef USE_MPI
		   && joint_info[i].plink->subchain &&
		   rank == joint_info[i].plink->subchain->rank
#endif
		   )
		{
			joint_info[i].plink->calc_acc(Root()->loc_lin_acc);
		}
	}
	// should come after computing Jacobians of all pjoints
	subchains->calc_acc();
#ifdef TIMING_CHECK
	cerr << "[" << rank << "] update_velocity end t = " << MPI_Wtime()-update_start_time << endl;
#endif
}

void pJoint::calc_jdot()
{
	if(parent_side)
	{
		static fVec3 v1, v2, v3;
		static fVec3 rel_pos;
		static fMat33 rel_att;
		if(link_side == joint->real)
		{
			rel_pos.set(joint->rpos_real);
			rel_att.set(joint->ratt_real);
		}
		else
		{
			rel_pos.set(joint->rel_pos);
			rel_att.set(joint->rel_att);
		}
		v1.cross(link_side->loc_ang_vel, rel_pos);
		v2.cross(link_side->loc_ang_vel, v1);
		v3.mul(v2, rel_att);

//		if(!joint->real)
		{
//			v1.set(joint->loc_ang_vel);
			v1.mul(link_side->loc_ang_vel, rel_att);
			// linear joint motion
			v2.cross(v1, joint->rel_lin_vel);
			v2 *= 2.0;
			v3 += v2;
			// angular joint motion
			v2.cross(v1, joint->rel_ang_vel);
		}

		Jdot(0) = v3(0);
		Jdot(1) = v3(1);
		Jdot(2) = v3(2);
		Jdot(3) = v2(0);
		Jdot(4) = v2(1);
		Jdot(5) = v2(2);
//		cerr << joint->name << ": jdot = " << tran(Jdot) << endl;
//		cerr << "rel_pos = " << rel_pos << endl;
//		cerr << "rel_lin_vel = " << joint->rel_lin_vel << endl;
//		cerr << "rel_ang_vel = " << joint->rel_ang_vel << endl;
	}
	else // child side
	{
	}
}

void pLink::calc_acc(const fVec3& g0)
{
	static fVec3 a, c1, c2, g;
	static fVec3 v1, v2;
	g.mul(g0, joint->abs_att);
	// nonlinear acc
	v1.cross(joint->loc_ang_vel, joint->loc_com);
	a.cross(joint->loc_ang_vel, v1);
	a += g;
	// c1
	c1.mul(joint->mass, a);
	// c2
	v1.mul(joint->inertia, joint->loc_ang_vel);
	v2.cross(joint->loc_ang_vel, v1);
	c2.cross(joint->loc_com, c1);
	c2 += v2;
	// external force/moment around com
	c1 -= joint->ext_force;
	c2 -= joint->ext_moment;
#ifdef VERBOSE
	update_log << joint->name << ": external force/moment = " << joint->ext_force << "/" << joint->ext_moment << endl;
#endif
//	v1.cross(joint->ext_force, joint->loc_com);
//	c2 -= v1;
	// -c
	c(0) = -c1(0);
	c(1) = -c1(1);
	c(2) = -c1(2);
	c(3) = -c2(0);
	c(4) = -c2(1);
	c(5) = -c2(2);
	// bias acc
	acc.mul(Minv, c);
//	cerr << joint->name << ": acc = " << tran(acc) << endl;
}

void pSubChain::calc_acc()
{
	if(!this) return;
	if(!last_joint)  // single link
	{
		calc_acc_leaf();
	}
	else  // have last joint
	{
		// process children first
		children[0]->calc_acc();
		if(children[1] != children[0])
		{
			children[1]->calc_acc();
		}
		calc_acc_body();
	}
}

#ifdef USE_MPI
void pSubChain::recv_acc()
{
	MPI_Status status;
#if 0
	int i;
	for(i=0; i<n_outer_joints; i++)
	{
		double* buf = acc_temp[i].data();
		MPI_Recv(buf, 6, MPI_DOUBLE, rank, PSIM_TAG_ACC, MPI_COMM_WORLD, &status);
	}
#else
#ifdef TIMING_CHECK
	double time1 = MPI_Wtime();
	cerr << "[" << sim->rank << "] recv_acc(0) t = " << MPI_Wtime()-update_start_time << endl;
#endif
	MPI_Recv(MPI_BOTTOM, 1, parent_acc_type, rank, PSIM_TAG_ACC, MPI_COMM_WORLD, &status);
#ifdef TIMING_CHECK
	cerr << "[" << sim->rank << "] recv_acc(1) t = " << MPI_Wtime()-update_start_time << endl;
	double time2 = MPI_Wtime();
	sim->acc_wait_time += time2-time1;
#endif
#endif
}

void pSubChain::send_acc(int dest)
{
#if 0
	int i;
	for(i=0; i<n_outer_joints; i++)
	{
		double* buf = acc_temp[i].data();
		MPI_Send(buf, 6, MPI_DOUBLE, dest, PSIM_TAG_ACC, MPI_COMM_WORLD);
	}
#else
#ifdef TIMING_CHECK
	cerr << "[" << sim->rank << "] send_acc(0) t = " << MPI_Wtime()-update_start_time << endl;
#endif
	MPI_Send(MPI_BOTTOM, 1, parent_acc_type, dest, PSIM_TAG_ACC, MPI_COMM_WORLD);
#ifdef TIMING_CHECK
	cerr << "[" << sim->rank << "] send_acc(1) t = " << MPI_Wtime()-update_start_time << endl;
#endif
#endif
}
#endif

void pSubChain::calc_acc_leaf()
{
#ifdef USE_MPI
	if(sim->rank != rank) return;
#endif
	// compute bias acc at all outer joints
//	update_log << "--- " << links[0]->joint->name << ": calc_acc_leaf" << endl;
	int i;
	for(i=0; i<n_outer_joints; i++)
	{
		if(outer_joints[i]->parent_side)
		{
			acc_temp[i].mul(outer_joints[i]->J, links[0]->acc);
			acc_temp[i] += outer_joints[i]->Jdot;
		}
		else
		{
//			acc_temp[i].set(links[0]->acc);
			acc_temp[i].add(links[0]->acc, outer_joints[i]->Jdot);
		}
//		update_log << "acc_temp[" << i << "] = " << tran(acc_temp[i]) << endl;
	}
#ifdef USE_MPI
	if(parent && sim->rank != parent->rank)
	{
		send_acc(parent->rank);
	}
#endif
}

void pSubChain::calc_acc_body()
{
//	if(!children[1]) return;
#ifdef USE_MPI
	if(sim->rank != rank) return;
	if(children[0] && sim->rank != children[0]->rank)
	{
		children[0]->recv_acc();
	}
	if(children[1] && children[0] != children[1] && sim->rank != children[1]->rank)
	{
		children[1]->recv_acc();
	}
#endif
//	update_log << "--- " << last_joint->name << ": calc_acc_body" << endl;
	int i, j;
	// compute f_temp
	static fVec da;
	static fMat PK;
	PK.resize(6, n_dof);
	da.resize(n_const);
	for(i=0; i<6; i++)
	{
		for(j=0; j<n_dof; j++)
			PK(i, j) = P(i, joint_index[j]);
	}
	if(last_joint->n_dof > 0)
	{
		switch(last_joint->j_type)
		{
		case JROTATE:
		case JSLIDE:
			tau(0) = last_joint->tau;
			break;
		case JSPHERE:
			tau(0) = last_joint->tau_n(0);
			tau(1) = last_joint->tau_n(1);
			tau(2) = last_joint->tau_n(2);
			break;
		case JFREE:
			tau(0) = last_joint->tau_f(0);
			tau(1) = last_joint->tau_f(1);
			tau(2) = last_joint->tau_f(2);
			tau(3) = last_joint->tau_n(0);
			tau(4) = last_joint->tau_n(1);
			tau(5) = last_joint->tau_n(2);
			break;
		default:
			break;
		}
		da6.mul(PK, tau);
	}
	else
		da6.zero();
//	cerr << "da6(0) = " << tran(da6) << endl;
	// + child_side - parent_side ?
	da6 += children[0]->acc_temp[last_index[0]];
//	cerr << "da6(1) = " << tran(da6) << endl;
	if(children[1])
		da6 -= children[1]->acc_temp[last_index[1]];
//	cerr << "da6(2) = " << tran(da6) << endl;
	// motion controlled joints
	if(!last_joint->t_given)
	{
		switch(last_joint->j_type)
		{
		case JROTATE:
		case JSLIDE:
			da6(axis) -= last_joint->qdd;
//			update_log << last_joint->name << ": qdd = " << last_joint->qdd << endl;
			break;
		case JSPHERE:
			da6(3) -= last_joint->rel_ang_acc(0);
			da6(4) -= last_joint->rel_ang_acc(1);
			da6(5) -= last_joint->rel_ang_acc(2);
			break;
		case JFREE:
			da6(0) -= last_joint->rel_lin_acc(0);
			da6(1) -= last_joint->rel_lin_acc(1);
			da6(2) -= last_joint->rel_lin_acc(2);
			da6(3) -= last_joint->rel_ang_acc(0);
			da6(4) -= last_joint->rel_ang_acc(1);
			da6(5) -= last_joint->rel_ang_acc(2);
			break;
		default:
		        break;
		}
	}
	static fVec f(6);
//	cerr << "Gamma = " << Gamma << endl;
//	cerr << "Gamma_inv = " << Gamma_inv << endl;
#if 0
	// actually we could save some computation by
	// selecting const rows first
	for(i=0; i<n_const; i++)
		da(i) = -da6(const_index[i]);
	f_temp.mul(Gamma_inv, da);
//	f_temp.lineq_posv(Gamma, da);
	// compute acc at all outer joints
	for(i=0; i<n_dof; i++)
		f(joint_index[i]) = tau(i);
	for(i=0; i<n_const; i++)
		f(const_index[i]) = f_temp(i);
//	cerr << "da = " << tran(da) << endl;
//	cerr << "f_temp = " << tran(f_temp) << endl;
//	cerr << "Gamma*f_temp - da = " << tran(Gamma*f_temp-da) << endl;
#else
#if 0
	f.mul(W, da6);
	for(i=0; i<n_dof; i++)
	{
		f(joint_index[i]) += tau(i);
	}
#else
	static fVec db(6), Wdb(6), IWRtau(6);
	static fMat IWR;
	IWR.resize(6, n_dof);
	db.set(children[0]->acc_temp[last_index[0]]);
	if(children[1])
		db -= children[1]->acc_temp[last_index[1]];
	Wdb.mul(W, db);
	for(i=0; i<6; i++)
	{
		for(j=0; j<n_dof; j++)
		{
			IWR(i, j) = IW(joint_index[j], i);
		}
	}
	IWRtau.mul(IWR, tau);
//	cerr << "W = " << tran(W) << endl;
//	update_log << "db = " << tran(db) << endl;
//	cerr << "Wdb = " << tran(Wdb) << endl;
//	cerr << "IWRtau = " << tran(IWRtau) << endl;
	f.add(Wdb, IWRtau);
//	update_log << "f = " << tran(f) << endl;
	
#ifdef PSIM_TEST
	////// -> test
	for(i=0; i<n_const; i++)
	{
		da(i) = -da6(const_index[i]);
		f_temp(i) = f(const_index[i]);
	}
//	cerr << "Gamma*f_temp - da = " << tran(Gamma*f_temp-da) << endl;
	total_gamma_error += (Gamma*f_temp-da) * (Gamma*f_temp-da);
	////// <-
#endif
#endif
#endif
	for(i=0; i<n_outer_joints; i++)
	{
		int org = outer_joints_origin[i];
		int index = outer_joints_index[i];
		int ilast = last_index[org];
		acc_temp[i].mul(children[org]->Lambda[index][ilast], f);
		if(org == 1)
		{
			acc_temp[i] *= -1.0;
		}
		acc_temp[i] += children[org]->acc_temp[index];
//		update_log << "acc_temp[" << i << "] = " << tran(acc_temp[i]) << endl;
	}
#ifdef USE_MPI
	if(parent && sim->rank != parent->rank)
	{
		send_acc(parent->rank);
	}
#endif
}

/**
 * disassembly
 */
void pSim::disassembly()
{
	subchains->disassembly();
}

void pSubChain::disassembly()
{
	if(!this) return;
	if(last_joint)
	{
		disassembly_body();
		// process children last
		children[0]->disassembly();
		if(children[1] != children[0]) children[1]->disassembly();
	}
	else 
	{
//		disassembly_leaf();
	}
}

#ifdef USE_MPI
void pSubChain::recv_force()
{
	MPI_Status status;
#if 0
	int i;
	for(i=0; i<n_outer_joints; i++)
	{
		double* buf = outer_joints[i]->f_final.data();
		MPI_Recv(buf, 6, MPI_DOUBLE, rank, PSIM_TAG_FORCE, MPI_COMM_WORLD, &status);
	}
	double* buf;
	buf = last_pjoints[0]->f_final.data();
	MPI_Recv(buf, 6, MPI_DOUBLE, rank, PSIM_TAG_FORCE, MPI_COMM_WORLD, &status);
	buf = last_pjoints[1]->f_final.data();
	MPI_Recv(buf, 6, MPI_DOUBLE, rank, PSIM_TAG_FORCE, MPI_COMM_WORLD, &status);
#else
#ifdef TIMING_CHECK
	double time1 = MPI_Wtime();
#endif
	MPI_Recv(MPI_BOTTOM, 1, parent_force_type, rank, PSIM_TAG_FORCE, MPI_COMM_WORLD, &status);
#ifdef TIMING_CHECK
	double time2 = MPI_Wtime();
	sim->force_wait_time += time2-time1;
#endif
#endif
}

void pSubChain::send_force(int dest)
{
#if 0
	int i;
	for(i=0; i<n_outer_joints; i++)
	{
		double* buf = outer_joints[i]->f_final.data();
		MPI_Send(buf, 6, MPI_DOUBLE, dest, PSIM_TAG_FORCE, MPI_COMM_WORLD);
	}
	double* buf;
	buf = last_pjoints[0]->f_final.data();
	MPI_Send(buf, 6, MPI_DOUBLE, dest, PSIM_TAG_FORCE, MPI_COMM_WORLD);
	buf = last_pjoints[1]->f_final.data();
	MPI_Send(buf, 6, MPI_DOUBLE, dest, PSIM_TAG_FORCE, MPI_COMM_WORLD);
#else
	MPI_Send(MPI_BOTTOM, 1, parent_force_type, dest, PSIM_TAG_FORCE, MPI_COMM_WORLD);
#endif
}
#endif

void pSubChain::disassembly_leaf()
{
	Joint* target_joint = links[0]->joint;
//	if(!target_joint->parent) return;  // skip space
	// convert all forces/moments to joint frame
	cerr << "---- " << target_joint->name << ": disassembly_leaf" << endl;
	int i;
	static fVec3 total_f, total_n;
	static fVec3 pos;
	static fMat33 att, t_att;
	static fVec acc(6);
	static fVec allf(6);
	total_f.zero();
	total_n.zero();
	acc.zero();
	pos.set(target_joint->abs_pos);
	att.set(target_joint->abs_att);
	t_att.tran(att);
	for(i=0; i<n_outer_joints; i++)
	{
		static fVec3 loc_f, loc_n, f, n, fn;
		static fVec3 jpos, rel_pos, pp;
		static fMat33 jatt, rel_att;
		cerr << "outer[" << i << "]: " << outer_joints[i]->joint->name << endl;
		cerr << "f_final = " << tran(outer_joints[i]->f_final) << endl;
		loc_f(0) = outer_joints[i]->f_final(0);
		loc_f(1) = outer_joints[i]->f_final(1);
		loc_f(2) = outer_joints[i]->f_final(2);
		loc_n(0) = outer_joints[i]->f_final(3);
		loc_n(1) = outer_joints[i]->f_final(4);
		loc_n(2) = outer_joints[i]->f_final(5);
		jpos.set(outer_joints[i]->joint->abs_pos);
		jatt.set(outer_joints[i]->joint->abs_att);
		pp.sub(jpos, pos);
		rel_pos.mul(t_att, pp);
		rel_att.mul(t_att, jatt);
		f.mul(rel_att, loc_f);  // force
		n.mul(rel_att, loc_n);
		fn.cross(rel_pos, f);
		n += fn;
		cerr << "(f n) = " << f << n << endl;
		total_f += f;
		total_n += n;
	}
	allf(0) = total_f(0);
	allf(1) = total_f(1);
	allf(2) = total_f(2);
	allf(3) = total_n(0);
	allf(4) = total_n(1);
	allf(5) = total_n(2);
	cerr << "total_f = " << total_f << endl;
	cerr << "total_n = " << total_n << endl;
	acc.lineq_posv(links[0]->M, allf);
	acc += links[0]->acc;
	cerr << "Minv = " << links[0]->Minv << endl;
	cerr << "acc = " << tran(links[0]->acc) << endl;
	cerr << "link acc = " << tran(acc) << endl;
}

void pSubChain::disassembly_body()
{
	// for space
	if(!children[1]) return;
#ifdef VERBOSE
	update_log << "disassembly_body" << endl;
#endif
#ifdef USE_MPI
	if(sim->rank != rank) return;
	if(parent && sim->rank != parent->rank)
	{
#ifdef TIMING_CHECK
		cerr << "[" << sim->rank << "] " << last_joint->name << ": recv force from " << parent->last_joint->name << " [" << parent->rank << "] t = " << MPI_Wtime()-update_start_time << endl;
#endif
		parent->recv_force();
	}
#endif
	int i;
#ifdef TIMING_CHECK
	if(children[1] && children[0] != children[1] && sim->rank != children[1]->rank)
		cerr << "[" << sim->rank << "] " << last_joint->name << " enter disassembly t = " << MPI_Wtime()-update_start_time << endl;
#endif
	// compute final constraint force
	static fVec KLf, Lf(6), Lf_temp[2], v(6), f, f_final(6);
	KLf.resize(n_const);
	Lf_temp[0].resize(6);
	Lf_temp[1].resize(6);
	f.resize(n_const);
	Lf_temp[0].zero();
	Lf_temp[1].zero();
	for(i=0; i<n_outer_joints; i++)
	{
		int org = outer_joints_origin[i];
		int index = outer_joints_index[i];
		fMat& Lambda_i = children[org]->Lambda[last_index[org]][index];
		// first multiply and increment
//		v.mul(Lambda_i, children[org]->outer_joints[index]->f_final);
		v.mul(Lambda_i, outer_joints[i]->f_final);
#ifdef VERBOSE
//		update_log << "children[" << org << "]->Lambda[" << last_index[org] << "][" << index << "] = " << Lambda_i << endl;
		update_log << outer_joints[i]->joint->name << ": f_final[" << i << "] = " << tran(outer_joints[i]->f_final) << endl;
#endif
		Lf_temp[org] += v;
	}
	Lf.sub(Lf_temp[0], Lf_temp[1]);
//	update_log << "Lf_temp[0] = " << tran(Lf_temp[0]) << endl;
//	update_log << "Lf_temp[1] = " << tran(Lf_temp[1]) << endl;
	// all test codes removed on 02/09/2007
	static fVec pp(6);
#ifndef USE_DCA
	// new formulation
	pp.add(da6, Lf);
	f_final.mul(W, pp);
	for(i=0; i<n_dof; i++)
		f_final(joint_index[i]) += tau(i);
	last_pjoints[0]->f_final.set(f_final);
	last_pjoints[1]->f_final.neg(f_final);
	v.mul(IW, pp);
	for(i=0; i<n_dof; i++)
	{
		acc_final(i) = v(joint_index[i]);
	}
	last_joint->joint_f.set(fVec3(f_final(0), f_final(1), f_final(2)));
	last_joint->joint_n.set(fVec3(f_final(3), f_final(4), f_final(5)));
#else  // #ifndef USE_DCA (DCA test)
	static fVec vp(6), svp;
	svp.resize(n_dof);
	pp.set(da6);
	pp += Lf;
	vp.mul(Vhat, pp);
	for(i=0; i<n_dof; i++)
	{
		svp(i) = tau(i) + vp(joint_index[i]);
	}
	acc_final.lineq_posv(SVS, svp);
//	cerr << "SVS = " << SVS << endl;
//	cerr << "svp = " << tran(svp) << endl;
//	cerr << "acc_final = " << tran(acc_final) << endl;
	v.zero();
	switch(last_joint->j_type)
	{
	case JROTATE:
	case JSLIDE:
		v(axis) = acc_final(0);
		break;
	case JSPHERE:
		v(3) = acc_final(0);
		v(4) = acc_final(1);
		v(5) = acc_final(2);
		break;
	case JFREE:
		v.set(acc_final);
		break;
	}
	pp -= v;
	f_final.mul(Vhat, pp);
	last_pjoints[0]->f_final.neg(f_final);
	last_pjoints[1]->f_final.set(f_final);
#endif
#ifndef USE_MPI
	if(last_joint->t_given) {
	  switch(last_joint->j_type) {
	    case JROTATE:
	  case JSLIDE:
	    last_joint->SetJointAcc(v(axis));
	    //		cerr << last_joint->name << ": " << v(axis) << endl;
	    break;
	  case JSPHERE:
	    last_joint->SetJointAcc(v(3), v(4), v(5));
	    break;
	  case JFREE:
	    last_joint->SetJointAcc(v(0), v(1), v(2), v(3), v(4), v(5));
#ifdef VERBOSE
	    update_log << last_joint->name << ": " << tran(v) << endl;
#endif
	    break;
	  default:
	    break;
	  }
	}
#endif
#ifdef USE_MPI
	if(children[0] && sim->rank != children[0]->rank)
	{
#ifdef TIMING_CHECK
		cerr << "[" << sim->rank << "] " << last_joint->name << ": send force to " << children[0]->last_joint->name << " [" << children[0]->rank << "] t = " << MPI_Wtime()-update_start_time << endl;
#endif
		send_force(children[0]->rank);
	}
	if(children[1] && children[0] != children[1] && sim->rank != children[1]->rank)
	{
#ifdef TIMING_CHECK
		cerr << "[" << sim->rank << "] " << last_joint->name << ": send force to " << children[1]->last_joint->name << " [" << children[1]->rank << "] t = " << MPI_Wtime()-update_start_time << endl;
#endif
		send_force(children[1]->rank);
	}
#endif
}
