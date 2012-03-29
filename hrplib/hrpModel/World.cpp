/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */
/**
   @file hrplib/hrpModel/World.cpp
   \author Shin'ichiro Nakaoka
*/

#include <iostream>
#include "World.h"
#include "Link.h"
#include "ForwardDynamicsABM.h"
#include "ForwardDynamicsCBM.h"
#include <string>

using namespace std;
using namespace hrp;

static const double DEFAULT_GRAVITY_ACCELERATION = 9.80665;

static const bool debugMode = false;


WorldBase::WorldBase()
{
    currentTime_ = 0.0;
    timeStep_ = 0.005;

    g << 0.0, 0.0, DEFAULT_GRAVITY_ACCELERATION;

    isEulerMethod =false;
    sensorsAreEnabled = false;
    numRegisteredLinkPairs = 0;
}


WorldBase::~WorldBase()
{

}


int WorldBase::bodyIndex(const std::string& name)
{
    NameToIndexMap::iterator p = nameToBodyIndexMap.find(name);
    return (p != nameToBodyIndexMap.end()) ? p->second : -1;
}


BodyPtr WorldBase::body(int index)
{
    if(index < 0 || (int)bodyInfoArray.size() <= index)
        return NULL;

    return bodyInfoArray[index].body; 
}


BodyPtr WorldBase::body(const std::string& name)
{
    int idx = bodyIndex(name);
    if(idx < 0 || (int)bodyInfoArray.size() <= idx)
        return NULL;

    return bodyInfoArray[idx].body;
}


void WorldBase::setTimeStep(double ts)
{
    timeStep_ = ts;
}


void WorldBase::setCurrentTime(double time)
{
    currentTime_ = time;
}


void WorldBase::setGravityAcceleration(const Vector3& g)
{
    this->g = g;
}


void WorldBase::enableSensors(bool on)
{
    sensorsAreEnabled = on;
}


void WorldBase::initialize()
{
    const int n = bodyInfoArray.size();

    for(int i=0; i < n; ++i){

        BodyInfo& info = bodyInfoArray[i];
        BodyPtr body = info.body;

        bool hasHighGainModeJoints = false;
        int nL = body->numLinks();
        for(int j=0; j < nL; ++j){
            if(body->link(j)->isHighGainMode){
                hasHighGainModeJoints = true;
                break;
            }
        }

        if(hasHighGainModeJoints){
            info.forwardDynamics.reset(new ForwardDynamicsMM(body));
        } else {
            info.forwardDynamics.reset(new ForwardDynamicsABM(body));
        }
        if(isEulerMethod){
            info.forwardDynamics->setEulerMethod();
        } else {
            info.forwardDynamics->setRungeKuttaMethod();
        }
        info.forwardDynamics->setGravityAcceleration(g);
    	info.forwardDynamics->setTimeStep(timeStep_);
        info.forwardDynamics->enableSensors(sensorsAreEnabled);
        info.forwardDynamics->initialize();
    }
}


void WorldBase::calcNextState()
{
    if(debugMode){
        cout << "World current time = " << currentTime_ << endl;
    }
    const int n = bodyInfoArray.size();

//#pragma omp parallel for num_threads(3) schedule(static)
#pragma omp parallel for num_threads(3) schedule(dynamic)
    for(int i=0; i < n; ++i){
        BodyInfo& info = bodyInfoArray[i];
        info.forwardDynamics->calcNextState();
    }
    currentTime_ += timeStep_;
}


int WorldBase::addBody(BodyPtr body)
{
    if(!body->name().empty()){
        nameToBodyIndexMap[body->name()] = bodyInfoArray.size();
    }
    BodyInfo info;
    info.body = body;
    bodyInfoArray.push_back(info);

    return bodyInfoArray.size() - 1;
}


void WorldBase::clearBodies()
{
    nameToBodyIndexMap.clear();
    bodyInfoArray.clear();
}


void WorldBase::clearCollisionPairs()
{
    linkPairKeyToIndexMap.clear();
    numRegisteredLinkPairs = 0;
}


void WorldBase::setEulerMethod()
{
    isEulerMethod = true;
}


void WorldBase::setRungeKuttaMethod()
{
    isEulerMethod = false;
}


std::pair<int,bool> WorldBase::getIndexOfLinkPairs(Link* link1, Link* link2)
{
    int index = -1;
    int isRegistered = false;

    if(link1 != link2){

        LinkPairKey linkPair;
        if(link1 < link2){
            linkPair.link1 = link1;
            linkPair.link2 = link2;
        } else {
            linkPair.link1 = link2;
            linkPair.link2 = link1;
        }

        LinkPairKeyToIndexMap::iterator p = linkPairKeyToIndexMap.find(linkPair);

        if(p != linkPairKeyToIndexMap.end()){
            index = p->second;
            isRegistered = true;
        } else {
            index = numRegisteredLinkPairs++;
            linkPairKeyToIndexMap[linkPair] = index;
        }
    }

    return std::make_pair(index, isRegistered);
}


bool WorldBase::LinkPairKey::operator<(const LinkPairKey& pair2) const
{
    if(link1 < pair2.link1){
        return true;
    } else if(link1 == pair2.link1){
        return (link2 < pair2.link2);
    } else {
        return false;
    }
}
