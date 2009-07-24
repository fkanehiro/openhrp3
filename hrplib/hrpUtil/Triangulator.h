/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

/*! @file
  @author Shin'ichiro Nakaoka
*/

#ifndef HRPUTIL_TRIANGULATOR_H_INCLUDED
#define HRPUTIL_TRIANGULATOR_H_INCLUDED

#include <vector>
#include <boost/dynamic_bitset.hpp>
#include <hrpUtil/Tvmet3d.h>
#include "VrmlNodes.h"

namespace hrp {
    
    class Triangulator
    {
    public:

        void setVertices(const MFVec3f& vertices) {
            this->vertices = &vertices;
        }

        /**
           @return The number of triangles
        */
        int triangulate(const std::vector<int>& polygon);

        /**
           Triangulated indices.
           This value is available after calling the 'triangulate' method.
           The indices are local ones in the polygon index vector given to the triangulate method.
        */
        const std::vector<int>& triangles() {
            return triangles_;
        }

    private:

        enum Convexity { FLAT, CONVEX, CONCAVE };
        
        const MFVec3f* vertices;
        const std::vector<int>* orgPolygon;                                                                  
        std::vector<int> triangles_;
        std::vector<int> workPolygon;
        Vector3 ccs; // cyclic cross sum
        boost::dynamic_bitset<> earMask;

        Vector3Ref vertex(int localIndex){
            return getVector3Ref((*vertices)[(*orgPolygon)[localIndex]].data());
        }

        Vector3Ref workVertex(int workPolygonIndex){
            return getVector3Ref((*vertices)[(*orgPolygon)[workPolygon[workPolygonIndex]]].data());
        }

        Convexity calcConvexity(int ear);
        bool checkIfEarContainsOtherVertices(int ear);
    };

}

#endif
