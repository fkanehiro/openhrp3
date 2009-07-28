/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

/**
   \file
   \brief Implementations of the LinkPath class
   \author Shin'ichiro Nakaoka
*/
  
#include "LinkPath.h"
#include "Link.h"
#include <algorithm>

using namespace std;
using namespace hrp;


LinkPath::LinkPath()
{

}


LinkPath::LinkPath(Link* base, Link* end)
{
    find(base, end);
}


/// path from the root link
LinkPath::LinkPath(Link* base)
{
    find(base);
}


/// This method is disabled.
void LinkPath::find(Link* root, bool doUpward, bool doDownward)
{
    throw "The find method for LinkTraverse cannot be used in LinkPath";
}


bool LinkPath::find(Link* base, Link* end)
{
    links.clear();
    numUpwardConnections = 0;
    bool found = findPathSub(base, 0, end, false);
    if(!found){
        links.clear();
    }
    return found;
}


bool LinkPath::findPathSub(Link* link, Link* prev, Link* end, bool isUpward)
{
    links.push_back(link);
    if(isUpward){
        ++numUpwardConnections;
    }
    
    if(link == end){
        return true;
    }

    for(Link* child = link->child; child; child = child->sibling){
        if(child != prev){
            if(findPathSub(child, link, end, false)){
                return true;
            }
        }
    }

    Link* parent = link->parent;
    if(parent && parent != prev){
        if(findPathSub(parent, link, end, true)){
            return true;
        }
    }

    links.pop_back();
    if(isUpward){
        --numUpwardConnections;
    }

    return false;
}


/// path from the root link
void LinkPath::find(Link* end)
{
    links.clear();
    numUpwardConnections = 0;
    findPathFromRootSub(end);
    std::reverse(links.begin(), links.end());
}


void LinkPath::findPathFromRootSub(Link* link)
{
    links.push_back(link);
    if(link->parent){
        findPathFromRootSub(link->parent);
    }
}
