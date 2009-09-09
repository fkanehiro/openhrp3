// -*- C++ -*-
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
*/

#ifndef OPENHRP_CONTROLLER_BRIDGE_VIRTUAL_ROBOT_RTC_H_INCLUDED
#define OPENHRP_CONTROLLER_BRIDGE_VIRTUAL_ROBOT_RTC_H_INCLUDED

#include <set>
#include <string>
#include <rtm/Manager.h>
#include <rtm/DataFlowComponentBase.h>

#include "VirtualRobotPortHandler.h"

class Controller_impl;

class VirtualRobotRTC : public RTC::DataFlowComponentBase
{
public:

    static void registerFactory(RTC::Manager* manager, const char* componentTypeName);

    VirtualRobotRTC(RTC::Manager* manager);
    ~VirtualRobotRTC();

    PortHandlerPtr getPortHandler(const std::string& name);

    RTC::RTCList* getConnectedRtcs();

    void inputDataFromSimulator(Controller_impl* controller);
    void outputDataToSimulator(Controller_impl* controller);

    void writeDataToOutPorts(Controller_impl* controller);
    void readDataFromInPorts(Controller_impl* controller);
    void stop();
    bool isOwnedByController;

private:

    typedef std::map<std::string, OutPortHandlerPtr> OutPortHandlerMap;
    OutPortHandlerMap outPortHandlers;

    typedef std::map<std::string, InPortHandlerPtr> InPortHandlerMap;
    InPortHandlerMap inPortHandlers;

    void createOutPortHandler(PortInfo& portInfo);
    void createInPortHandler(PortInfo& portInfo);

    template <class TOutPortHandler>
    void registerOutPortHandler(TOutPortHandler* handler) {
	const char* name = handler->outPort.name();
	if(!getPortHandler(name)){
            registerOutPort(name, handler->outPort);
            outPortHandlers.insert(std::make_pair(name, OutPortHandlerPtr(handler)));
	}
    }

    template <class TInPortHandler>
    void registerInPortHandler(TInPortHandler* handler) {
	const char* name = handler->inPort.name();
	if(!getPortHandler(name)){
            registerInPort(name, handler->inPort);
            inPortHandlers.insert(std::make_pair(name, InPortHandlerPtr(handler)));
	}
    }

    void updatePortObjectRefs();

    void addConnectedRtcs(Port_Service_Ptr_Type portRef, RTC::RTCList& rtcList, std::set<std::string>& foundRtcNames);
};


#endif
