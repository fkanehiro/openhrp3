/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*!
 * @file   lcp.h
 * @author Katsu Yamane
 * @date   12/15/2006
 * @brief  Solves a Linear Complementarity Problem (LCP)
 */

#ifndef __LCP_H__
#define __LCP_H__

#include <fMatrix.h>
#include <vector>
#include <assert.h>

//#define PIVOT_MINIMUM_ONLY  // only for test
//#define INVERSE_FROM_SCRATCH  // turn off to compute inverse incrementally
#define ACTIVATE_SAME_STATE  // activate same state check during planning

/*!
 * @class LCP lcp.h
 * Solves a linear complementarity problem (LCP): find g and a that satisfy
 * N*g + r = a, a>=0, g>=0, a^T*g = 0.
 */
class LCP
{
public:
	//! Constructor
	/*!
	 * Constructor.
	 * @param[in] _NN  N matrix
	 * @param[in] _r   r vector
	 */
	LCP(const fMat& _NN, const fVec& _r): N(_NN), r(_r) {
		assert(N.row() == N.col());
		assert(N.row() == r.size());
		n_vars = N.col();
	}
	~LCP() {
	}
	
	//! Solve using iterative method.
	/*!
	 * Solve using iterative method.
	 * @param[out] g  the solution (non-basic variables)
	 * @param[out] a  the solution (basic variables)
	 */
	int Solve(fVec& g, fVec& a);

	//! Solve using iterative method.
	/*!
	 * Solve using iterative method.
	 * @param[out] g  the solution (non-basic variables)
	 * @param[out] a  the solution (basic variables)
	 * @param[in]  g_init the initial value for g
	 * @param[in]  _max_error maximum permissible error
	 * @param[in]  _max_iteration maximum number of iterations
	 * @param[in]  _speed convergence speed
	 * @param[out] n_iteration actual number of iterations
	 */
	int SolveEx(fVec& g, fVec& a, const fVec& g_init, double _max_error = 1e-8, int _max_iteration = 100, double _speed = 0.5, int* n_iteration = 0);

	//! Solve using Lemke's method.
	/*!
	 * Solve using Lemke's method.
	 * @param[out] g  the solution (non-basic variables)
	 * @param[out] a  the solution (basic variables)
	 * @param[in]  _max_error maximum permissible error
	 * @param[in]  _max_iteration maximum number of iteration
	 * @param[out] n_iteration actual number of iterations
	 * @param[out] _g2w  resulting pivot
	 */
	int SolvePivot(fVec& g, fVec& a, double _max_error, int _max_iteration, int* n_iteration, std::vector<int>& _g2w);

	//! Solve by pivot using path planning algorithm.
	/*!
	 * Solve by pivot using path planning algorithm.
	 * @param[out] g  the solution (non-basic variables)
	 * @param[out] a  the solution (basic variables)
	 * @param[in]  _max_error maximum permissible error
	 * @param[in]  _max_iteration maximum number of iteration
	 * @param[out] n_iteration actual number of iterations
	 * @param[out] _g2w  resulting pivot
	 */
	int SolvePivot2(fVec& g, fVec& a, double _max_error, int _max_iteration, int* n_iteration, std::vector<int>& _g2w);

	static void Pivot(int idx1, int idx2,
					  const fMat& M, const fVec& q,
					  fMat& M_new, fVec& q_new);
	static void Pivot(std::vector<int>& idx1, std::vector<int>& idx2,
					  const fMat& M, const fVec& q,
					  fMat& M_new, fVec& q_new);
	static void Pivot(std::vector<int>& w2a, std::vector<int>& w2g,
					  std::vector<int>& z2a, std::vector<int>& z2g,
					  const fMat& M, const fVec& q,
					  fMat& M_new, fVec& q_new);
	static void Pivot(const fMat& M, const fVec& q,
					  std::vector<int>& w2a, std::vector<int>& w2g,
					  std::vector<int>& z2a, std::vector<int>& z2g,
					  int new_jr,
					  fMat& m_jr, fVec& q_new);
	static void Pivot(const fMat& M, const fVec& q,
					  const fMat& oldMinv,
					  std::vector<int>& old_w2a, std::vector<int>& old_w2g,
					  std::vector<int>& old_z2a, std::vector<int>& old_z2g,
					  int ys2a, int ys2g, int yr2a, int yr2g,
					  std::vector<int>& w2a, std::vector<int>& w2g,
					  std::vector<int>& z2a, std::vector<int>& z2g,
					  int jr,
					  fMat& newMinv, fMat& m_jr, fVec& q_new);

	double CheckPivotResult(const fVec& q_new, std::vector<int>& w2a, std::vector<int>& w2g);

	const fMat& Mref() {
		return M;
	}
	const fVec& Qref() {
		return q;
	}

	int NumLoops();
	int NumErrors();
	
protected:
	static void pivot_body(const fMat& M12, const fMat& M1, const fMat& M2, const fMat& M12bar, const fVec& q1, const fVec& q1bar, fMat& Md12, fMat& Md1, fMat& Md2, fMat& Md12bar, fVec& qd1, fVec& qd1bar);

	int check_q(const fVec& q, double max_error) {
		for(int i=0; i<n_vars; i++)
		{
			if(q(i) < -max_error)
			{
				return false;
			}
		}
		return true;
	}

	fMat N;
	fVec r;
	int n_vars;

	fMat M;
	fVec q;
};

#endif
