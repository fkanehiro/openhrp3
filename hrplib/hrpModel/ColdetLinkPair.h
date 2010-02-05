/*! @file
  @author Shin'ichiro Nakaoka
*/

#ifndef HRPMODEL_COLDET_LINK_PAIR_H_INCLUDED
#define HRPMODEL_COLDET_LINK_PAIR_H_INCLUDED

#include <hrpCollision/ColdetModelPair.h>
#include "Config.h"

namespace hrp {

    class Link;

    class HRPMODEL_API ColdetLinkPair : public ColdetModelPair
    {
      public:
        ColdetLinkPair(Link* link1, Link* link2);
        ColdetLinkPair(const ColdetLinkPair& org);
        virtual ~ColdetLinkPair();
        
        void updatePositions();
        
        hrp::Link* link(int index) { return links[index]; }
        
      protected:
        hrp::Link* links[2];
        
      private:
    };
    
    typedef boost::intrusive_ptr<ColdetLinkPair> ColdetLinkPairPtr;
}

#endif
