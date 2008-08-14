/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

/*!
  @file BodyInfo_impl.h
  @author Shin'ichiro Nakaoka
  @author Y.TSUNODA
*/

#ifndef OPENHRP_MODEL_LOADER_BODYINFO_IMPL_H_INCLUDED
#define OPENHRP_MODEL_LOADER_BODYINFO_IMPL_H_INCLUDED

#include <string>
#include <vector>

#include <hrpCorba/ORBwrap.h>
#include <hrpCorba/ModelLoader.h>

#include <hrpParser/ModelNodeSet.h>
#include <hrpParser/TriangleMeshShaper.h>
#include <hrpParser/VrmlNodes.h>

#include <hrpUtil/Tvmet3d.h>
#include <hrpUtil/Tvmet4d.h>

using namespace OpenHRP;
using namespace hrp;

class BodyInfo_impl : public POA_OpenHRP::BodyInfo
{
  public:
		
    BodyInfo_impl(PortableServer::POA_ptr poa);
    virtual ~BodyInfo_impl();

    virtual PortableServer::POA_ptr _default_POA();
		
    virtual char* name();
    virtual char* url();
    virtual StringSequence* info();
    virtual LinkInfoSequence* links();
    virtual AllLinkShapeIndexSequence* linkShapeIndices();
    virtual ShapeInfoSequence* shapes();
    virtual AppearanceInfoSequence* appearances();
    virtual MaterialInfoSequence* materials();
    virtual TextureInfoSequence* textures();

    void loadModelFile(const std::string& filename);

    void setLastUpdateTime(time_t time) { lastUpdate_ = time;};
    time_t getLastUpdateTime() { return lastUpdate_; }

  private:
        
    PortableServer::POA_var poa;
		
    time_t lastUpdate_;

    std::string name_;
    std::string url_;
    StringSequence info_;
    LinkInfoSequence links_;
    ShapeInfoSequence  shapes_;
    AppearanceInfoSequence appearances_;
    MaterialInfoSequence materials_;
    TextureInfoSequence textures_;
    AllLinkShapeIndexSequence linkShapeIndices_;

    TriangleMeshShaper triangleMeshShaper;
        
    typedef std::map<VrmlShapePtr, int> ShapeNodeToShapeInfoIndexMap;
    ShapeNodeToShapeInfoIndexMap shapeInfoIndexMap;

    int readJointNodeSet(JointNodeSetPtr jointNodeSet, int& currentIndex, int motherIndex);
    void setJointParameters(int linkInfoIndex, VrmlProtoInstancePtr jointNode );
    void setSegmentParameters(int linkInfoIndex, VrmlProtoInstancePtr segmentNode );
    void setSensors(int linkInfoIndex, JointNodeSetPtr jointNodeSet );
    void readSensorNode(int linkInfoIndex, SensorInfo& sensorInfo, VrmlProtoInstancePtr sensorNode);

    void traverseShapeNodes(int linkInfoIndex, MFNode& childNodes, const Matrix44& T);
    void calcTransformMatrix(VrmlTransformPtr transform, Matrix44& out_T);
    int createShapeInfo(VrmlShapePtr shapeNode);
    void setTriangleMesh(ShapeInfo& shapeInfo, VrmlIndexedFaceSet* triangleMesh);
    void setPrimitiveProperties(ShapeInfo& shapeInfo, VrmlShapePtr shapeNode);
    int createAppearanceInfo(ShapeInfo& shapeInfo, VrmlShapePtr& shapeNode, VrmlIndexedFaceSet* faceSet);
    void setColors(AppearanceInfo& appInfo, VrmlIndexedFaceSet* triangleMesh);
    void setNormals(AppearanceInfo& appInfo, VrmlIndexedFaceSet* triangleMesh);
    int createMaterialInfo(VrmlMaterialPtr materialNode);
    int createTextureInfo(VrmlTexturePtr textureNode);
    std::string getModelFileDirPath();
};


#endif
