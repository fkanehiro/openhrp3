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

#ifndef OPENHRP_FORWARD_DYNAMICS_H_INCLUDED
#define OPENHRP_FORWARD_DYNAMICS_H_INCLUDED

#include <boost/shared_ptr.hpp>
#include <boost/intrusive_ptr.hpp>

#include "tvmet3d.h"
#include "hrpModelExportDef.h"


namespace hrp
{
    class Body;
    typedef boost::intrusive_ptr<Body> BodyPtr;

	class AccelSensor;

    /**
       This class calculates the forward dynamics of a Body object
       by using the Featherstone's articulated body algorithm.
       The class also integrates motion using the Euler method or RungeKutta method.
    */
    class HRPMODEL_EXPORT ForwardDynamics {

    public:
        
        ForwardDynamics(BodyPtr body);
		virtual ~ForwardDynamics();
        
        void setGravityAcceleration(const vector3& g);
        void setEulerMethod();
        void setRungeKuttaMethod();
        void setTimeStep(double timeStep);
        void enableSensors(bool on);

        virtual void initialize() = 0;
        virtual void calcNextState() = 0;

    protected:

		virtual void initializeSensors();
		virtual void updateSensorsFinal();
		
		static void SE3exp(vector3& out_p, matrix33& out_R,
						   const vector3& p0, const matrix33& R0,
						   const vector3& w, const vector3& vo, double dt);
		
        BodyPtr body;
        vector3 g;
        double timeStep;
        bool sensorsEnabled;

        enum { EULER_METHOD, RUNGEKUTTA_METHOD } integrationMode;

	private:

		void updateAccelSensor(AccelSensor* sensor);
		void initializeAccelSensors();

		// varialbes for calculating sensor values
		// preview control gain matrices for force sensors
		typedef tvmet::Matrix<double, 2,2> matrix22;
		typedef tvmet::Vector<double, 2> vector2;
		matrix22 A;
		vector2 B;
		
    };

	typedef boost::shared_ptr<ForwardDynamics> ForwardDynamicsPtr;
	
};

#endif
