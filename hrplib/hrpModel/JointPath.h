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
#include <hrpUtil/Tvmet3d.h>
#include <hrpUtil/uBlasCommonTypes.h>
#include "LinkPath.h"
#include "IInverseKinematics.h"
#include "Config.h"

namespace hrp {

    class HRPMODEL_API JointPath : public IInverseKinematics
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
		
      protected:
		
        virtual void onJointPathUpdated();
		
        double maxIKErrorSqr;
        bool isBestEffortIKMode;
		
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
