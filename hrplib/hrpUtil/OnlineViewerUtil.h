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

#pragma warning(disable:4996)

#include "config.h"
#include <string>
#include <sstream>
#include <iostream>

#include <hrpCorba/ORBwrap.h>
#include <hrpCorba/OpenHRPCommon.hh>
#include <hrpCorba/OnlineViewer.hh>

namespace hrp
{
    HRP_UTIL_EXPORT OpenHRP::OnlineViewer_var getOnlineViewer(int argc, char **argv);
    HRP_UTIL_EXPORT OpenHRP::OnlineViewer_var getOnlineViewer(CORBA_ORB_var orb);
};

#endif
