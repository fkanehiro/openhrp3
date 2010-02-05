/*! @file
  @author Shin'ichiro Nakaoka
*/

#include "ColdetLinkPair.h"
#include "Link.h"
#include <hrpCollision/ColdetModel.h>

using namespace hrp;

ColdetLinkPair::ColdetLinkPair(Link* link1, Link* link2)
    : ColdetModelPair(new ColdetModel(*link1->coldetModel), new ColdetModel(*link2->coldetModel))
{
    links[0] = link1;
    links[1] = link2;
}


ColdetLinkPair::ColdetLinkPair(const ColdetLinkPair& org)
    : ColdetModelPair(org)
{
    links[0] = org.links[0];
    links[1] = org.links[1];
}


ColdetLinkPair::~ColdetLinkPair()
{

}


void ColdetLinkPair::updatePositions()
{
    for(int i=0; i < 2; ++i){
        Link* link = links[i];
        model(i)->setPosition(link->R, link->p);
    }
}
