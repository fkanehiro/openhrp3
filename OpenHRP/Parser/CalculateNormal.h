/*! @file
  @brief Header file of Calculate Normal class
  @author Y.TSUNODA
*/

#ifndef OPENHRP_CALCULATENORMAL_H_INCLUDED
#define OPENHRP_CALCULATENORMAL_H_INCLUDED

#include "ModelUniformConfig.h"
#include "UniformedShape.h"

using namespace std;

namespace OpenHRP
{
    //! Calculate Normal class
    class MODELUNIFORM_EXPORT CalculateNormal
    {
      public:
        bool calculateNormalsOfVertex
            (const vector<vector3d>& vertexList, const vector<vector3i>& triangleList, double creaseAngle );
        bool calculateNormalsOfMesh(const vector<vector3d>& vertexList, const vector<vector3i>& triangleList );

        const vector<vector3d>& getNormalsOfVertex() { return _normalsOfVertex; }
        const vector<vector3i>& getNormalIndex() { return _normalIndex; }
        const vector<vector3d>& getNormalsOfMesh() { return _normalsOfMesh; }

      private:
        vector<vector3d> _normalsOfVertex;
        vector<vector3i> _normalIndex;
        vector<vector3d> _normalsOfMesh; 

        vector< vector< long > > vertexContainedInMeshList_;

        vector3d	_calculateNormalOfTraiangleMesh( vector3d a,	vector3d b,	vector3d c );
    };
};

#endif	// CALCULATENORMAL_H_INCLUDED
