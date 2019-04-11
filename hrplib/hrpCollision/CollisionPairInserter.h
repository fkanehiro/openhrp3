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

#include "CollisionPairInserterBase.h"

namespace hrp {

    class CollisionPairInserter : public CollisionPairInserterBase
    {
      public:
        CollisionPairInserter();
        virtual ~CollisionPairInserter();
        virtual int detectTriTriOverlap(
            const Vector3& P1,
            const Vector3& P2,
            const Vector3& P3,
            const Vector3& Q1,
            const Vector3& Q2,
            const Vector3& Q3,
            collision_data* col_p);

        virtual int apply(const Opcode::AABBCollisionNode* b1,
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


      private:

        class tri
        {
          public:
            int id;
            Vector3 p1, p2, p3;
        };
        
        class col_tri
        {
          public:
            int status; // 0: unvisited, 1: visited, 2: included in the convex neighbor 
            Vector3 p1, p2, p3;
            Vector3 n;
        };

        static void copy_tri(col_tri* t1, tri* t2);
        
        static void copy_tri(col_tri* t1, col_tri* t2);
        
        static void calc_normal_vector(col_tri* t);
        
        static int is_convex_neighbor(col_tri* t1, col_tri* t2);
        
        void triangleIndexToPoint(ColdetModelSharedDataSet* model, int id, col_tri& tri);
        
        int get_triangles_in_convex_neighbor(ColdetModelSharedDataSet* model, int id, col_tri* tri_convex_neighbor, int max_num);
        
        void get_triangles_in_convex_neighbor(ColdetModelSharedDataSet* model, int id, col_tri* tri_convex_neighbor, std::vector<int>& map, int& count);
        
        void examine_normal_vector(int id1, int id2, int ctype);

        void check_separability(int id1, int id2, int ctype);

        void find_signed_distance(
            Vector3 &signed_distance, col_tri *trp, int nth, int ctype, int obj);

        void find_signed_distance(
            Vector3& signed_distance, const Vector3& vert, int nth, int ctype, int obj);

        void find_signed_distance(Vector3& signed_distance1, ColdetModelSharedDataSet* model0, int id1, int contactIndex, int ctype, int obj);

        int new_point_test(int k);
    };
}

#endif
