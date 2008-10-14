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

#ifndef OPENHRP_FORWARD_DYNAMICS_MM_H_INCLUDED
#define OPENHRP_FORWARD_DYNAMICS_MM_H_INCLUDED

#include <vector>
#include <boost/shared_ptr.hpp>
#include <boost/intrusive_ptr.hpp>

#include "tvmet3d.h"
#include "uBlasCommonTypes.h"
#include "ForwardDynamics.h"
#include "hrpModelExportDef.h"

namespace hrp
{
	class Link;
	
    class Body;
    typedef boost::intrusive_ptr<Body> BodyPtr;

    class LinkTraverse;
    class AccelSensor;
    class ForceSensor;

	/**
	   The ForwardDynamicsMM class calculates the forward dynamics using
	   the motion equation based on the generalized mass matrix.
	   The class is mainly used for a body that has high-gain mode joints.
	   If all the joints of a body are the torque mode, the ForwardDynamicsABM,
	   which uses the articulated body method, is more efficient.
	*/
    class HRPBASE_EXPORT ForwardDynamicsMM : public ForwardDynamics {

    public:
        
        ForwardDynamicsMM(BodyPtr body);
		~ForwardDynamicsMM();

		virtual void initialize();
        virtual void calcNextState();

		void initializeAccelSolver();
		void solveUnknownAccels(const vector3& fext, const vector3& tauext);

    private:
        
		/*
		   Elements of the motion equation
		   
		  |     |     |   | dv         |   | b1 |   | fext      |
		  | M11 | M12 |   | dw         |   |    |   | tauext    |
		  |     |     | * |ddq (unkown)| + |    | = | u (known) |
		  |-----+-----|   |------------|   |----|   |-----------|
		  | M21 | M22 |   | given ddq  |   | b2 |   | u2        |
		*/
		
		dmatrix M11;
		dmatrix M12;
		dmatrix b1;
		dvector c1;

		std::vector<Link*> torqueModeJoints;
		std::vector<Link*> highGainModeJoints;

		int rootDof; // dof of dv and dw (0 or 6)
		bool isNoUnknownAccelMode;

		dvector qGiven;
		dvector dqGiven;
		dmatrix ddqGiven;;

		bool accelSolverInitialized;
		bool ddqGivenCopied;

		dvector qGivenPrev;
		dvector dqGivenPrev;

		vector3 fextTotal;
		vector3 tauextTotal;

		vector3 root_w_x_v;

		// buffers for the unit vector method
		dvector ddqorg;
		dvector uorg;
		vector3 dvoorg;
		vector3 dworg;
		
		struct ForceSensorInfo {
			ForceSensor* sensor;
			bool hasSensorsAbove;
		};

		std::vector<ForceSensorInfo> forceSensorInfo;

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

		virtual void initializeSensors();

		void calcMotionWithEulerMethod();
		void calcMotionWithRungeKuttaMethod();
		void integrateRungeKuttaOneStep(double r, double dt);
		void preserveHighGainModeJointState();
		void calcPositionAndVelocityFK();
		void calcMassMatrix();
		void setColumnOfMassMatrix(dmatrix& M, int column);
		void calcInverseDynamics(Link* link, vector3& out_f, vector3& out_tau);
		void sumExternalForces();
		inline void solveUnknownAccels();
		inline void calcAccelFKandForceSensorValues();
		void calcAccelFKandForceSensorValues(Link* link, vector3& out_f, vector3& out_tau);
		void updateForceSensorInfo(Link* link, bool hasSensorsAbove);
    };

	typedef boost::shared_ptr<ForwardDynamicsMM> ForwardDynamicsMMPtr;
	
};

#endif
