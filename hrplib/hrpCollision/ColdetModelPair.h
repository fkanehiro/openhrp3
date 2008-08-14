
/**
   @author Shin'ichiro Nakaoka
*/

#ifndef OPENHRP_COLLISION_COLDET_PAIR_SET_H_INCLUDED
#define OPENHRP_COLLISION_COLDET_PAIR_SET_H_INCLUDED

#include "config.h"

namespace hrp {

    class ColdetPairSetImpl;

    class HRP_COLLISION_EXPORT ColdetPairSet
    {
      public:
        ColdetPairSet();
        ~ColdetPairSet();

        int numPairs();

        void addPair(ColdetModelPtr model1, ColdetModelPtr model2);

      private:
        ColdetPairSetImpl* impl;
    };
}


#endif
