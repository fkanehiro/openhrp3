/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

/**
   \file ModelLoader/server/ModelLoader_impl.cpp
   \author Shin'ichiro Nakaoka
*/

#include "ModelLoader_impl.h"

#include <iostream>
#include <sys/types.h>
#include <sys/stat.h>

#include "VrmlUtil.h"


using namespace std;


ModelLoader_impl::ModelLoader_impl(CORBA::ORB_ptr orb, PortableServer::POA_ptr poa)
    :
    orb(CORBA::ORB::_duplicate(orb)),
    poa(PortableServer::POA::_duplicate(poa))
{

}


ModelLoader_impl::~ModelLoader_impl()
{
    clearData();
}


PortableServer::POA_ptr ModelLoader_impl::_default_POA()
{
    return PortableServer::POA::_duplicate(poa);
}


BodyInfo_ptr ModelLoader_impl::loadBodyInfo(const char* url)
    throw (CORBA::SystemException, OpenHRP::ModelLoader::ModelLoaderException)
{
    OpenHRP::ModelLoader::ModelLoadOption option;
    option.readImage = false;
    option.AABBdata.length(0);
    option.AABBtype = OpenHRP::ModelLoader::AABB_NUM;
    BodyInfo_impl* bodyInfo = loadBodyInfoFromModelFile(url, option);
    return bodyInfo->_this();
}

BodyInfo_ptr ModelLoader_impl::loadBodyInfoEx(const char* url, const OpenHRP::ModelLoader::ModelLoadOption& option)
    throw (CORBA::SystemException, OpenHRP::ModelLoader::ModelLoaderException)
{
    BodyInfo_impl* bodyInfo = loadBodyInfoFromModelFile(url, option);
    if(option.AABBdata.length()){
        bodyInfo->setParam("AABBType", (int)option.AABBtype);
        int length=option.AABBdata.length();
        unsigned int* _AABBdata = new unsigned int[length];
        for(int i=0; i<length; i++)
            _AABBdata[i] = option.AABBdata[i];
        bodyInfo->changetoBoundingBox(_AABBdata);
        delete[] _AABBdata;
    }
    return bodyInfo->_this();
}


BodyInfo_ptr ModelLoader_impl::getBodyInfoEx(const char* url0, const OpenHRP::ModelLoader::ModelLoadOption& option)
    throw (CORBA::SystemException, OpenHRP::ModelLoader::ModelLoaderException)
{
    string url(url0);

    BodyInfo_impl* bodyInfo = 0;
    
    string filename(deleteURLScheme(url));
    struct stat statbuff;
    time_t mtime = 0;

    // get a file modification time
    if( stat( filename.c_str(), &statbuff ) == 0 ){
        mtime = statbuff.st_mtime;
    }

    UrlToBodyInfoMap::iterator p = urlToBodyInfoMap.find(url);
    if(p != urlToBodyInfoMap.end() && mtime == p->second->getLastUpdateTime() && p->second->checkInlineFileUpdateTime()){
        bodyInfo = p->second;
        cout << string("cache found for ") + url << endl;
        if(option.AABBdata.length()){
            bodyInfo->setParam("AABBType", (int)option.AABBtype);
            int length=option.AABBdata.length();
            unsigned int* _AABBdata = new unsigned int[length];
            for(int i=0; i<length; i++)
                _AABBdata[i] = option.AABBdata[i];
            bodyInfo->changetoBoundingBox(_AABBdata);
            delete[] _AABBdata;
        }
        return bodyInfo->_this();
    } 
    return loadBodyInfoEx(url0, option);
}

BodyInfo_ptr ModelLoader_impl::getBodyInfo(const char* url)
    throw (CORBA::SystemException, OpenHRP::ModelLoader::ModelLoaderException)
{
    OpenHRP::ModelLoader::ModelLoadOption option;
    option.readImage = false;
    option.AABBdata.length(0);
    option.AABBtype = OpenHRP::ModelLoader::AABB_NUM;
    return getBodyInfoEx(url, option);
}

BodyInfo_impl* ModelLoader_impl::loadBodyInfoFromModelFile(const string url, const OpenHRP::ModelLoader::ModelLoadOption option)
{
    cout << "loading " << url << endl;

    BodyInfo_impl* bodyInfo = new BodyInfo_impl(poa);
    bodyInfo->setParam("readImage", option.readImage);

    try {
	    bodyInfo->loadModelFile(url);
    }
    catch(OpenHRP::ModelLoader::ModelLoaderException& ex){
	    cout << "loading failed.\n";
	    cout << ex.description << endl;
	    //bodyInfo->_remove_ref();
	    throw;
    }
    cout << "The model was successfully loaded ! " << endl;
    
    //poa->activate_object(bodyInfo);
    urlToBodyInfoMap[url] = bodyInfo;

    string filename(deleteURLScheme(url));
    struct stat statbuff;
    time_t mtime = 0;

    // get a file modification time
    if( stat( filename.c_str(), &statbuff ) == 0 ){
        mtime = statbuff.st_mtime;
    }
    bodyInfo->setLastUpdateTime( mtime );

    return bodyInfo;
}



SceneInfo_ptr ModelLoader_impl::loadSceneInfo(const char* url)
    throw (CORBA::SystemException, OpenHRP::ModelLoader::ModelLoaderException)
{
    cout << "loading " << url << endl;

    SceneInfo_impl* sceneInfo = new SceneInfo_impl(poa);

    try {
	sceneInfo->load(url);
    }
    catch(OpenHRP::ModelLoader::ModelLoaderException& ex){
	cout << "loading failed.\n";
	cout << ex.description << endl;
	//sceneInfo->_remove_ref();
	throw;
    }
    cout << url << " was successfully loaded ! " << endl;
    
    //poa->activate_object(sceneInfo);

    return sceneInfo->_this();
}


void ModelLoader_impl::clearData()
{
    //UrlToBodyInfoMap::iterator p;
    //for(p = urlToBodyInfoMap.begin(); p != urlToBodyInfoMap.end(); ++p){
    //	BodyInfo_impl* bodyInfo = p->second;
    //	PortableServer::ObjectId_var objectId = poa->servant_to_id(bodyInfo);
    //	poa->deactivate_object(objectId);
    //	bodyInfo->_remove_ref();
    //}
    urlToBodyInfoMap.clear();
}


void ModelLoader_impl::shutdown()
{
    clearData();
    orb->shutdown(false);
}
