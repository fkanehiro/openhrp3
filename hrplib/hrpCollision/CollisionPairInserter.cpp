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

#include <cmath>
#include <cstdio>
#include <iostream>

#include "Opcode/Opcode.h"

using namespace std;
using namespace boost;
using namespace Opcode;

int tri_tri_overlap(dvector3 &P1,
                    dvector3 &P2,
                    dvector3 &P3,
                    dvector3 &Q1,
                    dvector3 &Q2,
                    dvector3 &Q3,
                    collision_data *col_p,
                    Opcode::CollisionPairInserter* collisionPairInserter);

namespace {
    const bool COLLIDE_DEBUG = false;
    const double MAX_DEPTH = 0.1;

    inline dvector3 vecProd(const dmatrix33& mat, const dvector3& vec) {
        return numeric::ublas::prod(mat, vec);
    }

    inline void outerProd(dvector3& ret, const dvector3& vec1, const dvector3& vec2) {
        ret[0] = vec1[1]*vec2[2] - vec1[2]*vec2[1];
        ret[1] = vec1[2]*vec2[0] - vec1[0]*vec2[2];
        ret[2] = vec1[0]*vec2[1] - vec1[1]*vec2[0];
    }
        
    inline double innerProd(const dvector3& vec1, const dvector3& vec2){
        return numeric::ublas::inner_prod(vec1, vec2);
    }

    inline void vecNormalize(dvector3& vec) {
        double Vnormalize_d = 1.0 / sqrt(numeric::ublas::inner_prod(vec, vec));
        vec[0] *= Vnormalize_d;
        vec[1] *= Vnormalize_d;
        vec[2] *= Vnormalize_d;
    }

    class tri
    {
    public:
        int id;
        dvector3 p1, p2, p3;
    };
    
    class col_tri
    {
    public:
        int status; // 0: unvisited, 1: visited, 2: included in the convex neighbor 
        dvector3 p1, p2, p3;
        dvector3 n;
    };
    
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

namespace Opcode {

    class CPIImpl
    {
    public:

        CPIImpl(CollisionPairInserter* self);

        CollisionPairInserter* self;

        //int cdNbCollisionPairAlloced;
        //collision_data* cdContact;
        //int cdContactsCount;

        vector<collision_data> cdContact;

        int insert_collision_pair(const Opcode::AABBCollisionNode *b1,
                                  const Opcode::AABBCollisionNode *b2,
                                  int id1, int id2,
                                  int num_of_i_points,
                                  dvector3 i_points[4],
                                  dvector3 &n_vector,
                                  double depth,
                                  dvector3 &n1,
                                  dvector3 &m1,
                                  int ctype,
                                  Opcode::MeshInterface *mesh1,
                                  Opcode::MeshInterface *mesh2);

        int count_num_of_triangles(const Opcode::AABBCollisionNode *root);

        void examine_normal_vector(const Opcode::AABBCollisionNode *b1,
                                   const Opcode::AABBCollisionNode *b2,
                                   int ctype,
                                   Opcode::MeshInterface *mesh1,
                                   Opcode::MeshInterface *mesh2);

        void check_separability(const Opcode::AABBCollisionNode *b1,
                                const Opcode::AABBCollisionNode *root1, int num_tri1,
                                const Opcode::AABBCollisionNode *b2,
                                const Opcode::AABBCollisionNode *root2, int num_tri2,
                                int ctype,
                                Opcode::MeshInterface *mesh1, Opcode::MeshInterface *mesh2);

        void find_signed_distance(dvector3 &signed_distance,
                                  const Opcode::AABBCollisionNode *b1,
                                  const Opcode::AABBCollisionNode *root,
                                  int num_tri,int num_cpair, int ctype, int obj,
                                  Opcode::MeshInterface *mesh);

        void find_signed_distance(dvector3 &signed_distance,
                                  col_tri *trp, int nth, int ctype, int obj);
        
        void find_signed_distance(dvector3 &signed_distance,
                                  dvector3 &vert, int nth, int ctype, int obj);

        void get_triangles_in_neighbor(col_tri *neighbor_tris,
                                       int *n,
                                       const Opcode::AABBCollisionNode *root,
                                       Opcode::MeshInterface *mesh);

        void get_neighboring_triangles(col_tri *tri_convex_neighbor, col_tri *tri_neighbor,
                                       int *start_tri, int *end_tri, int num_tri);

        int get_triangles_in_convex_neighbor(Opcode::AABBCollisionNode *root,
                                             col_tri *tri_convex_neighbor,
                                             col_tri *tri_neighbor,
                                             int num_tri);

        int get_triangles_in_convex_neighbor(tri *root, col_tri *tri_convex_neighbor,
                                             col_tri *tri_neighbor, int num_tri);

        int get_triangles_in_convex_neighbor(tri *root,
                                             col_tri *tri_convex_neighbor,
                                             col_tri *tri_neighbor,
                                             int num_tri,
                                             int max_num);
        
        int is_neighboring_triangle(col_tri *t1, col_tri *t2);
        int is_convex_neighbor(col_tri *t1, col_tri *t2);
        void calc_normal_vector(col_tri *t);
        int identical_ver(dvector3 &v1, dvector3 &v2);
        void copy_tri(col_tri *t1, tri *t2);
        void copy_tri(col_tri *t1, col_tri *t2);
        int new_point_test(int k);
        
    };
}


CollisionPairInserter::CollisionPairInserter()
{
    impl = new CPIImpl(this);
}


CPIImpl::CPIImpl(CollisionPairInserter* self)
    : self(self)
{

}


CollisionPairInserter::~CollisionPairInserter()
{
    delete impl;
}


std::vector<collision_data>& CollisionPairInserter::collisions()
{
    return impl->cdContact;
}


void CollisionPairInserter::clear()
{
    impl->cdContact.clear();
}
    

int CollisionPairInserter::detectTriTriOverlap(dvector3& P1,
                                dvector3& P2,
                                dvector3& P3,
                                dvector3& Q1,
                                dvector3& Q2,
                                dvector3& Q3,
                                collision_data* col_p)
{
    return tri_tri_overlap(P1, P2, P3, Q1, Q2, Q3, col_p, this);
}
    

int CollisionPairInserter::apply(
    const Opcode::AABBCollisionNode *b1,
    const Opcode::AABBCollisionNode *b2,
    int id1, int id2,
    int num_of_i_points,
    dvector3 i_points[4],
    dvector3 &n_vector,
    double depth,
    dvector3 &n1,
    dvector3 &m1,
    int ctype,
    Opcode::MeshInterface *mesh1,
    Opcode::MeshInterface *mesh2)
{
    return impl->insert_collision_pair(
        b1, b2, id1, id2, num_of_i_points, i_points, n_vector,
        depth, n1, m1, ctype, mesh1, mesh2);
}


//
// obsolute signatures
//
int CPIImpl::insert_collision_pair(
    const Opcode::AABBCollisionNode *b1,
    const Opcode::AABBCollisionNode *b2,
    int id1, int id2,
    int num_of_i_points,
    dvector3 i_points[4],
    dvector3 &n_vector,
    double depth,
    dvector3 &n1,
    dvector3 &m1,
    int ctype,
    Opcode::MeshInterface *mesh1,
    Opcode::MeshInterface *mesh2)
{
    int k;
    dvector3 i_points_w[4];
    dvector3 n_vector_w;
    dvector3 n1_w;
    dvector3 m1_w;

    cdContact.push_back(collision_data());
    collision_data& contact = cdContact.back();
    
    contact.id1 = id1;
    contact.id2 = id2;
    contact.depth = depth;
    contact.num_of_i_points = num_of_i_points;
    if(COLLIDE_DEBUG) printf("num_of_i_points = %d\n", num_of_i_points);

    for(k=0;k<num_of_i_points;++k){
        i_points_w[k] = self->CD_s2 * (vecProd(self->CD_Rot2, i_points[k]) + self->CD_Trans2);
        contact.i_points[k] = i_points_w[k];
    }

    n_vector_w = vecProd(self->CD_Rot2,n_vector);
    
    n1_w = vecProd(self->CD_Rot2,n1);
    m1_w = vecProd(self->CD_Rot2,m1);
    contact.n_vector = n_vector_w;
    contact.n = n1_w;
    contact.m = m1_w;
    examine_normal_vector(b1,b2,ctype, mesh1, mesh2);
    // analyze_neighborhood_of_i_point(b1, b2, cdContactsCount, ctype);
    // remove the intersecting point if depth is deeper than MAX_DEPTH meter
    if(fabs(contact.depth)<MAX_DEPTH){
        for(k=0; k<num_of_i_points; ++k){
            contact.i_point_new[k] = new_point_test(k);
        }
    }
    else{
        for(k=0; k<num_of_i_points; ++k);
	contact.i_point_new[k] = 0;
    }

    return CD_OK;
}


void CPIImpl::examine_normal_vector(
    const Opcode::AABBCollisionNode *b1,
    const Opcode::AABBCollisionNode *b2,
    int ctype,
    Opcode::MeshInterface *mesh1,
    Opcode::MeshInterface *mesh2)
{

    int flag, i, k, MAX_BACKTRACK_LEVEL, num_tri1, num_tri2, NUM_TRI;
    const Opcode::AABBCollisionNode *root_of_neighbor1;
    const Opcode::AABBCollisionNode *root_of_neighbor2;
  
    NUM_TRI = 10; // number of the neighbors of the neighbors
    MAX_BACKTRACK_LEVEL = 3; // ascend the box tree by backtrack_level times at most

    flag = 0; // replace the normal vector when flag == 1
    //
    // get the root node of each neighbor
    //
    root_of_neighbor1 = (Opcode::AABBCollisionNode*)b1;
    num_tri1 = 0; k = 0;

    while(num_tri1<NUM_TRI && k < MAX_BACKTRACK_LEVEL){
        ++k;
        for(i=0; i<k; ++i){
            if(root_of_neighbor1 != root_of_neighbor1->GetB())
		root_of_neighbor1 = root_of_neighbor1->GetB();
        }
	num_tri1 = count_num_of_triangles(root_of_neighbor1);
        if(COLLIDE_DEBUG) printf("num of triangles1 = %d\n", num_tri1);
    }
    root_of_neighbor2 = (Opcode::AABBCollisionNode*)b2;
    num_tri2 = 0; k = 0;
    while(num_tri2<NUM_TRI && k < MAX_BACKTRACK_LEVEL){
        ++k;
        for(i=0; i<k; ++i){
            if(root_of_neighbor2 != root_of_neighbor2->GetB())
		root_of_neighbor2 = root_of_neighbor2->GetB();
        }
        num_tri2 = count_num_of_triangles(root_of_neighbor2);
        if(COLLIDE_DEBUG) printf("num of triangles2 = %d\n", num_tri2);
    }
    check_separability(b1,root_of_neighbor1,num_tri1,b2,root_of_neighbor2,num_tri2, ctype, mesh1, mesh2); 
}


int CPIImpl::count_num_of_triangles(const Opcode::AABBCollisionNode *root)
{
    int num_p, num_n;

    num_p = num_n = 0;

    if(root->IsLeaf())
        return 1;
    else{
        if(root->GetPos()) num_p = count_num_of_triangles(root->GetPos());
        if(root->GetNeg()) num_n = count_num_of_triangles(root->GetNeg());
    }

    return num_p + num_n;
}


void CPIImpl::check_separability(
    const Opcode::AABBCollisionNode *b1,
    const Opcode::AABBCollisionNode *root1,
    int num_tri1,
    const Opcode::AABBCollisionNode *b2,
    const Opcode::AABBCollisionNode *root2,
    int num_tri2,
    int ctype,
    Opcode::MeshInterface *mesh1,
    Opcode::MeshInterface *mesh2)
{
    int contactIndex = cdContact.size() - 1;
    int i, k, max;
    dvector3 signed_distance, signed_distance1, signed_distance2;
    for(i=0; i<3; ++i){signed_distance1[i] = 99999999.0; signed_distance2[i] = -99999999.0;}

    // signed_distance is positive when a vertex is outside b2
    find_signed_distance(signed_distance1, b1, root1, num_tri1, contactIndex, ctype,1, mesh1);
    find_signed_distance(signed_distance2, b2, root2, num_tri2, contactIndex, ctype,2, mesh2);
    if(2<ctype) max = ctype; else max = 2;
    for(i=0; i<max; ++i){
        signed_distance[i] = signed_distance1[i] - signed_distance2[i];
        if(COLLIDE_DEBUG) printf("signed distance %d = %f\n", i, signed_distance[i]);
    }

    switch(ctype){
    case FV:
        if(signed_distance[0]<signed_distance[1]){
            cdContact[contactIndex].n_vector = cdContact[contactIndex].m;
            cdContact[contactIndex].depth = fabs(signed_distance[1]);
            if(COLLIDE_DEBUG) printf("normal replaced\n");
        }
        else{
            cdContact[contactIndex].depth = fabs(signed_distance[0]);
        }
        break;
    case VF:
        if(signed_distance[0]<signed_distance[1]){
            cdContact[contactIndex].n_vector = - cdContact[contactIndex].n;
            cdContact[contactIndex].depth = fabs(signed_distance[1]);
            if(COLLIDE_DEBUG) printf("normal replaced\n");
        }
        else{
            cdContact[contactIndex].depth = fabs(signed_distance[0]);
        }
        break;
    case EE:
        cdContact[contactIndex].num_of_i_points = 1;
        if(signed_distance[0]<signed_distance[1] && signed_distance[2]<=signed_distance[1]){
            cdContact[contactIndex].n_vector = cdContact[contactIndex].m;
            cdContact[contactIndex].depth = fabs(signed_distance[1]);
            if(COLLIDE_DEBUG) printf("normal replaced\n");
        }
        else if(signed_distance[0]<signed_distance[2] && signed_distance[1]<signed_distance[2]){
            cdContact[contactIndex].n_vector = - cdContact[contactIndex].n;
            cdContact[contactIndex].depth = fabs(signed_distance[2]);
            if(COLLIDE_DEBUG) printf("normal replaced\n");
        }
        else{
            cdContact[contactIndex].depth = fabs(signed_distance[0]);
            // cout << "depth in InsertCollisionPair.cpp = " << signed_distance[0] << endl;
        }
        cdContact[contactIndex].i_points[0] += cdContact[contactIndex].i_points[1];
        cdContact[contactIndex].i_points[0] *= 0.5;
    }
    if(COLLIDE_DEBUG){
        printf("final normal = %f %f %f\n", cdContact[contactIndex].n_vector[0],
               cdContact[contactIndex].n_vector[1], cdContact[contactIndex].n_vector[2]);
    }
    if(COLLIDE_DEBUG)
        for(k=0; k<cdContact[contactIndex].num_of_i_points; ++k){
            cout << "final depth = " << cdContact[contactIndex].depth << endl;
            cout << "final i_point = " << cdContact[contactIndex].i_points[k][0] << " "
                 << cdContact[contactIndex].i_points[k][1] << " " << cdContact[contactIndex].i_points[k][2]
                 << endl;
        }
    if(COLLIDE_DEBUG) cout << endl;

}


void CPIImpl::find_signed_distance(
    dvector3 &signed_distance,
    const Opcode::AABBCollisionNode *b1,
    const Opcode::AABBCollisionNode *root,
    int num_tri,
    int contactIndex,
    int ctype,
    int obj,
    Opcode::MeshInterface *mesh
    )
{
    int i, n, num;

    col_tri *tri_neighbor = new col_tri[num_tri];
    col_tri *tri_convex_neighbor = new col_tri[num_tri];

    for(i=0; i<num_tri; ++i) tri_neighbor[i].status = 0;

    // collect triangles in the neighborhood
    n = 0;
    get_triangles_in_neighbor(tri_neighbor, &n, root, mesh);
    //tri型に変換してから関数を呼ぶ
    tri t;
    Opcode::VertexPointers vp;
    mesh->GetTriangle(vp, (b1->GetPrimitive()));
    t.p1[0] = vp.Vertex[0]->x;  t.p1[1] = vp.Vertex[0]->y; t.p1[2] = vp.Vertex[0]->z;
    t.p2[0] = vp.Vertex[1]->x;  t.p2[1] = vp.Vertex[1]->y; t.p2[2] = vp.Vertex[1]->z;
    t.p3[0] = vp.Vertex[2]->x;  t.p3[1] = vp.Vertex[2]->y; t.p3[2] = vp.Vertex[2]->z;
  
    // get the triangles in the convex neighbor of the root triangle and their normal vectors
    num = get_triangles_in_convex_neighbor(&t, tri_convex_neighbor, tri_neighbor, num_tri);

    // if(COLLIDE_DEBUG) printf("num of triangles in convex neighbor = %d\n", num);

    // note that num = num of convex neighbor + 1
    for(i=0; i<num; ++i){
        find_signed_distance(signed_distance, &tri_convex_neighbor[i], contactIndex, ctype, obj);
    }


    delete [] tri_neighbor;
    delete [] tri_convex_neighbor;
}


void CPIImpl::find_signed_distance(
    dvector3 &signed_distance, col_tri *trp, int nth, int ctype, int obj)
{
    find_signed_distance(signed_distance, trp->p1, nth, ctype, obj);
    find_signed_distance(signed_distance, trp->p2, nth, ctype, obj);
    find_signed_distance(signed_distance, trp->p3, nth, ctype, obj);
}


void CPIImpl::find_signed_distance(
    dvector3 &signed_distance, dvector3 &vert, int nth, int ctype, int obj)
{
    dvector3 vert_w, vec;
    double dis0, dis1, dis2;
  
    if(obj==1)
        vert_w = self->CD_s1 * (vecProd(self->CD_Rot1, vert) + self->CD_Trans1);
    else
        vert_w = self->CD_s2 * (vecProd(self->CD_Rot2, vert) + self->CD_Trans2);

    if(COLLIDE_DEBUG) printf("vertex = %f %f %f\n", vert_w[0], vert_w[1], vert_w[2]);

    // use the first intersecting point to find the distance
    vec =  vert_w -  cdContact[nth].i_points[0];
    vecNormalize(cdContact[nth].n_vector);

    dis0 = innerProd(cdContact[nth].n_vector, vec);

#if 0
    switch(ctype){
    case FV:
        if(innerProd(cdContact[nth].n_vector,cdContact[nth].n)>0.0) dis0 = - dis0;
        break;
    case VF:
        if(innerProd(cdContact[nth].n_vector,cdContact[nth].m)<0.0) dis0 = - dis0;
        break;
    case EE:
        if(innerProd(cdContact[nth].n_vector,cdContact[nth].n)>0.0 ||
           innerProd(cdContact[nth].n_vector,cdContact[nth].m)<0.0)
            dis0 = - dis0;
    }
#endif

    if(COLLIDE_DEBUG) printf("dis0 = %f\n", dis0);

    dis1 = dis2 = dis0;

    switch(ctype){
    case FV:
        dis1 =   innerProd(cdContact[nth].m, vec);
        if(COLLIDE_DEBUG) printf("dis1 = %f\n", dis1);
        break;
    case VF:
        dis1 = - innerProd(cdContact[nth].n, vec);
        if(COLLIDE_DEBUG) printf("dis1 = %f\n", dis1);
        break;
    case EE:
        dis1 =   innerProd(cdContact[nth].m, vec);
        dis2 = - innerProd(cdContact[nth].n, vec);
        if(COLLIDE_DEBUG){
            printf("dis1 = %f\n", dis1);
            printf("dis2 = %f\n", dis2);
        }
    }

    if(COLLIDE_DEBUG) printf("obj = %d\n", obj);
    if(obj == 1){
        if(dis0<signed_distance[0]) signed_distance[0] = dis0;
        if(dis1<signed_distance[1]) signed_distance[1] = dis1;
        if(ctype==EE)
            if(dis2<signed_distance[2]) signed_distance[2] = dis2;
    }
    else{
        if(signed_distance[0]<dis0) signed_distance[0] = dis0;
        if(signed_distance[1]<dis1) signed_distance[1] = dis1;
        if(ctype==EE)
            if(signed_distance[2]<dis2) signed_distance[2] = dis2;
    }
}


void CPIImpl::get_triangles_in_neighbor(
    col_tri *neighbor_tris,
    int *n,
    const Opcode::AABBCollisionNode *root,
    Opcode::MeshInterface *mesh)
{
    if(root->IsLeaf()){
	//三角形のデータを変換する。
	tri t;
	Opcode::VertexPointers vp;
	mesh->GetTriangle(vp, (root->GetPrimitive()));
	t.p1[0] = (double)vp.Vertex[0]->x;  t.p1[1] = (double)vp.Vertex[0]->y; t.p1[2] = (double)vp.Vertex[0]->z;
	t.p2[0] = (double)vp.Vertex[1]->x;  t.p2[1] = (double)vp.Vertex[1]->y; t.p2[2] = (double)vp.Vertex[1]->z;
	t.p3[0] = (double)vp.Vertex[2]->x;  t.p3[1] = (double)vp.Vertex[2]->y; t.p3[2] = (double)vp.Vertex[2]->z;
        copy_tri(&neighbor_tris[*n], &t);
        *n += 1;
    }
    else{
	if(root->GetPos()) get_triangles_in_neighbor(neighbor_tris, n, root->GetPos(), mesh);
	if(root->GetNeg()) get_triangles_in_neighbor(neighbor_tris, n, root->GetNeg(), mesh);
    }
}


int CPIImpl::get_triangles_in_convex_neighbor(
    tri *root, col_tri *tri_convex_neighbor, col_tri *tri_neighbor, int num_tri)
{
    int MAX_EXPANSION_NUM;

    MAX_EXPANSION_NUM = 3;

    return get_triangles_in_convex_neighbor(root, tri_convex_neighbor, tri_neighbor, num_tri, MAX_EXPANSION_NUM);
}


int CPIImpl::get_triangles_in_convex_neighbor(
    tri *root, col_tri *tri_convex_neighbor, col_tri *tri_neighbor,
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


void CPIImpl::get_neighboring_triangles(
    col_tri *tri_convex_neighbor, col_tri *tri_neighbor,
    int *start_tri, int *end_tri, int num_tri)
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


int CPIImpl::is_neighboring_triangle(col_tri *t1, col_tri *t2)
{
    int num;

    num = 
        identical_ver(t1->p1,t2->p1)+identical_ver(t1->p1,t2->p2)+identical_ver(t1->p1,t2->p3)+
        identical_ver(t1->p2,t2->p1)+identical_ver(t1->p2,t2->p2)+identical_ver(t1->p2,t2->p3)+
        identical_ver(t1->p3,t2->p1)+identical_ver(t1->p3,t2->p2)+identical_ver(t1->p3,t2->p3);

    // printf("is_neighboring_triangle = %d\n", num);

    if(num==2) return 1; else return 0;
}


int CPIImpl::is_convex_neighbor(col_tri *t1, col_tri *t2)
{
    double EPS;
    dvector3 vec1, vec2, vec3;

    EPS = 1.0e-12; // a small number

    calc_normal_vector(t2);

    // printf("normal vector1 = %f %f %f\n", t1->n[0], t1->n[1], t1->n[2]);
    // printf("normal vector2 = %f %f %f\n", t2->n[0], t2->n[1], t2->n[2]);

    vec1 = t2->p1 - t1->p1;
    vec2 = t2->p2 - t1->p2;
    vec3 = t2->p3 - t1->p3;

    // printf("is_convex_neighbor = %f %f %f\n",innerProd(t1->n,vec1),innerProd(t1->n,vec2),innerProd(t1->n,vec3));

    if(innerProd(t2->n,vec1)<EPS && innerProd(t2->n,vec2)<EPS && innerProd(t2->n,vec3)<EPS)
        return 1;
    else
        return 0;
}


void CPIImpl::calc_normal_vector(col_tri *t)
{
    dvector3 e1, e2;

    if(t->status == 0){
        e1 = t->p2 - t->p1;
        e2 = t->p3 - t->p2;
    
        outerProd(t->n, e1, e2);
        vecNormalize(t->n);

        t->status = 1;
    }
}


int CPIImpl::identical_ver(dvector3 &v1, dvector3 &v2)
{
    int num;
    double EPS;

    num = 0;
    EPS = 1.0e-6; // 1 micro meter

    if(fabs(v1[0]-v2[0])<EPS) ++num;
    if(fabs(v1[1]-v2[1])<EPS) ++num;
    if(fabs(v1[2]-v2[2])<EPS) ++num;

    if(num==3) return 1; else return 0;
}


void CPIImpl::copy_tri(col_tri *t1, tri *t2)
{
    t1->p1 = t2->p1;
    t1->p2 = t2->p2;
    t1->p3 = t2->p3;
}


void CPIImpl::copy_tri(col_tri *t1, col_tri *t2)
{
    t1->p1 = t2->p1;
    t1->p2 = t2->p2;
    t1->p3 = t2->p3;

    if(t2->n[0] && t2->n[1] && t2->n[2]){
        t1->n = t2->n;
    }
}

#if 1
int CPIImpl::new_point_test(int k)
{
    int i,j;
    double eps,x,y,z,d;

    eps = 1.0e-12; // 1 micro meter to judge two contact points are identical

    int last = cdContact.size();
    
    for(i=0; i<last; ++i){
        for(j=0; j<cdContact[i].num_of_i_points; ++j){
            x = cdContact[i].i_points[j][0]-cdContact[last].i_points[k][0];
            y = cdContact[i].i_points[j][1]-cdContact[last].i_points[k][1];
            z = cdContact[i].i_points[j][2]-cdContact[last].i_points[k][2];
            d = cdContact[i].depth-cdContact[last].depth;
            if(x*x+y*y+z*z<eps && d*d<eps) return 0;
        }
    }
    return 1;
}
#endif

#if 0
int CPIImpl::new_point_test(int num, int k)
{
    int i,j, val;
    double eps,x,y,z;

    eps = 1.0e-12; // 1 micro meter to judge two contact points are identical

    val = 1;
    for(i=0; i<num; ++i){
        for(j=0; j<cdContact[i].num_of_i_points; ++j){
            x = cdContact[i].i_points[j][0]-cdContact[num].i_points[k][0];
            y = cdContact[i].i_points[j][1]-cdContact[num].i_points[k][1];
            z = cdContact[i].i_points[j][2]-cdContact[num].i_points[k][2];
            if(x*x+y*y+z*z<eps)
                if(cdContact[num].depth<cdContact[i].depth)
                    cdContact[i].i_point_new[j] = 0;
                else
                    val = 0;
        }
    }
    return val;
}
#endif
