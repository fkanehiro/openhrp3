/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*!
 * @file   dims_clapack.c
 * @author Katsu Yamane
 * @date   06/16/2003
 * @brief  Wrapper implementations for CLAPACK functions.
 */

#include <dims_common.h>
#include <f2c.h>
#include <malloc.h>
#include <stdio.h>

#ifdef USE_CLAPACK_INTERFACE
#include <cblas.h>
#endif

/* CLAPACK native functions */
extern "C" 
{
int dgesvx_(
		char *fact, char *trans, integer *n,
		integer *nrhs, doublereal *a, integer *lda,
		doublereal *af, integer *ldaf, integer *ipiv,
		char *equed, doublereal *r, doublereal *c,
		doublereal *b, integer *ldb, doublereal *x,
		integer *ldx, doublereal *rcond, doublereal *ferr,
		doublereal *berr, doublereal *work, integer *iwork,
		integer *info);

int dgesvd_(
		char* jobu, char* jobvt, integer* m, integer* n,
		doublereal *a, integer *lda, doublereal *s,
		doublereal *u, integer *ldu, doublereal *vt,
		integer *ldvt, doublereal* work, integer *lwork,
		integer *info);

int dgelss_(
		integer* m, integer* n, integer* nrhs,
		doublereal* A, integer* lda,
		doublereal* B, integer* ldb,
		doublereal* S, doublereal* rcond, integer* rank,
		doublereal* work, integer* lwork, integer* info);

int dporfs_(
		char* uplo, integer* n, integer* nrhs,
		doublereal* A, integer* lda,
		doublereal* AF, integer* ldaf,
		doublereal* b, integer* ldb,
		doublereal* x, integer* ldx,
		doublereal* ferr, doublereal* berr,
		doublereal* work, integer* iwork,
		integer* info);

int dpotrf_(char* uplo, integer* n, doublereal* A, integer* lda,
			integer* info);

int dpotrs_(char* uplo, integer* n, integer* nrhs,
			doublereal* A, integer* lda,
			doublereal* x, integer* ldb,
			integer* info);

int dposv_(
		char* uplo, integer* n, integer* nrhs,
		doublereal* A, integer* lda,
		doublereal* b, integer* ldb,
		integer* info);

int dposvx_(
		char* fact, char* uplo, integer* n, integer* nrhs,
		doublereal* A, integer* lda, doublereal* AF, integer* ldaf,
		char* equed, doublereal* S,
		doublereal* B, integer* ldb,
		doublereal* X, integer* ldx,
		doublereal* rcond, doublereal* ferr, doublereal* berr,
		doublereal* work, integer* iwork, integer* info);

int dgesvd_(
		char* jobu, char* jobvt, integer* m, integer* n,
		doublereal* A, integer* lda,
		doublereal* S, doublereal* U, integer* ldu,
		doublereal* VT, integer* ldvt,
		doublereal* work, integer* lwork, integer* info);

int dsyev_(char* jobz, char* uplo, integer* m, doublereal* a, integer* n, doublereal* w, doublereal* work, integer* lwork, integer* info);

int dgeev_(char *jobvl, char *jobvr, integer *n, doublereal *a, 
	   integer *lda, doublereal *wr, doublereal *wi, doublereal *vl, 
	   integer *ldvl, doublereal *vr, integer *ldvr, doublereal *work, 
	   integer *lwork, integer *info);

int dgetrf_(integer *m, integer *n, doublereal *a, 
	    integer *lda, integer *ipiv, integer *info);

#ifndef USE_CLAPACK_INTERFACE
int f2c_dscal(
		integer* n, doublereal* alpha, doublereal* x, integer* incx);

int f2c_dcopy(
		integer* n, doublereal* x, integer* incx, doublereal* y, integer* incy);
double f2c_ddot(
		integer* n, doublereal* x, integer* incx, doublereal* y, integer* incy);
int f2c_dgemv(
		char* trans, integer* m, integer* n,
		doublereal* alpha, doublereal* A, integer* lda,
		doublereal* x, integer* incx,
		doublereal* beta, doublereal* y, integer* incy);

int f2c_dgemm(
		char *transa, char *transb, integer *m, integer *n, integer *k,
		doublereal *alpha, doublereal *a, integer *lda, 
		doublereal *b, integer *ldb,
		doublereal *beta, doublereal *c, integer *ldc);

int f2c_dsyrk(
		char* uplo, char* trans, integer* n, integer* k,
		doublereal* alpha, doublereal* A, integer* lda,
		doublereal* beta, doublereal* C, integer* ldc);
		
int f2c_daxpy(
		integer* n, doublereal* alpha, doublereal* x, integer* incx,
		doublereal* y, integer* incy);
#endif
}

double dims_dot(double* _x, double* _y, int _n)
{
#ifndef USE_CLAPACK_INTERFACE
	integer incx = 1, incy = 1;
	integer n = _n;
	return f2c_ddot(&n, _x, &incx, _y, &incy);
#else
	const int n=_n, incX=1, incY=1;
	return cblas_ddot(n, _x, incX, _y, incY);
#endif

}

int dims_copy(double* _x, double* _y, int _n)
{
#ifndef USE_CLAPACK_INTERFACE
	integer incx = 1, incy = 1;
	integer n = _n;
	f2c_dcopy(&n, _x, &incx, _y, &incy);
#else
	const int n = _n, incX = 1, incY = 1;
	cblas_dcopy(n, _x, incX, _y, incY);
#endif
	return 0;
}

int dims_scale_myself(double* _x, double _alpha, int _n)
{
#ifndef USE_CLAPACK_INTERFACE
	integer incx = 1;
	integer n = _n;
	f2c_dscal(&n, &_alpha, _x, &incx);
#else
	const int n = _n, incX = 1;
	const double alpha = _alpha;
	cblas_dscal(n, alpha, _x, incX);
#endif
	return 0;
}

int dims_scale(double* _x, double _alpha, int _n, double*_y)
{
#ifndef USE_CLAPACK_INTERFACE
	integer incx = 1, incy = 1;
	integer n = _n;
	f2c_dcopy(&n, _x, &incx, _y, &incy);
	f2c_dscal(&n, &_alpha, _y, &incx);
#else
	const int incX = 1, incY = 1;
	cblas_dcopy(_n, _x, incX, _y, incY);
	cblas_dscal(_n, _alpha, _x, incX);
#endif
	return 0;
}

int dims_dgemv(double* _A, int _m, int _n, double* _x, double* _y)
{
#ifndef USE_CLAPACK_INTERFACE
	char trans = 'N';
	integer m = _m;
	integer n = _n;
	integer lda = _m;
	doublereal alpha = 1.0;
	integer incx = 1;
	doublereal beta = 0.0;
	integer incy = 1;
	f2c_dgemv(&trans, &m, &n, &alpha, _A, &lda, _x, &incx, &beta, _y, &incy);
#else
	cblas_dgemv(CblasColMajor, CblasNoTrans,
		    _m, _n, 1.0, _A,_m, _x, 1, 0.0, _y, 1);
#endif
	return 0;
}

int dims_dgemv_tran(double* _A, int _m, int _n, double* _x, double* _y)
{
#ifndef USE_CLAPACK_INTERFACE
	char trans = 'T';
	integer m = _m;
	integer n = _n;
	integer lda = _m;
	doublereal alpha = 1.0;
	integer incx = 1;
	doublereal beta = 0.0;
	integer incy = 1;
	f2c_dgemv(&trans, &m, &n, &alpha, _A, &lda, _x, &incx, &beta, _y, &incy);
#else
	cblas_dgemv(CblasColMajor, CblasTrans,
		    _m, _n, 1.0, _A, _m, _x, 1, 0.0, _y, 1);
#endif
	return 0;
}

int dims_dgemm(double* _A, double* _B, int _m, int _n, int _k, double* _C)
{
#ifndef USE_CLAPACK_INTERFACE
	char transa = 'N';
	char transb = 'N';
	integer m = _m;
	integer n = _n;
	integer k = _k;
	doublereal alpha = 1.0;
	integer lda = _m;
	integer ldb = _k;
	doublereal beta = 0.0;
	integer ldc = _m;
	f2c_dgemm(&transa, &transb, &m, &n, &k, &alpha, _A, &lda, _B, &ldb, &beta, _C, &ldc);
#else
	cblas_dgemm(CblasColMajor, CblasNoTrans, CblasNoTrans,
		    _m, _n, _k, 1.0,
		    _A, _m, // lda
		    _B, _k, // ldb
		    0.0, 
		    _C, _m  // ldc
		    );
#endif
	return 0;
}

int dims_dsyrk(double* _A, int _n, int _k, double* _C)
{
#ifndef USE_CLAPACK_INTERFACE
	char uplo = 'U';
	char trans = 'N';
	integer n = _n;
	integer k = _k;
	doublereal alpha = 1.0;
	integer lda = _n;
	doublereal beta = 0.0;
	integer ldc = _n;

	f2c_dsyrk(&uplo, &trans, &n, &k, &alpha, _A, &lda, &beta, _C, &ldc);
#else
	cblas_dsyrk(CblasColMajor, CblasUpper, CblasNoTrans,
		    _n, _k, 1.0, _A, _n, 0.0, _C, _n);
#endif
	return 0;
}

int dims_dsyrk_trans_first(double* _A, int _n, int _k, double* _C)
{
#ifndef USE_CLAPACK_INTERFACE
	char uplo = 'U';
	char trans = 'T';
	integer n = _n;
	integer k = _k;
	doublereal alpha = 1.0;
	integer lda = _k;
	doublereal beta = 0.0;
	integer ldc = _n;
	f2c_dsyrk(&uplo, &trans, &n, &k, &alpha, _A, &lda, &beta, _C, &ldc);
#else
	cblas_dsyrk(CblasColMajor, CblasUpper, CblasTrans,
		    _n, _k, 1.0, _A, _n, 0.0, _C, _n);
#endif
	return 0;
}

int dims_daxpy(int _n, double _alpha, double* _x, double* _y)
{
#ifndef USE_CLAPACK_INTERFACE
	integer n = _n;
	doublereal alpha = _alpha;
	integer incx = 1;
	integer incy = 1;
	f2c_daxpy(&n, &alpha, _x, &incx, _y, &incy);
#else
 	cblas_daxpy(_n, _alpha, _x, 1, _y, 1);
#endif
	return 0;
}

int dims_dporfs(double* _a, double* _x, double* _b, int _m, int _nrhs)
{
	char uplo = 'U';
	integer n = _m;
	integer nrhs = _nrhs;
	integer lda = _m;
	integer ldaf = _m;
	integer ldb = _m;
	integer ldx = _m;
	integer info;
	integer a_ndata = n*n;
	integer b_ndata = n*nrhs;
	integer i;
	doublereal* a = (doublereal *)malloc(sizeof(doublereal)*a_ndata);
	doublereal* u = (doublereal *)malloc(sizeof(doublereal)*a_ndata);
	doublereal* b = (doublereal *)malloc(sizeof(doublereal)*b_ndata);
	doublereal* x = (doublereal *)malloc(sizeof(doublereal)*b_ndata);
	doublereal* ferr = (doublereal *)malloc(sizeof(doublereal)*nrhs);
	doublereal* berr = (doublereal *)malloc(sizeof(doublereal)*nrhs);
	doublereal* work = (doublereal *)malloc(sizeof(doublereal)*3*n);
	integer* iwork = (integer*)malloc(sizeof(integer)*n);
	/* copy data in _a because they are overwritten by dposv() */
	for(i=0; i<a_ndata; i++)
	{
		a[i] = _a[i];
		u[i] = _a[i];
	}
	for(i=0; i<b_ndata; i++)
	{
		b[i] = _b[i];
		x[i] = _b[i];
	}
	dpotrf_(&uplo, &n, u, &lda, &info);
	dpotrs_(&uplo, &n, &nrhs, u, &lda, x, &ldb, &info);
#if 0
	printf("info=%d\n", info);
	printf("old x = ");
	for(i=0; i<b_ndata; i++)
	{
		printf("\t%.8g", x[i]);
	}
	printf("\n");
#endif
/*	dposv_(&uplo, &n, &nrhs, a, &lda, x, &ldb, &info);*/
	dporfs_(&uplo, &n, &nrhs, a, &lda, u, &ldaf, b, &ldb, x, &ldx, ferr, berr, work, iwork, &info);
	for(i=0; i<b_ndata; i++)
	{
		_x[i] = x[i];
	}
#if 0
	printf("info=%d\n", info);
	printf("new x = ");
	for(i=0; i<b_ndata; i++)
	{
		printf("\t%.8g", x[i]);
	}
	printf("\n");
#endif
	free(a);
	free(u);
	free(b);
	free(x);
	free(ferr);
	free(berr);
	free(work);
	free(iwork);
	return info;
}

int dims_dposvx(double* _a, double* _x, double* _b, int _m, int _nrhs, double* _rcond)
{
	char fact = 'E';
	char uplo = 'U';
	integer n = _m;
	integer nrhs = _nrhs;
	integer lda = _m;
	integer ldaf = _m;
	char equed = 'N';
	integer ldb = _m;
	integer ldx = _m;
	doublereal rcond;
	integer info;
	integer a_ndata = n*n;
	integer b_ndata = n*nrhs;
	integer i;
	doublereal* a = (doublereal *)malloc(sizeof(doublereal)*a_ndata);
	doublereal* af = (doublereal *)malloc(sizeof(doublereal)*a_ndata);
	doublereal* s = (doublereal *)malloc(sizeof(doublereal)*n);
	doublereal* b = (doublereal *)malloc(sizeof(doublereal)*b_ndata);
	doublereal* x = (doublereal *)malloc(sizeof(doublereal)*b_ndata);
	doublereal* ferr = (doublereal *)malloc(sizeof(doublereal)*nrhs);
	doublereal* berr = (doublereal *)malloc(sizeof(doublereal)*nrhs);
	doublereal* work = (doublereal *)malloc(sizeof(doublereal)*3*n);
	integer* iwork = (integer*)malloc(sizeof(integer)*n);
	/* copy data in _a because they are overwritten by dposv() */
	for(i=0; i<a_ndata; i++)
	{
		a[i] = _a[i];
	}
	for(i=0; i<b_ndata; i++)
	{
		b[i] = _b[i];
	}
	dposvx_(&fact, &uplo, &n, &nrhs, a, &lda, af, &ldaf, &equed, s, b, &ldb, x, &ldx, &rcond, ferr, berr, work, iwork, &info);
	for(i=0; i<b_ndata; i++)
	{
		_x[i] = x[i];
	}
#if 0
	printf("scale=[");
	for(i=0; i<n; i++)
	{
		printf("\t%.8g", s[i]);
	}
	printf("]\n");
#endif
	*_rcond = rcond;
	free(a);
	free(af);
	free(s);
	free(b);
	free(x);
	free(ferr);
	free(berr);
	free(work);
	free(iwork);
	return info;
}

int dims_dposv(double* _a, double* _x, double* _b, int _m, int _nrhs)
{
	char uplo = 'U';
	integer n = _m;
	integer nrhs = _nrhs;
	integer lda = _m;
	integer ldb = _m;
	integer info;
	integer a_ndata = n*n;
	integer b_ndata = n*nrhs;
	integer i;
	doublereal* a = (doublereal *)malloc(sizeof(doublereal)*a_ndata);
	doublereal* b = (doublereal *)malloc(sizeof(doublereal)*b_ndata);
	/* copy data in _a because they are overwritten by dposv() */
	for(i=0; i<a_ndata; i++)
	{
		a[i] = _a[i];
	}
	for(i=0; i<b_ndata; i++)
	{
		b[i] = _b[i];
	}
	dposv_(&uplo, &n, &nrhs, a, &lda, b, &ldb, &info);
	for(i=0; i<b_ndata; i++)
	{
		_x[i] = b[i];
	}
	free(a);
	free(b);
	return info;
}

int dims_dgesvx(double* _a, double* _x, double* _b, int _n, int _nrhs)
{
	char fact = 'N';
	char trans = 'N';
	integer n = (integer)_n;
	integer nrhs = (integer)_nrhs;
	doublereal* a = (doublereal*)_a;
	integer lda = n;
	doublereal* af = (doublereal*)malloc(sizeof(doublereal)*n*n);
	integer ldaf = n;
	integer *ipiv=(integer *)malloc(sizeof(integer)*n);
	char equed = 'N';
	doublereal *r=(doublereal *)malloc(sizeof(doublereal)*n);
	doublereal *c=(doublereal *)malloc(sizeof(doublereal)*n);
	doublereal* b = (doublereal*)_b;
	integer ldb = n;
	doublereal *x = (doublereal *)_x;
	integer ldx = n;

	doublereal rcond;
	doublereal *ferr = (doublereal *)malloc(sizeof(doublereal)*nrhs);
	doublereal *berr = (doublereal *)malloc(sizeof(doublereal)*nrhs);
	doublereal *work = (doublereal *)malloc(sizeof(doublereal)*4*n);
	integer *iwork = (integer *)malloc(sizeof(integer)*n);

	integer info;
	dgesvx_(&fact, &trans, &n, &nrhs, a, &lda, af, &ldaf, ipiv,
			&equed, r, c, b, &ldb, x, &ldx, &rcond, ferr, berr,
			work, iwork, &info);
	free(ipiv);
	free(r);
	free(c);
	free(iwork);
	free(work);
	free(berr);
	free(ferr);
	free(af);
	return info;
}

/* inputs: A(m x n), b(m x nrhs), x(n x nrhs) */
int dims_dgelss(double* _a, double* _x, double* _b, int _m, int _n, int _nrhs,
		   double* _s, int* _rank, int _lwork)
{
	int i, mm, tmp1, tmp2;
	integer m = (integer)_m;  /* row of A */
	integer n = (integer)_n;  /* col of A */
	integer nrhs = (integer)_nrhs;  /* row of b and x */
	integer lda, ldb, rank, lwork, info;
	doublereal *a, *b, *s, rcond, *work;
	mm = _m*_n;
	a = (doublereal*)malloc(sizeof(doublereal)*mm);  /* A is overwritten by its singular vectors */
	for(i=0; i<mm; i++) a[i] = _a[i];
	lda = MAX(1, _m);   /* row of A */
	ldb = MAX3(1, _m, _n);    /* row of B */
	mm = ldb*_nrhs;
	b = (doublereal*)malloc(sizeof(doublereal)*mm);  /* note that b is overwritten by lapack function */
	for(i=0; i<mm; i++) b[i] = _b[i];  /* copy b */
	s = (doublereal*)_s;
	rcond = -1;  /* rcond < 0 -> use machine precision */
	if(_lwork > 0) lwork = _lwork;
	else
	{
		lwork = MIN(_m, _n);  /* ? size of work */
		lwork *= 3;
		tmp1 = MIN(_m, _n);
		tmp2 = MAX(_m, _n);
		tmp1 *= 2;
		lwork += MAX3(tmp1, tmp2, _nrhs);
		lwork *= 5;
	}
	work = (doublereal*)malloc(sizeof(doublereal)*lwork);
	dgelss_(&m, &n, &nrhs, a, &lda, b, &ldb, s, &rcond, &rank,
			work, &lwork, &info);

	mm = _n*_nrhs;
	for(i=0; i<mm; i++) _x[i] = b[i];
	*_rank = (int)rank;

	free(a);
	free(b);
	free(work);
	return info;
}
 
int dims_svd(double* _a, int _m, int _n, double* _u, double* _sigma, double* _vt)
{
	char jobu = 'A';  /* compute all columns of U */
	char jobvt = 'A';  /* compute all rows of VT */
	int mn = _m * _n, i, large = MAX(_m, _n), small = MIN(_m, _n);
	integer m = (integer)_m;
	integer n = (integer)_n;
	integer lda = m;
	integer ldu = m;
	integer ldvt = n;
	integer lwork, info;
	doublereal* a, *work;
	doublereal* u = (doublereal*)_u;
	doublereal* s = (doublereal*)_sigma;
	doublereal* vt = (doublereal*)_vt;
	/* copy a - contents of a will be destroyed */
	a = (doublereal*)malloc(sizeof(doublereal) * mn);
	for(i=0; i<mn; i++) a[i] = _a[i];
	/* compute lwork */
	lwork = MAX(3*small+large, 5*small-4);
	lwork *= 5;  /* larger is better */
	/* create work */
	work = (doublereal*)malloc(sizeof(doublereal) * lwork);
	/* call dgesvd */
	dgesvd_(&jobu, &jobvt, &m, &n, a, &lda, s, u, &ldu, vt, &ldvt, work, &lwork, &info);

	free(a);
	free(work);
	return info;
}


int dims_eigs(int _n, double *_a, double *w)
{
	char     jobz='V'; /* compute both eigenvalues and vectors */
	char     uplo='U'; /* store the upper triangle */
	integer  lwork;
	double   *work;
	double   *a;
	integer  info;
	int      i;
	integer  n = (integer)_n;
  
	a=(double*)malloc(sizeof(double)*n*n);
	lwork=3*n;
	lwork=lwork*5;
	work=(double*)malloc(sizeof(double)*lwork);

	for(i=0;i<n*n;i++){
		a[i]=_a[i];
	}
	dsyev_(&jobz,&uplo,&n,a,&n,w,work,&lwork,&info);
	for(i=0;i<n*n;i++)
	{
		_a[i]=a[i];
	}
	free(a);
	free(work);
	return info;
}

int dims_eigs2(int _n, double *_a, double *w)
{
	char     jobz='V'; /* computes both eigenvalues and vectors */
	char     uplo='U'; /* store the upper triangle */
	integer  lwork;
	double   *work=NULL;
	double   *a=NULL;
	integer  info;
	int      i;
	integer  n = (integer)_n;

	if(!a)
		a=(double*)malloc(sizeof(double)*n*n);
	lwork=3*n;
	lwork=lwork*5;
	if(!work)
		work=(double*)malloc(sizeof(double)*lwork);

	for(i=0;i<n*n;i++)
	{
		a[i]=_a[i];
	}
	dsyev_(&jobz,&uplo,&n,a,&n,w,work,&lwork,&info);
	for(i=0;i<n*n;i++)
	{
		_a[i]=a[i];
	}
	free(a);
	free(work);

	return info;
}

int dims_dgeev(int _n, double* _a, double* _wr, double* _wi, double* _vr)
{
	int i, nn;
	char     jobvl = 'N';
	char     jobvr = 'V';
	integer n, lda, ldvl, ldvr, lwork, info;
	doublereal *a, *wr, *wi, *vl, *vr, *work;
  
	n = (integer)_n;  
	lda = n;
	ldvl = 1;
	ldvr = n;
	lwork = 4*n;
	nn = _n*_n;

	a = (doublereal*)malloc(sizeof(doublereal)*nn);
	wr = (doublereal*)malloc(sizeof(doublereal)*n);
	wi = (doublereal*)malloc(sizeof(doublereal)*n);
	vl = (doublereal*)malloc(sizeof(doublereal)*ldvl*n);
	vr = (doublereal*)malloc(sizeof(doublereal)*ldvr*n);
	work = (doublereal *)malloc(sizeof(doublereal)*lwork);

	for(i=0; i<nn; i++) a[i] = _a[i];
  
	dgeev_(&jobvl, &jobvr, &n, 
		   a, &lda, 
		   wr, wi,
		   vl, &ldvl,
		   vr, &ldvr,
		   work, &lwork, &info);

	for(i=0; i<n; i++) _wr[i] = wr[i];
	for(i=0; i<n; i++) _wi[i] = wi[i];
	for(i=0; i<nn; i++) _vr[i] = vr[i];
	free(a);
	free(wr);
	free(wi);
	free(vl);
	free(vr);
	free(work);
	return info;
}

int dims_dgeev_simple(int _n, double* _a, double* _wr, double* _wi)
{
	int i, nn;
	char     jobvl = 'N';
	char     jobvr = 'N';
	integer n, lda, ldvl, ldvr, lwork, info;
	doublereal *a, *wr, *wi, *vl, *vr, *work;
  
	n = (integer)_n;  
	lda = n;
	ldvl = 1;
	ldvr = n;
	lwork = 3*n;
	nn = _n*_n;

	a = (doublereal*)malloc(sizeof(doublereal)*nn);
	wr = (doublereal*)malloc(sizeof(doublereal)*n);
	wi = (doublereal*)malloc(sizeof(doublereal)*n);
	vl = (doublereal*)malloc(sizeof(doublereal)*ldvl*n);
	vr = (doublereal*)malloc(sizeof(doublereal)*ldvr*n);
	work = (doublereal *)malloc(sizeof(doublereal)*lwork);

	for(i=0; i<nn; i++) a[i] = _a[i];

	dgeev_(&jobvl, &jobvr, &n, 
		   a, &lda, 
		   wr, wi,
		   vl, &ldvl,
		   vr, &ldvr,
		   work, &lwork, &info);

	for(i=0; i<n; i++)  _wr[i] = wr[i];
	for(i=0; i<n; i++) _wi[i] = wi[i];

	free(a);
	free(wr);
	free(wi);
	free(vl);
	free(vr);
	free(work);
	return info;
}

int dims_det(int _n, double* _a, double* _x)
{
	int i, nn, cnt = 0;
	const double sign[2] = {1.0, -1.0};

	integer n, info;
	doublereal *a;
	integer* ipiv;
  
	n = (integer)_n;  
	nn = _n*_n;
  
	a = (doublereal*)malloc(sizeof(doublereal)*nn);
	ipiv = (integer*)malloc(sizeof(integer)*n);
  
	for(i=0; i<nn; i++) a[i] = _a[i];
  
	dgetrf_(&n, &n,
			a, &n, 
			ipiv, &info);
  
	*_x = 1.0;
	for(i=0; i<n; i++)
    {
		*_x *= a[i*n+i];
		cnt += ((i+1)!=ipiv[i]);
    }
	*_x *= sign[cnt%2];
/*	if(cnt%2 != 0) *_x *= -1.0; */
	free(a);
	free(ipiv);
	return info;
}
