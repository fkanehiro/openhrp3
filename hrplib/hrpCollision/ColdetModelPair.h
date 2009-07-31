/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */
/**
   @author Shin'ichiro Nakaoka
*/

#ifndef OPENHRP_COLDET_MODEL_PAIR_H_INCLUDED
#define OPENHRP_COLDET_MODEL_PAIR_H_INCLUDED

#include "config.h"
#include "CollisionData.h"
#include "ColdetModel.h"
#include "CollisionPairInserter.h"
#include <vector>
#include <hrpUtil/Referenced.h>

namespace hrp {

    class HRP_COLLISION_EXPORT ColdetModelPair : public Referenced
    {
      public:
        ColdetModelPair();
        ColdetModelPair(ColdetModelPtr model0, ColdetModelPtr model1,
                        double tolerance=0);
        ColdetModelPair(const ColdetModelPair& org);
        virtual ~ColdetModelPair();

        void set(ColdetModelPtr model0, ColdetModelPtr model1);
        ColdetModelPtr& model0() { return model0_; }
        ColdetModelPtr& model1() { return model1_; }

        std::vector<collision_data>& detectCollisions() {
            return detectCollisionsSub(true);
        }

        std::vector<collision_data>& collisions() {
            return collisionPairInserter.cdContact;
        }

        bool checkCollision() {
            return !detectCollisionsSub(false).empty();
        }

        double computeDistance(double *point0, double *point1);

        bool detectIntersection();

        double tolerance() const { return tolerance_; }

      private:
        std::vector<collision_data>& detectCollisionsSub(bool detectAllContacts);
        bool detectMeshMeshCollisions(bool detectAllContacts);
        bool detectPlaneCylinderCollisions(bool detectAllContacts);
        
        ColdetModelPtr model0_;
        ColdetModelPtr model1_;
        double tolerance_;

        CollisionPairInserter collisionPairInserter;

        int boxTestsCount;
        int triTestsCount;
    };

    typedef boost::intrusive_ptr<ColdetModelPair> ColdetModelPairPtr;
}


#endif
