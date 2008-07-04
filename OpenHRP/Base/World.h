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
/** \file
	\author Shin'ichiro Nakaoka
*/

#ifndef OPENHRP_WORLD_H_INCLUDED
#define OPENHRP_WORLD_H_INCLUDED

#include <vector>
#include <map>
#include <boost/shared_ptr.hpp>
#include <boost/intrusive_ptr.hpp>
#include "tvmet3d.h"

#include "hrpModelExportDef.h"


namespace OpenHRP {

	class CollisionSequence;
	
    class Body;
    typedef boost::intrusive_ptr<Body> BodyPtr;
    
    class ForwardDynamics;
	typedef boost::shared_ptr<ForwardDynamics> ForwardDynamicsPtr;
	
    class Link;


    class HRPMODEL_EXPORT WorldBase
    {
    public:
        WorldBase();
        virtual ~WorldBase();

        int numBodies() { return bodyInfoArray.size(); }
        BodyPtr body(int index);
        BodyPtr body(const std::string& name);

		ForwardDynamicsPtr forwardDynamics(int index) {
			return bodyInfoArray[index].forwardDynamics;
		}

		int bodyIndex(const std::string& name);

        int addBody(BodyPtr body);

        void clearBodies();

		void clearCollisionPairs();

		void setTimeStep(double);
		double timeStep(void) const { return timeStep_; }
	
		void setCurrentTime(double);
		double currentTime(void) const { return currentTime_; }
	
		void setGravityAcceleration(const vector3& g);
		const vector3& getGravityAcceleration() { return g; }

		void enableSensors(bool on);
		
		void setEulerMethod();
		void setRungeKuttaMethod();

		virtual void initialize();
		virtual void calcNextState();

		std::pair<int,bool> getIndexOfLinkPairs(BodyPtr body1, Link* link1, BodyPtr body2, Link* link2);

	private:
		
        double currentTime_;
        double timeStep_;

		struct BodyInfo {
            BodyPtr body;
            ForwardDynamicsPtr forwardDynamics;
        };
        std::vector<BodyInfo> bodyInfoArray;

        typedef std::map<std::string, int> NameToIndexMap;
        NameToIndexMap nameToBodyIndexMap;

        typedef std::map<BodyPtr, int> BodyToIndexMap;
        BodyToIndexMap bodyToIndexMap;
		
        struct LinkPairKey {
			BodyPtr body1;
			BodyPtr body2;
			Link* link1;
			Link* link2;
			bool operator<(const LinkPairKey& pair2) const;
		};
		typedef std::map<LinkPairKey, int> LinkPairKeyToIndexMap;
		LinkPairKeyToIndexMap linkPairKeyToIndexMap;

		int numRegisteredLinkPairs;
		
        vector3 g;

        bool isEulerMethod; // Euler or Runge Kutta ?

		bool sensorsAreEnabled;
		
	};


	template <class TContactForceSolver> class World : public WorldBase
	{
	public:
		TContactForceSolver contactForceSolver;

		World() : contactForceSolver(*this) { }

		virtual void initialize() {
			WorldBase::initialize();
			contactForceSolver.initialize();
		}

		virtual void calcNextState(CollisionSequence& corbaCollisionSequence) {
			contactForceSolver.solve(corbaCollisionSequence);
			WorldBase::calcNextState();
		}
	};

};

#endif
