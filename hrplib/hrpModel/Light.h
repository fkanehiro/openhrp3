/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */
#ifndef HRPMODEL_LIGHT_H_INCLUDED
#define HRPMODEL_LIGHT_H_INCLUDED

#include "Link.h"

namespace hrp {

    class Light{
    public:
        enum LightType {
            POINT,
            DIRECTIONAL,
            SPOT
        };
        Light(Link *parent, int lightType, const std::string &name_);

        static int nextId;
        // common attributes
        Link *link;
        int type;
        std::string name;
        Matrix33 localR;
        Vector3 localPos;
        int id; // unique id in the world
        double ambientIntensity, intensity;
        Vector3 color;
        bool on;
        // attributes for point light and spot light
        Vector3 attenuation, location;
        double radius; 
        // attribute for directional light and spot light
        Vector3 direction;
        // attributes for spot light
        double beamWidth, cutOffAngle; 
    };
};

#endif
