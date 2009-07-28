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
#include <boost/numeric/ublas/matrix.hpp>

namespace Opcode {

    typedef boost::numeric::ublas::bounded_matrix
        <double,3,3,boost::numeric::ublas::column_major> dmatrix33;

    class AABBCollisionNode;
    class MeshInterface;

    class CPIImpl;

    class CollisionPairInserter
    {
      public:
        CollisionPairInserter();
        ~CollisionPairInserter();

        void clear();

        int detectTriTriOverlap(dvector3& P1,
                                dvector3& P2,
                                dvector3& P3,
                                dvector3& Q1,
                                dvector3& Q2,
                                dvector3& Q3,
                                collision_data* col_p);

        int apply(const Opcode::AABBCollisionNode* b1,
                  const Opcode::AABBCollisionNode* b2,
                  int id1, int id2,
                  int num_of_i_points,
                  dvector3 i_points[4],
                  dvector3& n_vector,
                  double depth,
                  dvector3& n1,
                  dvector3& m1,
                  int ctype,
                  Opcode::MeshInterface* mesh1,
                  Opcode::MeshInterface* mesh2);

        std::vector<collision_data>& collisions();

        dmatrix33 CD_Rot1;
        dvector3 CD_Trans1;
        double CD_s1;

        dmatrix33 CD_Rot2;
        dvector3 CD_Trans2;
        double CD_s2;

      private:
        CPIImpl* impl;
    };
}

#endif
