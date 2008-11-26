#ifndef __SSV_TREE_COLLIDER_H__
#define __SSV_TREE_COLLIDER_H__

#include "Opcode/Opcode.h"

using namespace Opcode;

class SSVTreeCollider : public AABBTreeCollider {
public:
    SSVTreeCollider();
    ~SSVTreeCollider(){};

    bool Distance(BVTCache& cache, float& minD, Point &point0, Point&point1,
                  const Matrix4x4* world0=null, const Matrix4x4* world1=null);
private:
    void Distance(const AABBCollisionTree* tree0, 
                  const AABBCollisionTree* tree1, 
                  const Matrix4x4* world0, const Matrix4x4* world1, 
                  Pair* cache, float& minD,  Point &point0, Point&point1);

    void _Distance(const AABBCollisionNode* b0, const AABBCollisionNode* b1,
                   float& minD, Point& point0, Point& point1);
    float PrimDist(udword id0, udword id1, Point& point0, Point& point1);
    float PssPssDist(float r0, const Point& center0, float r1, const Point& center1);

    float mMinD;
};

#endif
