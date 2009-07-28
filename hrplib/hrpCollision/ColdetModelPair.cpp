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
#include "CollisionPairInserter.h"
#include "Opcode/Opcode.h"
#include "SSVTreeCollider.h"

using namespace hrp;


ColdetModelPair::ColdetModelPair()
{

}


ColdetModelPair::ColdetModelPair(ColdetModelPtr model0, ColdetModelPtr model1,
                                 double tolerance)
{
    model0_ = model0;
    model1_ = model1;
    tolerance_ = tolerance;
}


ColdetModelPair::ColdetModelPair(const ColdetModelPair& org)
{
    model0_ = org.model0_;
    model1_ = org.model1_;
    tolerance_ = org.tolerance_;
}


ColdetModelPair::~ColdetModelPair()
{

}


void ColdetModelPair::set(ColdetModelPtr model0, ColdetModelPtr model1)
{
    model0_ = model0;
    model1_ = model1;
}


std::vector<collision_data>& ColdetModelPair::detectCollisionsSub(bool detectAllContacts)
{
    collisionPairInserter.clear();

    bool detected;
    
    if ((model0_->getPrimitiveType() == ColdetModel::SP_PLANE &&
         model1_->getPrimitiveType() == ColdetModel::SP_CYLINDER)
        || (model1_->getPrimitiveType() == ColdetModel::SP_PLANE &&
            model0_->getPrimitiveType() == ColdetModel::SP_CYLINDER)){
        detected = detectPlaneCylinderCollisions(detectAllContacts);
    }else{
        detected = detectMeshMeshCollisions(detectAllContacts);
    }

    if(!detected){
        collisionPairInserter.clear();
    }

    return collisionPairInserter.collisions();
}


bool ColdetModelPair::detectMeshMeshCollisions(bool detectAllContacts)
{
    bool result = false;
    
    if(model0_->isValid() && model1_->isValid()){

        Opcode::BVTCache colCache;

        // inverse order because of historical background
        // this should be fixed.(note that the direction of normal is inversed when the order inversed 
        colCache.Model0 = &model1_->dataSet->model;
        colCache.Model1 = &model0_->dataSet->model;

        Opcode::AABBTreeCollider collider;
        collider.setCollisionPairInserter(&collisionPairInserter);
        
        if(!detectAllContacts){
            collider.SetFirstContact(true);
        }
        
        result = collider.Collide(colCache, model1_->transform, model0_->transform);
        
        boxTestsCount = collider.GetNbBVBVTests();
        triTestsCount = collider.GetNbPrimPrimTests();
    }

    return result;
}


bool ColdetModelPair::detectPlaneCylinderCollisions(bool detectAllContacts)
{
    ColdetModelPtr plane, cylinder;
    bool reversed=false;
    if (model0_->getPrimitiveType() == ColdetModel::SP_PLANE){
        plane = model0_;
    }else if(model0_->getPrimitiveType() == ColdetModel::SP_CYLINDER){
        cylinder = model0_;
    }
    if (model1_->getPrimitiveType() == ColdetModel::SP_PLANE){
        plane = model1_;
        reversed = true;
    }else if(model1_->getPrimitiveType() == ColdetModel::SP_CYLINDER){
        cylinder = model1_;
    }
    if (!plane || !cylinder) return false;

    IceMaths::Matrix4x4 pTrans = (*(plane->pTransform)) * (*(plane->transform));
    IceMaths::Matrix4x4 cTrans = (*(cylinder->pTransform)) * (*(cylinder->transform));

    float radius, height; // height and radius of cylinder
    cylinder->getPrimitiveParam(0, radius);
    cylinder->getPrimitiveParam(1, height);

    IceMaths::Point pTopLocal(0, height/2, 0), pBottomLocal(0, -height/2, 0);
    IceMaths::Point pTop, pBottom; // center points of top and bottom discs
    IceMaths::TransformPoint4x3(pTop,    pTopLocal,    cTrans);
    IceMaths::TransformPoint4x3(pBottom, pBottomLocal, cTrans);
    
    IceMaths::Point pOnPlane, nLocal(0,0,1), n;
    IceMaths::TransformPoint3x3(n, nLocal, pTrans);
    pTrans.GetTrans(pOnPlane);
    float d = pOnPlane|n; // distance between origin and plane

    float dTop    = (pTop|n) - d;
    float dBottom = (pBottom|n) - d;

    if (dTop > radius && dBottom > radius) return false;

    double theta = asin((dTop - dBottom)/height);
    double rcosth = radius*cos(theta);

    int contactsCount = 0;
    if (rcosth >= dTop) contactsCount+=2;
    if (rcosth >= dBottom) contactsCount+=2;

    if (contactsCount){
        std::vector<collision_data>& cdata = collisionPairInserter.collisions();
        cdata.resize(contactsCount);
        for (unsigned int i=0; i<contactsCount; i++){
            cdata[i].num_of_i_points = 1;
            cdata[i].i_point_new[0]=1; 
            cdata[i].i_point_new[1]=0; 
            cdata[i].i_point_new[2]=0; 
            cdata[i].i_point_new[3]=0; 
            if (reversed){
                cdata[i].n_vector[0] = -n.x;
                cdata[i].n_vector[1] = -n.y;
                cdata[i].n_vector[2] = -n.z;
            }else{
                cdata[i].n_vector[0] = n.x;
                cdata[i].n_vector[1] = n.y;
                cdata[i].n_vector[2] = n.z;
            }
        }
        IceMaths::Point vBottomTop = pTop - pBottom;
        IceMaths::Point v = vBottomTop^n;
        v.Normalize();
        IceMaths::Point w = v^n;
        w.Normalize();

        unsigned int index=0;
        if (rcosth >= dBottom){ // bottom disc collides
            double depth = rcosth - dBottom;
            IceMaths::Point iPoint = pBottom - dBottom*n - dBottom*tan(theta)*w;
            double x = dBottom/cos(theta);
            IceMaths::Point dv = sqrt(radius*radius - x*x)*v;
            cdata[index].i_points[0][0] = iPoint.x + dv.x;
            cdata[index].i_points[0][1] = iPoint.y + dv.y;
            cdata[index].i_points[0][2] = iPoint.z + dv.z;
            cdata[index].depth = depth;
            index++;
            cdata[index].i_points[0][0] = iPoint.x - dv.x;
            cdata[index].i_points[0][1] = iPoint.y - dv.y;
            cdata[index].i_points[0][2] = iPoint.z - dv.z;
            cdata[index].depth = depth;
            index++;
        }
        if (rcosth >= dTop){ // top disc collides
            double depth = rcosth - dTop;
            IceMaths::Point iPoint = pTop - dTop*n - dTop*tan(theta)*w;
            double x = dTop/cos(theta);
            IceMaths::Point dv = sqrt(radius*radius - x*x)*v;
            cdata[index].i_points[0][0] = iPoint.x + dv.x;
            cdata[index].i_points[0][1] = iPoint.y + dv.y;
            cdata[index].i_points[0][2] = iPoint.z + dv.z;
            cdata[index].depth = depth;
            index++;
            cdata[index].i_points[0][0] = iPoint.x - dv.x;
            cdata[index].i_points[0][1] = iPoint.y - dv.y;
            cdata[index].i_points[0][2] = iPoint.z - dv.z;
            cdata[index].depth = depth;
            index++;
        }

        return true;
    }
    return false;
}


double ColdetModelPair::computeDistance(double *point0, double *point1)
{
    if(model0_->isValid() && model1_->isValid()){

        Opcode::BVTCache colCache;

        colCache.Model0 = &model1_->dataSet->model;
        colCache.Model1 = &model0_->dataSet->model;
        
        SSVTreeCollider collider;
        
        float d;
        Point p0, p1;
        collider.Distance(colCache, d, p0, p1,
                          model1_->transform, model0_->transform);
        point0[0] = p0.x;
        point0[1] = p0.y;
        point0[2] = p0.z;
        point1[0] = p1.x;
        point1[1] = p1.y;
        point1[2] = p1.z;
        return d;
    }

    return -1;
}

bool ColdetModelPair::detectIntersection()
{
    if(model0_->isValid() && model1_->isValid()){

        Opcode::BVTCache colCache;

        colCache.Model0 = &model1_->dataSet->model;
        colCache.Model1 = &model0_->dataSet->model;
        
        SSVTreeCollider collider;
        
        return collider.Collide(colCache, tolerance_, 
                                model1_->transform, model0_->transform);
    }

    return false;
}
