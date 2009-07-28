/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

/** 
    \author Shin'ichiro Nakaoka
*/

#ifndef HRPMODEL_I_INVERSE_KINEMATICS_H_INCLUDED
#define HRPMODEL_I_INVERSE_KINEMATICS_H_INCLUDED

#include <boost/shared_ptr.hpp>
#include <hrpUtil/Tvmet3d.h>

namespace hrp {

    class IInverseKinematics
    {
      public:
        virtual ~IInverseKinematics() { }
        virtual bool calcInverseKinematics(const Vector3& end_p, const Matrix33& end_R) = 0;
    };

    typedef boost::shared_ptr<IInverseKinematics> IInverseKinematicsPtr;
}

#endif
