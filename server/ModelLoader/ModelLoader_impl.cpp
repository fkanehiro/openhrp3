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

#ifdef OPENHRP_COLLADA_FOUND
#include <boost/foreach.hpp>
#include "ColladaWriter.h"
#include "BodyInfoCollada_impl.h"
#include "SceneInfoCollada_impl.h"

static bool IsColladaFile(const std::string& filename)
{
    size_t len = filename.size();
    if( len < 4 ) {
        return false;
    }
    if( filename[len-4] == '.' && ::tolower(filename[len-3]) == 'd' && ::tolower(filename[len-2]) == 'a' && ::tolower(filename[len-1]) == 'e' ) {
        return true;
    }
    if( filename[len-4] == '.' && ::tolower(filename[len-3]) == 'z' && ::tolower(filename[len-2]) == 'a' && ::tolower(filename[len-1]) == 'e' ) {
        return true;
    }
    return false;
}

#endif

std::string replaceProjectDir(std::string url) {
  std::string path = url;
  if ( path.find("$(PROJECT_DIR)") != std::string::npos ) {
    std::string shdir = OPENHRP_SHARE_DIR;
    std::string pjdir = shdir + "/sample/project";
    path.replace(path.find("$(PROJECT_DIR)"),14, pjdir);
  }
  return path;
}

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

// the dynamic casts are necessary since the changetoBoundingBox functions are not part of BodyInfo class.
static void setLastUpdateTime(POA_OpenHRP::BodyInfo* bodyInfo, time_t time)
{
    BodyInfo_impl* pBodyInfo_impl = dynamic_cast<BodyInfo_impl*>(bodyInfo);
    if( !!pBodyInfo_impl ) {
        pBodyInfo_impl->setLastUpdateTime(time);
        return;
    }
#ifdef OPENHRP_COLLADA_FOUND
    BodyInfoCollada_impl* pBodyInfoCollada_impl = dynamic_cast<BodyInfoCollada_impl*>(bodyInfo);
    if( !!pBodyInfoCollada_impl ) {
        pBodyInfoCollada_impl->setLastUpdateTime(time);
        return;
    }
#endif
    throw ModelLoader::ModelLoaderException("setLastUpdateTime invalid pointer");
};

static time_t getLastUpdateTime(POA_OpenHRP::BodyInfo* bodyInfo) {
    BodyInfo_impl* pBodyInfo_impl = dynamic_cast<BodyInfo_impl*>(bodyInfo);
    if( !!pBodyInfo_impl ) {
        return pBodyInfo_impl->getLastUpdateTime();
    }
#ifdef OPENHRP_COLLADA_FOUND
    BodyInfoCollada_impl* pBodyInfoCollada_impl = dynamic_cast<BodyInfoCollada_impl*>(bodyInfo);
    if( !!pBodyInfoCollada_impl ) {
        return pBodyInfoCollada_impl->getLastUpdateTime();
    }
#endif
    throw ModelLoader::ModelLoaderException("getLastUpdateTime invalid pointer");
}

static bool checkInlineFileUpdateTime(POA_OpenHRP::BodyInfo* bodyInfo)
{
    BodyInfo_impl* pBodyInfo_impl = dynamic_cast<BodyInfo_impl*>(bodyInfo);
    if( !!pBodyInfo_impl ) {
        return pBodyInfo_impl->checkInlineFileUpdateTime();
    }
#ifdef OPENHRP_COLLADA_FOUND
    BodyInfoCollada_impl* pBodyInfoCollada_impl = dynamic_cast<BodyInfoCollada_impl*>(bodyInfo);
    if( !!pBodyInfoCollada_impl ) {
        return pBodyInfoCollada_impl->checkInlineFileUpdateTime();
    }
#endif
    throw ModelLoader::ModelLoaderException("checkInlineFileUpdateTime invalid pointer");
}

static void setParam(POA_OpenHRP::BodyInfo* bodyInfo, std::string param, int value)
{
    BodyInfo_impl* pBodyInfo_impl = dynamic_cast<BodyInfo_impl*>(bodyInfo);
    if( !!pBodyInfo_impl ) {
        pBodyInfo_impl->setParam(param,value);
        return;
    }
#ifdef OPENHRP_COLLADA_FOUND
    BodyInfoCollada_impl* pBodyInfoCollada_impl = dynamic_cast<BodyInfoCollada_impl*>(bodyInfo);
    if( !!pBodyInfoCollada_impl ) {
        pBodyInfoCollada_impl->setParam(param,value);
        return;
    }
#endif
    throw ModelLoader::ModelLoaderException("setParam(param,value) invalid pointer");
}

static void changetoBoundingBox(POA_OpenHRP::BodyInfo* bodyInfo, unsigned int* depth)
{
    BodyInfo_impl* pBodyInfo_impl = dynamic_cast<BodyInfo_impl*>(bodyInfo);
    if( !!pBodyInfo_impl ) {
        pBodyInfo_impl->changetoBoundingBox(depth);
        return;
    }
#ifdef OPENHRP_COLLADA_FOUND
    BodyInfoCollada_impl* pBodyInfoCollada_impl = dynamic_cast<BodyInfoCollada_impl*>(bodyInfo);
    if( !!pBodyInfoCollada_impl ) {
        pBodyInfoCollada_impl->changetoBoundingBox(depth);
        return;
    }
#endif
    throw ModelLoader::ModelLoaderException("changetoBoundingBox(depth) invalid pointer");
}

BodyInfo_ptr ModelLoader_impl::loadBodyInfo(const char* url)
    throw (CORBA::SystemException, OpenHRP::ModelLoader::ModelLoaderException)
{
    OpenHRP::ModelLoader::ModelLoadOption option;
    option.readImage = false;
    option.AABBdata.length(0);
    option.AABBtype = OpenHRP::ModelLoader::AABB_NUM;
    POA_OpenHRP::BodyInfo* bodyInfo = loadBodyInfoFromModelFile(replaceProjectDir(std::string(url)), option);
    return bodyInfo->_this();
}

BodyInfo_ptr ModelLoader_impl::loadBodyInfoEx(const char* url, const OpenHRP::ModelLoader::ModelLoadOption& option)
    throw (CORBA::SystemException, OpenHRP::ModelLoader::ModelLoaderException)
{
    POA_OpenHRP::BodyInfo* bodyInfo = loadBodyInfoFromModelFile(replaceProjectDir(std::string(url)), option);
    if(option.AABBdata.length()){
        setParam(bodyInfo,"AABBType", (int)option.AABBtype);
        int length=option.AABBdata.length();
        unsigned int* _AABBdata = new unsigned int[length];
        for(int i=0; i<length; i++)
            _AABBdata[i] = option.AABBdata[i];
        changetoBoundingBox(bodyInfo,_AABBdata);
        delete[] _AABBdata;
    }
    return bodyInfo->_this();
}

BodyInfo_ptr ModelLoader_impl::getBodyInfoEx(const char* url0, const OpenHRP::ModelLoader::ModelLoadOption& option)
    throw (CORBA::SystemException, OpenHRP::ModelLoader::ModelLoaderException)
{
    string url(url0);

    BodyInfo_ptr bodyInfo = 0;
    string filename(deleteURLScheme(replaceProjectDir(url)));
    struct stat statbuff;
    time_t mtime = 0;

    // get a file modification time
    if( stat( filename.c_str(), &statbuff ) == 0 ){
        mtime = statbuff.st_mtime;
    }

    UrlToBodyInfoMap::iterator p = urlToBodyInfoMap.find(url);
    if(p != urlToBodyInfoMap.end() && mtime == getLastUpdateTime(p->second) && checkInlineFileUpdateTime(p->second)){
        bodyInfo = p->second->_this();
        cout << string("cache found for ") + url << endl;
        if(option.AABBdata.length()){
            setParam(p->second,"AABBType", (int)option.AABBtype);
            int length=option.AABBdata.length();
            unsigned int* _AABBdata = new unsigned int[length];
            for(int i=0; i<length; i++)
                _AABBdata[i] = option.AABBdata[i];
            changetoBoundingBox(p->second,_AABBdata);
            delete[] _AABBdata;
        }
        return bodyInfo;
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

POA_OpenHRP::BodyInfo* ModelLoader_impl::loadBodyInfoFromModelFile(const string url, const OpenHRP::ModelLoader::ModelLoadOption option)
{
    cout << "loading " << url << endl;
    POA_OpenHRP::BodyInfo* bodyInfo;
    string resolved_url = url;
    if(resolved_url.find("$(CURRENT_DIR)") != string::npos){
      if(resolved_url.find_last_of("/") != string::npos){
        resolved_url.replace(resolved_url.find("$(CURRENT_DIR)"),string("$(CURRENT_DIR)").length(), 
                     resolved_url.substr(0, resolved_url.find_last_of("/")));
      }else{
        resolved_url.replace(resolved_url.find("$(CURRENT_DIR)"),string("$(CURRENT_DIR)").length() + 1, ""); 
      }
      if(resolved_url[0] != '/'){
        char buf[1024];
        resolved_url = string(getcwd(buf, 1024))+"/"+resolved_url;
      }
    }
    if(resolved_url.find("$(PROJECT_DIR)") != string::npos){
      string shdir = OPENHRP_SHARE_DIR;
      string pjdir = shdir + "/sample/project";
      resolved_url.replace(resolved_url.find("$(PROJECT_DIR)"),string("$(PROJECT_DIR)").length(), pjdir);
    }

    try {
#ifdef OPENHRP_COLLADA_FOUND
        if( IsColladaFile(resolved_url) ) {
            BodyInfoCollada_impl* p = new BodyInfoCollada_impl(poa);
            p->setParam("readImage", option.readImage);
            p->loadModelFile(resolved_url);
            bodyInfo = p;
        }
        else
#endif
        {
            BodyInfo_impl* p = new BodyInfo_impl(poa);
            p->setParam("readImage", option.readImage);
            p->loadModelFile(resolved_url);
            bodyInfo = p;
        }
    }
    catch(OpenHRP::ModelLoader::ModelLoaderException& ex){
        cout << "loading failed.\n";
        cout << ex.description << endl;
        //bodyInfo->_remove_ref();
        throw;
    }
        
    cout << "The model was successfully loaded ! " << endl;
    
#ifdef OPENHRP_COLLADA_FOUND
//    cout << "Saving COLLADA file to hiro.dae" << endl;
//    ColladaWriter colladawriter;
//    colladawriter.Write(bodyInfo);
//    colladawriter.Save("/home/jsk/rdiankov/hiro.dae");
#endif

    //poa->activate_object(bodyInfo);
    urlToBodyInfoMap[resolved_url] = bodyInfo;

    string filename(deleteURLScheme(resolved_url));
    struct stat statbuff;
    time_t mtime = 0;

    // get a file modification time
    if( stat( filename.c_str(), &statbuff ) == 0 ){
        mtime = statbuff.st_mtime;
    }
    setLastUpdateTime(bodyInfo, mtime );

    return bodyInfo;
}



SceneInfo_ptr ModelLoader_impl::loadSceneInfo(const char* url)
    throw (CORBA::SystemException, OpenHRP::ModelLoader::ModelLoaderException)
{
    cout << "loading " << url << endl;

    SceneInfo_impl* sceneInfo;
    try {
#ifdef OPENHRP_COLLADA_FOUND
        if( IsColladaFile(url) ) {
            SceneInfoCollada_impl* p = new SceneInfoCollada_impl(poa);
	    p->load(url);
	    sceneInfo = p;
	}
	else
#endif
	{
	    SceneInfo_impl* p = new SceneInfo_impl(poa);
	    p->load(url);
	    sceneInfo = p;
	}
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
