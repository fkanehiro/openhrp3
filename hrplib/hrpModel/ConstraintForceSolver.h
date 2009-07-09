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

#ifndef OPENHRP_CONSTRAINT_FORCE_SOLVER_H_INCLUDED
#define OPENHRP_CONSTRAINT_FORCE_SOLVER_H_INCLUDED

#include "exportdef.h"

namespace OpenHRP {
	class CollisionSequence;
}

namespace hrp
{
	class Link;
	class CFSImpl;
	class WorldBase;
	
    class HRPMODEL_API ConstraintForceSolver
    {
		CFSImpl* impl;
		
    public:
        ConstraintForceSolver(WorldBase& world);
        ~ConstraintForceSolver();
		
        bool addCollisionCheckLinkPair
		(int bodyIndex1, Link* link1, int bodyIndex2, Link* link2, double muStatic, double muDynamic, double culling_thresh, double epsilon);
		void clearCollisionCheckLinkPairs();

		void setGaussSeidelParameters(int maxNumIteration, int numInitialIteration, double maxRelError);
		bool enableJointRangeStopper(bool isEnabled);
		bool enableVelocityOverwriting(bool isEnabled);
		void useBuiltinCollisionDetector(bool on);

		void initialize(void);
        void solve(OpenHRP::CollisionSequence& corbaCollisionSequence);
		void clearExternalForces();
	};
};


#endif

