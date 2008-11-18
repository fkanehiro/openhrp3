/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */

/*! @file
  @author Shin'ichiro Nakaoka
*/


#ifndef OPENHRP_PARSER_TRIANGLE_MESH_SHAPER_H_INCLUDED
#define OPENHRP_PARSER_TRIANGLE_MESH_SHAPER_H_INCLUDED

#include "config.h"
#include "VrmlNodes.h"
#include "triangulator/geometry.h"
#include "triangulator/triangulator.h"
#include <string>
#include <boost/signal.hpp>

namespace hrp
{
    class TMSImpl;
    
    class HRP_PARSER_EXPORT TriangleMeshShaper
    {
      public:

        TriangleMeshShaper();
        ~TriangleMeshShaper();

        void setDivisionNumber(int n);
        void setNormalGenerationMode(bool on);
        VrmlNodePtr apply(VrmlNodePtr topNode);
        SFNode getOriginalGeometry(VrmlShapePtr shapeNode);
        void defaultTextureMapping(VrmlShape* shapeNode);
        
        boost::signal<void(const std::string& message)> sigMessage;

      private:
        TMSImpl* impl;

        void defaultTextureMappingFaceSet(VrmlIndexedFaceSet* triangleMesh);
        void defaultTextureMappingElevationGrid(VrmlElevationGrid* grid, VrmlIndexedFaceSet* triangleMesh);
        void defaultTextureMappingBox(VrmlIndexedFaceSet* triangleMesh);
        void defaultTextureMappingCone(VrmlIndexedFaceSet* triangleMesh);
        void defaultTextureMappingCylinder(VrmlIndexedFaceSet* triangleMesh);
        void defaultTextureMappingSphere(VrmlIndexedFaceSet* triangleMesh, double radius);
        void defaultTextureMappingExtrusion(VrmlIndexedFaceSet* triangleMesh, VrmlExtrusion* extrusion );
        int faceofBox(SFVec3f* point);
        int findPoint(MFVec2f& points, SFVec2f& target);
        double calcangle(SFVec3f& point);
    };

    enum { LEFT, TOP, FRONT, BOTTOM, RIGHT, BACK };

};

#endif

