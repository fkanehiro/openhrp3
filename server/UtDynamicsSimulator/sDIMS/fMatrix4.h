/*
 * fMatrix4.h
 * Create: Katsu Yamane, Univ. of Tokyo, 03.06.17
 */

#ifndef __F_MATRIX4_H__
#define __F_MATRIX4_H__

#include "fMatrix3.h"

class fVec4;

class fMat44
{
	friend class fVec4;
public:
	fMat44() {
		m_mat = 0;
		m_vec = 0;
		m_scalar = 1;
		temp = 0;
	}
	fMat44(const fMat44& m) {
		m_mat = m.m_mat;
		m_vec = m.m_vec;
		m_scalar = m.m_scalar;
		temp = 0;
	}
	fMat44(const fMat33& m, const fVec3& v) {
		m_mat = m;
		m_vec = v;
		m_scalar = 1;
		temp = 0;
	}
	~fMat44() {
	}

	friend fMat44 inv(const fMat44& mat);
	void inv(const fMat44& mat);

	fMat44 operator = (const fMat44& mat);
	void operator = (double d);
	
	fMat33& Mat() {
		return m_mat;
	}
	friend fMat33& Mat(fMat44& m) {
		return m.m_mat;
	}
	fVec3& Vec() {
		return m_vec;
	}
	friend fVec3& Vec(fMat44& m) {
		return m.m_vec;
	}
	double& Scalar() {
		return m_scalar;
	}
	friend double& Scalar(fMat44& m) {
		return m.m_scalar;
	}

	double& operator () (int i, int j);
	double operator () (int i, int j) const;
	void operator += (const fMat44&);
	void operator -= (const fMat44&);
	void operator *= (double);
	void operator /= (double);
	friend fMat44 operator - (const fMat44&);
	friend fMat44 operator * (const fMat44&, const fMat44&);
	friend fMat44 operator + (const fMat44&, const fMat44&);
	friend fMat44 operator - (const fMat44&, const fMat44&);
	friend fVec4 operator * (const fMat44&, const fVec4&);

protected:
	fMat33 m_mat;
	fVec3 m_vec;
	double m_scalar;
	double temp;
};

class fVec4
{
	friend class fMat44;
public:
	fVec4() {
		m_vec = 0;
		m_scalar = 1;
		temp = 0;
	}
	fVec4(const fVec4& v) {
		m_vec = v.m_vec;
		m_scalar = v.m_scalar;
		temp = 0;
	}
	fVec4(const fVec3& v) {
		m_vec = v;
		m_scalar = 1;
		temp = 0;
	}
	~fVec4() {
	}

	fVec4 operator = (const fVec4& vec);
	void operator = (double d);

	friend fVec3& Vec(fVec4& vec) {
		return vec.m_vec;
	}
	fVec3& Vec() {
		return m_vec;
	}
	friend double& Scalar(fVec4& vec) {
		return vec.m_scalar;
	}
	double& Scalar() {
		return m_scalar;
	}
	void set(const fVec3& v, double s) {
		m_scalar = s;
		m_vec = v;
	}
	void set(double s1, double s2, double s3, double s4) {
		m_vec.set(s1, s2, s3);
		m_scalar = s4;
	}

	friend ostream& operator << (ostream& ost, const fVec4& mat);
	
	double& operator () (int i);
	double operator () (int i) const;
	void operator += (const fVec4&);
	void operator -= (const fVec4&);
	void operator *= (double);
	void operator /= (double);
	friend fVec4 operator - (const fVec4&);
	friend double operator * (const fVec4&, const fVec4&);
	friend fVec4 operator * (double, const fVec4&);
	friend fVec4 operator + (const fVec4&, const fVec4&);
	friend fVec4 operator - (const fVec4&, const fVec4&);

	void mul(fVec4& _vec, double d);

	void zero();

	void sub(const fVec4& vec1, const fVec4& vec2);
	void add(const fVec4& vec1, const fVec4& vec2);

	void cross(const fVec4& vec1, const fVec4& vec2);
	
protected:
	fVec3 m_vec;
	double m_scalar;
	double temp;
};

#endif
