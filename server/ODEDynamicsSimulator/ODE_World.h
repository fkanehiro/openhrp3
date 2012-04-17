/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

#ifndef ODE_WORLD_H_INCLUDED
#define ODE_WORLD_H_INCLUDED

#include <ode/ode.h>
#include <hrpCorba/ModelLoader.hh>
#include <hrpModel/World.h>
#include <hrpModel/Body.h>
#include <hrpModel/ForwardDynamics.h>
#include <hrpUtil/Eigen4d.h>
#include <string>
#include <vector>

#include "ODE_Link.h"

static const bool USE_QUICKSTEP=true;
static const int QUICKSTEP_NUM_ITERATIONS = 20;

#ifdef dDOUBLE
static const dReal CFM = 10e-11;
#else
static const dReal CFM = 10e-6;
#endif
static const dReal ERP = 0.2;
static const dReal CONTACT_MAX_CORRECTING_VEL = dInfinity;
static const dReal CONTACT_SURFACE_LAYER = 0.0;

static const int COLLISION_MAX_POINT = 100;

static const int SURFACE_MODE =   0
//                                | dContactMu2
//                                | dContactFDir1
                                | dContactBounce
//                                | dContactSoftERP
//                                | ContactSoftCFM
//                                | dContactMotion1
//                                | dContactMotion2
//                                | dContactMotionN
//                                | dContactSlip1
//                                | dContactSlip2
//                                | dContactApprox0
//                                | dContactApprox1_1
//                                | dContactApprox1_2
//                                | dContactApprox1
                                ;
static const dReal SURFACE_MU = dInfinity;
static const dReal SURFACE_MU2 = 0.0;
static const dReal SURFACE_BOUNCE = 0.0;
static const dReal SURFACE_BOUNCE_VEL = 0.0;
static const dReal SURFACE_SOFT_ERP = 0.0;
static const dReal SURFACE_SOFT_CFM = 0.0;
static const dReal SURFACE_MOTION1 = 0.0;
static const dReal SURFACE_MOTION2 = 0.0;
static const dReal SURFACE_SLIP1 = 0.0;
static const dReal SURFACE_SLIP2 = 0.0;
static const dReal CONTACT_FDIR1_X = 0.0;
static const dReal CONTACT_FDIR1_Y = 0.0;
static const dReal CONTACT_FDIR1_Z = 0.0;

class  ODE_World : public hrp::WorldBase
    {
    public:
        ODE_World();
        ~ODE_World();

        /**
           @brief set gravity acceleration
           @param g gravity acceleration[m/s^2]
        */
        void setGravityAcceleration(const dVector3& gravity);

        /**
           @brief get gravity acceleration
           @return gravity accleration
        */
        void getGravityAcceleration(dVector3& gravity);

                /**
           @brief add body to this world
           @param body
           @return index of the body
           @note This must be called before initialize() is called.
        */
        void addBody(OpenHRP::BodyInfo_ptr body, const char *name);
	
        /**
           @brief initialize this world. This must be called after all bodies are registered.
         */
        void initialize();

        /**
           @brief compute forward dynamics and update current state
         */
        void calcNextState(OpenHRP::CollisionSequence& corbaCollisionSequence);

        void clearExternalForces();

        void useInternalCollisionDetector(bool use){ 
            useInternalCollisionDetector_ = use;
        };
    
        void addCollisionPair(OpenHRP::LinkPair& linkPair);

        dWorldID getWorldID() { return worldId; }
        dSpaceID getSpaceID() { return spaceId; }
        dJointGroupID getJointGroupID() { return contactgroupId; }

        OpenHRP::CollisionSequence    collisions;

        struct LinkPair{
            dBodyID bodyId1;
            dBodyID bodyId2;
        };
        typedef std::vector<LinkPair> LinkPairArray;
        LinkPairArray linkPairs;

    private:
        dWorldID worldId;
        dSpaceID spaceId;
        dJointGroupID contactgroupId;

        bool useInternalCollisionDetector_;

        void updateSensors();
};

static void ODE_collideCallback(void* data, dGeomID o1, dGeomID o2);

class ODE_ForwardDynamics : public hrp::ForwardDynamics{
    public :
        ODE_ForwardDynamics(hrp::BodyPtr body);

        virtual void initialize();
        virtual void calcNextState();
        void updateSensors();

    private :
        void updateForceSensor(ODE_ForceSensor* sensor);
};
typedef boost::shared_ptr<ODE_ForwardDynamics> ODE_ForwardDynamicsPtr;

#endif
