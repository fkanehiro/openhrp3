/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

#include "ODE_World.h"
#include "ODE_ModelLoaderUtil.h"

static const dReal DEFAULT_GRAVITY_ACCELERATION = -9.80665;

ODE_World::ODE_World()
{
    dInitODE();
    worldId = dWorldCreate();
    spaceId = dHashSpaceCreate(0);
    contactgroupId = dJointGroupCreate(0);
    
    dWorldSetCFM(worldId, CFM);
    dWorldSetERP(worldId, ERP);
    dWorldSetContactMaxCorrectingVel(worldId, CONTACT_MAX_CORRECTING_VEL);
    dWorldSetContactSurfaceLayer(worldId, CONTACT_SURFACE_LAYER);
    if(USE_QUICKSTEP)
        dWorldSetQuickStepNumIterations(worldId, QUICKSTEP_NUM_ITERATIONS);

    dWorldSetGravity(worldId, 0, 0, DEFAULT_GRAVITY_ACCELERATION);
}

ODE_World::~ODE_World()
{
    for(int i=0; i<numBodies(); i++){
        hrp::BodyPtr b = body(i);
        ODE_Link* link = (ODE_Link*)(b->rootLink());
        link->destroy();
    }
    dJointGroupDestroy(contactgroupId);
    dSpaceDestroy(spaceId);
    dWorldDestroy(worldId);
    dCloseODE();
}

void ODE_World::setGravityAcceleration(const dVector3& gravity)
{
    dWorldSetGravity(worldId, gravity[0], gravity[1], gravity[2]);
}

void  ODE_World::getGravityAcceleration(dVector3& gravity)
{
    dWorldGetGravity(worldId, gravity);
}

void ODE_World::addBody(OpenHRP::BodyInfo_ptr bodyInfo, const char *name)
{
    hrp::BodyPtr body = new hrp::Body();
    ODE_loadBodyFromBodyInfo(body, this, bodyInfo);
    body->setName(name);
    
    hrp::WorldBase::addBody(body);
}

void ODE_World::addCollisionPair(OpenHRP::LinkPair& linkPair){
    const char* bodyName[2];
    bodyName[0] = linkPair.charName1;
    bodyName[1] = linkPair.charName2;
	
    const char* linkName[2];
    linkName[0] = linkPair.linkName1;
    linkName[1] = linkPair.linkName2;

    hrp::BodyPtr body1 = this->body(bodyName[0]);
    if(body1 == NULL){
        std::cout << "ODE_World::addCollisionPair : Body ";
        std::cout << bodyName[0] << " is not found." << std::endl;
        return;
    }
    hrp::BodyPtr body2 = this->body(bodyName[1]);
    if(body2 == NULL){
        std::cout << "ODE_World::addCollisionPair : Body ";
        std::cout << bodyName[1] << " is not found." << std::endl;
        return;
    }
    ODE_Link* link1 = (ODE_Link*)body1->link(linkName[0]);
    if(link1 == NULL){
        std::cout << "ODE_World::addCollisionPair : Link ";
        std::cout << linkName[0] << " is not found." << std::endl;
        return;
    }
    ODE_Link* link2 = (ODE_Link*)body2->link(linkName[1]);
    if(link2 == NULL){
        std::cout << "ODE_World::addCollisionPair : Link ";
        std::cout << linkName[1] << " is not found." << std::endl;
        return;
    }

    LinkPair _linkPair;
    _linkPair.bodyId1 = link1->bodyId;
    _linkPair.bodyId2 = link2->bodyId;
    linkPairs.push_back(_linkPair);
}

void ODE_World::calcNextState(OpenHRP::CollisionSequence& corbaCollisionSequence){
    if(useInternalCollisionDetector_){
        int n = linkPairs.size();
        collisions.length(n);
        for(int i=0; i<n; i++)
            collisions[i].points.length(0);
        dSpaceCollide(spaceId, (void *)this, &ODE_collideCallback);
        corbaCollisionSequence = collisions;
    }else{
        for(int i=0; i<corbaCollisionSequence.length(); i++){
            OpenHRP::Collision& _collision = corbaCollisionSequence[i];
            std::string charName1 = (std::string)_collision.pair.charName1;
            std::string linkName1 = (std::string)_collision.pair.linkName1;
            ODE_Link* link1 = (ODE_Link*)body(charName1)->link(linkName1);
            std::string charName2 = (std::string)_collision.pair.charName2;
            std::string linkName2 = (std::string)_collision.pair.linkName2;
            ODE_Link* link2 = (ODE_Link*)body(charName2)->link(linkName2);

            OpenHRP::CollisionPointSequence& points = _collision.points;
            int n = points.length();
            for(int j=0; j<n; j++){
                dContact contact;
                contact.geom.pos[0] = points[j].position[0];
                contact.geom.pos[1] = points[j].position[1];
                contact.geom.pos[2] = points[j].position[2];
                contact.geom.normal[0] = -points[j].normal[0];
                contact.geom.normal[1] = -points[j].normal[1];
                contact.geom.normal[2] = -points[j].normal[2];
                contact.geom.depth = points[j].idepth;
                //TODO

 //               contact.geom.g1 = link1->geomId;
 //               contact.geom.g2 = link2->geomId;
                //std::cout << "out pos " << contact.geom.pos[0] << "  " << contact.geom.pos[1]  << "  " <<contact.geom.pos[2]  << std::endl;
                //std::cout << "out normal " << contact.geom.normal[0] << "  " << contact.geom.normal[1]  << "  " <<contact.geom.normal[2]  << std::endl;
                //std::cout << "out depth " << contact.geom.depth << std::endl;

                contact.surface.mode = SURFACE_MODE;
                contact.surface.mu = SURFACE_MU;
                contact.surface.mu2 = SURFACE_MU2;
                contact.surface.bounce = SURFACE_BOUNCE;
                contact.surface.bounce_vel = SURFACE_BOUNCE_VEL;
                contact.surface.soft_erp = SURFACE_SOFT_ERP;
                contact.surface.soft_cfm = SURFACE_SOFT_CFM;
                contact.surface.motion1 = SURFACE_MOTION1;
                contact.surface.motion2 = SURFACE_MOTION2;
                contact.surface.slip1 = SURFACE_SLIP1;
                contact.surface.slip2 = SURFACE_SLIP2;
                contact.fdir1[0] = CONTACT_FDIR1_X; 
                contact.fdir1[1] = CONTACT_FDIR1_Y;
                contact.fdir1[2] = CONTACT_FDIR1_Z;

                dJointID c = dJointCreateContact(worldId, contactgroupId, &contact);
                dJointAttach(c, link1->bodyId, link2->bodyId);
            }
            //std::cout << std::endl;
        }
    }

    if(USE_QUICKSTEP)
        dWorldQuickStep(worldId, timeStep_);
    else
        dWorldStep(worldId, timeStep_);

    updateSensors();

    currentTime_ += timeStep_;
}

void ODE_World::initialize(){
    for(int i=0; i<numBodies(); i++){
        hrp::BodyPtr b = body(i);
        for(int j=0; j<b->numLinks(); j++){
            ODE_Link* link = (ODE_Link*)b->link(j);
            if(link->jointType==ODE_Link::FIXED_JOINT)
                dJointSetFixed(link->odeJointId);
        }

        // for sensor
        BodyInfo& info = bodyInfoArray[i];
        info.forwardDynamics.reset(new ODE_ForwardDynamics(b));
        info.forwardDynamics->setTimeStep(timeStep_);
        info.forwardDynamics->enableSensors(sensorsAreEnabled);
        info.forwardDynamics->initialize();

    }

}

void ODE_World::clearExternalForces(){
    dJointGroupEmpty(contactgroupId);
}

void ODE_World::updateSensors(){
    for(int i=0; i<numBodies(); i++){
        BodyInfo& info = bodyInfoArray[i];
        ODE_ForwardDynamicsPtr forwardDynamics = boost::dynamic_pointer_cast<ODE_ForwardDynamics>(info.forwardDynamics);
        forwardDynamics->updateSensors();
    }
}

void ODE_collideCallback(void *data, dGeomID o1, dGeomID o2){

    dContact contact[COLLISION_MAX_POINT];
    ODE_World* world = (ODE_World*)data;
    OpenHRP::CollisionSequence& collisions = world->collisions;
    
    ODE_World::LinkPairArray& linkPairs = world->linkPairs;
    int collisionIndex = -1;
    dBodyID body1 = dGeomGetBody(o1);
    dBodyID body2 = dGeomGetBody(o2);

    for(int i=0; i<linkPairs.size(); i++){
        if( (linkPairs[i].bodyId1 == body1 && linkPairs[i].bodyId2 == body2) ||
            (linkPairs[i].bodyId1 == body2 && linkPairs[i].bodyId2 == body1) ){
            collisionIndex = i;
            break;
        }
    }
    if(collisionIndex == -1)
        return;

    int n= dCollide(o1, o2, COLLISION_MAX_POINT, &contact[0].geom, sizeof(dContact));
    collisions[collisionIndex].points.length(n);
    for(int i=0; i<n; i++){
        collisions[collisionIndex].points[i].position[0] = contact[i].geom.pos[0];
        collisions[collisionIndex].points[i].position[1] = contact[i].geom.pos[1];
        collisions[collisionIndex].points[i].position[2] = contact[i].geom.pos[2];
        collisions[collisionIndex].points[i].normal[0] = contact[i].geom.normal[0];
        collisions[collisionIndex].points[i].normal[1] = contact[i].geom.normal[1];
        collisions[collisionIndex].points[i].normal[2] = contact[i].geom.normal[2];
        collisions[collisionIndex].points[i].idepth = contact[i].geom.depth;

        contact[i].surface.mode = SURFACE_MODE;
        contact[i].surface.mu = SURFACE_MU;
        contact[i].surface.mu2 = SURFACE_MU2;
        contact[i].surface.bounce = SURFACE_BOUNCE;
        contact[i].surface.bounce_vel = SURFACE_BOUNCE_VEL;
        contact[i].surface.soft_erp = SURFACE_SOFT_ERP;
        contact[i].surface.soft_cfm = SURFACE_SOFT_CFM;
        contact[i].surface.motion1 = SURFACE_MOTION1;
        contact[i].surface.motion2 = SURFACE_MOTION2;
        contact[i].surface.slip1 = SURFACE_SLIP1;
        contact[i].surface.slip2 = SURFACE_SLIP2;
        contact[i].fdir1[0] = CONTACT_FDIR1_X; 
        contact[i].fdir1[1] = CONTACT_FDIR1_Y;
        contact[i].fdir1[2] = CONTACT_FDIR1_Z;

        dJointID c = dJointCreateContact(world->getWorldID(), world->getJointGroupID(), &contact[i]);
        dJointAttach(c, dGeomGetBody(contact[i].geom.g1), dGeomGetBody(contact[i].geom.g2));
    }
     
}

ODE_ForwardDynamics::ODE_ForwardDynamics(hrp::BodyPtr body) :
    ForwardDynamics(body)
{
}

void ODE_ForwardDynamics::calcNextState(){
}

void ODE_ForwardDynamics::initialize(){
    	initializeSensors();
}

#ifndef M_2PI
#define M_2PI   6.28318530717958647692
#endif
void ODE_ForwardDynamics::updateSensors(){
    for(int i=0; i<body->numLinks(); i++){
        ODE_Link* link = (ODE_Link*)body->link(i);
        const dReal* _w = link->getAngularVel();
        link->w << _w[0], _w[1], _w[2];
        hrp::Matrix33 R;
        link->getTransform(link->p, R);
        link->setSegmentAttitude(R);
        link->getLinearVel(link->v);

        link->dq = link->getVelocity();
        link->q += link->dq * timeStep;
        dReal q =link->getAngle();

        if(link->jointType == ODE_Link::ROTATIONAL_JOINT && fabs(q-link->q) > M_PI ){
            dReal oldq=link->q;
            int k = link->q/M_2PI;
            if( link->q>=0 )
                if( q>=0 )
                    link->q = k * M_2PI + q;
                else
                    link->q = (k+1) * M_2PI + q;
            else
                if( q<0 )
                    link->q = k * M_2PI + q;
                else
                    link->q = (k-1) * M_2PI + q;
        }else
            link->q = q;
    }
    updateSensorsFinal();

    int n = body->numSensors(hrp::Sensor::FORCE);
    for(int i=0; i < n; ++i){
		updateForceSensor((ODE_ForceSensor*)body->sensor<hrp::ForceSensor>(i));
	}
}

void ODE_ForwardDynamics::updateForceSensor(ODE_ForceSensor* sensor){
    ODE_Link* link = (ODE_Link*)sensor->link;
    dJointFeedback* fb = dJointGetFeedback(link->odeJointId);
    hrp::Vector3 f(fb->f2[0], fb->f2[1], fb->f2[2]);
    hrp::Vector3 tau(fb->t2[0], fb->t2[1], fb->t2[2]);
    hrp::Matrix33 sensorR(link->R * sensor->localR);
    hrp::Vector3 fs(sensorR.transpose() * f);
    //hrp::Vector3 sensorPos(link->p + link->R * sensor->localPos);
    hrp::Vector3 sensorPos(link->R * (sensor->localPos - link->parent->c));
    hrp::Vector3 ts(sensorR.transpose() * (tau - sensorPos.cross(f)));

	sensor->f   = fs;
	sensor->tau = ts;
}
