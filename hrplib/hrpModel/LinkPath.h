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
   \author Shin'ichiro Nakaoka
*/

#ifndef HRPMODEL_LINK_PATH_H_INCLUDED
#define HRPMODEL_LINK_PATH_H_INCLUDED

#include "LinkTraverse.h"
#include "exportdef.h"

namespace hrp {

    class HRPMODEL_API LinkPath : public LinkTraverse
    {
      public:
        
        LinkPath();
        LinkPath(Link* base, Link* end);
        LinkPath(Link* end);

        bool find(Link* base, Link* end);
        void find(Link* end);

        inline Link* baseLink() const {
            return links.front();
        }
        
        inline Link* endLink() const {
            return links.back();
        }

      private:

        virtual void find(Link* root, bool doUpward, bool doDownward);

        bool findPathSub(Link* link, Link* prev, Link* end, bool isForwardDirection);
        void findPathFromRootSub(Link* link);
    };

};

#endif
