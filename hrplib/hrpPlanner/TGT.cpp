// -*- mode: c++; indent-tabs-mode: nil; c-basic-offset: 4; tab-width: 4; -*-
#include "TGT.h"

using namespace PathEngine;
#if 0
std::vector<Position> TGT::getPath(const Position &from, const Position &to) const
{
    std::vector<Position> path;

    double dx = to.getX() - from.getX();
    double dy = to.getY() - from.getY();
    double theta = atan2(dy, dx);

    Position changedFrom(from.getX(), from.getY(), theta);
    Position changedTo(to.getX(), to.getY(), theta);

    path.push_back(from);

    unsigned int n;
    Position pos;

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

Position TGT::interpolate(const Position& from, const Position& to,
                          double ratio) const
{
    double dx = to.getX() - from.getX();
    double dy = to.getY() - from.getY();

    if (dx == 0 && dy == 0){
        double dth = theta_diff(from.getTheta(), to.getTheta());
        return Position(from.getX(), from.getY(), from.getTheta()+ratio*dth);
    }else{
        
        double theta = atan2(dy, dx);
        
        double dth1 = theta_diff(from.getTheta(), theta);
        double d1 = Position::getWeightTh()*fabs(dth1);
        
        dx *= Position::getWeightX();
        dy *= Position::getWeightY();
        double d2 = sqrt(dx*dx + dy*dy);
        
        double dth2 = theta_diff(theta, to.getTheta());
        double d3 = Position::getWeightTh() * fabs(dth2);
        
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
            return Position(from.getX(), from.getY(), 
                            from.getTheta() + ratio*d/d1*dth1);
        }else if (ratio*d >= d1 && ratio*d < (d1+d2)){
            double r = (ratio*d - d1)/d2;
            return Position((1-r)*from.getX() + r*to.getX(),
                            (1-r)*from.getY() + r*to.getY(),
                            theta);
        }else if (ratio*d >= (d1+d2) && ratio <= 1.0){
            return Position(to.getX(), to.getY(), 
                            theta + (ratio*d-d1-d2)/d3*dth2);
        }else{
            std::cout << "TGT::interpolate() : invalid ratio(" << ratio << ")"
                      << std::endl;
        }
    }
}

double TGT::distance(const Position& from, const Position& to) const
{
    double dx = to.getX() - from.getX();
    double dy = to.getY() - from.getY();
    double theta = atan2(dy, dx);

    dx *= Position::getWeightX();
    dy *= Position::getWeightY();

    if (dx == 0 && dy == 0) {
        return  Position::getWeightTh()*fabs(theta_diff(from.getTheta(), to.getTheta())); 
    }


    double dth1 = fabs(theta_diff(from.getTheta(), theta));
    dth1 *= Position::getWeightTh();
    
    double dth2 = fabs(theta_diff(theta, to.getTheta()));
    dth2 *= Position::getWeightTh();

    //std::cout << "d = " << sqrt(dx*dx + dy*dy) << " +  " << dth1 << " + " <<  dth2 << std::endl;
    return sqrt(dx*dx + dy*dy) + dth1 + dth2;
}
