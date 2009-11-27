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


#include "ColdetModel.h"
#include "ColdetModelSharedDataSet.h"

#include "Opcode/Opcode.h"


using namespace std;
using namespace hrp;


ColdetModel::ColdetModel()
{
    dataSet = new ColdetModelSharedDataSet();
    isValid_ = false;
    initialize();
}


ColdetModel::ColdetModel(const ColdetModel& org)
    : name_(org.name_),
      isValid_(org.isValid_)
{
    dataSet = org.dataSet;
    initialize();
}


void ColdetModel::initialize()
{
    dataSet->refCounter++;

    transform = new IceMaths::Matrix4x4();
    transform->Identity();

    pTransform = new IceMaths::Matrix4x4();
    pTransform->Identity();
}


ColdetModelSharedDataSet::ColdetModelSharedDataSet()
{
    refCounter = 0;
    pType = ColdetModel::SP_MESH;
    AABBTreeMaxDepth=0;
}    


ColdetModel::~ColdetModel()
{
    if(--dataSet->refCounter <= 0){
        delete dataSet;
    }
    delete pTransform;
    delete transform;
}


void ColdetModel::setNumVertices(int n)
{
    dataSet->vertices.resize(n);
}


int ColdetModel::getNumVertices() const
{
    return dataSet->vertices.size();
}


void ColdetModel::setNumTriangles(int n)
{
    dataSet->triangles.resize(n);
}


int ColdetModel::getNumTriangles() const
{
    return dataSet->triangles.size();
}

        
void ColdetModel::setVertex(int index, float x, float y, float z)
{
    dataSet->vertices[index].Set(x, y, z);
}


void ColdetModel::addVertex(float x, float y, float z)
{
    dataSet->vertices.push_back(IceMaths::Point(x, y, z));
}

        
void ColdetModel::getVertex(int index, float& x, float& y, float& z) const
{
    const Point& v = dataSet->vertices[index];
    x = v.x;
    y = v.y;
    z = v.z;
}

        
void ColdetModel::setTriangle(int index, int v1, int v2, int v3)
{
    udword* mVRef = dataSet->triangles[index].mVRef;
    mVRef[0] = v1;
    mVRef[1] = v2;
    mVRef[2] = v3;
}


void ColdetModel::addTriangle(int v1, int v2, int v3)
{
    dataSet->triangles.push_back(IceMaths::IndexedTriangle(v1, v2, v3));
}


void ColdetModel::build()
{
    isValid_ = dataSet->build();
    /*
    unsigned int maxDepth = dataSet->getAABBTreeDepth();
    for(unsigned int i=0; i<maxDepth; i++){
       vector<IceMaths::Point> data = getBoundingBoxData(i);   
       cout << "depth= " << i << endl;
       for(vector<IceMaths::Point>::iterator it=data.begin(); it!=data.end(); it++){
            cout << (*it).x << " " << (*it).y << " " << (*it).z << endl;
       }
   }
   */
}


bool ColdetModelSharedDataSet::build()
{
    bool result = false;
    
    if(triangles.size() > 0){

        Opcode::OPCODECREATE OPCC;

        iMesh.SetPointers(&triangles[0], &vertices[0]);
        iMesh.SetNbTriangles(triangles.size());
        iMesh.SetNbVertices(vertices.size());
        
        OPCC.mIMesh = &iMesh;
        
        OPCC.mNoLeaf = false;
        OPCC.mQuantized = false;
        OPCC.mKeepOriginal = false;
        
        model.Build(OPCC);

        AABBTreeMaxDepth = computeDepth(((Opcode::AABBCollisionTree*)model.GetTree())->GetNodes(), 0, -1) + 1;
        for(int i=0; i<AABBTreeMaxDepth; i++)
            for(int j=0; j<i; j++)
                numBBMap.at(i) += numLeafMap.at(j);

        result = true;
    }

    return result;
}

int ColdetModel::numofBBtoDepth(int minNumofBB){
    for(int i=0; i<getAABBTreeDepth(); i++)
        if(minNumofBB <= dataSet->getNumofBB(i))
            return i;
    return getAABBTreeDepth();
}

int ColdetModel::getAABBTreeDepth(){
    return dataSet->getAABBTreeDepth();
}

int ColdetModel::getAABBmaxNum(){
    return dataSet->getmaxNumofBB();
}

vector<IceMaths::Point> ColdetModel::getBoundingBoxData(const int depth){
    const Opcode::AABBCollisionNode* rootNode=((Opcode::AABBCollisionTree*)dataSet->model.GetTree())->GetNodes();
    vector<IceMaths::Point> data;
    getBoundingBoxData(rootNode, 0, depth, data);
    return data;
}

void ColdetModel::getBoundingBoxData(const Opcode::AABBCollisionNode* node, unsigned int currentDepth, unsigned int depth, std::vector<IceMaths::Point>& data){
    if(currentDepth == depth || node->IsLeaf() ){
        IceMaths::Point p = node->mAABB.mCenter;
        data.push_back(p);
        p = node->mAABB.mExtents;
        data.push_back(p);
    }
    currentDepth++;
    if(currentDepth > depth) return;
    if(!node->IsLeaf()){
        getBoundingBoxData(node->GetPos(), currentDepth, depth, data);
        getBoundingBoxData(node->GetNeg(), currentDepth, depth, data);
    }
}

int ColdetModelSharedDataSet::computeDepth(const Opcode::AABBCollisionNode* node, int currentDepth, int max )
{
    /*
	cout << "depth= " << currentDepth << " ";
    Point p = node->mAABB.mCenter;
    cout << p.x << " " << p.y << " " << p.z << "     ";
    p = node->mAABB.mExtents;
    cout << p.x << " " << p.y << " " << p.z << " ";
    if(node->IsLeaf()) cout << "is Leaf " ;
    cout << endl;
    */
    if(max < currentDepth){
        max = currentDepth;
        numBBMap.push_back(0);
        numLeafMap.push_back(0);
    }
    numBBMap.at(currentDepth)++;

    if(!node->IsLeaf()){
        currentDepth++;
        max = computeDepth(node->GetPos(), currentDepth, max);
        max = computeDepth(node->GetNeg(), currentDepth, max);
    }else
        numLeafMap.at(currentDepth)++;

    return max;
}

void ColdetModel::setPosition(const Matrix33& R, const Vector3& p)
{
    transform->Set((float)R(0,0), (float)R(1,0), (float)R(2,0), 0.0f,
                   (float)R(0,1), (float)R(1,1), (float)R(2,1), 0.0f,
                   (float)R(0,2), (float)R(1,2), (float)R(2,2), 0.0f,
                   (float)p(0),   (float)p(1),   (float)p(2),   1.0f);
}


void ColdetModel::setPosition(const double* R, const double* p)
{
    transform->Set((float)R[0], (float)R[3], (float)R[6], 0.0f,
                   (float)R[1], (float)R[4], (float)R[7], 0.0f,
                   (float)R[2], (float)R[5], (float)R[8], 0.0f,
                   (float)p[0], (float)p[1], (float)p[2], 1.0f);
}

void ColdetModel::setPrimitiveType(PrimitiveType ptype)
{
    dataSet->pType = ptype;
}

ColdetModel::PrimitiveType ColdetModel::getPrimitiveType() const
{
    return dataSet->pType;
}

void ColdetModel::setNumPrimitiveParams(unsigned int nparam)
{
    dataSet->pParams.resize(nparam);
}

bool ColdetModel::setPrimitiveParam(unsigned int index, float value)
{
    if (index >= dataSet->pParams.size()) return false;

    dataSet->pParams[index] = value;
    return true;
}

bool ColdetModel::getPrimitiveParam(unsigned int index, float& value) const
{
    if (index >= dataSet->pParams.size()) return false;

    value = dataSet->pParams[index];
    return true;
}

void ColdetModel::setPrimitivePosition(const double* R, const double* p)
{
    pTransform->Set((float)R[0], (float)R[3], (float)R[6], 0.0f,
                    (float)R[1], (float)R[4], (float)R[7], 0.0f,
                    (float)R[2], (float)R[5], (float)R[8], 0.0f,
                    (float)p[0], (float)p[1], (float)p[2], 1.0f);
}

double ColdetModel::computeDistanceWithRay(const double *point, 
                                           const double *dir)
{
    Opcode::RayCollider RC;
    Ray world_ray(Point(point[0], point[1], point[2]),
                  Point(dir[0], dir[1], dir[2]));
    Opcode::CollisionFace CF;
    Opcode::SetupClosestHit(RC, CF);
    udword Cache;
    RC.Collide(world_ray, dataSet->model, transform, &Cache);
    if (CF.mDistance == FLT_MAX){
        return 0;
    }else{
        return CF.mDistance;
    }
}

