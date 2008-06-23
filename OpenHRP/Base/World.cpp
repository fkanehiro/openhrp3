// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-
/** @file DynamicsSimulator/server/World.cpp
 *
 */

#include <string>

#include "Link.h"
#include "ForwardDynamicsABM.h"
#include "ForwardDynamicsCBM.h"
#include "Body.h"
#include "World.h"

using namespace std;
using namespace OpenHRP;

static const double DEFAULT_GRAVITY_ACCELERATION = 9.80665;

static const bool debugMode = false;


bool WorldBase::LinkPairKey::operator<(const LinkPairKey& pair2) const
{
	if((body1 == pair2.body1 && body2 == pair2.body2) ||
	   (body2 == pair2.body1 && body1 == pair2.body2 )){
		if(link1 < link2){
			if(pair2.link1 < pair2.link2){
				return (link1 < pair2.link1) ? true : (link2 < pair2.link2);
			} else {
				return (link1 < pair2.link2) ? true : (link2 < pair2.link1);
			}
		} else {
			if(pair2.link1 < pair2.link2){
				return (link2 < pair2.link1) ? true : (link1 < pair2.link2);
			} else {
				return (link2 < pair2.link2) ? true : (link1 < pair2.link1);
			}
		}
	} else {
		if(body1 < body2){
			if(pair2.body1 < pair2.body2){
				return (body1 < pair2.body1) ? true : (body2 < pair2.body2);
			} else {
				return (body1 < pair2.body2) ? true : (body2 < pair2.body1);
			}
		} else {
			if(pair2.body1 < pair2.body2){
				return (body2 < pair2.body1) ? true : (body1 < pair2.body2);
			} else {
				return (body2 < pair2.body2) ? true : (body1 < pair2.body1);
			}
		}
	}
}


WorldBase::WorldBase()
{
    currentTime_ = 0.0;
    timeStep_ = 0.005;

    g = 0.0, 0.0, DEFAULT_GRAVITY_ACCELERATION;

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
	return bodyInfoArray[index].body; 
}


BodyPtr WorldBase::body(const std::string& name)
{
    return bodyInfoArray[bodyIndex(name)].body;
}


void WorldBase::setTimeStep(double ts)
{
    timeStep_ = ts;
}


void WorldBase::setCurrentTime(double time)
{
    currentTime_ = time;
}


void WorldBase::setGravityAcceleration(const vector3& g)
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
		for(int j=1; j < nL; ++j){
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
    const string& name = body->name;

    if(!name.empty()){
        nameToBodyIndexMap[name] = bodyInfoArray.size();
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


void WorldBase::setEulerMethod()
{
    isEulerMethod = true;
}


void WorldBase::setRungeKuttaMethod()
{
    isEulerMethod = false;
}


std::pair<int,bool> WorldBase::getIndexOfLinkPairs(BodyPtr body1, Link* link1, BodyPtr body2, Link* link2)
{
	int index = -1;
	int isRegistered = false;

    if(link1 != link2){

		LinkPairKey linkPair;
		linkPair.body1 = body1;
		linkPair.link1 = link1;
		linkPair.body2 = body2;
		linkPair.link2 = link2;

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
