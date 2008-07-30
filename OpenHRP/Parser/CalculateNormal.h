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
  @author Y.TSUNODA
  @author Shin'ichiro Nakaoka
*/


#ifndef OPENHRP_PARSER_CALCULATENORMAL_H_INCLUDED
#define OPENHRP_PARSER_CALCULATENORMAL_H_INCLUDED

#include "config.h"
#include "TriangleMeshGenerator.h"

namespace OpenHRP
{
    //! Calculate Normal class
    class HRP_PARSER_EXPORT CalculateNormal
    {
      public:
        bool calculateNormalsOfVertex
            (const std::vector<vector3d>& vertexList, const std::vector<vector3i>& triangleList, double creaseAngle );
        bool calculateNormalsOfMesh(const std::vector<vector3d>& vertexList, const std::vector<vector3i>& triangleList );

        const std::vector<vector3d>& getNormalsOfVertex() { return _normalsOfVertex; }
        const std::vector<vector3i>& getNormalIndex() { return _normalIndex; }
        const std::vector<vector3d>& getNormalsOfMesh() { return _normalsOfMesh; }

      private:
        std::vector<vector3d> _normalsOfVertex;
        std::vector<vector3i> _normalIndex;
        std::vector<vector3d> _normalsOfMesh; 

        std::vector< std::vector< long > > vertexContainedInMeshList_;

        vector3d	_calculateNormalOfTraiangleMesh( vector3d a,	vector3d b,	vector3d c );
    };
};

#endif
