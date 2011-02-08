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

#ifndef HRPCOLLISION_COLDET_MODEL_PAIR_H_INCLUDED
#define HRPCOLLISION_COLDET_MODEL_PAIR_H_INCLUDED

#include "config.h"
#include "CollisionData.h"
#include "ColdetModel.h"
#include "CollisionPairInserterBase.h"
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

        ColdetModel* model(int index) { return models[index].get(); }

        std::vector<collision_data>& detectCollisions() {
            return detectCollisionsSub(true);
        }

        std::vector<collision_data>& collisions() {
            return collisionPairInserter->cdContact;
        }

        void clearCollisions(){
            collisionPairInserter->cdContact.clear();
        }

        bool checkCollision() {
            return !detectCollisionsSub(false).empty();
        }

        double computeDistance(double *point0, double *point1);

        /**
           @param out_triangle0, out_triangle1 Indices of the triangle pair that are originally registered by ColdeModel::setTraiangle().
           @param out_point0, out_point1 The closest points 
        */
        double computeDistance(int& out_triangle0, double* out_point0, int& out_triangle1, double* out_point1);

        bool detectIntersection();

        double tolerance() const { return tolerance_; }

        void setCollisionPairInserter(CollisionPairInserterBase *inserter); 

      private:
        std::vector<collision_data>& detectCollisionsSub(bool detectAllContacts);
        bool detectMeshMeshCollisions(bool detectAllContacts);
        bool detectPlaneCylinderCollisions(bool detectAllContacts);

        ColdetModelPtr models[2];
        double tolerance_;

        CollisionPairInserterBase *collisionPairInserter;

        int boxTestsCount;
        int triTestsCount;
    };

    typedef boost::intrusive_ptr<ColdetModelPair> ColdetModelPairPtr;
}


#endif
