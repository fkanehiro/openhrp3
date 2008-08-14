/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */

/**
   @author Shin'ichiro Nakaoka
*/

#ifndef OPENHRP_BASE_MODEL_LOADER_UTIL_H_INCLUDED
#define OPENHRP_BASE_MODEL_LOADER_UTIL_H_INCLUDED

#include "hrpModelExportDef.h"

#include <string>
#include <sstream>

#include <OpenHRP/Corba/ORBwrap.h>
#include <OpenHRP/Corba/ModelLoader.h>

#include "Body.h"


namespace hrp
{
    HRPMODEL_EXPORT BodyPtr loadBodyFromBodyInfo(OpenHRP::BodyInfo_ptr bodyInfo);
    HRPMODEL_EXPORT BodyPtr loadBodyFromModelLoader(const char *url, CORBA_ORB_var orb);
    HRPMODEL_EXPORT BodyPtr loadBodyFromModelLoader(const char *url, CosNaming::NamingContext_var cxt);
    HRPMODEL_EXPORT BodyPtr loadBodyFromModelLoader(const char *url, int argc, char *argv[]);
    HRPMODEL_EXPORT BodyPtr loadBodyFromModelLoader(const char *url, std::istringstream& strm);
};


#endif
