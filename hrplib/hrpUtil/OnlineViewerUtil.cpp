/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
#include "OnlineViewerUtil.h"

//using namespace hrp;
using namespace OpenHRP;

OnlineViewer_var hrp::getOnlineViewer(CosNaming::NamingContext_var cxt)
{  
    CosNaming::Name ncName;
    ncName.length(1);
    ncName[0].id = CORBA::string_dup("OnlineViewer");
    ncName[0].kind = CORBA::string_dup("");
    OnlineViewer_var onlineViewer = NULL;
    try {
        onlineViewer = OnlineViewer::_narrow(cxt->resolve(ncName));
    } catch(const CosNaming::NamingContext::NotFound &exc) {
        std::cerr << "OnlineViewer not found: ";
        switch(exc.why) {
        case CosNaming::NamingContext::missing_node:
            std::cerr << "Missing Node" << std::endl;
        case CosNaming::NamingContext::not_context:
            std::cerr << "Not Context" << std::endl;
            break;
        case CosNaming::NamingContext::not_object:
            std::cerr << "Not Object" << std::endl;
            break;
        }
        return 0;
    } catch(CosNaming::NamingContext::CannotProceed &exc) {
        std::cerr << "Resolve OnlineViewer CannotProceed" << std::endl;
        return 0;
    } catch(CosNaming::NamingContext::AlreadyBound &exc) {
        std::cerr << "Resolve OnlineViewer InvalidName" << std::endl;
        return 0;
    }
    return onlineViewer;
}

OnlineViewer_var hrp::getOnlineViewer(CORBA_ORB_var orb)
{
    CosNaming::NamingContext_var cxt;
    try {
        CORBA::Object_var nS = orb->resolve_initial_references("NameService");
        cxt = CosNaming::NamingContext::_narrow(nS);
    } catch(CORBA::SystemException& ex) {
        std::cerr << "NameService doesn't exist" << std::endl;
        return 0;
    }

    return getOnlineViewer(cxt);
}

OnlineViewer_var hrp::getOnlineViewer(int argc, char **argv)
{
    CORBA_ORB_var orb = CORBA::ORB_init(argc, argv);
    return getOnlineViewer(orb);
}
