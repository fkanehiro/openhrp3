
/**
   @author Shin'ichiro Nakaoka
*/

#ifndef HRP_COLLISION_COLDET_MODEL_PAIR_H_INCLUDED
#define HRP_COLLISION_COLDET_MODEL_PAIR_H_INCLUDED

#include "config.h"
#include "CollisionData.h"
#include "ColdetModel.h"

namespace hrp {

    class ColdetModelPairImpl;

    class HRP_COLLISION_EXPORT ColdetModelPair
    {
      public:
        ColdetModelPair();
        ColdetModelPair(ColdetModelPtr model1, ColdetModelPtr model2);
        ColdetModelPair(const ColdetModelPair& org);
        virtual ~ColdetModelPair();

        void set(ColdetModelPtr model1, ColdetModelPtr model2);
        
        collision_data* detectCollisions();

        bool checkCollision();

      private:
        ColdetModelPairImpl* impl;
    };
}


#endif
