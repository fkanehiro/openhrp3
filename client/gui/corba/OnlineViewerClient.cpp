#include "OnlineViewerClient.h"

using namespace std;
using namespace OpenHRP;

OnlineViewerClient::OnlineViewerClient(string name, string nsHost, int nsPort) {
	nsHost_ = nsHost;
	nsPort_ = nsPort;
}

CosNaming::NamingContext_var
OnlineViewerClient::OpenNamingContext(char **argv, CORBA::ORB_ptr orb)
{
	// Getting reference of name server
	CORBA::Object_var ns;
	try {
		ns = orb -> resolve_initial_references("NameService");
    } catch (const CORBA::ORB::InvalidName&) {
        cerr << argv[0] << ": can't resolve `NameService'" << endl;
    	exit(1);
    }
	if(CORBA::is_nil(ns)) {
		cerr << argv[0]
             << ": `NameService' is a nil object reference"
             << endl;
    	exit(1);
    }

    // Getting root naming context
    cxt = CosNaming::NamingContext::_narrow(ns);
    if(CORBA::is_nil(cxt)) {
		cerr << argv[0]
			 << ": `NameService' is not a NamingContext object reference"
			 << endl;
		exit(1);
	}
	CosNaming::Name ncName;
	ncName.length(1);
    return cxt;
}

OnlineViewer_var
OnlineViewerClient::GetOnlineViewer(CORBA::ORB_ptr orb, CosNaming::NamingContext_var cxt)
{
	CosNaming::Name ncFactory;
	ncFactory.length(1);
	ncFactory[0].id = CORBA::string_dup("OnlineViewer");
	ncFactory[0].kind = CORBA::string_dup("");
	try {
		olv = OnlineViewer::_narrow(cxt -> resolve(ncFactory));
        olv->clearLog();
	}catch(const CosNaming::NamingContext::NotFound&){
		cerr << "OnlineViewer not found" << endl;
		return 0;
	}
    return olv;
}

bool OnlineViewerClient::init(int argc, char **argv)
{
    CORBA::ORB_var orb = CORBA::ORB_init(argc, argv);
    return init(argv, orb);
}

bool OnlineViewerClient::init(char **argv, CORBA::ORB_ptr orb) 
{
	cxt = OpenNamingContext(argv, orb);
	olv = GetOnlineViewer(orb, cxt);
	return true;
}

void OnlineViewerClient::load(const char* name, const char* url)
{
	olv->load(name, url);
}

void OnlineViewerClient::update(const WorldState& state)
{
	olv->update(state);
}

void OnlineViewerClient::clearLog()
{
	olv->clearLog();
}
