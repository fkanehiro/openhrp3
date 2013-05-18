// -*- coding: utf-8 -*-
// Copyright (C) 2011 University of Tokyo, General Robotix Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//     http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
/*!
  @file BodyInfoCollada_impl.cpp
  @brief 
  @author Rosen Diankov (rosen.diankov@gmail.com)

  Used OpenRAVE files for reference.
*/
#ifndef MODELNODE_COLLADA_IMPL_H
#define MODELNODE_COLLADA_IMPL_H

#include <string>
#include <hrpCorba/ORBwrap.h>
#include <hrpCorba/ModelLoader.hh>
#include <hrpCollision/ColdetModel.h>

#include <boost/thread/thread.hpp>

#include "ShapeSetInfo_impl.h"

class ColladaReader;

/// \brief reads in collada files and initializes a BodyInfo struct
class BodyInfoCollada_impl :
    public virtual ShapeSetInfo_impl,
    public virtual POA_OpenHRP::BodyInfo
{
  public:
		
    BodyInfoCollada_impl(PortableServer::POA_ptr poa);
    virtual ~BodyInfoCollada_impl();

    virtual char* name();
    virtual char* url();
    virtual StringSequence* info();
    virtual LinkInfoSequence* links();
    virtual AllLinkShapeIndexSequence* linkShapeIndices();
	virtual ExtraJointInfoSequence* extraJoints();

    void loadModelFile(const std::string& filename);
    void setLastUpdateTime(time_t time) { lastUpdate_ = time;};
    time_t getLastUpdateTime() { return lastUpdate_; }
    bool checkInlineFileUpdateTime();

    bool getParam(std::string param);
    void setParam(std::string param, bool value);
    void setParam(std::string param, int value);
    void changetoBoundingBox(unsigned int* depth) ; 

protected:

    virtual const std::string& topUrl();

private:
        
    time_t lastUpdate_;
    std::map<std::string, time_t> fileTimeMap;
    bool readImage_;
    OpenHRP::ModelLoader::AABBdataType AABBdataType_;

    std::string name_;
    std::string url_;
    StringSequence info_;
    LinkInfoSequence links_;
    AllLinkShapeIndexSequence linkShapeIndices_;
	ExtraJointInfoSequence extraJoints_;

    std::vector<ColdetModelPtr> linkColdetModels;

    void setColdetModel(ColdetModelPtr& coldetModel, TransformedShapeIndexSequence shapeIndices, const Matrix44& Tparent, int& vertexIndex, int& triangleIndex);
    void setColdetModelTriangles(ColdetModelPtr& coldetModel, const TransformedShapeIndex& tsi, const Matrix44& Tparent, int& vertexIndex, int& triangleIndex);

    static boost::mutex lock_;

    friend class ColladaReader;
};

#endif
