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
    \author Rafael Cisneros
*/

#ifndef OPENHRP_CONSTRAINT_FORCE_SOLVER_H_INCLUDED
#define OPENHRP_CONSTRAINT_FORCE_SOLVER_H_INCLUDED

#include "Config.h"

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
		(int bodyIndex1, Link* link1, int bodyIndex2, Link* link2, double muStatic, double muDynamic, double culling_thresh, double restitution, double epsilon);
		bool addExtraJoint(int bodyIndex1, Link* link1, int bodyIndex2, Link* link2, const double* link1LocalPos, const double* link2LocalPos, const short jointType, const double* jointAxis );
		void clearCollisionCheckLinkPairs();

		void setGaussSeidelParameters(int maxNumIteration, int numInitialIteration, double maxRelError);
                void enableConstraintForceOutput(bool on);
		void useBuiltinCollisionDetector(bool on);
                void setNegativeVelocityRatioForPenetration(double ratio);

		void initialize(void);
        void solve(OpenHRP::CollisionSequence& corbaCollisionSequence);
		void clearExternalForces();
        void setAllowedPenetrationDepth(double dVal);
        double getAllowedPenetrationDepth() const;
	};
};


#endif

