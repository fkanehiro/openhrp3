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

    RTC::ReturnCode_t onInitialize();

    PortHandlerPtr getPortHandler(const std::string& name);

    RTC::RTCList* getConnectedRtcs();

    void inputDataFromSimulator(Controller_impl* controller);
    void outputDataToSimulator(Controller_impl* controller);

    void writeDataToOutPorts(Controller_impl* controller);
    void readDataFromInPorts(Controller_impl* controller);
    void stop();
    bool checkOutPortStepTime(double controlTimeStep);
    bool isOwnedByController;

private:

    typedef std::map<std::string, OutPortHandlerPtr> OutPortHandlerMap;
    OutPortHandlerMap outPortHandlers;

    typedef std::map<std::string, InPortHandlerPtr> InPortHandlerMap;
    InPortHandlerMap inPortHandlers;


#ifdef OPENRTM_VERSION_042
    void createOutPortHandler(PortInfo& portInfo);
    void createInPortHandler(PortInfo& portInfo);
    template <class TOutPortHandler>
    void registerOutPortHandler(TOutPortHandler* handler) {
        const std::string& name = handler->portName;
        if(!getPortHandler(name)){
            registerOutPort(name.c_str(), handler->outPort);
            outPortHandlers.insert(std::make_pair(name, OutPortHandlerPtr(handler)));
        }
    }
    template <class TInPortHandler>
    void registerInPortHandler(TInPortHandler* handler) {
        const std::string& name = handler->portName;
        if(!getPortHandler(name)){
            registerInPort(name.c_str(), handler->inPort);
            inPortHandlers.insert(std::make_pair(name, InPortHandlerPtr(handler)));
        }
    }
#else
    bool createOutPortHandler(PortInfo& portInfo);
    bool createInPortHandler(PortInfo& portInfo);
    template <class TOutPortHandler>
    bool registerOutPortHandler(TOutPortHandler* handler) {
        const std::string& name = handler->portName;
        if(!getPortHandler(name)){
            if (!addOutPort(name.c_str(), handler->outPort)) return false;
            outPortHandlers.insert(std::make_pair(name, OutPortHandlerPtr(handler)));
        }
        return true;
    }

    template <class TInPortHandler>
    bool registerInPortHandler(TInPortHandler* handler) {
        const std::string& name = handler->portName;
        if(!getPortHandler(name)){
            if (!addInPort(name.c_str(), handler->inPort)) return false;
            inPortHandlers.insert(std::make_pair(name, InPortHandlerPtr(handler)));
        }
        return true;
    }
#endif
    void updatePortObjectRefs();

    void addConnectedRtcs(Port_Service_Ptr_Type portRef, RTC::RTCList& rtcList, std::set<std::string>& foundRtcNames);
};


#endif
