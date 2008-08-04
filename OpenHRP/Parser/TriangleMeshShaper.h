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
#include "ModelNodeSet.h"
#include <OpenHRP/Util/Tvmet3d.h>
#include <vector>


namespace OpenHRP
{
    class HRP_PARSER_EXPORT TriangleMeshShaper
    {
      public:

        TriangleMeshShaper();

        void setDivisionNumber(int n);
        void setNormalGenerationMode(bool on);
        VrmlNodePtr apply(VrmlNodePtr topNode);
        VrmlGeometryPtr getOriginalGeometry(VrmlShapePtr shapeNode);
        
        boost::signal<void(const std::string& message)> sigMessage;

      private:

        int divisionNumber;
        bool isNormalGenerationMode;

        typedef std::map<VrmlShapePtr, VrmlGeometryPtr> ShapeToGeometryMap;
        ShapeToGeometryMap shapeToOriginalGeometryMap;

        // for triangulation
        std::vector<int> polygon;
        std::vector<int> trianglesInPolygon;
        std::vector<int> indexPositionMap;
        std::vector<int> faceIndexMap;

        // for normal generation
        std::vector<Vector3> faceNormals;
        std::vector< std::vector<int> > vertexIndexToFaceIndicesMap;
        std::vector<int> faceIndexToFaceNormalIndexMap;
        std::vector< std::vector<int> > vertexIndexToNormalIndicesMap;

        enum RemapType { REMAP_COLOR, REMAP_NORMAL };

        bool traverseShapeNodes(VrmlNode* node, AbstractVrmlGroup* parentNode, int indexInParent);
        bool convertShapeNode(VrmlShape* shapeNode);
        bool convertIndexedFaceSet(VrmlIndexedFaceSet* faceSet);

        int addTrianglesDividedFromPolygon(const std::vector<int>& polygon, const MFVec3f& vertices,
                                           std::vector<int>& out_trianglesInPolygon);

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
        void calculateVertexNormals(VrmlIndexedFaceSetPtr& triangleMesh);
        void setFaceNormals(VrmlIndexedFaceSetPtr& triangleMesh);

        void putMessage(const std::string& message);
    };
};

#endif

