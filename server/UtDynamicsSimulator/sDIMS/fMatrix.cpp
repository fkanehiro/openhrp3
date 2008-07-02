/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*!
 * @file   fMatrix.cpp
 * @author Katsu Yamane
 * @date   06/17/2003
 * @brief  Function implementations for fMat/fVec classes.
 */

#include "fMatrix.h"

/**
 ** fMat
 **/
fMat::fMat(const fVec& ini)
{
	p_data = 0;
	n_col = 0;
	n_row = 0;
	m_info = 0;
	int k = ini.size();
	resize(k, 1);
	dims_copy(ini.p_data, p_data, k);
}

void fMat::get_submat(int row_start, int col_start, const fMat& allM)
{
	assert(row_start >= 0 && col_start >= 0 && 
		   row_start+n_row <= allM.row() &&
		   col_start+n_col <= allM.col());
	for(int i=0; i<n_col; i++)
	{
		double* m = allM.data() + allM.row()*(i+col_start) + row_start;
		double* p = p_data + n_row*i;
		for(int j=0; j<n_row; j++, p++, m++)
		{
			*p = *m;
		}
	}
}

void fMat::set_submat(int row_start, int col_start, const fMat& subM)
{
	int row_span = subM.row(), col_span = subM.col();
	assert(row_start >= 0 && col_start >= 0 &&
		   row_start+row_span <= n_row &&
		   col_start+col_span <= n_col);
	for(int i=0; i<col_span; i++)
	{
		double* m = subM.data() + i*subM.row();
		double* p = p_data + n_row*(i+col_start) + row_start;
		for(int j=0; j<row_span; j++, p++, m++)
		{
			*p = *m;
		}
	}
}

void fVec::get_subvec(int start, const fVec& allV)
{
	assert(start >= 0 && start+n_row <= allV.size());
	double* m = allV.data() + start;
	double* p = p_data;
	for(int i=0; i<n_row; i++, p++, m++)
	{
		*p = *m;
	}
}

void fVec::set_subvec(int start, const fVec& subV)
{
	int row_span = subV.size();
	assert(start >= 0 && start+row_span <= n_row);
	double* m = subV.data();
	double* p = p_data + start;
	for(int i=0; i<row_span; i++, p++, m++)
	{
		*p = *m;
	}
}

void fMat::symmetric(char t)
{
	assert(n_row == n_col);
	double *p1, *p2;
	if(t == 'A')
	{
		// use average
		double sum;
		for(int i=0; i<n_row; i++)
		{
			int j;
			for(j=0, p1=p_data+i, p2=p_data+n_row*i; j<i; j++, p1+=n_row, p2++)
			{
				sum = *p1 + *p2;
				sum *= 0.5;
				*p1 = sum;
				*p2 = sum;
			}
		}
	}
	else if(t == 'U')
	{
		// use upper triangle
		for(int i=0; i<n_row; i++)
		{
			int j;
			for(j=0, p1=p_data+i, p2=p_data+n_row*i; j<i; j++, p1+=n_row, p2++)
				*p1 = *p2;
		}
	}
	else if(t == 'L')
	{
		// use lower triangle
		for(int i=0; i<n_row; i++)
		{
			int j;
			for(j=0, p1=p_data+i, p2=p_data+n_row*i; j<i; j++, p1+=n_row, p2++)
				*p2 = *p1;
		}
	}
	else
	{
		assert(0);
	}
}

/*
 * LU decomposition
 */
fMat lineq(const fMat& A, const fMat& b)
{
	assert(A.row() == b.row());
	fMat x(A.col(), b.col());
	double *a, *bb, *xx;
	a = A.data();
	bb = b.data();
	xx = x.data();
	int n = A.row(), nrhs = b.col();
	x.m_info = dims_dgesvx(a, xx, bb, n, nrhs);
	return x;
}

int fMat::lineq(const fMat& A, const fMat& b)
{
	assert(n_row == A.col() && n_col == b.col() && A.row() == b.row());
	double *a, *bb, *xx;
	int ret;
	a = A.data();
	bb = b.data();
	xx = p_data;
	int n = A.row(), nrhs = b.col();
	ret = dims_dgesvx(a, xx, bb, n, nrhs);
	return ret;
}

int fVec::lineq(const fMat& A, const fVec& b)
{
	assert(n_row == A.col() && A.row() == b.row());
	double *a, *bb, *xx;
	int ret;
	a = A.data();
	bb = b.data();
	xx = p_data;
	int n = A.row(), nrhs = 1;
	ret = dims_dgesvx(a, xx, bb, n, nrhs);
	return ret;
}

fMat inv(const fMat& mat)
{
	assert(mat.row() == mat.col());
	fMat ret(mat.row(), mat.col());
	double *a, *b, *x;
	int n = mat.row(), nrhs = mat.row();
	fMat I(mat.row(), mat.col());
	I.identity();
	a = mat.data();
	x = ret.data();
	b = I.data();
	ret.m_info = dims_dgesvx(a, x, b, n, nrhs);
	return ret;
}

fMat p_inv(const fMat& mat)
{
	fMat ret(mat.col(), mat.row());
	fMat tmat;
	fMat MM, MMinv;

	if(mat.row() < mat.col())
	{
		tmat.resize(mat.n_col, mat.n_row);
		tmat.tran(mat);
		MM.resize(mat.row(), mat.row());
		MMinv.resize(mat.row(), mat.row());
		MM.mul(mat, tmat);
		MMinv.inv(MM);
		ret.mul(tmat, MMinv);
		ret.m_info = MMinv.m_info;
	}
	else if(mat.row() > mat.col())
	{
		tmat.resize(mat.n_col, mat.n_row);
		tmat.tran(mat);
		MM.resize(mat.col(), mat.col());
		MMinv.resize(mat.col(), mat.col());
		MM.mul(tmat, mat);
		MMinv.inv(MM);
		ret.mul(MMinv, tmat);
		ret.m_info = MMinv.m_info;
	}
	else
	{
		ret.inv(mat);
	}
	return ret;
}

fMat sr_inv(const fMat& mat, fVec& w_err, fVec& w_norm, double k)
{
	assert(w_err.row() == mat.row() && w_norm.row() == mat.col());
	fMat ret(mat.n_col, mat.n_row);

	fMat Jhat, tJhat;
	Jhat.resize(mat.row(), mat.col());
	tJhat.resize(mat.col(), mat.row());
	int i, j, r = mat.n_row, c = mat.n_col;
	Jhat.set(mat);
	for(i=0; i<r; i++)
	{
		if(w_err(i) < 1e-8) 
		{
			w_err(i) = 1.0;
		}
		for(j=0; j<c; j++)
		{
			Jhat(i, j) *= w_err(i);
		}
	}
	for(j=0; j<c; j++)
	{
		if(w_norm(j) < 1e-8) 
		{
			w_norm(j) = 1.0;
		}
		for(i=0; i<r; i++)
		{
			Jhat(i, j) /= w_norm(j);
		}
	}
	tJhat.tran(Jhat);
	static fMat tmp, minv;
	if(mat.n_row < mat.n_col)
	{
		tmp.resize(mat.n_row, mat.n_row);
		minv.resize(mat.n_row, mat.n_row);
		tmp.mul(Jhat, tJhat);
		for(i=0; i<mat.n_row; i++)
		{
			tmp(i, i) += k;
		}
		minv.inv(tmp);
		ret.mul(tJhat, minv);
		ret.m_info = minv.m_info;
	}
	else
	{
		tmp.resize(mat.n_col, mat.n_col);
		minv.resize(mat.n_col, mat.n_col);
		tmp.mul(tJhat, Jhat);
		for(i=0; i<mat.n_col; i++)
		{
			tmp(i, i) += k;
		}
		minv.inv(tmp);
		ret.mul(minv, tJhat);
		ret.m_info = minv.m_info;
	}
	for(i=0; i<mat.n_col; i++)
	{
		for(j=0; j<mat.n_row; j++)
		{
			ret(i, j) *= w_err(j) / w_norm(i);
		}
	}
	return ret;
}

int fMat::inv(const fMat& mat)
{
	assert(n_row == n_col && n_row == mat.n_row && n_col == mat.n_col);
	double *a, *b, *x;
	int n = mat.row(), nrhs = mat.row();
	fMat I(mat.row(), mat.col());
	I.identity();
	a = mat.data();
	x = p_data;
	b = I.data();
	{
		m_info = dims_dgesvx(a, x, b, n, nrhs);
	}
	return m_info;
}

int fMat::p_inv(const fMat& mat)
{
	assert(n_row == mat.n_col && n_col == mat.n_row);
	fMat tmat;
	fMat MM, MMinv;

	if(mat.row() < mat.col())
	{
		tmat.resize(mat.n_col, mat.n_row);
		tmat.tran(mat);
		MM.resize(mat.row(), mat.row());
		MMinv.resize(mat.row(), mat.row());
		MM.mul_tran(mat, false);
		MMinv.inv(MM);
		mul(tmat, MMinv);
		m_info = MMinv.m_info;
	}
	else if(mat.row() > mat.col())
	{
		tmat.resize(mat.n_col, mat.n_row);
		tmat.tran(mat);
		MM.resize(mat.col(), mat.col());
		MMinv.resize(mat.col(), mat.col());
		MM.mul_tran(mat, true);
		MMinv.inv(MM);
		mul(MMinv, tmat);
		m_info = MMinv.m_info;
	}
	else inv(mat);
	return m_info;
}

int fMat::sr_inv(const fMat& mat, fVec& w_err, fVec& w_norm, double k)
{
	assert(n_col == mat.n_row && n_row == mat.n_col &&
		   w_err.row() == mat.row() && w_norm.row() == mat.col());
	fMat Jhat, tJhat;
	Jhat.resize(mat.row(), mat.col());
	tJhat.resize(mat.col(), mat.row());
	int i, j, r = mat.n_row, c = mat.n_col;
	Jhat.set(mat);
	for(i=0; i<r; i++)
	{
		if(w_err(i) < 1e-8) 
		{
			w_err(i) = 1.0;
		}
		for(j=0; j<c; j++)
		{
			Jhat(i, j) *= w_err(i);
		}
	}
	for(j=0; j<c; j++)
	{
		if(w_norm(j) < 1e-8) 
		{
			w_norm(j) = 1.0;
		}
		for(i=0; i<r; i++)
		{
			Jhat(i, j) /= w_norm(j);
		}
	}
	tJhat.tran(Jhat);
	fMat tmp, minv;
	if(mat.n_row < mat.n_col)
	{
		tmp.resize(mat.n_row, mat.n_row);
		minv.resize(mat.n_row, mat.n_row);
		tmp.mul(Jhat, tJhat);
		for(i=0; i<mat.n_row; i++)
		{
			tmp(i, i) += k;
		}
		minv.inv(tmp);
		mul(tJhat, minv);
		m_info = minv.m_info;
	}
	else
	{
		tmp.resize(mat.n_col, mat.n_col);
		minv.resize(mat.n_col, mat.n_col);
		tmp.mul(tJhat, Jhat);
		for(i=0; i<mat.n_col; i++)
		{
			tmp(i, i) += k;
		}
		minv.inv(tmp);
		mul(minv, tJhat);
		m_info = minv.m_info;
	}
	for(i=0; i<mat.n_col; i++)
	{
		for(j=0; j<mat.n_row; j++)
		{
			(*this)(i, j) *= w_err(j) / w_norm(i);
		}
	}
	return m_info;
}

/*
 * SVD
 */
fMat lineq_svd(const fMat& A, const fMat& b, int lwork)
{
	assert(A.row() == b.row());
	fMat x(A.col(), b.col());
	double *a, *bb, *xx, *s;
	a = A.data();
	bb = b.data();
	xx = x.data();
	int m = A.row(), n = A.col(), nrhs = b.col(), rank;
	if(m < n) s = new double[m];
	else s = new double[n];
	dims_dgelss(a, xx, bb, m, n, nrhs, s, &rank, lwork);
	delete[] s;
	return x;
}

int fMat::lineq_svd(const fMat& A, const fMat& b, int lwork)
{
	assert(n_row == A.col() && n_col == b.col() && A.row() == b.row());
	if(A.col() == 1)
	{
		fMat AA(1,1);
		fMat Ainv(A.col(), A.row());
		AA.mul_tran(A, true);
		Ainv.tran(A);
		Ainv /= AA(0,0);
		mul(Ainv, b);
		return 0;
	}
	else if(A.row() == 1)
	{
		fMat AA(1,1);
		fMat Ainv(A.col(), A.row());
		AA.mul_tran(A, false);
		Ainv.tran(A);
		Ainv /= AA(0,0);
		mul(Ainv, b);
		return 0;
	}
	double *a, *bb, *xx, *s;
	int ret;
	a = A.data();
	bb = b.data();
	xx = p_data;
	int m = A.row(), n = A.col(), nrhs = b.col(), rank;
	if(m<n) s = new double[m];
	else s = new double[n];
	ret = dims_dgelss(a, xx, bb, m, n, nrhs, s, &rank, lwork);
	delete[] s;
	return ret;
}

int fVec::lineq_svd(const fMat& A, const fVec& b, int lwork)
{
	assert(n_row == A.col() && A.row() == b.row());
	double *a, *bb, *xx, *s;
	int ret;
	a = A.data();
	bb = b.data();
	xx = p_data;
	int m = A.row(), n = A.col(), nrhs = 1, rank;
	if(m<n) s = new double[m];
	else s = new double[n];
	ret = dims_dgelss(a, xx, bb, m, n, nrhs, s, &rank, lwork);
	delete[] s;
	return ret;
}

fMat inv_svd(const fMat& mat, int lwork)
{
	assert(mat.row() == mat.col());
	fMat ret(mat.row(), mat.col());
	double *a, *b, *x, *s;
	int m = mat.row(), n = mat.col(), nrhs = mat.row(), rank;
	fMat I(mat.row(), mat.col());
	I.identity();
	a = mat.data();
	x = ret.data();
	b = I.data();
	s = new double[n];
	ret.m_info = dims_dgelss(a, x, b, m, n, nrhs, s, &rank, lwork);
	delete[] s;
	return ret;
}

fMat p_inv_svd(const fMat& mat, int lwork)
{
	fMat ret(mat.col(), mat.row());
	static fMat tmat;
	static fMat MM, MMinv;

	if(mat.row() < mat.col())
	{
		tmat.resize(mat.n_col, mat.n_row);
		tmat.tran(mat);
		MM.resize(mat.row(), mat.row());
		MMinv.resize(mat.row(), mat.row());
		MM.mul(mat, tmat);
		MMinv.inv_svd(MM, lwork);
		ret.mul(MMinv, tmat);
		ret.m_info = MMinv.m_info;
	}
	else if(mat.row() > mat.col())
	{
		tmat.resize(mat.n_col, mat.n_row);
		tmat.tran(mat);
		MM.resize(mat.col(), mat.col());
		MMinv.resize(mat.col(), mat.col());
		MM.mul(tmat, mat);
		MMinv.inv_svd(MM, lwork);
		ret.mul(tmat, MMinv);
		ret.m_info = MMinv.m_info;
	}
	else
	{
		ret.inv_svd(mat, lwork);
	}
	return ret;
}

fMat sr_inv_svd(const fMat& mat, fVec& w_err, fVec& w_norm, double k, int lwork)
{
	assert(w_err.row() == mat.row() && w_norm.row() == mat.col());
	fMat ret(mat.n_col, mat.n_row);
	static fMat Jhat, tJhat;
	Jhat.resize(mat.row(), mat.col());
	tJhat.resize(mat.col(), mat.row());
	int i, j;
	Jhat.set(mat);
	for(i=0; i<mat.n_row; i++)
	{
		if(w_err(i) < 1e-8) 
		{
			w_err(i) = 1.0;
		}
		for(j=0; j<mat.n_col; j++)
		{
			Jhat(i, j) *= w_err(i);
		}
	}
	for(j=0; j<mat.n_col; j++)
	{
		if(w_norm(j) < 1e-8) 
		{
			w_norm(j) = 1.0;
		}
		for(i=0; i<mat.n_row; i++)
		{
			Jhat(i, j) *= w_norm(j);
		}
	}
	tJhat.tran(Jhat);
	static fMat tmp, minv;
	if(mat.n_row < mat.n_col)
	{
		tmp.resize(mat.n_row, mat.n_row);
		minv.resize(mat.n_row, mat.n_row);
		tmp.mul(Jhat, tJhat);
		for(i=0; i<mat.n_row; i++)
		{
			tmp(i, i) += k;
		}
		minv.inv_svd(tmp, lwork);
		ret.mul(tJhat, minv);
		ret.m_info = minv.m_info;
	}
	else
	{
		tmp.resize(mat.n_col, mat.n_col);
		minv.resize(mat.n_col, mat.n_col);
		tmp.mul(tJhat, Jhat);
		for(i=0; i<mat.n_col; i++)
		{
			tmp(i, i) += k;
		}
		minv.inv_svd(tmp, lwork);
		ret.mul(minv, tJhat);
		ret.m_info = minv.m_info;
	}
	for(i=0; i<mat.n_col; i++)
	{
		for(j=0; j<mat.n_row; j++)
		{
			ret(i, j) *= w_err(j) / w_norm(i);
		}
	}
	return ret;
}

int fMat::inv_svd(const fMat& mat, int lwork)
{
	assert(n_row == mat.n_row && n_col == mat.n_col);
	if(n_col == 1)
	{
		p_data[0] = 1/mat(0, 0);
		m_info = 0;
		return m_info;
	}
	double *a, *b, *x, *s;
	int m = mat.row(), n = mat.col(), nrhs = mat.row(), rank;
	fMat I(mat.row(), mat.col());
	I.identity();
	a = mat.data();
	x = p_data;
	b = I.data();
	if(m<n) s = new double[m];
	else s = new double[n];
	m_info = dims_dgelss(a, x, b, m, n, nrhs, s, &rank, lwork);
	delete[] s;
	return m_info;
}

int fMat::p_inv_svd(const fMat& mat, int lwork)
{
	assert(n_row == mat.n_col && n_col == mat.n_row);
	fMat tmat;
	fMat MM, MMinv;

	if(mat.row() < mat.col())
	{
		tmat.resize(mat.n_col, mat.n_row);
		tmat.tran(mat);
		MM.resize(mat.row(), mat.row());
		MMinv.resize(mat.row(), mat.row());
//		MM.mul(mat, tmat);
		MM.mul_tran(mat, false);
		MMinv.inv_svd(MM, lwork);
		mul(tmat, MMinv);
		m_info = MMinv.m_info;
	}
	else if(mat.row() > mat.col())
	{
		tmat.resize(mat.n_col, mat.n_row);
		tmat.tran(mat);
		MM.resize(mat.col(), mat.col());
		MMinv.resize(mat.col(), mat.col());
//		MM.mul(tmat, mat);
		MM.mul_tran(mat, true);
		MMinv.inv_svd(MM, lwork);
		mul(MMinv, tmat);
		m_info = MMinv.m_info;
	}
	else inv_svd(mat, lwork);
	return m_info;
}

int fMat::sr_inv_svd(const fMat& mat, fVec& w_err, fVec& w_norm, double k, int lwork)
{
	assert(n_col == mat.n_row && n_row == mat.n_col &&
		   w_err.row() == mat.row() && w_norm.row() == mat.col());
	fMat Jhat, tJhat;
	Jhat.resize(mat.row(), mat.col());
	tJhat.resize(mat.col(), mat.row());
	int i, j;
	Jhat.set(mat);
	for(i=0; i<mat.n_row; i++)
	{
		if(w_err(i) < 1e-8) 
		{
			w_err(i) = 1.0;
		}
		for(j=0; j<mat.n_col; j++)
		{
			Jhat(i, j) *= w_err(i);
		}
	}
	for(j=0; j<mat.n_col; j++)
	{
		if(w_norm(j) < 1e-8) 
		{
			w_norm(j) = 1.0;
		}
		for(i=0; i<mat.n_row; i++)
		{
			Jhat(i, j) *= w_norm(j);
		}
	}
	tJhat.tran(Jhat);
	fMat tmp, minv;
	if(mat.n_row < mat.n_col)
	{
		tmp.resize(mat.n_row, mat.n_row);
		minv.resize(mat.n_row, mat.n_row);
//		tmp.mul_tran(Jhat, false);
		tmp.mul(Jhat, tJhat);
		for(i=0; i<mat.n_row; i++)
		{
			tmp(i, i) += k;
		}
		minv.inv_svd(tmp, lwork);
		mul(tJhat, minv);
		m_info = minv.m_info;
	}
	else
	{
		tmp.resize(mat.n_col, mat.n_col);
		minv.resize(mat.n_col, mat.n_col);
//		tmp.mul_tran(Jhat, true);
		tmp.mul(tJhat, Jhat);
		for(i=0; i<mat.n_col; i++)
		{
			tmp(i, i) += k;
		}
		minv.inv_svd(tmp, lwork);
		mul(minv, tJhat);
		m_info = minv.m_info;
	}
	for(i=0; i<mat.n_col; i++)
	{
		for(j=0; j<mat.n_row; j++)
		{
			(*this)(i, j) *= w_err(j) / w_norm(i);
		}
	}
	return m_info;
}

int fMat::svd(fMat& U, fVec& Sigma, fMat& VT)
{
	assert(n_row == U.n_row && n_row == U.n_col &&
		   Sigma.n_row == MIN(n_row, n_col) &&
		   n_col == VT.n_row && n_col == VT.n_col);
	double* a = p_data;
	int ret = dims_svd(a, n_row, n_col, U.data(), Sigma.data(), VT.data());
	return ret;
}

/*
 * POSV
 */
int fMat::lineq_posv(const fMat& A, const fMat& b)
{
	int m = A.row();
	int nrhs = b.col();
//	m_info = dims_dporfs(A.data(), p_data, b.data(), m, nrhs);
	m_info = dims_dposv(A.data(), p_data, b.data(), m, nrhs);
//	double rcond;
//	m_info = dims_dposvx(A.data(), p_data, b.data(), m, nrhs, &rcond);
	return m_info;
}

int fMat::inv_posv(const fMat& A)
{
	fMat I(A.row(), A.col());
	I.identity();
	m_info = lineq_posv(A, I);
	return m_info;
}

int fVec::lineq_posv(const fMat& A, const fVec& b)
{
	int m = A.row();
	m_info = dims_dposv(A.data(), p_data, b.data(), m, 1);
	return m_info;
}

int fMat::lineq_porfs(const fMat& A, const fMat& b)
{
	int m = A.row();
	int nrhs = b.col();
	m_info = dims_dporfs(A.data(), p_data, b.data(), m, nrhs);
	return m_info;
}

int fMat::inv_porfs(const fMat& A)
{
	fMat I(A.row(), A.col());
	I.identity();
	m_info = lineq_porfs(A, I);
	return m_info;
}

int fVec::lineq_porfs(const fMat& A, const fVec& b)
{
	int m = A.row();
	m_info = dims_dporfs(A.data(), p_data, b.data(), m, 1);
	return m_info;
}

/*
 * SR-inverse
 */
int fMat::lineq_sr(const fMat& A, fVec& w_err, fVec& w_norm, double k, const fMat& b){
	assert(n_col == b.col() && n_row == A.col() &&
		   w_err.row() == A.row() && w_norm.row() == A.col() &&
		   b.row() == A.row());
	fMat Jhat, tJhat, wb;
	int A_row = A.row(), A_col = A.col(), n_rhs = b.col();
	Jhat.resize(A_row, A_col);
	tJhat.resize(A_col, A_row);
	wb.resize(A_row, n_rhs);
	int i, j;
	Jhat.set(A);
	wb.set(b);
	for(i=0; i<A_row; i++)
    {
		for(j=0; j<A_col; j++)
		{
			Jhat(i, j) *= w_err(i);
			Jhat(i, j) /= w_norm(j);
		}
    }
	tJhat.tran(Jhat);
	for(i = 0; i < A_row; i++)
	{
		for(j = 0; j < n_rhs; j++)
		{
			wb(i, j) *= w_err(i);
		}
	}
	fMat tmp, tmpx, tmpb;
	if(A_row < A_col)
    {
		tmpx.resize(A_row, n_rhs);
		tmp.resize(A_row, A_row);
		tmp.mul_tran(Jhat, false);
		for(i=0; i<A_row; i++)
		{
			tmp(i, i) += k;
		}
 		tmpx.lineq_posv(tmp, wb);
		mul(tJhat, tmpx);
		m_info = 0; // ???
		for(i=0; i<A_col; i++)
		{
			for(j = 0; j < n_rhs; j++){
				(*this)(i, j) /= w_norm(i);
			}
		}
    }
	else
    {
		tmp.resize(A_col, A_col);
		tmpb.resize(A_col, n_rhs);
		tmp.mul_tran(Jhat, true);
		for(i=0; i<A_col; i++)
		{
			tmp(i, i) += k;
		}
		tmpb.mul(tJhat, wb);
		(*this).lineq_posv(tmp, tmpb);
		m_info = 0; // ???
		for(i = 0; i < A_col; i++)
		{
			for(j = 0; j < n_rhs; j++)
			{
				(*this)(i, j) /= w_norm(i);
			}
		}
    }
	return m_info;
}

int fVec::lineq_sr(const fMat& A, fVec& w_err, fVec& w_norm, double k, const fVec& b)
{
	assert(n_row == A.col() && w_err.row() == A.row() &&
		   w_norm.row() == A.col() && b.row() == A.row());
	fMat Jhat;
	fVec wb;
	int A_row = A.row(), A_col = A.col();
	Jhat.resize(A_row, A_col);
	wb.resize(A_row);
	int i, j;
	Jhat.set(A);
	wb.set(b);
	double* pp;
	for(i=0; i<A_row; i++)
    {
		for(j=0; j<A_col; j++)
		{
			pp = Jhat.data() + i + A_row*j;
			*pp *= w_err(i) / w_norm(j);
		}
		wb(i) *= w_err(i);
    }
	fMat tmp;
	fVec tmpx, tmpb;
	if(A_row < A_col)
    {
		tmpx.resize(A_row);
		tmp.resize(A_row, A_row);
		tmp.mul_tran(Jhat, false);
		for(i=0; i<A_row; i++)
		{
			tmp(i, i) += k;
		}
		tmpx.lineq_posv(tmp, wb);
		mul(tmpx, Jhat);
		m_info = 0; // ???
		for(i=0; i<A_col; i++)
		{
			(*this)(i) /= w_norm(i);
		}
    }
	else
    {
		tmp.resize(A_col, A_col);
		tmpb.resize(A_col);
		tmp.mul_tran(Jhat, true);
		for(i=0; i<A_col; i++)
		{
			tmp(i, i) += k;
		}
		tmpb.mul(wb, Jhat);
		lineq_posv(tmp, tmpb);
		m_info = 0; // ???
		for(i = 0; i < A_col; i++)
		{
			(*this)(i) /= w_norm(i);
		}
    }
	return m_info;
}

fMat lineq_sr(const fMat& A, fVec& w_err, fVec& w_norm, double k, const fMat& b)
{
	assert(w_err.row() == A.row() && w_norm.row() == A.col() &&
		   b.row() == A.row());
	fMat x(A.col(), b.col());
	fMat Jhat, tJhat, wb;
	Jhat.resize(A.row(), A.col());
	tJhat.resize(A.col(), A.row());
	wb.resize(b.row(), b.col());
	int i, j;
	Jhat.set(A);
	wb.set(b);
	for(i=0; i<A.row(); i++)
    {
		if(w_err(i) < 1e-8) 
		{
			w_err(i) = 1.0;
		}
		for(j=0; j<A.col(); j++)
		{
			Jhat(i, j) *= w_err(i);
		}
    }
	for(j=0; j<A.col(); j++)
    {
		if(w_norm(j) < 1e-8) 
		{
			w_norm(j) = 1.0;
		}
		for(i=0; i<A.row(); i++)
		{
			Jhat(i, j) /= w_norm(j);
		}
    }
	tJhat.tran(Jhat);
	for(i = 0; i < A.row(); i++){
		for(j = 0; j < b.col(); j++){
			wb(i, j) *= w_err(i);
		}
	}
	fMat tmp, tmpx, tmpb;
	if(A.row() < A.col())
    {
		tmpx.resize(A.row(), b.col());
		tmp.resize(A.row(), A.row());
		tmp.mul(Jhat, tJhat);
		for(i=0; i<A.row(); i++)
		{
			tmp(i, i) += k;
		}
		tmpx.lineq(tmp, wb);
		x.mul(tJhat, tmpx);
		for(i=0; i<A.col(); i++)
		{
			for(j = 0; j < b.col(); j++)
			{
				x(i, j) /= w_norm(i);
			}
		}
    }
	else
    {
		tmp.resize(A.col(), A.col());
		tmpx.resize(A.col(), b.col());
		tmpb.resize(A.col(), b.col());
		tmp.mul(tJhat, Jhat);
		for(i=0; i<A.col(); i++)
		{
			tmp(i, i) += k;
		}
		tmpb.mul(tJhat, wb);
		x.lineq(tmp, tmpb);
		for(i=0; i<A.col(); i++)
		{
			for(j = 0; j < b.col(); j++)
			{
				x(i, j) /= w_norm(i);
			}
		}
    }
	return x;
}

fMat tran(const fMat& mat)
{
	fMat ret(mat.n_col, mat.n_row);
	int i, j, c = mat.n_col, r = mat.n_row;
	for(i=0; i<c; i++)
	{
		for(j=0; j<r; j++)
		{
			ret(i,j) = mat.p_data[j+i*mat.n_row];
		}
	}
	return ret;
}

void fMat::tran(const fMat& mat)
{
	assert(n_col == mat.n_row && n_row == mat.n_col);
	int i, j, n, m, c = mat.n_col, r = mat.n_row;
	for(i=0, n=0; i<c; i++, n+=r)
	{
		for(j=0, m=0; j<r; j++, m+=c)
		{
			p_data[i+m] = mat.p_data[j+n];
		}
	}
}

void fMat::tran()
{
	assert(n_col == n_row);
	double tmp;
	int i, j, m, n, idx1, idx2;
	for(i=0, n=0; i<n_row; i++, n+=n_col)
	{
		for(j=i, m=i*n_row; j<n_col; j++, m+=n_row)
		{
			idx1 = i + m;
			idx2 = j + n;
			tmp = p_data[idx1];
			p_data[idx1] = p_data[idx2];
			p_data[idx2] = tmp;
		}
	}
}

void fMat::identity()
{
	assert(n_col == n_row);
	int i, j, m;
	for(i=0; i<n_row; i++)
	{
		for(j=0, m=i; j<n_col; j++, m+=n_row)
		{
			if(i==j)
				p_data[m] = 1.0;
			else
				p_data[m] = 0.0;
		}
	}
}

void fMat::set(const fMat& mat)
{
	assert(n_col == mat.n_col && n_row == mat.n_row);
	int n = n_row * n_col;
	dims_copy(mat.p_data, p_data, n);
}

void fMat::neg(const fMat& mat)
{
	assert(n_col == mat.n_col && n_row == mat.n_row);
	int i, n = n_row * n_col;
	for(i=0; i<n; i++) p_data[i] = - mat.p_data[i];
}

fMat operator - (const fMat& mat)
{
	fMat ret(mat.n_row, mat.n_col);
	int i, n = mat.n_row * mat.n_col;
	for(i=0; i<n; i++) ret.p_data[i] = - mat.p_data[i];
	return ret;
}

void fMat::operator += (const fMat& mat)
{
	assert(n_col == mat.n_col && n_row == mat.n_row);
	int i, n = n_col * n_row;
	for(i=0; i<n; i++) p_data[i] += mat.p_data[i];
}

void fMat::operator -= (const fMat& mat)
{
	assert(n_col == mat.n_col && n_row == mat.n_row);
	int i, n = n_col * n_row;
	for(i=0; i<n; i++) p_data[i] -= mat.p_data[i];
}

void fMat::operator *= (double d)
{
	int n = n_col * n_row;
	dims_scale_myself(p_data, d, n);
}

void fMat::mul(double d, const fMat& mat)
{
	assert(n_col == mat.n_col && n_row == mat.n_row);
	int n = n_col * n_row;
	dims_scale(mat.data(), d, n, data());
}

void fMat::mul(const fMat& mat, double d)
{
	assert(n_col == mat.n_col && n_row == mat.n_row);
	int n = n_col * n_row;
	dims_scale(mat.data(), d, n, data());
}

fMat operator * (double d, const fMat& mat)
{
	fMat ret(mat.row(), mat.col());
	int i, n = mat.col() * mat.row();
	for(i=0; i<n; i++) *(ret.data() + i) = d * *(mat.data() + i);
	return ret;
}

fMat operator * (const fMat& mat, double d)
{
	fMat ret(mat.row(), mat.col());
	int i, n = mat.col() * mat.row();
	for(i=0; i<n; i++) *(ret.data() + i) = d * *(mat.data() + i);
	return ret;
}

void fMat::operator /= (double d)
{
	int n = n_col * n_row;
	dims_scale_myself(p_data, 1.0/d, n);
}

fMat fMat::operator = (const fMat& mat)
{
	assert(n_row == mat.n_row && n_col == mat.n_col);
	int i, n = n_col * n_row;
	for(i=0; i<n; i++)
	{
		*(p_data + i) = *(mat.data() + i);
	}
	return *this;
}

void fMat::operator = (double d)
{
	int i, n = n_col * n_row;
	for(i=0; i<n; i++) p_data[i] = d;
}

void fMat::add(const fMat& mat1, const fMat& mat2)
{
	assert(n_col == mat1.n_col && n_row == mat1.n_row &&
		   mat1.n_col == mat2.n_col && mat1.n_row == mat2.n_row);
	int i, n = n_row * n_col;
	for(i=0; i<n; i++)
	{
		p_data[i] = mat1.p_data[i] + mat2.p_data[i];
	}
}

void fMat::add(const fMat& mat)
{
	assert(n_col == mat.n_col && n_row == mat.n_row);
	int i, n = n_row * n_col;
	for(i=0; i<n; i++) p_data[i] += mat.p_data[i];
}

fMat operator + (const fMat& mat1, const fMat& mat2)
{
	assert(mat1.n_col == mat2.n_col && mat1.n_row == mat2.n_row);
	fMat ret(mat1.n_row, mat1.n_col, 0.0);
	int i, n = mat1.n_row * mat1.n_col;
	for(i=0; i<n; i++) ret.p_data[i] = mat1.p_data[i] + mat2.p_data[i];
	return ret;
}

void fMat::sub(const fMat& mat1, const fMat& mat2)
{
	assert(n_col == mat1.n_col && n_row == mat1.n_row &&
		   mat1.n_col == mat2.n_col && mat1.n_row == mat2.n_row);
	int i, n = n_row * n_col;
	double* p1 = mat1.p_data, *p2 = mat2.p_data;
	for(i=0; i<n; i++)
	{
		p_data[i] = p1[i] - p2[i];
	}
}

fMat operator - (const fMat& mat1, const fMat& mat2)
{
	assert(mat1.n_col == mat2.n_col && mat1.n_row == mat2.n_row);
	fMat ret(mat1.n_row, mat1.n_col, 0.0);
	int i, n = mat1.n_row * mat1.n_col;
	for(i=0; i<n; i++) ret.p_data[i] = mat1.p_data[i] - mat2.p_data[i];
	return ret;
}

void fMat::mul(const fMat& mat1, const fMat& mat2)
{
	assert(n_row == mat1.n_row && n_col == mat2.n_col && mat1.n_col == mat2.n_row);
#if 0
	int i, j, k, n, c = mat1.col(), r = mat1.row();
	double* p1, *p2, *pp;
	for(i=0; i<n_row; i++)
	{
		for(j=0, n=0, pp=p_data+i; j<n_col; j++, n+=c, pp+=n_row)
		{
			double temp = 0.0;
			for(k=0, p1=mat1.p_data+i, p2=mat2.p_data+n; k<c; k++, p1+=r, p2++)
			{
				temp += *p1 * *p2;
			}
			*pp = temp;
		}
	}
#else
	int m = mat1.row(), k = mat1.col(), n = mat2.col();
	dims_dgemm(mat1.data(), mat2.data(), m, n, k, p_data);
#endif
}

void fMat::mul_tran(const fMat& mat1, int trans_first)
{
	assert(n_row == n_col);
	assert((trans_first && n_row == mat1.n_col) || (!trans_first && n_row ==mat1.n_row));
	if(trans_first)
	{
		int i, j, r = mat1.row();
		double* p1 = mat1.p_data, *p2, *pp;
		double* pp_first;
		for(j=0, p2=mat1.p_data, pp_first=p_data; j<n_col; j++, p2+=r, pp_first+=n_row)
		{
			pp = pp_first;
			for(i=0, p1=mat1.p_data; i<=j; i++, p1+=r, pp++)
			{
				double temp = dims_dot(p1, p2, r);
				*pp = temp;
				if(i!=j) p_data[j+n_row*i] = temp;
			}
		}
	}
	else
	{
		dims_dsyrk(mat1.p_data, mat1.n_row, mat1.n_col, p_data);
		symmetric();
	}
}

fMat operator * (const fMat& mat1, const fMat& mat2)
{
	assert(mat1.n_col == mat2.n_row);
	fMat ret(mat1.n_row, mat2.n_col, 0.0);
	int i, j, k, n;
	for(i=0; i<ret.row(); i++)
	{
		for(j=0; j<ret.col(); j++)
		{
			n = j * mat2.n_row;
			ret(i, j) = 0;
			for(k=0; k<mat1.col(); k++)
			{
				ret(i, j) += mat1.p_data[i+k*mat1.n_row] * mat2.p_data[k+n];
			}
		}
	}
	return ret;
}

int inv_enlarge(const fMat& m12, const fMat& m21, const fMat& m22, const fMat& P, fMat& X, fMat& y, fMat& z, fMat& w)
{
	int n_org = P.col();
	int n_add = m12.col();
	fMat Pm(n_org, n_add), mPm(n_add, n_add), mP(n_add, n_org);
	X.resize(n_org, n_org);
	y.resize(n_org, n_add);
	z.resize(n_add, n_org);
	w.resize(n_add, n_add);
	if(n_org == 0)
	{
		w.inv_svd(m22);
		return 0;
	}
	Pm.mul(P, m12);
	mPm.mul(m21, Pm);
	mPm *= -1.0;
	mPm += m22;
	w.inv_svd(mPm);
	y.mul(Pm, w);
	y *= -1.0;
	mP.mul(m21, P);
	z.mul(w, mP);
	z *= -1.0;
	X.mul(Pm, z);
	X *= -1.0;
	X += P;
	return 0;
}

int inv_shrink(const fMat& P, const fMat& q, const fMat& r, const fMat& s, fMat& X)
{
	int n_org = P.col();
	int n_add = q.col();
	if(n_org == 0)
	{
		return 0;
	}
	fMat sr(n_add, n_org), qsr(n_org, n_org);
	sr.lineq_svd(s, r);
	qsr.mul(q, sr);
	X.resize(n_org, n_org);
	X.sub(P, qsr);
	return 0;
}

int inv_row_replaced(const fMat& P, const fMat& q, const fMat& m2d, fMat& X, fMat& y)
{
	int n_org = P.col();
	int n_add = q.col();
	int n_total = P.row();
	fMat m2d_q(n_add, n_add), m2d_q_inv(n_add, n_add), m2d_P(n_add, n_org);
	X.resize(n_total, n_org);
	y.resize(n_total, n_add);
	if(n_org == 0)
	{
		y.inv_svd(m2d);
		return 0;
	}
	m2d_q.mul(m2d, q);
	m2d_q_inv.inv_svd(m2d_q);
	y.mul(q, m2d_q_inv);
	m2d_P.mul(m2d, P);
	X.mul(y, m2d_P);
	X *= -1.0;
	X += P;
	return 0;
}

int inv_col_replaced(const fMat& P, const fMat& q, const fMat& m2d, fMat& X, fMat& y)
{
	int n_org = P.row();
	int n_add = q.row();
	int n_total = P.col();
	fMat q_m2d(n_add, n_add), q_m2d_inv(n_add, n_add), P_m2d(n_org, n_add);
	X.resize(n_org, n_total);
	y.resize(n_add, n_total);
	if(n_org == 0)
	{
		y.inv_svd(m2d);
		return 0;
	}
	q_m2d.mul(q, m2d);
	q_m2d_inv.inv_svd(q_m2d);
	y.mul(q_m2d_inv, q);
	P_m2d.mul(P, m2d);
	X.mul(P_m2d, y);
	X *= -1.0;
	X += P;
	return 0;
}
	
void fVec::mul(const fMat& mat, const fVec& vec)
{
	assert(n_row == mat.row() && mat.col() == vec.row());
#if 0
	int i, k, c = mat.col(), r = mat.row();
	double* pm, *pv, *pp;
	for(i=0, pp=p_data; i<n_row; i++, pp++)
	{
		double temp = 0.0;
		for(k=0, pm=mat.data()+i, pv=vec.data(); k<c; k++, pm+=r, pv++)
		{
			temp += *pm * *pv;
		}
		*pp = temp;
	}
#else
	int c = mat.col(), r = mat.row();
	dims_dgemv(mat.data(), r, c, vec.data(), p_data);
#endif
}

void fVec::mul(const fVec& vec, const fMat& mat)
{
	assert(n_row == mat.col() && mat.row() == vec.row());
	int i, r = mat.row();
	double* pm, *pv = vec.p_data, *pp;
	for(i=0, pm=mat.data(), pp=p_data; i<n_row; i++, pm+=r, pp++)
	{
		*pp = dims_dot(pv, pm, r);
	}
}

fVec operator * (const fMat& mat, const fVec& vec)
{
	assert(mat.col() == vec.row());
	fVec ret(mat.row(), 0.0);
	int i, k;
	for(i=0; i<ret.row(); i++)
	{
		ret(i) = 0;
		for(k=0; k<mat.col(); k++)
		{
			ret(i) += mat.data()[i+k*mat.row()]*vec.data()[k];
		}
	}
	return ret;
}

void fMat::div(const fMat& mat, double d)
{
	assert(n_col == mat.n_col && n_row == mat.n_row);
	int n = n_col * n_row;
	dims_scale(mat.p_data, 1.0/d, n, p_data);
}

fMat operator / (const fMat& mat, double d)
{
	fMat ret(mat.n_row, mat.n_col);
	int i, n = mat.col() * mat.row();
	for(i=0; i<n; i++) ret.p_data[i] = mat.p_data[i] / d;
	return ret;
}

/**
 ** fVec
 **/
int fVec::max_index()
{
	int ret = -1;
	double max_val = 0.0;
	int i;
	for(i=0; i<n_row; i++)
	{
		if(ret < 0 || p_data[i] > max_val)
		{
			ret = i;
			max_val = p_data[i];
		}
	}
	return ret;
}

int fVec::min_index()
{
	int ret = -1;
	double min_val = 0.0;
	int i;
	for(i=0; i<n_row; i++)
	{
		if(ret < 0 || p_data[i] < min_val)
		{
			ret = i;
			min_val = p_data[i];
		}
	}
	return ret;
}

double fVec::max_value()
{
	int index = -1;
	double max_val = 0.0;
	int i;
	for(i=0; i<n_row; i++)
	{
		if(index < 0 || p_data[i] > max_val)
		{
			index = i;
			max_val = p_data[i];
		}
	}
	return max_val;
}

double fVec::min_value()
{
	int index = -1;
	double min_val = 0.0;
	int i;
	for(i=0; i<n_row; i++)
	{
		if(index < 0 || p_data[i] < min_val)
		{
			index = i;
			min_val = p_data[i];
		}
	}
	return min_val;
}

void fVec::abs()
{
	for(int i=0; i<n_row; i++)
	{
		p_data[i] = fabs(p_data[i]);
	}
}

void fVec::set(const fVec& vec)
{
	assert(n_row == vec.n_row);
	dims_copy(vec.p_data, p_data, n_row);
}

void fVec::neg(const fVec& vec)
{
	assert(n_row == vec.n_row);
	int i;
	for(i=0; i<vec.n_row; i++) p_data[i] = - vec.p_data[i];
}

fVec operator - (const fVec& vec)
{
	fVec ret(vec.n_row);
	int i;
	for(i=0; i<vec.n_row; i++) ret.p_data[i] = - vec.p_data[i];
	return ret;
}

void fVec::operator += (const fVec& vec)
{
	assert(n_row == vec.n_row);
	int i;
	for(i=0; i<n_row; i++) p_data[i] += vec.p_data[i];
}

void fVec::operator -= (const fVec& vec)
{
	assert(n_row == vec.n_row);
	dims_daxpy(n_row, -1.0, vec.p_data, p_data);
}

void fVec::operator *= (double d)
{
	dims_scale_myself(p_data, d, n_row);
}

void fVec::operator /= (double d)
{
	dims_scale_myself(p_data, 1.0/d, n_row);
}

void fVec::add(const fVec& vec1, const fVec& vec2)
{
	assert(n_row == vec1.n_row && vec1.n_row == vec2.n_row);
	for(int i=0; i<n_row; i++) p_data[i] = vec1.p_data[i] + vec2.p_data[i];
}

void fVec::add(const fVec& vec)
{
	assert(n_row == vec.n_row);
	for(int i=0; i<n_row; i++) p_data[i] += vec.p_data[i];
}

fVec operator + (const fVec& vec1, const fVec& vec2)
{
	assert(vec1.n_row == vec2.n_row);
	fVec ret(vec1.row(), 0.0);
	for(int i=0; i<vec1.n_row; i++)
		ret.p_data[i] = vec1.p_data[i] + vec2.p_data[i];
	return ret;
}

void fVec::sub(const fVec& vec1, const fVec& vec2)
{
	assert(n_row == vec1.n_row && vec1.n_row == vec2.n_row);
	for(int i=0; i<n_row; i++)
		p_data[i] = vec1.p_data[i] - vec2.p_data[i];
}

fVec operator - (const fVec& vec1, const fVec& vec2)
{
	assert(vec1.n_row == vec2.n_row);
	fVec ret(vec1.row(), 0.0);
	for(int i=0; i<vec1.n_row; i++)
		ret.p_data[i] = vec1.p_data[i] - vec2.p_data[i];
	return ret;
}

void fVec::div(const fVec& vec, double d)
{
	assert(n_row == vec.n_row);
	dims_scale(vec.p_data, 1.0/d, n_row, p_data);
}

fVec operator / (const fVec& vec, double d)
{
	fVec ret(vec.n_row);
	dims_scale(vec.p_data, 1.0/d, vec.n_row, ret.p_data);
	return ret;
}

double operator * (const fVec& vec1, const fVec& vec2)
{
	assert(vec1.n_row == vec2.n_row);
	double ret = 0;
	ret = dims_dot(vec1.p_data, vec2.p_data, vec1.n_row);
	return ret;
}

void fVec::mul(const fVec& vec, double d)
{
	assert(n_row == vec.n_row);
	dims_scale(vec.p_data, d, n_row, p_data);
}

void fVec::mul(double d, const fVec& vec)
{
	assert(n_row == vec.n_row);
	dims_scale(vec.p_data, d, n_row, p_data);
}

fVec operator * (double d, const fVec& vec)
{
	fVec ret(vec.n_row);
	dims_scale(vec.p_data, d, vec.n_row, ret.p_data);
	return ret;
}

fVec operator * (const fVec& vec, double d)
{
	fVec ret(vec.row());
	dims_scale(vec.p_data, d, vec.n_row, ret.p_data);
	return ret;
}

void fVec::operator = (double d)
{
	for(int i=0; i<n_row; i++) p_data[i] = d;
}

fVec fVec::operator = (const fVec& vec)
{
	assert(n_row == vec.n_row);
	dims_copy(vec.p_data, p_data, n_row);
	return *this;
}

/**********added by takano*******/
fMat eigs(const fMat& mat,double* w)
{
	int i,j,k,dim;
	double *a;
	fMat   eigmat;
	a= new double [mat.col()*mat.row()];
//	w=(double*)malloc(sizeof(double)*mat.col());
 
	k=0;
	for(i=0;i<mat.row();i++){
		for(j=0;j<mat.col();j++){
			a[k]=mat(i,j);
			k=k+1;
		}
	}
	dim=mat.row();  
	i=dims_eigs(dim,a,w);
	eigmat.resize(dim,dim);
	k=0;
	for(j=0;j<dim;j++){
		for(i=0;i<dim;i++){
			eigmat(i,j)=a[k];
			k=k+1;
		}
	}
	delete [] a;
	return eigmat; 
}

void fMat::dmul(const fMat& mat1, const fMat& mat2)
{
#ifndef NDEBUG
//	if(n_row != mat1.n_row || n_col != mat2.n_col ||
//	   mat1.n_col != mat2.n_row)
//	{
//		cerr << "fMat::mul(fMat, fMat): matrix size error" << endl;
//		return;
//	}
#endif
	int i, j, m;
	if(mat1.n_row == mat2.n_row && mat1.n_col == mat2.n_col)
	{
		for(i=0; i<n_row; i++)
		{
			for(j=0, m=i; j<n_col; j++, m+=n_row)
			{
					p_data[m] = mat1.p_data[m] * mat2.p_data[m];
			}
		}
	}
	else if(mat1.n_row == mat2.n_row && mat2.n_col == 1)
	{
		for(i=0; i<n_row; i++)
		{
			for(j=0, m=i; j<n_col; j++, m+=n_row)
			{
					p_data[m] = mat1.p_data[m] * mat2.p_data[i];
			}
		}
		
	}
	else if(mat1.n_col == mat2.n_col && mat2.n_row == 1)
	{
		for(i=0; i<n_row; i++)
		{
			for(j=0, m=i; j<n_col; j++, m+=n_row)
			{
					p_data[m] = mat1.p_data[m] * mat2.p_data[j];
			}
		}
	}
	else
	{
		cerr << "fMat::dmul(fMat, fMat): matrix size error" << endl;
	}
}


void fMat::ddiv(const fMat& mat1, const fMat& mat2)
{
#ifndef NDEBUG
//	if(n_row != mat1.n_row || n_col != mat2.n_col ||
//	   mat1.n_col != mat2.n_row)
//	{
//		cerr << "fMat::mul(fMat, fMat): matrix size error" << endl;
//		return;
//	}
#endif
	int i, j, m;
	if(mat1.n_row == mat2.n_row && mat1.n_col == mat2.n_col)
	{
		for(i=0; i<n_row; i++)
		{
			for(j=0, m=i; j<n_col; j++, m+=n_row)
			{
					p_data[m] = mat1.p_data[m] / mat2.p_data[m];
			}
		}
	}
	else if(mat1.n_row == mat2.n_row && mat2.n_col == 1)
	{
		for(i=0; i<n_row; i++)
		{
			for(j=0, m=i; j<n_col; j++, m+=n_row)
			{
					p_data[m] = mat1.p_data[m] / mat2.p_data[i];
			}
		}
		
	}
	else if(mat1.n_col == mat2.n_col && mat2.n_row == 1)
	{
		for(i=0; i<n_row; i++)
		{
			for(j=0, m=i; j<n_col; j++, m+=n_row)
			{
					p_data[m] = mat1.p_data[m] / mat2.p_data[j];
			}
		}
	}
	else
	{
		cerr << "fMat::dmul(fMat, fMat): matrix size error" << endl;
	}
}

double fMat::det(void)
{
#ifndef NDEBUG
  if((n_row !=n_col) || (n_row < 1))
    {
      cerr << "matrix size error at function det()" << endl;
      return 0;
    }
#endif
  double x;
  m_info = dims_det(n_row, p_data, &x);
  
  return x;
}

int fMat::eig(fVec& wr, fVec& wi)
{
#ifndef NDEBUG
  if((n_row !=n_col) || (n_row < 1) || 
     (n_row !=wr.row()) || (n_row !=wi.row()))
    {
      cerr << "matrix size error at function eig()" << endl;
      return -1;
    }
#endif

  m_info = dims_dgeev_simple(n_row, 
			     p_data, 
			     wr.data(), 
			     wi.data());
  
  return m_info;
}

int fMat::eig(fVec& wr, fVec& wi, fMat& v)
{
#ifndef NDEBUG
  if((n_row !=n_col) || (n_row < 1) || 
     (n_row !=wr.row()) || (n_row !=wi.row()) ||
     (n_row !=v.row()) || (n_row !=v.col()))
    {
      cerr << "matrix size error at function eig()" << endl;
      return -1;
    }
#endif

  m_info = dims_dgeev(n_row, 
		      p_data, 
		      wr.data(), 
		      wi.data(),
		      v.data());
  
  return m_info;
}

int fMat::eig(fVec& wr, fVec& wi, fMat& vr, fMat& vi)
{
#ifndef NDEBUG
  if((n_row !=n_col) || (n_row < 1) || 
     (n_row !=wr.row()) || (n_row !=wi.row()) ||
     (n_row !=vr.row()) || (n_row !=vr.col()) ||
     (n_row !=vi.row()) || (n_row !=vi.col()) ) 
    {
      cerr << "matrix size error at function eig()" << endl;
      return -1;
    }
#endif
  int i, j;
  double *p_wi, *p_vr, *p_vi;

  p_wi = wi.data();
  p_vr = vr.data();
  p_vi = vi.data();
  
  m_info = dims_dgeev(n_row, 
		      p_data, 
		      wr.data(), 
		      p_wi,
		      p_vr);
  
  for(i=0;i<n_row;i++, p_wi++, p_vr+=n_row, p_vi+=n_row)
    if(*p_wi == 0)
      for(j=0;j<n_row;j++)
	*(p_vi+j) = 0.0;
    else
      {
	p_vr+=n_row;
	for(j=0;j<n_row;j++)
	  *(p_vi+j) = *(p_vr+j);
	
	p_vi+=n_row;
	for(j=0;j<n_row;j++)
	  *(p_vi+j) = -*(p_vr+j);
	
	for(j=0;j<n_row;j++)
	  *(p_vr+j) = *(p_vr+j-n_row);
	
	i++, p_wi++;
      }
  
  return m_info;
}
