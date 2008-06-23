#ifndef ONLINEVIEWERCLIENT_H
#define ONLINEVIEWERCLIENT_H

#include "ORBwrap.h"
#include "OnlineViewer.h"
#include <iostream>
#include <stdlib.h>
#include <cstdio>

// for Windows DLL export 
#if defined(WIN32) || defined(_WIN32) || defined(__WIN32__) || defined(__NT__)
# ifdef _MAKE_DLL
#   define DllExport __declspec(dllexport)
# else 
#   define DllExport __declspec(dllimport)
# endif
#else 
# define DllExport 
#endif /* Windows */

class DllExport OnlineViewerClient {
public :
	OnlineViewerClient(std::string name="OnlineViewer", std::string nsHost="localhost", int nsPort = 2809); 
	bool init(int argc, char **argv);
	bool init(char **argv, CORBA::ORB_ptr orb);
	void load(const char* name, const char* url);
	void update(const OpenHRP::WorldState& state);
	void clearLog();
private:
	CosNaming::NamingContext_var OpenNamingContext(char **argv, CORBA::ORB_ptr orb);
	OpenHRP::OnlineViewer_var GetOnlineViewer(CORBA::ORB_ptr orb, CosNaming::NamingContext_var cxt);
	std::string nsHost_;
	int nsPort_;
	CosNaming::NamingContext_var cxt;
	OpenHRP::OnlineViewer_var olv;
};
#endif
