// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-
/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */
/** \file
    \brief The implementation for the BodyCustomizer class
    \author S.NAKAOKA
*/

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


#include "BodyCustomizerInterface.h"

using namespace hrp;
using namespace std;
using namespace boost;

namespace {

#ifdef _WIN32
# include <windows.h>
	const char* DLLSFX =	".dll";
	const char* PATH_DELIMITER =	";";
	typedef HINSTANCE DllHandle;
	inline DllHandle loadDll(const char* filename) { return LoadLibrary(filename); }
	inline void* resolveDllSymbol(DllHandle handle, const char* symbol) { return GetProcAddress(handle, symbol); }
	inline void unloadDll(DllHandle handle) { FreeLibrary(handle); }
#else
# include <dlfcn.h>
	const char* DLLSFX = ".so";
	const char* PATH_DELIMITER =	":";
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
	&& customizerInterface->destroy
	&& customizerInterface->initializeAnalyticIk
	&& customizerInterface->calcAnalyticIk
	&& customizerInterface->setVirtualJointForces;

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
					cout << "Body customizer \"" << filename << "\" is incomatible and cannot be loaded.";
				} else {
					cout << "Loading \"" << filename << "\" for ";
					
					const char** names = customizerInterface->getTargetModelNames();
					
					for(int i=0; names[i]; ++i){
						if(i > 0){
							cout << ", ";
						}
						string name(names[i]);
						if(!name.empty()){
							customizerRepository[name] = customizerInterface;
						}
						cout << names[i];
					}
					cout << endl;
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


/**
   The function loads the customizers in the directories specified
   by the environmental variable LIBHRPMODEL_PLUGINS_PATH.
*/
int hrp::loadBodyCustomizersInDefaultDirectories(BodyInterface* bodyInterface)
{
    int numLoaded = 0;

    if(!pluginLoadingFunctionsCalled){

		pluginLoadingFunctionsCalled = true;

		char* pathListEnv = getenv("LIBHRPMODEL_PLUGIN_PATH");

		if(pathListEnv){
			char_separator<char> sep(PATH_DELIMITER);
			string pathList(pathListEnv);
			tokenizer< char_separator<char> > paths(pathList, sep);
			tokenizer< char_separator<char> >::iterator p;
			for(p = paths.begin(); p != paths.end(); ++p){
				numLoaded = loadBodyCustomizers(*p, bodyInterface);
			}
		}
    }

    return numLoaded;
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
