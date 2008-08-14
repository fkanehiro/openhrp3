
/**
   @author Shin'ichiro Nakaoka
*/

#ifndef OPENHRP_COLLISION_COLDET_MODEL_H_INCLUDED
#define OPENHRP_COLLISION_COLDET_MODEL_H_INCLUDED

#include "config.h"

#include <boost/shared_ptr.hpp>

namespace hrp {

    class ColdetModelImpl;

    class HRP_COLLISION_EXPORT ColdetModel
    {
      public:
        ColdetModel();
        ~ColdetModel();

        void setNumVertices(int n);
        void setNumTriangles(int n);
        
        void setVertex(int index, float x, float y, float z);
        void setTriangle(int index, int v1, int v2, int v3);

        void update();

      private:
        ColdetModelImpl* impl;
    };

    typedef boost::shared_ptr<ColdetModel> ColdetModelPtr;
}


#endif
