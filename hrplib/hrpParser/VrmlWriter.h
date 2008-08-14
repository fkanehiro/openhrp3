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

#ifndef VRML_WRITER_INCLUDED
#define VRML_WRITER_INCLUDED

#include "config.h"
#include "VrmlNodes.h"

#include <map>
#include <string>
#include <ostream>


namespace hrp {

    class VrmlWriter;

    typedef void (VrmlWriter::*VrmlWriterNodeMethod)(VrmlNodePtr node);


    class HRP_PARSER_EXPORT VrmlWriter
    {
    public:
	VrmlWriter(std::ostream& out);
  
	void writeHeader();
	bool writeNode(VrmlNodePtr node);

	struct TIndent {
	    void clear() { n = 0; spaces.resize(n); }
	    TIndent& operator++() { n += 2; spaces.resize(n, ' '); return *this; }
	    TIndent& operator--() { 
		n -= 2;
		if(n < 0) { n = 0; }
		spaces.resize(n, ' '); return *this; 
	    }
	    std::string spaces;
	    int n;
	};

    private:
	std::ostream& out;

	TIndent indent;

	typedef std::map<std::string, VrmlWriterNodeMethod> TNodeMethodMap;
	typedef std::pair<std::string, VrmlWriterNodeMethod> TNodeMethodPair;

	static TNodeMethodMap nodeMethodMap;

	static void registNodeMethod(const std::type_info& t, VrmlWriterNodeMethod method) {
	    nodeMethodMap.insert(TNodeMethodPair(t.name(), method)); 
	}
	static VrmlWriterNodeMethod getNodeMethod(VrmlNodePtr node);

	static void registerNodeMethodMap();

	template <class MFValues> void writeMFValues(MFValues values, int numColumn);
	void writeMFInt32SeparatedByMinusValue(MFInt32& values);

	void writeNodeIter(VrmlNodePtr node);
	void beginNode(const char* nodename, VrmlNodePtr node);
	void endNode();
	void writeGroupNode(VrmlNodePtr node);
	void writeGroupFields(VrmlGroupPtr group);
	void writeTransformNode(VrmlNodePtr node);
	void writeShapeNode(VrmlNodePtr node);
	void writeAppearanceNode(VrmlAppearancePtr appearance);
	void writeMaterialNode(VrmlMaterialPtr material);
	void writeIndexedFaceSetNode(VrmlNodePtr node);
	void writeCoordinateNode(VrmlCoordinatePtr coord);

    };

};


#endif
