/*! @file
  @brief Header file of Calculate Normal class
  @author Y.TSUNODA
*/

#ifndef CALCULATENORMAL_H_INCLUDED
#define CALCULATENORMAL_H_INCLUDED

#include "ModelUniformConfig.h"
#include "UniformedShape.h"

using namespace std;

namespace OpenHRP
{
	//! Calculate Normal class
	class MODELUNIFORM_EXPORT CalculateNormal
	{
	public:
		bool calculateNormalsOfVertex( vector<vector3d> vertexList, vector<vector3i> triangleList, double creaseAngle );
		bool calculateNormalsOfMesh( vector<vector3d> vertexList, vector<vector3i> triangleList );

		vector<vector3d> getNormalsOfVertex() { return _normalsOfVertex; }
		vector<vector3i> getNormalIndex() { return _normalIndex; }
		vector<vector3d> getNormalsOfMesh() { return _normalsOfMesh; }

	private:
		vector<vector3d> _normalsOfVertex;
		vector<vector3i> _normalIndex;
		vector<vector3d> _normalsOfMesh; 

		vector< vector< long > > vertexContainedInMeshList_;

		vector3d	_calculateNormalOfTraiangleMesh( vector3d a,	vector3d b,	vector3d c );
	};
};

#endif	// CALCULATENORMAL_H_INCLUDED
