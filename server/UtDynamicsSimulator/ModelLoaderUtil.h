// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-
#ifndef MODEL_LOADER_UTIL_H_INCLUDED
#define MODEL_LOADER_UTIL_H_INCLUDED

#include <string>
#include <sstream>

#include <ModelLoader.h>
#include "ORBwrap.h"
#include "World.h"
#include "hrpModelExportDef.h"

class World;

namespace OpenHRP
{
	HRPMODEL_EXPORT int loadBodyFromBodyInfo(World* world, const char* name, BodyInfo_ptr bodyInfo);
	HRPMODEL_EXPORT int loadBodyFromModelLoader(World* world, const char* name,const char *url, CORBA_ORB_var orb);
	HRPMODEL_EXPORT int loadBodyFromModelLoader(World* world, const char* name, const char *url, CosNaming::NamingContext_var cxt);
	HRPMODEL_EXPORT int loadBodyFromModelLoader(World* world, const char* name, const char *url, int argc, char *argv[]);
	HRPMODEL_EXPORT int loadBodyFromModelLoader(World* world, const char* name, const char *url, std::istringstream& strm);
};


#endif
