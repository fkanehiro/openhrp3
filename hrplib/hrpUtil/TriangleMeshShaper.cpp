/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

/*!
  @file TriangleMeshShaper.cpp
  @author Shin'ichiro Nakaoka
*/

#include "TriangleMeshShaper.h"
#include "Triangulator.h"
#include <iostream>
#include <cmath>
#include <vector>
#include <map>
#include <hrpUtil/Tvmet3d.h>

using namespace std;
using namespace boost;
using namespace hrp;

namespace {
    const double PI = 3.14159265358979323846;
}

namespace hrp {

    class TMSImpl
    {
    public:
        TMSImpl(TriangleMeshShaper* self);

        TriangleMeshShaper* self;

        int divisionNumber;
        bool isNormalGenerationMode;

        typedef std::map<VrmlShapePtr, SFNode> ShapeToGeometryMap;
        ShapeToGeometryMap shapeToOriginalGeometryMap;

        // for triangulation
        Triangulator triangulator;
        std::vector<int> polygon;
        std::vector<int> indexPositionMap;
        std::vector<int> faceIndexMap;

        // for normal generation
        std::vector<Vector3> faceNormals;
        std::vector< std::vector<int> > vertexIndexToFaceIndicesMap;
        std::vector< std::vector<int> > vertexIndexToNormalIndicesMap;

        enum RemapType { REMAP_COLOR, REMAP_NORMAL };


        SFNode getOriginalGeometry(VrmlShapePtr shapeNode);
        bool traverseShapeNodes(VrmlNode* node, AbstractVrmlGroup* parentNode, int indexInParent);
        bool convertShapeNode(VrmlShape* shapeNode);
        bool convertIndexedFaceSet(VrmlIndexedFaceSet* faceSet);

        template <class TArray>
            bool remapDirectMapObjectsPerFaces(TArray& objects, const char* objectName);
        
        bool checkAndRemapIndices(RemapType type, int numElements, MFInt32& indices, bool perVertex,
                                  VrmlIndexedFaceSet* triangleMesh);
        void putError1(const char* valueName);
        
        bool convertBox(VrmlBox* box, VrmlIndexedFaceSetPtr& triangleMesh);
        bool convertCone(VrmlCone* cone, VrmlIndexedFaceSetPtr& triangleMesh);
        bool convertCylinder(VrmlCylinder* cylinder, VrmlIndexedFaceSetPtr& triangleMesh);
        bool convertSphere(VrmlSphere* sphere, VrmlIndexedFaceSetPtr& triangleMesh);
        bool convertElevationGrid(VrmlElevationGrid* grid, VrmlIndexedFaceSetPtr& triangleMesh);
        bool convertExtrusion(VrmlExtrusion* extrusion, VrmlIndexedFaceSetPtr& triangleMesh);
        void generateNormals(VrmlIndexedFaceSetPtr& triangleMesh);
        void calculateFaceNormals(VrmlIndexedFaceSetPtr& triangleMesh);
        void setVertexNormals(VrmlIndexedFaceSetPtr& triangleMesh);
        void setFaceNormals(VrmlIndexedFaceSetPtr& triangleMesh);
        bool setTexCoordIndex(VrmlIndexedFaceSetPtr faseSet );

        void putMessage(const std::string& message);
    };
}


TriangleMeshShaper::TriangleMeshShaper()
{
    impl = new TMSImpl(this);
}


TMSImpl::TMSImpl(TriangleMeshShaper* self) : self(self)
{
    divisionNumber = 20;
    isNormalGenerationMode = true;
}


TriangleMeshShaper::~TriangleMeshShaper()
{
    delete impl;
}


/*!
  @if jp
  プリミティブ形状のメッシュ化時における分割数の指定。

  デフォルトの分割数は20としている。
  @endif
*/
void TriangleMeshShaper::setDivisionNumber(int n)
{
    impl->divisionNumber = n;
}


/*!
  @if jp
  整形時に法線も生成するかどうかを指定する
  @endif
*/
void TriangleMeshShaper::setNormalGenerationMode(bool on)
{
    impl->isNormalGenerationMode = on;
}


/*!
  @if jp
  変換後のShapeノードに対して、変換前のノードが持っていたGeometryNodeを返す。

  例えば、元のGeometryがプリミティブ形式だった場合、プリミティブの種類やパラメータを知ることが出来る。
  @endif
*/
SFNode TriangleMeshShaper::getOriginalGeometry(VrmlShapePtr shapeNode)
{
    return impl->getOriginalGeometry(shapeNode);
}


SFNode TMSImpl::getOriginalGeometry(VrmlShapePtr shapeNode)
{
    SFNode originalGeometryNode;
    ShapeToGeometryMap::iterator p = shapeToOriginalGeometryMap.find(shapeNode);
    if(p != shapeToOriginalGeometryMap.end()){
        originalGeometryNode = p->second;
    }
    return originalGeometryNode;
}


/*!
  @if jp
  整形処理 (ModelNodeSet内のShape)

  引数で与えられたノードをトップとするシーングラフ中の Shape ノードを
  IndexedFaceSet形式においてすべてのポリゴンを三角形とした統一的な幾何形状形式に変換する.
  統一幾何形状形式に変換できないノードは削除される。

  @return 変換後のトップノード。
  トップノードがShapeノードのときに統一幾何形状形式へと変換出来なかった場合は無効なポインタを返す。
  @endif
*/
VrmlNodePtr TriangleMeshShaper::apply(VrmlNodePtr topNode)
{
    bool resultOfTopNode = impl->traverseShapeNodes(topNode.get(), 0, 0);
    return resultOfTopNode ? topNode : VrmlNodePtr();
}


bool TMSImpl::traverseShapeNodes(VrmlNode* node, AbstractVrmlGroup* parentNode, int indexInParent)
{
    bool result = true;

    if(node->isCategoryOf(PROTO_INSTANCE_NODE)){
        VrmlProtoInstance* protoInstance = static_cast<VrmlProtoInstance*>(node);
        if(protoInstance->actualNode){
            traverseShapeNodes(protoInstance->actualNode.get(), parentNode, indexInParent);
        }

    } else if(node->isCategoryOf(GROUPING_NODE)){
        AbstractVrmlGroup* group = static_cast<AbstractVrmlGroup*>(node);
        int numChildren = group->countChildren();
        for(int i = 0; i < numChildren; i++){
            traverseShapeNodes(group->getChild(i), group, i);
        }

    } else if(node->isCategoryOf(SHAPE_NODE)){
        VrmlShape* shapeNode = static_cast<VrmlShape*>(node);
        result = convertShapeNode(shapeNode);
        if(!result){
            if(parentNode){
                putMessage("Node is inconvertible and removed from the scene graph");
                parentNode->removeChild(indexInParent);
            }
        }
    }

    return result;
}


bool TMSImpl::convertShapeNode(VrmlShape* shapeNode)
{
    bool result = false;

    VrmlNode *node = shapeNode->geometry.get();
    VrmlGeometry* geometry = dynamic_cast<VrmlGeometry *>(node);
    if (!geometry){
        VrmlProtoInstance *protoInstance = dynamic_cast<VrmlProtoInstance *>(node);
        if (protoInstance){
            geometry = dynamic_cast<VrmlGeometry *>(protoInstance->actualNode.get());
        }
    }
    
    VrmlIndexedFaceSetPtr triangleMesh;

    if(VrmlIndexedFaceSet* faceSet = dynamic_cast<VrmlIndexedFaceSet*>(geometry)){
        if(faceSet->coord){
            result = convertIndexedFaceSet(faceSet);
            triangleMesh = faceSet;
        }

    } else {
    
        triangleMesh = new VrmlIndexedFaceSet();
        triangleMesh->coord = new VrmlCoordinate();
        
        if(VrmlBox* box = dynamic_cast<VrmlBox*>(geometry)){
            result = convertBox(box, triangleMesh);
            
        } else if(VrmlCone* cone = dynamic_cast<VrmlCone*>(geometry)){
            result = convertCone(cone, triangleMesh);
            
        } else if(VrmlCylinder* cylinder = dynamic_cast<VrmlCylinder*>(geometry)){
            result = convertCylinder(cylinder, triangleMesh);
            
        } else if(VrmlSphere* sphere = dynamic_cast<VrmlSphere*>(geometry)){
            result = convertSphere(sphere, triangleMesh);
            
        } else if(VrmlElevationGrid* elevationGrid = dynamic_cast<VrmlElevationGrid*>(geometry)){
            result = convertElevationGrid(elevationGrid, triangleMesh);
            
        } else if(VrmlExtrusion* extrusion = dynamic_cast<VrmlExtrusion*>(geometry)){
            result = convertExtrusion(extrusion, triangleMesh);
        }
        if(result){
            shapeToOriginalGeometryMap[shapeNode] = node;
            shapeNode->geometry = triangleMesh;
        }
    }
    
    if(result && !triangleMesh->normal && isNormalGenerationMode){
        generateNormals(triangleMesh);
    }

    return result;
}


bool TMSImpl::convertIndexedFaceSet(VrmlIndexedFaceSet* faceSet)
{
    MFVec3f& vertices = faceSet->coord->point;
    int numVertices = vertices.size();

    MFInt32& indices = faceSet->coordIndex;
    const MFInt32 orgIndices = indices;
    indices.clear();

    const int numOrgIndices = orgIndices.size();

    int orgFaceIndex = 0;
    int polygonTopIndexPosition = 0;

    indexPositionMap.clear();
    faceIndexMap.clear();
    polygon.clear();

    int triangleOrder[3];
    if(faceSet->ccw){
        triangleOrder[0] = 0; triangleOrder[1] = 1; triangleOrder[2] = 2;
    } else {
        triangleOrder[0] = 2; triangleOrder[1] = 1; triangleOrder[2] = 0;
    }
    
    triangulator.setVertices(vertices);
    
    for(int i=0; i < numOrgIndices; ++i){
        int index = orgIndices[i];
        if(index >= numVertices){
            putMessage("The coordIndex field has an index over the size of the vertices in the coord field");
        } else if(index >= 0){
            polygon.push_back(index);
        } else {
            int numTriangles = triangulator.apply(polygon);
            const vector<int>& triangles = triangulator.triangles();
            for(int j=0; j < numTriangles; ++j){
                for(int k=0; k < 3; ++k){
                    int localIndex = triangles[j * 3 + triangleOrder[k]];
                    indices.push_back(polygon[localIndex]);
                    indexPositionMap.push_back(polygonTopIndexPosition + localIndex);
                }
                indices.push_back(-1);
                indexPositionMap.push_back(-1);
                faceIndexMap.push_back(orgFaceIndex);
            }
            polygonTopIndexPosition = i + 1;
            orgFaceIndex++;
            polygon.clear();
        }
    }

    bool result = true;

    int numColors = faceSet->color ? faceSet->color->color.size() : 0;
    result &= checkAndRemapIndices
        (REMAP_COLOR, numColors, faceSet->colorIndex, faceSet->colorPerVertex, faceSet);

    int numNormals = faceSet->normal ? faceSet->normal->vector.size() : 0;
    result &= checkAndRemapIndices
        (REMAP_NORMAL, numNormals, faceSet->normalIndex, faceSet->normalPerVertex, faceSet);

    if(numNormals > 0 && !faceSet->ccw){
        // flip normal vectors
         MFVec3f& normals = faceSet->normal->vector;
         for(int i=0; i < normals.size(); ++i){
             SFVec3f& n = normals[i];
             n[0] = -n[0];
             n[1] = -n[1];
             n[2] = -n[2];
         }
    }
    faceSet->ccw = true;

    setTexCoordIndex(faceSet);
 
    return (result && !indices.empty());
}


template <class TArray>
bool TMSImpl::remapDirectMapObjectsPerFaces(TArray& values, const char* valueName)
{
    const TArray orgValues = values;
    int numOrgValues = orgValues.size();
    int numFaces = faceIndexMap.size();
    values.resize(numFaces);
    for(int i=0; i < numFaces; ++i){
        int faceIndex = faceIndexMap[i];
        if(faceIndex >= numOrgValues){
            putMessage(string("The number of ") + valueName + " is less than the number of faces.");
            return false;
        }
        values[i] = orgValues[faceIndex];
    }
    return true;
}


bool TMSImpl::checkAndRemapIndices
(RemapType type, int numElements, MFInt32& indices, bool perVertex, VrmlIndexedFaceSet* triangleMesh)
{
    const char* valueName = (type==REMAP_COLOR) ? "colors" : "normals" ;
    
    bool result = true;
    
    if(numElements == 0){
        if(!indices.empty()){
            putMessage(string("An IndexedFaceSet has no ") + valueName +
                       ", but it has a non-empty index field of " + valueName + ".");
            result = false;
        }

    } else {

        if(indices.empty()){
            if(perVertex){
                if(numElements < triangleMesh->coord->point.size()){
                    putMessage(string("The number of ") + valueName +
                              " is less than the number of vertices.");
                    result = false;
                }
            } else {
                if(type == REMAP_COLOR){
                    remapDirectMapObjectsPerFaces(triangleMesh->color->color, valueName);
                } else if(type == REMAP_NORMAL){
                    remapDirectMapObjectsPerFaces(triangleMesh->normal->vector, valueName);
                }
            }
        } else {
            const MFInt32 orgIndices = indices;

            if(perVertex){
                int numNewIndices = indexPositionMap.size();
                indices.resize(numNewIndices);
                for(int i=0; i < numNewIndices; ++i){
                    int orgPosition = indexPositionMap[i];
                    if(orgPosition < 0){
                        indices[i] = -1;
                    } else {
                        int index = orgIndices[orgPosition];
                        if(index < numElements){
                            indices[i] = index;
                        } else {
                            putError1(valueName);
                            result = false;
                        }
                    }
                }
            } else {
                int numNewIndices = faceIndexMap.size();
                indices.resize(numNewIndices);
                for(int i=0; i < numNewIndices; ++i){
                    int orgFaceIndex = faceIndexMap[i];
                    int index = orgIndices[orgFaceIndex];
                    if(index < numElements){
                        indices[i] = index;
                    } else {
                        putError1(valueName);
                        result = false;
                    }
                }
            }
        }
    }

    return result;
}


bool TMSImpl::setTexCoordIndex(VrmlIndexedFaceSetPtr faseSet)
{
    bool result = true;
    VrmlTextureCoordinatePtr texCoord = faseSet->texCoord;
    MFInt32& texCoordIndex = faseSet->texCoordIndex;
    MFInt32& coordIndex = faseSet->coordIndex;

	if(texCoord){
        if(texCoordIndex.empty()){   
            texCoordIndex.resize(coordIndex.size());
            copy( coordIndex.begin(), coordIndex.end(), texCoordIndex.begin() );
        } else {
            const MFInt32 orgIndices = texCoordIndex;
            int numNewIndices = indexPositionMap.size();
            texCoordIndex.resize(numNewIndices);
            for(int i=0; i < numNewIndices; ++i){
                if(indexPositionMap[i] == -1){
                    texCoordIndex[i] = -1;
                } else {
                    int index = orgIndices[indexPositionMap[i]];
                    if(index < texCoord->point.size()){
                        texCoordIndex[i] = index;
                    } else {
                        putError1("texCoordIndex");
                        result = false;
                    } 
                }
            }
        }
    }
    return result;
}


void TMSImpl::putError1(const char* valueName)
{
    putMessage(string("There is an index of ") + valueName +
               " beyond the size of " + valueName + ".");
}


namespace {

    inline int addVertex(MFVec3f& vertices, const double x, const double y, const double z)
    {
        SFVec3f v;
        v[0] = x;
        v[1] = y;
        v[2] = z;
        vertices.push_back(v);
        return vertices.size() - 1;
    }

    inline void addTriangle(MFInt32& indices, int x, int y, int z)
    {
        indices.push_back(x);
        indices.push_back(y);
        indices.push_back(z);
        indices.push_back(-1);
    }
}


bool TMSImpl::convertBox(VrmlBox* box, VrmlIndexedFaceSetPtr& triangleMesh)
{
    const double x = box->size[0] / 2.0;
    const double y = box->size[1] / 2.0;
    const double z = box->size[2] / 2.0;
    
    if(x < 0.0 || y < 0.0 || z < 0.0 ){
        putMessage("BOX : wrong value.");
        return false;
    }

    MFVec3f& vertices = triangleMesh->coord->point;
    vertices.reserve(8);

    static const int numTriangles = 12;
    
    static const double xsigns[] = { -1.0, -1.0, -1.0, -1.0,  1.0,  1.0,  1.0,  1.0 };
    static const double ysigns[] = { -1.0, -1.0,  1.0,  1.0, -1.0, -1.0,  1.0,  1.0 };
    static const double zsigns[] = { -1.0,  1.0,  1.0, -1.0, -1.0,  1.0,  1.0, -1.0 };

    static const int triangles[] = {
        0, 1, 2,
        2, 3, 0,
        3, 2, 6,
        3, 6, 7,
        6, 2, 1,
        1, 5, 6,
        1, 0, 5,
        5, 0, 4,
        5, 4, 6,
        6, 4, 7,
        7, 4, 0,
        0, 3, 7
    };

    for(int i=0; i < 8; ++i){
        addVertex(vertices, xsigns[i] * x, ysigns[i] * y, zsigns[i] * z);
    }
    
    MFInt32& indices = triangleMesh->coordIndex;
    indices.resize(numTriangles * 4);

    int di = 0;
    int si = 0;
    for(int i=0; i < numTriangles; i++){
        indices[di++] = triangles[si++];
        indices[di++] = triangles[si++];
        indices[di++] = triangles[si++];
        indices[di++] = -1;
    }

    return true;
}


bool TMSImpl::convertCone(VrmlCone* cone, VrmlIndexedFaceSetPtr& triangleMesh)
{
    const double radius = cone->bottomRadius;
    
    if(cone->height < 0.0 || radius < 0.0 ){
        putMessage( "CONE : wrong value." );
        return false;
    }

    MFVec3f& vertices = triangleMesh->coord->point;
    vertices.reserve(divisionNumber + 1);

    for(int i=0;  i < divisionNumber; ++i){
        const double angle = i * 2.0 * PI / divisionNumber;
        addVertex(vertices, radius * cos(angle), -cone->height/2.0, radius * sin(angle));
    }

    const int topIndex = addVertex(vertices, 0.0, cone->height/2.0, 0.0);
    const int bottomCenterIndex = addVertex(vertices, 0.0, -cone->height/2.0, 0.0);

    MFInt32& indices = triangleMesh->coordIndex;
    indices.reserve((divisionNumber * 2) * 4);
    const int offset = divisionNumber * 2;

    for(int i=0; i < divisionNumber; ++i){
        // side faces
        if(cone->side)
            addTriangle(indices, topIndex, (i + 1) % divisionNumber, i);
        // bottom faces
        if(cone->bottom)       
            addTriangle(indices, bottomCenterIndex, i, (i + 1) % divisionNumber);
    }

    triangleMesh->creaseAngle = 3.14 / 2.0;

    return true;
}


bool TMSImpl::convertCylinder(VrmlCylinder* cylinder, VrmlIndexedFaceSetPtr& triangleMesh)
{
    if(cylinder->height < 0.0 || cylinder->radius < 0.0){
        putMessage("CYLINDER : wrong value.");
        return false;
    }

    MFVec3f& vertices = triangleMesh->coord->point;
    vertices.reserve(divisionNumber * 2 + 2);
    vertices.resize(divisionNumber * 2);

    const double y = cylinder->height / 2.0;

    for(int i=0 ; i < divisionNumber ; i++ ){
        const double angle = i * 2.0 * PI / divisionNumber;
        SFVec3f& vtop = vertices[i];
        SFVec3f& vbottom = vertices[i + divisionNumber];
        vtop[0] = vbottom[0] = cylinder->radius * cos(angle);
        vtop[2] = vbottom[2] = cylinder->radius * sin(angle);
        vtop[1]    =  y;
        vbottom[1] = -y;
    }

    const int topCenterIndex    = addVertex(vertices, 0.0,  y, 0.0);
    const int bottomCenterIndex = addVertex(vertices, 0.0, -y, 0.0);

    MFInt32& indices = triangleMesh->coordIndex;
    indices.reserve((divisionNumber * 4) * 4);

    for(int i=0; i < divisionNumber; ++i){
        // top face
        if(cylinder->top)
            addTriangle(indices, topCenterIndex, (i+1) % divisionNumber, i);
        // side face (upward convex triangle)
        if(cylinder->side){        
            addTriangle(indices, i, ((i+1) % divisionNumber) + divisionNumber, i + divisionNumber);
            // side face (downward convex triangle)
            addTriangle(indices, i, (i+1) % divisionNumber, ((i + 1) % divisionNumber) + divisionNumber);
        }
        // bottom face
        if(cylinder->bottom)
            addTriangle(indices, bottomCenterIndex, i + divisionNumber, ((i+1) % divisionNumber) + divisionNumber);
    }

    triangleMesh->creaseAngle = 3.14 / 2.0;

    return true;
}


bool TMSImpl::convertSphere(VrmlSphere* sphere, VrmlIndexedFaceSetPtr& triangleMesh)
{
    const double r = sphere->radius;

    if(r < 0.0) {
        putMessage("SPHERE : wrong value.");
        return false;
    }

    const int vdn = divisionNumber;  // latitudinal division number
    const int hdn = divisionNumber;  // longitudinal division number
    
    MFVec3f& vertices = triangleMesh->coord->point;
    vertices.reserve((vdn - 1) * hdn + 2);

    for(int i=1; i < vdn; i++){ // latitudinal direction
        double tv = i * PI / vdn;
        for(int j=0; j < hdn; j++){ // longitudinal direction
            double th = j * 2.0 * PI / hdn;
            addVertex(vertices, r*sin(tv)*cos(th), r*cos(tv), r*sin(tv)*sin(th));
        }
    }
    
    const int topIndex    = addVertex(vertices, 0.0,  r, 0.0);
    const int bottomIndex = addVertex(vertices, 0.0, -r, 0.0);

    MFInt32& indices = triangleMesh->coordIndex;
    indices.reserve(vdn * hdn * 2 * 4);

    // top faces
    for(int i=0; i < hdn; ++i){
        addTriangle(indices, topIndex, (i+1) % hdn, i);
    }

    // side faces
    for(int i=0; i < vdn - 2; ++i){
        const int upper = i * hdn;
        const int lower = (i + 1) * hdn;
        for(int j=0; j < hdn; ++j) {
            // upward convex triangle
            addTriangle(indices, j + upper, ((j + 1) % hdn) + lower, j + lower);
            // downward convex triangle
            addTriangle(indices, j + upper, ((j + 1) % hdn) + upper, ((j + 1) % hdn) + lower);
        }
    }
    
    // bottom faces
    const int offset = (vdn - 2) * hdn;
    for(int i=0; i < hdn; ++i){
        addTriangle(indices, bottomIndex, (i % hdn) + offset, ((i+1) % hdn) + offset);
    }

    triangleMesh->creaseAngle = PI;

    return true;
}


/**
   \todo copy colors and color indices to triangleMesh
*/
bool TMSImpl::convertElevationGrid(VrmlElevationGrid* grid, VrmlIndexedFaceSetPtr& triangleMesh)
{
    if(grid->xDimension * grid->zDimension != static_cast<SFInt32>(grid->height.size())){
        putMessage("ELEVATIONGRID : wrong value.");
        return false;
    }

    MFVec3f& vertices = triangleMesh->coord->point;
    vertices.reserve(grid->zDimension * grid->xDimension);

    for(int z=0; z < grid->zDimension; z++){
        for(int x=0; x < grid->xDimension; x++ ){
            addVertex(vertices, x * grid->xSpacing, grid->height[z * grid->xDimension + x], z * grid->zSpacing);
        }
    }

    MFInt32& indices = triangleMesh->coordIndex;
    indices.reserve((grid->zDimension - 1) * (grid->xDimension - 1) * 2 * 4);

    for(int z=0; z < grid->zDimension - 1; ++z){
        const int current = z * grid->xDimension;
        const int next = (z + 1) * grid->xDimension;
        for(int x=0; x < grid->xDimension - 1; ++x){
            addTriangle(indices, x + current, x + next, (x + 1) + next);
            addTriangle(indices, x + current, (x + 1) + next, (x + 1) + current);
        }
    }

    triangleMesh->creaseAngle = grid->creaseAngle;

    if(grid->texCoord){
        triangleMesh->texCoord->point.resize(grid->texCoord->point.size());
        copy(grid->texCoord->point.begin(), grid->texCoord->point.end(), triangleMesh->texCoord->point.begin());
        triangleMesh->texCoordIndex.resize(indices.size());
        copy(indices.begin(), indices.end(), triangleMesh->texCoordIndex.begin());
    }

    return true;
}


bool TMSImpl::convertExtrusion(VrmlExtrusion* extrusion, VrmlIndexedFaceSetPtr& triangleMesh)
{
    bool isClosed = false;
    int numSpine = extrusion->spine.size();
    int numcross = extrusion->crossSection.size();
    if( extrusion->spine[0][0] == extrusion->spine[numSpine-1][0] &&
        extrusion->spine[0][1] == extrusion->spine[numSpine-1][1] &&
        extrusion->spine[0][2] == extrusion->spine[numSpine-1][2] )
        isClosed = true;
    bool crossSectionisClosed = false;
    if( extrusion->crossSection[0][0] == extrusion->crossSection[numcross-1][0] &&
        extrusion->crossSection[0][1] == extrusion->crossSection[numcross-1][1] )
        crossSectionisClosed = true;

    MFVec3f& vertices = triangleMesh->coord->point;
    vertices.reserve(numSpine*numcross);

    Vector3 preZaxis;
    int definedZaxis=-1;
    std::vector<Vector3> Yaxisarray;
    std::vector<Vector3> Zaxisarray;
    if(numSpine > 2){
        for(int i=0; i<numSpine; i++){
            Vector3 spine1, spine2, spine3;
            Vector3 Yaxis, Zaxis;
            if(i==0){
                if(isClosed){
                    spine1 = Vector3(extrusion->spine[numSpine-2].begin(),3);
                    spine2 = Vector3(extrusion->spine[0].begin(),3);
                    spine3 = Vector3(extrusion->spine[1].begin(),3);
                    Yaxis = Vector3(spine3-spine1);
                    Zaxis = Vector3(cross((spine3-spine2),(spine1-spine2)));
                }else{
                    spine1 = Vector3(extrusion->spine[0].begin(),3);
                    spine2 = Vector3(extrusion->spine[1].begin(),3);
                    spine3 = Vector3(extrusion->spine[2].begin(),3);
                    Yaxis = Vector3(spine2-spine1);
                    Zaxis = Vector3(cross((spine3-spine2),(spine1-spine2)));
                }
            }else if(i==numSpine-1){
                if(isClosed){
                    spine1 = Vector3(extrusion->spine[numSpine-2].begin(),3);
                    spine2 = Vector3(extrusion->spine[0].begin(),3);
                    spine3 = Vector3(extrusion->spine[1].begin(),3);
                    Yaxis = Vector3(spine3-spine1);
                    Zaxis = Vector3(cross((spine3-spine2),(spine1-spine2)));
                }else{
                    spine1 = Vector3(extrusion->spine[numSpine-3].begin(),3);
                    spine2 = Vector3(extrusion->spine[numSpine-2].begin(),3);
                    spine3 = Vector3(extrusion->spine[numSpine-1].begin(),3);
                    Yaxis = Vector3(spine3-spine2);
                    Zaxis = Vector3(cross((spine3-spine2),(spine1-spine2)));
                }
            }else{
                spine1 = Vector3(extrusion->spine[i-1].begin(),3);
                spine2 = Vector3(extrusion->spine[i].begin(),3);
                spine3 = Vector3(extrusion->spine[i+1].begin(),3);
                Yaxis = Vector3(spine3-spine1);
                Zaxis = Vector3(cross((spine3-spine2),(spine1-spine2)));
            }
            if(!norm2(Zaxis)){
                if(definedZaxis!=-1)
                    Zaxis=preZaxis;
            }else{
                if(definedZaxis==-1)
                    definedZaxis=i;
                preZaxis = Zaxis;
            }
            Yaxisarray.push_back(Yaxis);
            Zaxisarray.push_back(Zaxis);
        }
    }else{
        Vector3 spine1, spine2;
        Vector3 Yaxis;
        spine1 = Vector3(extrusion->spine[0].begin(),3);
        spine2 = Vector3(extrusion->spine[1].begin(),3);
        Yaxis = Vector3(spine2-spine1);
        Yaxisarray.push_back(Yaxis);
        Yaxisarray.push_back(Yaxis);
    }
    for(int i=0; i<numSpine; i++){
        Matrix33 Scp;
        if(definedZaxis==-1){
            Vector3 y(normalize(Yaxisarray[i]));    
            SFRotation R;
            R[0] = y[2]; R[1] = 0.0; R[2] = -y[0]; R[3] = y[1];
            Scp = rodrigues(Vector3(R[0],R[1],R[2]), R[3]);
        }else{
            if(i<definedZaxis)
                Zaxisarray[i] = Zaxisarray[definedZaxis];
            if( i && dot(Zaxisarray[i],Zaxisarray[i-1])<0 )
                Zaxisarray[i] *= -1;
            Vector3 y(normalize(Yaxisarray[i]));
		    Vector3 z(normalize(Zaxisarray[i]));
		    Vector3 x(cross(y, z));
 		    setVector3(x, Scp, 0, 0);
		    setVector3(y, Scp, 0, 1);
		    setVector3(z, Scp, 0, 2);
        }

        Vector3 spine(extrusion->spine[i].begin(),3);
        Vector3 scale;
        if(extrusion->scale.size()==1)
            scale = Vector3(extrusion->scale[0][0], 0, extrusion->scale[0][1]);
        else
            scale = Vector3(extrusion->scale[i][0], 0, extrusion->scale[i][1]);
        Matrix33 orientation;
        if(extrusion->orientation.size()==1)
            orientation = rodrigues(Vector3(extrusion->orientation[0][0],extrusion->orientation[0][1],extrusion->orientation[0][2]),
                extrusion->orientation[0][3]);
        else
            orientation = rodrigues(Vector3(extrusion->orientation[i][0],extrusion->orientation[i][1],extrusion->orientation[i][2]),
                extrusion->orientation[i][3]);

        for(int j=0; j<numcross; j++){
            Vector3 crossSection(extrusion->crossSection[j][0], 0, extrusion->crossSection[j][1] );
            Vector3 v1(crossSection[0]*scale[0], 0, crossSection[2]*scale[2]);
            Vector3 v2(Scp*orientation*v1+spine); 
            addVertex(vertices,v2[0], v2[1], v2[2]);
        }
    }

    MFInt32& indices = triangleMesh->coordIndex;
    for(int i=0; i < numSpine-1 ; i++){
        const int upper = i * numcross;
        const int lower = (i + 1) * numcross;
        for(int j=0; j < numcross-1; ++j) {
            // upward convex triangle
            addTriangle(indices, j + upper, (j + 1)+ lower, j + lower);
            // downward convex triangle
            addTriangle(indices, j + upper, (j + 1)+ upper, j + 1 + lower);
        }
    }

    int j=0;
    if(crossSectionisClosed)
        j=1;
    if(extrusion->beginCap && !isClosed){
        triangulator.setVertices(vertices);
        polygon.clear();
        for(int i=0; i<numcross-j; i++)
            polygon.push_back(i);
        triangulator.apply(polygon);
        const vector<int>& triangles = triangulator.triangles();
        for(int i=0; i<triangles.size(); i+=3 )
            addTriangle(indices, polygon[triangles[i]], polygon[triangles[i+1]], polygon[triangles[i+2]]);
    }

    if(extrusion->endCap && !isClosed){
        triangulator.setVertices(vertices);
        polygon.clear();
        for(int i=0; i<numcross-j; i++)
            polygon.push_back(numcross*(numSpine-1)+i);
        triangulator.apply(polygon);
        const vector<int>& triangles = triangulator.triangles();
        for(int i=0; i<triangles.size(); i+=3 )
            addTriangle(indices, polygon[triangles[i]], polygon[triangles[i+1]], polygon[triangles[i+2]]);
    }

    triangleMesh->creaseAngle = extrusion->creaseAngle;
 
    return true;
}


void TMSImpl::generateNormals(VrmlIndexedFaceSetPtr& triangleMesh)
{
    triangleMesh->normal = new VrmlNormal();
    triangleMesh->normalPerVertex = (triangleMesh->creaseAngle > 0.0) ? true : false;

    calculateFaceNormals(triangleMesh);

    if(triangleMesh->normalPerVertex){
        setVertexNormals(triangleMesh);
    } else {
        setFaceNormals(triangleMesh);
    }
}


void TMSImpl::calculateFaceNormals(VrmlIndexedFaceSetPtr& triangleMesh)
{
    const MFVec3f& vertices = triangleMesh->coord->point;
    const int numVertices = vertices.size();
    const MFInt32& triangles = triangleMesh->coordIndex;
    const int numFaces = triangles.size() / 4;

    faceNormals.clear();

    if(triangleMesh->normalPerVertex){
        vertexIndexToFaceIndicesMap.clear();
        vertexIndexToFaceIndicesMap.resize(numVertices);
    }

    for(int faceIndex=0; faceIndex < numFaces; ++faceIndex){

        Vector3Ref v0(getVector3Ref(vertices[triangles[faceIndex * 4 + 0]].data()));
        Vector3Ref v1(getVector3Ref(vertices[triangles[faceIndex * 4 + 1]].data()));
        Vector3Ref v2(getVector3Ref(vertices[triangles[faceIndex * 4 + 2]].data()));
        const Vector3 normal(tvmet::normalize(Vector3(tvmet::cross(v1 - v0, v2 - v0))));
        faceNormals.push_back(normal);

        if(triangleMesh->normalPerVertex){
            for(int i=0; i < 3; ++i){
                int vertexIndex = triangles[faceIndex * 4 + i];
                vector<int>& facesOfVertex = vertexIndexToFaceIndicesMap[vertexIndex];
                bool isSameNormalFaceFound = false;
                for(int j=0; j < facesOfVertex.size(); ++j){
                    const Vector3& otherNormal = faceNormals[facesOfVertex[j]];
                    const Vector3 d(otherNormal - normal);
                    // the same face is not appended
                    if(tvmet::dot(d, d) <= numeric_limits<double>::epsilon()){
                    	isSameNormalFaceFound = true;
                    	break;
                    }
                }
                if(!isSameNormalFaceFound){
                    facesOfVertex.push_back(faceIndex);
                }
            }
        }
    }
}


void TMSImpl::setVertexNormals(VrmlIndexedFaceSetPtr& triangleMesh)
{
    const MFVec3f& vertices = triangleMesh->coord->point;
    const int numVertices = vertices.size();
    const MFInt32& triangles = triangleMesh->coordIndex;
    const int numFaces = triangles.size() / 4;

    MFVec3f& normals = triangleMesh->normal->vector;
    MFInt32& normalIndices = triangleMesh->normalIndex;
    normalIndices.clear();
    normalIndices.reserve(triangles.size());

    vertexIndexToNormalIndicesMap.clear();
    vertexIndexToNormalIndicesMap.resize(numVertices);

    //const double cosCreaseAngle = cos(triangleMesh->creaseAngle);

    for(int faceIndex=0; faceIndex < numFaces; ++faceIndex){

        for(int i=0; i < 3; ++i){

            int vertexIndex = triangles[faceIndex * 4 + i];
            vector<int>& facesOfVertex = vertexIndexToFaceIndicesMap[vertexIndex];
            const Vector3& currentFaceNormal = faceNormals[faceIndex];
            Vector3 normal = currentFaceNormal;
            bool normalIsFaceNormal = true;

            // avarage normals of the faces whose crease angle is below the 'creaseAngle' variable
            for(int j=0; j < facesOfVertex.size(); ++j){
                int adjoingFaceIndex = facesOfVertex[j];
                const Vector3& adjoingFaceNormal = faceNormals[adjoingFaceIndex];
                double angle = acos(tvmet::dot(currentFaceNormal, adjoingFaceNormal)
                                    / (tvmet::norm2(currentFaceNormal) * tvmet::norm2(adjoingFaceNormal)));
                if(angle > 1.0e-6 && angle < triangleMesh->creaseAngle){
                    normal += adjoingFaceNormal;
                    normalIsFaceNormal = false;
                }
                
            }
            if(!normalIsFaceNormal){
                alias(normal) = tvmet::normalize(normal);
            }

            int normalIndex = -1;

            for(int j=0; j < 3; ++j){
                int vertexIndex2 = triangles[faceIndex * 4 + j];
                vector<int>& normalIndicesOfVertex = vertexIndexToNormalIndicesMap[vertexIndex2];
                for(int k=0; k < normalIndicesOfVertex.size(); ++k){
                    int index = normalIndicesOfVertex[k];
                    const SFVec3f& norg = normals[index];
                    const Vector3 d(Vector3(norg[0], norg[1], norg[2]) - normal);
                    if(tvmet::dot(d, d) <= numeric_limits<double>::epsilon()){
                        normalIndex = index;
                        goto normalIndexFound;
                    }
                }
            }
            if(normalIndex < 0){
                SFVec3f n;
                std::copy(normal.begin(), normal.end(), n.begin());
                normalIndex = normals.size();
                normals.push_back(n);
                vertexIndexToNormalIndicesMap[vertexIndex].push_back(normalIndex);
            }
            
          normalIndexFound:
            normalIndices.push_back(normalIndex);
        }
        normalIndices.push_back(-1);
    }
}


void TMSImpl::setFaceNormals(VrmlIndexedFaceSetPtr& triangleMesh)
{
    const MFInt32& triangles = triangleMesh->coordIndex;
    const int numFaces = triangles.size() / 4;

    MFVec3f& normals = triangleMesh->normal->vector;
    MFInt32& normalIndices = triangleMesh->normalIndex;
    normalIndices.clear();
    normalIndices.reserve(numFaces);

    const int numVertices = triangleMesh->coord->point.size();
    vertexIndexToNormalIndicesMap.clear();
    vertexIndexToNormalIndicesMap.resize(numVertices);

    for(int faceIndex=0; faceIndex < numFaces; ++faceIndex){

        const Vector3& normal = faceNormals[faceIndex];
        int normalIndex = -1;

        // find the same normal from the existing normals
        for(int i=0; i < 3; ++i){
            int vertexIndex = triangles[faceIndex * 4 + i];
            vector<int>& normalIndicesOfVertex = vertexIndexToNormalIndicesMap[vertexIndex];
            for(int j=0; j < normalIndicesOfVertex.size(); ++j){
                int index = normalIndicesOfVertex[j];
                const SFVec3f& norg = normals[index];
                const Vector3 n(norg[0], norg[1], norg[2]);
                if(tvmet::norm2(n - normal) <= numeric_limits<double>::epsilon()){
                    normalIndex = index;
                    goto normalIndexFound2;
                }
            }
        }
        if(normalIndex < 0){
            SFVec3f n;
            std::copy(normal.begin(), normal.end(), n.begin());
            normalIndex = normals.size();
            normals.push_back(n);
            for(int i=0; i < 3; ++i){
                int vertexIndex = triangles[faceIndex * 4 + i];
                vertexIndexToNormalIndicesMap[vertexIndex].push_back(normalIndex);
            }
        }
      normalIndexFound2:
        normalIndices.push_back(normalIndex);
    }
}
    

void TMSImpl::putMessage(const std::string& message)
{
    if(!self->sigMessage.empty()){
        self->sigMessage(message + "\n" );
    }
}


void TriangleMeshShaper::defaultTextureMapping(VrmlShape* shapeNode)
{
    VrmlIndexedFaceSet* triangleMesh = dynamic_cast<VrmlIndexedFaceSet*>(shapeNode->geometry.get());
    if(!triangleMesh)
        return;
    VrmlNode* node = getOriginalGeometry(shapeNode).get();
    VrmlGeometry *originalGeometry = dynamic_cast<VrmlGeometry *>(node);
    if (!originalGeometry){
        VrmlProtoInstance *protoInstance = dynamic_cast<VrmlProtoInstance *>(node);
        if (protoInstance){
            originalGeometry = dynamic_cast<VrmlGeometry *>(protoInstance->actualNode.get());
        }
    }
    if(originalGeometry){
        if(VrmlBox* box = dynamic_cast<VrmlBox*>(originalGeometry)){    //Box
            defaultTextureMappingBox(triangleMesh);
        }else if(VrmlCone* cone = dynamic_cast<VrmlCone*>(originalGeometry)){  //cone
            defaultTextureMappingCone(triangleMesh);
        }else if(VrmlCylinder* cylinder = dynamic_cast<VrmlCylinder*>(originalGeometry)){   //Cylinder
            defaultTextureMappingCylinder(triangleMesh);
        }else if(VrmlSphere* sphere = dynamic_cast<VrmlSphere*>(originalGeometry)){     //sphere
            defaultTextureMappingSphere(triangleMesh, sphere->radius);
        }else if(VrmlElevationGrid* grid = dynamic_cast<VrmlElevationGrid*>(originalGeometry)){     //ElevationGrid
            defaultTextureMappingElevationGrid(grid, triangleMesh);
        }else if(VrmlExtrusion* extrusion = dynamic_cast<VrmlExtrusion*>(originalGeometry)){     //Extrusion
            defaultTextureMappingExtrusion(triangleMesh, extrusion);
        }
    }else{      //IndexedFaceSet
        defaultTextureMappingFaceSet(triangleMesh);
    }
}


void TriangleMeshShaper::defaultTextureMappingFaceSet(VrmlIndexedFaceSet* triangleMesh)
{
    if(!triangleMesh->texCoord){
        float max[3]={triangleMesh->coord->point[0][0],triangleMesh->coord->point[0][1],triangleMesh->coord->point[0][2]};
        float min[3]={triangleMesh->coord->point[0][0],triangleMesh->coord->point[0][1],triangleMesh->coord->point[0][2]};
        int n = triangleMesh->coord->point.size();
        for(int i=1; i<n; i++){
            for(int j=0; j<3; j++){
                float w = triangleMesh->coord->point[i][j];
                max[j] = std::max( max[j], w );
                min[j] = std::min( min[j], w );
            }
        }
        float size[3]={0,0,0};
        for(int j=0; j<3; j++)
            size[j] = max[j]-min[j];
        int s,t;
        size[0] >= size[1] ? 
              ( size[0] >= size[2] ? 
                      ( s=0 , t=size[1] >= size[2] ? 1 : 2 ) 
                    : ( s=2 , t=0) ) 
            : ( size[1] >= size[2] ? 
                      ( s=1 , t=size[0] >= size[2] ? 0 : 2 )
                    : ( s=2 , t=1) ) ;
        triangleMesh->texCoord = new VrmlTextureCoordinate();
        double ratio = size[t]/size[s];
        for(int i=0; i<n; i++){
            SFVec2f point;
            point[0] = (triangleMesh->coord->point[i][s]-min[s])/size[s];
            point[1] = (triangleMesh->coord->point[i][t]-min[t])/size[t]*ratio;
            triangleMesh->texCoord->point.push_back(point);
        }
        triangleMesh->texCoordIndex.resize(triangleMesh->coordIndex.size());
        copy( triangleMesh->coordIndex.begin(), triangleMesh->coordIndex.end(), 
			triangleMesh->texCoordIndex.begin() );
    }   
}


void TriangleMeshShaper::defaultTextureMappingElevationGrid(VrmlElevationGrid* grid, VrmlIndexedFaceSet* triangleMesh)
{
    float xmax = grid->xSpacing * (grid->xDimension-1);
    float zmax = grid->zSpacing * (grid->zDimension-1);
    triangleMesh->texCoord = new VrmlTextureCoordinate();
    for(int i=0; i<triangleMesh->coord->point.size(); i++){
       SFVec2f point;
       point[0] = (triangleMesh->coord->point[i][0])/xmax;
       point[1] = (triangleMesh->coord->point[i][2])/zmax;
       triangleMesh->texCoord->point.push_back(point);
    }
    triangleMesh->texCoordIndex.resize(triangleMesh->coordIndex.size());
    copy( triangleMesh->coordIndex.begin(), triangleMesh->coordIndex.end(), 
		triangleMesh->texCoordIndex.begin() );
}


int TriangleMeshShaper::faceofBox(SFVec3f* point)
{
    if(point[0][0] <= 0 && point[1][0] <= 0 && point[2][0] <= 0 ) return LEFT;
    if(point[0][0] > 0 && point[1][0] > 0 && point[2][0] > 0 ) return RIGHT;
    if(point[0][1] <= 0 && point[1][1] <= 0 && point[2][1] <= 0 ) return BOTTOM;
    if(point[0][1] > 0 && point[1][1] > 0 && point[2][1] > 0 ) return TOP;
    if(point[0][2] <= 0 && point[1][2] <= 0 && point[2][2] <= 0 ) return BACK;
    if(point[0][2] > 0 && point[1][2] > 0 && point[2][2] > 0 ) return FRONT;
    return -1;
}


int TriangleMeshShaper::findPoint(MFVec2f& points, SFVec2f& target)
{
    for(int i=0; i<points.size(); i++){
        if((points[i][0]-target[0])*(points[i][0]-target[0]) +
            (points[i][1]-target[1])*(points[i][1]-target[1]) < 1.0e-9 )
            return i;
    }
    return -1;
}


double TriangleMeshShaper::calcangle(SFVec3f& point)
{
    double angle = atan2( point[2], point[0] );
    if(angle>=0) angle=1.5*PI-angle;
    else if(-0.5*PI<angle) angle=-angle+1.5*PI;
    else angle=-angle-0.5*PI;
    return angle;
}


void TriangleMeshShaper::defaultTextureMappingBox(VrmlIndexedFaceSet* triangleMesh)
{
    triangleMesh->texCoord = new VrmlTextureCoordinate();
    SFVec2f point ;
    point[0] = 0.0; point[1] = 0.0;     //index=0
    triangleMesh->texCoord->point.push_back(point);
    point[0] = 1.0; point[1] = 0.0;     //index=1
    triangleMesh->texCoord->point.push_back(point);
    point[0] = 0.0; point[1] = 1.0;     //index=2
    triangleMesh->texCoord->point.push_back(point);
    point[0] = 1.0; point[1] = 1.0;     //index=3
    triangleMesh->texCoord->point.push_back(point);
    
    for(int i=0; i<12; i++){
        SFVec3f point[3];
        for(int j=0; j<3; j++)
            point[j] = triangleMesh->coord->point[triangleMesh->coordIndex[i*4+j]];
        switch(faceofBox(point)){
            case LEFT:
                for(int j=0; j<3; j++){
                    if(point[j][1] > 0 && point[j][2] > 0 ) triangleMesh->texCoordIndex.push_back(3);
                    else if(point[j][1] > 0 && point[j][2] <= 0 ) triangleMesh->texCoordIndex.push_back(2);
                    else if(point[j][1] <= 0 && point[j][2] > 0 ) triangleMesh->texCoordIndex.push_back(1);
                    else if(point[j][1] <= 0 && point[j][2] <= 0 ) triangleMesh->texCoordIndex.push_back(0);
                }
                break;
            case RIGHT:
                for(int j=0; j<3; j++){
                    if(point[j][1] > 0 && point[j][2] > 0 ) triangleMesh->texCoordIndex.push_back(2);
                    else if(point[j][1] > 0 && point[j][2] <= 0 ) triangleMesh->texCoordIndex.push_back(3);
                    else if(point[j][1] <= 0 && point[j][2] > 0 ) triangleMesh->texCoordIndex.push_back(0);
                    else if(point[j][1] <= 0 && point[j][2] <= 0 ) triangleMesh->texCoordIndex.push_back(1);
                }
                break;
            case BOTTOM:
                for(int j=0; j<3; j++){
                    if(point[j][2] > 0 && point[j][0] > 0 ) triangleMesh->texCoordIndex.push_back(3);
                    else if(point[j][2] > 0 && point[j][0] <= 0 ) triangleMesh->texCoordIndex.push_back(2);
                    else if(point[j][2] <= 0 && point[j][0] > 0 ) triangleMesh->texCoordIndex.push_back(1);
                    else if(point[j][2] <= 0 && point[j][0] <= 0 ) triangleMesh->texCoordIndex.push_back(0);
                }
                break;
            case TOP:
                for(int j=0; j<3; j++){
                    if(point[j][2] > 0 && point[j][0] > 0 ) triangleMesh->texCoordIndex.push_back(1);
                    else if(point[j][2] > 0 && point[j][0] <= 0 ) triangleMesh->texCoordIndex.push_back(0);
                    else if(point[j][2] <= 0 && point[j][0] > 0 ) triangleMesh->texCoordIndex.push_back(3);
                    else if(point[j][2] <= 0 && point[j][0] <= 0 ) triangleMesh->texCoordIndex.push_back(2);
                }
                break;
            case BACK:
                for(int j=0; j<3; j++){
                    if(point[j][1] > 0 && point[j][0] > 0 ) triangleMesh->texCoordIndex.push_back(2);
                    else if(point[j][1] > 0 && point[j][0] <= 0 ) triangleMesh->texCoordIndex.push_back(3);
                    else if(point[j][1] <= 0 && point[j][0] > 0 ) triangleMesh->texCoordIndex.push_back(0);
                    else if(point[j][1] <= 0 && point[j][0] <= 0 ) triangleMesh->texCoordIndex.push_back(1);
                }
                break;
            case FRONT:
                 for(int j=0; j<3; j++){
                    if(point[j][1] > 0 && point[j][0] > 0 ) triangleMesh->texCoordIndex.push_back(3);
                    else if(point[j][1] > 0 && point[j][0] <= 0 ) triangleMesh->texCoordIndex.push_back(2);
                    else if(point[j][1] <= 0 && point[j][0] > 0 ) triangleMesh->texCoordIndex.push_back(1);
                    else if(point[j][1] <= 0 && point[j][0] <= 0 ) triangleMesh->texCoordIndex.push_back(0);
                }
                break;
            default:
                break;
        }
        triangleMesh->texCoordIndex.push_back(-1);
    }
}


void TriangleMeshShaper::defaultTextureMappingCone(VrmlIndexedFaceSet* triangleMesh)
{
    triangleMesh->texCoord = new VrmlTextureCoordinate();
    SFVec2f texPoint ;
    texPoint[0] = 0.5; texPoint[1] = 0.5;     //center of bottom index=0
    triangleMesh->texCoord->point.push_back(texPoint);
    int texIndex = 1;
    for(int i=0; i<triangleMesh->coordIndex.size(); i++){
        SFVec3f point[3];
        int top=-1;
        int center=-1;
        for(int j=0; j<3; j++){
            point[j] = triangleMesh->coord->point[triangleMesh->coordIndex[i++]];
            if(point[j][1] > 0) top = j;
            if(point[j][0] == 0.0 && point[j][2] == 0.0) center = j;
        }
        if(top>=0){         //side
            double s[3]={0,0,0};
            int pre=-1;
            for(int j=0; j<3; j++){
                if(j!=top){
                    double angle = calcangle(point[j]);
                    s[j] = angle/2/PI;     
                    if(pre!=-1)
                        if(s[pre] > 0.5 && s[j] < 1.0e-6)
                            s[j] = 1.0;
                    pre = j;

                }
            }
            for(int j=0; j<3; j++){
                if(j!=top){
                    texPoint[0] = s[j];        
                    texPoint[1] = 0.0;
                }else{
                    texPoint[0] = (s[0]+s[1]+s[2])/2.0;        
                    texPoint[1] = 1.0;
                }
                int k=findPoint(triangleMesh->texCoord->point, texPoint);
                if(k!=-1){
                    triangleMesh->texCoordIndex.push_back(k);
                }else{
                    triangleMesh->texCoord->point.push_back(texPoint);
                    triangleMesh->texCoordIndex.push_back(texIndex++);
                }
            }
            triangleMesh->texCoordIndex.push_back(-1);
        }else{              // bottom
            for(int j=0; j<3; j++){
                if(j!=center){
                    double angle = atan2( point[j][2], point[j][0] );
                    texPoint[0] = 0.5 + 0.5*cos(angle);        
                    texPoint[1] = 0.5 + 0.5*sin(angle);
                    int k=findPoint(triangleMesh->texCoord->point, texPoint);
                    if(k!=-1){
                         triangleMesh->texCoordIndex.push_back(k);
                    }else{
                         triangleMesh->texCoord->point.push_back(texPoint);
                         triangleMesh->texCoordIndex.push_back(texIndex++);
                    }
                }else{
                    triangleMesh->texCoordIndex.push_back(0);
                }
            }
            triangleMesh->texCoordIndex.push_back(-1);
        }
    }
}


void TriangleMeshShaper::defaultTextureMappingCylinder(VrmlIndexedFaceSet* triangleMesh)
{
    triangleMesh->texCoord = new VrmlTextureCoordinate();
    SFVec2f texPoint ;
    texPoint[0] = 0.5; texPoint[1] = 0.5;     //center of top(bottom) index=0
    triangleMesh->texCoord->point.push_back(texPoint);
    int texIndex = 1;
    for(int i=0; i<triangleMesh->coordIndex.size(); i++){
        SFVec3f point[3];
        bool notside=true;
        int center=-1;
        for(int j=0; j<3; j++){
            point[j] = triangleMesh->coord->point[triangleMesh->coordIndex[i++]];
            if(j)
                if(point[0][1] == point[j][1] ) notside &= true;
                else   notside &= false;
            if(point[j][0] == 0.0 && point[j][2] == 0.0) center = j;
        }
        if(!notside){         //side
            bool over=false;
            double s[3]={0,0,0};
            for(int j=0; j<3; j++){
                double angle = calcangle(point[j]);
                s[j] = angle/2/PI;
                if(s[j] > 0.5)
                    over = true;
            }
            for(int j=0; j<3; j++){
                if(over && s[j]<1.0e-6)
                    s[j] = 1.0;
                texPoint[0] = s[j];        
                if(point[j][1] > 0) texPoint[1] = 1.0;
                else    texPoint[1] = 0.0;
                int k=findPoint(triangleMesh->texCoord->point, texPoint);
                if(k!=-1){
                    triangleMesh->texCoordIndex.push_back(k);
                }else{
                    triangleMesh->texCoord->point.push_back(texPoint);
                    triangleMesh->texCoordIndex.push_back(texIndex++);
                }
            }
            triangleMesh->texCoordIndex.push_back(-1);
        }else{              // top / bottom
            for(int j=0; j<3; j++){
                if(j!=center){
                    double angle = atan2( point[j][2], point[j][0] );
                    texPoint[0] = 0.5 + 0.5*cos(angle);    
                    if(point[0][1] > 0)  //top
                        texPoint[1] = 0.5 - 0.5*sin(angle);
                    else                //bottom
                        texPoint[1] = 0.5 + 0.5*sin(angle);   
                    int k=findPoint(triangleMesh->texCoord->point, texPoint);
                    if(k!=-1){
                        triangleMesh->texCoordIndex.push_back(k);
                    }else{
                        triangleMesh->texCoord->point.push_back(texPoint);
                        triangleMesh->texCoordIndex.push_back(texIndex++);
                    }
                }else{
                    triangleMesh->texCoordIndex.push_back(0);
                }
            }
            triangleMesh->texCoordIndex.push_back(-1);
        }
    }
}


void TriangleMeshShaper::defaultTextureMappingSphere(VrmlIndexedFaceSet* triangleMesh, double radius)
{
    triangleMesh->texCoord = new VrmlTextureCoordinate();
    SFVec2f texPoint ;
    int texIndex = 0;
    for(int i=0; i<triangleMesh->coordIndex.size(); i++){
        SFVec3f point[3];
        bool over=false;
        double s[3]={0,0,0};
        for(int j=0; j<3; j++){
            point[j] = triangleMesh->coord->point[triangleMesh->coordIndex[i++]];
            double angle = calcangle(point[j]);
            s[j] = angle/2/PI; 
            if(s[j] > 0.5)
                over = true;
        }
        for(int j=0; j<3; j++){
            if(over && s[j]<1.0e-6)
                s[j] = 1.0;
            texPoint[0] = s[j];
            double theta = acos(point[j][1]/radius);        
            texPoint[1] = 1.0-theta/PI;
            int k=findPoint(triangleMesh->texCoord->point, texPoint);
            if(k!=-1){
                triangleMesh->texCoordIndex.push_back(k);
            }else{
                triangleMesh->texCoord->point.push_back(texPoint);
                triangleMesh->texCoordIndex.push_back(texIndex++);
            }
        }
        triangleMesh->texCoordIndex.push_back(-1);
    }
}


void TriangleMeshShaper::defaultTextureMappingExtrusion(VrmlIndexedFaceSet* triangleMesh, VrmlExtrusion* extrusion )
{
    int numSpine = extrusion->spine.size();
    int numcross = extrusion->crossSection.size();
        
    triangleMesh->texCoord = new VrmlTextureCoordinate();
    std::vector<double> s;
    std::vector<double> t;
    double slen=0;
    s.push_back(0);
    for(int i=1; i<extrusion->crossSection.size(); i++){
        double x=extrusion->crossSection[i][0]-extrusion->crossSection[i-1][0];
        double z=extrusion->crossSection[i][1]-extrusion->crossSection[i-1][1];
        slen += sqrt(x*x+z*z);
        s.push_back(slen);
    }
    double tlen=0;
    t.push_back(0);
    for(int i=1; i<extrusion->spine.size(); i++){
        double x=extrusion->spine[i][0]-extrusion->spine[i-1][0];
        double y=extrusion->spine[i][1]-extrusion->spine[i-1][1];
        double z=extrusion->spine[i][2]-extrusion->spine[i-1][2];
        tlen += sqrt(x*x+y*y+z*z);
        t.push_back(tlen);
    }
    for(int i=0; i<extrusion->spine.size(); i++){
        SFVec2f point;
        point[1] = t[i]/tlen;
        for(int j=0; j<extrusion->crossSection.size(); j++){
            point[0] = s[j]/slen;
            triangleMesh->texCoord->point.push_back(point);
        }
    }

    int endofspin = (numSpine-1)*(numcross-1)*2*4;
    triangleMesh->texCoordIndex.resize(endofspin);
    copy( triangleMesh->coordIndex.begin(), triangleMesh->coordIndex.begin()+endofspin, 
		triangleMesh->texCoordIndex.begin() );
    int endofbegincap = endofspin;
    int endofpoint = triangleMesh->texCoord->point.size();

    if(extrusion->beginCap){
        if(extrusion->endCap)
            endofbegincap += (triangleMesh->coordIndex.size()-endofspin)/2;
        else
            endofbegincap = triangleMesh->coordIndex.size();
        double xmin, xmax;
        double zmin, zmax;
        xmin = xmax = extrusion->crossSection[0][0];
        zmin = zmax = extrusion->crossSection[0][1];
        for(int i=1; i<extrusion->crossSection.size(); i++){
            xmax = std::max(xmax,extrusion->crossSection[i][0]);
            xmin = std::min(xmin,extrusion->crossSection[i][0]);
            zmax = std::max(zmax,extrusion->crossSection[i][1]);
            zmin = std::min(xmin,extrusion->crossSection[i][1]);
        }
        double xsize = xmax-xmin;
        double zsize = zmax-zmin;
        for(int i=0; i<numcross; i++){
            SFVec2f point;
            point[0] = (extrusion->crossSection[i][0]-xmin)/xsize;
            point[1] = (extrusion->crossSection[i][1]-zmin)/zsize;
            triangleMesh->texCoord->point.push_back(point);
        }
        for(int i=endofspin; i<endofbegincap; i++){
            int k=triangleMesh->coordIndex[i];
            if(k != -1)
                triangleMesh->texCoordIndex.push_back(k+endofpoint);
            else
                triangleMesh->texCoordIndex.push_back(-1);
        }
    }

    if(extrusion->endCap){
        double xmax,xmin;
        double zmax,zmin;
        xmin = xmax = extrusion->crossSection[0][0];
        zmin = zmax = extrusion->crossSection[0][1];
        for(int i=1; i<extrusion->crossSection.size(); i++){
            xmax = std::max(xmax,extrusion->crossSection[i][0]);
            xmin = std::min(xmin,extrusion->crossSection[i][0]);
            zmax = std::max(zmax,extrusion->crossSection[i][1]);
            zmin = std::min(xmin,extrusion->crossSection[i][1]);
        }
        double xsize = xmax-xmin;
        double zsize = zmax-zmin;
        for(int i=0; i<extrusion->crossSection.size(); i++){
            SFVec2f point;
            point[0] = (extrusion->crossSection[i][0]-xmin)/xsize;
            point[1] = (extrusion->crossSection[i][1]-zmin)/zsize;
            triangleMesh->texCoord->point.push_back(point);
        }
        for(int i=endofbegincap; i<triangleMesh->coordIndex.size(); i++){
            int k=triangleMesh->coordIndex[i];
            if(k!=-1)   
                triangleMesh->texCoordIndex.push_back(triangleMesh->texCoord->point.size()+k-endofpoint);
            else
                triangleMesh->texCoordIndex.push_back(-1);
        }
    }
}

 bool TriangleMeshShaper::convertBox(VrmlBox* box, VrmlIndexedFaceSetPtr& triangleMesh){
    return impl->convertBox(box, triangleMesh);
}
