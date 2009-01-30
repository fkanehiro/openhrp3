/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */
/**
   @author Shin'ichiro Nakaoka
*/

#ifndef OPENHRP_COLLISION_DETECTOR_COLDET_BODY_H_INCLUDED
#define OPENHRP_COLLISION_DETECTOR_COLDET_BODY_H_INCLUDED

#include <map>
#include <vector>
#include <string>
#include <boost/shared_ptr.hpp>
#include <hrpUtil/Tvmet4d.h>
#include <hrpCorba/ModelLoader.hh>
#include <hrpCollision/ColdetModel.h>

using namespace std;
using namespace boost;
using namespace hrp;
using namespace OpenHRP;


class ColdetBody
{
public:
    ColdetBody(BodyInfo_ptr bodyInfo);

    /**
       do shallow copy (sharing the same ColdetModel instances)
    */
    ColdetBody(const ColdetBody& org);

    void setName(const char* name) { name_ = name; }
    const char* name() { return name_.c_str(); }
    
    unsigned int numLinks() const {
        return linkColdetModels.size();
    }
    ColdetModelPtr linkColdetModel(int linkIndex) {
        return linkColdetModels[linkIndex];
    }

    ColdetModelPtr linkColdetModel(const string& linkName){
        map<string, ColdetModelPtr>::iterator p = linkNameToColdetModelMap.find(linkName);
        return (p == linkNameToColdetModelMap.end()) ? ColdetModelPtr() : p->second;
    }

    void setLinkPositions(const LinkPositionSequence& linkPositions);

  private:
    void addLinkPrimitiveInfo(ColdetModelPtr& coldetModel, 
                              const double *R, const double *p,
                              const ShapeInfo& shapeInfo);
    void addLinkVerticesAndTriangles
        (ColdetModelPtr& coldetModel, LinkInfo& linkInfo, ShapeInfoSequence_var& shapes);
    void addLinkVerticesAndTriangles
        (ColdetModelPtr& coldetModel, const TransformedShapeIndex& tsi, const Matrix44& Tparent, ShapeInfoSequence_var& shapes, int& vertexIndex, int& triangleIndex);
    
    vector<ColdetModelPtr> linkColdetModels;
    map<string, ColdetModelPtr> linkNameToColdetModelMap;
    string name_;
};

typedef boost::shared_ptr<ColdetBody> ColdetBodyPtr;

#endif
