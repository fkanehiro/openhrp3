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
#include <hrpCorba/CollisionDetector.hh>
#include <hrpCorba/ModelLoader.hh>
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

    virtual void registerCharacter(const char* name,	BodyInfo_ptr bodyInfo);

    virtual void addCollisionPair(const LinkPair& colPair);


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
    
    virtual void queryDistanceForDefinedPairs(
        const CharacterPositionSequence& characterPositions,
        DistanceSequence_out distances
        );


    virtual void queryDistanceForGivenPairs(
        const LinkPairSequence& checkPairs,
        const CharacterPositionSequence& characterPositions,
        DistanceSequence_out distances
        );
    
    virtual CORBA::Double queryDistanceWithRay(
                                               const DblArray3 point,
                                               const DblArray3 dir
                                               );

    virtual DblSequence* scanDistanceWithRay(const DblArray3 p, const DblArray9 R, CORBA::Double step, CORBA::Double range);

private:

    CORBA_ORB_var orb;
        
    typedef map<string, ColdetBodyPtr> StringToColdetBodyMap;

    // Existing ColdetBodies are used for sharing their ColdetModels
    StringToColdetBodyMap bodyInfoToColdetBodyMap;

    StringToColdetBodyMap nameToColdetBodyMap;

    class ColdetModelPairEx : public ColdetModelPair
    {
    public:
        ColdetModelPairEx(ColdetBodyPtr& body0, ColdetModelPtr& link0, ColdetBodyPtr& body1, ColdetModelPtr& link1, double tolerance=0) :
            ColdetModelPair(link0, link1, tolerance),
            body0(body0),
            body1(body1)
            { }
        ColdetBodyPtr body0;
        ColdetBodyPtr body1;
        double tolerance;
    };

    vector<ColdetModelPairEx> coldetModelPairs;

    void addCollisionPairSub(const LinkPair& linkPair, vector<ColdetModelPairEx>& io_coldetPairs);
    void updateAllLinkPositions(const CharacterPositionSequence& characterPositions);
    bool detectAllCollisions(vector<ColdetModelPairEx>& coldetPairs, CollisionSequence_out& out_collisions);
    bool detectCollisionsOfLinkPair(
        ColdetModelPairEx& coldetPair, CollisionPointSequence& out_collisionPoints, const bool addCollisionPoints);
    bool detectIntersectionOfLinkPair(ColdetModelPairEx& coldetPair);
    bool detectCollidedLinkPairs(
        vector<ColdetModelPairEx>& coldetPairs, LinkPairSequence_out& out_collidedPairs, const bool checkAll);
    bool detectIntersectingLinkPairs(
        vector<ColdetModelPairEx>& coldetPairs, LinkPairSequence_out& out_collidedPairs, const bool checkAll);
    void computeDistances(
        vector<ColdetModelPairEx>& coldetPairs, DistanceSequence_out& out_distances);
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
