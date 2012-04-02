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
}

RTC::ReturnCode_t VirtualRobotRTC::onInitialize()
{
  BridgeConf* bridgeConf = BridgeConf::instance();

  PortInfoMap::iterator it;

  for(it = bridgeConf->outPortInfos.begin(); it != bridgeConf->outPortInfos.end(); ++it){
#ifdef OPENRTM_VERSION_042
      createOutPortHandler(it->second);
#else
    if (!createOutPortHandler(it->second)){
      cerr << "createOutPortHandler(" << it->second.portName << ") failed" << std::endl;
    }
#endif
  }

  for(it = bridgeConf->inPortInfos.begin(); it != bridgeConf->inPortInfos.end(); ++it){
#ifdef OPENRTM_VERSION_042
      createInPortHandler(it->second);
#else
    if (!createInPortHandler(it->second)){
      cerr << "createInPortHandler(" << it->second.portName << ") failed" << std::endl;
    }
#endif
  }

  updatePortObjectRefs();
  return RTC::RTC_OK;
}


VirtualRobotRTC::~VirtualRobotRTC()
{
  if(CONTROLLER_BRIDGE_DEBUG){
    cout << "VirtualRobotRTC::~VirtualRobotRTC" << endl;
  }
}

#ifdef OPENRTM_VERSION_042

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
    case EXTERNAL_FORCE:
      std::cout << "createInPortHandler()" << std::endl;
      registerInPortHandler(new LinkDataInPortHandler(portInfo));
      break;
    default:
      break;
    }
  }
}

#else

bool VirtualRobotRTC::createOutPortHandler(PortInfo& portInfo)
{
  DataTypeId dataTypeId = portInfo.dataTypeId;
  bool ret = false;

  if(portInfo.dataOwnerName.empty()){
    switch(dataTypeId) {

    case JOINT_VALUE:
    case JOINT_VELOCITY:
    case JOINT_ACCELERATION:
    case JOINT_TORQUE:
    case FORCE_SENSOR:
    case RATE_GYRO_SENSOR:
    case ACCELERATION_SENSOR:
      ret = registerOutPortHandler(new SensorStateOutPortHandler(portInfo));
      break;

    case COLOR_IMAGE:
      ret = registerOutPortHandler(new ColorImageOutPortHandler(portInfo));
      break;

    case GRAYSCALE_IMAGE:
      ret = registerOutPortHandler(new GrayScaleImageOutPortHandler(portInfo));
      break;

    case DEPTH_IMAGE:
      ret = registerOutPortHandler(new DepthImageOutPortHandler(portInfo));
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
    case ABS_TRANSFORM:
    case ABS_VELOCITY:
    case ABS_ACCELERATION:
    case CONSTRAINT_FORCE:
      ret = registerOutPortHandler(new LinkDataOutPortHandler(portInfo));
      break;

    case FORCE_SENSOR:
    case RANGE_SENSOR:
      ret = registerOutPortHandler(new SensorDataOutPortHandler(portInfo));
      break;
    case RATE_GYRO_SENSOR:
      ret = registerOutPortHandler(new GyroSensorOutPortHandler(portInfo));
      break;
    case ACCELERATION_SENSOR:
      ret = registerOutPortHandler(new AccelerationSensorOutPortHandler(portInfo));
      break;

    default:
      break;
    }
  }
  return ret;
}


bool VirtualRobotRTC::createInPortHandler(PortInfo& portInfo)
{
  DataTypeId dataTypeId = portInfo.dataTypeId;
  bool ret = false;
  if(portInfo.dataOwnerName.empty()){
    switch(dataTypeId) {
    case JOINT_VALUE:
    case JOINT_VELOCITY:
    case JOINT_ACCELERATION:
    case JOINT_TORQUE:
      ret = registerInPortHandler(new JointDataSeqInPortHandler(portInfo));
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
      ret = registerInPortHandler(new LinkDataInPortHandler(portInfo));
      break;
    case ABS_TRANSFORM:
    case ABS_VELOCITY:
    case ABS_ACCELERATION:
    case EXTERNAL_FORCE:
      std::cout << "createInPortHandler()" << std::endl;
      ret = registerInPortHandler(new LinkDataInPortHandler(portInfo));
      break;
    default:
      break;
    }
  }
  return ret;
}
#endif

PortHandlerPtr VirtualRobotRTC::getPortHandler(const std::string& name_)
{
  string name(name_);
  string::size_type index = name.rfind(".");
  if (index != string::npos) name = name.substr(index+1);

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
    handler->portRef = Port_Service_Type::_nil();
  }
  for(InPortHandlerMap::iterator it = inPortHandlers.begin(); it != inPortHandlers.end(); ++it){
    InPortHandlerPtr& handler = it->second;
    handler->portRef = Port_Service_Type::_nil();
  }

  Port_Service_List_Var_Type ports = get_ports();

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


void VirtualRobotRTC::addConnectedRtcs(Port_Service_Ptr_Type portRef, RTC::RTCList& rtcList, std::set<std::string>& foundRtcNames)
{
    RTC::PortProfile_var portProfile = portRef->get_port_profile();
    string portName(portProfile->name);

    RTC::ConnectorProfileList_var connectorProfiles = portRef->get_connector_profiles();

    for(CORBA::ULong i=0; i < connectorProfiles->length(); ++i){
        RTC::ConnectorProfile& connectorProfile = connectorProfiles[i];
        Port_Service_List_Type& connectedPorts = connectorProfile.ports;

        for(CORBA::ULong j=0; j < connectedPorts.length(); ++j){
        	Port_Service_Ptr_Type connectedPortRef = connectedPorts[j];
            RTC::PortProfile_var connectedPortProfile = connectedPortRef->get_port_profile();
            RTC::RTObject_var connectedRtcRef = RTC::RTObject::_duplicate(connectedPortProfile->owner);
            RTC::RTObject_var thisRef = RTC::RTObject::_duplicate(getObjRef());

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

#ifdef OPENRTM_VERSION_042
                	RTC::ExecutionContextServiceList_var execServices = connectedRtcRef->get_execution_context_services();

                    for(CORBA::ULong k=0; k < execServices->length(); k++) {
                        RTC::ExecutionContextService_var execContext = execServices[k];

                    	ExtTrigExecutionContextService_Var_Type extTrigExecContext =
                    		ExtTrigExecutionContextService_Type::_narrow(execContext);

                        if(!CORBA::is_nil(extTrigExecContext)){
                            CORBA::ULong n = rtcList.length();
                            rtcList.length(n + 1);
                            rtcList[n] = connectedRtcRef;
                            foundRtcNames.insert(connectedRtcName);
                        }
                    }
#else
                    RTC::ExecutionContextList_var execServices = connectedRtcRef->get_owned_contexts();

                    for(CORBA::ULong k=0; k < execServices->length(); k++) {
                    	RTC::ExecutionContext_var execContext = execServices[k];

                    	ExtTrigExecutionContextService_Var_Type extTrigExecContext =
                    		ExtTrigExecutionContextService_Type::_narrow(execContext);

                        if(!CORBA::is_nil(extTrigExecContext)){
                            CORBA::ULong n = rtcList.length();
                            rtcList.length(n + 1);
                            rtcList[n] = connectedRtcRef;
                            foundRtcNames.insert(connectedRtcName);
                        }
                    }
#endif

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

bool VirtualRobotRTC::checkOutPortStepTime(double controlTimeStep)
{
    bool ret = true;
    for(OutPortHandlerMap::iterator it = outPortHandlers.begin(); it != outPortHandlers.end(); ++it){
        double stepTime = it->second->stepTime;
        if(stepTime && stepTime < controlTimeStep){
            cerr << "OutPort(" << it->second->portName << ") : Output interval(" << stepTime << ") must be longer than the control interval(" << controlTimeStep << ")." << std::endl;
            ret &= false;
        }
    }
    return ret;
}
