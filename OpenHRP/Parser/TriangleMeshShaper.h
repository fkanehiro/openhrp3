/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */

/*! @file
  @author Shin'ichiro Nakaoka
*/


#ifndef OPENHRP_PARSER_TRIANGLE_MESH_SHAPER_H_INCLUDED
#define OPENHRP_PARSER_TRIANGLE_MESH_SHAPER_H_INCLUDED

#include "config.h"
#include "VrmlNodes.h"
#include <string>
#include <boost/signal.hpp>

namespace hrp
{
    class TMSImpl;
    
    class HRP_PARSER_EXPORT TriangleMeshShaper
    {
      public:

        TriangleMeshShaper();
        ~TriangleMeshShaper();

        void setDivisionNumber(int n);
        void setNormalGenerationMode(bool on);
        VrmlNodePtr apply(VrmlNodePtr topNode);
        VrmlGeometryPtr getOriginalGeometry(VrmlShapePtr shapeNode);
        
        boost::signal<void(const std::string& message)> sigMessage;

      private:
        TMSImpl* impl;
    };
};

#endif

