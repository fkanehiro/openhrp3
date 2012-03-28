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
   \brief Implementations of the LinkPath class
   \author Shin'ichiro Nakaoka
*/
  
#include "JointPath.h"
#include "Link.h"
#include <hrpUtil/MatrixSolvers.h>
#include <algorithm>

using namespace std;
using namespace hrp;


JointPath::JointPath()
{
    initialize();
}


JointPath::JointPath(Link* base, Link* end)
    : linkPath(base, end), 
      joints(linkPath.size())
{
    initialize();
    extractJoints();
}


JointPath::JointPath(Link* end)
    : linkPath(end), 
      joints(linkPath.size())
{
    initialize();
    extractJoints();
}


void JointPath::initialize()
{
    maxIKErrorSqr = 1.0e-6 * 1.0e-6;
    isBestEffortIKMode = false;
}
	

JointPath::~JointPath()
{

}


bool JointPath::find(Link* base, Link* end)
{
    if(linkPath.find(base, end)){
        extractJoints();
    }
    onJointPathUpdated();

    return (!joints.empty());
}


bool JointPath::find(Link* end)
{
    linkPath.find(end);
    extractJoints();
    onJointPathUpdated();
	
    return !joints.empty();
}


void JointPath::extractJoints()
{
    numUpwardJointConnections = 0;

    int n = linkPath.size();
    if(n <= 1){
        joints.clear();
    } else {
        int i = 0;
        if(linkPath.isDownward(i)){
            i++;
        }
        joints.resize(n); // reserve size n buffer
        joints.clear();
        int m = n - 1;
        while(i < m){
            Link* link = linkPath[i];
            if(link->jointId >= 0){
                if(link->jointType == Link::ROTATIONAL_JOINT || link->jointType == Link::SLIDE_JOINT){
                    joints.push_back(link);
                    if(!linkPath.isDownward(i)){
                        numUpwardJointConnections++;
                    }
                }
            }
            ++i;
        }
        if(linkPath.isDownward(m-1)){
            Link* link = linkPath[m];
            if(link->jointId >= 0){
                if(link->jointType == Link::ROTATIONAL_JOINT || link->jointType == Link::SLIDE_JOINT){
                    joints.push_back(link);
                }
            }
        }
    }
}


void JointPath::onJointPathUpdated()
{

}


void JointPath::calcJacobian(dmatrix& out_J) const
{
    const int n = joints.size();
    out_J.resize(6, n);
	
    if(n > 0){
		
        Link* targetLink = linkPath.endLink();
		
        for(int i=0; i < n; ++i){
			
            Link* link = joints[i];
			
            switch(link->jointType){
				
            case Link::ROTATIONAL_JOINT:
            {
                Vector3 omega(link->R * link->a);
                Vector3 arm(targetLink->p - link->p);
                if(!isJointDownward(i)){
                    omega *= -1.0;
                } 
                Vector3 dp(omega.cross(arm));
                setVector3(dp,    out_J, 0, i);
                setVector3(omega, out_J, 3, i);
            }
            break;
				
            case Link::SLIDE_JOINT:
            {
                Vector3 dp(link->R * link->d);
                if(!isJointDownward(i)){
                    dp *= -1.0;
                }
                setVector3(dp, out_J, 0, i);
                out_J(3, i) = 0.0;
                out_J(4, i) = 0.0;
                out_J(5, i) = 0.0;
            }
            break;
				
            default:
                for(int j=0; j < 6; ++j){
                    out_J(j, i) = 0.0;
                }
            }
        }
    }
}


void JointPath::setMaxIKError(double e)
{
    maxIKErrorSqr = e * e;
}


void JointPath::setBestEffortIKMode(bool on)
{
    isBestEffortIKMode = on;
}


bool JointPath::calcInverseKinematics
(const Vector3& base_p, const Matrix33& base_R, const Vector3& end_p, const Matrix33& end_R)
{
    Link* baseLink = linkPath.baseLink();
    baseLink->p = base_p;
    baseLink->R = base_R;

    if(!hasAnalyticalIK()){
        calcForwardKinematics();
    }
	
    return calcInverseKinematics(end_p, end_R);
}


bool JointPath::calcInverseKinematics(const Vector3& end_p, const Matrix33& end_R)
{
    static const int MAX_IK_ITERATION = 50;
    static const double LAMBDA = 0.9;
    
    if(joints.empty()){
        if(linkPath.empty()){
            return false;
        }
        if(baseLink() == endLink()){
            baseLink()->p = end_p;
            baseLink()->R = end_R;
            return true;
        } else {
            // \todo implement here
            return false;
        }
    }
    
    const int n = numJoints();

    Link* target = linkPath.endLink();

    std::vector<double> qorg(n);
    for(int i=0; i < n; ++i){
        qorg[i] = joints[i]->q;
    }

    dmatrix J(6, n);
    dvector dq(n);
    dvector v(6);

    double errsqr = maxIKErrorSqr * 100.0;
    bool converged = false;

    for(int i=0; i < MAX_IK_ITERATION; i++){
	
        calcJacobian(J);
	
        Vector3 dp(end_p - target->p);
        Vector3 omega(target->R * omegaFromRot(Matrix33(target->R.transpose() * end_R)));

        if(isBestEffortIKMode){
            const double errsqr0 = errsqr;
            errsqr = dp.dot(dp) + omega.dot(omega);
            if(fabs(errsqr - errsqr0) < maxIKErrorSqr){
                converged = true;
                break;
            }
        } else {
            const double errsqr = dp.dot(dp) + omega.dot(omega);
            if(errsqr < maxIKErrorSqr){
                converged = true;
                break;
            }
        }

        setVector3(dp   , v, 0);
        setVector3(omega, v, 3);
		
        if(n == 6){ 
            solveLinearEquationLU(J, v, dq);
        } else {
            solveLinearEquationSVD(J, v, dq);  // dq = pseudoInverse(J) * v
        }
		
        for(int j=0; j < n; ++j){
            joints[j]->q += LAMBDA * dq(j);
        }

        calcForwardKinematics();
    }

    if(!converged){
        for(int i=0; i < n; ++i){
            joints[i]->q = qorg[i];
        }
        calcForwardKinematics();
    }
    
    return converged;
}


bool JointPath::hasAnalyticalIK()
{
    return false;
}


std::ostream& operator<<(std::ostream& os, JointPath& path)
{
    int n = path.numJoints();
    for(int i=0; i < n; ++i){
        Link* link = path.joint(i);
        os << link->name;
        if(i != n){
            os << (path.isJointDownward(i) ? " => " : " <= ");
        }
    }
    os << std::endl;
    return os;
}
