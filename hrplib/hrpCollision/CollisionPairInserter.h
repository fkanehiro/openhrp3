/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */

#ifndef HRPCOLLISION_COLLISION_PAIR_INSERTER_H_INCLUDED
#define HRPCOLLISION_COLLISION_PAIR_INSERTER_H_INCLUDED

#include "CollisionData.h"
#include <vector>

namespace Opcode {

    class AABBCollisionNode;
    class MeshInterface;
}
    

namespace hrp {

    class CPIImpl;

    class CollisionPairInserter
    {
      public:
        CollisionPairInserter();
        ~CollisionPairInserter();

        void clear();

        int detectTriTriOverlap(Vector3& P1,
                                Vector3& P2,
                                Vector3& P3,
                                Vector3& Q1,
                                Vector3& Q2,
                                Vector3& Q3,
                                collision_data* col_p);

        int apply(const Opcode::AABBCollisionNode* b1,
                  const Opcode::AABBCollisionNode* b2,
                  int id1, int id2,
                  int num_of_i_points,
                  Vector3 i_points[4],
                  Vector3& n_vector,
                  double depth,
                  Vector3& n1,
                  Vector3& m1,
                  int ctype,
                  Opcode::MeshInterface* mesh1,
                  Opcode::MeshInterface* mesh2);

        std::vector<collision_data>& collisions();

        Matrix33 CD_Rot1;
        Vector3 CD_Trans1;
        double CD_s1;

        Matrix33 CD_Rot2;
        Vector3 CD_Trans2;
        double CD_s2;

      private:
        CPIImpl* impl;
    };
}

#endif
