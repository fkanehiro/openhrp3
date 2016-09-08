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
   \brief The header file of the LinkTraverse class
   \author Shin'ichiro Nakaoka
*/

#ifndef HRPMODEL_LINK_TRAVERSE_H_INCLUDED
#define HRPMODEL_LINK_TRAVERSE_H_INCLUDED

#include <vector>
#include <ostream>
#include <hrpUtil/config.h>
#include "Config.h"

namespace hrp {

    class Link;

    class HRPMODEL_API LinkTraverse
    {
      public:
        LinkTraverse();
        LinkTraverse(int size);
        LinkTraverse(Link* root, bool doUpward = false, bool doDownward = true);

        virtual ~LinkTraverse();

        virtual void find(Link* root, bool doUpward = false, bool doDownward = true);

        inline unsigned int numLinks() const {
            return links.size();
        }

        inline bool empty() const {
            return links.empty();
        }

        inline size_t size() const {
            return links.size();
        }

        inline Link* rootLink() const {
            return (links.empty() ? 0 : links.front());
        }

        inline Link* link(int index) const {
            return links[index];
        }

        inline Link* operator[] (int index) const {
            return links[index];
        }

        inline std::vector<Link*>::const_iterator begin() const {
            return links.begin();
        }

        inline std::vector<Link*>::const_iterator end() const {
            return links.end();
        }
	
        /**
           If the connection from the queried link to the next link is downward (forward) direction,
           the method returns true. Otherwise, returns false.
           The range of valid indices is 0 to (numLinks() - 2).
        */
        inline bool isDownward(int index) const {
            return (index >= numUpwardConnections);
        }
	
        void calcForwardKinematics(bool calcVelocity = false, bool calcAcceleration = false) const;

      protected:
        
        std::vector<Link*> links;
        int numUpwardConnections;

      private:
        
        void traverse(Link* link, bool doUpward, bool doDownward, bool isUpward, Link* prev);

    };

};

HRPMODEL_API std::ostream& operator<<(std::ostream& os, hrp::LinkTraverse& traverse);

#endif
