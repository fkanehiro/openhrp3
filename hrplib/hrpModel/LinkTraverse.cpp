/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */

/** 
    \file
    \brief Implementations of the LinkTraverse class
    \author Shin'ichiro Nakaoka, Rafael Cisneros
*/
  

#include "LinkTraverse.h"
#include "Link.h"
#include <hrpUtil/Eigen3d.h>

using namespace std;
using namespace hrp;


LinkTraverse::LinkTraverse()
{

}


LinkTraverse::LinkTraverse(int size)
    : links(size)
{
    links.clear();
}


LinkTraverse::LinkTraverse(Link* root, bool doUpward, bool doDownward)
{
    find(root, doUpward, doDownward);
}


LinkTraverse::~LinkTraverse()
{

}


void LinkTraverse::find(Link* root, bool doUpward, bool doDownward)
{
    numUpwardConnections = 0;
    links.clear();
    traverse(root, doUpward, doDownward, false, 0);
}


void LinkTraverse::traverse(Link* link, bool doUpward, bool doDownward, bool isUpward, Link* prev)
{
    links.push_back(link);
    if(isUpward){
        ++numUpwardConnections;
    }
    
    if(doUpward && link->parent){
        traverse(link->parent, doUpward, true, true, link);
    }
    if(doDownward){
        for(Link* child = link->child; child; child = child->sibling){
            if(child != prev){
                traverse(child, false, true, false, 0);
            }
        }
    }
}


void LinkTraverse::calcForwardKinematics(bool calcVelocity, bool calcAcceleration) const
{
    Vector3 arm;
    int i;
    for(i=1; i <= numUpwardConnections; ++i){

        Link* link = links[i];
        Link* child = links[i-1];

        switch(child->jointType){

        case Link::ROTATIONAL_JOINT:
            link->R.noalias() = child->R * rodrigues(child->a, child->q).transpose();
            arm.noalias() = link->R * child->b;
            link->p = child->p - arm;

            if(calcVelocity){
                child->sw.noalias() = link->R * child->a;
                link->w = child->w - child->dq * child->sw;
                link->v = child->v - link->w.cross(arm);

                if(calcAcceleration){
                    link->dw = child->dw - child->dq * child->w.cross(child->sw) - (child->ddq * child->sw);
                    link->dv = child->dv - child->w.cross(child->w.cross(arm)) - child->dw.cross(arm);
                }
            }
            break;
            
        case Link::SLIDE_JOINT:
            link->R = child->R;
            arm.noalias() = link->R * (child->b + child->q * child->d);
            link->p = child->p - arm;

            if(calcVelocity){
                child->sv.noalias() = link->R * child->d;
                link->w = child->w;
                link->v = child->v - child->dq * child->sv;

                if(calcAcceleration){
                    link->dw = child->dw;
                    link->dv = child->dv - child->w.cross(child->w.cross(arm)) - child->dw.cross(arm)
                        - 2.0 * child->dq * child->w.cross(child->sv) - child->ddq * child->sv;
                }
            }
            break;
            
        case Link::FIXED_JOINT:
        default:
            link->R = child->R;
            link->p = child->p - (link->R * child->b);

            if(calcVelocity){
                link->w = child->w;
                link->v = child->v;
				
                if(calcAcceleration){
                    link->dw = child->dw;
                    link->dv = child->dv;
                }
            }
            break;
        }
    }

    int n = links.size();
    for( ; i < n; ++i){
        
        Link* link = links[i];
        Link* parent = link->parent;

        switch(link->jointType){
            
        case Link::ROTATIONAL_JOINT:
            link->R.noalias() = parent->R * rodrigues(link->a, link->q);
            arm.noalias() = parent->R * link->b;
            link->p = parent->p + arm;

            if(calcVelocity){
                link->sw.noalias() = parent->R * link->a;
                link->w = parent->w + link->sw * link->dq;
                link->v = parent->v + parent->w.cross(arm);

                if(calcAcceleration){
                    link->dw = parent->dw + link->dq * parent->w.cross(link->sw) + (link->ddq * link->sw);
                    link->dv = parent->dv + parent->w.cross(parent->w.cross(arm)) + parent->dw.cross(arm);
                }
            }
            break;
            
        case Link::SLIDE_JOINT:
            link->R = parent->R;
            arm.noalias() = parent->R * (link->b + link->q * link->d);
            link->p = parent->p + arm;

            if(calcVelocity){
                link->sv.noalias() = parent->R * link->d;
                link->w = parent->w;
                link->v = parent->v + link->sv * link->dq;

                if(calcAcceleration){
                    link->dw = parent->dw;
                    link->dv = parent->dv + parent->w.cross(parent->w.cross(arm)) + parent->dw.cross(arm)
                        + 2.0 * link->dq * parent->w.cross(link->sv) + link->ddq * link->sv;
                }
            }
            break;

        case Link::FIXED_JOINT:
        default:
            link->R = parent->R;
            link->p = parent->R * link->b + parent->p;

            if(calcVelocity){
                link->w = parent->w;
                link->v = parent->v;

                if(calcAcceleration){
                    link->dw = parent->dw;
                    link->dv = parent->dv;
                }
            }
            break;
        }
    }
}


double LinkTraverse::calcTotalMass()
{
  totalMass_ = 0.0;

  for(int i = 0; i < numLinks(); ++i) {
    totalMass_ += links[i]->m;
  }

  return totalMass_;
}


std::ostream& operator<<(std::ostream& os, LinkTraverse& traverse)
{
    int n = traverse.numLinks();
    for(int i=0; i < n; ++i){
        Link* link = traverse[i];
        os << link->name;
        if(i != n){
            os << (traverse.isDownward(i) ? " => " : " <= ");
        }
    }
    os << std::endl;

    return os;
}
