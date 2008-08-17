
/**
   @author Shin'ichiro Nakaoka
*/

#ifndef OPENHRP_COLLISION_COLDET_MODEL_H_INCLUDED
#define OPENHRP_COLLISION_COLDET_MODEL_H_INCLUDED

#include "config.h"
#include <string>
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

        void setName(const char* name) { name_ = name; }
        const char* name() { return name_.c_str(); }

        void setNumVertices(int n);
        void setNumTriangles(int n);
        
        void setVertex(int index, float x, float y, float z);
        void setTriangle(int index, int v1, int v2, int v3);

        void build();

        bool isValid() { return isValid_; }

        void setPosition(const Matrix33& R, const Vector3& p);
        void setPosition(const double* R, const double* p);

      private:
        void initialize();
        
        ColdetModelSharedDataSet* dataSet;
        IceMaths::Matrix4x4* transform;
        std::string name_;
        bool isValid_;

        friend class ColdetModelPair;
    };

    typedef boost::shared_ptr<ColdetModel> ColdetModelPtr;
}


#endif
