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


#ifndef OPENHRP_PARSER_NORMAL_GENERATOR_H_INCLUDED
#define OPENHRP_PARSER_NORMAL_GENERATOR_H_INCLUDED

#include "config.h"
#include "TriangleMeshShaper.h"

namespace OpenHRP
{
    //! Calculate Normal class
    class HRP_PARSER_EXPORT NormalGenerator
    {
      public:
        bool calculateNormalsOfVertex
            (const std::vector<Vector3>& vertexList, const std::vector<vector3i>& triangleList, double creaseAngle );
        bool calculateNormalsOfMesh(const std::vector<Vector3>& vertexList, const std::vector<vector3i>& triangleList );

        const std::vector<Vector3>& getNormalsOfVertex() { return _normalsOfVertex; }
        const std::vector<vector3i>& getNormalIndex() { return _normalIndex; }
        const std::vector<Vector3>& getNormalsOfMesh() { return _normalsOfMesh; }

      private:
        std::vector<Vector3> _normalsOfVertex;
        std::vector<vector3i> _normalIndex;
        std::vector<Vector3> _normalsOfMesh; 

        std::vector< std::vector< long > > vertexContainedInMeshList_;

        Vector3	_calculateNormalOfTraiangleMesh( Vector3 a,	Vector3 b,	Vector3 c );
    };
};

#endif
