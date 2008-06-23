// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-
/**
   \file
   \author Shin'ichiro Nakaoka
*/

#include <iostream>
#include <rtm/Manager.h>

#include "BridgeConf.h"
#include "Controller_impl.h"
#include "VirtualRobotRTC.h"


#if ( defined ( WIN32 ) || defined ( _WIN32 ) || defined(__WIN32__) ) && defined ( USE_stub_in_nt_dll )
#include <ace/OS_main.h> 
#endif


using namespace std;
using namespace OpenHRP;
using namespace OpenHRP::ControllerBridge;

namespace RTC {

  class OpenHRPExecutionContext : public virtual PeriodicExecutionContext
  {
  public:
    OpenHRPExecutionContext() : PeriodicExecutionContext() { }
    virtual ~OpenHRPExecutionContext() { }
    virtual void tick() throw (CORBA::SystemException)
    { 
      std::for_each(m_comps.begin(), m_comps.end(), invoke_worker()); 
    }
    virtual int svc(void) { return 0; }
    
    static void init(RTC::Manager* manager)
    {
      manager->registerECFactory("OpenHRPExecutionContext",
				 ECCreate<OpenHRPExecutionContext>,
				 ECDelete<OpenHRPExecutionContext>);
    }
  };
}


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

		ControllerFactory_impl* controllerFactoryServant = new ControllerFactory_impl(rtcManager, bridgeConf);
		CORBA::Object_var controllerFactory = controllerFactoryServant->_this();

		CosNaming::Name controllerFactoryName;
		controllerFactoryName.length(1);
		controllerFactoryName[0].id = CORBA::string_dup(bridgeConf->getControllerFactoryName());
		controllerFactoryName[0].kind = CORBA::string_dup("");

		namingContext->rebind(controllerFactoryName, controllerFactory);

		bridgeConf->setupModules();
    
		return true;
	}
  
}


int main(int argc, char* argv[])
{
	RTC::Manager* rtcManager;

	try {
		rtcManager = RTC::Manager::init(0, 0);

		RTC::OpenHRPExecutionContext::init(rtcManager);

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

	bool ret = 0;

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
