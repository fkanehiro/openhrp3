/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

/*!
  @file SceneInfo_impl.h
  @author Shin'ichiro Nakaoka
*/

#include "SceneInfo_impl.h"

#include <map>
#include <vector>
#include <iostream>
#include <boost/bind.hpp>

#include <hrpCorba/ViewSimulator.hh>
#include <hrpUtil/EasyScanner.h>
#include <hrpUtil/VrmlNodes.h>
#include <hrpUtil/VrmlParser.h>

#include "VrmlUtil.h"


using namespace std;
using namespace boost;

SceneInfo_impl::SceneInfo_impl(PortableServer::POA_ptr poa) :
    ShapeSetInfo_impl(poa)
{

}


SceneInfo_impl::~SceneInfo_impl()
{
    
}


const std::string& SceneInfo_impl::topUrl()
{
    return url_;
}


char* SceneInfo_impl::url()
{
    return CORBA::string_dup(url_.c_str());
}


TransformedShapeIndexSequence* SceneInfo_impl::shapeIndices()
{
    return new TransformedShapeIndexSequence(shapeIndices_);
}


void SceneInfo_impl::load(const std::string& url)
{
    string filename(deleteURLScheme(url));

    // URL文字列の' \' 区切り子を'/' に置き換え  Windows ファイルパス対応  
    string url2;
    url2 = filename;
    replace( url2, string("\\"), string("/") );
    filename = url2;
    url_ = CORBA::string_dup(url2.c_str());

    try {
        VrmlParser parser;
        parser.load(filename);

        Matrix44 E(Matrix44::Identity());

        while(VrmlNodePtr node = parser.readNode()){
            if(!node->isCategoryOf(PROTO_DEF_NODE)){
                applyTriangleMeshShaper(node);
                traverseShapeNodes(node.get(), E, shapeIndices_, inlinedShapeTransformMatrices_, &topUrl());
            }
        }
    } catch(EasyScanner::Exception& ex){
        cout << ex.getFullMessage() << endl;
	throw ModelLoader::ModelLoaderException(ex.getFullMessage().c_str());
    }
}
