/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

/** \file
    \brief The header file of the LinkPath and JointPath classes
    \author Shin'ichiro Nakaoka
*/

#ifndef HRPMODEL_JOINT_PATH_H_INCLUDED
#define HRPMODEL_JOINT_PATH_H_INCLUDED

#include <boost/shared_ptr.hpp>
#include <hrpUtil/Eigen3d.h>
#include "LinkPath.h"
#include "InverseKinematics.h"
#include "Config.h"

namespace hrp {

    class HRPMODEL_API JointPath : public InverseKinematics
    {
      public:
		
        JointPath();
        JointPath(Link* base, Link* end);
        JointPath(Link* end);
        virtual ~JointPath();
		
        bool find(Link* base, Link* end);
        bool find(Link* end);

        inline bool empty() const {
            return joints.empty();
        }
		
        inline int numJoints() const {
            return joints.size();
        }
		
        inline Link* joint(int index) const {
            return joints[index];
        }

        inline Link* baseLink() const {
            return linkPath.baseLink();
        }

        inline Link* endLink() const {
            return linkPath.endLink();
        }
        
        inline bool isJointDownward(int index) const {
            return (index >= numUpwardJointConnections);
        }

        inline void calcForwardKinematics(bool calcVelocity = false, bool calcAcceleration = false) const {
            linkPath.calcForwardKinematics(calcVelocity, calcAcceleration);
        }
		
        void calcJacobian(dmatrix& out_J) const;
		
        inline dmatrix Jacobian() const {
            dmatrix J;
            calcJacobian(J);
            return J;
        }

        // InverseKinematics Interface
        virtual void setMaxIKError(double e);
        virtual void setBestEffortIKMode(bool on);
        virtual bool calcInverseKinematics(const Vector3& end_p, const Matrix33& end_R);
        virtual bool hasAnalyticalIK();

        bool calcInverseKinematics(
            const Vector3& base_p, const Matrix33& base_R, const Vector3& end_p, const Matrix33& end_R);
		
        virtual bool calcJacobianInverseNullspace(dmatrix &J, dmatrix &Jinv, dmatrix &Jnull);
        virtual bool calcInverseKinematics2Loop(const Vector3& dp, const Vector3& omega, const double LAMBDA, const double avoid_gain = 0.0, const double reference_gain = 0.0, const dvector* reference_q = NULL);
        virtual bool calcInverseKinematics2(const Vector3& end_p, const Matrix33& end_R, const double avoid_gain = 0.0, const double reference_gain = 0.0, const dvector* reference_q = NULL);
        double getSRGain() { return sr_gain; }
        virtual bool setSRGain(double g) { sr_gain = g; }
        virtual double getManipulabilityLimit() { return manipulability_limit; }
        virtual bool setManipulabilityLimit(double l) { manipulability_limit = l; }
        virtual bool setManipulabilityGain(double l) { manipulability_gain = l; }
        virtual void setMaxIKError(double epos, double erot);
        virtual void setMaxIKIteration(int iter);

      protected:
		
        virtual void onJointPathUpdated();
		
        double maxIKErrorSqr;
        bool isBestEffortIKMode;
		
        double maxIKPosErrorSqr, maxIKRotErrorSqr;
        int maxIKIteration;
        std::vector<double> avoid_weight_gain;
	double sr_gain, manipulability_limit, manipulability_gain;

      private:
		
        void initialize();
        void extractJoints();

        LinkPath linkPath;
        std::vector<Link*> joints;
        int numUpwardJointConnections;
    };

    typedef boost::shared_ptr<JointPath> JointPathPtr;
	
};


#endif
