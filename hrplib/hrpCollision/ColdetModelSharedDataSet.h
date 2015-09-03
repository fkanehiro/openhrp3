/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */
/**
   @author Shin'ichiro Nakaoka
*/

#ifndef OPENHRP_COLDET_MODEL_SHARED_DATA_SET_H_INCLUDED
#define OPENHRP_COLDET_MODEL_SHARED_DATA_SET_H_INCLUDED


#include "ColdetModel.h"
#include "Opcode/Opcode.h"
#include <vector>

using namespace std;
using namespace hrp;

namespace hrp {
     struct triangle3 {
        int triangles[3];
     };

    class ColdetModelSharedDataSet
    {
    public:
         struct NeighborTriangleSet {
             int triangles[3];
             NeighborTriangleSet(){
                 triangles[0] = triangles[1] = triangles[2] = -1;
             }
             void addNeighbor(int neighbor){
                 for(int i=0; i < 3; ++i){
                     if(triangles[i] < 0){
                         triangles[i] = neighbor;
                         break;
                     }
                 }
             }
             void deleteNeighbor(int neighbor){
                 for(int i=0; i<3; i++){
                     if(triangles[i]==neighbor){
                         for(int j=i+1; j<3; j++){
                             triangles[j-1] = triangles[j];
                         }
                         triangles[2] = -1;
                     }
                     break;
                 }
             }
             int operator[](int index) const { return triangles[index]; }
         };

         typedef std::vector<NeighborTriangleSet> NeighborTriangleSetArray;

        ColdetModelSharedDataSet();

        bool build();

        // need two instances ?
        Opcode::Model model;

	    Opcode::MeshInterface iMesh;

	    vector<IceMaths::Point> vertices;
	    vector<IceMaths::IndexedTriangle> triangles;

        ColdetModel::PrimitiveType pType;
        std::vector<float> pParams;

        NeighborTriangleSetArray neighbor;

        int getAABBTreeDepth() {
            return AABBTreeMaxDepth;
        };
        int getNumofBB(int depth){
            return numBBMap.at(depth);
        };
        int getmaxNumofBB(){
            if(AABBTreeMaxDepth>0)
                return numBBMap.at(AABBTreeMaxDepth-1);
            else
                return 0;
        };

      private:
        int refCounter;
        int AABBTreeMaxDepth;
        std::vector<int> numBBMap;
        std::vector<int> numLeafMap;
        int computeDepth(const Opcode::AABBCollisionNode* node, int currentDepth, int max );

        friend class ColdetModel;
    };
}

#endif
