/*
 * update_lcp.cpp
 * Create: Katsu Yamane, 04.02.25
 */

#include "psim.h"
#include <limits>

#define USE_INITIAL_GUESS

#include <sdcontact.h>
#include <lcp.h>
//#include <QuadProg.h>

#ifdef VERBOSE
#include <fstream>
extern std::ofstream update_log;
#endif

int pSim::Update(double timestep, std::vector<SDContactPair*>& sdContactPairs)
{
	ClearExtForce();
	int n_pairs = sdContactPairs.size();
	if(n_pairs == 0)
	{
		return pSim::Update();
	}
#ifdef VERBOSE
	update_log << "Update(" << timestep << ")" << endl;
#endif
	int n_total_points = 0;
#ifdef VERBOSE
	update_log << "num pairs = " << n_pairs << endl;
#endif
	in_create_chain = true;
	clear_contact();
	// add spherical joints to the links in contact
	for(int i=0; i<n_pairs; i++)
	{
		SDContactPair* sd_pair = sdContactPairs[i];
		Joint* pjoint = sd_pair->GetJoint(0);
		Joint* rjoint = sd_pair->GetJoint(1);
		int n_points = sd_pair->NumPoints();
		if(n_points == 0) continue;
#ifdef VERBOSE
		update_log << "== [" << pjoint->name << ", " << rjoint->name << "]: contact points = " << n_points << endl;
#endif
		// velocity of rjoint in pjoint frame
		static fVec3 rjoint_lin_vel, rjoint_ang_vel, rjoint_pos, temp;
		static fMat33 t_p_att, rjoint_att, t_rjoint_att;
		t_p_att.tran(pjoint->abs_att);
		temp.sub(rjoint->abs_pos, pjoint->abs_pos);
		rjoint_pos.mul(t_p_att, temp);
		rjoint_att.mul(t_p_att, rjoint->abs_att);
		rjoint_lin_vel.mul(rjoint_att, rjoint->loc_lin_vel);
		rjoint_ang_vel.mul(rjoint_att, rjoint->loc_ang_vel);
		t_rjoint_att.tran(rjoint_att);
#ifdef VERBOSE
		update_log << rjoint->name << ": rel_lin_vel = " << rjoint->rel_lin_vel << endl;
		update_log << rjoint->name << ": rel_ang_vel = " << rjoint->rel_ang_vel << endl;
//		update_log << "rjoint_ang_vel = " << rjoint_ang_vel << endl;
#endif
		std::vector<fVec3> pair_positions;
		std::vector<fVec3> pair_normals;
		std::vector<fVec3> pair_relvels;
		std::vector<fVec3> pair_positions_real;
		std::vector<fMat33> pair_orientations;
		std::vector<fMat33> pair_orientations_real;
		std::vector<fVec3> pair_jdot;
		std::vector<fVec3> pair_jdot_real;
		for(int m=0; m<n_points; m++)
		{
#ifdef VERBOSE
			update_log << "- contact point " << m << endl;
#endif
			// location: contact point 2
			// z: normal
			// x: relative slip velocity (arbitrary if no slip)

			static fVec3 temp;
			static fVec3 relpos1, relpos2, relnorm;
			double depth;
			relpos1(0) = sd_pair->Coord(m)(0);
			relpos1(1) = sd_pair->Coord(m)(1);
			relpos1(2) = sd_pair->Coord(m)(2);
			relpos2.set(relpos1);
			relnorm(0) = sd_pair->Normal(m)(0);
			relnorm(1) = sd_pair->Normal(m)(1);
			relnorm(2) = sd_pair->Normal(m)(2);
			relnorm.unit();
			depth = sd_pair->Depth(m);
#ifdef VERBOSE
			update_log << "p1 = " << relpos1 << ", p2 = " << relpos2 << ", norm = " << relnorm << ", depth = " << depth << endl;
#endif
			// relative velocity at contact point 2
			static fVec3 vel1, vel2, relvel, slipvel;
			vel1.cross(pjoint->loc_ang_vel, relpos2);
			vel1 += pjoint->loc_lin_vel;
			vel2.cross(rjoint_ang_vel, relpos2-rjoint_pos);
			vel2 += rjoint_lin_vel;
#ifdef VERBOSE
			update_log << "vel1 = " << vel1 << endl;
			update_log << "vel2 = " << vel2 << endl;
#endif
			relvel.sub(vel2, vel1);
			slipvel.set(relvel);
			slipvel -= (relnorm*relvel)*relnorm;
#ifdef VERBOSE
			update_log << "relvel = " << relvel << endl;
#endif
//			if(relvel*relnorm < 0.0)
			{
				relvel -= (0.005*depth/timestep)*relnorm;
//				relvel -= (depth/timestep)*relnorm;
				n_total_points++;
				// relative orientation in joint 0 frame
				double abs_slipvel = slipvel.length();
				static fVec3 x, y, z;
				static fMat33 ratt;
				z.set(relnorm);
				if(abs_slipvel > 1e-6)
				{
					slipvel.unit();
					x.set(slipvel);
				}
				else
				{
					fVec3 tmp(1,0,0);
					slipvel.set(tmp);
					slipvel -= (relnorm*tmp)*relnorm;
					abs_slipvel = slipvel.length();
					if(abs_slipvel > 1e-6)
					{
						slipvel.unit();
						x.set(slipvel);
					}
					else
					{
						tmp(0) = 0.0; tmp(1) = 1.0; tmp(2) = 0.0;
						slipvel.set(tmp);
						slipvel -= (relnorm*tmp)*relnorm;
						abs_slipvel = slipvel.length();
						slipvel.unit();
						x.set(slipvel);
					}
				}
				y.cross(z, x);
				ratt(0,0) = x(0); ratt(0,1) = y(0); ratt(0,2) = z(0);
				ratt(1,0) = x(1); ratt(1,1) = y(1); ratt(1,2) = z(1);
				ratt(2,0) = x(2); ratt(2,1) = y(2); ratt(2,2) = z(2);
				// position/orientation in rjoint frame
				static fVec3 relpos2_r;
				static fMat33 ratt_r;
				temp.sub(relpos2, rjoint_pos);
				relpos2_r.mul(t_rjoint_att, temp);
				ratt_r.mul(t_rjoint_att, ratt);
				pair_positions.push_back(relpos2);
				pair_normals.push_back(relnorm);
				pair_relvels.push_back(relvel);
#ifdef VERBOSE
//				update_log << "ratt = " << ratt << endl;
#endif
				pair_orientations.push_back(ratt);
				pair_positions_real.push_back(relpos2_r);
				pair_orientations_real.push_back(ratt_r);
				static fVec3 omega_x_p, omega_x_omega_x_p, jdot;
				omega_x_p.cross(pjoint->rel_ang_vel, relpos2);
				omega_x_omega_x_p.cross(pjoint->rel_ang_vel, omega_x_p);
				jdot.mul(omega_x_omega_x_p, ratt);
				pair_jdot.push_back(jdot);
				omega_x_p.cross(rjoint_ang_vel, relpos2);
				omega_x_omega_x_p.cross(rjoint_ang_vel, omega_x_p);
				jdot.mul(omega_x_omega_x_p, ratt);
				pair_jdot_real.push_back(jdot);
			}
		}
		// leave only the outmost points and remove others
		int n_valid_points = pair_relvels.size();
		if(n_valid_points == 0) continue;
		static char jname[256];
		sprintf(jname, "%s_%s", pjoint->name, rjoint->name);
		Joint* main_vjoint = new Joint(jname, JFREE);
		main_vjoint->real = rjoint;
		// pos/ori of pjoint in rjoint frame
		static fVec3 pjoint_pos;
		static fMat33 pjoint_att;
		pjoint_att.tran(rjoint_att);
		pjoint_pos.mul(pjoint_att, rjoint_pos);
		pjoint_pos *= -1.0;
		main_vjoint->rpos_real.set(pjoint_pos);
		main_vjoint->ratt_real.set(pjoint_att);
		AddJoint(main_vjoint, pjoint);
		contact_vjoints.push_back(main_vjoint);
		std::vector<int> add;
		add.resize(n_valid_points);
		double min_distance = 1.0e-3;
		for(int m=0; m<n_valid_points; m++)
		{
			add[m] = true;
			fVec3& posm = pair_positions[m];
			for(int n=0; n<m; n++)
			{
				if(!add[n]) continue;
				fVec3& posn = pair_positions[n];
				static fVec3 pp;
				pp.sub(posn, posm);
				double len = pp.length();
				// very close: leave the first one
				if(len < min_distance)
				{
#ifdef VERBOSE
					update_log << " " << m << ": too close: " << len << endl;
#endif
					add[m] = false;
					break;
				}
			}
		}
		for(int m=0; m<n_valid_points; m++)
		{
			if(!add[m]) continue;
			fVec3& posm = pair_positions[m];
			static fVec3 first_vec;
			double min_angle=0.0, max_angle=0.0;
			int have_first_vec = false;
			for(int n=0; n<n_valid_points; n++)
			{
				if(n == m || !add[n]) continue;
				fVec3& posn = pair_positions[n];
				fVec3& norm = pair_normals[n];
				static fVec3 pp;
				pp.sub(posn, posm);
				double len = pp.length();
				pp /= len;
				if(!have_first_vec)
				{
					first_vec.set(pp);
					have_first_vec = true;
					min_angle = 0.0;
					max_angle = 0.0;
					continue;
				}
				// compute angle
				static fVec3 fcp;
				double cs = first_vec * pp;
				fcp.cross(first_vec, pp);
				double ss = fcp.length();
				if(fcp * norm < 0.0) ss *= -1.0;
				double angle = atan2(ss, cs);
				if(angle < min_angle) min_angle = angle;
				if(angle > max_angle) max_angle = angle;
				if(max_angle - min_angle >= PI)
				{
#ifdef VERBOSE
					update_log << " inside: " << max_angle << ", " << min_angle << endl;
#endif
					add[m] = false;
					break;
				}
			}
		}
		for(int m=0; m<n_valid_points; m++)
		{
#ifdef VERBOSE
			update_log << "check[" << m << "]: pos = " << pair_positions[m] << ", add = " << add[m] << endl;
#endif
			if(add[m])
			{
				static fVec3 loc_relvel;
				all_vjoints.push_back(main_vjoint);
				loc_relvel.mul(pair_relvels[m], pair_orientations[m]);
				contact_relvels.push_back(loc_relvel);
				fric_coefs.push_back(sd_pair->SlipFric());
				// compute Jacobian
				static fMat Jv(3,6), Jr(3,6);
				static fMat33 Pcross, RtPx;
				fVec3& rpos = pair_positions[m];
				static fMat33 t_ratt;
				t_ratt.tran(pair_orientations[m]);
				Pcross.cross(rpos);
				RtPx.mul(t_ratt, Pcross);
				Jv(0,0) = t_ratt(0,0);
				Jv(0,1) = t_ratt(0,1);
				Jv(0,2) = t_ratt(0,2);
				Jv(1,0) = t_ratt(1,0);
				Jv(1,1) = t_ratt(1,1);
				Jv(1,2) = t_ratt(1,2);
				Jv(2,0) = t_ratt(2,0);
				Jv(2,1) = t_ratt(2,1);
				Jv(2,2) = t_ratt(2,2);
				Jv(0,3) = -RtPx(0,0);
				Jv(0,4) = -RtPx(0,1);
				Jv(0,5) = -RtPx(0,2);
				Jv(1,3) = -RtPx(1,0);
				Jv(1,4) = -RtPx(1,1);
				Jv(1,5) = -RtPx(1,2);
				Jv(2,3) = -RtPx(2,0);
				Jv(2,4) = -RtPx(2,1);
				Jv(2,5) = -RtPx(2,2);
#ifdef VERBOSE
//				update_log << "rpos_v = " << rpos << endl;
//				update_log << "t_ratt_v = " << t_ratt << endl;
//				update_log << "Jv = " << Jv << endl;
#endif
				rpos = pair_positions_real[m];
				t_ratt.tran(pair_orientations_real[m]);
				Pcross.cross(rpos);
				RtPx.mul(t_ratt, Pcross);
				Jr(0,0) = t_ratt(0,0);
				Jr(0,1) = t_ratt(0,1);
				Jr(0,2) = t_ratt(0,2);
				Jr(1,0) = t_ratt(1,0);
				Jr(1,1) = t_ratt(1,1);
				Jr(1,2) = t_ratt(1,2);
				Jr(2,0) = t_ratt(2,0);
				Jr(2,1) = t_ratt(2,1);
				Jr(2,2) = t_ratt(2,2);
				Jr(0,3) = -RtPx(0,0);
				Jr(0,4) = -RtPx(0,1);
				Jr(0,5) = -RtPx(0,2);
				Jr(1,3) = -RtPx(1,0);
				Jr(1,4) = -RtPx(1,1);
				Jr(1,5) = -RtPx(1,2);
				Jr(2,3) = -RtPx(2,0);
				Jr(2,4) = -RtPx(2,1);
				Jr(2,5) = -RtPx(2,2);
#ifdef VERBOSE
//				update_log << "rpos_r = " << rpos << endl;
//				update_log << "t_ratt_r = " << t_ratt << endl;
//				update_log << "Jr = " << Jr << endl;
#endif
				all_Jv.push_back(Jv);
				all_Jr.push_back(Jr);
				all_jdot_v.push_back(pair_jdot[m]);
				all_jdot_r.push_back(pair_jdot_real[m]);
			}
		}
	}
	init_contact();
	in_create_chain = false;
//	Schedule();
	//////////////
	if(n_total_points == 0)
	{
		pSim::Update();
		return 0;
	}
	//
//	DumpSchedule(update_log);
	CalcPosition();
	CalcVelocity();
	update_position();
	update_velocity();

	// compute contact force
	subchains->calc_contact_force(timestep);

	//
	disassembly();
#ifdef USE_MPI
	// scatter results
	scatter_acc();
#endif
	// clear f_final; use ext_force and ext_moment for the next steps
	// (e.g. in Runge-Kutta)
	subchains->clear_f_final();
	return 0;
}

void pSubChain::clear_f_final()
{
	for(int i=0; i<n_outer_joints; i++)
	{
		outer_joints[i]->f_final.zero();
	}
}

int pSubChain::calc_contact_force(double timestep)
{
	assert(n_outer_joints/2 == sim->contact_vjoints.size());
	int n_contacts = sim->all_vjoints.size();
	int n_contacts_3 = 3*n_contacts;
	int n_coefs = N_FRIC_CONE_DIV+1;
	fMat Phi_hat(n_contacts_3, n_contacts_3);
	fVec bias_hat(n_contacts_3);
	Phi_hat.zero();
	bias_hat.zero();
	std::vector<fVec3>& rvels = sim->contact_relvels;
	for(int i=0; i<n_contacts; i++)
	{
		int index = 3*i;
		static fVec3 v;
		v.set(rvels[i]);
		bias_hat(index) = v(0);
		bias_hat(index+1) = v(1);
		bias_hat(index+2) = v(2);
	}
//	update_log << "bias_hat(0) = " << bias_hat << endl;
	std::vector<int> r_index;
	std::vector<int> v_index;
	r_index.resize(n_contacts);
	v_index.resize(n_contacts);
	for(int i=0; i<n_contacts; i++)
	{
		Joint* vjoint = sim->all_vjoints[i];
		for(int j=0; j<n_outer_joints; j++)
		{
			if(outer_joints[j] == sim->joint_info[vjoint->i_joint].pjoints[0])
			{
				r_index[i] = j;
			}
			if(outer_joints[j] == sim->joint_info[vjoint->i_joint].pjoints[1])
			{
				v_index[i] = j;
			}
		}
//		update_log << "contact[" << i << "]: r_index = " << r_index[i] << ", v_index = " << v_index[i] << endl;
	}
	for(int i=0; i<n_contacts; i++)
	{
		int i3 = 3*i;
		// bias_hat
		static fVec dacc_r(3), dacc_v(3);
//		dacc_r.mul(sim->all_Jr[i], acc_temp[r_index[i]]);
		dacc_r.mul(sim->all_Jv[i], acc_temp[r_index[i]]);
		dacc_v.mul(sim->all_Jv[i], acc_temp[v_index[i]]);
#if 1
		dacc_r(0) += sim->all_jdot_r[i](0);
		dacc_r(1) += sim->all_jdot_r[i](1);
		dacc_r(2) += sim->all_jdot_r[i](2);
		dacc_v(0) += sim->all_jdot_v[i](0);
		dacc_v(1) += sim->all_jdot_v[i](1);
		dacc_v(2) += sim->all_jdot_v[i](2);
#endif
		dacc_r -= dacc_v;
		dacc_r *= timestep;
		bias_hat(i3) += dacc_r(0);
		bias_hat(i3+1) += dacc_r(1);
		bias_hat(i3+2) += dacc_r(2);
#ifdef VERBOSE
//		update_log << "contact[" << i << "]: bias_hat = [" << bias_hat(i3) << " " << bias_hat(i3+1) << " " << bias_hat(i3+2) << "]" << endl;
#endif
		// Phi_hat
		for(int j=0; j<n_contacts; j++)
		{
			int j3 = 3*j;
			static fMat PJ(6,3), JPJ(3,3), L(3,3);
			L.zero();
			PJ.mul(Lambda[r_index[i]][r_index[j]], tran(sim->all_Jv[j]));
			JPJ.mul(sim->all_Jv[i], PJ);
//			update_log << "Lambda[" << r_index[i] << "][" << r_index[j] << "] = " << Lambda[r_index[i]][r_index[j]] << endl;
//			update_log << "Lambda[" << r_index[i] << "][" << v_index[j] << "] = " << Lambda[r_index[i]][v_index[j]] << endl;
//			update_log << "Lambda[" << v_index[i] << "][" << r_index[j] << "] = " << Lambda[v_index[i]][r_index[j]] << endl;
//			update_log << "Lambda[" << v_index[i] << "][" << v_index[j] << "] = " << Lambda[v_index[i]][v_index[j]] << endl;
//			update_log << "JPJ = " << JPJ << endl;
			L += JPJ;
			PJ.mul(Lambda[v_index[i]][r_index[j]], tran(sim->all_Jv[j]));
			JPJ.mul(sim->all_Jv[i], PJ);
			L -= JPJ;
			PJ.mul(Lambda[r_index[i]][v_index[j]], tran(sim->all_Jv[j]));
			JPJ.mul(sim->all_Jv[i], PJ);
			L -= JPJ;
			PJ.mul(Lambda[v_index[i]][v_index[j]], tran(sim->all_Jv[j]));
			JPJ.mul(sim->all_Jv[i], PJ);
			L += JPJ;
#ifdef VERBOSE
//			update_log << "Phi_hat[" << i << "][" << j << "] = " << L << endl;
#endif
			Phi_hat.set_submat(i3, j3, L);
#if 0
			static fMat Lsub(3,3);
			Lsub.get_submat(i3, j3, Phi_hat);
			update_log << "Phi_hat[" << i << "][" << j << "] = " << Lsub << endl;
#if 1
			Lsub.get_submat(0, 0, Lambda[r_index[i]][r_index[j]]);
			update_log << "L[" << r_index[i] << "][" << r_index[j] << "] = " << Lsub << endl;
			Lsub.get_submat(0, 0, Lambda[v_index[i]][r_index[j]]);
			update_log << "L[" << v_index[i] << "][" << r_index[j] << "] = " << Lsub << endl;
			Lsub.get_submat(0, 0, Lambda[r_index[i]][v_index[j]]);
			update_log << "L[" << r_index[i] << "][" << v_index[j] << "] = " << Lsub << endl;
			Lsub.get_submat(0, 0, Lambda[v_index[i]][v_index[j]]);
			update_log << "L[" << v_index[i] << "][" << v_index[j] << "] = " << Lsub << endl;
#endif
#endif
		}
	}
#ifdef VERBOSE
//	update_log << "Phi_hat = " << Phi_hat << endl;
//	update_log << "bias_hat = " << bias_hat << endl;
#endif
	Phi_hat *= timestep;
	// LCP formulation
	int n_total_coef = n_coefs*n_contacts + n_contacts;
	fMat N(n_total_coef, n_total_coef);
	fMat Ck(3, n_coefs);
	fMat E(n_coefs, 1);
	fMat* Ehat_t = new fMat [n_contacts];
	fVec r(n_total_coef);
	// Ck, E
	Ck.zero();
	Ck(2, 0) = 1.0;
	E.zero();
	for(int m=0; m<N_FRIC_CONE_DIV; m++)
	{
		Ck(0, m+1) = sim->cone_dir[m](0);
		Ck(1, m+1) = sim->cone_dir[m](1);
		E(m+1, 0) = 1.0;
	}
	for(int i=0; i<n_contacts; i++)
	{
		double fric_coef = sim->fric_coefs[i];
		Ehat_t[i].resize(1, n_coefs);
		Ehat_t[i](0,0) = fric_coef;
		for(int m=0; m<N_FRIC_CONE_DIV; m++)
		{
			Ehat_t[i](0, m+1) = -1.0;
		}
	}
	N.zero();
	r.zero();
	for(int i=0; i<n_contacts; i++)
	{
		static fMat N_ij(n_coefs,n_coefs);
		static fVec r_i(n_coefs);
		int i3 = i*3, iN = i*n_coefs;
		r_i.zero();
		for(int j=0; j<n_contacts; j++)
		{
			static fMat Phi_ij(3,3), CP(n_coefs,3);
			static fMat B_ij(n_coefs,n_coefs), CinvP(n_coefs, 3);
			// N (CPhiC)
			int j3 = j*3, jN = j*(n_coefs);
			Phi_ij.get_submat(i3, j3, Phi_hat);
			CP.mul(tran(Ck), Phi_ij);
			N_ij.mul(CP, Ck);
			N.set_submat(iN, jN, N_ij);
		}
		// r
		static fVec bias_i(3);
		bias_i.get_subvec(i3, bias_hat);
		r_i.mul(bias_i, Ck);
		r.set_subvec(iN, r_i);
		// E
		N.set_submat(iN, n_coefs*n_contacts+i, E);
		N.set_submat(n_coefs*n_contacts+i, iN, Ehat_t[i]);
	}
	// LCP parameters
	double max_error = 1.0e-3;
//	int max_iteration = n_total_coef * 10;
	int max_iteration = 10000;
	// presolve
	std::vector<int> presolve_g2w;
	int presolve_failed = false;
	fVec presolve_g2;
	presolve_g2w.resize(n_contacts);
	for(int i=0; i<n_contacts; i++)
	{
		presolve_g2w[i] = -1;
	}
	{
		fMat Ns(n_contacts, n_contacts);
		fVec rs(n_contacts), as;
		int n_iteration = 0;
		for(int i=0; i<n_contacts; i++)
		{
			rs(i) = bias_hat(i*3+2);
			for(int j=0; j<n_contacts; j++)
			{
				Ns(i, j) = Phi_hat(i*3+2, j*3+2);
			}
		}
		LCP lcp_s(Ns, rs);
		presolve_failed = lcp_s.SolvePivot2(presolve_g2, as, max_error, max_iteration, &n_iteration, presolve_g2w);
		if(presolve_failed)
		{
			cerr << "presolve_failed (" << n_iteration << "/" << max_iteration << ")" << endl;
			cerr << "Phi_hat = " << Phi_hat << endl;
		}
#ifdef VERBOSE
		for(int i=0; i<n_contacts; i++)
		{
			update_log << "presolve_g2w[" << i << "] = " << presolve_g2w[i] << endl;
		}
#endif
#ifdef MEASURE_COLLISION_TIME
		sim->n_total_nodes += n_iteration;
#endif
	}
#ifdef USE_INITIAL_GUESS
	std::vector<int> w2a, w2g, z2a, z2g;
#endif
	// search for appropriate set of constraints
	std::vector<int> active2all;
	fVec* fk = new fVec [n_contacts];
	fVec* vk = new fVec [n_contacts];
	fMat N_active;
	fVec r_active;
#if 1
	if(!presolve_failed)
	{
		for(int i=0; i<n_contacts; i++)
		{
			if(presolve_g2w[i] >= 0)
			{
				active2all.push_back(i);
			}
		}
	}
	else
#endif
	{
		for(int i=0; i<n_contacts; i++)
		{
			active2all.push_back(i);
		}
	}
#ifdef VERBOSE
	update_log << "*** start searching constraint set" << endl;
#endif
	int max_global_iteration = 1;
	int count = 0;
	while(count < max_global_iteration)
	{
		std::vector<int> all2active;
		std::vector<int> g2w;
		all2active.resize(n_contacts);
		for(int i=0; i<n_contacts; i++)
		{
			all2active[i] = -1;
			fk[i].resize(3);
			fk[i].zero();
		}
		int n_active_contacts = active2all.size();
		if(n_active_contacts > 0)
		{
			int n_active_coef = n_active_contacts*n_coefs + n_active_contacts;
			N_active.resize(n_active_coef, n_active_coef);
			r_active.resize(n_active_coef);
			N_active.zero();
			r_active.zero();
			for(int i=0; i<n_active_contacts; i++)
			{
				all2active[active2all[i]] = i;
			}
#ifdef VERBOSE
			update_log << "n_active_contacts = " << n_active_contacts << endl;
			update_log << "n_active_coef = " << n_active_coef << endl;
#endif
			for(int i=0; i<n_active_contacts; i++)
			{
				static fMat N_ij(n_coefs,n_coefs);
				static fVec r_i(n_coefs);
				int i_active_N = i*n_coefs;
				int i_all = active2all[i];
				int i_all_N = i_all*n_coefs;
				for(int j=0; j<n_active_contacts; j++)
				{
					int j_all = active2all[j];
					int j_all_N = j_all*n_coefs;
					int j_active_N = j*n_coefs;
					N_ij.get_submat(i_all_N, j_all_N, N);
					N_active.set_submat(i_active_N, j_active_N, N_ij);
				}
				r_i.get_subvec(i_all_N, r);
				r_active.set_subvec(i_active_N, r_i);
				N_active.set_submat(i_active_N, n_coefs*n_active_contacts+i, E);
				N_active.set_submat(n_coefs*n_active_contacts+i, i_active_N, Ehat_t[i]);
			}
#ifdef USE_INITIAL_GUESS
			static fMat N_init;
			static fVec r_init;
			N_init.resize(n_active_coef, n_active_coef);
			r_init.resize(n_active_coef);
			w2a.resize(n_active_coef);
			w2g.resize(n_active_coef);
			z2a.resize(n_active_coef);
			z2g.resize(n_active_coef);
			for(int i=0; i<n_active_coef; i++)
			{
				w2a[i] = i;
				w2g[i] = -1;
				z2a[i] = -1;
				z2g[i] = i;
			}
			for(int i=0; i<n_active_contacts; i++)
			{
				int idx = i*n_coefs;
				w2a[idx] = -1;
				w2g[idx] = idx;
				z2a[idx] = idx;
				z2g[idx] = -1;
			}
			LCP::Pivot(w2a, w2g, z2a, z2g, N_active, r_active, N_init, r_init);
			N_active.set(N_init);
			r_active.set(r_init);
#endif
			LCP lcp(N_active, r_active);
			// solution
			static fVec g2, a;
			int n_iteration = 0;
			g2w.resize(n_active_coef);
#ifdef MEASURE_COLLISION_TIME
			sim->n_total_contacts += n_contacts;
			sim->n_active_contacts += n_active_contacts;
			LongInteger t1 = GetTick();
#endif
#ifdef USE_NORMAL_LEMKE
			int failed = lcp.SolvePivot(g2, a, max_error, max_iteration, &n_iteration, g2w);
#else
			int failed = lcp.SolvePivot2(g2, a, max_error, max_iteration, &n_iteration, g2w);
#endif
			count++;
#ifdef MEASURE_COLLISION_TIME
			LongInteger t2 = GetTick();
			sim->lcp_solve_time += ExpiredTime(t1, t2);
			sim->n_total_nodes += n_iteration;
			sim->n_loops += lcp.NumLoops();
			sim->n_errors += lcp.NumErrors();
#endif
#ifdef VERBOSE
			update_log << "n_iteration = " << n_iteration << endl;
#endif
			fVec error(n_active_coef);
			error.mul(N_active, g2);
			error += r_active;
			error -= a;
#ifdef VERBOSE
			update_log << "error = " << error.length() << endl;
#endif
			if(error.length() > 1e-4) failed = true;
			if(failed)
			{
				cerr << "failed (" << n_iteration << "/" << max_iteration << "): n_contacts = " << n_contacts << endl;
#ifdef MEASURE_COLLISION_TIME
				sim->n_failure_frames++;
#endif
#ifdef VERBOSE
				update_log << "failed" << endl;
#endif
				if(!presolve_failed)
				{
					cerr << "using presolve result" << endl;
					for(int i=0; i<n_contacts; i++)
					{
						fk[i](2) = presolve_g2(i);
					}
				}
				break;
			}
			else
			{
#ifdef VERBOSE
				update_log << "success" << endl;
				update_log << "g2 = " << tran(g2) << endl;
				update_log << "a = " << tran(a) << endl;
				update_log << "g2w = " << endl;
				for(int i=0; i<n_active_contacts; i++)
				{
					update_log << "active contact[" << i << "] a = " << flush;
					for(int j=0; j<n_coefs; j++)
					{
						update_log << g2w[i*n_coefs+j] << " " << flush;
					}
					update_log << endl;
				}
				for(int i=0; i<n_active_contacts; i++)
				{
					update_log << "active contact[" << i << "] lambda = " << g2w[n_active_contacts*n_coefs+i] << endl;
				}
#endif
#ifdef USE_INITIAL_GUESS
				for(int i=0; i<n_active_coef; i++)
				{
					if(w2g[i] >= 0)
					{
						g2(i) = a(w2g[i]);
					}
				}
#endif
				for(int i=0; i<n_active_contacts; i++)
				{
					static fVec g2_i(n_coefs);
					int i3 = i*3, iN = i*n_coefs;
					g2_i.get_subvec(iN, g2);
					fk[active2all[i]].mul(Ck, g2_i);
#ifdef VERBOSE
					update_log << "fk[" << active2all[i] << "] = " << tran(fk[active2all[i]]) << endl;
#endif
				}
			}
			active2all.clear();
		}
		// check velocity
		for(int i=0; i<n_contacts; i++)
		{
			vk[i].resize(3);
			vk[i].zero();
		}
		int constraint_modified = false;
		for(int i=0; i<n_contacts; i++)
		{
			static fVec bias_i(3);
			bias_i.get_subvec(3*i, bias_hat);
			for(int j=0; j<n_contacts; j++)
			{
				static fMat Phi_ij(3,3);
				Phi_ij.get_submat(3*i, 3*j, Phi_hat);
				vk[i] += Phi_ij * fk[j];
			}
			vk[i] += bias_i;
#ifdef VERBOSE
			update_log << "vk[" << i << "] = " << tran(vk[i]) << endl;
			if(vk[i](2) < -max_error)
			{
				update_log << "--- too small vertical velocity" << endl;
			}
#endif
#ifdef USE_INITIAL_GUESS
			if((all2active[i] >= 0 && w2g[all2active[i]*n_coefs] < 0 && g2w[all2active[i]*n_coefs] >= 0) ||
			   (all2active[i] >= 0 && w2g[all2active[i]*n_coefs] >= 0 && g2w[all2active[i]*n_coefs] < 0) ||
#else
			if((all2active[i] >= 0 && g2w[all2active[i]*n_coefs] >= 0) ||
#endif
			   vk[i](2) < -max_error)
			{
				active2all.push_back(i);
#ifdef VERBOSE
				update_log << "added to active" << endl;
#endif
				if(vk[i](2) < -max_error)
				{
					constraint_modified = true;
#ifdef VERBOSE
					update_log << "constraint modified" << endl;
#endif
				}
			}
#ifdef VERBOSE
			else
#ifdef USE_INITIAL_GUESS
			if((all2active[i] >= 0 && w2g[all2active[i]*n_coefs] < 0 && g2w[all2active[i]*n_coefs] >= 0) ||
			   (all2active[i] >= 0 && w2g[all2active[i]*n_coefs] >= 0 && g2w[all2active[i]*n_coefs] < 0))
#else
			if(all2active[i] >= 0 && g2w[all2active[i]*n_coefs] >= 0)
#endif
			{
				update_log << "removed from active" << endl;
			}
#endif
		}
		if(!constraint_modified) break;
	}
#ifdef VERBOSE
	update_log << "end of search" << endl;
#endif
//	cerr << "trials = " << count << endl;
	// apply the force
	for(int i=0; i<n_outer_joints; i++)
	{
		outer_joints[i]->f_final.zero();
	}
	for(int i=0; i<n_contacts; i++)
	{
		static fVec3 f, f_virt, f_real, moment;
		f(0) = fk[i](0);
		f(1) = fk[i](1);
		f(2) = fk[i](2);
		Joint* r_joint = outer_joints[r_index[i]]->joint;
		Joint* v_joint = outer_joints[v_index[i]]->joint;
		static fVec f0(6);
		f0.mul(fk[i], sim->all_Jr[i]);
		r_joint->real->ext_force(0) += f0(0);
		r_joint->real->ext_force(1) += f0(1);
		r_joint->real->ext_force(2) += f0(2);
		r_joint->real->ext_moment(0) += f0(3);
		r_joint->real->ext_moment(1) += f0(4);
		r_joint->real->ext_moment(2) += f0(5);
		f0.mul(fk[i], sim->all_Jv[i]);
		v_joint->parent->ext_force(0) -= f0(0);
		v_joint->parent->ext_force(1) -= f0(1);
		v_joint->parent->ext_force(2) -= f0(2);
		v_joint->parent->ext_moment(0) -= f0(3);
		v_joint->parent->ext_moment(1) -= f0(4);
		v_joint->parent->ext_moment(2) -= f0(5);
		outer_joints[r_index[i]]->f_final += f0;
		outer_joints[v_index[i]]->f_final -= f0;
#ifdef VERBOSE
		update_log << r_joint->real->name << " force/moment: " << r_joint->real->ext_force << r_joint->real->ext_moment << endl;
		update_log << v_joint->parent->name << " force/moment: " << v_joint->parent->ext_force << v_joint->parent->ext_moment << endl;
#endif
	}
	delete[] vk;
	delete[] fk;
	delete[] Ehat_t;
	return 0;
}


