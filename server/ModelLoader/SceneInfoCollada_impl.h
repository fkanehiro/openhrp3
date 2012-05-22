// -*- coding: utf-8 -*-
// Copyright (C) 2011 University of Tokyo, General Robotix Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//     http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
/*!
  @file SceneInfoCollada_impl.h
  @brief 
  @author Kei Okada (kei.okada@gmail.com)

  Used OpenRAVE files for reference.
*/
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

#ifndef OPENHRP_MODEL_LOADER_SCENE_INFO_COLLADA_IMPL_H_INCLUDED
#define OPENHRP_MODEL_LOADER_SCENE_INFO_COLLADA_IMPL_H_INCLUDED

#include <string>
#include <hrpCorba/ModelLoader.hh>
#include <hrpUtil/EasyScanner.h>

#include "SceneInfo_impl.h"

using namespace OpenHRP;

class SceneInfoCollada_impl : public SceneInfo_impl
{
public:
    BodyInfoCollada_impl* probot;

    SceneInfoCollada_impl(PortableServer::POA_ptr poa) : ShapeSetInfo_impl(poa), SceneInfo_impl(poa)
    {
	probot = new BodyInfoCollada_impl(poa);
    }
    virtual ~SceneInfoCollada_impl()
    {
    }

    void load(const std::string& filename)
    {
	try {
            probot->loadModelFile(filename);
	    url_ = CORBA::string_dup(filename.c_str());

	    shapes_ = probot->shapes_;
	    appearances_ = probot->appearances_;
	    materials_ = probot->materials_;
	    textures_ = probot->textures_;

	    Matrix44 E(Matrix44::Identity());
	    AllLinkShapeIndexSequence* asis = probot->linkShapeIndices();
	    size_t linkLength = asis->length();
	    for(size_t linkIndex = 0; linkIndex < linkLength; ++linkIndex) {
		TransformedShapeIndexSequence tsis = (*asis)[linkIndex];
		for(size_t segmentIndex = 0; segmentIndex < tsis.length(); ++segmentIndex) {
		    long length = shapeIndices_.length();
		    shapeIndices_.length(length+1);
		    TransformedShapeIndex tsi = tsis[segmentIndex];
		    shapeIndices_[length] = tsi;

		    const DblArray12& M = tsi.transformMatrix;
		    inlinedShapeTransformMatrices_.length(length+1);
		    for(int i=0; i<12;i++) {
			inlinedShapeTransformMatrices_[length][i] = M[i];
		    }
		}
	    }
	} catch(EasyScanner::Exception& ex){
	    cout << ex.getFullMessage() << endl;
	    throw ModelLoader::ModelLoaderException(ex.getFullMessage().c_str());
	}
    }
};


#endif
