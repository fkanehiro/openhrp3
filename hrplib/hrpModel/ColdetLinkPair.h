/*! @file
  @author Shin'ichiro Nakaoka
*/

#ifndef HRPMODEL_COLDET_LINK_PAIR_H_INCLUDED
#define HRPMODEL_COLDET_LINK_PAIR_H_INCLUDED

#include <hrpCollision/ColdetModelPair.h>
#include "Link.h"
#include "Config.h"

namespace hrp {
    
    class Link;
    
    class HRPMODEL_API ColdetLinkPair : public ColdetModelPair
    {
      public:
        ColdetLinkPair(Link* link1, Link* link2)
            : ColdetModelPair(link1->coldetModel, link2->coldetModel) {
            links[0] = link1;
            links[1] = link2;
        }
        
        ColdetLinkPair(const ColdetLinkPair& org)
            : ColdetModelPair(org) {
            links[0] = org.links[0];
            links[1] = org.links[1];
        }
        
        virtual ~ColdetLinkPair() { }
        
        void updatePositions() {
            model(0)->setPosition(links[0]->R, links[0]->p);
            model(1)->setPosition(links[1]->R, links[1]->p);
        }
        
        hrp::Link* link(int index) { return links[index]; }
        
      protected:
        hrp::Link* links[2];
        
      private:
    };
    
    typedef boost::intrusive_ptr<ColdetLinkPair> ColdetLinkPairPtr;
}

#endif
