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

#ifndef OPENHRP_PARSER_VRML_PARSER_H_INCLUDED
#define OPENHRP_PARSER_VRML_PARSER_H_INCLUDED

#include "config.h"
#include "VrmlNodes.h"
#include <string>

namespace hrp {

    class VrmlParserImpl;

    /**
       \brief Parser for VRML97 format

       The VrmlParser class reads a VRML97 file and extract its nodes.
    */
    class HRP_PARSER_EXPORT VrmlParser
    {
    public:

	/**
	   Constructor. This version of constructor do 'load' mehtod 
	   after constructing the object.

	   \param filename file name of a target VRML97 file.
	*/
	VrmlParser(const std::string& filename);
	VrmlParser();
	~VrmlParser();

        void setProtoInstanceActualNodeExtractionMode(bool isOn);
	void load(const std::string& filename);

	/**
	   This method returns the top node of the next node tree written in the file.
	*/
	VrmlNodePtr readNode();

      private:

        VrmlParserImpl* impl;

        void init();
    };
};

#endif
