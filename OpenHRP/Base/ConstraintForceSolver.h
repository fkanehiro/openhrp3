// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-
#ifndef CONTACT_FORCE_SOLVER_H_INCLUDED
#define CONTACT_FORCE_SOLVER_H_INCLUDED

#include "hrpModelExportDef.h"

namespace OpenHRP
{
	class CollisionSequence;
	
	class Link;
	class CFSImpl;
	class WorldBase;
	
    class HRPMODEL_EXPORT ContactForceSolver
    {
		CFSImpl* impl;
		
    public:
        ContactForceSolver(WorldBase& world);
        ~ContactForceSolver();
		
        bool addCollisionCheckLinkPair
		(int bodyIndex1, Link* link1, int bodyIndex2, Link* link2, double muStatic, double muDynamic, double epsilon);

		void setGaussSeidelParameters(int maxNumIteration, int numInitialIteration, double maxRelError);
		bool enableJointRangeStopper(bool isEnabled);
		bool enableVelocityOverwriting(bool isEnabled);

		void initialize(void);
        void solve(CollisionSequence& corbaCollisionSequence);
		void clearExternalForces();
	};
};


#endif

