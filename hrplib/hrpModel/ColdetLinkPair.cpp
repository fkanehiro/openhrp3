/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

#include "ColdetLinkPair.h"

namespace hrp {
    
    class Link;
    
    ColdetLinkPair::ColdetLinkPair(Link* link1, Link* link2)
     : ColdetModelPair(link1->coldetModel, link2->coldetModel) {
        links[0] = link1;
        links[1] = link2;
    }
    
    ColdetLinkPair::ColdetLinkPair(const ColdetLinkPair& org)
     : ColdetModelPair(org) {
        links[0] = org.links[0];
        links[1] = org.links[1];
    }
        
    ColdetLinkPair::~ColdetLinkPair() { }
        
    void ColdetLinkPair::updatePositions() {
        model(0)->setPosition(links[0]->R, links[0]->p);
        model(1)->setPosition(links[1]->R, links[1]->p);
    }
        
    hrp::Link* ColdetLinkPair::link(int index) { return links[index]; }
}
