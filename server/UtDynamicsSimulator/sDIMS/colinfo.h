/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/**
 * colinfo.h
 * Create: Katsu Yamane, 04.03.05
 */

#ifndef __COLINFO_H__
#define __COLINFO_H__

#include <chain.h>
//#include <tList.h>

class ColPair
{
	friend class ColInfo;
public:
	ColPair(Chain* chain, const char* jointname1, const char* charname1, const char* jointname2, const char* charname2) {
	}
	
	~ColPair() {
	}

	Joint* GetJoint(int i) {
		return joints[i];
	}

private:
	Joint* joints[2];
};

class ColInfo
{
	friend class ColPair;
	friend class ColModel;
public:
	ColInfo() {
		n_pairs = 0;
		n_allocated_pairs = 0;
		n_models = 0;
		n_allocated_models = 0;
		n_total_tri = 0;
		pairs = 0;
		models = 0;
	}
	~ColInfo() {
		if(pairs)
		{
			for(int i=0; i<n_pairs; i++) delete pairs[i];
			delete[] pairs;
		}
		if(models)
		{
			for(int i=0; i<n_models; i++) delete models[i];
			delete[] models;
		}
	}

	/*
	 * add collision pairs by character name
	 */
	int AddCharPairs(const char* char1, const char* char2,
					 Chain* chain, SceneGraph* sg);
	/*
	 * add collision pairs by joint name
	 */
	int AddJointPair(const char* joint1, const char* joint2,
					 Chain* chain, SceneGraph* sg);

	/*
	 * query information
	 */
	int NumPairs() {
		return n_pairs;
	}
	int NumModels() {
		return n_models;
	}
	int NumTriangles() {
		return n_total_tri;
	}
	ColPair* Pair(int i) {
		return pairs[i];
	}
	ColModel* Model(int i) {
		return models[i];
	}
	ColModel* Model(Joint* jref) {
		for(int i=0; i<n_models; i++)
		{
			if(models[i]->joint == jref) return models[i];
		}
		return 0;
	}
	ColPair* Pair(Joint* jref1, Joint* jref2) {
		for(int i=0; i<n_pairs; i++)
		{
			if(pairs[i]->models[0]->joint == jref1 && pairs[i]->models[1]->joint == jref2) return pairs[i];
			else if(pairs[i]->models[1]->joint == jref1 && pairs[i]->models[0]->joint == jref2) return pairs[i];
		}
		return 0;
	}
	ColModel* AddModel(Joint* jref, SceneGraph* sg);

private:
	void allocate_pair(int n_new_alloc) {
		ColPair** tmp = pairs;
		pairs = new ColPair* [n_new_alloc];
		if(tmp)
		{
			for(int i=0; i<n_allocated_pairs; i++) pairs[i] = tmp[i];
			delete[] tmp;
		}
		n_allocated_pairs = n_new_alloc;
	}
	void allocate_model(int n_new_alloc) {
		ColModel** tmp = models;
		models = new ColModel* [n_new_alloc];
		if(tmp)
		{
			for(int i=0; i<n_allocated_models; i++) models[i] = tmp[i];
			delete[] tmp;
		}
		n_allocated_models = n_new_alloc;
	}
	void add_pair(ColPair* p);
	void add_model(ColModel* m);

	int add_char_pairs(Joint* cur, const char* char1, const char* char2, Chain* chain, SceneGraph* sg);
	int add_char_pairs(Joint* j1, Joint* j2, const char* char2, SceneGraph* sg);
	int add_joint_pair(Joint* j1, Joint* j2, SceneGraph* sg);
	
	int n_pairs;
	int n_allocated_pairs;
	ColPair** pairs;

	int n_models;
	int n_allocated_models;
	ColModel** models;
	int n_total_tri;
};


#endif
