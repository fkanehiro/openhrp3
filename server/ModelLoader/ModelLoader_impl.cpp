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

#include "UtilFunctions.h"

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
    BodyInfo_impl* bodyInfo = loadBodyInfoFromModelFile(url);
    return bodyInfo->_this();
}


BodyInfo_ptr ModelLoader_impl::getBodyInfo(const char* url0)
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
    // VRMLが複数のファイルからなる場合、inlineのファイルの更新時刻もみないことには、
    // タイムスタンプの比較は完全ではない。
    //if(p != urlToBodyInfoMap.end() && mtime == p->second->getLastUpdateTime()){
    if(false){
        bodyInfo = p->second;
        cout << string("cache found for ") + url << endl;
    } else {
        bodyInfo = loadBodyInfoFromModelFile(url);
        bodyInfo->setLastUpdateTime( mtime );
    }

    return bodyInfo->_this();
}


BodyInfo_impl* ModelLoader_impl::loadBodyInfoFromModelFile(const string url)
{
    cout << "loading " << url << endl;

    BodyInfo_impl* bodyInfo = new BodyInfo_impl(poa);

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
