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

#include "ColdetModelPair.h"
#include "ColdetModelSharedDataSet.h"
#include "Opcode/Opcode.h"

using namespace hrp;


ColdetModelPair::ColdetModelPair()
{

}


ColdetModelPair::ColdetModelPair(ColdetModelPtr model0, ColdetModelPtr model1)
{
    model0_ = model0;
    model1_ = model1;
}


ColdetModelPair::ColdetModelPair(const ColdetModelPair& org)
{
    model0_ = org.model0_;
    model1_ = org.model1_;
}


ColdetModelPair::~ColdetModelPair()
{

}


void ColdetModelPair::set(ColdetModelPtr model0, ColdetModelPtr model1)
{
    model0_ = model0;
    model1_ = model1;
}


collision_data* ColdetModelPair::detectCollisionsSub(bool detectAllContacts)
{
    collision_data* result = 0;
    
    if(cdContact){
        delete[] cdContact;
        cdContact = 0;
        cdContactsCount = 0;
    }

    if(model0_->isValid() && model1_->isValid()){

        Opcode::BVTCache colCache;

        // inverse order because of historical background
        // this should be fixed.(note that the direction of normal is inversed when the order inversed 
        colCache.Model0 = &model1_->dataSet->model;
        colCache.Model1 = &model0_->dataSet->model;
        
        Opcode::AABBTreeCollider collider;
        
        if(!detectAllContacts){
            collider.SetFirstContact(true);
        }
        
        bool isOK = collider.Collide(colCache, model1_->transform, model0_->transform);
        
        cdBoxTestsCount = collider.GetNbBVBVTests();
        cdTriTestsCount = collider.GetNbPrimPrimTests();
        
        if(isOK){
            result = cdContact;
        }
    }

    return result;
}
