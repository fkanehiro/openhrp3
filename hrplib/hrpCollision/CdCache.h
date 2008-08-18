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

/** @file CollisionDetector/server/CdCache.h
 *
 * This file defines classes that represent relationship between strings to Opcode objects
 *
 * @version 0.2
 * @date 2002/02/28
 * @version 0.3 support OPCODE
 * @date 2006/02/28
 * @version 0.4
 * @date 2006/06/30
 *
 */

#ifndef __CDCACHE_H__
#define __CDCACHE_H__

#include <string>
#include <vector>
#include <map>

#include "utilities.h"
#include "Opcode.h"
using namespace std;


class CdModelSet
{
public:
	HRP_COLLISION_EXPORT CdModelSet();
	HRP_COLLISION_EXPORT ~CdModelSet();
	HRP_COLLISION_EXPORT void addTriangle(const double *p1, const double *p2, const double *p3);
	HRP_COLLISION_EXPORT void endModel();
	Opcode::Model *model_[2];
	int linkIndex; // index in a model file (and the model loader)
private:
	udword trisCount_;
	udword pointsCount_;
	udword trisCountAlloced_;
	udword pointsCountAlloced_;
	IceMaths::IndexedTriangle *tris_;
	IceMaths::Point *points_;
	Opcode::MeshInterface *iMesh_;
	Opcode::OPCODECREATE  OPCC_;
};

class CdModelCache
{
public:
	HRP_COLLISION_EXPORT CdModelCache();
	HRP_COLLISION_EXPORT ~CdModelCache();
	HRP_COLLISION_EXPORT int addModel(const char* name,CdModelSet* obj);
	HRP_COLLISION_EXPORT int removeModel(const char* name);
	HRP_COLLISION_EXPORT CdModelSet* getModel(const char* name);
	HRP_COLLISION_EXPORT int exist(const char* name);
	HRP_COLLISION_EXPORT vector<string> getNameList();
private:
	map<string,CdModelSet*> map_;
};

class CdCharCache
{
public:
	HRP_COLLISION_EXPORT CdCharCache();
	HRP_COLLISION_EXPORT ~CdCharCache();
	HRP_COLLISION_EXPORT int addChar(const char* name,CdModelCache* obj);
	HRP_COLLISION_EXPORT int removeChar(const char* name);
	HRP_COLLISION_EXPORT int removeAllChar();
	HRP_COLLISION_EXPORT CdModelCache* getChar(const char* name);
	HRP_COLLISION_EXPORT int exist(const char* name);
private:
	map<string,CdModelCache*> map_;
};

#endif
