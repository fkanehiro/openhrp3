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
/**
   \file
   \author Shin'ichiro Nakaoka
   \author Kei Okada
*/

#include <iostream>
#include <rtm/Manager.h>

#include "BridgeConf.h"
#include "Controller_impl.h"
#include "VirtualRobotRTC.h"


#if ( defined ( WIN32 ) || defined ( _WIN32 ) || defined(__WIN32__) ) 
#  if( defined (OPENRTM_VERSION_042) )
#include <ace/OS_main.h> 
#  endif
#endif


using namespace std;

namespace {


	const bool CONTROLLER_BRIDGE_DEBUG = false;

	
	CosNaming::NamingContext_ptr getNamingContext(CORBA::ORB_ptr orb, const char* nameServerIdentifier)
	{
		CosNaming::NamingContext_ptr namingContext = CosNaming::NamingContext::_nil();

		CORBA::Object_var nameServer;

		try {
			nameServer = orb->string_to_object(nameServerIdentifier);
		}
		catch (const CORBA::ORB::InvalidName&) {
			cerr << "`NameService' cannot be resolved" << endl;
		}

		if(CORBA::is_nil(nameServer)){
			cerr << "`NameService' is a nil object reference" << endl;
		} else {
			try {
				namingContext = CosNaming::NamingContext::_narrow(nameServer);
			}
			catch(...){
				cerr << "`NameService' is not a NamingContext object reference" << endl;
			}
		}

		return namingContext;
	}


	bool setup(RTC::Manager* rtcManager, BridgeConf* bridgeConf)
	{
		CosNaming::NamingContext_var namingContext =
			getNamingContext(rtcManager->getORB(), bridgeConf->getOpenHRPNameServerIdentifier());

		if(CORBA::is_nil(namingContext)){
			return false;
		}
        Controller_impl* controllerServant = new Controller_impl( rtcManager, bridgeConf );

        CORBA::Object_var controller = controllerServant->_this();

        CosNaming::Name controllerName;
        controllerName.length(1);
        controllerName[0].id = CORBA::string_dup(bridgeConf->getControllerName());
        controllerName[0].kind = CORBA::string_dup("");

        namingContext->rebind(controllerName, controller);

        bridgeConf->setupModules();

		return true;
	}
}


int main(int argc, char* argv[])
{
	int ret = 0;
	RTC::Manager* rtcManager;

    try {
        unsigned int i;
        int rtc_argc = 1;
        char** rtc_argv = (char **)malloc(sizeof(char *)*argc);
        rtc_argv[0] = argv[0];
        for (i=1; i<argc; i++){
            if (strncmp(argv[i], "--", 2)!=0 ) {
                rtc_argv[rtc_argc] = argv[i];
                rtc_argc++;
            }else {
                i++;
            } 
        }
        rtcManager = RTC::Manager::init(rtc_argc, rtc_argv);
        rtcManager->activateManager();
	}
	catch(...) {
		cerr << "Cannot initialize OpenRTM" << endl;
		exit(1);
	}

	BridgeConf* bridgeConf;

	try {
		bridgeConf = BridgeConf::initialize(argc, argv);
	} catch (std::exception& ex) {
		cerr << argv[0] << ": " << ex.what() << endl;
		exit(1);
	}


	if(bridgeConf->isReady()){
		if(setup(rtcManager, bridgeConf)){
			cout << "ready" << endl;
			rtcManager->runManager();
		} else {
			ret = 1;
		}
	}
	return ret;
}
