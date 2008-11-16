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
   @file CollisionDetector/server/CollisionDetector_impl.cpp
   Implementation of CollisionDetector_impl and CollisionDetectorFactory_impl
   @author Shin'ichiro Nakaoka
*/

#include "CollisionDetector_impl.h"
#include <hrpCollision/ColdetModel.h>
#include <iostream>
#include <string>


using namespace std;
using namespace hrp;


CollisionDetector_impl::CollisionDetector_impl(CORBA_ORB_ptr orb)
    : orb(CORBA_ORB::_duplicate(orb))
{

}


CollisionDetector_impl::~CollisionDetector_impl()
{

}


void CollisionDetector_impl::destroy()
{
    PortableServer::POA_var poa = _default_POA();
    PortableServer::ObjectId_var id = poa->servant_to_id(this);
    poa->deactivate_object(id);
}


void CollisionDetector_impl::registerCharacter(const char* name,	BodyInfo_ptr bodyInfo)
{
    cout << "adding " << name << " ";
    ColdetBodyPtr coldetBody;
	
    string bodyInfoId(orb->object_to_string(bodyInfo));

    // test
    cout << "BodyInfo CORBA ID of " << name << " is " << bodyInfoId << endl;
	
    StringToColdetBodyMap::iterator it = bodyInfoToColdetBodyMap.find(bodyInfoId);
    if(it != bodyInfoToColdetBodyMap.end()){
        coldetBody.reset(new ColdetBody(*it->second));
    } else {
        coldetBody.reset(new ColdetBody(bodyInfo));
    }

    it = nameToColdetBodyMap.find(name);
    if(it != nameToColdetBodyMap.end()){
        cout << "\n";
        cout << "The model of the name " << name;
        cout << " has already been registered. It is replaced." << endl;
        nameToColdetBodyMap[name] = coldetBody;
    } else {
        nameToColdetBodyMap.insert(it, make_pair(name, coldetBody));
        cout << " is ok !" << endl;
    }
}


void CollisionDetector_impl::addCollisionPair
(const LinkPair& linkPair, CORBA::Boolean convexsize1, CORBA::Boolean convexsize2)
{
    addCollisionPairSub(linkPair, coldetModelPairs);
}


void CollisionDetector_impl::addCollisionPairSub
(const LinkPair& linkPair, vector<ColdetModelPairEx>& io_coldetPairs)
{
    const char* bodyName[2];
    bodyName[0] = linkPair.charName1;
    bodyName[1] = linkPair.charName2;
	
    const char* linkName[2];
    linkName[0] = linkPair.linkName1;
    linkName[1] = linkPair.linkName2;

    bool notFound = false;
    ColdetBodyPtr coldetBody[2];
    ColdetModelPtr coldetModel[2];

    for(int i=0; i < 2; ++i){
        StringToColdetBodyMap::iterator it = nameToColdetBodyMap.find(bodyName[i]);
        if(it == nameToColdetBodyMap.end()){
            cout << "CollisionDetector::addCollisionPair : Body ";
            cout << bodyName[i] << " is not found." << endl;
            notFound = true;
        } else {
            coldetBody[i] = it->second;
            coldetModel[i] = coldetBody[i]->linkColdetModel(linkName[i]);
            if(!coldetModel[i]){
                cout << "CollisionDetector::addCollisionPair : Link ";
                cout << linkName[i] << " is not found." << endl;
                notFound = true;
            }
        }
    }

    if(!notFound){
        io_coldetPairs.push_back(ColdetModelPairEx(coldetBody[0], coldetModel[0], coldetBody[1], coldetModel[1]));
    }
}	


CORBA::Boolean CollisionDetector_impl::queryContactDeterminationForDefinedPairs
(const CharacterPositionSequence& characterPositions, CollisionSequence_out out_collisions)
{
    updateAllLinkPositions(characterPositions);
    return detectAllCollisions(coldetModelPairs, out_collisions);
}


CORBA::Boolean CollisionDetector_impl::queryContactDeterminationForGivenPairs
(const LinkPairSequence& checkPairs,
 const CharacterPositionSequence& characterPositions,
 CollisionSequence_out out_collisions)
{
    updateAllLinkPositions(characterPositions);

    vector<ColdetModelPairEx> tmpColdetPairs;
	
    for(unsigned int i=0; i < checkPairs.length(); ++i){
        const LinkPair& linkPair = checkPairs[i];
        addCollisionPairSub(linkPair, tmpColdetPairs);
    }

    return detectAllCollisions(tmpColdetPairs, out_collisions);
}


void CollisionDetector_impl::updateAllLinkPositions
(const CharacterPositionSequence& characterPositions)
{
    for(unsigned int i=0; i < characterPositions.length(); i++){
        const CharacterPosition& characterPosition = characterPositions[i];
        const string bodyName(characterPosition.characterName);
        StringToColdetBodyMap::iterator it = nameToColdetBodyMap.find(bodyName);
        if(it != nameToColdetBodyMap.end()){
            ColdetBodyPtr& coldetBody = it->second;
            coldetBody->setLinkPositions(characterPosition.linkPositions);
        }
    }
}


bool CollisionDetector_impl::detectAllCollisions
(vector<ColdetModelPairEx>& coldetPairs, CollisionSequence_out& out_collisions)
{
    bool detected = false;
    const int numColdetPairs = coldetPairs.size();
    out_collisions = new CollisionSequence;
    out_collisions->length(numColdetPairs);
	
    for(CORBA::ULong i=0; i < numColdetPairs; ++i){

        ColdetModelPairEx& coldetPair = coldetPairs[i];
        Collision& collision = out_collisions[i];

        if(detectCollisionsOfLinkPair(coldetPair, collision.points, true)){
            detected = true;
        }
		
        collision.pair.charName1 = CORBA::string_dup(coldetPair.body0->name());
        collision.pair.linkName1 = CORBA::string_dup(coldetPair.model0()->name());
        collision.pair.charName2 = CORBA::string_dup(coldetPair.body1->name());
        collision.pair.linkName2 = CORBA::string_dup(coldetPair.model1()->name());
    }

    return detected;
}


bool CollisionDetector_impl::detectCollisionsOfLinkPair
(ColdetModelPairEx& coldetPair, CollisionPointSequence& out_collisionPoints, const bool addCollisionPoints)
{
    bool detected = false;
	
    collision_data* cdata = coldetPair.detectCollisions();

    int npoints = 0;
    for(int i=0; i < cdContactsCount; i++) {
        for(int j=0; j < cdata[i].num_of_i_points; j++){
            if(cdata[i].i_point_new[j]){
                npoints ++;
            }
        }
    }
    if(npoints > 0){
        detected = true;
        if(addCollisionPoints){
            out_collisionPoints.length(npoints);
            int index = 0;
            for(int i=0; i < cdContactsCount; i++) {
                collision_data& cd = cdata[i];
                for(int j=0; j < cd.num_of_i_points; j++){
                    if (cd.i_point_new[j]){
                        CollisionPoint& point = out_collisionPoints[index];
                        for(int k=0; k < 3; k++){
                            point.position[k] = cd.i_points[j][k];
                        }
                        for(int k=0; k < 3; k++){
                            point.normal[k] = cd.n_vector[k];
                        }
                        point.idepth = cd.depth;
                        index++;
                    }
                }
            }
        }
    }
	
    return detected;
}


CORBA::Boolean CollisionDetector_impl::queryIntersectionForDefinedPairs
(
    CORBA::Boolean checkAll,
    const CharacterPositionSequence& characterPositions,
    LinkPairSequence_out out_collidedPairs
    )
{
    updateAllLinkPositions(characterPositions);
    return detectCollidedLinkPairs(coldetModelPairs, out_collidedPairs, checkAll);
}


CORBA::Boolean CollisionDetector_impl::queryIntersectionForGivenPairs
(
    CORBA::Boolean checkAll,
    const LinkPairSequence& checkPairs,
    const CharacterPositionSequence& characterPositions,
    LinkPairSequence_out out_collidedPairs
    )
{
    updateAllLinkPositions(characterPositions);

    vector<ColdetModelPairEx> tmpColdetPairs;
	
    for(unsigned int i=0; i < checkPairs.length(); ++i){
        const LinkPair& linkPair = checkPairs[i];
        addCollisionPairSub(linkPair, tmpColdetPairs);
    }

    return detectCollidedLinkPairs(tmpColdetPairs, out_collidedPairs, checkAll);
}


bool CollisionDetector_impl::detectCollidedLinkPairs
(vector<ColdetModelPairEx>& coldetPairs, LinkPairSequence_out& out_collidedPairs, const bool checkAll)
{
    CollisionPointSequence dummy;
	
    bool detected = false;
	
    vector<int> collidedPairIndices;
    collidedPairIndices.reserve(coldetPairs.size());

    for(unsigned int i=0; i < coldetPairs.size(); ++i){
        if(detectCollisionsOfLinkPair(coldetPairs[i], dummy, false)){
            detected = true;
            collidedPairIndices.push_back(i);
            if(!checkAll){
                break;
            }
        }
    }

    out_collidedPairs = new LinkPairSequence();
    out_collidedPairs->length(collidedPairIndices.size());

    for(CORBA::ULong i=0; i < collidedPairIndices.size(); ++i){
        int pairIndex = collidedPairIndices[i];
        ColdetModelPairEx& coldetPair = coldetPairs[pairIndex];
        LinkPair& linkPair = out_collidedPairs[i];
        linkPair.charName1 = CORBA::string_dup(coldetPair.body0->name());
        linkPair.linkName1 = CORBA::string_dup(coldetPair.model0()->name());
        linkPair.charName2 = CORBA::string_dup(coldetPair.body1->name());
        linkPair.linkName2 = CORBA::string_dup(coldetPair.model1()->name());
    }

    return detected;
}


CollisionDetectorFactory_impl::CollisionDetectorFactory_impl(CORBA_ORB_ptr orb)
    : orb(CORBA_ORB::_duplicate(orb))
{
    
}


CollisionDetectorFactory_impl::~CollisionDetectorFactory_impl()
{
    PortableServer::POA_var poa = _default_POA();
    PortableServer::ObjectId_var id = poa->servant_to_id(this);
    poa->deactivate_object(id);
}


CollisionDetector_ptr CollisionDetectorFactory_impl::create()
{
    CollisionDetector_impl* collisionDetector = new CollisionDetector_impl(orb);
    PortableServer::ServantBase_var collisionDetectorrServant = collisionDetector;
    PortableServer::POA_var poa = _default_POA();
    PortableServer::ObjectId_var id = poa->activate_object(collisionDetector);
    return collisionDetector->_this();
}


void CollisionDetectorFactory_impl::shutdown()
{
    orb->shutdown(false);
}
