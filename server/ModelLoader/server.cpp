
#include "ModelLoader_impl.h"

#ifdef _WIN32
#include "winbase.h"
#else
#include <unistd.h>
#endif /* _WIN32 */

#include <iostream>

using namespace std;
using namespace OpenHRP;


int main(int argc, char* argv[])
{
    
    CORBA::ORB_var orb = CORBA::ORB::_nil();
  
    try {

	orb = CORBA::ORB_init(argc, argv);
	
	CORBA::Object_var obj;
	
	obj = orb->resolve_initial_references("RootPOA");
	PortableServer::POA_var poa = PortableServer::POA::_narrow(obj);
	if(CORBA::is_nil(poa)){
	    throw string("error: failed to narrow root POA.");
	}
	
	PortableServer::POAManager_var poaManager = poa->the_POAManager();
	if(CORBA::is_nil(poaManager)){
	    throw string("error: failed to narrow root POA manager.");
	}
	
	ModelLoader_impl* modelLoaderImpl = new ModelLoader_impl(orb, poa);
	poa->activate_object(modelLoaderImpl);
	ModelLoader_var modelLoader = modelLoaderImpl->_this();
	modelLoaderImpl->_remove_ref();

	obj = orb->resolve_initial_references("NameService");
	CosNaming::NamingContext_var namingContext = CosNaming::NamingContext::_narrow(obj);
	if(CORBA::is_nil(namingContext)){
	    throw string("error: failed to narrow naming context.");
	}
	
	CosNaming::Name name;
	name.length(1);
	name[0].id = CORBA::string_dup("ModelLoader");
	name[0].kind = CORBA::string_dup("");
	namingContext->rebind(name, modelLoader);

	poaManager->activate();
	
	cout << "ready" << endl;

	orb->run();

    }
    catch (CORBA::SystemException& ex) {
	cerr << ex._rep_id() << endl;
    }
    catch (const string& error){
	cerr << error << endl;
    }

    try {
	orb->destroy();
    }
    catch(...){

    }
    
    return 0;
}
