#include <iostream>
#include "SSVTreeCollider.h"
#include "DistFuncs.h"


SSVTreeCollider::SSVTreeCollider() : mMinD(-1)
{
}

float SSVTreeCollider::Distance(BVTCache& cache, Point &point0, Point&point1,
                                const Matrix4x4* world0, const Matrix4x4* world1)
{
    // Checkings
    if(!cache.Model0 || !cache.Model1)								return false;
    if(cache.Model0->HasLeafNodes()!=cache.Model1->HasLeafNodes())	return false;
    if(cache.Model0->IsQuantized()!=cache.Model1->IsQuantized())	return false;

    // Checkings
    if(!Setup(cache.Model0->GetMeshInterface(), cache.Model1->GetMeshInterface()))	return false;
    
    // Simple double-dispatch
    const AABBCollisionTree* T0 = (const AABBCollisionTree*)cache.Model0->GetTree();
    const AABBCollisionTree* T1 = (const AABBCollisionTree*)cache.Model1->GetTree();
    return Distance(T0, T1, world0, world1, &cache, point0, point1);
}

float SSVTreeCollider::Distance(const AABBCollisionTree* tree0, 
                                const AABBCollisionTree* tree1, 
                                const Matrix4x4* world0, const Matrix4x4* world1, 
                                Pair* cache, Point &point0, Point&point1)
{
    // Init collision query
    InitQuery(world0, world1);
    
    // Compute initial value using temporal coherency
    // todo : cache should be used 

    const AABBCollisionNode *n;
    for (unsigned int i=0; i<tree0->GetNbNodes(); i++){
        n = tree0->GetNodes()+i;
        if (n->IsLeaf()){
            mId0 = n->GetPrimitive();
            break;
        }
    } 
    for (unsigned int i=0; i<tree1->GetNbNodes(); i++){
        n = tree1->GetNodes()+i;
        if (n->IsLeaf()){
            mId1 = n->GetPrimitive();
            break;
        }
    } 
    Point p0, p1;
    mMinD = PrimDist(mId0, mId1, p0, p1);
    
    // Perform distance computation
    _Distance(tree0->GetNodes(), tree1->GetNodes(), p0, p1);

    // transform points
    TransformPoint4x3(point0, p0, *world1);
    TransformPoint4x3(point1, p1, *world1);

    // update cache
    cache->id0 = mId0;
    cache->id1 = mId1;
}

float SSVTreeCollider::PssPssDist(float r0, const Point& center0, float r1, const Point& center1)
{
    Point c0;
    TransformPoint(c0, center0, mR0to1, mT0to1);
    return (center1-c0).Magnitude() - r0 - r1;
}

float SSVTreeCollider::_Distance(const AABBCollisionNode* b0, const AABBCollisionNode* b1,
                                 Point& point0, Point& point1)
{
    mNowNode0 = b0;
    mNowNode1 = b1;
    float d;

    // Perform BV-BV distance test
    d = PssPssDist(sqrtf(b0->GetSize()), b0->mAABB.mCenter, 
                   sqrtf(b1->GetSize()), b1->mAABB.mCenter);

    if(d > mMinD) return d;
    
    if(b0->IsLeaf() && b1->IsLeaf()) { 
        Point p0, p1;
        d = PrimDist(b0->GetPrimitive(), b1->GetPrimitive(), p0, p1);
        if (d < mMinD){
            mMinD = d;
            point0 = p0;
            point1 = p1;
            mId0 = b0->GetPrimitive();
            mId1 = b1->GetPrimitive(); 
        }
        return d;  
    }
    
    if(b1->IsLeaf() || (!b0->IsLeaf() && (b0->GetSize() > b1->GetSize())))
	{
            _Distance(b0->GetNeg(), b1, point0, point1);
            _Distance(b0->GetPos(), b1, point0, point1);
	}
    else
	{
            _Distance(b0, b1->GetNeg(), point0, point1);
            _Distance(b0, b1->GetPos(), point0, point1);
	}
}

float SSVTreeCollider::PrimDist(udword id0, udword id1, Point& point0, Point& point1)
{
    // Request vertices from the app
    VertexPointers VP0;
    VertexPointers VP1;
    mIMesh0->GetTriangle(VP0, id0);
    mIMesh1->GetTriangle(VP1, id1);
    
    // Modified by S-cubed, Inc.
    // Transform from space 0 (old : 1) to space 1 (old : 0)
    // CD では変換が逆なのであわせる。
    Point u0,u1,u2;
    TransformPoint(u0, *VP0.Vertex[0], mR0to1, mT0to1);
    TransformPoint(u1, *VP0.Vertex[1], mR0to1, mT0to1);
    TransformPoint(u2, *VP0.Vertex[2], mR0to1, mT0to1);

    // Perform triangle-triangle distance test
    return TriTriDist(u0, u1, u2,
                      *VP1.Vertex[0], *VP1.Vertex[1], *VP1.Vertex[2],
                      point0, point1);
}
