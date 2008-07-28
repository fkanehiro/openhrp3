/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */

/*!
  @file BodyInfo_impl.h
  @author Shin'ichiro Nakaoka
  @author Y.TSUNODA (Ergovision)
*/

#ifndef OPENHRP_BODYINFO_IMPL_H_INCLUDED
#define OPENHRP_BODYINFO_IMPL_H_INCLUDED

#include <string>
#include <vector>

#include <OpenHRP/Corba/ORBwrap.h>
#include <OpenHRP/Corba/ModelLoader.h>

#include <OpenHRP/Parser/ModelNodeSet.h>
#include <OpenHRP/Parser/UniformedShape.h>
#include <OpenHRP/Parser/VrmlNodes.h>

using namespace std;

namespace OpenHRP
{
    class BodyInfo_impl : public POA_OpenHRP::BodyInfo
    {
    public:
		
        BodyInfo_impl(PortableServer::POA_ptr poa);
        ~BodyInfo_impl();

        void loadModelFile(const std::string& filename);

        string& replace(string& str, const string sb, const string sa);

        void setLastUpdateTime(time_t time) { lastUpdate_ = time;};
        time_t getLastUpdateTime() { return lastUpdate_; }

        virtual PortableServer::POA_ptr _default_POA();
		
        virtual char* name();
        virtual char* url();
        virtual StringSequence* info();
        virtual LinkInfoSequence* links();

        virtual AllLinkShapeIndices* linkShapeIndices();
        virtual ShapeInfoSequence* shapes();
        virtual AppearanceInfoSequence* appearances();
        virtual MaterialInfoSequence* materials();
        virtual TextureInfoSequence* textures();

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
        AllLinkShapeIndices linkShapeIndices_;
        
        /// ShapeInfoのindexと，そのshapeを算出したtransformのペア
        struct ShapeObject
        {
            matrix44d transform;
            short     index;
        };

        /**
          Map for sharing shapeInfo
          if it is node that has already stored in shape_, it has the corresponding index.
        */
        typedef map<OpenHRP::VrmlShapePtr, ShapeObject> SharedShapeInfoMap;

        SharedShapeInfoMap sharedShapeInfoMap;

        int readJointNodeSet(JointNodeSetPtr jointNodeSet, int& currentIndex, int motherIndex);

        void putMessage(const std::string& message);

        void setJointParameters(int linkInfoIndex, VrmlProtoInstancePtr jointNode );
        void setSegmentParameters(int linkInfoIndex, VrmlProtoInstancePtr segmentNode );
        void setSensors(int linkInfoIndex, JointNodeSetPtr jointNodeSet );
        void readSensorNode(int linkInfoIndex, SensorInfo& sensorInfo, VrmlProtoInstancePtr sensorNode);

        void traverseShapeNodes(int index, MFNode& childNodes, const matrix44d& transform);
        int createShapeInfo(VrmlShapePtr shapeNode, const matrix44d& transform);
        int createAppearanceInfo(VrmlShapePtr shapeNode, UniformedShape& uniformedShape, const matrix44d& transform);

        void setVertices(ShapeInfo_var& shape, const vector<vector3d>& vertices, const matrix44d& transform);
        void setTriangles(ShapeInfo_var& shape, const vector<vector3i>& triangles);
        void setNormals(AppearanceInfo_var& appearance, const vector<vector3d>& vertexList,
                        const vector<vector3i>& traiangleList, const matrix44d& transform);
        void setShapeInfoType(ShapeInfo_var& shapeInfo, UniformedShape::ShapePrimitiveType type);
        int createTextureInfo(VrmlTexturePtr textureNode);
        int createMaterialInfo(VrmlMaterialPtr materialNode);
        void calcTransform(VrmlTransformPtr transform, matrix44d& out_matrix);
        string getModelFileDirPath();        
    };
};


#endif
