
/**
   @author Shin'ichiro Nakaoka
*/

#ifndef OPENHRP_COLLISION_COLDET_MODEL_H_INCLUDED
#define OPENHRP_COLLISION_COLDET_MODEL_H_INCLUDED

#include "config.h"
#include <boost/shared_ptr.hpp>
#include <hrpUtil/Tvmet3d.h>


namespace IceMaths {
    class Matrix4x4;
}

namespace hrp {

    class ColdetModelSharedDataSet;

    class HRP_COLLISION_EXPORT ColdetModel
    {
      public:
        ColdetModel();
        ColdetModel(const ColdetModel& org);
        virtual ~ColdetModel();

        void setNumVertices(int n);
        void setNumTriangles(int n);
        
        void setVertex(int index, float x, float y, float z);
        void setTriangle(int index, int v1, int v2, int v3);

        void build();

        void setPosition(const Matrix33& R, const Vector3& p);
        void setPosition(const double* R, const double* p);

      private:
        ColdetModelSharedDataSet* dataSet;
        IceMaths::Matrix4x4* transform;

        friend class ColdetModelPairImpl;
    };

    typedef boost::shared_ptr<ColdetModel> ColdetModelPtr;
}


#endif
