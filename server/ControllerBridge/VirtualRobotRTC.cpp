// -*- mode: c++; c-basic-offset: 2; -*-
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

#include"VirtualRobotRTC.h"
#include "Controller_impl.h"

#include <iostream>
#include <boost/bind.hpp>

using namespace std;
using namespace boost;

namespace {
  const bool CONTROLLER_BRIDGE_DEBUG = false;
}


void VirtualRobotRTC::registerFactory(RTC::Manager* manager, const char* componentTypeName)
{
  static const char* spec[] = {
    "implementation_id", "VirtualRobot",
    "type_name",         "VirtualRobot",
    "description",       "This component enables controller components to"
    "access the I/O of a virtual robot in a OpenHRP simulation",
    "version",           "1.0",
    "vendor",            "AIST",
    "category",          "OpenHRP",
    "activity_type",     "DataFlowComponent",
    "max_instance",      "1",
    "language",          "C++",
    "lang_type",         "compile",
    ""
  };
	
  if(CONTROLLER_BRIDGE_DEBUG){
    cout << "initVirtualRobotRTC()" << endl;
  }
	
  RTC::Properties profile(spec);

  profile.setDefault("implementation_id", componentTypeName);
  profile.setDefault("type_name", componentTypeName);
	
  manager->registerFactory(profile,
			   RTC::Create<VirtualRobotRTC>,
			   RTC::Delete<VirtualRobotRTC>);
}


VirtualRobotRTC::VirtualRobotRTC(RTC::Manager* manager)
  : RTC::DataFlowComponentBase(manager)
{
  if(CONTROLLER_BRIDGE_DEBUG){
    cout << "VirtualRobotRTC::VirtualRobotRTC" << endl;
  }

  isOwnedByController = false;

  BridgeConf* bridgeConf = BridgeConf::instance();

  PortInfoMap::iterator it;

  for(it = bridgeConf->outPortInfos.begin(); it != bridgeConf->outPortInfos.end(); ++it){
    createOutPortHandler(it->second);
  }

  for(it = bridgeConf->inPortInfos.begin(); it != bridgeConf->inPortInfos.end(); ++it){
    createInPortHandler(it->second);
  }

  updatePortObjectRefs();
}


VirtualRobotRTC::~VirtualRobotRTC()
{
  if(CONTROLLER_BRIDGE_DEBUG){
    cout << "VirtualRobotRTC::~VirtualRobotRTC" << endl;
  }
}


void VirtualRobotRTC::createOutPortHandler(PortInfo& portInfo)
{
  DataTypeId dataTypeId = portInfo.dataTypeId;

  if(portInfo.dataOwnerName.empty()){
    switch(dataTypeId) {

    case JOINT_VALUE:
    case JOINT_VELOCITY:
    case JOINT_ACCELERATION:
    case JOINT_TORQUE:
    case FORCE_SENSOR:
    case RATE_GYRO_SENSOR:
    case ACCELERATION_SENSOR:
      registerOutPortHandler(new SensorStateOutPortHandler(portInfo));
      break;

    case COLOR_IMAGE:
      registerOutPortHandler(new ColorImageOutPortHandler(portInfo));
      break;

    case GRAYSCALE_IMAGE:
      registerOutPortHandler(new GrayScaleImageOutPortHandler(portInfo));
      break;

    case DEPTH_IMAGE:
      registerOutPortHandler(new DepthImageOutPortHandler(portInfo));
      break;

    default:
      break;
    }
  } else {

    switch(dataTypeId) {

    case JOINT_VALUE:
    case JOINT_VELOCITY:
    case JOINT_ACCELERATION:
    case JOINT_TORQUE:
    case EXTERNAL_FORCE:
    case ABS_TRANSFORM:
    case ABS_VELOCITY:
    case ABS_ACCELERATION:
    case CONSTRAINT_FORCE:
      registerOutPortHandler(new LinkDataOutPortHandler(portInfo));
      break;

    case FORCE_SENSOR:
    case RATE_GYRO_SENSOR:
    case ACCELERATION_SENSOR:
    case RANGE_SENSOR:
      registerOutPortHandler(new SensorDataOutPortHandler(portInfo));
      break;

    default:
      break;
    }
  }
}


void VirtualRobotRTC::createInPortHandler(PortInfo& portInfo)
{
  DataTypeId dataTypeId = portInfo.dataTypeId;

  if(portInfo.dataOwnerName.empty()){
    switch(dataTypeId) {
    case JOINT_VALUE:
    case JOINT_VELOCITY:
    case JOINT_ACCELERATION:
    case JOINT_TORQUE:
      registerInPortHandler(new JointDataSeqInPortHandler(portInfo));
      break;
    default:
      break;
    }
  }else{
    switch(dataTypeId){
    case JOINT_VALUE:
    case JOINT_VELOCITY:
    case JOINT_ACCELERATION:
    case JOINT_TORQUE:
      registerInPortHandler(new LinkDataInPortHandler(portInfo));
      break;
    case ABS_TRANSFORM:
    case ABS_VELOCITY:
    case ABS_ACCELERATION:
      std::cout << "createInPortHandler()" << std::endl;
      registerInPortHandler(new LinkDataInPortHandler(portInfo));
      break;
    default:
      break;
    }
  }
}


PortHandlerPtr VirtualRobotRTC::getPortHandler(const std::string& name)
{
  PortHandlerPtr portHandler;

  OutPortHandlerMap::iterator p = outPortHandlers.find(name);
  if(p != outPortHandlers.end()){
    portHandler = p->second;
  } else {
    InPortHandlerMap::iterator q = inPortHandlers.find(name);
    if(q != inPortHandlers.end()){
      portHandler = q->second;
    }
  }

  return portHandler;
}


void VirtualRobotRTC::updatePortObjectRefs()
{
  for(OutPortHandlerMap::iterator it = outPortHandlers.begin(); it != outPortHandlers.end(); ++it){
    OutPortHandlerPtr& handler = it->second;
    handler->portRef = RTC::Port::_nil();
  }
  for(InPortHandlerMap::iterator it = inPortHandlers.begin(); it != inPortHandlers.end(); ++it){
    InPortHandlerPtr& handler = it->second;
    handler->portRef = RTC::Port::_nil();
  }
	
  RTC::PortList_var ports = get_ports();

  for(CORBA::ULong i=0; i < ports->length(); ++i){

    RTC::PortProfile_var profile = ports[i]->get_port_profile();
    PortHandlerPtr portHandler = getPortHandler(string(profile->name));

    if(portHandler){
      portHandler->portRef = ports[i];
    }
  }
}


RTC::RTCList* VirtualRobotRTC::getConnectedRtcs()
{
  RTC::RTCList* rtcList = new RTC::RTCList;

  set<string> foundRtcNames;
	
  for(OutPortHandlerMap::iterator it = outPortHandlers.begin(); it != outPortHandlers.end(); ++it){
    OutPortHandlerPtr& handler = it->second;
    addConnectedRtcs(handler->portRef, *rtcList, foundRtcNames);
  }
  for(InPortHandlerMap::iterator it = inPortHandlers.begin(); it != inPortHandlers.end(); ++it){
    InPortHandlerPtr& handler = it->second;
    addConnectedRtcs(handler->portRef, *rtcList, foundRtcNames);
  }

  return rtcList;
}


void VirtualRobotRTC::addConnectedRtcs(RTC::Port_ptr portRef, RTC::RTCList& rtcList, std::set<std::string>& foundRtcNames)
{
    RTC::PortProfile_var portProfile = portRef->get_port_profile();
    string portName(portProfile->name);

    RTC::ConnectorProfileList_var connectorProfiles = portRef->get_connector_profiles();

    for(CORBA::ULong i=0; i < connectorProfiles->length(); ++i){
        RTC::ConnectorProfile& connectorProfile = connectorProfiles[i];
        RTC::PortList& connectedPorts = connectorProfile.ports;

        for(CORBA::ULong j=0; j < connectedPorts.length(); ++j){
            RTC::Port_ptr connectedPortRef = connectedPorts[j];
            RTC::PortProfile_var connectedPortProfile = connectedPortRef->get_port_profile();
            RTC::RTObject_var connectedRtcRef = connectedPortProfile->owner;
            RTC::RTObject_var thisRef = getObjRef();

            if(!CORBA::is_nil(connectedRtcRef) && !connectedRtcRef->_is_equivalent(thisRef)){
                CORBA::ULong ii=0;
                for(; ii<rtcList.length(); ii++){
                    if(rtcList[ii]->_is_equivalent(connectedRtcRef))
                        break;
                }

                RTC::ComponentProfile_var componentProfile = connectedRtcRef->get_component_profile();
                string connectedRtcName(componentProfile->instance_name);

                cout << "detected a port connection: ";
                cout << "\"" << portName << "\" of " << getInstanceName() << " <--> \"";
                cout << connectedPortProfile->name << "\" of " << connectedRtcName << endl;

                if(ii == rtcList.length()){
                    RTC::ExecutionContextServiceList_var execServices = connectedRtcRef->get_execution_context_services();

                    for(CORBA::ULong k=0; k < execServices->length(); k++) {
                        RTC::ExecutionContextService_var execContext = execServices[k];

                        RTC::ExtTrigExecutionContextService_var extTrigExecContext =
                        RTC::ExtTrigExecutionContextService::_narrow(execContext);

                        if(!CORBA::is_nil(extTrigExecContext)){
                            CORBA::ULong n = rtcList.length();
                            rtcList.length(n + 1);
                            rtcList[n] = connectedRtcRef;
                            foundRtcNames.insert(connectedRtcName);
                        }
                    }
                }
            }
        }
    }
}


void VirtualRobotRTC::inputDataFromSimulator(Controller_impl* controller)
{
    double controlTime = controller->controlTime;
    double controlTimeStep = controller->getTimeStep();
    for(OutPortHandlerMap::iterator it = outPortHandlers.begin(); it != outPortHandlers.end(); ++it){
        double stepTime = it->second->stepTime;
        if(stepTime){
            double w = controlTime-(int)(controlTime/stepTime)*stepTime + controlTimeStep/2;
            if(w >= stepTime) w=0;
            if(w < controlTimeStep )
                it->second->inputDataFromSimulator(controller);
        }else{
            it->second->inputDataFromSimulator(controller);
        }
    }
}


void VirtualRobotRTC::outputDataToSimulator(Controller_impl* controller)
{
  for(InPortHandlerMap::iterator it = inPortHandlers.begin(); it != inPortHandlers.end(); ++it){
    it->second->outputDataToSimulator(controller);
  }
}


void VirtualRobotRTC::writeDataToOutPorts(Controller_impl* controller)
{
    double controlTime = controller->controlTime;
    double controlTimeStep = controller->getTimeStep();
    for(OutPortHandlerMap::iterator it = outPortHandlers.begin(); it != outPortHandlers.end(); ++it){
        double stepTime = it->second->stepTime;
        if(stepTime){
            double w = controlTime-(int)(controlTime/stepTime)*stepTime + controlTimeStep/2;
            if(w >= stepTime) w=0;
            if(w < controlTimeStep )
                it->second->writeDataToPort();
        }else{
                it->second->writeDataToPort();
        }
    }
}


void VirtualRobotRTC::readDataFromInPorts(Controller_impl* controller)
{
  for(InPortHandlerMap::iterator it = inPortHandlers.begin(); it != inPortHandlers.end(); ++it){
    it->second->readDataFromPort(controller);
  }
}

void VirtualRobotRTC::stop()
{
    
}
