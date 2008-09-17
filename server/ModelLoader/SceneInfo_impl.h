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

#ifndef OPENHRP_MODEL_LOADER_SCENE_INFO_IMPL_H_INCLUDED
#define OPENHRP_MODEL_LOADER_SCENE_INFO_IMPL_H_INCLUDED

#include <string>
#include <hrpCorba/ModelLoader.h>

#include "ShapeSetInfo_impl.h"

using namespace OpenHRP;


class SceneInfo_impl :
    public virtual POA_OpenHRP::SceneInfo,
    public virtual ShapeSetInfo_impl
{
public:
		
    SceneInfo_impl(PortableServer::POA_ptr poa);
    virtual ~SceneInfo_impl();

    virtual char* url();
    virtual TransformedShapeIndexSequence* shapeIndices();

    void load(const std::string& filename);

protected:

    virtual const std::string& topUrl();

private:
        
    std::string url_;
    TransformedShapeIndexSequence shapeIndices_;
};


#endif
