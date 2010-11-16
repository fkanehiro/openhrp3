/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

#ifndef ODE_LINK_H_INCLUDED
#define ODE_LINK_H_INCLUDED

#include <ode/ode.h>
#include <hrpModel/Link.h>
#include <hrpModel/Sensor.h>
#include <hrpUtil/Tvmet4d.h>
#include <vector>

class ODE_Link : public hrp::Link{
    public :

        void destroy();

        void getTransform(hrp::Vector3& pos, hrp::Matrix33& R);
        void setTransform(const hrp::Vector3& pos, const hrp::Matrix33& R);

        dReal getAngle();
        dReal getVelocity();

        const dReal* getAngularVel();
        void getLinearVel(hrp::Vector3& v);
        void setAbsVelocity(hrp::Vector3& v, hrp::Vector3& w);
        const dReal* getForce();
        const dReal* getTorque();
        void setForce(double fx, double fy, double fz);
        void setTorque(double fx, double fy, double fz);
        void setTorque(dReal data);

        dBodyID bodyId;
        dJointID odeJointId;
        
        std::vector<dGeomID> geomIds;
        dTriMeshDataID triMeshDataId;
        std::vector<dReal> vertices;
        std::vector<int> indices;

        hrp::Vector3 C;
};

class  ODE_ForceSensor : public hrp::ForceSensor
{
    public:
        dJointFeedback feedback;
};

#endif