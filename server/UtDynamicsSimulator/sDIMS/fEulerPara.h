/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*!
 * @file   fEulerPara.h
 * @author Katsu Yamane
 * @date   06/17/2003
 * @brief  Euler parameter representation of orientation.
 */

#ifndef __FEULERPARA_H_
#define __FEULERPARA_H_

#include "hrpModelExportDef.h"
#include "fMatrix4.h"

/*!
 * @class fEulerPara  fEulerPara.h
 * @brief Euler parameter class.
 *
 * Euler parameter class, based on 4-element vector class.
 */
class HRPBASE_EXPORT fEulerPara 
	: public fVec4
{
public:
	//! Default constructor.
	fEulerPara() : fVec4() {
	}
	~fEulerPara() {
	}
	//! Constructor specifying vector and scalar parts.
	fEulerPara(const fVec3& v, double s) {
		set(v, s);
	}
	//! Constructor specifying the four elements; scalar part is @c s4.
	fEulerPara(double s1, double s2, double s3, double s4) {
		set(s1, s2, s3, s4);
	}
	
	//! Access the scalar part (cos(theta/2))
	friend double& Ang(fEulerPara& ep) {
		return ep.m_scalar;
	}
	//! Access the scalar part (cos(theta/2))
	double& Ang() {
		return m_scalar;
	}
	//! Access the scalar part (cos(theta/2))
	double Ang() const {
		return m_scalar;
	}
	//! Access the vector part (a*sin(theta/2))
	friend fVec3& Axis(fEulerPara& ep) {
		return ep.m_vec;
	}
	//! Access the vector part (a*sin(theta/2))
	fVec3& Axis() {
		return m_vec;
	}
	//! Access the vector part (a*sin(theta/2))
	const fVec3& Axis() const {
		return m_vec;
	}
	//! Assignment operator.
	fEulerPara operator= (const fEulerPara&);
	//! Assignment operator.
	fEulerPara operator= (const fMat33&);
	//! Set the elements.
	void set(const fVec3& v, double s) {
		m_scalar = s;
		m_vec = v;
	}
	//! Set the elements; scalar part is @c s4.
	void set(double s1, double s2, double s3, double s4) {
		m_vec.set(s1, s2, s3);
		m_scalar = s4;
	}
	//! Copy from a reference.
	void set(const fEulerPara&);
	//! Set from an orientation matrix.
	void set(const class fMat33&);

	//! Set identity (no rotation).
	void identity() {
		m_scalar = 1.0;
		m_vec.zero();
	}
	//! Convert to unit vector.
	void unit();
	//! Convert to unit vector.
	friend fEulerPara unit(const fEulerPara&);

	//! Multiply with a scalar.
	friend fEulerPara operator * (double, const fEulerPara&);
	//! Convert to the negative vector (represents the same orientation).
	friend fEulerPara operator - (const fEulerPara&);
	//! Convert an orientation matrix to Euler parameters.
	friend fEulerPara mat2ep(const fMat33&);
	//! Convert an Euler parameter representation to matrix.
	friend fMat33 ep2mat(const fEulerPara&);

	//! Convert angular velocity to Euler parameter veclotiy.
	/*!
	 * Convert angular velocity to Euler parameter veclotiy.
	 * @param[in] _ep  current orientation
	 * @param[in] _omega  angular velocity
	 */
	void angvel2epdot(const fEulerPara& _ep, const fVec3& _omega);
	//! Convert angular velocity to Euler parameter veclotiy.
	friend fEulerPara angvel2epdot(const fEulerPara& epara, const fVec3& vel);
	//! Convert Euler parameter velocity to angular velocity.
	friend fVec3 epdot2angvel(const fEulerPara& epara, const fEulerPara& edot);
	//! Convert Euler parameter acceleration to angular acceleration.
	/*!
	 * Convert Euler parameter acceleration to angular acceleration.
	 * @param[in] e  current orientation (Euler parameters)
	 * @param[in] de  current Euler parameter velocity
	 * @param[in] dde current Euler parameter acceleration
	 */
	friend fVec3 epddot2angacc(const fEulerPara& e, const fEulerPara& de, const fEulerPara& dde);

	//! SLERP interpolation: interpolates @c ep1 and @c ep2 into t:(1-t)
	void interpolate(const fEulerPara& ep1, const fEulerPara& ep2, double t);

	//! returns the rotation angle that makes the orientation closest to ep0 by rotating around axis s
	double rotation(const fEulerPara& ep0, const fVec3& s);

};

#endif
