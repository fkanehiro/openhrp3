// -*- mode: c++; indent-tabs-mode: nil; c-basic-offset: 4; tab-width: 4; -*-
#include "TGT.h"
#define _USE_MATH_DEFINES // for MSVC
#include <math.h>

using namespace PathEngine;

inline double theta_diff(double from, double to)
{
    double diff = to - from;
    if (diff > M_PI){
        return diff - 2*M_PI;
    }else if (diff < -M_PI){
        return diff + 2*M_PI;
    }else{
        return diff;
    }
}

#if 0
std::vector<Configuration> TGT::getPath(const Configuration &from, const Configuration &to) const
{
    std::vector<Configuration> path;

    double dx = to.value(0) - from.value(0);
    double dy = to.value(1) - from.value(1);
    double theta = atan2(dy, dx);

    Configuration changedFrom(from.value(0), from.value(1), theta);
    Configuration changedTo(to.value(0), to.value(1), theta);

    path.push_back(from);

    unsigned int n;
    Configuration pos;

    std::cout << 1 << std::endl;
    // from - changedFrom
    n = (unsigned int)(distance(from, changedFrom)/interpolationDistance())+1;
    for (unsigned int i=1; i<=n; i++){
        pos = interpolate(from, changedFrom, ((double)i)/n);
        std::cout << pos << std::endl;
        path.push_back(pos);
    }

    std::cout << 2 << std::endl;
    // changedFrom - changedTo
    n = (unsigned int)(distance(changedFrom, changedTo)/interpolationDistance())+1;
    for (unsigned int i=1; i<=n; i++){
        pos = interpolate(changedFrom, changedTo, ((double)i)/n);
        std::cout << pos << std::endl;
        path.push_back(pos);
    }

    std::cout << 3 << std::endl;
    // changedTo - to
    n = (unsigned int)(distance(changedTo, to)/interpolationDistance())+1;
    for (unsigned int i=1; i<=n; i++){
        pos = interpolate(changedTo, to, ((double)i)/n);
        std::cout << pos << std::endl;
        path.push_back(pos);
    }
    getchar();

    return path;
}
#endif

Configuration TGT::interpolate(const Configuration& from, const Configuration& to,
                          double ratio) const
{
    double dx = to.value(0) - from.value(0);
    double dy = to.value(1) - from.value(1);

    if (dx == 0 && dy == 0){
        double dth = theta_diff(from.value(2), to.value(2));
        Configuration cfg;
        cfg.value(0) = from.value(0);
        cfg.value(1) = from.value(1);
        cfg.value(2) = from.value(2)+ratio*dth;
        return cfg;
    }else{
        
        double theta = atan2(dy, dx);
        
        double dth1 = theta_diff(from.value(2), theta);
        double d1 = Configuration::weight(2)*fabs(dth1);
        
        dx *= Configuration::weight(0);
        dy *= Configuration::weight(1);
        double d2 = sqrt(dx*dx + dy*dy);
        
        double dth2 = theta_diff(theta, to.value(2));
        double d3 = Configuration::weight(2) * fabs(dth2);
        
        double d = d1 + d2 + d3;

#if 0        
        std::cout << "theta = " << theta << ", dth1 = " << dth1 
                  << ", dth2 = " << dth2 << std::endl;
        std::cout << "d1:" << d1 << ", d2:" << d2 << ", d3:" << d3 << std::endl;  
#endif

        if (d == 0){
            return from;
        }
        
        if (ratio >= 0 && ratio*d < d1){
            Configuration cfg;
            cfg.value(0) = from.value(0);
            cfg.value(1) = from.value(1);
            cfg.value(2) = from.value(2) + ratio*d/d1*dth1;
            return cfg;
        }else if (ratio*d >= d1 && ratio*d < (d1+d2)){
            double r = (ratio*d - d1)/d2;
            Configuration cfg;
            cfg.value(0) = (1-r)*from.value(0) + r*to.value(0);
            cfg.value(1) = (1-r)*from.value(1) + r*to.value(1);
            cfg.value(2) = theta;
            return cfg;
        }else if (ratio*d >= (d1+d2) && ratio <= 1.0){
            Configuration cfg;
            cfg.value(0) = to.value(0);
            cfg.value(1) = to.value(1);
            cfg.value(2) = theta + (ratio*d-d1-d2)/d3*dth2;
            return cfg;
        }else{
            std::cout << "TGT::interpolate() : invalid ratio(" << ratio << ")"
                      << std::endl;
        }
    }
}

double TGT::distance(const Configuration& from, const Configuration& to) const
{
    double dx = to.value(0) - from.value(0);
    double dy = to.value(1) - from.value(1);
    double theta = atan2(dy, dx);

    dx *= Configuration::weight(0);
    dy *= Configuration::weight(1);

    if (dx == 0 && dy == 0) {
        return  Configuration::weight(2)*fabs(theta_diff(from.value(2), to.value(2))); 
    }


    double dth1 = fabs(theta_diff(from.value(2), theta));
    dth1 *= Configuration::weight(2);
    
    double dth2 = fabs(theta_diff(theta, to.value(2)));
    dth2 *= Configuration::weight(2);

    //std::cout << "d = " << sqrt(dx*dx + dy*dy) << " +  " << dth1 << " + " <<  dth2 << std::endl;
    return sqrt(dx*dx + dy*dy) + dth1 + dth2;
}
