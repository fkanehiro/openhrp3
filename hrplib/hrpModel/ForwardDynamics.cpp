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

#include "ForwardDynamics.h"
#include "Body.h"
#include "Link.h"
#include "Sensor.h"

using namespace hrp;


ForwardDynamics::ForwardDynamics(BodyPtr body)
	: body(body)
{
    g.setZero();
    timeStep = 0.005;

    integrationMode = RUNGEKUTTA_METHOD;
    sensorsEnabled = false;
}


ForwardDynamics::~ForwardDynamics()
{

}


void ForwardDynamics::setTimeStep(double ts)
{
    timeStep = ts;
}


void ForwardDynamics::setGravityAcceleration(const Vector3& g)
{
    this->g = g;
}


void ForwardDynamics::setEulerMethod()
{
    integrationMode = EULER_METHOD;
}


void ForwardDynamics::setRungeKuttaMethod()
{
    integrationMode = RUNGEKUTTA_METHOD;
}


void ForwardDynamics::enableSensors(bool on)
{
    sensorsEnabled = on;
}


/// function from Murray, Li and Sastry p.42
void ForwardDynamics::SE3exp(Vector3& out_p, Matrix33& out_R,
							 const Vector3& p0, const Matrix33& R0,
							 const Vector3& w, const Vector3& vo, double dt)
{
    using ::std::numeric_limits;
	
    double norm_w = w.norm();
	
    if(norm_w < numeric_limits<double>::epsilon() ) {
		out_p = p0 + vo * dt;
		out_R = R0;
    } else {
		double th = norm_w * dt;
		Vector3 w_n(w / norm_w);
		Vector3 vo_n(vo / norm_w);
		Matrix33 rot = rodrigues(w_n, th);
		
		out_p = rot * p0 + (Matrix33::Identity() - rot) * Vector3(w_n.cross(vo_n)) + VVt_prod(w_n, w_n) * vo_n * th;
		out_R = rot * R0;
    }
}


void ForwardDynamics::initializeSensors()
{
	body->clearSensorValues();

	if(sensorsEnabled){
		initializeAccelSensors();
	}
}
	

void ForwardDynamics::updateSensorsFinal()
{
    int n;

	n = body->numSensors(Sensor::RATE_GYRO);
    for(int i=0; i < n; ++i){
        RateGyroSensor* sensor = body->sensor<RateGyroSensor>(i);
        Link* link = sensor->link;
        sensor->w = sensor->localR.transpose() * Vector3(link->R.transpose() * link->w);
	}

	n = body->numSensors(Sensor::ACCELERATION);
    for(int i=0; i < n; ++i){
		updateAccelSensor(body->sensor<AccelSensor>(i));
	}

}


void ForwardDynamics::updateAccelSensor(AccelSensor* sensor)
{
	Link* link = sensor->link;
	vector2* x = sensor->x;

	Vector3 o_Vgsens(link->R * Vector3(link->R.transpose() * link->w).cross(sensor->localPos) + link->v);

    if(sensor->isFirstUpdate){
		sensor->isFirstUpdate = false;
		for(int i=0; i < 3; ++i){
			x[i](0) = o_Vgsens(i);
			x[i](1) = 0.0;
		}
    } else {
		// kalman filtering
		for(int i=0; i < 3; ++i){
			x[i] = A * x[i] + o_Vgsens(i) * B;
		}
    }

    Vector3 o_Agsens(x[0](1), x[1](1), x[2](1));
    o_Agsens += g;

    sensor->dv = link->R.transpose() * o_Agsens;
}


void ForwardDynamics::initializeAccelSensors()
{
	int n = body->numSensors(Sensor::ACCELERATION);
	if(n > 0){
		for(int i=0; i < n; ++i){
			AccelSensor* sensor = body->sensor<AccelSensor>(i);
			if(sensor){
				sensor->isFirstUpdate = true;
			}
		}

		// Kalman filter design
		static const double n_input = 100.0;  // [N]
		static const double n_output = 0.001; // [m/s]

		// Analytical solution of Kalman filter (continuous domain)
		// s.kajita  2003 Jan.22

		matrix22 Ac;
		Ac << -sqrt(2*n_input/n_output), 1.0,
			-n_input/n_output, 0.0;

		vector2 Bc(sqrt(2*n_input/n_output), n_input/n_output);

		A = matrix22::Identity();
		matrix22 An(matrix22::Identity());
		matrix22 An2;
		B = timeStep * Bc;
		vector2 Bn(B);
		vector2 Bn2;

		double factorial[14];
		double r = 1.0;
		factorial[1] = r;
		for(int i=2; i <= 13; ++i){
			r += 1.0;
			factorial[i] = factorial[i-1] * r;
		}

		for(int i=1; i <= 12; i++){
			An2 = Ac * An;
			An = timeStep * An2;
			A += (1.0 / factorial[i]) * An;

			Bn2 = Ac * Bn;
			Bn = timeStep * Bn2;
			B += (1.0 / factorial[i+1]) * Bn;
		}
	}
}
