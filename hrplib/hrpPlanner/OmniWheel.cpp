// -*- mode: c++; indent-tabs-mode: nil; c-basic-offset: 4; tab-width: 4; -*-
#include "OmniWheel.h"
#include <math.h>
#ifndef M_PI
#define M_PI 3.14159
#endif

using namespace PathEngine;

inline double theta_limit(double theta)
{
    while (theta >= 2*M_PI) theta -= 2*M_PI;
    while (theta < 0) theta += 2*M_PI;
    return theta;
}

Configuration OmniWheel::interpolate(const Configuration& from, 
                                     const Configuration& to,
                                     double ratio) const
{
    Configuration cfg;
    for (unsigned int i=0; i<Configuration::size(); i++){
        if (Configuration::unboundedRotation(i)){
            double dth = to.value(i) - from.value(i);
            if (fabs(dth) > M_PI){
                dth = dth > 0 ? -(2*M_PI-dth) : 2*M_PI+dth;
            }
            cfg.value(i) = theta_limit(from.value(2) + ratio*dth);
            
        }else{
            cfg.value(i) = (1-ratio)*from.value(i) + ratio*to.value(i);
        }
    }
    
    return cfg;
}

double OmniWheel::distance(const Configuration& from, const Configuration& to) const
{
    double v=0, d;
    for (unsigned int i=0; i<Configuration::size(); i++){
        if (Configuration::unboundedRotation(i)){
            double dth = fabs(to.value(i) - from.value(i));
            if (dth > M_PI) dth = 2*M_PI - dth;
            d *= Configuration::weight(i);
        }else{
            d = Configuration::weight(i)*(to.value(i) - from.value(i));
        }
        v += d*d;
    }
    return sqrt(v);
}



