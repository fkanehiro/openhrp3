// -*- coding: utf-8 -*-
// Copyright (C) 2011 University of Tokyo, General Robotix Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//     http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
/*!
  @file ColladaUtil.h
  @brief 
  @author Rosen Diankov (rosen.diankov@gmail.com)

  Utilities for the COLLADA reader and writers. Used OpenRAVE files for reference.
*/
#ifndef OPENHRP_COLLADA_UTIL_H
#define OPENHRP_COLLADA_UTIL_H

#include <dae.h>
#include <dae/daeErrorHandler.h>
#include <dom/domCOLLADA.h>
#include <dae/domAny.h>
#include <dom/domConstants.h>
#include <dom/domTriangles.h>
#include <dae/daeDocument.h>
#include <dom/domTypes.h>
#include <dom/domElements.h>
#include <dom/domKinematics.h>
#include <dae/daeStandardURIResolver.h>
#include <locale>
#include <string>
#include <vector>
#include <list>
#include <map>
#include <list>
#include <vector>
#include <sstream>

#if (BOOST_VERSION > 104400)
#define BOOST_ENABLE_ASSERT_HANDLER
#endif
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/date_time/time_facet.hpp>
#include <boost/algorithm/string.hpp>
#include <boost/format.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/array.hpp>
#include <boost/lexical_cast.hpp>

#include <hrpCorba/ORBwrap.h>
#include <hrpCorba/ModelLoader.hh>

#ifdef _MSC_VER
#ifndef __PRETTY_FUNCTION__
#define __PRETTY_FUNCTION__ __FUNCDNAME__
#endif
#endif

#ifndef __PRETTY_FUNCTION__
#define __PRETTY_FUNCTION__ __func__
#endif

#define COLLADA_ASSERT(b) { if( !(b) ) { std::stringstream ss; ss << "ikfast exception: " << __FILE__ << ":" << __LINE__ << ": " <<__PRETTY_FUNCTION__ << ": Assertion '" << #b << "' failed"; throw OpenHRP::ModelLoader::ModelLoaderException(ss.str().c_str()); } }

namespace ColladaUtil
{
typedef double dReal;

// logging functions
inline void COLLADALOG_VERBOSE(const std::string& s) {
    //std::cout << "Collada Verbose: " << s << std::endl;
}
inline void COLLADALOG_DEBUG(const std::string& s) {
    //std::cout << "Collada Debug: " << s << std::endl;
}
inline void COLLADALOG_INFO(const std::string& s) {
    std::cout << "Collada Info: " << s << std::endl;
}
inline void COLLADALOG_WARN(const std::string& s) {
    std::cout << "Collada Warning: " << s << std::endl;
}
inline void COLLADALOG_ERROR(const std::string& s) {
    std::cout << "Collada Error: " << s << std::endl;
}

//
// openrave math functions (from geometry.h)
//

inline void PoseMultVector(OpenHRP::DblArray3& vnew, const OpenHRP::DblArray12& m, const OpenHRP::DblArray3& v)
{
    dReal x = v[0], y = v[1], z = v[2];
    vnew[0] = m[4*0+0] * x + m[4*0+1] * y + m[4*0+2] * z + m[4*0+3];
    vnew[1] = m[4*1+0] * x + m[4*1+1] * y + m[4*1+2] * z + m[4*1+3];
    vnew[2] = m[4*2+0] * x + m[4*2+1] * y + m[4*2+2] * z + m[4*2+3];
}

inline void PoseRotateVector(OpenHRP::DblArray3& vnew, const OpenHRP::DblArray12& m, const OpenHRP::DblArray3& v)
{
    dReal x = v[0], y = v[1], z = v[2];
    vnew[0] = m[4*0+0] * x + m[4*0+1] * y + m[4*0+2] * z;
    vnew[1] = m[4*1+0] * x + m[4*1+1] * y + m[4*1+2] * z;
    vnew[2] = m[4*2+0] * x + m[4*2+1] * y + m[4*2+2] * z;
}

inline void PoseIdentity(OpenHRP::DblArray12& pose)
{
    pose[0] = 1; pose[1] = 0; pose[2] = 0; pose[3] = 0;
    pose[4] = 0; pose[5] = 1; pose[6] = 0; pose[7] = 0;
    pose[8] = 0; pose[9] = 0; pose[10] = 1; pose[11] = 0;
}

inline void PoseInverse(OpenHRP::DblArray12& poseinv, const OpenHRP::DblArray12& pose)
{
    COLLADA_ASSERT((void*)&poseinv != (void*)&pose);
    poseinv[0] = pose[0]; poseinv[1] = pose[4]; poseinv[2] = pose[8]; poseinv[3] = -pose[3]*pose[0]-pose[7]*pose[4]-pose[11]*pose[8];
    poseinv[4] = pose[1]; poseinv[5] = pose[5]; poseinv[6] = pose[9]; poseinv[7] = -pose[3]*pose[1]-pose[7]*pose[5]-pose[11]*pose[9];
    poseinv[8] = pose[2]; poseinv[9] = pose[6]; poseinv[10] = pose[10]; poseinv[11] = -pose[3]*pose[2]-pose[7]*pose[6]-pose[11]*pose[10];
}

template <typename T>
inline void PoseMult(OpenHRP::DblArray12& mres, const T& m0, const OpenHRP::DblArray12& m1)
{
    COLLADA_ASSERT((void*)&mres != (void*)&m0 && (void*)&mres != (void*)&m1);
    mres[0*4+0] = m0[0*4+0]*m1[0*4+0]+m0[0*4+1]*m1[1*4+0]+m0[0*4+2]*m1[2*4+0];
    mres[0*4+1] = m0[0*4+0]*m1[0*4+1]+m0[0*4+1]*m1[1*4+1]+m0[0*4+2]*m1[2*4+1];
    mres[0*4+2] = m0[0*4+0]*m1[0*4+2]+m0[0*4+1]*m1[1*4+2]+m0[0*4+2]*m1[2*4+2];
    mres[1*4+0] = m0[1*4+0]*m1[0*4+0]+m0[1*4+1]*m1[1*4+0]+m0[1*4+2]*m1[2*4+0];
    mres[1*4+1] = m0[1*4+0]*m1[0*4+1]+m0[1*4+1]*m1[1*4+1]+m0[1*4+2]*m1[2*4+1];
    mres[1*4+2] = m0[1*4+0]*m1[0*4+2]+m0[1*4+1]*m1[1*4+2]+m0[1*4+2]*m1[2*4+2];
    mres[2*4+0] = m0[2*4+0]*m1[0*4+0]+m0[2*4+1]*m1[1*4+0]+m0[2*4+2]*m1[2*4+0];
    mres[2*4+1] = m0[2*4+0]*m1[0*4+1]+m0[2*4+1]*m1[1*4+1]+m0[2*4+2]*m1[2*4+1];
    mres[2*4+2] = m0[2*4+0]*m1[0*4+2]+m0[2*4+1]*m1[1*4+2]+m0[2*4+2]*m1[2*4+2];
    mres[3] = m1[3] * m0[0] + m1[7] * m0[1] + m1[11] * m0[2] + m0[3];
    mres[7] = m1[3] * m0[4] + m1[7] * m0[5] + m1[11] * m0[6] + m0[7];
    mres[11] = m1[3] * m0[8] + m1[7] * m0[9] + m1[11] * m0[10] + m0[11];
}

template <typename T>
inline void QuatFromMatrix(OpenHRP::DblArray4& quat, const T& mat)
{
    dReal tr = mat[4*0+0] + mat[4*1+1] + mat[4*2+2];
    if (tr >= 0) {
        quat[0] = tr + 1;
        quat[1] = (mat[4*2+1] - mat[4*1+2]);
        quat[2] = (mat[4*0+2] - mat[4*2+0]);
        quat[3] = (mat[4*1+0] - mat[4*0+1]);
    }
    else {
        // find the largest diagonal element and jump to the appropriate case
        if (mat[4*1+1] > mat[4*0+0]) {
            if (mat[4*2+2] > mat[4*1+1]) {
                quat[3] = (mat[4*2+2] - (mat[4*0+0] + mat[4*1+1])) + 1;
                quat[1] = (mat[4*2+0] + mat[4*0+2]);
                quat[2] = (mat[4*1+2] + mat[4*2+1]);
                quat[0] = (mat[4*1+0] - mat[4*0+1]);
            }
            else {
                quat[2] = (mat[4*1+1] - (mat[4*2+2] + mat[4*0+0])) + 1;
                quat[3] = (mat[4*1+2] + mat[4*2+1]);
                quat[1] = (mat[4*0+1] + mat[4*1+0]);
                quat[0] = (mat[4*0+2] - mat[4*2+0]);
            }
        }
        else if (mat[4*2+2] > mat[4*0+0]) {
            quat[3] = (mat[4*2+2] - (mat[4*0+0] + mat[4*1+1])) + 1;
            quat[1] = (mat[4*2+0] + mat[4*0+2]);
            quat[2] = (mat[4*1+2] + mat[4*2+1]);
            quat[0] = (mat[4*1+0] - mat[4*0+1]);
        }
        else {
            quat[1] = (mat[4*0+0] - (mat[4*1+1] + mat[4*2+2])) + 1;
            quat[2] = (mat[4*0+1] + mat[4*1+0]);
            quat[3] = (mat[4*2+0] + mat[4*0+2]);
            quat[0] = (mat[4*2+1] - mat[4*1+2]);
        }
    }
    dReal fnorm = std::sqrt(quat[0]*quat[0]+quat[1]*quat[1]+quat[2]*quat[2]+quat[3]*quat[3]);
    // don't touch the divides
    quat[0] /= fnorm;
    quat[1] /= fnorm;
    quat[2] /= fnorm;
    quat[3] /= fnorm;
}

inline void AxisAngleFromQuat(OpenHRP::DblArray4& rotation, const OpenHRP::DblArray4& quat)
{
    dReal sinang = quat[1]*quat[1]+quat[2]*quat[2]+quat[3]*quat[3];
    if( sinang == 0 ) {
        rotation[0] = 1;
        rotation[1] = 0;
        rotation[2] = 0;
        rotation[3] = 0;
    }
    else {
        dReal _quat[4];
        if( quat[0] < 0 ) {
            for(int i = 0; i < 4; ++i) {
                _quat[i] = -quat[i];
            }
        }
        else {
            for(int i = 0; i < 4; ++i) {
                _quat[i] = quat[i];
            }
        }
        sinang = sqrt(sinang);
        rotation[0] = _quat[1]/sinang;
        rotation[1] = _quat[2]/sinang;
        rotation[2] = _quat[3]/sinang;
        rotation[3] = 2.0*atan2(sinang,_quat[0]);
    }
}

template <typename T>
inline void QuatFromAxisAngle(OpenHRP::DblArray4& quat, const T& rotation, dReal fanglemult=1)
{
    dReal axislen = sqrt(rotation[0]*rotation[0]+rotation[1]*rotation[1]+rotation[2]*rotation[2]);
    if( axislen == 0 ) {
        quat[0] = 1;
        quat[1] = 0;
        quat[2] = 0;
        quat[3] = 0;
    }
    else {
        dReal angle = rotation[3] * 0.5*fanglemult;
        dReal sang = sin(angle)/axislen;
        quat[0] = cos(angle);
        quat[1] = rotation[0]*sang;
        quat[2] = rotation[1]*sang;
        quat[3] = rotation[2]*sang;
    }
}

inline void PoseFromQuat(OpenHRP::DblArray12& pose, const OpenHRP::DblArray4& quat)
{
    dReal qq1 = 2*quat[1]*quat[1];
    dReal qq2 = 2*quat[2]*quat[2];
    dReal qq3 = 2*quat[3]*quat[3];
    pose[4*0+0] = 1 - qq2 - qq3;
    pose[4*0+1] = 2*(quat[1]*quat[2] - quat[0]*quat[3]);
    pose[4*0+2] = 2*(quat[1]*quat[3] + quat[0]*quat[2]);
    pose[4*0+3] = 0;
    pose[4*1+0] = 2*(quat[1]*quat[2] + quat[0]*quat[3]);
    pose[4*1+1] = 1 - qq1 - qq3;
    pose[4*1+2] = 2*(quat[2]*quat[3] - quat[0]*quat[1]);
    pose[4*1+3] = 0;
    pose[4*2+0] = 2*(quat[1]*quat[3] - quat[0]*quat[2]);
    pose[4*2+1] = 2*(quat[2]*quat[3] + quat[0]*quat[1]);
    pose[4*2+2] = 1 - qq1 - qq2;
    pose[4*2+3] = 0;
}

inline void PoseFromAxisAngleTranslation(OpenHRP::DblArray12& pose, const OpenHRP::DblArray4& rotation, const OpenHRP::DblArray3& translation)
{
    OpenHRP::DblArray4 quat;
    QuatFromAxisAngle(quat,rotation);
    PoseFromQuat(pose,quat);
    pose[3] = translation[0];
    pose[7] = translation[1];
    pose[11] = translation[2];
}

inline void AxisAngleTranslationFromPose(OpenHRP::DblArray4& rotation, OpenHRP::DblArray3& translation, const OpenHRP::DblArray12& pose)
{
    OpenHRP::DblArray4 quat;
    QuatFromMatrix(quat,pose);
    AxisAngleFromQuat(rotation,quat);
    translation[0] = pose[3];
    translation[1] = pose[7];
    translation[2] = pose[11];
}

inline std::vector<dReal> toVector(const OpenHRP::DblSequence& seq)
{
    std::vector<dReal> v;
    v.resize(seq.length());
    for(size_t i = 0; i < v.size(); ++i) {
        v[i] = seq[i];
    }
    return v;
}

// returns a lower case version of the string
inline std::string tolowerstring(const std::string & s)
{
    std::string d = s;
    std::transform(d.begin(), d.end(), d.begin(), ::tolower);
    return d;
}

}

/// Modifications controlling boost library behavior.
namespace boost
{
    inline void assertion_failed(char const * expr, char const * function, char const * file, long line) {
        throw OpenHRP::ModelLoader::ModelLoaderException(boost::str(boost::format("[%s:%d] -> %s, expr: %s")%file%line%function%expr).c_str());
    }
}

#endif
