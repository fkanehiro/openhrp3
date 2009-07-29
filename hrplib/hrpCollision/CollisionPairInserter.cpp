/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */

#include "CollisionPairInserter.h"
#include "Opcode/Opcode.h"
#include <cstdio>
#include <iostream>

using namespace std;
using namespace Opcode;
using namespace hrp;


int tri_tri_overlap(
    const Vector3& P1,
    const Vector3& P2,
    const Vector3& P3,
    const Vector3& Q1,
    const Vector3& Q2,
    const Vector3& Q3,
    collision_data* col_p,
    CollisionPairInserter* collisionPairInserter);
    

namespace {
    const bool COLLIDE_DEBUG = false;
    const double MAX_DEPTH = 0.1;

    const int CD_OK = 0;
    const int CD_ALL_CONTACTS = 1;
    const int CD_FIRST_CONTACT = 2;
    const int CD_ERR_COLLIDE_OUT_OF_MEMORY = 2;
    
    enum {
        FV = 1,
        VF,
        EE
    };
}


CollisionPairInserter::CollisionPairInserter()
{

}


CollisionPairInserter::~CollisionPairInserter()
{

}


void CollisionPairInserter::copy_tri(col_tri* t1, tri* t2)
{
    t1->p1 = t2->p1;
    t1->p2 = t2->p2;
    t1->p3 = t2->p3;
}


void CollisionPairInserter::copy_tri(col_tri* t1, col_tri* t2)
{
    t1->p1 = t2->p1;
    t1->p2 = t2->p2;
    t1->p3 = t2->p3;

    if(t2->n[0] && t2->n[1] && t2->n[2]){
        t1->n = t2->n;
    }
}


void CollisionPairInserter::calc_normal_vector(col_tri* t)
{
    if(t->status == 0){
        const Vector3 e1(t->p2 - t->p1);
        const Vector3 e2(t->p3 - t->p2);
        t->n = normalize(Vector3(cross(e1, e2)));
        t->status = 1;
    }
}


int CollisionPairInserter::is_convex_neighbor(col_tri* t1, col_tri* t2)
{
    const double EPS = 1.0e-12; // a small number
        
    calc_normal_vector(t2);
        
    // printf("normal vector1 = %f %f %f\n", t1->n[0], t1->n[1], t1->n[2]);
    // printf("normal vector2 = %f %f %f\n", t2->n[0], t2->n[1], t2->n[2]);
        
    const Vector3 vec1(t2->p1 - t1->p1);
    const Vector3 vec2(t2->p2 - t1->p2);
    const Vector3 vec3(t2->p3 - t1->p3);
        
    // printf("is_convex_neighbor = %f %f %f\n",innerProd(t1->n,vec1),innerProd(t1->n,vec2),innerProd(t1->n,vec3));
        
    if(dot(t2->n, vec1) < EPS && dot(t2->n, vec2) < EPS && dot(t2->n, vec3) < EPS){
        return 1;
    } else {
        return 0;
    }
}


int CollisionPairInserter::identical_ver(const Vector3& v1, const Vector3& v2)
{
    int num = 0;
    const double EPS = 1.0e-6; // 1 micro meter
        
    if(fabs(v1[0]-v2[0]) < EPS) ++num;
    if(fabs(v1[1]-v2[1]) < EPS) ++num;
    if(fabs(v1[2]-v2[2]) < EPS) ++num;
        
    return (num==3) ? 1 : 0;
}


int CollisionPairInserter::is_neighboring_triangle(col_tri* t1, col_tri* t2)
{
    int num = 
        identical_ver(t1->p1,t2->p1)+identical_ver(t1->p1,t2->p2)+identical_ver(t1->p1,t2->p3)+
        identical_ver(t1->p2,t2->p1)+identical_ver(t1->p2,t2->p2)+identical_ver(t1->p2,t2->p3)+
        identical_ver(t1->p3,t2->p1)+identical_ver(t1->p3,t2->p2)+identical_ver(t1->p3,t2->p3);
        
    // printf("is_neighboring_triangle = %d\n", num);
        
    return (num==2) ? 1 : 0;
}


void CollisionPairInserter::get_neighboring_triangles(
    col_tri* tri_convex_neighbor, col_tri* tri_neighbor,
    int* start_tri, int* end_tri, int num_tri)
{
    int i, j, m, m_previous;
        
    m = *end_tri;
    for(i=*start_tri; i<*end_tri; ++i){
        m_previous = m;
        for(j=*end_tri; j<num_tri; ++j){
            if(tri_neighbor[j].status != 2 &&
               is_neighboring_triangle(&tri_convex_neighbor[i], &tri_neighbor[j]) &&
               is_convex_neighbor(&tri_convex_neighbor[i], &tri_neighbor[j])){
                tri_neighbor[j].status = 2;
                copy_tri(&tri_convex_neighbor[m], &tri_neighbor[j]);
                ++m;
            }
            if(m==m_previous+2) break;
        }
    }
        
    *start_tri = *end_tri;
    *end_tri   = m;
}


int CollisionPairInserter::get_triangles_in_convex_neighbor(
    tri* root, col_tri* tri_convex_neighbor, col_tri* tri_neighbor,
    int num_tri, int max_num)
{
    int i, start_tri, end_tri;
    copy_tri(&tri_convex_neighbor[0], root);
    tri_convex_neighbor[0].status = 0;
    calc_normal_vector(&tri_convex_neighbor[0]);
        
    start_tri = 0; end_tri = 1;
    for(i=0; i<max_num; ++i){
        if(start_tri < end_tri)
            get_neighboring_triangles(tri_convex_neighbor, tri_neighbor, &start_tri, &end_tri, num_tri);
    }
        
    return end_tri;
}


int CollisionPairInserter::get_triangles_in_convex_neighbor(
    tri* root, col_tri* tri_convex_neighbor, col_tri* tri_neighbor, int num_tri)
{
    const int MAX_EXPANSION_NUM = 3;
    return get_triangles_in_convex_neighbor(root, tri_convex_neighbor, tri_neighbor, num_tri, MAX_EXPANSION_NUM);
}


void CollisionPairInserter::get_triangles_in_neighbor(
    col_tri* neighbor_tris,
    int* n,
    const Opcode::AABBCollisionNode* root,
    Opcode::MeshInterface* mesh)
{
    if(root->IsLeaf()){
        //三角形のデータを変換する。
        tri t;
        Opcode::VertexPointers vp;
        mesh->GetTriangle(vp, (root->GetPrimitive()));
        t.p1[0] = (double)vp.Vertex[0]->x; t.p1[1] = (double)vp.Vertex[0]->y; t.p1[2] = (double)vp.Vertex[0]->z;
        t.p2[0] = (double)vp.Vertex[1]->x; t.p2[1] = (double)vp.Vertex[1]->y; t.p2[2] = (double)vp.Vertex[1]->z;
        t.p3[0] = (double)vp.Vertex[2]->x; t.p3[1] = (double)vp.Vertex[2]->y; t.p3[2] = (double)vp.Vertex[2]->z;
        copy_tri(&neighbor_tris[*n], &t);
        *n += 1;
    } else {
        if(root->GetPos()) get_triangles_in_neighbor(neighbor_tris, n, root->GetPos(), mesh);
        if(root->GetNeg()) get_triangles_in_neighbor(neighbor_tris, n, root->GetNeg(), mesh);
    }
}


int CollisionPairInserter::count_num_of_triangles(const Opcode::AABBCollisionNode* root)
{
    int num_p = 0;
    int num_n = 0;

    if(root->IsLeaf())
        return 1;
    else{
        if(root->GetPos()) num_p = count_num_of_triangles(root->GetPos());
        if(root->GetNeg()) num_n = count_num_of_triangles(root->GetNeg());
    }

    return num_p + num_n;
}


void CollisionPairInserter::examine_normal_vector(
    const Opcode::AABBCollisionNode* b1,
    const Opcode::AABBCollisionNode* b2,
    int ctype,
    Opcode::MeshInterface* mesh1,
    Opcode::MeshInterface* mesh2)
{
    static const int NUM_TRI = 10; // number of the neighbors of the neighbors
    static const int MAX_BACKTRACK_LEVEL = 3; // ascend the box tree by backtrack_level times at most

    //
    // get the root node of each neighbor
    //
    const Opcode::AABBCollisionNode* root_of_neighbor1 = (Opcode::AABBCollisionNode*)b1;

    int num_tri1 = 0;
    int k = 0;

    while(num_tri1 < NUM_TRI && k < MAX_BACKTRACK_LEVEL){
        ++k;
        for(int i=0; i < k; ++i){
            if(root_of_neighbor1 != root_of_neighbor1->GetB())
		root_of_neighbor1 = root_of_neighbor1->GetB();
        }
	num_tri1 = count_num_of_triangles(root_of_neighbor1);
        if(COLLIDE_DEBUG) printf("num of triangles1 = %d\n", num_tri1);
    }
    const Opcode::AABBCollisionNode* root_of_neighbor2 = (Opcode::AABBCollisionNode*)b2;
    int num_tri2 = 0;
    k = 0;
    while(num_tri2 < NUM_TRI && k < MAX_BACKTRACK_LEVEL){
        ++k;
        for(int i=0; i < k; ++i){
            if(root_of_neighbor2 != root_of_neighbor2->GetB())
		root_of_neighbor2 = root_of_neighbor2->GetB();
        }
        num_tri2 = count_num_of_triangles(root_of_neighbor2);
        if(COLLIDE_DEBUG) printf("num of triangles2 = %d\n", num_tri2);
    }
    check_separability(b1, root_of_neighbor1, num_tri1, b2, root_of_neighbor2, num_tri2, ctype, mesh1, mesh2); 
}


void CollisionPairInserter::check_separability(
    const Opcode::AABBCollisionNode* b1,
    const Opcode::AABBCollisionNode* root1,
    int num_tri1,
    const Opcode::AABBCollisionNode* b2,
    const Opcode::AABBCollisionNode* root2,
    int num_tri2,
    int ctype,
    Opcode::MeshInterface* mesh1,
    Opcode::MeshInterface* mesh2)
{
    int contactIndex = cdContact.size() - 1;
    Vector3 signed_distance;
    Vector3 signed_distance1(99999999.0);
    Vector3 signed_distance2(-99999999.0);

    // signed_distance is positive when a vertex is outside b2
    find_signed_distance(signed_distance1, b1, root1, num_tri1, contactIndex, ctype,1, mesh1);
    find_signed_distance(signed_distance2, b2, root2, num_tri2, contactIndex, ctype,2, mesh2);

    int max = (2 < ctype) ? ctype : 2;
    
    for(int i=0; i < max; ++i){
        signed_distance[i] = signed_distance1[i] - signed_distance2[i];
        if(COLLIDE_DEBUG) printf("signed distance %d = %f\n", i, signed_distance[i]);
    }

    switch(ctype){

    case FV:
        if(signed_distance[0] < signed_distance[1]){
            cdContact[contactIndex].n_vector = cdContact[contactIndex].m;
            cdContact[contactIndex].depth = fabs(signed_distance[1]);
            if(COLLIDE_DEBUG) printf("normal replaced\n");
        } else {
            cdContact[contactIndex].depth = fabs(signed_distance[0]);
        }
        break;
        
    case VF:
        if(signed_distance[0] < signed_distance[1]){
            cdContact[contactIndex].n_vector = - cdContact[contactIndex].n;
            cdContact[contactIndex].depth = fabs(signed_distance[1]);
            if(COLLIDE_DEBUG) printf("normal replaced\n");
        } else{
            cdContact[contactIndex].depth = fabs(signed_distance[0]);
        }
        break;
        
    case EE:
        cdContact[contactIndex].num_of_i_points = 1;
        if(signed_distance[0] < signed_distance[1] && signed_distance[2] <= signed_distance[1]){
            cdContact[contactIndex].n_vector = cdContact[contactIndex].m;
            cdContact[contactIndex].depth = fabs(signed_distance[1]);
            if(COLLIDE_DEBUG) printf("normal replaced\n");
        } else if(signed_distance[0] < signed_distance[2] && signed_distance[1] < signed_distance[2]){
            cdContact[contactIndex].n_vector = - cdContact[contactIndex].n;
            cdContact[contactIndex].depth = fabs(signed_distance[2]);
            if(COLLIDE_DEBUG) printf("normal replaced\n");
        } else {
            cdContact[contactIndex].depth = fabs(signed_distance[0]);
            // cout << "depth in InsertCollisionPair.cpp = " << signed_distance[0] << endl;
        }
        cdContact[contactIndex].i_points[0] += cdContact[contactIndex].i_points[1];
        cdContact[contactIndex].i_points[0] *= 0.5;
        break;
    }
    
    if(COLLIDE_DEBUG){
        printf("final normal = %f %f %f\n", cdContact[contactIndex].n_vector[0],
               cdContact[contactIndex].n_vector[1], cdContact[contactIndex].n_vector[2]);
    }
    if(COLLIDE_DEBUG){
        for(int i=0; i < cdContact[contactIndex].num_of_i_points; ++i){
            cout << "final depth = " << cdContact[contactIndex].depth << endl;
            cout << "final i_point = " << cdContact[contactIndex].i_points[i][0] << " "
                 << cdContact[contactIndex].i_points[i][1] << " " << cdContact[contactIndex].i_points[i][2]
                 << endl;
        }
    }
    
    if(COLLIDE_DEBUG) cout << endl;
}


void CollisionPairInserter::find_signed_distance(
    Vector3& signed_distance, col_tri* trp, int nth, int ctype, int obj)
{
    find_signed_distance(signed_distance, trp->p1, nth, ctype, obj);
    find_signed_distance(signed_distance, trp->p2, nth, ctype, obj);
    find_signed_distance(signed_distance, trp->p3, nth, ctype, obj);
}


void CollisionPairInserter::find_signed_distance(
    Vector3& signed_distance, const Vector3& vert, int nth, int ctype, int obj)
{
    Vector3 vert_w;
    if(obj==1){
        vert_w = CD_s1 * Vector3(CD_Rot1 * vert + CD_Trans1);
    } else {
        vert_w = CD_s2 * Vector3(CD_Rot2 * vert + CD_Trans2);
    }
        
    if(COLLIDE_DEBUG) printf("vertex = %f %f %f\n", vert_w[0], vert_w[1], vert_w[2]);
        
    // use the first intersecting point to find the distance
    const Vector3 vec(vert_w - cdContact[nth].i_points[0]);
    //vecNormalize(cdContact[nth].n_vector);
    tvmet::alias(cdContact[nth].n_vector) = normalize(cdContact[nth].n_vector);
        
    double dis0 = dot(cdContact[nth].n_vector, vec);
        
#if 0
    switch(ctype){
    case FV:
        if(dot(cdContact[nth].n_vector, cdContact[nth].n) > 0.0) dis0 = - dis0;
        break;
    case VF:
        if(dot(cdContact[nth].n_vector, cdContact[nth].m) < 0.0) dis0 = - dis0;
        break;
    case EE:
        if(dot(cdContact[nth].n_vector, cdContact[nth].n) > 0.0 ||
           dot(cdContact[nth].n_vector, cdContact[nth].m) < 0.0)
            dis0 = - dis0;
    }
#endif
        
    if(COLLIDE_DEBUG) printf("dis0 = %f\n", dis0);
        
    double dis1 = dis0;
    double dis2 = dis0;
        
    switch(ctype){
    case FV:
        dis1 =   dot(cdContact[nth].m, vec);
        if(COLLIDE_DEBUG) printf("dis1 = %f\n", dis1);
        break;
    case VF:
        dis1 = - dot(cdContact[nth].n, vec);
        if(COLLIDE_DEBUG) printf("dis1 = %f\n", dis1);
        break;
    case EE:
        dis1 =   dot(cdContact[nth].m, vec);
        dis2 = - dot(cdContact[nth].n, vec);
        if(COLLIDE_DEBUG){
            printf("dis1 = %f\n", dis1);
            printf("dis2 = %f\n", dis2);
        }
    }

    if(COLLIDE_DEBUG) printf("obj = %d\n", obj);
    if(obj == 1){
        if(dis0 < signed_distance[0]) signed_distance[0] = dis0;
        if(dis1 < signed_distance[1]) signed_distance[1] = dis1;
        if(ctype==EE)
            if(dis2 < signed_distance[2]) signed_distance[2] = dis2;
    }
    else{
        if(signed_distance[0] < dis0) signed_distance[0] = dis0;
        if(signed_distance[1] < dis1) signed_distance[1] = dis1;
        if(ctype==EE)
            if(signed_distance[2] < dis2) signed_distance[2] = dis2;
    }
}


void CollisionPairInserter::find_signed_distance(
    Vector3& signed_distance,
    const Opcode::AABBCollisionNode* b1,
    const Opcode::AABBCollisionNode* root,
    int num_tri,
    int contactIndex,
    int ctype,
    int obj,
    Opcode::MeshInterface* mesh
    )
{
    int num;
        
    col_tri* tri_neighbor = new col_tri[num_tri];
    col_tri* tri_convex_neighbor = new col_tri[num_tri];

    for(int i=0; i<num_tri; ++i){
        tri_neighbor[i].status = 0;
    }

    // collect triangles in the neighborhood
    int n = 0;
    get_triangles_in_neighbor(tri_neighbor, &n, root, mesh);
    //tri型に変換してから関数を呼ぶ
    tri t;
    Opcode::VertexPointers vp;
    mesh->GetTriangle(vp, (b1->GetPrimitive()));
    t.p1[0] = vp.Vertex[0]->x; t.p1[1] = vp.Vertex[0]->y; t.p1[2] = vp.Vertex[0]->z;
    t.p2[0] = vp.Vertex[1]->x; t.p2[1] = vp.Vertex[1]->y; t.p2[2] = vp.Vertex[1]->z;
    t.p3[0] = vp.Vertex[2]->x; t.p3[1] = vp.Vertex[2]->y; t.p3[2] = vp.Vertex[2]->z;
  
    // get the triangles in the convex neighbor of the root triangle and their normal vectors
    num = get_triangles_in_convex_neighbor(&t, tri_convex_neighbor, tri_neighbor, num_tri);

    // if(COLLIDE_DEBUG) printf("num of triangles in convex neighbor = %d\n", num);

    // note that num = num of convex neighbor + 1
    for(int i=0; i<num; ++i){
        find_signed_distance(signed_distance, &tri_convex_neighbor[i], contactIndex, ctype, obj);
    }

    delete [] tri_neighbor;
    delete [] tri_convex_neighbor;
}


int CollisionPairInserter::new_point_test(int k)
{
    const double eps = 1.0e-12; // 1 micro meter to judge two contact points are identical

    int last = cdContact.size();
    
    for(int i=0; i < last; ++i){
        for(int j=0; j < cdContact[i].num_of_i_points; ++j){
            Vector3 dv(cdContact[i].i_points[j] - cdContact[last].i_points[k]);
            double d = cdContact[i].depth - cdContact[last].depth;
            if(dot(dv, dv) < eps && d*d < eps) return 0;
        }
    }
    return 1;
}


//
// obsolute signatures
//
int CollisionPairInserter::apply(
    const Opcode::AABBCollisionNode* b1,
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
    Opcode::MeshInterface* mesh2)
{
    cdContact.push_back(collision_data());
    collision_data& contact = cdContact.back();
    
    contact.id1 = id1;
    contact.id2 = id2;
    contact.depth = depth;
    contact.num_of_i_points = num_of_i_points;

    if(COLLIDE_DEBUG) printf("num_of_i_points = %d\n", num_of_i_points);

    for(int i=0; i < num_of_i_points; ++i){
        contact.i_points[i] = CD_s2 * Vector3((CD_Rot2 * i_points[i]) + CD_Trans2);
    }

    contact.n_vector = CD_Rot2 * n_vector;
    contact.n = CD_Rot2 * n1;
    contact.m = CD_Rot2 * m1;
    examine_normal_vector(b1,b2,ctype, mesh1, mesh2);

    // analyze_neighborhood_of_i_point(b1, b2, cdContactsCount, ctype);
    // remove the intersecting point if depth is deeper than MAX_DEPTH meter
    if(fabs(contact.depth) < MAX_DEPTH){
        for(int i=0; i < num_of_i_points; ++i){
            contact.i_point_new[i] = new_point_test(i);
        }
    } else {
        for(int i=0; i < num_of_i_points; ++i){
            contact.i_point_new[i] = 0;
        }
    }

    return CD_OK;
}


int CollisionPairInserter::detectTriTriOverlap(
    const Vector3& P1,
    const Vector3& P2,
    const Vector3& P3,
    const Vector3& Q1,
    const Vector3& Q2,
    const Vector3& Q3,
    collision_data* col_p)
{
    return tri_tri_overlap(P1, P2, P3, Q1, Q2, Q3, col_p, this);
}
