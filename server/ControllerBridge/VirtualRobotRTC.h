// -*- C++ -*-
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

namespace OpenHRP {

  namespace ControllerBridge {

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

      void writeDataToOutPorts();
      void readDataFromInPorts(Controller_impl* controller);

      virtual RTC::ReturnCode_t onExecute(RTC::UniqueId ex_id);

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

      void addConnectedRtcs(RTC::Port_ptr portRef, RTC::RTCList& rtcList, std::set<std::string>& foundRtcNames);
    };

  }
}


#endif
