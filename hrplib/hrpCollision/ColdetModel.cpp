
/**
   @author Shin'ichiro Nakaoka
*/


#include "ColdetModel.h"
#include "Opcode.h"
#include <vector>


using namespace std;
using namespace hrp;


namespace hrp {

    class ColdetModelImpl
    {
    public:
        ColdetModelImpl();

        void update();

        // need two instances ?
        Opcode::Model model;

	Opcode::MeshInterface iMesh;
	Opcode::OPCODECREATE OPCC;

	vector<IceMaths::Point> vertices;
	vector<IceMaths::IndexedTriangle> triangles;
    };
}


ColdetModel::ColdetModel()
{
    impl = new ColdetModelImpl();
}


ColdetModelImpl::ColdetModelImpl()
{
 
}    


ColdetModel::~ColdetModel()
{
    delete impl;
}


void ColdetModel::setNumVertices(int n)
{
    impl->vertices.resize(n);
}


void ColdetModel::setNumTriangles(int n)
{
    impl->triangles.resize(n);
}

        
void ColdetModel::setVertex(int index, float x, float y, float z)
{
    impl->vertices[index].Set(x, y, z);
}

        
void ColdetModel::setTriangle(int index, int v1, int v2, int v3)
{
    udword* mVRef = impl->triangles[index].mVRef;
    mVRef[0] = v1;
    mVRef[1] = v2;
    mVRef[2] = v3;
}


void ColdetModel::update()
{
    impl->update();
}


void ColdetModelImpl::update()
{
    iMesh.SetPointers(&triangles[0], &vertices[0]);
    iMesh.SetNbTriangles(triangles.size());
    iMesh.SetNbVertices(vertices.size());

    OPCC.mIMesh = &iMesh;
    
    OPCC.mNoLeaf = false;
    OPCC.mQuantized = false;
    OPCC.mKeepOriginal = false;
        
    model.Build(OPCC);
}
