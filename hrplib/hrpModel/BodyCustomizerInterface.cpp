/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */
/**
   \file
   \brief The implementation for the BodyCustomizer class
   \author Shin'ichiro Nakaoka
*/

#include "BodyCustomizerInterface.h"
#include "Body.h"
#include <cstdlib>
#include <map>
#include <boost/version.hpp>
#include <boost/regex.hpp>
#include <boost/tokenizer.hpp>

#if (BOOST_VERSION <= 103301)
#include <boost/filesystem/path.hpp>
#include <boost/filesystem/operations.hpp>
#else
#include <boost/filesystem.hpp>
#endif

using namespace hrp;
using namespace std;
using namespace boost;

namespace {

#ifdef _WIN32
# include <windows.h>
    const char* DLLSFX = ".dll";
    const char* PATH_DELIMITER = ";";
    typedef HINSTANCE DllHandle;
    inline DllHandle loadDll(const char* filename) { return LoadLibrary(filename); }
    inline void* resolveDllSymbol(DllHandle handle, const char* symbol) { return GetProcAddress(handle, symbol); }
    inline void unloadDll(DllHandle handle) { FreeLibrary(handle); }
#else
# include <dlfcn.h>
#ifdef __darwin__
    const char* DLLSFX = ".dylib";
#else
    const char* DLLSFX = ".so";
#endif
    const char* PATH_DELIMITER = ":";
    typedef void* DllHandle;
    inline DllHandle loadDll(const char* filename) { return dlopen(filename, RTLD_LAZY); }
    inline void* resolveDllSymbol(DllHandle handle, const char* symbol) { return dlsym(handle, symbol); }
    inline void unloadDll(DllHandle handle) { dlclose(handle); }
#endif

    typedef std::map<std::string, BodyCustomizerInterface*> NameToInterfaceMap;
    NameToInterfaceMap customizerRepository;
    bool pluginLoadingFunctionsCalled = false;

    inline string toNativePathString(const filesystem::path& path) {
#if (BOOST_VERSION <= 103301)
        return path.native_file_string();
#else
        return path.file_string();
#endif
    }

}


static bool checkInterface(BodyCustomizerInterface* customizerInterface)
{
    bool qualified = true
	&& (customizerInterface->version == BODY_CUSTOMIZER_INTERFACE_VERSION)
	&& customizerInterface->getTargetModelNames
	&& customizerInterface->create
	&& customizerInterface->destroy;
	//&& customizerInterface->initializeAnalyticIk
	//&& customizerInterface->calcAnalyticIk
	//&& customizerInterface->setVirtualJointForces;

    return qualified;
}


static bool loadCustomizerDll(BodyInterface* bodyInterface, const std::string filename)
{
    BodyCustomizerInterface* customizerInterface = 0;

    DllHandle dll = loadDll(filename.c_str());
	
    if(dll){
		
        GetBodyCustomizerInterfaceFunc getCustomizerInterface =
            (GetBodyCustomizerInterfaceFunc)resolveDllSymbol(dll, "getHrpBodyCustomizerInterface");
		
        if(!getCustomizerInterface){
            unloadDll(dll);
        } else {
            customizerInterface = getCustomizerInterface(bodyInterface);
			
            if(customizerInterface){
				
                if(!checkInterface(customizerInterface)){
                    cerr << "Body customizer \"" << filename << "\" is incomatible and cannot be loaded.";
                } else {
                    cerr << "Loading body customizer \"" << filename << "\" for ";
					
                    const char** names = customizerInterface->getTargetModelNames();
					
                    for(int i=0; names[i]; ++i){
                        if(i > 0){
                            cerr << ", ";
                        }
                        string name(names[i]);
                        if(!name.empty()){
                            customizerRepository[name] = customizerInterface;
                        }
                        cerr << names[i];
                    }
                    cerr << endl;
                }
            }
        }
    }
	
    return (customizerInterface != 0);
}


/**
   DLLs of body customizer in the path are loaded and
   they are registered to the customizer repository.

   The loaded customizers can be obtained by using
   hrp::findBodyCustomizer() function.

   \param pathString the path to a DLL file or a directory that contains DLLs
   \param bodyInterface
*/
int hrp::loadBodyCustomizers(const std::string pathString, BodyInterface* bodyInterface)
{
    pluginLoadingFunctionsCalled = true;
	
    int numLoaded = 0;

    filesystem::path pluginPath(pathString, filesystem::native);
	
    if(filesystem::exists(pluginPath)){

        if(!filesystem::is_directory(pluginPath)){
            if(loadCustomizerDll(bodyInterface, toNativePathString(pluginPath))){
                numLoaded++;
            }
        } else {
            regex pluginNamePattern(string(".+Customizer") + DLLSFX);
            filesystem::directory_iterator end;
			
            for(filesystem::directory_iterator it(pluginPath); it != end; ++it){
                const filesystem::path& filepath = *it;
                if(!filesystem::is_directory(filepath)){
                    if(regex_match(filepath.leaf(), pluginNamePattern)){
                        if(loadCustomizerDll(bodyInterface, toNativePathString(filepath))){
                            numLoaded++;
                        }
                    }
                }
            }
        }
    }

    return numLoaded;
}


int hrp::loadBodyCustomizers(const std::string pathString)
{
    return loadBodyCustomizers(pathString, Body::bodyInterface());
}


/**
   The function loads the customizers in the directories specified
   by the environmental variable HRPMODEL_CUSTOMIZER_PATH
*/
int hrp::loadBodyCustomizers(BodyInterface* bodyInterface)
{
    int numLoaded = 0;

    if(!pluginLoadingFunctionsCalled){

        pluginLoadingFunctionsCalled = true;

        char* pathListEnv = getenv("HRPMODEL_CUSTOMIZER_PATH");

        if(pathListEnv){
            char_separator<char> sep(PATH_DELIMITER);
            string pathList(pathListEnv);
            tokenizer< char_separator<char> > paths(pathList, sep);
            tokenizer< char_separator<char> >::iterator p;
            for(p = paths.begin(); p != paths.end(); ++p){
                numLoaded = loadBodyCustomizers(*p, bodyInterface);
            }
        }

#ifndef _WIN32
        Dl_info info;
        if(dladdr((void*)&hrp::findBodyCustomizer, &info)){
            filesystem::path customizerPath =
                filesystem::path(info.dli_fname).branch_path().branch_path() / OPENHRP_RELATIVE_SHARE_DIR / "customizer";
            numLoaded += loadBodyCustomizers(customizerPath.string(), bodyInterface);
        }
#else
        string customizerPath(OPENHRP_SHARE_DIR);
        customizerPath.append("/customizer");
        numLoaded += loadBodyCustomizers(customizerPath, bodyInterface);
#endif

    }

    return numLoaded;
}


int hrp::loadBodyCustomizers()
{
    return loadBodyCustomizers(Body::bodyInterface());
}


BodyCustomizerInterface* hrp::findBodyCustomizer(std::string modelName)
{
    BodyCustomizerInterface* customizerInterface = 0;

    NameToInterfaceMap::iterator p = customizerRepository.find(modelName);
    if(p != customizerRepository.end()){
	customizerInterface = p->second;
    }

    return customizerInterface;
}
