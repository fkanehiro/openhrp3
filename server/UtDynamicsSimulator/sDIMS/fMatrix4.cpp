/*
 * fMatrix4.cpp
 * Create: Katsu Yamane, Univ. of Tokyo, 03.06.17
 */

#include "fMatrix4.h"

void fVec4::cross(const fVec4& vec1, const fVec4& vec2)
{
	m_scalar = vec1.m_scalar*vec2.m_scalar - vec1.m_vec*vec2.m_vec;
	m_vec.cross(vec1.m_vec, vec2.m_vec);
	m_vec += vec1.m_scalar*vec2.m_vec;
	m_vec += vec2.m_scalar*vec1.m_vec;
}

ostream& operator << (ostream& ost, const fVec4& mat)
{
	ost << "(" << mat.m_scalar << " [" << mat.m_vec(0) << " " << mat.m_vec(1) << " " << mat.m_vec(2) << "])" << flush;
	return ost;
}

double& fMat44::operator () (int i, int j)
{
#ifndef NDEBUG
	if(i < 0 || i >= 4 || j < 0 || j >= 4)
	{
		cerr << "matrix size error at operator ()" << endl;
		return temp;
	}
#endif
	if(i<3 && j<3) return m_mat(i,j);
	else if(i<3 && j==3) return m_vec(i);
	else if(i==3 && j==3) return m_scalar;
	else return temp;
}

double fMat44::operator () (int i, int j) const
{
#ifndef NDEBUG
	if(i < 0 || i >= 4 || j < 0 || j >= 4)
	{
		cerr << "matrix size error at operator ()" << endl;
		return temp;
	}
#endif
	if(i<3 && j<3) return m_mat(i,j);
	else if(i<3 && j==3) return m_vec(i);
	else if(i==3 && j==3) return m_scalar;
	else return temp;
}

void fMat44::operator = (double d)
{
	m_mat = d;
	m_vec = d;
	m_scalar = d;
}

fMat44 fMat44::operator = (const fMat44& mat)
{
	m_mat = mat.m_mat;
	m_vec = mat.m_vec;
	m_scalar = mat.m_scalar;
	return *this;
}

void fMat44::operator += (const fMat44& m)
{
	m_mat += m.m_mat;
	m_vec += m.m_vec;
	m_scalar += m.m_scalar;
}

void fMat44::operator -= (const fMat44& m)
{
	m_mat -= m.m_mat;
	m_vec -= m.m_vec;
	m_scalar -= m.m_scalar;
}

void fMat44::operator *= (double d)
{
	m_mat *= d;
	m_vec *= d;
	m_scalar *= d;
}

void fMat44::operator /= (double d)
{
	m_mat /= d;
	m_vec /= d;
	m_scalar /= d;
}

fMat44 operator - (const fMat44& m)
{
	fMat44 ret;
	ret.m_mat = - m.m_mat;
	ret.m_vec = - m.m_vec;
	ret.m_scalar = - m.m_scalar;
	return ret;
}

fMat44 operator * (const fMat44& m1, const fMat44& m2)
{
	fMat44 ret;
	ret.m_mat = m1.m_mat * m2.m_mat;
	ret.m_vec = m1.m_mat * m2.m_vec + m1.m_vec * m2.m_scalar;
	ret.m_scalar = m1.m_scalar * m2.m_scalar;
	return ret;
}

fMat44 operator + (const fMat44& m1, const fMat44& m2)
{
	fMat44 ret;
	ret.m_mat = m1.m_mat + m2.m_mat;
	ret.m_vec = m1.m_vec + m2.m_vec;
	ret.m_scalar = m1.m_scalar + m2.m_scalar;
	return ret;
}

fMat44 operator - (const fMat44& m1, const fMat44& m2)
{
	fMat44 ret;
	ret.m_mat = m1.m_mat - m2.m_mat;
	ret.m_vec = m1.m_vec - m2.m_vec;
	ret.m_scalar = m1.m_scalar - m2.m_scalar;
	return ret;
}

fVec4 operator * (const fMat44& m, const fVec4& v)
{
	fVec4 ret, tmp(v);
	ret.Vec() = m.m_mat * tmp.Vec() + m.m_vec * tmp.Scalar();
	ret.Scalar() = m.m_scalar * tmp.Scalar();
	return ret;
}

/*
 * fVec4
 */
void fVec4::sub(const fVec4& vec1, const fVec4& vec2)
{
	m_scalar = vec1.m_scalar - vec2.m_scalar;
	m_vec.sub(vec1.m_vec, vec2.m_vec);
}

void fVec4::add(const fVec4& vec1, const fVec4& vec2)
{
	m_scalar = vec1.m_scalar + vec2.m_scalar;
	m_vec.add(vec1.m_vec, vec2.m_vec);
}

void fVec4::mul(fVec4& _vec, double d)
{
	m_vec.mul(_vec.m_vec, d);
	m_scalar = _vec.m_scalar * d;
}

double& fVec4::operator () (int i)
{
#ifndef NDEBUG
	if(i < 0 || i >= 4)
	{
		cerr << "vector size error at operator ()" << endl;
		return temp;
	}
#endif
	if(i<3) return m_vec(i);
	else if(i==3) return m_scalar;
	else return temp;
}

double fVec4::operator () (int i) const
{
#ifndef NDEBUG
	if(i < 0 || i >= 4)
	{
		cerr << "vector size error at operator ()" << endl;
		return temp;
	}
#endif
	if(i<3) return m_vec(i);
	else if(i==3) return m_scalar;
	else return temp;
}

fVec4 fVec4::operator = (const fVec4& vec)
{
	m_vec = vec.m_vec;
	m_scalar = vec.m_scalar;
	return (*this);
}

void fVec4::operator = (double d)
{
	int i;
	for(i=0; i<4; i++) (*this)(i) = d;
}

void fVec4::operator += (const fVec4& vec)
{
	m_vec += vec.m_vec;
	m_scalar += vec.m_scalar;
}

void fVec4::operator -= (const fVec4& vec)
{
	m_vec -= vec.m_vec;
	m_scalar -= vec.m_scalar;
}

void fVec4::operator *= (double d)
{
	m_vec *= d;
	m_scalar *= d;
}

void fVec4::operator /= (double d)
{
	m_vec /= d;
	m_scalar /= d;
}

fVec4 operator - (const fVec4& vec)
{
	fVec4 ret;
	ret.m_vec -= vec.m_vec;
	ret.m_scalar -= vec.m_scalar;
	return ret;
}

double operator * (const fVec4& v1, const fVec4& v2)
{
	return (v1.m_vec * v2.m_vec + v1.m_scalar * v2.m_scalar);
}

fVec4 operator * (double d, const fVec4& v)
{
	fVec4 ret;
	ret.m_vec = d * v.m_vec;
	ret.m_scalar = d * v.m_scalar;
	return ret;
}

fVec4 operator + (const fVec4& v1, const fVec4& v2)
{
	fVec4 ret;
	ret.m_vec = v1.m_vec + v2.m_vec;
	ret.m_scalar = v1.m_scalar + v2.m_scalar;
	return ret;
}

fVec4 operator - (const fVec4& v1, const fVec4& v2)
{
	fVec4 ret;
	ret.m_vec = v1.m_vec - v2.m_vec;
	ret.m_scalar = v1.m_scalar - v2.m_scalar;
	return ret;
}

void fVec4::zero()
{
	m_vec.zero();
	m_scalar = 0;
}
