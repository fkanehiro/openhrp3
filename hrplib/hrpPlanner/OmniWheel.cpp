// -*- mode: c++; indent-tabs-mode: nil; c-basic-offset: 4; tab-width: 4; -*-
#include "OmniWheel.h"

#ifndef M_PI
#define M_PI 3.14159
#endif

using namespace PathEngine;

Position OmniWheel::interpolate(const Position& from, const Position& to,
				double ratio) const
{
    double x = (1-ratio)*from.getX() + ratio*to.getX();
    double y = (1-ratio)*from.getY() + ratio*to.getY();
    double dth = to.getTheta() - from.getTheta();
    if (fabs(dth) > M_PI){
        dth = dth > 0 ? -(2*M_PI-dth) : 2*M_PI+dth;
    }
    double th = theta_limit(from.getTheta() + ratio*dth);
    return Position(x,y,th);
}

double OmniWheel::distance(const Position& from, const Position& to) const
{
    double dx = Position::getWeightX()*(to.getX() - from.getX());
    double dy = Position::getWeightY()*(to.getY() - from.getY());
    double dth = fabs(to.getTheta() - from.getTheta());
    if (dth > M_PI) dth = 2*M_PI - dth;
    dth *= Position::getWeightTh();
    
    return sqrt(dx*dx + dy*dy + dth*dth);
}



