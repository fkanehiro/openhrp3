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
/** @file CollisionDetector/server/CdScene.h
 *
 * This file defines classes that represent relationship between current model states to OPCODE objects
 *
 * @version 0.2
 * @date 2002/02/28
 * @version 0.3  support OPCODE
 * @date 2006/02/28
 * @version 0.4  
 * @date 2006/06/30
 *
 */
#ifndef CD_SCENE_H
#define CD_SCENE_H

#include <string>
#include <vector>
#include <map>
#include "CdCache.h"
#include "CdWrapper.h"

class CdChar;

class CdJoint
{
public:
	HRP_COLLISION_EXPORT CdJoint(CdModelSet* model, const char* name, CdChar* parent);
	HRP_COLLISION_EXPORT ~CdJoint();
        
	void setTransform(const double t[3],const double q[4]);
	double translation_[3];
	double rotation_[3][3];
        
	string name_;
	CdModelSet* model_;
	CdChar* parent_;
};

class CdCheckPair
{
public:
	HRP_COLLISION_EXPORT CdCheckPair(CdJoint* j1,CdJoint* j2);
	HRP_COLLISION_EXPORT ~CdCheckPair();
	HRP_COLLISION_EXPORT int collide(int* num, collision_data** cPair, int flag = CD_ALL_CONTACTS);
	HRP_COLLISION_EXPORT int collide(int* num, int flag = CD_ALL_CONTACTS);
        
	CdJoint* joint_[2];
};

class CdChar
{
public:
	HRP_COLLISION_EXPORT CdChar(CdModelCache* model, const char* name);
	HRP_COLLISION_EXPORT ~CdChar();
	HRP_COLLISION_EXPORT CdJoint* getJoint(const char* name);
	HRP_COLLISION_EXPORT CdJoint* getJoint(int linkIndex);
	string name_;
private:
	vector<CdJoint*> linkIndexToCdJoint;
	map<string, CdJoint*> map_;
};


class CdScene
{
public:
	HRP_COLLISION_EXPORT CdScene();
	HRP_COLLISION_EXPORT ~CdScene();
	HRP_COLLISION_EXPORT int addChar(const char* name, CdChar* obj);
	HRP_COLLISION_EXPORT int removeChar(const char* name);
	HRP_COLLISION_EXPORT CdChar* getChar(const char* name);
	int exist(const char* name);
	HRP_COLLISION_EXPORT void addCheckPair(CdCheckPair* cPair);
        
	HRP_COLLISION_EXPORT int getNumCheckPairs();
	HRP_COLLISION_EXPORT CdCheckPair* getCheckPair(int i);
        
private:
	map<string,CdChar*> map_;
	vector<CdCheckPair*> pairs_;
};

#endif
