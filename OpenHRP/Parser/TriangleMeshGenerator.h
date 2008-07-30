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


#ifndef OPENHRP_PARSER_TRIANGLE_MESH_GENERATOR_H_INCLUDED
#define OPENHRP_PARSER_TRIANGLE_MESH_GENERATOR_H_INCLUDED

#include "config.h"
#include "VrmlNodes.h"
#include "ModelNodeSet.h"

#include <vector>

// for tvmet
#ifdef _WIN32
#pragma warning( disable : 4251 4275 4661 )
#undef min
#undef max
#endif
#include <tvmet/Matrix.h>
#include <tvmet/Vector.h>


namespace OpenHRP
{
    typedef tvmet::Matrix<double, 3, 3>	matrix33d;
    typedef tvmet::Matrix<double, 4, 4>	matrix44d;
    typedef tvmet::Vector<double, 3>	vector3d;
    typedef tvmet::Vector<double, 4>	vector4d;
    typedef tvmet::Vector<int, 3>       vector3i;

    //! Uniformd Shape class
    class HRP_PARSER_EXPORT TriangleMeshGenerator
    {
      public:
        //! enumeration of primitive shape types
        enum ShapePrimitiveType
        {
            S_UNKNOWN_TYPE = 0,
            S_BOX,
            S_CONE,
            S_CYLINDER,
            S_SPHERE,
            S_INDEXED_FACE_SET,
            S_ELEVATION_GRID,
            S_EXTRUSION,
            SHAPE_TYPES_NUM
        };

        TriangleMeshGenerator();

        bool setFlgUniformIndexedFaceSet( bool val );
        bool uniform( VrmlNodePtr node );
        bool uniform( ModelNodeSet& modelNodeSet );
        ShapePrimitiveType  getShapeType(){ return type_; };
        const std::vector<vector3d>& getVertexList(){ return vertexList_; };
        const std::vector<vector3i>& getTriangleList(){ return triangleList_; };

        boost::signal<void(const std::string& message)> signalOnStatusMessage;
        bool setMessageOutput( bool val ) { return( flgMessageOutput_ = val ); }

      private:
        ShapePrimitiveType  type_;						//!< primitive type
        std::vector<vector3d>	vertexList_;				//!< vertex list
        std::vector<vector3i>	triangleList_;				//!< triangle mesh list
        bool				flgUniformIndexedFaceSet_;
        bool				flgMessageOutput_;

        bool uniformBox( VrmlBoxPtr box );
        bool uniformCone( VrmlConePtr cone, int divisionNumber = 20 );
        bool uniformCylinder( VrmlCylinderPtr cylinder, int divisionNumber = 20 );
        bool uniformSphere( VrmlSpherePtr sphere, int vDivisionNumber = 20, int hDivisionNumber = 20 );
        bool uniformIndexedFaceSet( VrmlIndexedFaceSetPtr faceSet );
        bool uniformElevationGrid( VrmlElevationGridPtr elevationGrid );
        bool uniformExtrusion( VrmlExtrusionPtr extrusion );

        size_t _addVertexList( vector3d v );
        size_t _addTriangleList( int v1, int v2, int v3, bool ccw = true );
        size_t _addTriangleList( vector3i t );

        int _traverseJointNode(	JointNodeSetPtr, int&, int );
        void _traverseShapeNodes( MFNode& childNodes );

        int _createTriangleMesh( std::vector<int> mesh, bool ccw );

        void putMessage( const std::string& message );
    };
};

#endif

