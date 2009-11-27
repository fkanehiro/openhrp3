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

    class ColdetModelSharedDataSet
    {
    public:
        ColdetModelSharedDataSet();

        bool build();

        // need two instances ?
        Opcode::Model model;

	    Opcode::MeshInterface iMesh;

	    vector<IceMaths::Point> vertices;
	    vector<IceMaths::IndexedTriangle> triangles;

        ColdetModel::PrimitiveType pType;
        std::vector<float> pParams;

        int getAABBTreeDepth() {
            return AABBTreeMaxDepth;
        };
        int getNumofBB(int depth){
            return numBBMap.at(depth);
        };
        int getmaxNumofBB(){
            return numBBMap.at(AABBTreeMaxDepth-1);
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
