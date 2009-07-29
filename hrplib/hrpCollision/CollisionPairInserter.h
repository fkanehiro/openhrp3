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

    class CollisionPairInserter
    {
      public:
        CollisionPairInserter();
        ~CollisionPairInserter();

        void clear(){
            cdContact.clear();
        }

        int detectTriTriOverlap(
            const Vector3& P1,
            const Vector3& P2,
            const Vector3& P3,
            const Vector3& Q1,
            const Vector3& Q2,
            const Vector3& Q3,
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

        std::vector<collision_data>& collisions() {
            return cdContact;
        }

        Matrix33 CD_Rot1;
        Vector3 CD_Trans1;
        double CD_s1;

        Matrix33 CD_Rot2;
        Vector3 CD_Trans2;
        double CD_s2;

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

        std::vector<collision_data> cdContact;

        static void copy_tri(col_tri* t1, tri* t2);
        
        static void copy_tri(col_tri* t1, col_tri* t2);
        
        static void calc_normal_vector(col_tri* t);
        
        static int is_convex_neighbor(col_tri* t1, col_tri* t2);
        
        static int identical_ver(const Vector3& v1, const Vector3& v2);
        
        static int is_neighboring_triangle(col_tri* t1, col_tri* t2);
        
        static void get_neighboring_triangles(
            col_tri* tri_convex_neighbor, col_tri* tri_neighbor,
            int* start_tri, int* end_tri, int num_tri);
        
        static int get_triangles_in_convex_neighbor(
            tri* root, col_tri* tri_convex_neighbor, col_tri* tri_neighbor,
            int num_tri, int max_num);
        
        static int get_triangles_in_convex_neighbor(
            tri* root, col_tri* tri_convex_neighbor, col_tri* tri_neighbor, int num_tri);
        
        static void get_triangles_in_neighbor(
            col_tri* neighbor_tris,
            int* n,
            const Opcode::AABBCollisionNode* root,
            Opcode::MeshInterface* mesh);

        static int count_num_of_triangles(const Opcode::AABBCollisionNode* root);

        void examine_normal_vector(
            const Opcode::AABBCollisionNode* b1,
            const Opcode::AABBCollisionNode* b2,
            int ctype,
            Opcode::MeshInterface* mesh1,
            Opcode::MeshInterface* mesh2);

        void check_separability(
            const Opcode::AABBCollisionNode* b1,
            const Opcode::AABBCollisionNode* root1, int num_tri1,
            const Opcode::AABBCollisionNode* b2,
            const Opcode::AABBCollisionNode* root2, int num_tri2,
            int ctype,
            Opcode::MeshInterface* mesh1, Opcode::MeshInterface* mesh2);

        void find_signed_distance(
            Vector3 &signed_distance, col_tri *trp, int nth, int ctype, int obj);

        void find_signed_distance(
            Vector3& signed_distance, const Vector3& vert, int nth, int ctype, int obj);

        void find_signed_distance(
            Vector3& signed_distance,
            const Opcode::AABBCollisionNode* b1,
            const Opcode::AABBCollisionNode* root,
            int num_tri,
            int contactIndex,
            int ctype,
            int obj,
            Opcode::MeshInterface* mesh);

        int new_point_test(int k);
    };
}

#endif
