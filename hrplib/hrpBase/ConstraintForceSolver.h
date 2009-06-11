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

#ifndef CONTACT_FORCE_SOLVER_H_INCLUDED
#define CONTACT_FORCE_SOLVER_H_INCLUDED

#include "hrpModelExportDef.h"

namespace OpenHRP {
	class CollisionSequence;
}

namespace hrp
{
	class Link;
	class CFSImpl;
	class WorldBase;
	
    class HRPBASE_EXPORT ContactForceSolver
    {
		CFSImpl* impl;
		
    public:
        ContactForceSolver(WorldBase& world);
        ~ContactForceSolver();
		
        bool addCollisionCheckLinkPair
		(int bodyIndex1, Link* link1, int bodyIndex2, Link* link2, double muStatic, double muDynamic, double culling_thresh, double epsilon);

		void setGaussSeidelParameters(int maxNumIteration, int numInitialIteration, double maxRelError);
		bool enableJointRangeStopper(bool isEnabled);
		bool enableVelocityOverwriting(bool isEnabled);

		void initialize(void);
        void solve(OpenHRP::CollisionSequence& corbaCollisionSequence);
		void clearExternalForces();
	};
};


#endif

