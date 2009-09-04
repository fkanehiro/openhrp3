/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

/**
   @author Shin'ichiro Nakaoka
*/

#ifndef OPENHRP_MODEL_LOADER_UTIL_H_INCLUDED
#define OPENHRP_MODEL_LOADER_UTIL_H_INCLUDED

#pragma warning(disable:4996)

#include <string>
#include <sstream>

#include <hrpCorba/ORBwrap.h>
#include <hrpCorba/ModelLoader.hh>

#include "Body.h"


namespace hrp
{
    HRPMODEL_API BodyPtr loadBodyFromBodyInfo(OpenHRP::BodyInfo_ptr bodyInfo, bool loadGeometryForCollisionDetection = false);
    HRPMODEL_API BodyPtr loadBodyFromModelLoader(const char *url, CORBA_ORB_var orb);
    HRPMODEL_API bool loadBodyFromModelLoader(const char *url, Body *body, CORBA_ORB_var orb);
    HRPMODEL_API BodyPtr loadBodyFromModelLoader(const char *url, CosNaming::NamingContext_var cxt);
    HRPMODEL_API bool loadBodyFromModelLoader(const char *url, hrp::Body *body, CosNaming::NamingContext_var cxt);
    HRPMODEL_API BodyPtr loadBodyFromModelLoader(const char *url, int argc, char *argv[]);
    HRPMODEL_API bool loadBodyFromModelLoader(const char *url, hrp::Body *body, int argc, char *argv[]);
    HRPMODEL_API BodyPtr loadBodyFromModelLoader(const char *url, std::istringstream& strm);
};


#endif
