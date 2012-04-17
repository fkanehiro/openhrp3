/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

#include "ODE_Link.h"

void ODE_Link::getTransform(hrp::Vector3& pos_, hrp::Matrix33& R_){
    const dReal* R = dBodyGetRotation(bodyId);
    R_ << R[0],R[1],R[2],
          R[4],R[5],R[6],
          R[8],R[9],R[10];
    dVector3 result;
    dBodyGetRelPointPos(bodyId, -C[0], -C[1], -C[2], result);
    pos_ << result[0], result[1], result[2];

}

void ODE_Link::setTransform(const hrp::Vector3& pos, const hrp::Matrix33& R){
    hrp::Vector3 _pos(R * C + pos);
    dBodySetPosition(bodyId, _pos(0), _pos(1), _pos(2));
    dMatrix3 _R = {R(0,0), R(0,1), R(0,2), 0,
                  R(1,0), R(1,1), R(1,2), 0,
                  R(2,0), R(2,1), R(2,2), 0};
    dBodySetRotation(bodyId, _R);
}

dReal ODE_Link::getAngle(){
    if(jointType == ODE_Link::ROTATIONAL_JOINT)
        return dJointGetHingeAngle(odeJointId);
    else if(jointType == ODE_Link::SLIDE_JOINT)
        return dJointGetSliderPosition(odeJointId);
    else
        return 0;
}

dReal ODE_Link::getVelocity(){
    if(jointType == ODE_Link::ROTATIONAL_JOINT)
        return dJointGetHingeAngleRate(odeJointId);
    else if(jointType == ODE_Link::SLIDE_JOINT)
        return dJointGetSliderPositionRate(odeJointId);
    else
        return 0;
}

void ODE_Link::setTorque(dReal t){
    if(jointType == ODE_Link::ROTATIONAL_JOINT)
        return dJointAddHingeTorque(odeJointId, t);
    else if(jointType == ODE_Link::SLIDE_JOINT)
        return dJointAddSliderForce(odeJointId, t);
}

const dReal* ODE_Link::getAngularVel(){
    return dBodyGetAngularVel(bodyId);
}

void ODE_Link::getLinearVel(hrp::Vector3& v){
    dVector3 result;
    dBodyGetRelPointVel(bodyId, -C[0], -C[1], -C[2], result);
    v << result[0], result[1], result[2];
}

void ODE_Link::setAbsVelocity(hrp::Vector3& v, hrp::Vector3& w){
    dBodySetAngularVel(bodyId, w[0], w[1], w[2]);
    hrp::Vector3 p;
    hrp::Matrix33 R;
    getTransform(p, R);
    hrp::Vector3 cpos(R*C);
    hrp::Vector3 _v(v + w.cross(cpos));
    dBodySetLinearVel(bodyId, _v[0], _v[1], _v[2]);
}

const dReal* ODE_Link::getForce(){
    return dBodyGetForce(bodyId);
}

const dReal* ODE_Link::getTorque(){
    return dBodyGetTorque(bodyId);
}

void ODE_Link::setForce(double fx, double fy, double fz){
    dBodyAddForce(bodyId, fx, fy, fz);
}

void ODE_Link::setTorque(double tx, double ty, double tz){
    dBodyAddTorque(bodyId, tx, ty, tz);
}

void ODE_Link::destroy()
{
    if(jointType!=FREE_JOINT)
        dJointDestroy(odeJointId);
    for(int i=0; i<geomIds.size(); i++)
        dGeomDestroy(geomIds.at(i));
    if(triMeshDataId)
        dGeomTriMeshDataDestroy(triMeshDataId);
    dBodyDestroy(bodyId);
    ODE_Link* link = static_cast<ODE_Link*>(child);
    while(link){
        ODE_Link* linkToDelete = (ODE_Link*)link;
        link = static_cast<ODE_Link*>(link->sibling);
        linkToDelete->destroy();
    }
}


