/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*!
 * @file   fMatrix3.h
 * @author Katsu Yamane
 * @date   06/17/2003
 * @brief  3x3 matrix and 3-element vector classes.
 */

#ifndef __F_MATRIX3_H__
#define __F_MATRIX3_H__

#include <dims_common.h>
#include "fMatrix.h"

class fVec3;

/*!
 * @class  fMat33 fMatrix3.h
 * @brief  3x3 matrix class.
 */
class fMat33
{
	friend class fVec3;
public:
	//! Default constructor.
	/*!
	 * Creates a 3x3 matrix and initialize as an identity matrix.
	 */
	fMat33() {
		m11 = 1.0, m12 = 0.0, m13 = 0.0;
		m21 = 0.0, m22 = 1.0, m23 = 0.0;
		m31 = 0.0, m32 = 0.0, m33 = 1.0;
		n_row = 3;
		n_col = 3;
		temp = 0.0;
	}
	//! Constructor with initial values as array.
	/*!
	 * Constructor with initial values as array of at least 9 elements.
	 */
	fMat33(double* ini) {
		m11 = ini[0], m12 = ini[1], m13 = ini[2];
		m21 = ini[3], m22 = ini[4], m23 = ini[5];
		m31 = ini[6], m32 = ini[7], m33 = ini[8];
		n_row = 3;
		n_col = 3;
		temp = 0.0;
	}
	//! Constructor with the same initial diagonal value.
	/*!
	 * Constructor with initial diagonal values.
	 */
	fMat33(double ini) {
		m11 = m22 = m33 = ini;
		m12 = m13 = 0.0;
		m21 = m23 = 0.0;
		m31 = m32 = 0.0;
		n_row = 3;
		n_col = 3;
		temp = 0.0;
	}
	//! Constructor with all values.
	/*!
	 * Constructor with all values.
	 */
	fMat33(double _m11, double _m12, double _m13,
		   double _m21, double _m22, double _m23,
		   double _m31, double _m32, double _m33) {
		m11 = _m11;  m12 = _m12;  m13 = _m13;
		m21 = _m21;  m22 = _m22;  m23 = _m23;
		m31 = _m31;  m32 = _m32;  m33 = _m33;
		n_row = 3;
		n_col = 3;
		temp = 0.0;
	}
	//! Constructor with diagonal values.
	/*!
	 * Constructor with diagonal values.
	 */
	fMat33(double d1, double d2, double d3) {
		m11 = d1;  m12 = 0.0; m13 = 0.0;
		m21 = 0.0; m22 = d2;  m23 = 0.0;
		m31 = 0.0; m32 = 0.0; m33 = d3;
		n_row = 3;
		n_col = 3;
		temp = 0.0;
	}
	//! Copy constructor.
	/*!
	 * Copy constructor.
	 */
	fMat33(const fMat33& ini) {
		m11 = ini.m11;
		m12 = ini.m12;
		m13 = ini.m13;
		m21 = ini.m21;
		m22 = ini.m22;
		m23 = ini.m23;
		m31 = ini.m31;
		m32 = ini.m32;
		m33 = ini.m33;
		n_row = 3;
		n_col = 3;
		temp = 0.0;
	}

	//! Destructor.
	~fMat33() {
	}

	//! The reference to the (i, j)-th element.
	double& operator () (int i, int j);
	//! The value of the (i, j)-th element.
	double operator () (int i, int j) const;

	//! Value of the i-th element in the array.
	double operator[] (int i) const {
		return *(&m11 + i);
	}
	//! Assignment operator.
	fMat33 operator = (const fMat33& mat);
	//! Set the same value to the diagonal elements.
	void operator = (double d);

	/*!
	 * @name Operators
	 */
	friend fMat33 operator - (const fMat33& mat);
	void operator += (const fMat33& mat);
	void operator -= (const fMat33& mat);
	void operator *= (double d);
	void operator /= (double d);

	friend fMat33 operator + (const fMat33& mat1, const fMat33& mat2);
	friend fMat33 operator - (const fMat33& mat1, const fMat33& mat2);
	friend fMat33 operator * (double d, const fMat33& mat);
	friend fMat33 operator * (const fMat33& m1, const fMat33& m2);
	friend fVec3 operator * (const fMat33& m, const fVec3& v);
	friend fMat33 operator * (const fMat33& mat, double d);
	friend fMat33 operator / (const fMat33& mat, double d);

	//! Outputs the elements to a stream.
	friend ostream& operator << (ostream& ost, const fMat33& mat);

	//! Converts to an array.
	operator double *() {
		return &m11;
	}
	//! Pointer to the first element.
	double* data() {
		return &m11;
	}
	//! Returns the transpose.
	friend fMat33 tran(const fMat33& m);
	//! Sets the transpose.
	void tran(const fMat33&);
	//! Sets spectial matrices.
	void cross(const fVec3& p);        //!< Cross product matrix.
	void diag(double, double, double); //!< Diagonal matrix.
	void identity();                   //!< Identity matrix.
	void zero();                       //!< Zero matrix.
	//! Copies a matrix.
	void set(const fMat33& mat);
	//! Convert from Euler parameter description.
	void set(const class fEulerPara& ep);
	//! Functions for basic operations.
	void neg(const fMat33& mat);
	void add(const fMat33& mat1, const fMat33& mat2);
	void add(const fMat33& mat);
	void sub(const fMat33& mat1, const fMat33& mat2);
	void mul(const fMat33& mat1, const fMat33& mat2);
	void mul(double d, const fMat33& mat);
	void mul(const fMat33& mat, double d);
	void div(const fMat33& mat, double d);
	void mul(const fVec3& v1, const fVec3& v2);  // v1 * v2^T
	//! Converts from/to equivalent rotation axis and angle.
	void rot2mat(const fVec3&, double);
	void mat2rot(fVec3&, double&) const;

	//! Converts from Euler angles.
	void ea2mat_xyz(const fVec3& ea);  //!< Euler angles to matrix
	void ea2mat_xzy(const fVec3& ea);  //!< Euler angles to matrix
	void ea2mat_zyx(const fVec3& ea);  //!< Euler angles to matrix
	void ea2mat_yzx(const fVec3& ea);  //!< Euler angles to matrix

protected:
	double m11, m21, m31;
	double m12, m22, m32;
	double m13, m23, m33;
	double temp;
	int n_row, n_col;
};

/*!
 * @class  fVec3 fMatrix3.h
 * @brief  3-element vector class.
 */
class fVec3
{
	friend class fMat33;
public:
	fVec3() {
		m1 = m2 = m3 = 0.0;
		n_row = 3;
		n_col = 1;
		temp = 0.0;
	}
	fVec3(double* ini) {
		m1 = ini[0];
		m2 = ini[1];
		m3 = ini[2];
		n_row = 3;
		n_col = 1;
		temp = 0.0;
	}
	fVec3(double ini) {
		m1 = m2 = m3 = ini;
		n_row = 3;
		n_col = 1;
		temp = 0.0;
	}
	fVec3(const fVec3& ini) {
		m1 = ini.m1;
		m2 = ini.m2;
		m3 = ini.m3;
		n_row = 3;
		n_col = 1;
		temp = 0.0;
	}
	fVec3(double _m1, double _m2, double _m3) {
		m1 = _m1;
		m2 = _m2;
		m3 = _m3;
		n_row = 3;
		n_col = 1;
		temp = 0.0;
	}
	~fVec3() {
	}

	//! Access the i-th element.
	double& operator () (int i);
	double operator () (int i) const;
	double operator [] (int i) const {
		return *(&m1 + i);
	}
	//! Assignment operators.
	void operator = (double d);
	fVec3 operator = (const fVec3& vec);
	//! Pointer to the first element.
	double* data() {
		return &m1;
	}
	operator double *() {
		return &m1;
	}
	//! operators
	friend fVec3 operator - (const fVec3& vec);
	void operator += (const fVec3& vec);
	void operator -= (const fVec3& vec);
	void operator *= (double d);
	void operator /= (double d);
	friend fVec3 operator + (const fVec3& vec1, const fVec3& vec2);
	friend fVec3 operator - (const fVec3& vec1, const fVec3& vec2);
	friend fVec3 operator / (const fVec3& vec, double d);
	friend fVec3 operator * (double d, const fVec3& vec1);
	friend fVec3 operator * (const fVec3& vec1, double d);
	friend double operator * (const fVec3& vec1, const fVec3& vec2);
	//! Cross product.
	void cross(const fVec3& vec1, const fVec3& vec2);
	friend fVec3 operator & (const fVec3& vec1, const fVec3& vec2);
	//! Outputs to a stream.
	friend ostream& operator << (ostream& ost, const fVec3& mat);
	//! Creates a zero vector.
	void zero() {
		m1 = m2 = m3 = 0.0;
	}
	//! Returns the distance between two points.
	friend double dist(const fVec3& p1, const fVec3& p2) {
		fVec3 pp;
		pp.sub(p2, p1);
		return pp.length();
	}
	//! Returns the length of a vector.
	friend double length(const fVec3& v) {
		return sqrt(v*v);
	}
	double length() const {
		return sqrt((*this) * (*this));
	}
	//! Returns the unit vector with the same direction (with length check)
	friend fVec3 unit(const fVec3& v) {
		fVec3 ret;
		double len = v.length();
		if(len > TINY) ret = v / len;
		else ret.zero();
		return ret;
	}
	//! Converts to unit vector.
	void unit() {
		double len = length();
		if(len > TINY) (*this) /= len;
		else zero();
	}
	//! Set element values from array or three values.
	void set(double* v){
		m1=v[0];
		m2=v[1];
		m3=v[2];
	}
	void set(double d1, double d2, double d3) {
		m1 = d1;
		m2 = d2;
		m3 = d3;
	}
	//! Functions for basic operations.
	void set(const fVec3& vec);
	void neg(const fVec3& vec);
	void add(const fVec3& vec1, const fVec3& vec2);
	void add(const fVec3& vec);
	void sub(const fVec3& vec1, const fVec3& vec2);
	void div(const fVec3& vec, double d);
	void mul(const fVec3& vec, double d);
	void mul(double d, const fVec3& vec);
	void mul(const fMat33& mat, const fVec3& vec);
	//! v^T*M, same as mat^T * vec.
	void mul(const fVec3& vec, const fMat33& mat);

	//! Computes the rotation from tgt to ref.
	void rotation(const fMat33& ref, const fMat33& tgt);

	//! Orientation matrix to Euler angles.
	void mat2ea_xyz(const fMat33& mat);
	void mat2ea_xyz(const fMat33& mat, const fVec3& ea_ref);
	void mat2ea_xzy(const fMat33& mat);
	void mat2ea_zyx(const fMat33& mat);
	void mat2ea_yzx(const fMat33& mat);

	//! Computes the angular velocity from the velocity of Euler parameters.
	/*!
	 * Computes the angular velocity from the velocity of Euler parameters.
	 * @param[in] epara  Current orientation.
	 * @param[in] edot   Current Euler parameter velocity.
	 */
	void epdot2angvel(const fEulerPara& epara, const fEulerPara& edot);

	//! Computes angular acceleration from the acceleration of Euler parameters.
	/*!
	 * Computes angular acceleration from the acceleration of Euler parameters.
	 * @param[in] _e   Current orientation.
	 * @param[in] _de  Current Euler parameter velocity.
	 * @param[in] _dde Current Euler parameter acceleration.
	 */
	void epddot2angacc(const fEulerPara& _e, const fEulerPara& _de, const fEulerPara& _dde);
protected:
	double m1, m2, m3;
	double temp;
	int n_row, n_col;
};

#endif
