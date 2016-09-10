/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*!
 * @file   lcp_pivot.cpp
 * @author Katsu Yamane
 * @date   12/15/2006
 * @brief  Pivot-based solver implementation for Linear Complementarity Problems (LCP)
 */

#include "lcp.h"
#include <limits>
#include <dp.h>
#include <list>

//#define VERBOSE

void swap_index(std::vector<int>& idx1, std::vector<int>& idx2, std::vector<int>& w2a, std::vector<int>& w2g, std::vector<int>& z2a, std::vector<int>& z2g)
{
	int size = idx1.size();
	for(int i=0; i<size; i++)
	{
		int m = idx1[i], n = idx2[i];
#ifdef VERBOSE
		cerr << "swap: w[" << m << "] <-> z[" << n << "]" << endl;
		cerr << "w[" << m << "]: a/g = " << w2a[m] << "/" << w2g[m] << endl;
		cerr << "z[" << n << "]: a/g = " << z2a[n] << "/" << z2g[n] << endl;
#endif
		int w2a_org = w2a[m], z2g_org = z2g[n];
		w2a[m] = z2a[n];
		z2g[n] = w2g[m];
		z2a[n] = w2a_org;
		w2g[m] = z2g_org;
	}
}

void swap_index(int idx1, int idx2, std::vector<int>& w2a, std::vector<int>& w2g, std::vector<int>& z2a, std::vector<int>& z2g)
{
	int w2a_org = w2a[idx1], z2g_org = z2g[idx2];
	w2a[idx1] = z2a[idx2];
	z2g[idx2] = w2g[idx1];
	z2a[idx2] = w2a_org;
	w2g[idx1] = z2g_org;
}

class LCPNode
	: public dpNode
{
public:
	LCPNode(LCP* _lcp,
			LCPNode* parent_lcp,
			const fVec& _q_old,
			double _max_error, 
			const vector<int>& _w2a, const vector<int>& _w2g,
			const vector<int>& _z2a, const vector<int>& _z2g,
			int _n_steps,
			int _js, int _jr, double _cost) : dpNode() {
		terminate = false;
		n_vars = _w2a.size();
		q_old.resize(n_vars);
		q_old.set(_q_old);
		max_error = _max_error;
		lcp = _lcp;
		js = _js;
		jr = _jr;
		w2a = _w2a;
		w2g = _w2g;
		z2a = _z2a;
		z2g = _z2g;
		n_steps = _n_steps;
//		cost = _cost;
//		cost = _cost-1.0;
		cost = exp(_cost);
//		cost = exp(_cost)-1.0;
//		cost = exp(_cost/max_error)-1.0;
		if(js >= 0)
		{
			ys2a = w2a[js];
			ys2g = w2g[js];
		}
		else
		{
			ys2a = -1;
			ys2g = -1;
		}
		if(jr >= 0)
		{
			yr2a = z2a[jr];
			yr2g = z2g[jr];
		}
		else
		{
			yr2a = -1;
			yr2g = -1;
		}
#ifdef VERBOSE
		cerr << "new node: step = " << n_steps << ", cost = " << cost << ", js = " << js << ", jr = " << jr << endl;
#endif
		if(ys2g == n_vars)
		{
#ifdef VERBOSE
			cerr << "another pivot to terminate" << endl;
#endif
			static fMat m_jr_dummy;
			m_jr_dummy.resize(n_vars, 1);
			swap_index(js, jr, w2a, w2g, z2a, z2g);
#ifdef INVERSE_FROM_SCRATCH
			LCP::Pivot(lcp->Mref(), lcp->Qref(), w2a, w2g, z2a, z2g, 0, m_jr_dummy, q_old);
#else
			LCP::Pivot(lcp->Mref(), lcp->Qref(), parent_lcp->Maa_inv, parent_lcp->w2a, parent_lcp->w2g, parent_lcp->z2a, parent_lcp->z2g, ys2a, ys2g, yr2a, yr2g, w2a, w2g, z2a, z2g, 0, Maa_inv, m_jr_dummy, q_old);
#endif
			terminate = true;
		}
		else if(js >= 0)
		{
			swap_index(js, jr, w2a, w2g, z2a, z2g);
		}
		for(int i=0; i<n_vars; i++)
		{
			if(w2g[i] >= 0)
			{
				g_in_w.push_back(w2g[i]);
			}
		}
		for(int i=0; i<=n_vars; i++)
		{
			if(z2a[i] >= 0)
			{
				a_in_z.push_back(z2a[i]);
			}
		}
		g_in_w.sort();
		a_in_z.sort();
#ifdef VERBOSE
#if 1
		cerr << "g_in_w = [" << flush;
		for(std::list<int>::iterator i=g_in_w.begin(); i!=g_in_w.end(); i++)
		{
			cerr << *i << " " << flush;
		}
		cerr << "]" << endl;
		cerr << "a_in_z = [" << flush;
		for(std::list<int>::iterator i=a_in_z.begin(); i!=a_in_z.end(); i++)
		{
			cerr << *i << " " << flush;
		}
		cerr << "]" << endl;
#endif
#endif
		new_jr = -1;
		if(js < 0)
		{
			new_jr = jr;
		}
		else
		{
			// set next yr2a
			int new_yr2a=0, new_yr2g=0;
			if(ys2a >= 0)
			{
				new_yr2a = -1;
				new_yr2g = ys2a;
			}
			else if(ys2g >= 0)
			{
				new_yr2a = ys2g;
				new_yr2g = -1;
			}
			// find next jr
			if(new_yr2a >= 0)
			{
				for(int i=0; i<=n_vars; i++)
				{
					if(new_yr2a == z2a[i])
					{
						new_jr = i;
						break;
					}
				}
			}
			else if(new_yr2g >= 0)
			{
				for(int i=0; i<=n_vars; i++)
				{
					if(new_yr2g == z2g[i])
					{
						new_jr = i;
						break;
					}
				}
			}
#ifdef VERBOSE
			if(new_jr < 0)
			{
				cerr << "new_jr = " << new_jr << endl;
				cerr << "ys2a = " << ys2a << endl;
				cerr << "ys2g = " << ys2g << endl;
				cerr << "new_yr2a = " << new_yr2a << endl;
				cerr << "new_yr2g = " << new_yr2g << endl;
			}
#endif
			assert(terminate || new_jr >= 0);
		}
#ifdef VERBOSE
		cerr << "new_jr = " << new_jr << endl;
#endif
	}
	
	~LCPNode() {
	}

	int NumStep() {
		return n_steps;
	}

#ifndef INVERSE_FROM_SCRATCH
	fMat Maa_inv;
#endif
	fVec q_old;
	vector<int> w2a, w2g, z2a, z2g;


	static int num_loops;
	static int num_errors;
protected:
	std::list<int> g_in_w, a_in_z;
	int new_jr;

	int create_child_nodes(dpNode**& nodes) {
#ifdef VERBOSE
		cerr << "=== create_child_nodes (step = " << n_steps << ", cost = " << cost << ", total cost = " << total_cost << ", total_astar_cost = " << total_astar_cost << ", js = " << js << ", jr = " << jr << ")" << endl;
#endif
		// compute new M, q
		static fVec q;
		q.resize(n_vars);
		static fMat m_jr;
		m_jr.resize(n_vars, 1);
		if(js < 0)  // initial node: no pivot
		{
			q.set(q_old);
			m_jr.get_submat(0, jr, lcp->Mref());
#ifdef VERBOSE
			cerr << "initial node" << endl;
#endif
		}
		else
		{
#if 0
			// duplicate check
			dpNode* n;
			for(n=Parent(); n; n=n->Parent())
			{
				LCPNode* ln = (LCPNode*)n;
//				if(ln->w2a == w2a && ln->w2g == w2g)
				if(ln->g_in_w == g_in_w && ln->a_in_z == a_in_z)
				{
					num_loops++;
#ifdef VERBOSE
					cerr << "loop; don't open" << endl;
#endif
					q_old.resize(0);
					return 0;
				}
			}
#endif
#ifdef INVERSE_FROM_SCRATCH  // solve linear equation every time
			LCP::Pivot(lcp->Mref(), lcp->Qref(), w2a, w2g, z2a, z2g, new_jr, m_jr, q);
#else  // update Maa_inv
			LCPNode* lcpnode = (LCPNode*)parent;
			LCP::Pivot(lcp->Mref(), lcp->Qref(), lcpnode->Maa_inv, lcpnode->w2a, lcpnode->w2g, lcpnode->z2a, lcpnode->z2g, ys2a, ys2g, yr2a, yr2g, w2a, w2g, z2a, z2g, new_jr, Maa_inv, m_jr, q);
#endif
			double error = lcp->CheckPivotResult(q, w2a, w2g);
			if(error >= max_error)
			{
				num_errors++;
#ifdef VERBOSE
				cerr << "error is too large (" << error << "); don't open" << endl;
#endif
				return 0;
			}
		}
		q_old.resize(0);
		// find candidates of ys
#ifdef PIVOT_MINIMUM_ONLY
		int min_q_m_index = -1;
		double min_q_m = 0.0;
#else
		std::vector<int> js_cand;
		std::vector<double> js_cost;
		std::vector<double> js_g0;
#endif
		// create children
		// find current g0_index
		int current_g0_index = -1;
		for(int j=0; j<n_vars; j++)
		{
			if(w2g[j] == n_vars)
			{
				current_g0_index = j;
				break;
			}
		}
		// find smallest ratio and get index of g0 in w
		int g0_index = -1;
		for(int j=0; j<n_vars; j++)
		{
			if(js < 0 || m_jr(j,0) < 0.0)
			{
				double new_q_g0 = 0.0;
				// compute new q when pivoted here
				double q_m = min_new_q(q, m_jr, j, max_error, current_g0_index, &new_q_g0);
				if(w2g[j] == n_vars && q_m >= -max_error)
				{
					g0_index = j;
					break;
				}
#ifdef VERBOSE
//				if(j<50)
//					cerr << "[" << j << "]: w(a/g) = " << w2a[j] << "/" << w2g[j] << ", q/m_jr = " << q(j) << "/" << m_jr(j,0) << ", q_m = " << q_m << endl;
#endif
#ifdef PIVOT_MINIMUM_ONLY
				if(q_m >= -max_error && (min_q_m_index < 0 || q_m > min_q_m))
				{
					min_q_m_index = j;
					min_q_m = q_m;
				}
#else
				if(q_m >= -max_error)
				{
					js_cand.push_back(j);
					js_cost.push_back(q_m);
					js_g0.push_back(new_q_g0);
#ifdef VERBOSE
					cerr << j << ": q_m = " << q_m << ", q_g0 = " << new_q_g0 << endl;
#endif
				}
#endif
			}
		}
#ifdef VERBOSE
		if(js_cand.size() == 0)
		{
			cerr << "no candidate" << endl;
		}
#endif
		// check if we can pivot g0
		if(g0_index >= 0)
		{
#ifdef VERBOSE
			cerr << "z0 can be pivoted" << endl;
#endif
			nodes = new dpNode* [1];
//			LCPNode* node = new LCPNode(lcp, this, q, max_error, w2a, w2g, z2a, z2g, n_steps+1, g0_index, new_jr, 0.0);
			LCPNode* node = new LCPNode(lcp, this, q, max_error, w2a, w2g, z2a, z2g, n_steps+1, g0_index, new_jr, -1.0);
			nodes[0] = (dpNode*)node;
			return 1;
		}
#ifdef PIVOT_MINIMUM_ONLY
		if(min_q_m_index >= 0)
		{
			nodes = new dpNode* [1];
			LCPNode* node = new LCPNode(lcp, this, q, max_error, w2a, w2g, z2a, z2g, n_steps+1, min_q_m_index, new_jr, -min_q_m);
			nodes[0] = (dpNode*)node;
			return 1;
		}
		return 0;
#else  // #ifdef PIVOT_MINIMUM_ONLY
#if 0
		// test->
		if(js_cand.size() > 1)
		{
		for(int i=0; i<n_vars; i++)
		{
			static fMat beta0;
			beta0.resize(n_vars, 1);
			// is a[i] in z?
			int a2z = -1;
			for(int j=0; j<=n_vars; j++)
			{
				if(z2a[j] == i)
				{
					a2z = j;
					break;
				}
			}
			if(a2z >= 0)
			{
				static fVec q0;
				q0.resize(n_vars);
				LCP::Pivot(lcp->Mref(), lcp->Qref(), w2a, w2g, z2a, z2g, a2z, beta0, q0);
				beta0 *= -1.0;
			}
			else
			{
				// find a[i] in w
				int a2w = -1;
				for(int j=0; j<n_vars; j++)
				{
					if(w2a[j] == i)
					{
						a2w = j;
						break;
					}
				}
				beta0.zero();
				beta0(a2w, 0) = 1.0;
			}
			int minimum_defined = false;
			double min_beta = std::numeric_limits<double>::max();
			double min_diff = 1e-3;
#ifdef VERBOSE
			cerr << "column " << i << ": beta = " << flush;
#endif
			for(int j=0; j<js_cand.size(); j++)
			{
				if(j == 0)
				{
					min_beta = beta0(js_cand[j],0);
				}
				else if(fabs(beta0(js_cand[j],0)-min_beta) < min_diff)
				{
					minimum_defined = false;
				}
				else if(beta0(js_cand[j],0) < min_beta - min_diff)
				{
					minimum_defined = true;
					min_beta = beta0(js_cand[j],0);
				}
#ifdef VERBOSE
				cerr << beta0(js_cand[j],0) << " " << flush;
#endif
			}
#ifdef VERBOSE
			cerr << endl;
#endif
			if(minimum_defined)
			{
#ifdef VERBOSE
				cerr << "minimum found" << endl;
#endif
				for(int j=0; j<js_cand.size(); j++)
				{
					js_cost[j] = -beta0(js_cand[j],0);
				}
				break;
			}
		}
		}
		// <-test
#endif

		nodes = new dpNode* [js_cand.size()];
		for(unsigned int j=0; j<js_cand.size(); j++)
		{
			LCPNode* node = new LCPNode(lcp, this, q, max_error, w2a, w2g, z2a, z2g, n_steps+1, js_cand[j], new_jr, -js_cost[j]);
			nodes[j] = (dpNode*)node;
		}
		return js_cand.size();
#endif
	}
	
	double calc_cost() {
		return cost;
	}
	
	double calc_astar_cost(dpNode* potential_parent) {
//		return 0.0;
		return -n_steps;
	}

	int same_state(dpNode* refnode) {
#ifndef ACTIVATE_SAME_STATE
		return false;
#else
		if(terminate) return false;
		LCPNode* lcp_refnode = (LCPNode*)refnode;
		int ret = (lcp_refnode->z2a[lcp_refnode->new_jr] == z2a[new_jr] && lcp_refnode->z2g[lcp_refnode->new_jr] == z2g[new_jr] &&
				   lcp_refnode->g_in_w == g_in_w && lcp_refnode->a_in_z == a_in_z);
#ifdef VERBOSE
		if(ret)
		{
			cerr << "same_state = " << ret << " (step = " << lcp_refnode->n_steps << ", js = " << lcp_refnode->js << ", jr = " << lcp_refnode->jr << ", z2a[" << new_jr << "] = " << z2a[new_jr] << ", z2g[" << new_jr << "] = " << z2g[new_jr] << ", cost diff = " << lcp_refnode->total_cost - total_cost << ")" << endl;
		}
#endif
		return ret;
#endif
	}
	
	int is_goal() {
		return terminate;
	}

	void calc_new_q(const fVec& q, const fMat& m_jr, int piv, fVec& q_new) {
		double q_m = - q(piv) / m_jr(piv, 0);
		q_new.set(q);
		for(int i=0; i<n_vars; i++)
		{
			q_new(i) += m_jr(i,0)*q_m;
		}
		q_new(piv) = q_m;
	}
	
	double min_new_q(const fVec& q, const fMat& m_jr, int piv, double max_error, int z0_index = -1, double* new_q_z0 = 0) {
		double q_m = - q(piv) / m_jr(piv, 0);
//		double min_q = 0.0;
		double min_q = q_m;
		// q'(piv) = q_m >= 0
		// q(piv) + m_jr(piv)*q_m = 0 so it always passes the check
		for(int i=0; i<n_vars; i++)
		{
			double qi = q(i) + m_jr(i,0)*q_m;
			if(qi < -max_error)
			{
				return qi;
			}
//			if(qi < min_q && i != piv)
			if(qi < min_q)
			{
				min_q = qi;
			}
		}
		// compute the element of new q corresponding to z0_index
		if(z0_index >= 0 && new_q_z0)
		{
			if(z0_index == piv)
			{
				*new_q_z0 = q_m;
			}
			else
			{
				*new_q_z0 = q(z0_index) + m_jr(z0_index,0)*q_m;
			}
		}
		return min_q;
	}

	double max_error;
	int js, jr;
	int ys2a, ys2g, yr2a, yr2g;
	int n_vars;
	LCP* lcp;
	int terminate;
	int n_steps;
};

int LCPNode::num_loops = 0;
int LCPNode::num_errors = 0;

// query statistics
int LCP::NumLoops() {
	return LCPNode::num_loops;
}
int LCP::NumErrors() {
	return LCPNode::num_errors;
}

int LCP::SolvePivot2(fVec& g, fVec& a, double _max_error, int _max_nodes, int* n_nodes, std::vector<int>& _g2w)
{
	LCPNode::num_loops = 0;
	LCPNode::num_errors = 0;
	if(n_nodes) *n_nodes = 0;
	// index mapping
	std::vector<int> z2g, w2g, z2a, w2a;
	int yr2g = -1, yr2a = -1, ys2g = -1, ys2a = -1;
	z2g.resize(n_vars+1);
	z2a.resize(n_vars+1);
	w2g.resize(n_vars);
	w2a.resize(n_vars);
	for(int i=0; i<n_vars; i++)
	{
		z2g[i] = i;
		z2a[i] = -1;
		w2g[i] = -1;
		w2a[i] = i;
	}
	z2g[n_vars] = n_vars;  // z0
	z2a[n_vars] = -1;
	// set initial M, q
	// w = Mz + q, M = [N c]
	fMat c(n_vars, 1);
	c = 1.0;
	M.resize(n_vars, n_vars+1);
	q.resize(n_vars);
	M.set_submat(0, 0, N);
	M.set_submat(0, n_vars, c);
	q.set(r);
	int max_nodes = _max_nodes;
	double max_error = _max_error;
	int terminate = true;
#ifdef VERBOSE
	cerr << "LCP::SolvePivot2(" << n_vars << ")" << endl;
//	cerr << "--- inputs ---" << endl;
//	cerr << "M = " << M << endl;
//	cerr << "q = " << tran(q) << endl;
#endif
	//
	// step 0
	//
#ifdef VERBOSE
	cerr << "--- step 0 ---" << endl;
#endif
	for(int j=0; j<n_vars; j++)
	{
		if(q(j) < 0.0)
		{
			terminate = false;
			break;
		}
	}
	if(terminate)
	{
#ifdef VERBOSE
		cerr << "q >= 0: trivial solution" << endl;
#endif
		g.resize(n_vars);
		a.resize(n_vars);
		g.zero();
		a.set(r);
		return 0;
	}
	dpMain* dp = new dpMain;
	LCPNode* snode = new LCPNode(this, NULL, q, max_error, w2a, w2g, z2a, z2g, 0, -1, n_vars, 0.0);
	dp->SetStartNode((dpNode*)snode);
	dp->Search(max_nodes, 1);
	dpNode* goal = dp->BestGoal();
	if(n_nodes) *n_nodes = dp->NumNodes();
//	cerr << "pivot result:" << endl;
//	cerr << "M_new = " << M_new << endl;
//	cerr << "q_new = " << tran(q_new) << endl;
	g.resize(n_vars);
	a.resize(n_vars);
	g.zero();
	a.zero();
	if(!goal)
	{
		cerr << "[LCP] solution not found (number of nodes = " << dp->NumNodes() << ", number of variables = " << n_vars << ")" << endl;
#ifdef VERBOSE
		cerr << "M = " << M << endl;
		cerr << "q = " << tran(q) << endl;
#endif
		delete dp;
		return -1;
	}
	LCPNode* lgoal = (LCPNode*)goal;
#ifdef VERBOSE
	cerr << "goal found (" << dp->NumNodes() << "/" << lgoal->NumStep() << ")" << endl;
	cerr << "num_loops = " << LCPNode::num_loops << endl;
	cerr << "w =" << flush;
#endif
	for(int j=0; j<n_vars; j++)
	{
		if(lgoal->w2a[j] >= 0)
		{
#ifdef VERBOSE
			cerr << "\ta[" << lgoal->w2a[j] << "]" << flush;
#endif
			a(lgoal->w2a[j]) = lgoal->q_old(j);
		}
		else if(lgoal->w2g[j] >= 0 && lgoal->w2g[j] != n_vars)
		{
#ifdef VERBOSE
			cerr << "\tg[" << lgoal->w2g[j] << "]" << flush;
#endif
			g(lgoal->w2g[j]) = lgoal->q_old(j);
		}
		else
		{
			assert(0);
		}
	}
#ifdef VERBOSE
	cerr << endl;
	cerr << "a = " << tran(a) << endl;
	cerr << "g = " << tran(g) << endl;
#endif
	if((int)_g2w.size() == n_vars)
	{
		for(int i=0; i<n_vars; i++)
		{
			_g2w[i] = -1;
		}
		for(int i=0; i<n_vars; i++)
		{
			if(lgoal->w2g[i] >= 0 && lgoal->w2g[i] < n_vars)
			{
				_g2w[lgoal->w2g[i]] = i;
			}
		}
	}
	delete dp;
	return 0;
}

int LCP::SolvePivot(fVec& g, fVec& a, double _max_error, int _max_iteration, int* n_iteration, std::vector<int>& _g2w)
{
	if(n_iteration) *n_iteration = 0;
	// w = Mz + q, M = [N c]
	fMat M(n_vars, n_vars+1);
	fMat c(n_vars, 1);
	fVec q(n_vars);
	c = 1.0;
	M.set_submat(0, 0, N);
	M.set_submat(0, n_vars, c);
	q.set(r);

	// index mapping
	std::vector<int> z2g, w2g, z2a, w2a;
	int yr2g = -1, yr2a = -1, ys2g = -1, ys2a = -1;
	z2g.resize(n_vars+1);
	z2a.resize(n_vars+1);
	w2g.resize(n_vars);
	w2a.resize(n_vars);
	for(int i=0; i<n_vars; i++)
	{
		z2g[i] = i;
		z2a[i] = -1;
		w2g[i] = -1;
		w2a[i] = i;
	}
	z2g[n_vars] = n_vars;  // z0
	z2a[n_vars] = -1;

	int max_iteration = _max_iteration;
	double max_error = _max_error;
	int terminate = true;
#ifdef VERBOSE
	cerr << "LCP::SolvePivot" << endl;
	cerr << "--- inputs ---" << endl;
	cerr << "M = " << M << endl;
	cerr << "q = " << tran(q) << endl;
#endif
	//
	// step 0
	//
#ifdef VERBOSE
	cerr << "--- step 0 ---" << endl;
#endif
	for(int j=0; j<n_vars; j++)
	{
		if(q(j) < 0.0)
		{
			terminate = false;
			break;
		}
	}
	if(terminate)
	{
#ifdef VERBOSE
		cerr << "q >= 0: trivial solution" << endl;
#endif
		g.resize(n_vars);
		a.resize(n_vars);
		g.zero();
		a.set(r);
		return 0;
	}
	// find jr
	int jr = -1;
	double min_q_c = 0.0;
	for(int j=0; j<n_vars; j++)
	{
		double q_c = q(j) / M(j, n_vars);
		if(jr < 0 || q_c < min_q_c)
		{
			jr = j;
			min_q_c = q_c;
		}
	}
	// pivot w_jr <-> z0
	fMat M_new(n_vars, n_vars+1);
	fVec q_new(n_vars);
	std::vector<int> idx1, idx2;
	idx1.resize(1);
	idx2.resize(1);
	idx1[0] = jr;
	idx2[0] = n_vars;
	swap_index(idx1, idx2, w2a, w2g, z2a, z2g);
	Pivot(idx1, idx2, M, q, M_new, q_new);
	yr2g = z2g[jr];
//	cerr << "pivot result:" << endl;
//	cerr << "M_new = " << M_new << endl;
//	cerr << "q_new = " << tran(q_new) << endl;
	int n_iter;
	for(n_iter=0; n_iter<max_iteration; n_iter++)
	{
#if 0
		for(int i=0; i<n_vars; i++)
		{
			cerr << "w[" << i << "]: a/g = " << w2a[i] << "/" << w2g[i] << endl;
		}
		for(int i=0; i<n_vars+1; i++)
		{
			cerr << "z[" << i << "]: a/g = " << z2a[i] << "/" << z2g[i] << endl;
		}
#endif
		//
		// step1
		//
#ifdef VERBOSE
		cerr << "--- step 1 [iteration " << n_iter << "] ---" << endl;
		cerr << "jr = " << jr << endl;
#endif
		fMat m_jr(n_vars, 1);
		m_jr.get_submat(0, jr, M_new);
		// find js
		double min_q_m = 0.0;
		int js = -1;
		std::vector<int> js_cand;
		for(int j=0; j<n_vars; j++)
		{
			if(q_new(j) >= 0.0 && m_jr(j,0) < 0.0)
			{
				double q_m = - q_new(j) / m_jr(j,0);
#ifdef VERBOSE
				cerr << "[" << j << "]: z = " << w2g[j] << ", q_new/m_jr = " << q_new(j) << "/" << m_jr(j,0) << " = " << q_m << endl;
#endif
				if(js_cand.empty())
				{
//					js = j;
					js_cand.push_back(j);
					min_q_m = q_m;
				}
//				else if(fabs(q_m-min_q_m) < numeric_limits<double>::epsilon())
				else if(fabs(q_m-min_q_m) < max_error)
				{
//					js = j;
					min_q_m = (min_q_m*js_cand.size() + q_m)/(js_cand.size()+1);
					js_cand.push_back(j);
				}
				else if(q_m < min_q_m)
				{
//					js = j;
					js_cand.clear();
					js_cand.push_back(j);
					min_q_m = q_m;
				}
			}
		}
		if(js_cand.empty())
		{
//#ifdef VERBOSE
			cerr << "[LCP] m_jr >= 0: no solution " << endl;
//#endif
			terminate = false;
			break;
		}
		int n_js_cand = js_cand.size();
		double min_q_negative = 0.0;
#ifdef VERBOSE
		cerr << "n_js_cand = " << n_js_cand << endl;
#endif
		for(int j=0; j<n_js_cand; j++)
		{
			idx1[0] = js_cand[j];
			idx2[0] = jr;
			swap_index(idx1, idx2, w2a, w2g, z2a, z2g);
			Pivot(w2a, w2g, z2a, z2g, M, q, M_new, q_new);
			swap_index(idx1, idx2, w2a, w2g, z2a, z2g);
			q_new *= -1.0;
			double q_negative = q_new.max_value();
#ifdef VERBOSE
			cerr << "js_cand[" << j << "] = " << js_cand[j] << endl;
			cerr << "q_negative = " << q_negative << endl;
#endif
			if(w2g[js_cand[j]] == n_vars && q_negative <= max_error)
			{
				js = js_cand[j];
				break;
			}
			if(js < 0 || q_negative < min_q_negative)
			{
				js = js_cand[j];
				min_q_negative = q_negative;
			}
		}
//		if(js < 0 || min_q_negative > max_error)
		if(js < 0)
		{
			cerr << "[LCP] no pivot found" << endl;
			terminate = false;
			break;
		}
#ifdef VERBOSE
		cerr << "js = " << js << endl;
#endif
		if(w2a[js] >= 0)
		{
			ys2a = w2a[js];
			ys2g = -1;
#ifdef VERBOSE
			cerr << "ys is a[" << ys2a << "]" << endl;
#endif
		}
		else if(w2g[js] >= 0)
		{
			ys2g = w2g[js];
			ys2a = -1;
#ifdef VERBOSE
			cerr << "ys is g[" << ys2g << "]" << endl;
#endif
		}
		else
		{
			assert(0);
		}
		//
		// step 2
		//
#ifdef VERBOSE
		cerr << "--- step 2 [iteration " << n_iter << "] ---" << endl;
#endif
		// pivot w[js] <-> z[jr]
		idx1[0] = js;
		idx2[0] = jr;
		swap_index(idx1, idx2, w2a, w2g, z2a, z2g);
		Pivot(w2a, w2g, z2a, z2g, M, q, M_new, q_new);
//		cerr << "pivot result:" << endl;
//		cerr << "M_new = " << M_new << endl;
#ifdef VERBOSE
		cerr << "q_new = " << tran(q_new) << endl;
#endif
#if 1
		double error = CheckPivotResult(q_new, w2a, w2g);
		if(error > 1e-2)
		{
//			cerr << "[LCP] too large error" << endl;
			terminate = false;
			break;
		}
#endif
		if(ys2g == n_vars)  // ys == z0
		{
#ifdef VERBOSE
			cerr << "success" << endl;
#endif
			terminate = true;
			break;
		}
		if(ys2g >= 0)
		{
			yr2a = ys2g;
			yr2g = -1;
#ifdef VERBOSE
			cerr << "yr is a[" << yr2a << "]" << endl;
#endif
			jr = -1;
			for(int j=0; j<=n_vars; j++)
			{
				if(z2a[j] == yr2a)
				{
					jr = j;
					break;
				}
			}
			assert(jr >= 0);
		}
		else if(ys2a >= 0)
		{
			yr2g = ys2a;
			yr2a = -1;
#ifdef VERBOSE
			cerr << "yr is g[" << yr2g << "]" << endl;
#endif
			jr = -1;
			for(int j=0; j<=n_vars; j++)
			{
				if(z2g[j] == yr2g)
				{
					jr = j;
					break;
				}
			}
			assert(jr >= 0);
		}
		else
		{
			assert(0);
		}
#ifdef VERBOSE
		cerr << "g2w = " << endl;
		int* __g2w = new int [n_vars];
		for(int i=0; i<n_vars; i++)
		{
			__g2w[i] = 0;
		}
		for(int i=0; i<n_vars; i++)
		{
			if(w2g[i] >= 0 && w2g[i] < n_vars)
			{
				__g2w[w2g[i]] = 1;
			}
		}
		for(int i=0; i<n_vars; i++)
		{
			cerr << " " << __g2w[i] << flush;
		}
		cerr << endl;
		delete[] __g2w;
#endif
	}
	if(n_iter == max_iteration)
	{
		cerr << "[LCP] did not converge" << endl;
	}
	g.resize(n_vars);
	a.resize(n_vars);
	g.zero();
	a.zero();
	if(terminate)
	{
#ifdef VERBOSE
		cerr << "q_new = " << tran(q_new) << endl;
#endif
		for(int j=0; j<n_vars; j++)
		{
			if(w2a[j] >= 0)
			{
#ifdef VERBOSE
				cerr << "w2a[" << j << "] = " << w2a[j] << endl;
#endif
				a(w2a[j]) = q_new(j);
			}
			else if(w2g[j] >= 0 && w2g[j] != n_vars)
			{
#ifdef VERBOSE
				cerr << "w2g[" << j << "] = " << w2g[j] << endl;
#endif
				g(w2g[j]) = q_new(j);
			}
			else
			{
				assert(0);
			}
		}
#ifdef VERBOSE
		cerr << "a = " << tran(a) << endl;
		cerr << "g = " << tran(g) << endl;
#endif
	}
	if(n_iteration) *n_iteration = n_iter;
	if((int)_g2w.size() == n_vars)
	{
		for(int i=0; i<n_vars; i++)
		{
			_g2w[i] = -1;
		}
		for(int i=0; i<n_vars; i++)
		{
			if(w2g[i] >= 0 && w2g[i] < n_vars)
			{
				_g2w[w2g[i]] = i;
			}
		}
	}
	return !terminate;
}

//
void LCP::Pivot(std::vector<int>& w2a, std::vector<int>& w2g,
				 std::vector<int>& z2a, std::vector<int>& z2g,
				 const fMat& M, const fVec& q,
				 fMat& M_new, fVec& q_new)
{
	int n_row = M.row(), n_col = M.col();
	std::vector<int> a_piv, a_bar, g_piv, g_bar;
	M_new.resize(n_row, n_col);
	q_new.resize(n_row);
	// count number of g elements in w
	for(int i=0; i<n_row; i++)
	{
//		cerr << "w[" << i << "]: a/g = " << w2a[i] << "/" << w2g[i] << endl;
		if(w2g[i] >= 0)
		{
			g_piv.push_back(w2g[i]);
		}
		else
		{
			a_bar.push_back(w2a[i]);
		}
	}
	for(int i=0; i<n_col; i++)
	{
//		cerr << "z[" << i << "]: a/g = " << z2a[i] << "/" << z2g[i] << endl;
		if(z2a[i] >= 0) a_piv.push_back(z2a[i]);
		else g_bar.push_back(z2g[i]);
	}
	int n_pivot = g_piv.size();
#ifdef VERBOSE
	cerr << "n_pivot = " << n_pivot << endl;
#endif
	if(n_pivot == 0)
	{
		M_new.set(M);
		q_new.set(q);
		return;
	}
	int n_row_bar = n_row - n_pivot, n_col_bar = n_col - n_pivot;
	fMat M12bar(n_row-n_pivot, n_col-n_pivot);
	fMat M1(n_pivot, n_col-n_pivot), M2(n_row-n_pivot, n_pivot);
	fMat M12(n_pivot, n_pivot);
	fVec q1(n_pivot), q1bar(n_row-n_pivot);
	for(int i=0; i<n_pivot; i++)
	{
		q1(i) = q(a_piv[i]);
		for(int j=0; j<n_pivot; j++)
		{
			M12(i, j) = M(a_piv[i], g_piv[j]);
		}
		for(int j=0; j<n_col_bar; j++)
		{
			M1(i, j) = M(a_piv[i], g_bar[j]);
		}
	}
	for(int i=0; i<n_row_bar; i++)
	{
		q1bar(i) = q(a_bar[i]);
		for(int j=0; j<n_pivot; j++)
		{
			M2(i, j) = M(a_bar[i], g_piv[j]);
		}
		for(int j=0; j<n_row_bar; j++)
		{
			M12bar(i, j) = M(a_bar[i], g_bar[j]);
		}
	}
#if 0
	cerr << "M12 = " << M12 << endl;
	cerr << "M1 = " << M1 << endl;
	cerr << "M2 = " << M2 << endl;
	cerr << "M12bar = " << M12bar << endl;
	cerr << "q1 = " << tran(q1) << endl;
	cerr << "q1bar = " << tran(q1bar) << endl;
#endif
	fMat Md12(n_pivot, n_pivot);
	fMat Md1(n_pivot, n_col-n_pivot);
	fMat Md2(n_row-n_pivot, n_pivot);
	fMat Md12bar(n_row-n_pivot, n_col-n_pivot);
	fVec qd1(n_pivot), qd1bar(n_row-n_pivot);

	pivot_body(M12, M1, M2, M12bar, q1, q1bar, Md12, Md1, Md2, Md12bar, qd1, qd1bar);

	// output
	int i_piv = 0, i_bar = 0;
	for(int i=0; i<n_row; i++)
	{
		if(w2g[i] >= 0)
		{
			q_new(i) = qd1(i_piv);
			int j_piv = 0, j_bar = 0;
			for(int j=0; j<n_col; j++)
			{
				if(z2a[j] >= 0)
				{
					M_new(i, j) = Md12(i_piv, j_piv);
					j_piv++;
				}
				else
				{
					M_new(i, j) = Md1(i_piv, j_bar);
					j_bar++;
				}
			}
			i_piv++;
		}
		else
		{
			q_new(i) = qd1bar(i_bar);
			int j_piv = 0, j_bar = 0;
			for(int j=0; j<n_col; j++)
			{
				if(z2a[j] >= 0)
				{
					M_new(i, j) = Md2(i_bar, j_piv);
					j_piv++;
				}
				else
				{
					M_new(i, j) = Md12bar(i_bar, j_bar);
					j_bar++;
				}
			}
			i_bar++;
		}
	}
#if 0
//	cerr << "M_new = " << M_new << endl;
//	cerr << "q_new = " << tran(q_new) << endl;
#endif
}

// pivot w[idx1] and z[idx2] in w=Mz+q
void LCP::Pivot(std::vector<int>& idx1, std::vector<int>& idx2,
				const fMat& M, const fVec& q,
				fMat& M_new, fVec& q_new)
{
	assert(idx1.size() == idx2.size());
//	cerr << "pivot->" << endl;
	int n_pivot = idx1.size();
	int n_row = M.row(), n_col = M.col();
	std::vector<int> row_pivot_index;
	std::vector<int> col_pivot_index;
	row_pivot_index.resize(n_row);
	col_pivot_index.resize(n_col);
	for(int i=0; i<n_row; i++) row_pivot_index[i] = -1;
	for(int i=0; i<n_col; i++) col_pivot_index[i] = -1;
	for(int i=0; i<n_pivot; i++) row_pivot_index[idx1[i]] = i;
	for(int i=0; i<n_pivot; i++) col_pivot_index[idx2[i]] = i;

	fMat M12bar(n_row-n_pivot, n_col-n_pivot);
	fMat M1(n_pivot, n_col-n_pivot), M2(n_row-n_pivot, n_pivot);
	fMat M12(n_pivot, n_pivot);
	fVec q1(n_pivot), q1bar(n_row-n_pivot);
	int i_bar = 0;
	for(int i=0; i<n_row; i++)
	{
		int i_piv = row_pivot_index[i];
		if(i_piv >= 0)
		{
			q1(i_piv) = q(i);
			int j_bar = 0;
			for(int j=0; j<n_col; j++)
			{
				int j_piv = col_pivot_index[j];
				if(j_piv >= 0)
				{
					M12(i_piv, j_piv) = M(i, j);
				}
				else
				{
					M1(i_piv, j_bar) = M(i, j);
					j_bar++;
				}
			}
		}
		else
		{
			q1bar(i_bar) = q(i);
			int j_bar = 0;
			for(int j=0; j<n_col; j++)
			{
				int j_piv = col_pivot_index[j];
				if(j_piv >= 0)
				{
					M2(i_bar, j_piv) = M(i, j);
				}
				else
				{
					M12bar(i_bar, j_bar) = M(i, j);
					j_bar++;
				}
			}
			i_bar++;
		}
	}
//	cerr << "M12 = " << M12 << endl;
//	cerr << "M1 = " << M1 << endl;
//	cerr << "M2 = " << M2 << endl;
//	cerr << "M12bar = " << M12bar << endl;
	// new M
	fMat Md12(n_pivot, n_pivot);
	fMat Md1(n_pivot, n_col-n_pivot);
	fMat Md2(n_row-n_pivot, n_pivot);
	fMat Md12bar(n_row-n_pivot, n_col-n_pivot);
	fVec qd1(n_pivot), qd1bar(n_row-n_pivot);

	pivot_body(M12, M1, M2, M12bar, q1, q1bar, Md12, Md1, Md2, Md12bar, qd1, qd1bar);

	// output
	M_new.resize(n_row, n_col);
	q_new.resize(n_row);
	i_bar = 0;
	for(int i=0; i<n_row; i++)
	{
		int i_piv = row_pivot_index[i];
		if(i_piv >= 0)
		{
			q_new(i) = qd1(i_piv);
			int j_bar = 0;
			for(int j=0; j<n_col; j++)
			{
				int j_piv = col_pivot_index[j];
				if(j_piv >= 0)
				{
					M_new(i, j) = Md12(i_piv, j_piv);
				}
				else
				{
					M_new(i, j) = Md1(i_piv, j_bar);
					j_bar++;
				}
			}
		}
		else
		{
			q_new(i) = qd1bar(i_bar);
			int j_bar = 0;
			for(int j=0; j<n_col; j++)
			{
				int j_piv = col_pivot_index[j];
				if(j_piv >= 0)
				{
					M_new(i, j) = Md2(i_bar, j_piv);
				}
				else
				{
					M_new(i, j) = Md12bar(i_bar, j_bar);
					j_bar++;
				}
			}
			i_bar++;
		}
	}
//	cerr << "M_new = " << M_new << endl;
//	cerr << "<- pivot" << endl;
}

void LCP::Pivot(int idx1, int idx2,
				const fMat& M, const fVec& q,
				fMat& M_new, fVec& q_new)
{
	int n_row = M.row(), n_col = M.col();
	static fMat M12bar, M1, M2, M12;
	static fVec q1, q1bar;
	int i_bar = 0;
	M12bar.resize(n_row-1, n_col-1);
	M1.resize(1, n_col-1);
	M2.resize(n_row-1, 1);
	M12.resize(1, 1);
	q1.resize(1);
	q1bar.resize(n_row-1);
	for(int i=0; i<n_row; i++)
	{
		if(i == idx1)
		{
			q1(0) = q(i);
			int j_bar = 0;
			for(int j=0; j<n_col; j++)
			{
				if(j == idx2)
				{
					M12(0, 0) = M(i, j);
				}
				else
				{
					M1(0, j_bar) = M(i, j);
					j_bar++;
				}
			}
		}
		else
		{
			q1bar(i_bar) = q(i);
			int j_bar = 0;
			for(int j=0; j<n_col; j++)
			{
				if(j == idx2)
				{
					M2(i_bar, 0) = M(i, j);
				}
				else
				{
					M12bar(i_bar, j_bar) = M(i, j);
					j_bar++;
				}
			}
			i_bar++;
		}
	}
	// new M
	static fMat Md12;
	static fMat Md1;
	static fMat Md2;
	static fMat Md12bar;
	static fVec qd1, qd1bar;
	Md12.resize(1, 1);
	Md1.resize(1, n_col-1);
	Md2.resize(n_row-1, 1);
	Md12bar.resize(n_row-1, n_col-1);
	qd1.resize(1);
	qd1bar.resize(n_row-1);
	pivot_body(M12, M1, M2, M12bar, q1, q1bar, Md12, Md1, Md2, Md12bar, qd1, qd1bar);

	// output
	M_new.resize(n_row, n_col);
	q_new.resize(n_row);
	i_bar = 0;
	for(int i=0; i<n_row; i++)
	{
		if(i == idx1)
		{
			q_new(i) = qd1(0);
			int j_bar = 0;
			for(int j=0; j<n_col; j++)
			{
				if(j == idx2)
				{
					M_new(i, j) = Md12(0, 0);
				}
				else
				{
					M_new(i, j) = Md1(0, j_bar);
					j_bar++;
				}
			}
		}
		else
		{
			q_new(i) = qd1bar(i_bar);
			int j_bar = 0;
			for(int j=0; j<n_col; j++)
			{
				if(j == idx2)
				{
					M_new(i, j) = Md2(i_bar, 0);
				}
				else
				{
					M_new(i, j) = Md12bar(i_bar, j_bar);
					j_bar++;
				}
			}
			i_bar++;
		}
	}
//	cerr << "M_new = " << M_new << endl;
//	cerr << "<- pivot" << endl;
}

void LCP::pivot_body(const fMat& M12, const fMat& M1, const fMat& M2, const fMat& M12bar, const fVec& q1, const fVec& q1bar,
					 fMat& Md12, fMat& Md1, fMat& Md2, fMat& Md12bar, fVec& qd1, fVec& qd1bar)
{
	int n_pivot = M12.row();
	int n_row_bar = M2.row();
	int n_col_bar = M1.col();
	static fMat M2Md12, M2Md12M1;
	M2Md12.resize(n_row_bar, n_pivot);
	M2Md12M1.resize(n_row_bar, n_col_bar);

	Md12.inv_svd(M12);
	Md1.mul(Md12, M1);
	Md1 *= -1.0;
	Md2.mul(M2, Md12);
	Md12bar.set(M12bar);
//	M2Md12.mul(M2, Md12);
//	M2Md12M1.mul(M2Md12, M1);
//	Md12bar -= M2Md12M1;
	M2Md12M1.mul(M2, Md1);
	Md12bar += M2Md12M1;

#ifdef VERBOSE
//	cerr << "Md12 = " << Md12 << endl;
	cerr << "M12*Md12 = " << M12*Md12 << endl;
//	cerr << "Md1 = " << Md1 << endl;
//	cerr << "Md2 = " << Md2 << endl;
//	cerr << "Md_bar = " << Md_bar << endl;
#endif
	// new q
	static fVec qd1bar_temp;
	qd1bar_temp.resize(n_row_bar);
	qd1.mul(Md12, q1);
	qd1 *= -1.0;
	qd1bar.set(q1bar);
//	qd1bar_temp.mul(M2Md12, q1);
//	qd1bar -= qd1bar_temp;
	qd1bar_temp.mul(M2, qd1);
	qd1bar += qd1bar_temp;
#ifdef VERBOSE
//	cerr << "Md12 = " << Md12 << endl;
//	cerr << "Md1 = " << Md1 << endl;
//	cerr << "Md2 = " << Md2 << endl;
//	cerr << "Md12bar = " << Md12bar << endl;
//	cerr << "qd1 = " << tran(qd1) << endl;
//	cerr << "qd1bar = " << tran(qd1bar) << endl;
#endif
}

void LCP::Pivot(const fMat& M, const fVec& q,
				const fMat& oldMinv, 
				std::vector<int>& old_w2a, std::vector<int>& old_w2g,
				std::vector<int>& old_z2a, std::vector<int>& old_z2g,
				int ys2a, int ys2g, int yr2a, int yr2g,
				std::vector<int>& w2a, std::vector<int>& w2g,
				std::vector<int>& z2a, std::vector<int>& z2g,
				int jr,
				fMat& newMinv, fMat& m_jr, fVec& q_new)
{
	int n_vars = w2a.size();
	std::vector<int> piv2g, piv2a, piv2w, piv2z;
	std::vector<int> non2g, non2a, non2w, non2z;
	std::vector<int> old_piv2g, old_piv2a;
	std::vector<int>::iterator piv2g_iter, piv2w_iter;
	std::vector<int>::iterator non2w_iter, non2a_iter;
	int i;
//	cerr << "w = " << endl;
	for(i=0; i<n_vars; i++)
	{
//		cerr << " [" << i << "] g/a = " << w2g[i] << "/" << w2a[i] << endl;
		if(old_w2g[i] >= 0) old_piv2g.push_back(old_w2g[i]);
		int wgi = w2g[i];
		if(wgi >= 0)
		{
			piv2w.push_back(i);
			piv2g.push_back(wgi);
			continue;
		}
		int wai = w2a[i];
		if(wai >= 0)
		{
			non2w.push_back(i);
			non2a.push_back(wai);
		}
	}
	int jr2piv = -1, jr2non = -1;
//	cerr << "z = " << endl;
	for(i=0; i<=n_vars; i++)  // z has n_vars+1 elements
	{
//		cerr << " [" << i << "] a/g = " << z2a[i] << "/" << z2g[i] << endl;
		if(old_z2a[i] >= 0) old_piv2a.push_back(old_z2a[i]);
		int zai = z2a[i];
		if(zai >= 0)
		{
			piv2z.push_back(i);
			piv2a.push_back(zai);
			if(i == jr) jr2piv = piv2a.size()-1;
			continue;
		}
		int zgi = z2g[i];
		if(zgi >= 0)
		{
			non2z.push_back(i);
			non2g.push_back(zgi);
			if(i == jr) jr2non = non2g.size()-1;
		}
	}
	assert(piv2g.size() == piv2a.size());
	int n_pivot = piv2g.size(), n_none = n_vars-n_pivot;
	int jr2a = z2a[jr], jr2g = z2g[jr];
	static fMat Maa, b, x;
	Maa.resize(n_pivot, n_pivot);
	b.resize(n_pivot, 2);
	x.resize(n_pivot, 2);
	b.zero();
	// b = [e(jr) or m_ab(jr) | q_a]
	if(jr2a >= 0)
	{
		assert(jr2piv >= 0);
		b(jr2piv, 0) = 1.0;
	}
	else
	{
		assert(jr2g >= 0);
		assert(jr2non >= 0);
		for(i=0; i<n_pivot; i++)
		{
			b(i, 0) = M(piv2a[i], jr2g);
		}
	}
	for(i=0; i<n_pivot; i++)
	{
		b(i, 1) = q(piv2a[i]);
	}
	for(i=0; i<n_pivot; i++)
	{
		int pai = piv2a[i];
		for(int j=0; j<n_pivot; j++)
		{
			Maa(i, j) = M(pai, piv2g[j]);
		}
	}
	// compute new Minv
	newMinv.resize(n_pivot, n_pivot);
	if(ys2a >=0)
	{
		if(yr2a >= 0) // row_replaced
		{
#ifdef VERBOSE
			cerr << "row_replaced" << endl;
#endif
			static fMat P, q, m2d, X, y;
			P.resize(n_pivot, n_pivot-1);
			q.resize(n_pivot, 1);
			m2d.resize(1, n_pivot);
			int count = 0;
			for(i=0; i<n_pivot; i++)
			{
				if(piv2a[i] == ys2a)
				{
					for(int j=0; j<n_pivot; j++)
					{
						q(j, 0) = oldMinv(j, i);
					}
				}
				else
				{
					for(int j=0; j<n_pivot; j++)
					{
						P(j, count) = oldMinv(j, i);
					}
					count++;
				}
			}
			for(int j=0; j<n_pivot; j++)
			{
				m2d(0, j) = M(ys2a, piv2g[j]);
			}
			inv_row_replaced(P, q, m2d, X, y);
			for(i=0; i<n_pivot; i++)
			{
				count = 0;
				for(int j=0; j<n_pivot; j++)
				{
					if(piv2a[j] == ys2a)
					{
						newMinv(i,j) = y(i, 0);
					}
					else
					{
						newMinv(i,j) = X(i, count);
						count++;
					}
				}
			}
		}
		else if(yr2g >= 0) // enlarge
		{
#ifdef VERBOSE
			cerr << "enlarge" << endl;
#endif
			static fMat m12, m21, m22, X, y, z, w;
			m12.resize(n_pivot-1, 1);
			m21.resize(1, n_pivot-1);
			m22.resize(1, 1);
			int count1 = 0, count2 = 0;
			for(i=0; i<n_pivot; i++)
			{
				if(piv2a[i] != ys2a) m12(count1++, 0) = M(piv2a[i], yr2g);
				if(piv2g[i] != yr2g) m21(0, count2++) = M(ys2a, piv2g[i]);
			}
			m22(0, 0) = M(ys2a, yr2g);
			inv_enlarge(m12, m21, m22, oldMinv, X, y, z, w);
			count1 = 0;
			for(i=0; i<n_pivot; i++)
			{
				if(piv2g[i] == yr2g)
				{
					int count2 = 0;
					for(int j=0; j<n_pivot; j++)
					{
						if(piv2a[j] == ys2a)
						{
							newMinv(i,j) = w(0,0);
						}
						else
						{
							newMinv(i,j) = z(0, count2);
							count2++;
						}
					}
				}
				else
				{
					int count2 = 0;
					for(int j=0; j<n_pivot; j++)
					{
						if(piv2a[j] == ys2a)
						{
							newMinv(i,j) = y(count1, 0);
						}
						else
						{
							newMinv(i,j) = X(count1, count2);
							count2++;
						}
					}
					count1++;
				}
			}
#ifdef VERBOSE
//			cerr << "Maa = " << Maa << endl;
//			cerr << "m12 = " << m12 << endl;
//			cerr << "m21 = " << m21 << endl;
//			cerr << "m22 = " << m22 << endl;
//			cerr << "newMinv = " << newMinv << endl;
#endif
		}
		else
		{
			assert(0);
		}
	}
	else if(ys2g >= 0)
	{
		if(yr2a >= 0) // shrink
		{
#ifdef VERBOSE
			cerr << "shrink" << endl;
#endif
			static fMat P, q, r, s;
			P.resize(n_pivot, n_pivot);
			q.resize(n_pivot, 1);
			r.resize(1, n_pivot);
			s.resize(1, 1);
			int count1 = 0;
			for(i=0; i<n_pivot+1; i++)
			{
				if(old_piv2g[i] == ys2g)
				{
					int count2 = 0;
					for(int j=0; j<n_pivot+1; j++)
					{
						if(old_piv2a[j] == yr2a)
						{
							s(0,0) = oldMinv(i,j);
						}
						else
						{
							r(0, count2) = oldMinv(i,j);
							count2++;
						}
					}
				}
				else
				{
					int count2 = 0;
					for(int j=0; j<n_pivot+1; j++)
					{
						if(old_piv2a[j] == yr2a)
						{
							q(count1,0) = oldMinv(i,j);
						}
						else
						{
							P(count1, count2) = oldMinv(i,j);
							count2++;
						}
					}
					count1++;
				}
			}
			inv_shrink(P, q, r, s, newMinv);
		}
		else if(yr2g >= 0) // col_replaced
		{
#ifdef VERBOSE
			cerr << "col_replaced" << endl;
#endif
			static fMat P, q, m2d, X, y;
			P.resize(n_pivot-1, n_pivot);
			q.resize(1, n_pivot);
			m2d.resize(n_pivot, 1);
			int count = 0;
			for(i=0; i<n_pivot; i++)
			{
				if(piv2g[i] == yr2g)
				{
					for(int j=0; j<n_pivot; j++)
					{
						q(0, j) = oldMinv(i, j);
					}
				}
				else
				{
					for(int j=0; j<n_pivot; j++)
					{
						P(count, j) = oldMinv(i, j);
					}
					count++;
				}
			}
			for(int j=0; j<n_pivot; j++)
			{
				m2d(j, 0) = M(piv2a[j], yr2g);
			}
			inv_col_replaced(P, q, m2d, X, y);
			count = 0;
			for(i=0; i<n_pivot; i++)
			{
				if(piv2g[i] == yr2g)
				{
					for(int j=0; j<n_pivot; j++)
					{
						newMinv(i,j) = y(0, j);
					}
				}
				else
				{
					for(int j=0; j<n_pivot; j++)
					{
						newMinv(i,j) = X(count, j);
					}
					count++;
				}
			}
		}
		else
		{
			assert(0);
		}
	}
	else
	{
		assert(0);
	}
	x.mul(newMinv, b);
#ifdef VERBOSE
	cerr << "newMinv * Maa = " << newMinv*Maa << endl;
#endif
	if(jr2a >= 0)
	{
		for(i=0; i<n_pivot; i++)
		{
			m_jr(piv2w[i], 0) = x(i, 0);
		}
		for(i=0, non2w_iter=non2w.begin(), non2a_iter=non2a.begin(); i<n_none; i++, non2w_iter++, non2a_iter++)
		{
			int nwi = *non2w_iter, nai = *non2a_iter;
			m_jr(nwi, 0) = 0.0;
			int j;
			for(j=0, piv2g_iter=piv2g.begin(), piv2w_iter=piv2w.begin(); j<n_pivot; j++, piv2g_iter++, piv2w_iter++)
			{
				m_jr(nwi, 0) += M(nai, *piv2g_iter) * m_jr(*piv2w_iter, 0);
			}
		}
	}
	else
	{
		for(i=0; i<n_pivot; i++)
		{
			m_jr(piv2w[i], 0) = -x(i, 0);
		}
		for(i=0, non2w_iter=non2w.begin(), non2a_iter=non2a.begin(); i<n_none; i++, non2w_iter++, non2a_iter++)
		{
			int nwi = *non2w_iter, nai = *non2a_iter;
			m_jr(nwi, 0) = M(nai, jr2g);
			int j;
			for(j=0, piv2g_iter=piv2g.begin(), piv2w_iter=piv2w.begin(); j<n_pivot; j++, piv2g_iter++, piv2w_iter++)
			{
				m_jr(nwi, 0) += M(nai, *piv2g_iter) * m_jr(*piv2w_iter, 0);
			}
		}
	}
//	cerr << "m_jr = " << tran(m_jr) << endl;
	q_new.resize(n_vars);
	q_new.zero();
	for(i=0; i<n_pivot; i++)
	{
		q_new(piv2w[i]) = -x(i, 1);
	}
//	cerr << "q_new(temp) = " << tran(q_new) << endl;
	for(i=0, non2w_iter=non2w.begin(), non2a_iter=non2a.begin(); i<n_none; i++, non2w_iter++, non2a_iter++)
	{
		int nwi = *non2w_iter, nai = *non2a_iter;
		q_new(nwi) = q(nai);
		int j;
		for(j=0, piv2g_iter=piv2g.begin(), piv2w_iter=piv2w.begin(); j<n_pivot; j++, piv2g_iter++, piv2w_iter++)
		{
			q_new(nwi) += M(nai, *piv2g_iter) * q_new(*piv2w_iter);
		}
	}
}

void LCP::Pivot(const fMat& M, const fVec& q,
				std::vector<int>& w2a, std::vector<int>& w2g,
				std::vector<int>& z2a, std::vector<int>& z2g,
				int jr,
				fMat& m_jr, fVec& q_new)
{
	int n_vars = w2a.size();
	std::vector<int> piv2g, piv2a, piv2w, piv2z;
	std::vector<int> non2g, non2a, non2w, non2z;
	std::vector<int>::iterator piv2g_iter, piv2w_iter;
	std::vector<int>::iterator non2w_iter, non2a_iter;
	int i;
//	cerr << "w = " << endl;
	for(i=0; i<n_vars; i++)
	{
//		cerr << " [" << i << "] g/a = " << w2g[i] << "/" << w2a[i] << endl;
		int wgi = w2g[i];
		if(wgi >= 0)
		{
			piv2w.push_back(i);
			piv2g.push_back(wgi);
			continue;
		}
		int wai = w2a[i];
		if(wai >= 0)
		{
			non2w.push_back(i);
			non2a.push_back(wai);
		}
	}
	int jr2piv = -1, jr2non = -1;
//	cerr << "z = " << endl;
	for(i=0; i<=n_vars; i++)  // z has n_vars+1 elements
	{
//		cerr << " [" << i << "] a/g = " << z2a[i] << "/" << z2g[i] << endl;
		int zai = z2a[i];
		if(zai >= 0)
		{
			piv2z.push_back(i);
			piv2a.push_back(zai);
			if(i == jr) jr2piv = piv2a.size()-1;
			continue;
		}
		int zgi = z2g[i];
		if(zgi >= 0)
		{
			non2z.push_back(i);
			non2g.push_back(zgi);
			if(i == jr) jr2non = non2g.size()-1;
		}
	}
	assert(piv2g.size() == piv2a.size());
	int n_pivot = piv2g.size(), n_none = n_vars-n_pivot;
	int jr2a = z2a[jr], jr2g = z2g[jr];
	static fMat Maa, b, x;
	Maa.resize(n_pivot, n_pivot);
	b.resize(n_pivot, 2);
	x.resize(n_pivot, 2);
	b.zero();
//	cerr << "jr = " << jr << ", jr2piv = " << jr2piv << ", jr2non = " << jr2non << endl;
//	cerr << "original q = " << tran(q) << endl;
	// b = [e(jr) or m_ab(jr) | q_a]
	if(jr2a >= 0)
	{
		assert(jr2piv >= 0);
		b(jr2piv, 0) = 1.0;
	}
	else
	{
		assert(jr2g >= 0);
		assert(jr2non >= 0);
		for(i=0; i<n_pivot; i++)
		{
			b(i, 0) = M(piv2a[i], jr2g);
		}
	}
	for(i=0; i<n_pivot; i++)
	{
		b(i, 1) = q(piv2a[i]);
	}
	for(i=0; i<n_pivot; i++)
	{
		int pai = piv2a[i];
		for(int j=0; j<n_pivot; j++)
		{
			Maa(i, j) = M(pai, piv2g[j]);
		}
	}
	x.lineq_svd(Maa, b);
	if(jr2a >= 0)
	{
		for(i=0; i<n_pivot; i++)
		{
			m_jr(piv2w[i], 0) = x(i, 0);
		}
		for(i=0, non2w_iter=non2w.begin(), non2a_iter=non2a.begin(); i<n_none; i++, non2w_iter++, non2a_iter++)
		{
			int nwi = *non2w_iter, nai = *non2a_iter;
			m_jr(nwi, 0) = 0.0;
			int j;
			for(j=0, piv2g_iter=piv2g.begin(), piv2w_iter=piv2w.begin(); j<n_pivot; j++, piv2g_iter++, piv2w_iter++)
			{
				m_jr(nwi, 0) += M(nai, *piv2g_iter) * m_jr(*piv2w_iter, 0);
			}
		}
	}
	else
	{
		for(i=0; i<n_pivot; i++)
		{
			m_jr(piv2w[i], 0) = -x(i, 0);
		}
		for(i=0, non2w_iter=non2w.begin(), non2a_iter=non2a.begin(); i<n_none; i++, non2w_iter++, non2a_iter++)
		{
			int nwi = *non2w_iter, nai = *non2a_iter;
			m_jr(nwi, 0) = M(nai, jr2g);
			int j;
			for(j=0, piv2g_iter=piv2g.begin(), piv2w_iter=piv2w.begin(); j<n_pivot; j++, piv2g_iter++, piv2w_iter++)
			{
				m_jr(nwi, 0) += M(nai, *piv2g_iter) * m_jr(*piv2w_iter, 0);
			}
		}
	}
//	cerr << "m_jr = " << tran(m_jr) << endl;
	q_new.resize(n_vars);
	q_new.zero();
	for(i=0; i<n_pivot; i++)
	{
		q_new(piv2w[i]) = -x(i, 1);
	}
//	cerr << "q_new(temp) = " << tran(q_new) << endl;
	for(i=0, non2w_iter=non2w.begin(), non2a_iter=non2a.begin(); i<n_none; i++, non2w_iter++, non2a_iter++)
	{
		int nwi = *non2w_iter, nai = *non2a_iter;
		q_new(nwi) = q(nai);
		int j;
		for(j=0, piv2g_iter=piv2g.begin(), piv2w_iter=piv2w.begin(); j<n_pivot; j++, piv2g_iter++, piv2w_iter++)
		{
			q_new(nwi) += M(nai, *piv2g_iter) * q_new(*piv2w_iter);
		}
	}
}

double LCP::CheckPivotResult(const fVec& q_new, std::vector<int>& w2a, std::vector<int>& w2g)
{
	static fVec a, g, c;
	a.resize(n_vars);
	g.resize(n_vars);
	c.resize(n_vars);
	double g0=0.0;
	int g0_found = false;
	a.zero();
	g.zero();
	c.zero();
	for(int j=0; j<n_vars; j++)
	{
		if(w2a[j] >= 0)
		{
			a(w2a[j]) = q_new(j);
		}
		else if(w2g[j] >= 0)
		{
			if(w2g[j] == n_vars)
			{
				g0 = q_new(j);
				g0_found = true;
			}
			else
			{
				g(w2g[j]) = q_new(j);
			}
		}
	}
	if(g0_found)
	{
#ifdef VERBOSE
//		cerr << "g0 = " << g0 << endl;
#endif
		c = g0;
	}
	static fVec error;
	error.resize(n_vars);
	error.mul(N, g);
	error -= a;
	error += r;
	error += c;
#ifdef VERBOSE
//	cerr << "error = " << tran(error) << endl;
#endif
	return error.length();
}
