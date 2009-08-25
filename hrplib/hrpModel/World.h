/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
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
#include <hrpUtil/Tvmet3d.h>
#include "Body.h"
#include "ForwardDynamics.h"
#include "Config.h"

namespace OpenHRP {
	class CollisionSequence;
}

namespace hrp {

    class Link;

    class HRPMODEL_API WorldBase
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
	
		void setGravityAcceleration(const Vector3& g);
		const Vector3& getGravityAcceleration() { return g; }

		void enableSensors(bool on);
		
		void setEulerMethod();
		void setRungeKuttaMethod();

		virtual void initialize();
		virtual void calcNextState();

		std::pair<int,bool> getIndexOfLinkPairs(Link* link1, Link* link2);

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
			Link* link1;
			Link* link2;
			bool operator<(const LinkPairKey& pair2) const;
		};
		typedef std::map<LinkPairKey, int> LinkPairKeyToIndexMap;
		LinkPairKeyToIndexMap linkPairKeyToIndexMap;

		int numRegisteredLinkPairs;
		
        Vector3 g;

        bool isEulerMethod; // Euler or Runge Kutta ?

		bool sensorsAreEnabled;
		
	};


	template <class TConstraintForceSolver> class World : public WorldBase
	{
	public:
		TConstraintForceSolver constraintForceSolver;

		World() : constraintForceSolver(*this) { }

		virtual void initialize() {
			WorldBase::initialize();
			constraintForceSolver.initialize();
		}

		virtual void calcNextState(OpenHRP::CollisionSequence& corbaCollisionSequence) {
			constraintForceSolver.solve(corbaCollisionSequence);
			WorldBase::calcNextState();
		}
	};

};

#endif
