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

#ifndef HRPMODEL_MODEL_LOADER_UTIL_H_INCLUDED
#define HRPMODEL_MODEL_LOADER_UTIL_H_INCLUDED

#ifdef __WIN32__
#pragma warning(disable:4996)
#endif

#include "Body.h"
#include <hrpCorba/ORBwrap.h>
#include <hrpCorba/ModelLoader.hh>
#include <string>
#include <sstream>

namespace hrp
{
    HRPMODEL_API bool loadBodyFromBodyInfo(BodyPtr body, OpenHRP::BodyInfo_ptr bodyInfo, bool loadGeometryForCollisionDetection = false);
    HRPMODEL_API OpenHRP::BodyInfo_var loadBodyInfo(const char* url, int& argc, char* argv[]);
    HRPMODEL_API OpenHRP::BodyInfo_var loadBodyInfo(const char* url, CORBA_ORB_var orb);
    HRPMODEL_API OpenHRP::BodyInfo_var loadBodyInfo(const char* url, CosNaming::NamingContext_var cxt);
    HRPMODEL_API bool loadBodyFromModelLoader(BodyPtr body, const char* url, CORBA_ORB_var orb, bool loadGeometryForCollisionDetection = false);
    HRPMODEL_API bool loadBodyFromModelLoader(BodyPtr body, const char* url, CosNaming::NamingContext_var cxt,  bool loadGeometryForCollisionDetection = false);
    HRPMODEL_API bool loadBodyFromModelLoader(BodyPtr body, const char* url, int& argc, char* argv[],  bool loadGeometryForCollisionDetection = false);
    OpenHRP::ModelLoader_var getModelLoader(CosNaming::NamingContext_var cxt);
};


#endif
