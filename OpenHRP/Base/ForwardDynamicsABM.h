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
/**
   \file
   \author S.NAKAOKA
*/

#ifndef OPENHRP_FORWARD_DYNAMICS_ABM_H_INCLUDED
#define OPENHRP_FORWARD_DYNAMICS_ABM_H_INCLUDED

#include "tvmet3d.h"
#include "ForwardDynamics.h"
#include "hrpModelExportDef.h"

#include <vector>
#include <boost/intrusive_ptr.hpp>


namespace OpenHRP
{
    class Body;
    typedef boost::intrusive_ptr<Body> BodyPtr;

    class LinkTraverse;
	class AccelSensor;
	class ForceSensor;

    /**
	   Forward dynamics calculation using Featherstone's Articulated Body Method (ABM)
    */
    class HRPMODEL_EXPORT ForwardDynamicsABM : public ForwardDynamics {

    public:
        
        ForwardDynamicsABM(BodyPtr body);
		~ForwardDynamicsABM();
        
        virtual void initialize();
        virtual void calcNextState();

    private:
        
        void calcMotionWithEulerMethod();
		void integrateRungeKuttaOneStep(double r, double dt);
        void calcMotionWithRungeKuttaMethod();
		
        void calcABMPhase1();
		void calcABMPhase2();
		void calcABMPhase2Part1();
		void calcABMPhase2Part2();
        void calcABMPhase3();

        inline void calcABMFirstHalf();
		inline void calcABMLastHalf();

        void updateForceSensors();
		void updateForceSensor(ForceSensor* sensor);
		
        // Buffers for the Runge Kutta Method
		vector3 p0;
		matrix33 R0;
		vector3 vo0;
		vector3 w0;
		std::vector<double> q0;
		std::vector<double> dq0;
		
		vector3 vo;
		vector3 w;
		vector3 dvo;
		vector3 dw;
		std::vector<double> dq;
		std::vector<double> ddq;

    };
	
};

#endif
