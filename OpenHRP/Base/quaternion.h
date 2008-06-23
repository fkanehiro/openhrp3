// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-
#ifndef OPENHRP_DQUATERNION_H_INCLUDED
#define OPENHRP_DQUATERNION_H_INCLUDED

#include <boost/math/quaternion.hpp>

#include "tvmet3d.h"
#include "hrpModelExportDef.h"

namespace OpenHRP
{
    typedef boost::math::quaternion<double> dquaternion;

	// These next functions from Boost HSO3.hpp
	HRPMODEL_EXPORT matrix33 rotFromQuaternion(const dquaternion& q);
	HRPMODEL_EXPORT dquaternion quaternionFromRot(const matrix33 & rot, const dquaternion* hint = 0);
};

#endif
