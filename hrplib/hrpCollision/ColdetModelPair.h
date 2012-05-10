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
   @author Rafael Cisneros
*/

#ifndef HRPCOLLISION_COLDET_MODEL_PAIR_H_INCLUDED
#define HRPCOLLISION_COLDET_MODEL_PAIR_H_INCLUDED

#define LOCAL_EPSILON 0.0001f

#include "config.h"
#include "CollisionData.h"
#include "ColdetModel.h"
#include "CollisionPairInserterBase.h"
#include <vector>
#include <hrpUtil/Referenced.h>

namespace hrp {

    class HRP_COLLISION_EXPORT ColdetModelPair : public Referenced
    {
      public:
        ColdetModelPair();
        ColdetModelPair(ColdetModelPtr model0, ColdetModelPtr model1,
                        double tolerance=0);
        ColdetModelPair(const ColdetModelPair& org);
        virtual ~ColdetModelPair();

        void set(ColdetModelPtr model0, ColdetModelPtr model1);

        ColdetModel* model(int index) { return models[index].get(); }
        IceMaths::Matrix4x4* transform(int index) { return models[index]->transform; }

        std::vector<collision_data>& detectCollisions() {
            return detectCollisionsSub(true);
        }

        std::vector<collision_data>& collisions() {
            return collisionPairInserter->cdContact;
        }

        void clearCollisions(){
            collisionPairInserter->cdContact.clear();
        }

        bool checkCollision() {
            return !detectCollisionsSub(false).empty();
        }

        double computeDistance(double *point0, double *point1);

        /**
           @param out_triangle0, out_triangle1 Indices of the triangle pair that are originally registered by ColdeModel::setTraiangle().
           @param out_point0, out_point1 The closest points 
        */
        double computeDistance(int& out_triangle0, double* out_point0, int& out_triangle1, double* out_point1);

        bool detectIntersection();

        double tolerance() const { return tolerance_; }

        void setCollisionPairInserter(CollisionPairInserterBase *inserter); 

	int calculateCentroidIntersection(float &cx, float &cy, float &A, float radius, std::vector<float> vx, std::vector<float> vy);
		
	int makeCCW(std::vector<float> &vx, std::vector<float> &vy);
		
	float calculatePolygonArea(const std::vector<float> &vx, const std::vector<float> &vy);
	void calculateSectorCentroid(float &cx, float &cy, float radius, float th1, float th2);

	inline bool isInsideCircle(float r, float x, float y) {
		return sqrt(pow(x, 2) + pow(y, 2)) <= r;
	}
	bool isInsideTriangle(float x, float y, const std::vector<float> &vx, const std::vector<float> &vy);

	int calculateIntersection(std::vector<float> &x, std::vector<float> &y, float radius, float x1, float y1, float x2, float y2);

      private:
        std::vector<collision_data>& detectCollisionsSub(bool detectAllContacts);
        bool detectMeshMeshCollisions(bool detectAllContacts);
		bool detectSphereSphereCollisions(bool detectAllContacts);
		bool detectSphereMeshCollisions(bool detectAllContacts);
        bool detectPlaneCylinderCollisions(bool detectAllContacts);

        ColdetModelPtr models[2];
        double tolerance_;

        CollisionPairInserterBase *collisionPairInserter;

        int boxTestsCount;
        int triTestsCount;
	
	enum pointType {vertex, inter};
	enum figType {tri, sector};

	struct pointStruct {
		float x, y, angle;
		pointType type;
		int code;
	};
	
	struct figStruct {
		figType type;
		int p1, p2;
		float area;
		float cx, cy;
	};
    };

    typedef boost::intrusive_ptr<ColdetModelPair> ColdetModelPairPtr;
}


#endif
