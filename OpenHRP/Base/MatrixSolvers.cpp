// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-
/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */

#include <iostream>
#include <vector>
#include "MatrixSolvers.h"

using namespace std;

#ifdef USE_CLAPACK_INTERFACE
extern "C" {
#include <cblas.h>
#include <clapack.h>
};
#else
extern "C" void dgesvx_(char *fact, char *trans, int *n,
						int *nrhs, double *a, int *lda,
						double *af, int *ldaf, int *ipiv,
						char *equed, double *r, double *c,
						double *b, int *ldb, double *x,
						int *ldx, double *rcond, double *ferr,
						double *berr, double *work, int *iwork,
						int *info);

extern "C" void dgesv_(const int* n, const int* nrhs, 
					   double* a, int* lda, int* ipiv, 
					   double* b, int* ldb, int* info);

extern "C" void dgetrf_( int *m, int *n, double *a, int *lda, int *ipiv, int *info);
#endif

extern "C" void dgesvd_(char const* jobu, char const* jobvt, 
						int const* m, int const* n, double* a, int const* lda, 
						double* s, double* u, int const* ldu, 
						double* vt, int const* ldvt,
						double* work, int const* lwork, int* info);

extern "C" void dgeev_(char const*jobvl, char const*jobvr, int *n, double *A, 
					  int *lda, double *wr,double *wi, double *vl, 
					  int *ldvl, double *vr, int *ldvr, double *work, int *lwork, int *info);


// originally in hrpCLAPACK.{cpp,h}
// solveLinearEquation()
// b = a * x, x = b^(-1) * a
namespace OpenHRP {
		
		static inline int max(int a, int b) { return (a >= b) ? a : b; }
		static inline int min(int a, int b) { return (a <= b) ? a : b; }
		

		int solveLinearEquationLU(dmatrix a, const dmatrix &b, dmatrix &out_x)
		{
				assert(a.size1() == a.size2() && a.size2() == b.size1() );

				out_x = b;

				const int n = a.size1();
				const int nrhs = b.size2();
				int info;
				std::vector<int> ipiv(n);

#ifndef USE_CLAPACK_INTERFACE

				int lda = n;
				int ldb = n;
				dgesv_(&n, &nrhs, &(a(0,0)), &lda, &(ipiv[0]), &(out_x(0,0)), &ldb, &info);
#else
				info = clapack_dgesv(CblasRowMajor,
									 n, nrhs, &(a(0,0)), n, 
									 &(ipiv[0]),
									 &(out_x(0,0)),
									 n);
#endif
				assert(info == 0);
				
				return info;
		}
				
		
		/**
		   solve linear equation using LU decomposition
		   by lapack library DGESVX (_a must be square matrix)
		*/
		int solveLinearEquationLU(const dmatrix &_a, const dvector &_b, dvector &_x)
		{
				assert(_a.size2() == _a.size1() && _a.size2() == _b.size() );

				typedef boost::numeric::ublas::matrix<double> mlapack;
				typedef boost::numeric::ublas::vector<double> vlapack;

				int n = (int)_a.size2();
				int nrhs = 1;

				mlapack a = _a; // <-

				int lda = n;

				//int *ipiv = new int[n];
				std::vector<int> ipiv(n);
				vlapack x(n);

				int ldb = n;

				int info;

				// compute the solution
#ifndef USE_CLAPACK_INTERFACE
  				char fact      = 'N';
				char transpose = 'T';

				double *af = new double[n*n];

				int ldaf = n;

				char equed = 'N';

				double *r = new double[n];
				double *c = new double[n];

				vlapack b = _b;
		   
				int ldx = n;

				double rcond;

				double *ferr = new double[nrhs];
				double *berr = new double[nrhs];
				double *work = new double[4*n];

				int *iwork = new int[n];

				dgesvx_(&fact, &transpose, &n, &nrhs, &(a(0,0)), &lda, af, &ldaf, &(ipiv[0]),
						&equed, r, c, &(b(0)), &ldb, &(x(0)), &ldx, &rcond,
						ferr, berr, work, iwork, &info);

				delete [] iwork;
				delete [] work;
				delete [] berr;
				delete [] ferr;
				delete [] c;
				delete [] r;

				delete [] af;
#else
				x = _b;
				info = clapack_dgesv(CblasRowMajor,
									 n, nrhs, &(a(0,0)), lda, &(ipiv[0]),
									 &(x(0)), ldb);
#endif
				_x = x;		// result

				//delete [] ipiv;

				return info;
		}

		/**
		   solve linear equation using SVD(Singular Value Decomposition)
		   by lapack library DGESVD (_a can be non-square matrix)
		*/
		int solveLinearEquationSVD(const dmatrix &_a, const dvector &_b, dvector &_x, double _sv_ratio)
		{
				const int m = _a.size1();
				const int n = _a.size2();
				assert( m == static_cast<int>(_b.size()) );
				_x.resize(n);

				int i, j;
				char jobu  = 'A';
				char jobvt = 'A';
        
				int max_mn = max(m,n);
				int min_mn = min(m,n);

				dmatrix a(m,n);
				a = _a;

				int lda = m;
				double *s = new double[max_mn];		// singular values
				int ldu = m;
				double *u = new double[ldu*m];
				int ldvt = n;
				double *vt = new double[ldvt*n];

				int lwork = max(3*min_mn+max_mn, 5*min_mn);     // for CLAPACK ver.2 & ver.3
				double *work = new double[lwork];
				int info;

				for(i = 0; i < max_mn; i++) s[i] = 0.0;

				dgesvd_(&jobu, &jobvt, &m, &n, &(a(0,0)), &lda, s, u, &ldu, vt, &ldvt, work,
						&lwork, &info);

				double tmp;

				double smin, smax=0.0;
				for (j = 0; j < min_mn; j++) if (s[j] > smax) smax = s[j];
				smin = smax*_sv_ratio; // 1.0e-3;
				for (j = 0; j < min_mn; j++) if (s[j] < smin) s[j] = 0.0;
	
				double *utb = new double[m];		// U^T*b

				for (j = 0; j < m; j++){
						tmp = 0;
						if (s[j]){
								for (i = 0; i < m; i++) tmp += u[j*m+i] * _b(i);
								tmp /= s[j];
						}
						utb[j] = tmp;
				}

				// v*utb
				for (j = 0; j < n; j++){
						tmp = 0;
						for (i = 0; i < n; i++){
								if(s[i]) tmp += utb[i] * vt[j*n+i];
						}
						_x(j) = tmp;
				}

				delete [] utb;
				delete [] work;
				delete [] vt;
				delete [] s;
				delete [] u;
	
				return info;
		}

		//======================= Unified interface to solve a linear equation =============================
		/**
		   Unified interface to solve a linear equation
		*/
		int solveLinearEquation(const dmatrix &_a, const dvector &_b, dvector &_x, double _sv_ratio)
		{
				if(_a.size2() == _a.size1())
						return solveLinearEquationLU(_a, _b, _x);
				else
						return solveLinearEquationSVD(_a, _b, _x,  _sv_ratio);
		}
		
		//======================= Calculate pseudo-inverse =================================================
		/**
		   calculate Pseudo-Inverse using SVD(Singular Value Decomposition)
		   by lapack library DGESVD (_a can be non-square matrix)
		*/
		int calcPseudoInverse(const dmatrix &_a, dmatrix &_a_pseu, double _sv_ratio)
		{
				int i, j, k;
				char jobu  = 'A';
				char jobvt = 'A';
				int m = (int)_a.size1();
				int n = (int)_a.size2();
				int max_mn = max(m,n);
				int min_mn = min(m,n);

				dmatrix a(m,n);
				a = _a;

				int lda = m;
				double *s = new double[max_mn];
				int ldu = m;
				double *u = new double[ldu*m];
				int ldvt = n;
				double *vt = new double[ldvt*n];
				int lwork = max(3*min_mn+max_mn, 5*min_mn);     // for CLAPACK ver.2 & ver.3
				double *work = new double[lwork];
				int info;

				for(i = 0; i < max_mn; i++) s[i] = 0.0;
		   
				dgesvd_(&jobu, &jobvt, &m, &n, &(a(0,0)), &lda, s, u, &ldu, vt, &ldvt, work,
						&lwork, &info);


				double smin, smax=0.0;
				for (j = 0; j < min_mn; j++) if (s[j] > smax) smax = s[j];
				smin = smax*_sv_ratio; 			// default _sv_ratio is 1.0e-3
				for (j = 0; j < min_mn; j++) if (s[j] < smin) s[j] = 0.0;

				//------------ calculate pseudo inverse   pinv(A) = V*S^(-1)*U^(T)
				// S^(-1)*U^(T)
				for (j = 0; j < m; j++){
						if (s[j]){
								for (i = 0; i < m; i++) u[j*m+i] /= s[j];
						}
						else {
								for (i = 0; i < m; i++) u[j*m+i] = 0.0;
						}
				}

				// V * (S^(-1)*U^(T)) 
				_a_pseu.resize(n,m);
				for(j = 0; j < n; j++){
						for(i = 0; i < m; i++){
								_a_pseu(j,i) = 0.0;
								for(k = 0; k < min_mn; k++){
										if(s[k]) _a_pseu(j,i) += vt[j*n+k] * u[k*m+i];
								}
						}
				}

				delete [] work;
				delete [] vt;
				delete [] s;
				delete [] u;

				return info;
		}

		//----- Calculation of eigen vectors and eigen values -----
		int calcEigenVectors(const dmatrix &_a, dmatrix  &_evec, dvector &_eval)
		{
				assert( _a.size2() == _a.size1() );

				typedef boost::numeric::ublas::matrix<double> mlapack;
				typedef boost::numeric::ublas::vector<double> vlapack;
				
				mlapack a    = _a; // <-
				mlapack evec = _evec;
				vlapack eval = _eval;
				
				int n = (int)_a.size2();
				
				double *wi = new double[n];
				double *vl = new double[n*n];
				double *work = new double[4*n];

				int lwork = 4*n;
				int info;
				
				dgeev_("N","V", &n, &(a(0,0)), &n, &(eval(0)), wi, vl, &n, &(evec(0,0)), &n, work, &lwork, &info);
				
				_evec = trans(evec);
				_eval = eval;
				
				delete [] wi;
				delete [] vl;
				delete [] work;
				
				return info;
		}


		//--- Calculation of determinamt ---
		double det(const dmatrix &_a)
		{
				assert( _a.size2() == _a.size1() );

				typedef boost::numeric::ublas::matrix<double> mlapack;
				mlapack a = _a;	// <-

				int info;
				int n = a.size2();
				int lda = n;
				std::vector<int> ipiv(n);

#ifdef USE_CLAPACK_INTERFACE
				info = clapack_dgetrf(CblasRowMajor,
									  n, n, &(a(0,0)), lda, &(ipiv[0]));
#else
				dgetrf_(&n, &n, &a(0,0), &lda, &(ipiv[0]), &info);
#endif

				double det=1.0;
	
				for(int i=0; i < n-1; i++)
						if(ipiv[i] != i+1)  det = -det;
				
				for(int i=0; i < n; i++)  det *= a(i,i);

				assert(info == 0);
				
				return det;
		}

} // namespace OpenHRP
