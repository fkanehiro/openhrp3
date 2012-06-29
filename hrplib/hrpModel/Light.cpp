/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */
#include "Light.h"

using namespace hrp;

int Light::nextId=0;

Light::Light(Link *parent, int lightType, const std::string &name_) :
    link(parent), type(lightType), name(name_), id(nextId){
    link->lights.push_back(this);
    nextId++;
}

