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
/** @file CollisionDetector/server/CollisionDetector_impl.cpp
 * Implementation of CollisionDetector_impl and CollisionDetectorFactory_impl
 *
 * @version 0.2
 * @date 2002/01/15
 *
 */

#include "CollisionDetector_impl.h"
#include <OpenHRP/Collision/CdCache.h>
#include <OpenHRP/Util/Tvmet4d.h>

#include <time.h>
#include <iostream>
#include <string>
#include <vector>

using namespace std;
using namespace OpenHRP;

//#define COLLISIONDETECTOR_DEBUG

CollisionDetector_impl::CollisionDetector_impl
(
 CORBA_ORB_ptr        orb,
 CdCharCache*         cache
 ) : orb_(CORBA_ORB::_duplicate(orb))
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetector_impl::CollisionDetector_impl()" << endl;
#endif
    cache_ = cache;
    scene_ = new CdScene();
}

CollisionDetector_impl::~CollisionDetector_impl()
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetector_impl::~CollisionDetector_impl()" << endl;
#endif
    delete scene_;
}

void
CollisionDetector_impl::destroy()
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetector_impl::destroy()" << endl;
#endif
    PortableServer::POA_var poa = _default_POA();
    PortableServer::ObjectId_var id = poa -> servant_to_id(this);
    poa -> deactivate_object(id);
}




void CollisionDetector_impl::addModel(const char* charName,	BodyInfo_ptr bodyInfo)
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetector_impl::addModel() " << charName <<endl;
#endif

    CdModelCache* cachedModel;

    //既にキャッシュに入ってる？
    CORBA::String_var url = bodyInfo->url();

    if(cache_->exist(url)){
        cachedModel = cache_->getChar( url );
    } else {
        cerr << "creating Cd object..." << endl;

        //ロード
        LinkInfoSequence_var links = bodyInfo->links();
		ShapeInfoSequence_var shapes = bodyInfo->shapes();
		AllLinkShapeIndexSequence_var allLinkShapeIndices = bodyInfo->linkShapeIndices(); 
		
        cachedModel = new CdModelCache();

        // ジョイントごとに三角形集合追加
		for(int linkIndex = 0; linkIndex < links->length(); ++linkIndex){
			
			const TransformedShapeIndexSequence& shapeIndices = allLinkShapeIndices[linkIndex];
			
			CdModelSet* modelSet = new CdModelSet();
			modelSet->linkIndex = linkIndex;

			int numTriangles = addTriangleVertices(modelSet, shapeIndices, shapes);

			if(numTriangles > 0){
				modelSet->endModel();
			} else {
				delete modelSet;
				modelSet = 0;
			}
			const char* linkName = links[linkIndex].name;
			cachedModel->addModel(linkName, modelSet);
			cout << linkName << " has "<< numTriangles << " triangles." << endl;
        }
		cache_->addChar(url, cachedModel);
		cout << "finished." << endl;
	}

	scene_->addChar(charName, new CdChar(cachedModel, charName));
}


int CollisionDetector_impl::addTriangleVertices
(CdModelSet* modelSet, const TransformedShapeIndexSequence& shapeIndices, ShapeInfoSequence_var& shapes)
{
	int totalNumTriangles = 0;
	
	for(int i=0; i < shapeIndices.length(); i++){
		const TransformedShapeIndex& tsi = shapeIndices[i];
		short shapeIndex = tsi.shapeIndex;
		const DblArray12& M = tsi.transformMatrix;;
		Matrix44 T;
		T = M[0], M[1], M[2],  M[3],
			M[4], M[5], M[6],  M[7],
			M[8], M[9], M[10], M[11],
			0.0,  0.0,  0.0,   1.0;

		const ShapeInfo& shapeInfo = shapes[shapeIndex];
		const FloatSequence& vertices = shapeInfo.vertices;
		const int numTriangles = shapeInfo.triangles.length() / 3;
		totalNumTriangles += numTriangles;

		for(int triangleIndex=0; triangleIndex < numTriangles; ++triangleIndex){
			Vector4 v[3];
			for(int j=0; j < 3; ++j){
				long vertexIndex = shapeInfo.triangles[triangleIndex * 3 + j];
				int p = vertexIndex * 3;
				v[j] = T * Vector4(vertices[p+0], vertices[p+1], vertices[p+2], 1.0);
			}
			modelSet->addTriangle(v[0].data(), v[1].data(), v[2].data());
		}
	}

	return totalNumTriangles;
}


void CollisionDetector_impl::addCollisionPair(const LinkPair& colPair,
                                              CORBA::Boolean convexsize1,
                                              CORBA::Boolean convexsize2)
{
    const char* charName1 = colPair.charName1;
    const char* charName2 = colPair.charName2;
    const char* jointName1 = colPair.linkName1;
    const char* jointName2 = colPair.linkName2;
    //#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetector_impl::addCollisionPair("
         << charName1 <<  "::" << jointName1 << ", "
         << charName2 <<  "::" << jointName2 << ")" << endl;
    //#endif

	CdJoint* joint1 = 0;
	CdJoint* joint2 = 0;

    CdChar* char1 = scene_->getChar(charName1);
    if (!char1){
        cerr << "CollisionDetector_impl::addCollisionPair : Character not found("
             << charName1 << ")" << endl;
    } else {
	    joint1 = char1->getJoint(jointName1);
    	if (!joint1){
        	cerr << "CollisionDetector_impl::addCollisionPair : Joint not found("
            	 << charName1 << ","
             	<< jointName1 << ")" << endl;
	    }
    }

    CdChar* char2 = scene_->getChar(charName2);
    if (!char2){
        cerr << "CollisionDetector_impl::addCollisionPair : Character not found("
             << charName2 << ")" << endl;
    } else {
    	joint2 = char2->getJoint(jointName2);
    	if (!joint2){
        	cerr << "CollisionDetector_impl::addCollisionPair : Joint not found("
            	 << charName2 << ","
             	<< jointName2 << ")" << endl;
    	}
    }

    static const bool NOT_SKIP_EMPTY_JOINT = true;

    if(NOT_SKIP_EMPTY_JOINT || (joint1 && joint2)){
    	scene_->addCheckPair(new CdCheckPair(joint2, joint1));
    }

}

void CollisionDetector_impl::removeCollisionPair(const LinkPair& colPair)
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetector_impl::removeCollisionPair()" << endl;
#endif
}


//ジョイント位置セット
void CollisionDetector_impl::_setCharacterData
(
 const CharacterPositionSequence& characterPositions
 )
{
    if (joints.size() == 0){ //first time
        for (unsigned int i=0; i < characterPositions.length(); i++){
            const CharacterPosition& characterPosition = characterPositions[i];
            CdChar *rChar = scene_->getChar(characterPosition.characterName);
            if (rChar){
				const LinkPositionSequence& linkPositions = characterPosition.linkPositions;
                for (unsigned int j=0; j < linkPositions.length(); j++){
                    CdJoint *rJoint = rChar->getJoint(j);
                    if (rJoint){
                        joints.push_back(rJoint);
                    }else{
                        joints.push_back(NULL);
                    }
                }
            } else {
                cerr << "CollisionDetector_impl::_setCharacterData : Character not found(" << characterPosition.characterName << ")" << endl;
            }
        }
    }

    //ジョイントの位置姿勢をセット
    vector<CdJoint *>::iterator it = joints.begin();
    for(unsigned int i = 0; i < characterPositions.length(); i++){
		const LinkPositionSequence& linkPositions = characterPositions[i].linkPositions;
        for (unsigned int j=0; j < linkPositions.length(); j++, it++){
			CdJoint* cdJoint = *it;
            if (cdJoint){
                const LinkPosition& linkPosition = linkPositions[j];
				const DblArray3& po = linkPosition.p;
				const DblArray9& Ro = linkPosition.R;
				double* p    = cdJoint->translation_;
				double (*R)[3] = cdJoint->rotation_;
				p[0] = po[0]; p[1] = po[1]; p[2] = po[2];
				R[0][0] = Ro[0]; R[0][1] = Ro[1]; R[0][2] = Ro[2];
				R[1][0] = Ro[3]; R[1][1] = Ro[4]; R[1][2] = Ro[5];
				R[2][0] = Ro[6]; R[2][1] = Ro[7]; R[2][2] = Ro[8];
            }
        }
    }
}

//下請け
int CollisionDetector_impl::_contactIntersection
(
 CdCheckPair* rPair
 )
{
    collision_data* pair;
    int ret;
    int i;

    rPair->collide(&ret,&pair,CD_FIRST_CONTACT);

    // この段階では接触点数分からないので、接触点数をカウントする。
    int npoints = 0;
    for (i = 0; i < ret; ++i) {
        npoints += pair[i].num_of_i_points;
    }
    delete pair;
    return npoints;
}
//下請け
void CollisionDetector_impl::_contactDetermination(CdCheckPair* rPair, Collision&  out_collision)
{
    int ret;

	collision_data *cdata;
    rPair->collide(&ret, &cdata);

    // この段階では接触点数分からないので、接触点数をカウントする。
    int npoints = 0;
    for (int i = 0; i < ret; i++) {
        for (int j = 0; j<cdata[i].num_of_i_points; j++){
            if (cdata[i].i_point_new[j]) npoints ++;
        }
    }
    if (npoints > 0){
        out_collision.points.length(npoints);

        int idx = 0;
        for (int i = 0; i < ret; i++) {
			collision_data& cd = cdata[i];
            for (int j=0; j < cd.num_of_i_points; j++){
                if (cd.i_point_new[j]){
					CollisionPoint& point = out_collision.points[idx];
                    for(int k=0; k < 3; k++){
                        point.position[k] = cd.i_points[j][k];
					}
                    for(int k=0; k < 3; k++){
                        point.normal[k]   = cd.n_vector[k];
                    }
                    point.idepth = cd.depth;
                    idx++;
                }
            }
        }
    }
    if (rPair->joint_[0]){
        out_collision.pair.charName1 = CORBA::string_dup(rPair->joint_[0]->parent_->name_.c_str());    // パス名1
        out_collision.pair.linkName1 = CORBA::string_dup(rPair->joint_[0]->name_.c_str());    // パス名1
    }
    if (rPair->joint_[1]){
        out_collision.pair.charName2 = CORBA::string_dup(rPair->joint_[1]->parent_->name_.c_str());    // パス名2
        out_collision.pair.linkName2 = CORBA::string_dup(rPair->joint_[1]->name_.c_str());    // パス名2
    }
}


CORBA::Boolean
CollisionDetector_impl::queryContactDeterminationForDefinedPairs
(
 const CharacterPositionSequence& characterPositions,
 CollisionSequence_out collisions
 )
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetector_impl::queryContactDeterminationForDefinedPairs()" << endl;
#endif
    _setCharacterData(characterPositions);

    long unsigned int numPair = scene_->getNumCheckPairs();

    collisions = new CollisionSequence;
    collisions->length(numPair);
    bool flag = false;
    for(long unsigned int pCount = 0 ; pCount < numPair ; ++pCount){
        CdCheckPair* rPair = scene_->getCheckPair(pCount);
        _contactDetermination(rPair, collisions[pCount]);
        if (collisions[pCount].points.length() > 0) flag = true;
    }

    return flag;
}

CORBA::Boolean
CollisionDetector_impl::queryContactDeterminationForGivenPairs
(
 const LinkPairSequence& checkPairs,
 const CharacterPositionSequence& characterPositions,
 CollisionSequence_out collisions
 )
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetector_impl::queryContactDeterminationForGivenPairs()" << endl;
#endif

    _setCharacterData(characterPositions);

    vector<CdCheckPair*> rPairSeq;

    //臨時ペア生成
    for(unsigned int i=0;i<checkPairs.length();++i){
        const char* charName1 = checkPairs[i].charName1;
        const char* charName2 = checkPairs[i].charName2;
        const char* jointName1 = checkPairs[i].linkName1;
        const char* jointName2 = checkPairs[i].linkName2;

        CdChar* char1 = scene_->getChar(charName1);
        CdChar* char2 = scene_->getChar(charName2);

        if(char1 && char2){
            CdJoint* joint1 = char1->getJoint(jointName1);
            CdJoint* joint2 = char2->getJoint(jointName2);
            if(joint1 && joint2){
                rPairSeq.push_back(
                                   new CdCheckPair(
												   joint2,
												   joint1
												   )
                                   );
            }
        }

    }

    collisions = new CollisionSequence;
    collisions->length(rPairSeq.size());
    bool flag = false;
    // check
    unsigned long int pCount;
    for(pCount = 0 ; pCount < rPairSeq.size() ; ++pCount){
        CdCheckPair* rPair = rPairSeq[pCount];
        _contactDetermination(rPair,collisions[pCount]);
        if (collisions[pCount].points.length() > 0) flag = true;
    }
    for(pCount = 0 ; pCount < rPairSeq.size() ; ++pCount){
        // delete
        CdCheckPair* rPair = rPairSeq[pCount];
        delete rPair;
    }

    return flag;
}

CORBA::Boolean CollisionDetector_impl::queryIntersectionForDefinedPairs
(
 CORBA::Boolean checkAll,
 const CharacterPositionSequence& characterPositions,
 LinkPairSequence_out collidedPairs
 )
{
    _setCharacterData(characterPositions);

    int numPair = scene_->getNumCheckPairs();


    vector<CdCheckPair*> contactPairs;

    bool flag = false;
    for(int pCount = 0 ; pCount < numPair ; ++pCount){
        CdCheckPair* rPair = scene_->getCheckPair(pCount);
        int ret = _contactIntersection(rPair);
        if (ret){
            contactPairs.push_back(rPair);
        }
        flag = (flag || ret);
        if(!checkAll && flag){
            break;
        }
    }

    collidedPairs = new LinkPairSequence();
    collidedPairs->length(contactPairs.size());

    for(unsigned int i=0;i<contactPairs.size();++i){
        CdCheckPair* rPair = contactPairs[i];
        (*collidedPairs)[i].charName1 = CORBA::string_dup(rPair->joint_[0]->parent_->name_.c_str());
        (*collidedPairs)[i].linkName1 = CORBA::string_dup(rPair->joint_[0]->name_.c_str());
        (*collidedPairs)[i].charName2 = CORBA::string_dup(rPair->joint_[1]->parent_->name_.c_str());
        (*collidedPairs)[i].linkName2 = CORBA::string_dup(rPair->joint_[1]->name_.c_str());
    }

    return flag;
}

CORBA::Boolean CollisionDetector_impl::queryIntersectionForGivenPairs
(
 CORBA::Boolean checkAll,
 const LinkPairSequence& checkPairs,
 const CharacterPositionSequence& characterPositions,
 LinkPairSequence_out collidedPairs
 )
{
#ifdef COLLISIONDETECTOR_DEBUG
    //cerr << "CollisionDetector_impl::queryIntersectionForGivenPairs()" << endl;
#endif
    _setCharacterData(characterPositions);

    vector<CdCheckPair*> rPairSeq;

    //臨時ペア生成
    unsigned int i;
    for(i=0;i<checkPairs.length();++i){
        const char* charName1  = checkPairs[i].charName1;
        const char* charName2  = checkPairs[i].charName2;
        const char* jointName1 = checkPairs[i].linkName1;
        const char* jointName2 = checkPairs[i].linkName2;

        //cerr << checkPairs[i].charName1 << "." <<checkPairs[i].jointName1<< "-";
        //cerr << checkPairs[i].charName2 << "." <<checkPairs[i].jointName2<<endl;

        CdChar* char1 = scene_->getChar(charName1);
        CdChar* char2 = scene_->getChar(charName2);

        if(char1 && char2){
            CdJoint* joint1 = char1->getJoint(jointName1);
            CdJoint* joint2 = char2->getJoint(jointName2);
            if(joint1 && joint2){
                rPairSeq.push_back(
                                   new CdCheckPair(
												   joint2,
												   joint1
												   )
                                   );
            }
            //cerr << "create" ;
        }
        //cerr << endl;

    }


    vector<CdCheckPair*> contactPairs;

    bool flag = false;
    unsigned int pCount;
    for(pCount = 0 ; pCount < rPairSeq.size() ; ++pCount){
        CdCheckPair* rPair = rPairSeq[pCount];
        int ret = _contactIntersection(rPair);
        if (ret){
            contactPairs.push_back(rPair);
            //cerr << "insects! "<< endl;
        }
        flag = (flag || ret);
        if(!checkAll && flag){
            break;
        }
    }

    collidedPairs = new LinkPairSequence();
    collidedPairs->length(contactPairs.size());

    for(i=0;i<contactPairs.size();++i){
        CdCheckPair* rPair = contactPairs[i];
        (*collidedPairs)[i].charName1 = CORBA::string_dup(rPair->joint_[0]->parent_->name_.c_str());
        (*collidedPairs)[i].linkName1 = CORBA::string_dup(rPair->joint_[0]->name_.c_str());
        (*collidedPairs)[i].charName2 = CORBA::string_dup(rPair->joint_[1]->parent_->name_.c_str());
        (*collidedPairs)[i].linkName2 = CORBA::string_dup(rPair->joint_[1]->name_.c_str());
    }

    for(pCount = 0 ; pCount < rPairSeq.size() ; ++pCount){
        // delete
        CdCheckPair* rPair = rPairSeq[pCount];
        delete rPair;
    }

    return flag;

}


void CollisionDetector_impl::clearCache(const char* url)
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetector_impl::clearCache()" << endl;
#endif
    cache_->removeChar(url);
}

void CollisionDetector_impl::clearAllCache()
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetector_impl::clearAllCache()" << endl;
    cache_->removeAllChar();
#endif
}

CollisionDetectorFactory_impl::CollisionDetectorFactory_impl
(
 CORBA_ORB_ptr   orb
 ) : orb_(CORBA_ORB::_duplicate(orb))
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetectorFactory_impl::CollisionDetectorFactory_impl()" << endl;
#endif
    cache_ = new CdCharCache();
}

CollisionDetectorFactory_impl::~CollisionDetectorFactory_impl()
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetectorFactory_impl::~CollisionDetectorFactory_impl()" << endl;
#endif
    PortableServer::POA_var poa = _default_POA();
    PortableServer::ObjectId_var id = poa -> servant_to_id(this);
    poa -> deactivate_object(id);
    delete cache_;
}

CollisionDetector_ptr
CollisionDetectorFactory_impl::create()
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetectorFactory_impl::create()" << endl;
#endif

    CollisionDetector_impl* collisionDetectorImpl = new CollisionDetector_impl(orb_,cache_);

    PortableServer::ServantBase_var collisionDetectorrServant = collisionDetectorImpl;
    PortableServer::POA_var poa_ = _default_POA();
    PortableServer::ObjectId_var id =
        poa_ -> activate_object(collisionDetectorImpl);
    return collisionDetectorImpl -> _this();
}

void CollisionDetectorFactory_impl::shutdown()
{
    orb_->shutdown(false);
}
