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
*/

#ifndef OPENHRP_MODEL_LOADER_BODYINFO_IMPL_H_INCLUDED
#define OPENHRP_MODEL_LOADER_BODYINFO_IMPL_H_INCLUDED

#include <string>
#include <hrpCorba/ORBwrap.h>
#include <hrpCorba/ModelLoader.hh>
#include <hrpParser/ModelNodeSet.h>

#include "ShapeSetInfo_impl.h"

using namespace OpenHRP;
using namespace hrp;

class BodyInfo_impl :
    public virtual POA_OpenHRP::BodyInfo,
    public virtual ShapeSetInfo_impl
{
  public:
		
    BodyInfo_impl(PortableServer::POA_ptr poa);
    virtual ~BodyInfo_impl();

    virtual char* name();
    virtual char* url();
    virtual StringSequence* info();
    virtual LinkInfoSequence* links();
    virtual AllLinkShapeIndexSequence* linkShapeIndices();

    void loadModelFile(const std::string& filename);

    void setLastUpdateTime(time_t time) { lastUpdate_ = time;};
    time_t getLastUpdateTime() { return lastUpdate_; }

protected:

    virtual const std::string& topUrl();

private:
        
    time_t lastUpdate_;

    std::string name_;
    std::string url_;
    StringSequence info_;
    LinkInfoSequence links_;
    AllLinkShapeIndexSequence linkShapeIndices_;

    int readJointNodeSet(JointNodeSetPtr jointNodeSet, int& currentIndex, int motherIndex);
    void setJointParameters(int linkInfoIndex, VrmlProtoInstancePtr jointNode );
    void setSegmentParameters(int linkInfoIndex, VrmlProtoInstancePtr segmentNode);
    void setSensors(int linkInfoIndex, JointNodeSetPtr jointNodeSet);
    void readSensorNode(int linkInfoIndex, SensorInfo& sensorInfo, VrmlProtoInstancePtr sensorNode);
};


#endif
