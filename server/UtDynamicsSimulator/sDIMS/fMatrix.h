/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*!
 * @file   fMatrix.h
 * @author Katsu Yamane
 * @date   06/17/2003
 * @brief  Generic matrix/vector classes.
 */

#ifndef __F_MATRIX_H__
#define __F_MATRIX_H__

#include <dims_common.h>
#include <dims_clapack.h>
#if defined(__ia64) || defined(__x68_64)
#include <cmath>
#else
#include <math.h>
#endif

#include <iostream>
using namespace std;

#include <assert.h>

class fVec;

/*!
 * @class  fMat fMatrix.h
 * @brief  Matrix of generic size.
 * The elements are stored in a one-dimensional array in row-major order.
 */
class fMat 
{
public:
	//! Default constructor.
	/*!
	 * Creates an empty matrix with size 0x0.
	 */
	fMat() {
		p_data = 0;
		n_col = 0;
		n_row = 0;
		m_info = 0;
		temp = 0.0;
	}
	//! Constructor with size and initial values (optional).
	/*!
	 * Creates a matrix with specific size and initial values.
	 * @param[in] m   Row size.
	 * @param[in] n   Column size.
	 * @param[in] ini Array of initial values; should have at least mxn elements (optional).
	 */
	fMat(int m, int n, double* ini = 0) {
		p_data = 0;
		n_col = 0;
		n_row = 0;
		m_info = 0;
		temp = 0.0;
		resize(m, n);
		if(ini)
		{
			int k = m * n;
			for(int i=0; i<k; i++) p_data[i] = ini[i];
		}
	}
	//! Constructor with size and common initial values.
	/*!
	 * Creates a matrix with specific size and the same initial values.
	 * @param[in] m   Row size.
	 * @param[in] n   Column size.
	 * @param[in] ini Initial value for all elements.
	 */
	fMat(int m, int n, double ini) {
		p_data = 0;
		n_col = 0;
		n_row = 0;
		m_info = 0;
		temp = 0.0;
		resize(m, n);
		int k = m * n;
		for(int i=0; i<k; i++) p_data[i] = ini;
	}
	//! Copy constructor.
	/*!
	 * Creates a matrix with the same size and elements as the original.
	 * @param[in] ini  The reference matrix.
	 */
	fMat(const fMat& ini) {
		p_data = 0;
		n_col = 0;
		n_row = 0;
		m_info = ini.m_info;
		temp = ini.temp;
		resize(ini.row(), ini.col());
		int k = ini.row() * ini.col();
		for(int i=0; i<k; i++) p_data[i] = ini.data()[i];
	}
	//! Constructor from a vector.
	/*!
	 * Creates an nx1 matrix from a vector.
	 * @param[in] ini  The reference vector.
	 */
	fMat(const fVec& ini);
	
	//! Destructor.
	~fMat() {
		if(p_data) delete[] p_data;
	}

	//! Changes the matrix size.
	/*!
	 * Changes the size of the matrix.  If the new size is the same as the
	 * previous, nothing happens (memory is not re-allocated).
	 * The values are not initialized.
	 * @param[in] i  The new row size.
	 * @param[in] j  The new column size.
	 * @return       0 in success, -1 if some error happens.
	 */
	int resize(int i, int j) {
		if(n_row == i && n_col == j) return 0;
		if(p_data) delete[] p_data;
		p_data = 0;
		n_row = i;
		n_col = j;
		if(n_row > 0 && n_col > 0)
		{
			int nn = n_row * n_col;
			p_data = new double[nn];
			if(!p_data)
			{
				n_row = 0;
				n_col = 0;
				m_info = 1;
				return -1;
			}
		}
		return 0;
	}
	//! Returns the number of columns.
	int col() const {
		return n_col;
	}
	//! Returns the number of rows.
	int row() const {
		return n_row;
	}
	//! Returns the value of m_info
	int info() {
		return m_info;
	}
	//! Sets m_info.
	void setinfo(int _info) {
		m_info = _info;
	}

	//! Returns the (non-constant) reference to the (i,j) element.
	double& operator() (int i, int j) {
		assert(i>=0 && i<n_row && j>=0 && j<n_col);
		return *(p_data + i + j*n_row);
	}
	
	//! Returns the value of the (i,j) element.
	double operator() (int i, int j) const {
		assert(i>=0 && i<n_row && j>=0 && j<n_col);
		return *(p_data + i + j*n_row);
	}

	//! Assignment from a reference matrix.
	fMat operator = (const fMat& mat);
	//! Assigns the same value to all elements.
	void operator = (double d);
	//! Sets the values from an array.
	void set(double* _d) {
		int n_elements = n_row * n_col;
		dims_copy(_d, p_data, n_elements);
	}

	//! Returns the pointer to the first element.
	double* data() const {
		return p_data;
	}
	//! Converts to an array of double.
	operator double *() {
		return p_data;
	}

	//! Outputs to a stream.
	friend ostream& operator << (ostream& ost, const fMat& mat) {
		int i, j;
		ost << "[" << mat.row() << ", " << mat.col() << "]" << endl;
		for(i=0; i<mat.row(); i++)
		{
			for(j=0; j<mat.col(); j++)
			{
				ost << *(mat.data() + i + j*mat.row());
				if(j != mat.col()-1) ost << "\t";
			}
			if(i == mat.row()-1) ost << flush;
			else ost << endl;
		}
		return ost;
	}
	
	//! Sets a sub-matrix of myself.
	/*!
	 * Sets subM as an subM.n_row x subM.n_col sub-matrix of myself.
	 * @param[in] row_start  First row to set.
	 * @param[in] col_start  First column to set.
	 * @param[in] subM       The original matrix.
	 */
	void set_submat(int row_start, int col_start, const fMat& subM);

	//! Extract a sub-matrix and set to myself.
	/*!
	 * Extracts an n_row x n_col sub-matrix from a large matrix.
	 * @param[in] row_start  First row to extract.
	 * @param[in] col_start  First column to extract.
	 * @param[in] allM       The original matrix.
	 */
	void get_submat(int row_start, int col_start, const fMat& allM);

	//! Returns the transpose of a matrix.
	friend fMat tran(const fMat& mat);

	//! Sets the transpose of a matrix.
	void tran(const fMat& mat);

	//! Transposes a matrix (only for square matrices).
	void tran();

	//! Creates an identity matrix (only for square matrices).
	void identity();

	//! Creates a zero matrix.
	void zero() {
		int ndata = n_row * n_col;
		for(int i=0; i<ndata; i++) p_data[i] = 0.0;
	}
		
	/*!
	 * @name LU
	 * Functions using LU decomposition.
	 */
	/*@{*/
	/*!
	 * @name friend
	 * Friend function versions.
	 */
	 /*@{*/
	//! inverse
	friend fMat inv(const fMat& mat);
	//! pseudo inverse
	friend fMat p_inv(const fMat& mat);
	//! singularity-robust (SR) inverse
	friend fMat sr_inv(const fMat& mat, fVec& w_err, fVec& w_norm, double k);
	//! solve linear equation Ax = b
	friend fMat lineq(const fMat& A, const fMat& b);
	//! solve linear equation Ax = b using SR-inverse
	friend fMat lineq_sr(const fMat& A, fVec& w_err, fVec& w_norm, double k, const fMat& b);
	/*@}*/
	/*!
	 * @name friend
	 * Member function versions.
	 */
	 /*@{*/
	//! inverse
	int inv(const fMat&);
	//! pseudo inverse
	int p_inv(const fMat&);
	//! singularity-robust (SR) inverse
	int sr_inv(const fMat& mat, fVec& w_err, fVec& w_norm, double k);
	//! solve linear equation Ax = b
	int lineq(const fMat& A, const fMat& b);
	//! solve linear equation Ax = b using SR-inverse
	int lineq_sr(const fMat& A, fVec& w_err, fVec& w_norm, double k, const fMat& b);
	/*@}*/
	/*@}*/

	/*!
	 * @name SVD
	 * Functions using singular-value decomposition.
	 */
	/*@{*/
	/*!
	 * @name friend
	 * Friend function versions.
	 */
	 /*@{*/
	//! inverse
	friend fMat inv_svd(const fMat& mat, int lwork = -1);
	//! pseudo inverse
	friend fMat p_inv_svd(const fMat& mat, int lwork = -1);
	//! SR-inverse
	friend fMat sr_inv_svd(const fMat& mat, fVec& w_err, fVec& w_norm, double k, int lwork = -1);
	//! solve linear equation Ax = b
	friend fMat lineq_svd(const fMat& A, const fMat& b, int lwork = -1);
	/*@}*/
	/*!
	 * @name friend
	 * Member function versions.
	 */
	 /*@{*/
	//! inverse
	int inv_svd(const fMat&, int lwork = -1);
	//! pseudo inverse
	int p_inv_svd(const fMat&, int lwork = -1);
	//! SR-inverse
	int sr_inv_svd(const fMat& mat, fVec& w_err, fVec& w_norm, double k, int lwork = -1);
	//! solve linear equation Ax = b
	int lineq_svd(const fMat& A, const fMat& b, int lwork = -1);
	/*@}*/
	/*@}*/

	//! inverse of positive-definite, symmetric matrix
	int inv_posv(const fMat&);
	//! solve linear equation Ax = b, where A is positive-definite, symmetric
	int lineq_posv(const fMat& A, const fMat& b);
	//! inverse of positive-definite, symmetric matrix
	int inv_porfs(const fMat&);
	//! solve linear equation Ax = b, where A is positive-definite, symmetric
	int lineq_porfs(const fMat& A, const fMat& b);

	/*!
	 * @name operators
	 * Operators.
	 */
	/*@{*/
	friend fMat operator - (const fMat& mat);
	void operator += (const fMat& mat);
	void operator -= (const fMat& mat);
	void operator *= (double d);
	void operator /= (double d);

	friend fMat operator + (const fMat& mat1, const fMat& mat2);
	friend fMat operator - (const fMat& mat1, const fMat& mat2);
	friend fMat operator * (const fMat& mat1, const fMat& mat2);
	friend fVec operator * (const fMat& mat, const fVec& vec);
	friend fMat operator * (double d, const fMat& mat);
	friend fMat operator * (const fMat& mat, double d);
	friend fMat operator / (const fMat& mat, double d);
	/*@}*/

	/*!
	 * @name functions
	 * Operators in function forms (faster).
	 */
	/*@{*/
	void set(const fMat& mat);
	void neg(const fMat& mat);
	void add(const fMat& mat1, const fMat& mat2);
	void add(const fMat& mat);
	void sub(const fMat& mat1, const fMat& mat2);
	void mul(const fMat& mat1, const fMat& mat2);
	void mul(double d, const fMat& mat);
	void mul(const fMat& mat, double d);
	void mul_tran(const fMat& mat1, int trans_first);
	void div(const fMat& mat, double d);
	/*@}*/
	
	//! Element-wise multiplication: .* operator in MATLAB
	void dmul(const fMat& mat1, const fMat& mat2);
	//! Element-wise division: ./ operator in MATLAB
	void ddiv(const fMat& mat1, const fMat& mat2);

	//! singular value decomposition
	/*!
	 * Performs singular-value decomposition (SVD) M = U*S*V^T.
	 * @param[out] U      An mxm matrix.
	 * @param[out] Sigma  Vector of min(m, n) elements containing the diagonal elements of S
	 * @param[out] VT     An nxn matrix, transpose of V.
	 */
	int svd(fMat& U, fVec& Sigma, fMat& VT);

	//! Change to symmetric matrix
	/*!
	 * Creates a symmetric matrix
	 * @param[in] t  Command- 'A': average, 'U': use upper triangle, 'L': use lower triangle
	 */
	void symmetric(char t = 'U');

	//! Computes the determinant.
	double det(void);

	//! Compute the eigenvalues.
	friend fMat eigs(const fMat& mat, double *w);
	//! Compute the eigenvalues.
	friend fMat eigs2(const fMat& mat, double *w);
	
	//! Computes the eigenvalues.
	/*!
	 * Computes the eigenvalues.
	 * @param[out] wr  Vector of the real parts.
	 * @param[out] wi  Vector of the imaginary parts.
	 */
	int eig(fVec& wr, fVec& wi);

	//! Computes the eigenvalues and eigenvectors.
	/*!
	 * Computes the eigenvalues and eigenvectors.
	 * @param[out] wr  Vector of the real parts of the eigenvalues.
	 * @param[out] wi  Vector of the imaginary parts of the eigenvlaues.
	 * @param[out] v   Eigenvectors.
	 */
	int eig(fVec& wr, fVec& wi, fMat& v);
	
	//! Computes the eigenvalues and eigenvectors.
	/*!
	 * Computes the eigenvalues and eigenvectors.
	 * @param[out] wr  Vector of the real parts of the eigenvalues.
	 * @param[out] wi  Vector of the imaginary parts of the eigenvlaues.
	 * @param[out] vr  Real parts of the eigenvectors.
	 * @param[out] vi  Imaginary parts of the eigenvectors.
	 */
	int eig(fVec& wr, fVec& wi, fMat& vr, fMat& vi);

	//! Computes the inverse of an enlarged matrix.
	/*!
	 * Computes the inverse of an enlarged matrix:
	 * @verbatim
	   | X  y | = | M11  m12 |-1
	   | z  w |   | m21  m22 |  @endverbatim
	 * where M11, m12, m21, m22, and P=M11^{-1} are known
	 */
	friend int inv_enlarge(const fMat& m12, const fMat& m21, const fMat& m22, const fMat& P, fMat& X, fMat& y, fMat& z, fMat& w);

	//! Computes the inverse of a shrinked matrix.
	/*!
	 * Computes the inverse of a shrinked matrix:
	 * X = M11^(-1) where
	 * @verbatim
	   | P  q | = | M11 m12 |-1
	   | r  s |   | m21 m22 |  @endverbatim
	 * are known
	 */
	friend int inv_shrink(const fMat& P, const fMat& q, const fMat& r, const fMat& s, fMat& X);

	//! Computes the inverse when some rows are replaced.
	/*!
	 * Computes the inverse when some rows are replaced:
	 * @verbatim
	   | X  y | = | M1  |-1
	   |      |   | m2' |  @endverbatim
	 * where
	 * @verbatim
	   | P  q | = | M1 |-1
	   |      |   | m2 |  @endverbatim
	 * are known
	 */
	friend int inv_row_replaced(const fMat& P, const fMat& q, const fMat& m2d, fMat& X, fMat& y);
	
	//! Computes the inverse when some columns are replaced.
	/*!
	 * Computes the inverse when some columns are replaced:
	 * @verbatim
	   | X | = | M1  m2' |-1
	   | y |                @endverbatim
	 * where
	 * @verbatim
	   | P | = | M1  m2 |-1
	   | q |   |        |  @endverbatim
	 * are known
	 */
	friend int inv_col_replaced(const fMat& P, const fMat& q, const fMat& m2d, fMat& X, fMat& y);

protected:
	double* p_data;
	double temp;
	int m_info;
	int n_col;
	int n_row;
};

/*!
 * @class  fVec fMatrix.h
 * @brief  Vector of generic size.
 */
class fVec
	: public fMat
{
public:
	fVec() : fMat() {
	}
	fVec(int m, double* ini = 0) : fMat(m, 1, ini) {
	}
	fVec(int m, double ini) : fMat(m, 1, ini) {
	}
	fVec(const fVec& ini) : fMat(ini) {
	}
	~fVec() {
	}

	//! Size of the vector (same as row()).
	int size() const {
		return n_row;
	}
	//! Change the size.
	void resize(int i) {
		if(n_row == i) return;
		if(p_data) delete[] p_data;
		p_data = 0;
		n_row = i;
		n_col = 1;
		if(n_row > 0)
		{
			p_data = new double[n_row];
		}
	}

	//! Copy a sub-vector of a larger vector.
	void get_subvec(int start, const fVec& allV);
	//! Copy a smaller vector as a sub-vector.
	void set_subvec(int start, const fVec& subV);

	//! Assignment operations.
	void operator = (double d);
	fVec operator = (const fVec& vec);

	//! Sets all elements.
	void set(double* _d) {
		dims_copy(_d, p_data, n_row);
	}

	//! Returns the reference to the i-th element.
	double& operator () (int i) {
		assert(i >= 0 && i < n_row);
		return *(p_data + i);
	}
	//! Returns the value of the i-th element.
	double operator () (int i) const {
		assert(i >= 0 && i < n_row);
		return *(p_data + i);
	}

	//! Length of the vector.
	friend double length(const fVec& v) {
		return sqrt(v*v);
	}
	//! Length of the vector.
	double length() const {
		return sqrt((*this) * (*this));
	}
	//! Returns a unit vector with the same direction (no length check!).
	friend fVec unit(const fVec& v) {
		fVec ret(v.n_col);
		double len = v.length();
		ret = v / len;
		return ret;
	}
	//! Converts to a unit vector (no length check!).
	void unit() {
		double len = length();
		(*this) /= len;
	}
	//! Returns the sum of the elements.
	double sum() {
		double ret = 0.0;
		for(int i=0; i<n_row; i++) ret += p_data[i];
		return ret;
	}
	//! Creates a zero vector.
	void zero() {
		for(int i=0; i<n_row; i++) p_data[i] = 0;
	}

	//! Returns the index of the largest element.
	int max_index();
	//! Returns the maximum value.
	double max_value();
	//! Returns the index of the smallest element.
	int min_index();
	//! Returns the minimum value.
	double min_value();

	//! Converts all elements to their absolute values.
	void abs();

	//! Solves linear equation Ax = b.
	int lineq(const fMat& A, const fVec& b);
	int lineq_svd(const fMat& A, const fVec& b, int lwork = -1);
	int lineq_posv(const fMat& A, const fVec& b);
	int lineq_porfs(const fMat& A, const fVec& b);
	int lineq_sr(const fMat& A, fVec& w_err, fVec& w_norm, double k, const fVec& b);

	/*!
	 * @name Operators
	 * Operators
	 */
	/*@{*/
	friend fVec operator - (const fVec& vec);
	void operator += (const fVec& vec);
	void operator -= (const fVec& vec);
	void operator *= (double d);
	void operator /= (double d);

	friend fVec operator + (const fVec& vec1, const fVec& vec2);
	friend fVec operator - (const fVec& vec1, const fVec& vec2);
	friend fVec operator / (const fVec& vec, double d);
	friend fVec operator * (double d, const fVec& vec);
	friend fVec operator * (const fVec& vec, double d);
	friend double operator * (const fVec& vec1, const fVec& vec2);
	/*@}*/

	/*!
	 * @name Functions
	 * Operators in function forms (faster).
	 */
	/*@{*/
	void set(const fVec& vec);
	void neg(const fVec& vec);
	void add(const fVec& vec1, const fVec& vec2);
	void add(const fVec& vec);
	void sub(const fVec& vec1, const fVec& vec2);
	void div(const fVec& vec, double d);
	void mul(const fVec& vec, double d);
	void mul(double d, const fVec& vec);
	//! M * v
	void mul(const fMat& mat, const fVec& vec);
	//! v^T * M
	void mul(const fVec& vec, const fMat& mat);
	/*@}*/
protected:
};

#endif
