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
#ifndef MODEL_LOADER_UTIL_H_INCLUDED
#define MODEL_LOADER_UTIL_H_INCLUDED

#include <string>
#include <sstream>

#include <ModelLoader.h>
#include "ORBwrap.h"
#include "Body.h"
#include "hrpModelExportDef.h"

namespace OpenHRP
{
	HRPMODEL_EXPORT BodyPtr loadBodyFromBodyInfo(BodyInfo_ptr bodyInfo);
	HRPMODEL_EXPORT BodyPtr loadBodyFromModelLoader(const char *url, CORBA_ORB_var orb);
	HRPMODEL_EXPORT BodyPtr loadBodyFromModelLoader(const char *url, CosNaming::NamingContext_var cxt);
	HRPMODEL_EXPORT BodyPtr loadBodyFromModelLoader(const char *url, int argc, char *argv[]);
	HRPMODEL_EXPORT BodyPtr loadBodyFromModelLoader(const char *url, std::istringstream& strm);
};


#endif
