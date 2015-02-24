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
    manipulability_limit = 0.1;
    manipulability_gain = 0.001;
    maxIKPosErrorSqr = 1.0e-8;
    maxIKRotErrorSqr = 1.0e-6:
    maxIKIteration = 50;
    isBestEffortIKMode = false;
    avoid_weight_gain.resize(numJoints());
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
                out_J.col(i) << dp, omega;
            }
            break;
				
            case Link::SLIDE_JOINT:
            {
                Vector3 dp(link->R * link->d);
                if(!isJointDownward(i)){
                    dp *= -1.0;
                }
                out_J.col(i) << dp, Vector3::Zero();
            }
            break;
				
            default:
                out_J.col(i).setZero();
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
        Vector3 omega(target->R * omegaFromRot(target->R.transpose() * end_R));

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

        v << dp, omega;
		
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


void JointPath::setMaxIKError(double epos, double erot) {
  maxIKPosErrorSqr = epos*epos;
  maxIKRotErrorSqr = erot*erot;
}

void JointPath::setMaxIKError(double e)
{
    maxIKErrorSqr = e * e;
}

void JointPath::setMaxIKIteration(int iter) {
    maxIKIteration = iter;
}

bool JointPath::calcJacobianInverseNullspace(dmatrix &J, dmatrix &Jinv, dmatrix &Jnull) {
    const int n = numJoints();
                
    hrp::dmatrix w = hrp::dmatrix::Identity(n,n);
    //
    // wmat/weight: weighting joint angle weight
    //
    // w_i = 1 + | dH/dt |      if d|dH/dt| >= 0
    //     = 1                  if d|dH/dt| <  0
    // dH/dt = (t_max - t_min)^2 (2t - t_max - t_min)
    //         / 4 (t_max - t)^2 (t - t_min)^2
    //
    // T. F. Chang and R.-V. Dubey: "A weighted least-norm solution based
    // scheme for avoiding joint limits for redundant manipulators", in IEEE
    // Trans. On Robotics and Automation, 11((2):286-292, April 1995.
    //
    for ( int j = 0; j < n ; j++ ) {
        double jang = joints[j]->q;
        double jmax = joints[j]->ulimit;
        double jmin = joints[j]->llimit;
        double e = deg2rad(1);
        if ( eps_eq(jang, jmax,e) && eps_eq(jang, jmin,e) ) {
        } else if ( eps_eq(jang, jmax,e) ) {
            jang = jmax - e;
        } else if ( eps_eq(jang, jmin,e) ) {
            jang = jmin + e;
        }

        double r;
        if ( eps_eq(jang, jmax,e) && eps_eq(jang, jmin,e) ) {
            r = DBL_MAX;
        } else {
            r = fabs( (pow((jmax - jmin),2) * (( 2 * jang) - jmax - jmin)) /
                      (4 * pow((jmax - jang),2) * pow((jang - jmin),2)) );
            if (isnan(r)) r = 0;
        }

        if (( r - avoid_weight_gain[j] ) >= 0 ) {
	  w(j, j) = ( 1.0 / ( 1.0 + r) );
	} else {
	  w(j, j) = 1.0;
	}
        avoid_weight_gain[j] = r;
    }
    if ( DEBUG ) {
        std::cerr << " cost :";
        for(int j = 0; j < n; j++ ) { std::cerr << std::setw(8) << std::setiosflags(std::ios::fixed) << std::setprecision(4) << avoid_weight_gain[j]; }
        std::cerr << std::endl;
        std::cerr << "    w :";
        for(int j = 0; j < n; j++ ) { std::cerr << std::setw(8) << std::setiosflags(std::ios::fixed) << std::setprecision(4) << w(j, j); }
        std::cerr << std::endl;
    }

    calcJacobian(J);

    double manipulability = sqrt((J*J.transpose()).determinant());
    double k = 0;
    if ( manipulability < manipulability_limit ) {
	k = manipulability_gain * pow((1 - ( manipulability / manipulability_limit )), 2);
    }
    if ( DEBUG ) {
	std::cerr << " manipulability = " <<  manipulability << " < " << manipulability_limit << ", k = " << k << " -> " << sr_gain * k << std::endl;
    }

    calcSRInverse(J, Jinv, sr_gain * k, w);

    Jnull = ( hrp::dmatrix::Identity(n, n) - Jinv * J);

    return true;
}

bool JointPath::calcInverseKinematics2Loop(const Vector3& dp, const Vector3& omega,
                                             const double LAMBDA, const double avoid_gain, const double reference_gain, const hrp::dvector* reference_q) {
    const int n = numJoints();

    if ( DEBUG ) {
        std::cerr << "angle :";
        for(int j=0; j < n; ++j){
            std::cerr << std::setw(8) << std::setiosflags(std::ios::fixed) << std::setprecision(4) << rad2deg(joints[j]->q);
        }
        std::cerr << endl;
    }
    dvector v(6);
    v << dp, omega;

    hrp::dmatrix J(6, n);
    hrp::dmatrix Jinv(n, 6);
    hrp::dmatrix Jnull(n, n);

    calcJacobianInverseNullspace(J, Jinv, Jnull);

    hrp::dvector dq(n);
    dq = Jinv * v; // dq = pseudoInverse(J) * v

    if ( DEBUG ) {
        std::cerr << "    v :";
        for(int j=0; j < 6; ++j){
            std::cerr << " " << v(j);
        }
        std::cerr << std::endl;
        std::cerr << "    J :" << std::endl << J;
        std::cerr << " Jinv :" << std::endl << Jinv;
    }
    // If avoid_gain is set, add joint limit avoidance by null space vector
    if ( avoid_gain > 0.0 ) {
      // dq = J#t a dx + ( I - J# J ) Jt b dx
      // avoid-nspace-joint-limit: avoiding joint angle limit
      //
      // dH/dq = (((t_max + t_min)/2 - t) / ((t_max - t_min)/2)) ^2
      hrp::dvector u(n);
      for ( int j = 0; j < n ; j++ ) {
        double jang = joint(j)->q;
        double jmax = joint(j)->ulimit;
        double jmin = joint(j)->llimit;
        double r = ((( (jmax + jmin) / 2.0) - jang) / ((jmax - jmin) / 2.0));
        if ( r > 0 ) { r = r*r; } else { r = - r*r; }
        u[j] = avoid_gain * r;
      }
      if ( DEBUG ) {
        std::cerr << " u(jl):";
        for(int j=0; j < n; ++j){
          std::cerr << std::setw(8) << std::setiosflags(std::ios::fixed) << std::setprecision(4) << rad2deg(u(j));
        }
        std::cerr << std::endl;
        std::cerr << " JN*u :";
        hrp::dvector Jnullu = Jnull * u;
        for(int j=0; j < n; ++j){
          std::cerr << std::setw(8) << std::setiosflags(std::ios::fixed) << std::setprecision(4) << rad2deg(Jnullu(j));
        }
        std::cerr << std::endl;
      }
      dq = dq + Jnull * u;
    }
    // If reference_gain and reference_q are set, add following to reference_q by null space vector
    if ( reference_gain > 0.0 && reference_q != NULL ) {
      //
      // qref - qcurr
      hrp::dvector u(n);
      for ( int j = 0; j < numJoints(); j++ ) {
        u[j] = reference_gain * ( (*reference_q)[joint(j)->jointId] - joint(j)->q );
      }
      if ( DEBUG ) {
        std::cerr << "u(ref):";
        for(int j=0; j < n; ++j){
          std::cerr << std::setw(8) << std::setiosflags(std::ios::fixed) << std::setprecision(4) << rad2deg(u(j));
        }
        std::cerr << std::endl;
        std::cerr << "  JN*u:";
        hrp::dvector nullu = Jnull * u;
        for(int j=0; j < n; ++j){
          std::cerr << std::setw(8) << std::setiosflags(std::ios::fixed) << std::setprecision(4) << rad2deg(nullu(j));
        }
        std::cerr << std::endl;
      }
      dq = dq + Jnull * u;
    }
    if ( DEBUG ) {
      std::cerr << "   dq :";
      for(int j=0; j < n; ++j){
        std::cerr << std::setw(8) << std::setiosflags(std::ios::fixed) << std::setprecision(4) << rad2deg(dq(j));
      }
      std::cerr << std::endl;
    }

    // default servoErrorLimit in RobotHardware(DEFAULT_ANGLE_ERROR_LIMIT) = 0.2[rad]
    double max_speed = 0;
    for(int j=0; j < n; ++j){
      max_speed = std::max(max_speed, fabs(dq(j)));
    }
    if ( max_speed > 0.2*0.5 ) { // 0.5 safety margin
      if ( DEBUG ) {
        std::cerr << "spdlmt: ";
        for(int j=0; j < n; ++j) { std::cerr << dq(j) << " "; } std::cerr << std::endl;
      }
      for(int j=0; j < n; ++j) {
        dq(j) = dq(j) * 0.2*0.5 / max_speed;
      }
      if ( DEBUG ) {
        std::cerr << "spdlmt: ";
        for(int j=0; j < n; ++j) { std::cerr << dq(j) << " "; } std::cerr << std::endl;
      }
    }

    // check nan / inf
    bool solve_linear_equation = true;
    for(int j=0; j < n; ++j){
      if ( isnan(dq(j)) || isinf(dq(j)) ) {
        solve_linear_equation = false;
        break;
      }
    }
    if ( ! solve_linear_equation ) {
      std::cerr << "ERROR nan/inf is found" << std::endl;
      return false;
    }

    // joint angles update
    for(int j=0; j < n; ++j){
      joints[j]->q += LAMBDA * dq(j);
    }

    // upper/lower limit check
    for(int j=0; j < n; ++j){
      if ( joints[j]->q > joints[j]->ulimit) {
        std::cerr << "Upper joint limit error " << joints[j]->name << std::endl;
        joints[j]->q = joints[j]->ulimit;
      }
      if ( joints[j]->q < joints[j]->llimit) {
        std::cerr << "Lower joint limit error " << joints[j]->name << std::endl;
        joints[j]->q = joints[j]->llimit;
      }
      joints[j]->q = std::max(joints[j]->q, joints[j]->llimit);
    }

    calcForwardKinematics();

    return true;
}


bool JointPath::calcInverseKinematics2(const Vector3& end_p, const Matrix33& end_R,
                                         const double avoid_gain, const double reference_gain, const hrp::dvector* reference_q)
{
    static const int MAX_IK_ITERATION = maxIKIteration;
    static const double LAMBDA = 0.9;

    LinkPath linkPath(baseLink(), endLink());

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
    dvector qorg(n);

    Link* target = linkPath.endLink();

    for(int i=0; i < n; ++i){
        qorg[i] = joints[i]->q;
        avoid_weight_gain[i] = 100000000000000000000.0;
    }

    
    double errsqr = DBL_MAX;//maxIKErrorSqr * 100.0;
    double errsqr0 = errsqr;
    bool converged = false;

    int iter = 0;
    for(iter = 0; iter < MAX_IK_ITERATION; iter++){
        
      if ( DEBUG ) {
        std::cerr << " iter : " << iter << " / " << MAX_IK_ITERATION << ", n = " << n << std::endl;
      }
        
      Vector3 dp(end_p - target->p);
      Vector3 omega(target->R * omegaFromRotEx(target->R.transpose() * end_R));
      if ( dp.norm() > 0.1 ) dp = dp*0.1/dp.norm();
      if ( omega.norm() > 0.5 ) omega = omega*0.5/omega.norm();


      if ( DEBUG ) {
        std::cerr << "   dp : " << dp[0] << " " << dp[1] << " " << dp[2] << std::endl;
        std::cerr << "omega : " << omega[0] << " " << omega[1] << " " << omega[2] << std::endl;
        //std::cerr << "    J :" << std::endl << J;
        //std::cerr << "  det : " << det(J) << std::endl;
        std::cerr << "  err : dp = " << dp.dot(dp) << ", omega = " <<  omega.dot(omega) << std::endl;
      }

      if(isBestEffortIKMode){
        errsqr0 = errsqr;
        errsqr = dp.dot(dp) + omega.dot(omega);
        if ( DEBUG ) std::cerr << "  err : fabs(" << std::setw(18) << std::setiosflags(std::ios::fixed) << std::setprecision(14) << errsqr << " - " << errsqr0 << ") = " << fabs(errsqr-errsqr0) << " < " << maxIKErrorSqr << " BestEffortIKMode" << std::endl;
        if(fabs(errsqr - errsqr0) < maxIKErrorSqr){
          converged = true;
          break;
        }
      } else {
        if ( DEBUG ) std::cerr << "  err : " << std::setw(18) << std::setiosflags(std::ios::fixed) << std::setprecision(14) << sqrt(dp.dot(dp)) << " < " << sqrt(maxIKPosErrorSqr) << ", " << std::setw(18) << std::setiosflags(std::ios::fixed) << std::setprecision(14) << sqrt(omega.dot(omega)) << " < " << sqrt(maxIKRotErrorSqr) << std::endl;
        if( (dp.dot(dp) < maxIKPosErrorSqr) && (omega.dot(omega) < maxIKRotErrorSqr) ) {
          converged = true;
          break;
        }
      }

      if ( !calcInverseKinematics2Loop(dp, omega, LAMBDA, avoid_gain, reference_gain, reference_q) )
        return false;
    }

    if(!converged){
      std::cerr << "IK Fail, iter = " << iter << std::endl;
      Vector3 dp(end_p - target->p);
      Vector3 omega(target->R * omegaFromRotEx(target->R.transpose() * end_R));
      const double errsqr = dp.dot(dp) + omega.dot(omega);
      if(isBestEffortIKMode){
        std::cerr << "  err : fabs(" << errsqr << " - " << errsqr0 << ") = " << fabs(errsqr-errsqr0) << " < " << maxIKErrorSqr << " BestEffortIKMode" << std::endl;
      } else {
          std::cerr << "  err : " << dp.dot(dp) << " ( " << dp[0] << " " << dp[1] << " " << dp[2] << ") < " << maxIKPosErrorSqr << std::endl;
          std::cerr << "      : " << omega.dot(omega) << " ( " << omega[0] << " " << omega[1] << " " << omega[2] << ") < " << maxIKRotErrorSqr << std::endl;
      }
      for(int i=0; i < n; ++i){
        joints[i]->q = qorg[i];
      }
      calcForwardKinematics();
    }
    
    return converged;
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
