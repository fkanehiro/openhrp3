// -*- mode: c++;
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
   @file CollisionDetector/server/CollisionDetector_impl.h
   Implementation of CollisionDetector_impl and CollisionDetectorFactory_impl
   @author Shin'ichiro Nakaoka
*/

#ifndef OPENHRP_COLISIONDETECTOR_IMPL_H_INCLUDED
#define OPENHRP_COLISIONDETECTOR_IMPL_H_INCLUDED

#include <map>
#include <vector>
#include <hrpCorba/ORBwrap.h>
#include <hrpCorba/CollisionDetector.h>
#include <hrpCorba/ModelLoader.h>
#include <hrpCollision/ColdetModelPair.h>
#include "ColdetBody.h"

using namespace std;
using namespace OpenHRP;


class CollisionDetector_impl : virtual public POA_OpenHRP::CollisionDetector,
                               virtual public PortableServer::RefCountServantBase
{
public:

    CollisionDetector_impl(CORBA_ORB_ptr orb);

    ~CollisionDetector_impl();

    virtual void destroy();

    virtual void addModel(const char* name,	BodyInfo_ptr bodyInfo);

    virtual void addCollisionPair(const LinkPair& colPair, CORBA::Boolean convexsize1, CORBA::Boolean convexsize2);


    virtual CORBA::Boolean queryIntersectionForDefinedPairs(
        CORBA::Boolean checkAll,
        const CharacterPositionSequence& characterPositions,
        LinkPairSequence_out collidedPairs
        );


    virtual CORBA::Boolean queryIntersectionForGivenPairs(
        CORBA::Boolean checkAll,
        const LinkPairSequence& checkPairs,
        const CharacterPositionSequence& characterPositions,
        LinkPairSequence_out collidedPairs
        );

    virtual CORBA::Boolean queryContactDeterminationForDefinedPairs(
        const CharacterPositionSequence& characterPositions,
        CollisionSequence_out collisions
        );

    virtual CORBA::Boolean queryContactDeterminationForGivenPairs(
        const LinkPairSequence& checkPairs,
        const CharacterPositionSequence& characterPositions,
        CollisionSequence_out collisions
        );
    
private:

    CORBA_ORB_var orb;
        
    typedef map<string, ColdetBodyPtr> StringToColdetBodyMap;

    // Existing ColdetBodies are used for sharing their ColdetModels
    StringToColdetBodyMap bodyInfoToColdetBodyMap;

    StringToColdetBodyMap nameToColdetBodyMap;

    class ColdetModelPairEx : public ColdetModelPair
    {
    public:
        ColdetModelPairEx(ColdetBodyPtr body0, ColdetModelPtr link0, ColdetBodyPtr body1, ColdetModelPtr link1) :
            ColdetModelPair(link0, link1),
            body0(body0),
            body1(body1)
            { }
        ColdetBodyPtr body0;
        ColdetBodyPtr body1;
    };

    vector<ColdetModelPairEx> coldetModelPairs;

    void addCollisionPairSub(const LinkPair& linkPair, vector<ColdetModelPairEx>& io_coldetPairs);
    void updateAllLinkPositions(const CharacterPositionSequence& characterPositions);
    bool detectAllCollisions(vector<ColdetModelPairEx>& coldetPairs, CollisionSequence_out& out_collisions);
    bool detectCollisionsOfLinkPair(
        ColdetModelPairEx& coldetPair, CollisionPointSequence& out_collisionPoints, bool addCollisionPoints);
    bool detectCollidedLinkPairs(
        vector<ColdetModelPairEx>& coldetPairs, LinkPairSequence_out& out_collidedPairs, bool checkAll);

};


class CollisionDetectorFactory_impl
    : virtual public POA_OpenHRP::CollisionDetectorFactory,
      virtual public PortableServer::RefCountServantBase
{
public:

    CollisionDetectorFactory_impl(CORBA_ORB_ptr orb);

    ~CollisionDetectorFactory_impl();

    CollisionDetector_ptr create();

    void shutdown();

private:
    CORBA_ORB_var orb;
};

#endif
