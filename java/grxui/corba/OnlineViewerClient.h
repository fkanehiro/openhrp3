/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
#ifndef ONLINEVIEWERCLIENT_H
#define ONLINEVIEWERCLIENT_H

#include "ORBwrap.h"
#ifdef OPENHRP_VER
#include <hrpCorba/OnlineViewer.hh>
#else
#include "OnlineViewer.h"
#endif
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
