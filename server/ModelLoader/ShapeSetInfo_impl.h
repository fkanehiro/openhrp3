/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

/*!
  @file ShapeSetInfo_impl.h
  @author Shin'ichiro Nakaoka
*/

#ifndef OPENHRP_MODEL_LOADER_SHAPE_SET_INFO_INPL_H_INCLUDED
#define OPENHRP_MODEL_LOADER_SHAPE_SET_INFO_INPL_H_INCLUDED

#include <string>
#include <hrpCorba/ORBwrap.h>
#include <hrpCorba/ModelLoader.hh>
#include <hrpUtil/TriangleMeshShaper.h>
#include <hrpUtil/VrmlNodes.h>
#include <hrpUtil/Eigen3d.h>
#include <hrpUtil/Eigen4d.h>
#include <hrpCollision/ColdetModel.h>

using namespace OpenHRP;
using namespace hrp;

class ShapeSetInfo_impl : public virtual POA_OpenHRP::ShapeSetInfo
{
public:
		
    ShapeSetInfo_impl(PortableServer::POA_ptr poa);
    virtual ~ShapeSetInfo_impl();

    virtual PortableServer::POA_ptr _default_POA();
		
    virtual ShapeInfoSequence* shapes();
    virtual AppearanceInfoSequence* appearances();
    virtual MaterialInfoSequence* materials();
    virtual TextureInfoSequence* textures();

protected:

    void applyTriangleMeshShaper(VrmlNodePtr node);
    static void putMessage(const std::string& message);
    std::string& replace(std::string& str, const std::string& sb, const std::string& sa);
    void traverseShapeNodes(VrmlNode* node, const Matrix44& T, TransformedShapeIndexSequence& io_shapeIndices, DblArray12Sequence& inlinedShapeM, const SFString* url = NULL);
    virtual const std::string& topUrl() = 0;
    void setColdetModel(ColdetModelPtr& coldetModel, TransformedShapeIndexSequence shapeIndices, const Matrix44& Tparent, int& vertexIndex, int& triangleIndex);
    void saveOriginalData();
    void restoreOriginalData();
    void createAppearanceInfo();
    void setBoundingBoxData(const Vector3& boxSize, int shapeIndex);
    bool checkFileUpdateTime();
    bool readImage;

private:
        
    PortableServer::POA_var poa;
		
    ShapeInfoSequence  shapes_;
    AppearanceInfoSequence appearances_;
    MaterialInfoSequence materials_;
    TextureInfoSequence textures_;

    ShapeInfoSequence  originShapes_;
    AppearanceInfoSequence originAppearances_;
    MaterialInfoSequence originMaterials_;

    TriangleMeshShaper triangleMeshShaper;
        
    typedef std::map<VrmlShapePtr, int> ShapeNodeToShapeInfoIndexMap;
    ShapeNodeToShapeInfoIndexMap shapeInfoIndexMap;

    std::map<std::string, time_t> fileTimeMap;

    int createShapeInfo(VrmlShape* shapeNode, const SFString* url);
    void setTriangleMesh(ShapeInfo& shapeInfo, VrmlIndexedFaceSet* triangleMesh);
    void setPrimitiveProperties(ShapeInfo& shapeInfo, VrmlShape* shapeNode);
    int createAppearanceInfo(ShapeInfo& shapeInfo, VrmlShape* shapeNode, VrmlIndexedFaceSet* faceSet, const SFString *url);
    void setColors(AppearanceInfo& appInfo, VrmlIndexedFaceSet* triangleMesh);
    void setNormals(AppearanceInfo& appInfo, VrmlIndexedFaceSet* triangleMesh);
    void setTexCoords(AppearanceInfo& appInfo, VrmlIndexedFaceSet* triangleMesh);
    int createMaterialInfo(VrmlMaterialPtr& materialNode);
    int createTextureInfo(VrmlTexturePtr& textureNode, const SFString *url);
    void createTextureTransformMatrix(AppearanceInfo& appInfo, VrmlTextureTransformPtr& textureTransform );
    std::string getModelFileDirPath(const std::string& url);
    void setColdetModelTriangles(ColdetModelPtr& coldetModel, const TransformedShapeIndex& tsi, const Matrix44& Tparent, int& vertexIndex, int& triangleIndex);

    friend class BodyInfo_impl;
    friend class SceneInfo_impl;
#ifdef OPENHRP_COLLADA_FOUND
    friend class ColladaReader;
    friend class BodyInfoCollada_impl;
    friend class SceneInfoCollada_impl;
#endif
};
#endif
