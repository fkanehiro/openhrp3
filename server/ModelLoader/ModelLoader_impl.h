/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

/*! @file
   \file ModelLoader/server/ModelLoader_impl.h
   @author Shin'ichiro Nakaoka
*/

#ifndef OPENHRP_MODELLOADER_IMPL_H_INCLUDED
#define OPENHRP_MODELLOADER_IMPL_H_INCLUDED

#include <map>
#include <string>
#include <hrpCorba/ORBwrap.h>
#include <hrpCorba/ModelLoader.hh>

#include "BodyInfo_impl.h"
#include "SceneInfo_impl.h"

using namespace OpenHRP;


class ModelLoader_impl : public POA_OpenHRP::ModelLoader
{
    CORBA::ORB_var orb;
    PortableServer::POA_var poa;
		
    typedef std::map<std::string, BodyInfo_impl*> UrlToBodyInfoMap;
    UrlToBodyInfoMap urlToBodyInfoMap;

    BodyInfo_impl* loadBodyInfoFromModelFile(const std::string url, const OpenHRP::ModelLoader::ModelLoadOption option );
		
  public:
		
    ModelLoader_impl(CORBA::ORB_ptr orb, PortableServer::POA_ptr poa);
    virtual ~ModelLoader_impl();
		
    virtual PortableServer::POA_ptr _default_POA();
		
    virtual BodyInfo_ptr getBodyInfo(const char* url)
        throw (CORBA::SystemException, OpenHRP::ModelLoader::ModelLoaderException);

    virtual BodyInfo_ptr getBodyInfoEx(const char* url, const OpenHRP::ModelLoader::ModelLoadOption& option )
        throw (CORBA::SystemException, OpenHRP::ModelLoader::ModelLoaderException);

    virtual BodyInfo_ptr loadBodyInfo(const char* url)
        throw (CORBA::SystemException, OpenHRP::ModelLoader::ModelLoaderException);

    virtual BodyInfo_ptr loadBodyInfoEx(const char* url, const OpenHRP::ModelLoader::ModelLoadOption& option)
        throw (CORBA::SystemException, OpenHRP::ModelLoader::ModelLoaderException);

    virtual SceneInfo_ptr loadSceneInfo(const char* url)
        throw (CORBA::SystemException, OpenHRP::ModelLoader::ModelLoaderException);
		
    virtual void clearData();
		
    void shutdown();
};


#endif
