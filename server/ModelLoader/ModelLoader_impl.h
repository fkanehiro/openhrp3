/*! @file
  @author S.NAKAOKA
*/

#ifndef MODELLOADER_IMPL_H_INCLUDED
#define MODELLOADER_IMPL_H_INCLUDED

/**
   \file ModelLoader/server/ModelLoader_impl.h
*/

#include "BodyInfo_impl.h"

#include <OpenHRP/Corba/ORBwrap.h>
#include <OpenHRP/Corba/ModelLoader.h>

#include <map>
#include <string>


namespace OpenHRP {

    class ModelLoader_impl : public POA_OpenHRP::ModelLoader
    {
        CORBA::ORB_var orb;
        PortableServer::POA_var poa;
		
        typedef std::map<std::string, BodyInfo_impl*> UrlToBodyInfoMap;
        UrlToBodyInfoMap urlToBodyInfoMap;

        BodyInfo_impl* loadBodyInfoFromModelFile(const std::string url);
		
    public:
		
        ModelLoader_impl(CORBA::ORB_ptr orb, PortableServer::POA_ptr poa);
        ~ModelLoader_impl();
		
        virtual PortableServer::POA_ptr _default_POA();
		
        virtual BodyInfo_ptr getBodyInfo(const char* url)
            throw (CORBA::SystemException, OpenHRP::ModelLoader::ModelLoaderException);

        virtual BodyInfo_ptr loadBodyInfo(const char* url)
            throw (CORBA::SystemException, OpenHRP::ModelLoader::ModelLoaderException);
		
        virtual void clearData();
		
        void shutdown();
    };
};

#endif
