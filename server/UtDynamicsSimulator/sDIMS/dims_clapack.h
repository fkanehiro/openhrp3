/*!
 * @file   dims_clapack.h
 * @author Katsu Yamane
 * @date   06/17/2003
 * @brief  Comprehensive wrapper for CLAPACK functions.
 */

#ifndef __CLAPACK_H__
#define __CLAPACK_H__

//extern "C" 
//{
	//! Solves linear equation using LU decomposition.
	int dims_dgesvx(double* _a, double* _x, double* _b, int _n, int _nrhs);
	//! Solves linear equation using singular-value decomposition.
	int dims_dgelss(double* _a, double* _x, double* _b, int _m, int _n, int _nrhs,
					double* _s, int* _rank, int _lwork);
	//! For positive-definite, symmetric matrices.
	int dims_dporfs(double* _a, double* _x, double* _b, int _m, int _nrhs);
	int dims_dposv(double* _a, double* _x, double* _b, int _m, int _nrhs);
	int dims_dposvx(double* _a, double* _x, double* _b, int _m, int _nrhs, double* _rcond);

	//! Performs singular value decomposition.
	int dims_svd(double* _a, int m, int n, double* _u, double* _sigma, double* _vt);

	//! Eigenvalues / eigenvectors.
	int dims_eigs(int _n, double *_a, double *w);
	int dims_eigs2(int _n, double *_a, double *w);

	//! Computes eigenvalues and eigenvectors.
	/*!
	 * Computes eigenvalues and eigenvectors.
	 * @param[in]  _n  Size of the matrix.
	 * @param[in]  _a  Array of the matrix elements (NxN)
	 * @param[out] _wr Real parts of the eigenvalues (N)
	 * @param[out] _wi Imaginary parts of the eigenvalues (N)
	 * @param[out] _vr Real and imaginary parts of the right eigenvectors (NxN)
	 */
	int dims_dgeev(int _n, double* _a, double* _wr, double* _wi, double* _vr);

	//! Computes eigenvalues only.
	/*!
	 * Computes eigenvalues only.
	 * @param[in]  _n  Size of the matrix.
	 * @param[in]  _a  Array of the matrix elements (NxN)
	 * @param[out] _wr Real parts of the eigenvalues (N)
	 * @param[out] _wi Imaginary parts of the eigenvalues (N)
	 */
	int dims_dgeev_simple(int _n, double* _a, double* _wr, double* _wi);

	//! Computes the determinant.
	/*!
	 * Computes the determinant.
	 * @param[in]  _n  Size of the matrix.
	 * @param[in]  _a  Array of the matrix elements (NxN)
	 * @param[out] _x  Pointer to store the determinant (1)
	 */
	int dims_det(int _n, double* _a, double* _x);

	//! Wrappers of BLAS functions.
	int dims_copy(double* _x, double* _y, int _n);
	int dims_scale_myself(double* _x, double _alpha, int _n);
	int dims_scale(double* _x, double _alpha, int _n, double* _y);
	double dims_dot(double* _x, double* _y, int _n);
	int dims_dgemv(double* _A, int _m, int _n, double* _x, double* _y);
	int dims_dgemv_tran(double* _A, int _m, int _n, double* _x, double* _y);
	int dims_dgemm(double* _A, double* _B, int _m, int _n, int _k, double* _C);
	int dims_dsyrk(double* _A, int _n, int _k, double* _C);
	int dims_dsyrk_trans_first(double* _A, int _n, int _k, double* _C);
	int dims_daxpy(int _n, double _alpha, double* _x, double* _y);
//}

#endif
