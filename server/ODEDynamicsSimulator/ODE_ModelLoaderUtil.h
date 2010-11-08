/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

#ifndef ODE_MODEL_LOADER_UTIL_H_INCLUDED
#define ODE_MODEL_LOADER_UTIL_H_INCLUDED

#include <ode/ode.h>
#include "ODE_World.h"
#include "ODE_Link.h"

#include <hrpCorba/ORBwrap.h>
#include <hrpCorba/ModelLoader.hh>
#include <hrpUtil/Tvmet4d.h>
#include <hrpModel/Body.h>
#include <hrpModel/Sensor.h>


bool ODE_loadBodyFromBodyInfo(hrp::BodyPtr body, ODE_World* world, OpenHRP::BodyInfo_ptr bodyInfo );


#endif
