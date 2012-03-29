/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */
/**
   \file
   \author Shin'ichiro Nakaoka
*/

#ifndef OPENHRP_FORWARD_DYNAMICS_MM_H_INCLUDED
#define OPENHRP_FORWARD_DYNAMICS_MM_H_INCLUDED

#include <vector>
#include <boost/shared_ptr.hpp>
#include <boost/intrusive_ptr.hpp>
#include <hrpUtil/Eigen3d.h>
#include "ForwardDynamics.h"
#include "Config.h"

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
    class HRPMODEL_API ForwardDynamicsMM : public ForwardDynamics {

    public:
        
        ForwardDynamicsMM(BodyPtr body);
		~ForwardDynamicsMM();

		virtual void initialize();
        virtual void calcNextState();

		void initializeAccelSolver();
		void solveUnknownAccels(const Vector3& fext, const Vector3& tauext);
        void solveUnknownAccels(Link* link, const Vector3& fext, const Vector3& tauext, const Vector3& rootfext, const Vector3& roottauext);
        void sumExternalForces();
		void solveUnknownAccels();

    private:
        
		/*
		   Elements of the motion equation
		   
		  |     |     |   | dv         |   | b1 |   | 0  |   | totalfext      |
		  | M11 | M12 |   | dw         |   |    |   | 0  |   | totaltauext    |
		  |     |     | * |ddq (unkown)| + |    | + | d1 | = | u (known)      |
		  |-----+-----|   |------------|   |----|   |----|   |----------------|
		  | M21 | M22 |   | given ddq  |   | b2 |   | d2 |   | u2             |
		
                         |fext  |
            d1 = trans(s)|      |
                         |tauext|

        */
		
		dmatrix M11;
		dmatrix M12;
		dmatrix b1;
        dmatrix d1;
		dvector c1;

		std::vector<Link*> torqueModeJoints;
		std::vector<Link*> highGainModeJoints;

		//int rootDof; // dof of dv and dw (0 or 6)
        int unknown_rootDof;
        int given_rootDof;

		bool isNoUnknownAccelMode;

		dvector qGiven;
		dvector dqGiven;
		dvector ddqGiven;
        Vector3 pGiven;
        Matrix33 RGiven;
        Vector3 voGiven;
        Vector3 wGiven;

		bool accelSolverInitialized;
		bool ddqGivenCopied;

		dvector qGivenPrev;
		dvector dqGivenPrev;
        Vector3 pGivenPrev;
        Matrix33 RGivenPrev;
        Vector3 voGivenPrev;
        Vector3 wGivenPrev;

		Vector3 fextTotal;
		Vector3 tauextTotal;

		Vector3 root_w_x_v;

		// buffers for the unit vector method
		dvector ddqorg;
		dvector uorg;
		Vector3 dvoorg;
		Vector3 dworg;
		
		struct ForceSensorInfo {
			ForceSensor* sensor;
			bool hasSensorsAbove;
		};

		std::vector<ForceSensorInfo> forceSensorInfo;

		// Buffers for the Runge Kutta Method
		Vector3 p0;
		Matrix33 R0;
		Vector3 vo0;
		Vector3 w0;
		std::vector<double> q0;
		std::vector<double> dq0;
		
		Vector3 vo;
		Vector3 w;
		Vector3 dvo;
		Vector3 dw;
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
		void calcInverseDynamics(Link* link, Vector3& out_f, Vector3& out_tau);
        void calcd1(Link* link, Vector3& out_f, Vector3& out_tau);
		inline void calcAccelFKandForceSensorValues();
		void calcAccelFKandForceSensorValues(Link* link, Vector3& out_f, Vector3& out_tau);
		void updateForceSensorInfo(Link* link, bool hasSensorsAbove);
    };

	typedef boost::shared_ptr<ForwardDynamicsMM> ForwardDynamicsMMPtr;
	
};

#endif
