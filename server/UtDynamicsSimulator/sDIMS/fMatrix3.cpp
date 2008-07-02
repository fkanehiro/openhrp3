/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*!
 * @file   fMatrix3.cpp
 * @author Katsu Yamane
 * @date   06/17/2003
 * @brief  Function implementations for 3x3 matrix and 3-element vector classes.
 */

#include "fMatrix3.h"
#include "fEulerPara.h"

/*
 * Euler Parameters
 */
// x-y-z (zyx Euler angles)
void fMat33::ea2mat_xyz(const fVec3& ea)
{
	double ca = cos(ea.m1), sa = sin(ea.m1);
	double cb = cos(ea.m2), sb = sin(ea.m2);
	double cc = cos(ea.m3), sc = sin(ea.m3);
	m11 = cb*cc;
	m12 = sa*sb*cc - ca*sc;
	m13 = ca*sb*cc + sa*sc;
	m21 = cb*sc;
	m22 = sa*sb*sc + ca*cc;
	m23 = ca*sb*sc - sa*cc;
	m31 = -sb;
	m32 = sa*cb;
	m33 = ca*cb;
}

void fVec3::mat2ea_xyz(const fMat33& mat)
{
	double cb = sqrt(mat.m32*mat.m32 + mat.m33*mat.m33);
	m2 = atan2(-mat.m31, cb);
	if(cb < TINY)
	{
		m1 = atan2(mat.m12, mat.m22);
		m3 = 0.0;
	}
	else
	{
		m1 = atan2(mat.m32/cb, mat.m33/cb);
		m3 = atan2(mat.m21/cb, mat.m11/cb);
	}
}

// x-z-y (yzx Euler angles)
void fMat33::ea2mat_xzy(const fVec3& ea)
{
	double c1 = cos(ea.m1), s1 = sin(ea.m1);
	double c2 = cos(ea.m2), s2 = sin(ea.m2);
	double c3 = cos(ea.m3), s3 = sin(ea.m3);
	m11 = c2*c3;
	m12 = s1*s3 - c1*s2*c3;
	m13 = c1*s3 + s1*s2*c3;
	m21 = s2;
	m22 = c1*c2;
	m23 = -s1*c2;
	m31 = -c2*s3;
	m32 = s1*c3 + c1*s2*s3;
	m33 = c1*c3 - s1*s2*s3;
}

void fVec3::mat2ea_xzy(const fMat33& mat)
{
	double c2 = sqrt(mat.m22*mat.m22 + mat.m23*mat.m23);
	m2 = atan2(mat.m21, c2);
	if(c2 < TINY)
	{
		m1 = atan2(mat.m32, mat.m33);
		m3 = 0.0;
	}
	else
	{
		m1 = atan2(-mat.m23, mat.m22);
		m3 = atan2(-mat.m31, mat.m11);
	}
}

// z-y-x (xyz Euler angles)
void fMat33::ea2mat_zyx(const fVec3& ea)
{
	double c1 = cos(ea.m1), s1 = sin(ea.m1);
	double c2 = cos(ea.m2), s2 = sin(ea.m2);
	double c3 = cos(ea.m3), s3 = sin(ea.m3);
	m11 = c1*c2;
	m12 = -s1*c2;
	m13 = s2;
	m21 = s1*c3 + c1*s2*s3;
	m22 = c1*c3 - s1*s2*s3;
	m23 = -c2*s3;
	m31 = s1*s3 - c1*s2*c3;
	m32 = c1*s3 + s1*s2*s3;
	m33 = c2*c3;
}

void fVec3::mat2ea_zyx(const fMat33& mat)
{
	double c2 = sqrt(mat.m11*mat.m11 + mat.m12*mat.m12);
	m2 = atan2(mat.m13, c2);
	if(c2 < TINY)
	{
		m1 = atan2(mat.m21, mat.m22);
		m3 = 0.0;
	}
	else
	{
		m1 = atan2(-mat.m12, mat.m11);
		m3 = atan2(-mat.m23, mat.m33);
	}
}

// y-z-x (xzy Euler angles)
void fMat33::ea2mat_yzx(const fVec3& ea)
{
	double c1 = cos(ea.m1), s1 = sin(ea.m1);
	double c2 = cos(ea.m2), s2 = sin(ea.m2);
	double c3 = cos(ea.m3), s3 = sin(ea.m3);
	m11 = c1*c2;
	m12 = -s2;
	m13 = s1*c2;
	m21 = s1*s3 + c1*s2*c3;
	m22 = c2*c3;
	m23 = -c1*s3 + s1*s2*c3;
	m31 = -s1*c3 + c1*s2*s3;
	m32 = c2*s3;
	m33 = c1*c3 + s1*s2*s3;
}

void fVec3::mat2ea_yzx(const fMat33& mat)
{
	double c2 = sqrt(mat.m11*mat.m11 + mat.m13*mat.m13);
	m2 = atan2(-mat.m12, c2);
	if(c2 < TINY)
	{
		m1 = atan2(mat.m23, mat.m33);
		m3 = 0.0;
	}
	else
	{
		m1 = atan2(mat.m13, mat.m11);
		m3 = atan2(mat.m32, mat.m22);
	}
}

void fVec3::mat2ea_xyz(const fMat33& mat, const fVec3& ea_ref)
{
	mat2ea_xyz(mat);
	if(fabs(m1-ea_ref(0)) > PI)
	{
		if(m1 < -PI*0.5 || ea_ref(0) > PI*0.5)
		{
			m1 += 2.0*PI;
		}
		else if(m1 > PI*0.5 || ea_ref(0) < -PI*0.5)
		{
			m1 -= 2.0*PI;
		}
	}
	if(fabs(m3-ea_ref(2)) > PI)
	{
//		cout << m3 << ", " << ea_ref(2) << endl;
		if(m3 < -PI*0.5 || ea_ref(2) > PI*0.5)
		{
//			cout << " " << m3 << "->" << flush;
			m3 += 2.0*PI;
//			cout << m3 << endl;
		}
		else if(m3 > PI*0.5 || ea_ref(2) < -PI*0.5)
		{
//			cout << " " << m3 << "->" << flush;
			m3 -= 2.0*PI;
//			cout << m3 << endl;
		}
	}
}

void fVec3::epdot2angvel(const fEulerPara& _epara, const fEulerPara& _edot)
{
	static fVec3 temp;
	fEulerPara epara(_epara), edot(_edot);
	fMat33 mat(epara(3), epara(2), -epara(1), -epara(2), epara(3), epara(0), epara(1), -epara(0), epara(3));
	mul(mat, edot.Vec());
	temp.mul(-edot(3), epara.Vec());
	add(temp);
	(*this) *= 2.0;
//	ret = (- edot(3)*epara.Vec() + mat*edot.Vec()) * 2;
}

void fVec3::epddot2angacc(const fEulerPara& _e, const fEulerPara& _de, const fEulerPara& _dde)
{
	fEulerPara e(_e), de(_de), dde(_dde);
	fMat33 mat1(e(3), e(2), -e(1), -e(2), e(3), e(0), e(1), -e(0), e(3));
	fMat33 mat2(de(3), de(2), -de(1), -de(2), de(3), de(0), de(1), -de(0), de(3));
	static fVec3 tmp;
	mul(mat2, de.Vec());
	tmp.mul(mat1, dde.Vec());
	add(tmp);
	tmp.mul(-de(3), de.Vec());
	add(tmp);
	tmp.mul(-dde(3), e.Vec());
	add(tmp);
	(*this) *= 2.0;
}

/*
 * set special values
 */
void fMat33::cross(const fVec3& p)
{
	m11 = 0;  m12 = -p.m3;  m13 = p.m2;
	m21 = p.m3;  m22 = 0;  m23 = -p.m1;
	m31 = -p.m2;  m32 = p.m1;  m33 = 0;
}

void fMat33::diag(double v1, double v2, double v3)
{
	m11 = v1;  m12 = 0;  m13 = 0;
	m21 = 0;  m22 = v2;  m23 = 0;
	m31 = 0;  m32 = 0;  m33 = v3;
}
void fMat33::identity()
{
	m11 = 1;   m12 = 0;   m13 = 0;
	m21 = 0;   m22 = 1;   m23 = 0;
	m31 = 0;   m32 = 0;   m33 = 1;
}
void fMat33::zero()
{
	m11 = 0;   m12 = 0;   m13 = 0;
	m21 = 0;   m22 = 0;   m23 = 0;
	m31 = 0;   m32 = 0;   m33 = 0;
}

/*
 * equivalent rotation
 */
inline double sgn(double x)
{
	if(x < 0.0) return -1.0;
	else if(x > 0.0) return 1.0;
	else return 0.0;
}

void fMat33::mat2rot(fVec3& axis, double& angle) const
{
	double e = ((*this)(2,1) - (*this)(1,2)) * ((*this)(2,1) - (*this)(1,2)) +
	           ((*this)(0,2) - (*this)(2,0)) * ((*this)(0,2) - (*this)(2,0)) +
	           ((*this)(1,0) - (*this)(0,1)) * ((*this)(1,0) - (*this)(0,1));
	double c = 0.5 * ((*this)(0,0) + (*this)(1,1) + (*this)(2,2) - 1.0);

	double v = 1.0 - c;
	if(v < TINY)
	{
		angle = 0.0;
		axis(0) = 0.0;
		axis(1) = 0.0;
		axis(2) = 1.0;
	}
	else
	{
		double s = 0.5 * sqrt(e);
		angle = atan2(s, c);

		if(c > -0.7071)
		{
			// when angle < 3 * pi / 4
			if(fabs(s) < TINY)
			{
				axis(0) = 0.0;
				axis(1) = 0.0;
				axis(2) = 1.0;
			}
			else
			{
				axis(0) = 0.5 * ((*this)(2,1) - (*this)(1,2)) / s;
				axis(1) = 0.5 * ((*this)(0,2) - (*this)(2,0)) / s;
				axis(2) = 0.5 * ((*this)(1,0) - (*this)(0,1)) / s;
			}
		}
		else
		{
			// when 3* pi /4 < angle
			double rx = (*this)(0,0) - c;
			double ry = (*this)(1,1) - c;
			double rz = (*this)(2,2) - c;
		
			if(rx > ry)
			{
				if(rx > rz)
				{
					// rx is largest
					if (fabs(s) < TINY)
					{
						axis(0) = sqrt(rx / v);
					}
					else
					{
						axis(0) = sgn((*this)(2,1) - (*this)(1,2)) * sqrt(rx / v);
					}
					axis(1) = ((*this)(1,0) + (*this)(0,1)) / (2 * axis(0) * v);
					axis(2) = ((*this)(0,2) + (*this)(2,0)) / (2 * axis(0) * v);
				}
				else
				{
					// rz is largest
					if (fabs(s) < TINY)
					{
						axis(2) = sqrt(rz / v);
					}
					else
					{
						axis(2) = sgn((*this)(1,0) - (*this)(0,1)) * sqrt(rz / v);
					}
					axis(0) = ((*this)(0,2) + (*this)(2,0)) / (2 * axis(2) * v);
					axis(1) = ((*this)(1,2) + (*this)(2,1)) / (2 * axis(2) * v);
				}
			}
			else
			{
				if(ry > rz)
				{
					// ry is largest
					if (fabs(s) < TINY)
					{
						axis(1) = sqrt(ry / v);
					}
					else
					{
						axis(1) = sgn((*this)(0,2) - (*this)(2,0)) * sqrt(ry / v);
					}
					axis(0) = ((*this)(0,1) + (*this)(1,0)) / (2 * axis(1) * v);
					axis(2) = ((*this)(1,2) + (*this)(2,1)) / (2 * axis(1) * v);
				}
				else
				{
					// rz is largest
					if (fabs(s) < TINY)
					{
						axis(2) = sqrt(rz / v);
					}
					else
					{
						axis(2) = sgn((*this)(1,0) - (*this)(0,1)) * sqrt(rz / v);
					}
					axis(0) = ((*this)(0,2) + (*this)(2,0)) / (2 * axis(2) * v);
					axis(1) = ((*this)(1,2) + (*this)(2,1)) / (2 * axis(2) * v);
				}
			}
		}
	}
}

void fMat33::rot2mat(const fVec3& axis, double angle)
{
	double x = axis.m1, y = axis.m2, z = axis.m3;
	double sa = sin(angle), ca = cos(angle);
	(*this)(0,0) = ca + (1-ca)*x*x;
	(*this)(0,1) = (1-ca)*x*y - sa*z;
	(*this)(0,2) = (1-ca)*x*z + sa*y;
	(*this)(1,0) = (1-ca)*y*x + sa*z;
	(*this)(1,1) = ca + (1-ca)*y*y;
	(*this)(1,2) = (1-ca)*y*z - sa*x;
	(*this)(2,0) = (1-ca)*z*x - sa*y;
	(*this)(2,1) = (1-ca)*z*y + sa*x;
	(*this)(2,2) = ca + (1-ca)*z*z;
}

/*
 * operators (matrix)
 */
fMat33 fMat33::operator = (const fMat33& mat)
{
	m11 = mat.m11;
	m12 = mat.m12;
	m13 = mat.m13;
	m21 = mat.m21;
	m22 = mat.m22;
	m23 = mat.m23;
	m31 = mat.m31;
	m32 = mat.m32;
	m33 = mat.m33;
	return *this;
}

void fMat33::operator = (double d)
{
	m11 = m22 = m33 = d;
	m12 = m13 = 0.0;
	m21 = m23 = 0.0;
	m31 = m32 = 0.0;
}

fMat33 operator - (const fMat33& mat)
{
	fMat33 ret;
	ret.m11 = - mat.m11;
	ret.m12 = - mat.m12;
	ret.m13 = - mat.m13;
	ret.m21 = - mat.m21;
	ret.m22 = - mat.m22;
	ret.m23 = - mat.m23;
	ret.m31 = - mat.m31;
	ret.m32 = - mat.m32;
	ret.m33 = - mat.m33;
	return ret;
}

void fMat33::operator += (const fMat33& mat)
{
	m11 += mat.m11;
	m12 += mat.m12;
	m13 += mat.m13;
	m21 += mat.m21;
	m22 += mat.m22;
	m23 += mat.m23;
	m31 += mat.m31;
	m32 += mat.m32;
	m33 += mat.m33;
}

void fMat33::operator -= (const fMat33& mat)
{
	m11 -= mat.m11;
	m12 -= mat.m12;
	m13 -= mat.m13;
	m21 -= mat.m21;
	m22 -= mat.m22;
	m23 -= mat.m23;
	m31 -= mat.m31;
	m32 -= mat.m32;
	m33 -= mat.m33;
}

void fMat33::operator *= (double d)
{
	m11 *= d;  	m12 *= d;  	m13 *= d;
	m21 *= d;  	m22 *= d;  	m23 *= d;
	m31 *= d;  	m32 *= d;  	m33 *= d;
} 

void fMat33::operator /= (double d)
{
	m11 /= d;  	m12 /= d;  	m13 /= d;
	m21 /= d;  	m22 /= d;  	m23 /= d;
	m31 /= d;  	m32 /= d;  	m33 /= d;
} 

fMat33 operator + (const fMat33& mat1, const fMat33& mat2)
{
	fMat33 ret;
	ret.m11 = mat1.m11 + mat2.m11;
	ret.m12 = mat1.m12 + mat2.m12;
	ret.m13 = mat1.m13 + mat2.m13;
	ret.m21 = mat1.m21 + mat2.m21;
	ret.m22 = mat1.m22 + mat2.m22;
	ret.m23 = mat1.m23 + mat2.m23;
	ret.m31 = mat1.m31 + mat2.m31;
	ret.m32 = mat1.m32 + mat2.m32;
	ret.m33 = mat1.m33 + mat2.m33;
	return ret;
}

fMat33 operator - (const fMat33& mat1, const fMat33& mat2)
{
	fMat33 ret;
	ret.m11 = mat1.m11 - mat2.m11;
	ret.m12 = mat1.m12 - mat2.m12;
	ret.m13 = mat1.m13 - mat2.m13;
	ret.m21 = mat1.m21 - mat2.m21;
	ret.m22 = mat1.m22 - mat2.m22;
	ret.m23 = mat1.m23 - mat2.m23;
	ret.m31 = mat1.m31 - mat2.m31;
	ret.m32 = mat1.m32 - mat2.m32;
	ret.m33 = mat1.m33 - mat2.m33;
	return ret;
}

fMat33 operator * (double d, const fMat33& mat)
{
	fMat33 ret;
	ret.m11 = d * mat.m11;
	ret.m12 = d * mat.m12;
	ret.m13 = d * mat.m13;
	ret.m21 = d * mat.m21;
	ret.m22 = d * mat.m22;
	ret.m23 = d * mat.m23;
	ret.m31 = d * mat.m31;
	ret.m32 = d * mat.m32;
	ret.m33 = d * mat.m33;
	return ret;
}

fMat33 operator * (const fMat33& mat1, const fMat33& mat2)
{
	fMat33 ret;
	ret.m11 = mat1.m11*mat2.m11 + mat1.m12*mat2.m21 + mat1.m13*mat2.m31;
	ret.m21 = mat1.m21*mat2.m11 + mat1.m22*mat2.m21 + mat1.m23*mat2.m31;
	ret.m31 = mat1.m31*mat2.m11 + mat1.m32*mat2.m21 + mat1.m33*mat2.m31;
	ret.m12 = mat1.m11*mat2.m12 + mat1.m12*mat2.m22 + mat1.m13*mat2.m32;
	ret.m22 = mat1.m21*mat2.m12 + mat1.m22*mat2.m22 + mat1.m23*mat2.m32;
	ret.m32 = mat1.m31*mat2.m12 + mat1.m32*mat2.m22 + mat1.m33*mat2.m32;
	ret.m13 = mat1.m11*mat2.m13 + mat1.m12*mat2.m23 + mat1.m13*mat2.m33;
	ret.m23 = mat1.m21*mat2.m13 + mat1.m22*mat2.m23 + mat1.m23*mat2.m33;
	ret.m33 = mat1.m31*mat2.m13 + mat1.m32*mat2.m23 + mat1.m33*mat2.m33;
	return ret;
}

double& fMat33::operator () (int i, int j)
{
#ifndef NDEBUG
	if(i<0 || i>=3 || j<0 || j>=3)
	{
		cerr << "matrix size error at operator ()" << endl;
		return temp;
	}
#endif
	return *(&m11 + i + j*3);
}

double fMat33::operator () (int i, int j) const
{
#ifndef NDEBUG
	if(i<0 || i>=3 || j<0 || j>=3)
	{
		cerr << "matrix size error at operator ()" << endl;
		return temp;
	}
#endif
	return *(&m11 + i + j*3);
}

double& fVec3::operator () (int i)
{
#ifndef NDEBUG
	if(i<0 || i>=3)
	{
		cerr << "vector size error at operator ()" << endl;
		return temp;
	}
#endif
	return *(&m1 + i);
}

double fVec3::operator () (int i) const
{
#ifndef NDEBUG
	if(i<0 || i>=3)
	{
		cerr << "vector size error at operator ()" << endl;
		return temp;
	}
#endif
	return *(&m1 + i);
}

fVec3 operator * (const fMat33& m, const fVec3& v)
{
	fVec3 ret;
	ret.data()[0] = m.m11*v[0] + m.m12*v[1] + m.m13*v[2];
	ret.data()[1] = m.m21*v[0] + m.m22*v[1] + m.m23*v[2];
	ret.data()[2] = m.m31*v[0] + m.m32*v[1] + m.m33*v[2];
	return ret;
}

fMat33 operator * (const fMat33& mat, double d)
{
	fMat33 ret;
	ret.m11 = mat.m11 * d;
	ret.m12 = mat.m12 * d;
	ret.m13 = mat.m13 * d;
	ret.m21 = mat.m21 * d;
	ret.m22 = mat.m22 * d;
	ret.m23 = mat.m23 * d;
	ret.m31 = mat.m31 * d;
	ret.m32 = mat.m32 * d;
	ret.m33 = mat.m33 * d;
	return ret;
}

fMat33 operator / (const fMat33& mat, double d)
{
	fMat33 ret;
	ret.m11 = mat.m11 / d;
	ret.m12 = mat.m12 / d;
	ret.m13 = mat.m13 / d;
	ret.m21 = mat.m21 / d;
	ret.m22 = mat.m22 / d;
	ret.m23 = mat.m23 / d;
	ret.m31 = mat.m31 / d;
	ret.m32 = mat.m32 / d;
	ret.m33 = mat.m33 / d;
	return ret;
}

/*
 * functions (matrix)
 */
fMat33 tran(const fMat33& mat)
{
	fMat33 ret;
	ret.m11 = mat.m11;  ret.m12 = mat.m21;  ret.m13 = mat.m31;
	ret.m21 = mat.m12;  ret.m22 = mat.m22;  ret.m23 = mat.m32;
	ret.m31 = mat.m13;  ret.m32 = mat.m23;  ret.m33 = mat.m33;
	return ret;
}

void fMat33::tran(const fMat33& mat)
{
	m11 = mat.m11;  m12 = mat.m21;  m13 = mat.m31;
	m21 = mat.m12;  m22 = mat.m22;  m23 = mat.m32;
	m31 = mat.m13;  m32 = mat.m23;  m33 = mat.m33;
}

void fMat33::set(const fMat33& mat)
{
	m11 = mat.m11;   m12 = mat.m12;   m13 = mat.m13;
	m21 = mat.m21;   m22 = mat.m22;   m23 = mat.m23;
	m31 = mat.m31;   m32 = mat.m32;   m33 = mat.m33;
}

void fMat33::set(const fEulerPara& ep)
{
	fEulerPara epara(ep);
	double ex = epara(0), ey = epara(1), ez = epara(2), e = epara(3);
	double ee = e * e, exx = ex * ex, eyy = ey * ey, ezz = ez * ez;
	double exy = ex * ey, eyz = ey * ez, ezx = ez * ex;
	double eex = e * ex, eey = e * ey, eez = e * ez;
	m11 = exx - eyy - ezz + ee;
	m22 = - exx + eyy - ezz + ee;
	m33 = - exx - eyy + ezz + ee;
	m12 = 2.0 * (exy - eez);
	m21 = 2.0 * (exy + eez);
	m13 = 2.0 * (ezx + eey);
	m31 = 2.0 * (ezx - eey);
	m23 = 2.0 * (eyz - eex);
	m32 = 2.0 * (eyz + eex);
}

void fMat33::neg(const fMat33& mat)
{
	m11 = -mat.m11;   m12 = -mat.m12;   m13 = -mat.m13;
	m21 = -mat.m21;   m22 = -mat.m22;   m23 = -mat.m23;
	m31 = -mat.m31;   m32 = -mat.m32;   m33 = -mat.m33;
}

void fMat33::add(const fMat33& mat1, const fMat33& mat2)
{
	m11 = mat1.m11 + mat2.m11;
	m12 = mat1.m12 + mat2.m12;
	m13 = mat1.m13 + mat2.m13;
	m21 = mat1.m21 + mat2.m21;
	m22 = mat1.m22 + mat2.m22;
	m23 = mat1.m23 + mat2.m23;
	m31 = mat1.m31 + mat2.m31;
	m32 = mat1.m32 + mat2.m32;
	m33 = mat1.m33 + mat2.m33;
}

void fMat33::add(const fMat33& mat)
{
	m11 += mat.m11;
	m12 += mat.m12;
	m13 += mat.m13;
	m21 += mat.m21;
	m22 += mat.m22;
	m23 += mat.m23;
	m31 += mat.m31;
	m32 += mat.m32;
	m33 += mat.m33;
}

void fMat33::sub(const fMat33& mat1, const fMat33& mat2)
{
	m11 = mat1.m11 - mat2.m11;
	m12 = mat1.m12 - mat2.m12;
	m13 = mat1.m13 - mat2.m13;
	m21 = mat1.m21 - mat2.m21;
	m22 = mat1.m22 - mat2.m22;
	m23 = mat1.m23 - mat2.m23;
	m31 = mat1.m31 - mat2.m31;
	m32 = mat1.m32 - mat2.m32;
	m33 = mat1.m33 - mat2.m33;
}

void fMat33::mul(const fMat33& mat1, const fMat33& mat2)
{
	m11 = mat1.m11*mat2.m11 + mat1.m12*mat2.m21 + mat1.m13*mat2.m31;
	m21 = mat1.m21*mat2.m11 + mat1.m22*mat2.m21 + mat1.m23*mat2.m31;
	m31 = mat1.m31*mat2.m11 + mat1.m32*mat2.m21 + mat1.m33*mat2.m31;
	m12 = mat1.m11*mat2.m12 + mat1.m12*mat2.m22 + mat1.m13*mat2.m32;
	m22 = mat1.m21*mat2.m12 + mat1.m22*mat2.m22 + mat1.m23*mat2.m32;
	m32 = mat1.m31*mat2.m12 + mat1.m32*mat2.m22 + mat1.m33*mat2.m32;
	m13 = mat1.m11*mat2.m13 + mat1.m12*mat2.m23 + mat1.m13*mat2.m33;
	m23 = mat1.m21*mat2.m13 + mat1.m22*mat2.m23 + mat1.m23*mat2.m33;
	m33 = mat1.m31*mat2.m13 + mat1.m32*mat2.m23 + mat1.m33*mat2.m33;
}

void fMat33::mul(double d, const fMat33& mat)
{
	m11 = d * mat.m11;
	m12 = d * mat.m12;
	m13 = d * mat.m13;
	m21 = d * mat.m21;
	m22 = d * mat.m22;
	m23 = d * mat.m23;
	m31 = d * mat.m31;
	m32 = d * mat.m32;
	m33 = d * mat.m33;
}

void fMat33::mul(const fMat33& mat, double d)
{
	m11 = d * mat.m11;
	m12 = d * mat.m12;
	m13 = d * mat.m13;
	m21 = d * mat.m21;
	m22 = d * mat.m22;
	m23 = d * mat.m23;
	m31 = d * mat.m31;
	m32 = d * mat.m32;
	m33 = d * mat.m33;
}

void fMat33::mul(const fVec3& v1, const fVec3& v2)
{
	m11 = v1.m1 * v2.m1;
	m12 = v1.m1 * v2.m2;
	m13 = v1.m1 * v2.m3;
	m21 = v1.m2 * v2.m1;
	m22 = v1.m2 * v2.m2;
	m23 = v1.m2 * v2.m3;
	m31 = v1.m3 * v2.m1;
	m32 = v1.m3 * v2.m2;
	m33 = v1.m3 * v2.m3;
}

void fMat33::div(const fMat33& mat, double d)
{
	m11 = mat.m11 / d;
	m12 = mat.m12 / d;
	m13 = mat.m13 / d;
	m21 = mat.m21 / d;
	m22 = mat.m22 / d;
	m23 = mat.m23 / d;
	m31 = mat.m31 / d;
	m32 = mat.m32 / d;
	m33 = mat.m33 / d;
}

/*
 * operators (vector)
 */
void fVec3::operator = (double d)
{
	m1 = m2 = m3 = d;
}

fVec3 fVec3::operator = (const fVec3& vec)
{
	m1 = vec.m1;  m2 = vec.m2;  m3 = vec.m3;
	return *this;
}

fVec3 operator & (const fVec3& vec1, const fVec3& vec2)
{
	fVec3 ret;
	ret.m1 = vec1.m2*vec2.m3 - vec1.m3*vec2.m2;
	ret.m2 = vec1.m3*vec2.m1 - vec1.m1*vec2.m3;
	ret.m3 = vec1.m1*vec2.m2 - vec1.m2*vec2.m1;
	return ret;
}

fVec3 operator - (const fVec3& vec)
{
	fVec3 ret;
	ret.m1 = -vec.m1;
	ret.m2 = -vec.m2;
	ret.m3 = -vec.m3;
	return ret;
}

void fVec3::operator += (const fVec3& vec)
{
	m1 += vec.m1;
	m2 += vec.m2;
	m3 += vec.m3;
}

void fVec3::operator -= (const fVec3& vec)
{
	m1 -= vec.m1;
	m2 -= vec.m2;
	m3 -= vec.m3;
}

void fVec3::operator *= (double d)
{
	m1 *= d;
	m2 *= d;
	m3 *= d;
}

void fVec3::operator /= (double d)
{
	m1 /= d;
	m2 /= d;
	m3 /= d;
}

fVec3 operator - (const fVec3& vec1, const fVec3& vec2)
{
	fVec3 ret;
	ret.m1 = vec1.m1 - vec2.m1;
	ret.m2 = vec1.m2 - vec2.m2;
	ret.m3 = vec1.m3 - vec2.m3;
	return ret;
}

fVec3 operator + (const fVec3& vec1, const fVec3& vec2)
{
	fVec3 ret;
	ret.m1 = vec1.m1 + vec2.m1;
	ret.m2 = vec1.m2 + vec2.m2;
	ret.m3 = vec1.m3 + vec2.m3;
	return ret;
}

fVec3 operator / (const fVec3& vec, double d)
{
	fVec3 ret;
	ret.m1 = vec.m1 / d;
	ret.m2 = vec.m2 / d;
	ret.m3 = vec.m3 / d;
	return ret;
}

double operator * (const fVec3& vec1, const fVec3& vec2)
{
	double ret = 0;
	ret = vec1.m1*vec2.m1 + vec1.m2*vec2.m2 + vec1.m3*vec2.m3;
	return ret;
}

fVec3 operator * (const fVec3& vec1, double d)
{
	fVec3 ret;
	ret.m1 = vec1.m1 * d;
	ret.m2 = vec1.m2 * d;
	ret.m3 = vec1.m3 * d;
	return ret;
}

fVec3 operator * (double d, const fVec3& vec1)
{
	fVec3 ret;
	ret.m1 = vec1.m1 * d;
	ret.m2 = vec1.m2 * d;
	ret.m3 = vec1.m3 * d;
	return ret;
}

/*
 * functions (vector)
 */
void fVec3::set(const fVec3& vec)
{
	m1 = vec.m1;
	m2 = vec.m2;
	m3 = vec.m3;
}

void fVec3::neg(const fVec3& vec)
{
	m1 = -vec.m1;
	m2 = -vec.m2;
	m3 = -vec.m3;
}

void fVec3::add(const fVec3& vec1, const fVec3& vec2)
{
	m1 = vec1.m1 + vec2.m1;
	m2 = vec1.m2 + vec2.m2;
	m3 = vec1.m3 + vec2.m3;
}

void fVec3::add(const fVec3& vec)
{
	m1 += vec.m1;
	m2 += vec.m2;
	m3 += vec.m3;
}

void fVec3::sub(const fVec3& vec1, const fVec3& vec2)
{
	m1 = vec1.m1 - vec2.m1;
	m2 = vec1.m2 - vec2.m2;
	m3 = vec1.m3 - vec2.m3;
}

void fVec3::div(const fVec3& vec, double d)
{
	m1 = vec.m1 / d;
	m2 = vec.m2 / d;
	m3 = vec.m3 / d;
}

void fVec3::mul(const fVec3& vec, double d)
{
	m1 = vec.m1 * d;
	m2 = vec.m2 * d;
	m3 = vec.m3 * d;
}

void fVec3::mul(double d, const fVec3& vec)
{
	m1 = vec.m1 * d;
	m2 = vec.m2 * d;
	m3 = vec.m3 * d;
}

void fVec3::mul(const fMat33& mat, const fVec3& vec)
{
	m1 = mat.m11*vec.m1 + mat.m12*vec.m2 + mat.m13*vec.m3;
	m2 = mat.m21*vec.m1 + mat.m22*vec.m2 + mat.m23*vec.m3;
	m3 = mat.m31*vec.m1 + mat.m32*vec.m2 + mat.m33*vec.m3;
}

void fVec3::mul(const fVec3& vec, const fMat33& mat)
{
	m1 = vec.m1*mat.m11 + vec.m2*mat.m21 + vec.m3*mat.m31;
	m2 = vec.m1*mat.m12 + vec.m2*mat.m22 + vec.m3*mat.m32;
	m3 = vec.m1*mat.m13 + vec.m2*mat.m23 + vec.m3*mat.m33;
}

void fVec3::cross(const fVec3& vec1, const fVec3& vec2)
{
	m1 = vec1.m2*vec2.m3 - vec1.m3*vec2.m2;
	m2 = vec1.m3*vec2.m1 - vec1.m1*vec2.m3;
	m3 = vec1.m1*vec2.m2 - vec1.m2*vec2.m1;
}

/*
 * rotation from target to ref
 */
void fVec3::rotation(const fMat33& ref, const fMat33& target)
{
	static fMat33 tmp;
	static fVec3 v;
	tmp.mul(ref, tran(target));
	v.m1 = tmp.m32 - tmp.m23;
	v.m2 = tmp.m13 - tmp.m31;
	v.m3 = tmp.m21 - tmp.m12;
	(*this) = 0.5 * v;
}

/*
 * stream output
 */
ostream& operator << (ostream& ost, const fVec3& v)
{
  ost << "(" << v.m1 << ", " << v.m2 << ", " << v.m3 << ")" << flush;
  return ost;
}

ostream& operator << (ostream& ost, const fMat33& m)
{
  ost << "(" << m.m11 << ", " << m.m12 << ", " << m.m13 << "," << endl
      << " " << m.m21 << ", " << m.m22 << ", " << m.m23 << "," << endl
      << " " << m.m31 << ", " << m.m32 << ", " << m.m33 << ")" << flush;
  return ost;
}
