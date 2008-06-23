// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-

/**
   \file
   \brief The header file of the LinkTraverse class
   \author Shin'ichiro Nakaoka
*/

#ifndef OPENHRP_LINK_TRAVERSE_H_INCLUDED
#define OPENHRP_LINK_TRAVERSE_H_INCLUDED

#include <vector>
#include <ostream>

#include "hrpModelExportDef.h"


namespace OpenHRP {

    class Link;

    class HRPMODEL_EXPORT LinkTraverse
    {
    public:
		LinkTraverse();
		LinkTraverse(int size);
		LinkTraverse(Link* root, bool doUpward = false, bool doDownward = true);

		virtual ~LinkTraverse();
	
		void find(Link* root, bool doUpward = false, bool doDownward = true);

		inline int numLinks() const {
			return links.size();
		}
		
		inline Link* rootLink() const {
			return links.front();
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

		/**
		   @deprecated use operator<<
		*/
        void putInformation(std::ostream& os);
	
    protected:
		std::vector<Link*> links;
		int numUpwardConnections;
	
    private:
        void traverse(Link* link, bool doUpward, bool doDownward, bool isUpward, Link* prev);

    };

};


HRPMODEL_EXPORT std::ostream& operator<<(std::ostream& os, OpenHRP::LinkTraverse& traverse);


#endif
