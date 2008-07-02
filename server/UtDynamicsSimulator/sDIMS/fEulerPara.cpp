/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*
 * fEulerPara.cpp
 * Create: Katz Yamane, 01.07.09
 */

#include "fEulerPara.h"

#define tiny 0.05
#define eps 1e-8

void fEulerPara::interpolate(const fEulerPara& ep1, const fEulerPara& _ep2, double t)
{
	fEulerPara ep2;
	ep2.set(_ep2);
	double cth = ep1 * ep2;
	if(cth < 0)
	{
		ep2 *= -1.0;
		cth *= -1.0;
	}
	if(cth > 1.0) cth = 1.0;
	double th = acos(cth);
	double sth = sin(th);
	double th2 = th * t;
	fEulerPara ep_temp1, ep_temp2;
	if(fabs(sth) < eps)
	{
		ep_temp1 = (1-t)*ep1;
		ep_temp2 = t*ep2;
		add(ep_temp1, ep_temp2);
	}
	else
	{
		ep_temp1 = sin(th-th2) * ep1;
		ep_temp2 = sin(th2) * ep2;
		add(ep_temp1, ep_temp2);
		(*this) /= sth;
	}
	unit();
}

void fEulerPara::set(const fEulerPara& _ep)
{
	m_scalar = _ep.m_scalar;
	m_vec.set(_ep.m_vec);
}

void fEulerPara::set(const fMat33& _mat)
{
	static fMat33 mat;
	mat.set(_mat);
	double tr, s;
	tr = mat(0,0) + mat(1,1) + mat(2,2);
	if(tr >= 0)
	{
		s = sqrt(tr + 1);
		m_scalar = 0.5 * s;
		s = 0.5 / s;
		m_vec(0) = (mat(2,1) - mat(1,2)) * s;
		m_vec(1) = (mat(0,2) - mat(2,0)) * s;
		m_vec(2) = (mat(1,0) - mat(0,1)) * s;
	}
	else
	{
		int i = 0;
		if(mat(1,1) > mat(0,0)) i = 1;
		if(mat(2,2) > mat(i,i)) i = 2;
		switch(i)
		{
		case 0:
			s = sqrt((mat(0,0) - (mat(1,1) + mat(2,2))) + 1);
			m_vec(0) = 0.5 * s;
			s = 0.5 / s;
			m_vec(1) = (mat(0,1) + mat(1,0)) * s;
			m_vec(2) = (mat(2,0) + mat(0,2)) * s;
			m_scalar = (mat(2,1) - mat(1,2)) * s;
			break;
		case 1:
			s = sqrt((mat(1,1) - (mat(2,2) + mat(0,0))) + 1);
			m_vec(1) = 0.5 * s;
			s = 0.5 / s;
			m_vec(2) = (mat(1,2) + mat(2,1)) * s;
			m_vec(0) = (mat(0,1) + mat(1,0)) * s;
			m_scalar = (mat(0,2) - mat(2,0)) * s;
			break;
		case 2:
			s = sqrt((mat(2,2) - (mat(0,0) + mat(1,1))) + 1);
			m_vec(2) = 0.5 * s;
			s = 0.5 / s;
			m_vec(0) = (mat(2,0) + mat(0,2)) * s;
			m_vec(1) = (mat(1,2) + mat(2,1)) * s;
			m_scalar = (mat(1,0) - mat(0,1)) * s;
			break;
		}
	}
}

void fEulerPara::angvel2epdot(const fEulerPara& _ep, const fVec3& _omega)
{
	fEulerPara ep(_ep);
	static fVec3 vel;
	vel.set(_omega);
	double e0 = ep(3), e1 = ep(0), e2 = ep(1), e3 = ep(2);
	double x = vel(0), y = vel(1), z = vel(2);
	m_scalar = - 0.5 * (e1*x + e2*y + e3*z);
	m_vec(0) = 0.5 * (e0*x - e3*y + e2*z);
	m_vec(1) = 0.5 * (e3*x + e0*y - e1*z);
	m_vec(2) = 0.5 * (- e2*x + e1*y + e0*z);
}

fEulerPara mat2ep(const fMat33& _mat)
{
	fEulerPara ret;
	static fMat33 mat;
	mat.set(_mat);
#if 1  // based on Baraff's code
	double tr, s;
	tr = mat(0,0) + mat(1,1) + mat(2,2);
	if(tr >= 0)
	{
		s = sqrt(tr + 1);
		ret(3) = 0.5 * s;
		s = 0.5 / s;
		ret(0) = (mat(2,1) - mat(1,2)) * s;
		ret(1) = (mat(0,2) - mat(2,0)) * s;
		ret(2) = (mat(1,0) - mat(0,1)) * s;
	}
	else
	{
		int i = 0;
		if(mat(1,1) > mat(0,0)) i = 1;
		if(mat(2,2) > mat(i,i)) i = 2;
		switch(i)
		{
		case 0:
			s = sqrt((mat(0,0) - (mat(1,1) + mat(2,2))) + 1);
			ret(0) = 0.5 * s;
			s = 0.5 / s;
			ret(1) = (mat(0,1) + mat(1,0)) * s;
			ret(2) = (mat(2,0) + mat(0,2)) * s;
			ret(3) = (mat(2,1) - mat(1,2)) * s;
			break;
		case 1:
			s = sqrt((mat(1,1) - (mat(2,2) + mat(0,0))) + 1);
			ret(1) = 0.5 * s;
			s = 0.5 / s;
			ret(2) = (mat(1,2) + mat(2,1)) * s;
			ret(0) = (mat(0,1) + mat(1,0)) * s;
			ret(3) = (mat(0,2) - mat(2,0)) * s;
			break;
		case 2:
			s = sqrt((mat(2,2) - (mat(0,0) + mat(1,1))) + 1);
			ret(2) = 0.5 * s;
			s = 0.5 / s;
			ret(0) = (mat(2,0) + mat(0,2)) * s;
			ret(1) = (mat(1,2) + mat(2,1)) * s;
			ret(3) = (mat(1,0) - mat(0,1)) * s;
			break;
		}
	}
#else
	double e0, e1, e2, e3, temp, ee, e0e1, e0e2, e0e3;
	ee = (mat(0,0) + mat(1,1) + mat(2,2) + 1) / 4;
	if(ee < 0) ee = 0;
	e0 = sqrt(ee);
	ret.Ang() = e0;
	if(e0 < tiny)
	{
		temp = ee - ((mat(1,1) + mat(2,2)) / 2);
		if(temp < 0) temp = 0;
		e1 = sqrt(temp);
		e0e1 = mat(2,1) - mat(1,2);
		if(e0e1 < 0) e1 *= -1.0;
		ret(0) = e1;
		if(e1 < tiny)
		{
			temp = ee - ((mat(2,2) + mat(0,0)) / 2);
			if(temp < 0) temp = 0;
			e2 = sqrt(temp);
			e0e2 = mat(0,2) - mat(2,0);
			if(e0e2 < 0) e2 *= -1.0;
			ret(1) = e2;
			if(e2 < tiny)
			{
				temp = ee  - ((mat(0,0) + mat(1,1)) / 2);
				if(temp < 0) temp = 0;
				e3 = sqrt(temp);
				e0e3 = mat(1,0) - mat(0,1);
				if(e0e3 < 0) e3 *= -1.0;
				ret(2) = e3;
			}
			else
			{
				temp = 4 * e2;
				ret(2) = (mat(1,2) + mat(2,1)) / temp;
			}
		}
		else
		{
			temp = 4 * e1;
			ret(1) = (mat(0,1) + mat(1,0)) / temp;
			ret(2) = (mat(0,2) + mat(2,0)) / temp;
		}
	}
	else
	{
		temp = 4 * e0;
		ret(0) = (mat(2,1) - mat(1,2)) / temp;
		ret(1) = (mat(0,2) - mat(2,0)) / temp;
		ret(2) = (mat(1,0) - mat(0,1)) / temp;
	}
#endif
	ret.unit();
	return ret;
}

fMat33 ep2mat(const fEulerPara& _epara)
{
	fMat33 ret;
	fEulerPara epara(_epara);
	double ex = epara(0), ey = epara(1), ez = epara(2), e = epara(3);
	double ee = e * e, exx = ex * ex, eyy = ey * ey, ezz = ez * ez;
	double exy = ex * ey, eyz = ey * ez, ezx = ez * ex;
	double eex = e * ex, eey = e * ey, eez = e * ez;
	ret(0,0) = exx - eyy - ezz + ee;
	ret(1,1) = - exx + eyy - ezz + ee;
	ret(2,2) = - exx - eyy + ezz + ee;
	ret(0,1) = 2.0 * (exy - eez);
	ret(1,0) = 2.0 * (exy + eez);
	ret(0,2) = 2.0 * (ezx + eey);
	ret(2,0) = 2.0 * (ezx - eey);
	ret(1,2) = 2.0 * (eyz - eex);
	ret(2,1) = 2.0 * (eyz + eex);
	return ret;
}

fEulerPara fEulerPara::operator= (const fEulerPara& ep)
{
	m_vec = ep.m_vec;
	m_scalar = ep.m_scalar;
	return (*this);
}

fEulerPara fEulerPara::operator= (const fMat33& mat)
{
	fMat33 tmp(mat);
//	fEulerPara ep = mat2ep(tmp);
//	(*this) = ep;
	(*this) = mat2ep(tmp);
	return *this;
}

fEulerPara operator * (double d, const fEulerPara& ep)
{
	fEulerPara ret;
	ret.m_vec = d * ep.m_vec;
	ret.m_scalar = d * ep.m_scalar;
	return ret;
}

fEulerPara angvel2epdot(const fEulerPara& _epara, const fVec3& vel)
{
	fEulerPara ret, epara(_epara);
	fMat33 mat(epara(3), -epara(2), epara(1), epara(2), epara(3), -epara(0), -epara(1), epara(0), epara(3));
	ret(3) = - epara.Vec() * vel * 0.5;
	ret.Axis() = mat * vel * 0.5;
	return ret;
}

fVec3 epdot2angvel(const fEulerPara& _epara, const fEulerPara& _edot)
{
	fVec3 ret;
	fEulerPara epara(_epara), edot(_edot);
	fMat33 mat(epara(3), epara(2), -epara(1), -epara(2), epara(3), epara(0), epara(1), -epara(0), epara(3));
	ret = (- edot(3)*epara.Vec() + mat*edot.Vec()) * 2;
	return ret;
}

fVec3 epddot2angacc(const fEulerPara& _e, const fEulerPara& _de, const fEulerPara& _dde)
{
	fVec3 ret;
	fEulerPara e(_e), de(_de), dde(_dde);
	fMat33 mat1(e(3), e(2), -e(1), -e(2), e(3), e(0), e(1), -e(0), e(3));
	fMat33 mat2(de(3), de(2), -de(1), -de(2), de(3), de(0), de(1), -de(0), de(3));
	ret = 2 * (-de(3)*de.Vec() + mat2*de.Vec() - dde(3)*e.Vec() + mat1*dde.Vec());
	return ret;
}

void fEulerPara::unit()
{
	double len = sqrt((*this) * (*this));
	if(len > eps) (*this) /= len;
}

fEulerPara unit(const fEulerPara& ep)
{
	fEulerPara ret = ep;
	ret.unit();
	return ret;
}

fEulerPara operator - (const fEulerPara& _ep)
{
	fEulerPara ret, ep(_ep);
	ret.Ang() = -ep.Ang();
	ret.Axis() = -ep.Axis();
	return ret;
}

double fEulerPara::rotation(const fEulerPara& ep0, const fVec3& s)
{
	double y = ep0.m_vec * s;
	double x = ep0.m_scalar;
	return atan2(y, x);
}

