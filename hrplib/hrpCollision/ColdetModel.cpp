
/**
   @author Shin'ichiro Nakaoka
*/


#include "ColdetModel.h"
#include "ColdetModelSharedDataSet.h"

#include "Opcode.h"


using namespace std;
using namespace hrp;


ColdetModel::ColdetModel()
{
    dataSet = new ColdetModelSharedDataSet();
    isValid_ = false;
    initialize();
}


ColdetModel::ColdetModel(const ColdetModel& org)
{
    dataSet = org.dataSet;
    isValid_ = org.isValid_;
    initialize();
}


void ColdetModel::initialize()
{
    dataSet->refCounter++;

    transform = new IceMaths::Matrix4x4();

    transform->Set(1.0f, 0.0f, 0.0f, 0.0f,
                   0.0f, 1.0f, 0.0f, 0.0f,
                   0.0f, 0.0f, 1.0f, 0.0f,
                   0.0f, 0.0f, 0.0f, 1.0f);
}


ColdetModelSharedDataSet::ColdetModelSharedDataSet()
{
    refCounter = 0;
}    


ColdetModel::~ColdetModel()
{
    if(--dataSet->refCounter <= 0){
        delete dataSet;
    }
    delete transform;
}


void ColdetModel::setNumVertices(int n)
{
    dataSet->vertices.resize(n);
}


void ColdetModel::setNumTriangles(int n)
{
    dataSet->triangles.resize(n);
}

        
void ColdetModel::setVertex(int index, float x, float y, float z)
{
    dataSet->vertices[index].Set(x, y, z);
}

        
void ColdetModel::setTriangle(int index, int v1, int v2, int v3)
{
    udword* mVRef = dataSet->triangles[index].mVRef;
    mVRef[0] = v1;
    mVRef[1] = v2;
    mVRef[2] = v3;
}


void ColdetModel::build()
{
    isValid_ = dataSet->build();
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
        
        result = true;
    }

    return result;
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
