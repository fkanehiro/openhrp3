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
#include <hrpCorba/ModelLoader.h>
#include <hrpParser/TriangleMeshShaper.h>
#include <hrpParser/VrmlNodes.h>
#include <hrpUtil/Tvmet4d.h>

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
    void traverseShapeNodes(VrmlNode* node, const Matrix44& T, TransformedShapeIndexSequence& io_shapeIndices);
    virtual const std::string& topUrl() = 0;

private:
        
    PortableServer::POA_var poa;
		
    ShapeInfoSequence  shapes_;
    AppearanceInfoSequence appearances_;
    MaterialInfoSequence materials_;
    TextureInfoSequence textures_;

    TriangleMeshShaper triangleMeshShaper;
        
    typedef std::map<VrmlShapePtr, int> ShapeNodeToShapeInfoIndexMap;
    ShapeNodeToShapeInfoIndexMap shapeInfoIndexMap;

    void calcTransformMatrix(VrmlTransform* transform, Matrix44& out_T);
    int createShapeInfo(VrmlShape* shapeNode);
    void setTriangleMesh(ShapeInfo& shapeInfo, VrmlIndexedFaceSet* triangleMesh);
    void setPrimitiveProperties(ShapeInfo& shapeInfo, VrmlShape* shapeNode);
    int createAppearanceInfo(ShapeInfo& shapeInfo, VrmlShape* shapeNode, VrmlIndexedFaceSet* faceSet);
    void setColors(AppearanceInfo& appInfo, VrmlIndexedFaceSet* triangleMesh);
    void setNormals(AppearanceInfo& appInfo, VrmlIndexedFaceSet* triangleMesh);
    void setTexCoords(AppearanceInfo& appInfo, VrmlIndexedFaceSet* triangleMesh);
    int createMaterialInfo(VrmlMaterialPtr& materialNode);
    int createTextureInfo(VrmlTexturePtr& textureNode);
    void createTextureTransformMatrix(AppearanceInfo& appInfo, VrmlTextureTransformPtr& textureTransform );
    std::string getModelFileDirPath(const std::string& url);
};


#endif
