/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

/*!
  @author Shin'ichiro Nakaoka
*/

#include "Triangulator.h"

using namespace std;
using namespace hrp;


int Triangulator::triangulate(const vector<int>& polygon)
{
    orgPolygon = &polygon;

    int numOrgVertices = polygon.size();
    
    workPolygon.resize(numOrgVertices);
    for(int i=0; i < numOrgVertices; ++i){
        workPolygon[i] = i;
    }
    
    ccs = 0.0;
    const Vector3Ref o(vertex(0));
    for(int i=1; i < numOrgVertices; ++i){
        ccs += tvmet::cross(Vector3(vertex(i) - o), Vector3(vertex((i+1) % numOrgVertices) - o));
    }

    int numTriangles = 0;

    while(true) {
        int n = workPolygon.size();
        if(n < 3){
            break;
        }
        int target = -1;
        for(int i=0; i < n; ++i){
            Convexity convexity = calcConvexity(i);
            if(convexity == FLAT){
                target = i;
                break;
            } else if(convexity == CONVEX){
                if(!checkIfEarContainsOtherVertices(i)){
                    triangles_.push_back((i - 1) % n);
                    triangles_.push_back(i);
                    triangles_.push_back((i + 1) % n);
                    target = i;
                    numTriangles++;
                    break;
                }
            }
        }
        if(target < 0){
            break;
        }
        for(int i = target + 1; i < n; ++i){
            workPolygon[target++] = workPolygon[i];
        }
        workPolygon.pop_back();
    }
    
    return numTriangles;
}


Triangulator::Convexity Triangulator::calcConvexity(int ear)
{
    int n = workPolygon.size();
    const Vector3Ref p0(workVertex((ear - 1) % n));
    Vector3 a(workVertex(ear) - p0);
    Vector3 b(workVertex((ear+1) % n) - p0);
    Vector3 ccs(cross(a, b));

    Convexity convexity;
    
    if((norm2(ccs) / (norm2(a) + norm2(b))) < 1.0e-4){
        convexity = FLAT;
    } else {
        convexity = (dot(this->ccs, ccs) > 0.0) ? CONVEX : CONCAVE;
    }

    return convexity;
}
    

bool Triangulator::checkIfEarContainsOtherVertices(int ear)
{
    bool contains = false;

    const int n = workPolygon.size();
    const Vector3Ref a(workVertex((ear-1) % n));
    const Vector3Ref b(workVertex(ear));
    const Vector3Ref c(workVertex((ear+1) % n));

    for(int i=0; i < workPolygon.size(); ++i){
        if(i < ear - 1 || i > ear + 1){
            const Vector3Ref p(workVertex(i));

            if(tvmet::dot(tvmet::cross(a - p, b - p), ccs) < 0){
                continue;
            }
            if(tvmet::dot(tvmet::cross(b - p, c - p), ccs) < 0){
                continue;
            }
            if(tvmet::dot(tvmet::cross(c - p, a - p), ccs) < 0){
                continue;
            }
            contains = true;
            break;
        }
    }

    return contains;
}
