// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-
/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */
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
