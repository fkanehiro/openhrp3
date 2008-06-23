// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-
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
