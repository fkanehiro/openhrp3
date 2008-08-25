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

#include "Controller_impl.h"

#include <string>
#include <iostream>
#include <rtm/Manager.h>
#include <rtm/RTObject.h>
#include <rtm/NVUtil.h>

#include <hrpCorba/ORBwrap.h>

#include "VirtualRobotRTC.h"

using namespace std;
using namespace boost;

namespace {
  const bool CONTROLLER_BRIDGE_DEBUG = false;
}


Controller_impl::Controller_impl(BridgeConf* bridgeConf, const char* robotName, VirtualRobotRTC* virtualRobotRTC)
  : bridgeConf(bridgeConf),
    robotName(robotName),
    virtualRobotRTC(virtualRobotRTC)
{
  if(CONTROLLER_BRIDGE_DEBUG){
    cout << "Controller_impl::Controller_impl" << endl;
  }

  virtualRobotRTC->isOwnedByController = true;

  detectRtcs();
  setupRtcConnections();
}


Controller_impl::~Controller_impl()
{
  if(CONTROLLER_BRIDGE_DEBUG){
    cout << "Controller_impl::~Controller_impl" << endl;
  }

  delete naming;

  virtualRobotRTC->isOwnedByController = false;
  //virtualRobotRTC->exit();
}


void Controller_impl::detectRtcs()
{
  RTC::Manager& rtcManager = RTC::Manager::instance();

  string nameServer = rtcManager.getConfig()["corba.nameservers"];
  cout << "setting naming" << endl;
  int comPos = nameServer.find(",");
  if (comPos < 0){
    comPos = nameServer.length();
  }
  nameServer = nameServer.substr(0, comPos);
  naming = new RTC::CorbaNaming(rtcManager.getORB(), nameServer.c_str());

  cout << "setup RT components" << endl;

  for(size_t i=0; i < bridgeConf->portConnections.size(); ++i){

    PortConnection& connection = bridgeConf->portConnections[i];

    RTC::RTObject_var rtcRef;
    string rtcName;

    if(connection.controllerInstanceName.empty()){
      if(!bridgeConf->moduleInfoList.empty()){
	RTC::RtcBase* rtcServant = bridgeConf->moduleInfoList.front().rtcServant;
	rtcName = rtcServant->getInstanceName();
	rtcRef = rtcServant->getObjRef();
      }
    } else {
      rtcName = connection.controllerInstanceName;
      string rtcNamingName = rtcName + ".rtc";
      CORBA::Object_var objRef = naming->resolve(rtcNamingName.c_str());
      if(CORBA::is_nil(objRef)){
	cout << rtcName << " is not found." << endl;
      } else {
	rtcRef = RTC::RTObject::_narrow(objRef);
	if(CORBA::is_nil(rtcRef)){
	  cout << rtcName << " is not an RTC object." << endl;
	}
      }
    }

    if(!CORBA::is_nil(rtcRef)){
      addRtcWithConnection(rtcRef);
    }
  }

  RTC::RTCList_var rtcList = virtualRobotRTC->getConnectedRtcs();
  for(CORBA::ULong i=0; i < rtcList->length(); ++i){
    addRtcWithConnection(rtcList[i]);
  }
}


void Controller_impl::makePortMap(RtcInfoPtr& rtcInfo)
{
  RTC::PortList_var ports = rtcInfo->rtcRef->get_ports();
  for(CORBA::ULong i=0; i < ports->length(); ++i){
    RTC::PortProfile_var profile = ports[i]->get_port_profile();
    rtcInfo->portMap[string(profile->name)] = ports[i];
  }
}


void Controller_impl::addRtcWithConnection(RTC::RTObject_var rtcRef)
{
  RTC::ComponentProfile_var profile = rtcRef->get_component_profile();

  string instanceName(profile->instance_name);
  
  RtcInfoMap::iterator p = rtcInfoMap.find(instanceName);

  if(p == rtcInfoMap.end()){

    cout << "detected " << instanceName << endl;

    RtcInfoPtr rtcInfo(new RtcInfo());
    rtcInfo->rtcRef = rtcRef;
    makePortMap(rtcInfo);
    rtcInfo->timeRate = 1.0;

    rtcInfoMap.insert(make_pair(instanceName, rtcInfo));

    RTC::ExecutionContextServiceList_var eclist = rtcInfo->rtcRef->get_execution_context_services();
    for(CORBA::ULong i=0; i < eclist->length(); ++i){
      if(!CORBA::is_nil(eclist[i])){
	rtcInfo->execContext = RTC::ExtTrigExecutionContextService::_narrow(eclist[i]);
	if(!CORBA::is_nil(rtcInfo->execContext)){
	  cout << "detected the ExtTrigExecutionContext of " << instanceName << endl;
	}
	break;
      }
    }
  }
}


void Controller_impl::setupRtcConnections()
{
  for(size_t i=0; i < bridgeConf->portConnections.size(); ++i){

    const PortConnection& connection = bridgeConf->portConnections[i];

    string controllerInstanceName = connection.controllerInstanceName;
    if(controllerInstanceName.empty()){
      if(!bridgeConf->moduleInfoList.empty()){
	RTC::RtcBase* rtcServant = bridgeConf->moduleInfoList.front().rtcServant;
	controllerInstanceName = rtcServant->getInstanceName();
      }
    }

    cout << "connect " << virtualRobotRTC->getInstanceName() << ":" << connection.robotPortName;
    cout << " <--> " << controllerInstanceName << ":" << connection.controllerPortName;

    bool connected = false;
    
    RtcInfoMap::iterator p = rtcInfoMap.find(controllerInstanceName);
    if(p != rtcInfoMap.end()){

      RtcInfoPtr rtcInfo = p->second;

      PortMap::iterator q = rtcInfo->portMap.find(connection.controllerPortName);
      if(q == rtcInfo->portMap.end()){
	cout << "\n";
	cout << controllerInstanceName << " does not have a port ";
	cout << connection.controllerPortName << "\n";
      } else {
	RTC::Port_ptr controllerPortRef = q->second;
	
	PortHandlerPtr robotPortHandler = virtualRobotRTC->getPortHandler(connection.robotPortName);
	if(!robotPortHandler){
	  cout << "\n";
	  cout << "The robot does not have a port named " << connection.robotPortName << "\n";
	} else {
	  RTC::Port_ptr robotPortRef = robotPortHandler->portRef;

	  if(!CORBA::is_nil(robotPortRef)){
	    if(dynamic_pointer_cast<OutPortHandler>(robotPortHandler)){
	      connected = connectPorts(robotPortRef, controllerPortRef);
	    } else {
	      connected = connectPorts(controllerPortRef, robotPortRef);
	    }
	  }
	}
      }
    }

    if(connected){
      cout << " ...ok" << endl;
    } else {
      cout << "Connection failed." << endl;
    }
  }
}


bool Controller_impl::connectPorts(RTC::Port_ptr outPort, RTC::Port_ptr inPort)
{
  // connect ports
  RTC::ConnectorProfile cprof;
  cprof.connector_id = "";
  cprof.name = CORBA::string_dup("connector0");
  cprof.ports.length(2);
  cprof.ports[0] = RTC::Port::_duplicate(inPort);
  cprof.ports[1] = RTC::Port::_duplicate(outPort);

  CORBA_SeqUtil::push_back(cprof.properties,
			   NVUtil::newNV("dataport.interface_type",
					 "CORBA_Any"));
  CORBA_SeqUtil::push_back(cprof.properties,
			   NVUtil::newNV("dataport.dataflow_type",
					 "Push"));
  CORBA_SeqUtil::push_back(cprof.properties,
			   NVUtil::newNV("dataport.subscription_type",
					 "Flush"));
  RTC::ReturnCode_t result = inPort->connect(cprof);

  return (result == RTC::RTC_OK);
}


void Controller_impl::setDynamicsSimulator(DynamicsSimulator_ptr dynamicsSimulator)
{
  this->dynamicsSimulator = DynamicsSimulator::_duplicate(dynamicsSimulator);
}


void Controller_impl::setViewSimulator(ViewSimulator_ptr viewSimulator)
{
  this->viewSimulator = ViewSimulator::_duplicate(viewSimulator);
}


void Controller_impl::start()
{
  if(CONTROLLER_BRIDGE_DEBUG){
    cout << "Controller_impl::onStart" << endl;
  }

  controlTime = 0.0;

  if(!CORBA::is_nil(viewSimulator)) {
    viewSimulator->getCameraSequenceOf(robotName.c_str(), cameras);
  }

  for(RtcInfoMap::iterator p = rtcInfoMap.begin(); p != rtcInfoMap.end(); ++p){
    RtcInfoPtr& rtcInfo = p->second;
    rtcInfo->timeRateCounter = 1.0;
    if(!CORBA::is_nil(rtcInfo->execContext)){
      rtcInfo->execContext->activate_component(rtcInfo->rtcRef);
    }
  }
}


SensorState& Controller_impl::getCurrentSensorState()
{
  if(!sensorStateUpdated){
    dynamicsSimulator->getCharacterSensorState(robotName.c_str(), sensorState);
    sensorStateUpdated = true;
  }

  return sensorState;
}


DblSequence* Controller_impl::getLinkDataFromSimulator
(const std::string& linkName, DynamicsSimulator::LinkDataType linkDataType)
{
  DblSequence_var data;
  dynamicsSimulator->getCharacterLinkData(robotName.c_str(), linkName.c_str(), linkDataType, data.out());
  return data._retn();
}


DblSequence* Controller_impl::getSensorDataFromSimulator(const std::string& sensorName)
{
  DblSequence_var data;
  dynamicsSimulator->getCharacterSensorValues(robotName.c_str(), sensorName.c_str(), data.out());
  return data._retn();
}


ImageData* Controller_impl::getCameraImageFromSimulator(int cameraId)
{
  ImageData_var imageData = cameras[cameraId]->getImageData();
  return imageData._retn();
}
    

void Controller_impl::input()
{
  if(CONTROLLER_BRIDGE_DEBUG){
    cout << "Controller_impl::onInput" << endl;
  }

  sensorStateUpdated = false;

  virtualRobotRTC->inputDataFromSimulator(this);
}


DblSequence& Controller_impl::getJointDataSeqRef(DynamicsSimulator::LinkDataType linkDataType)
{
  return outputJointValueSeqInfos[linkDataType].values;
}


void Controller_impl::flushJointDataSeqToSimulator(DynamicsSimulator::LinkDataType linkDataType)
{
  JointValueSeqInfoMap::iterator p = outputJointValueSeqInfos.find(linkDataType);
  if(p != outputJointValueSeqInfos.end()){
    JointValueSeqInfo& info = p->second;
    if(!info.flushed){
      dynamicsSimulator->setCharacterAllLinkData(robotName.c_str(), linkDataType, info.values);
      info.flushed = true;
    }
  }
}

void Controller_impl::flushLinkDataToSimulator(const std::string& linkName, 
					       DynamicsSimulator::LinkDataType linkDataType,
					       const DblSequence& linkData)
{
  dynamicsSimulator->setCharacterLinkData(robotName.c_str(), linkName.c_str(),
					  linkDataType, linkData);
}

void Controller_impl::output()
{
  if(CONTROLLER_BRIDGE_DEBUG){
    cout << "Controller_impl::output" << endl;
  }

  for(JointValueSeqInfoMap::iterator p = outputJointValueSeqInfos.begin();
      p != outputJointValueSeqInfos.end(); ++p){
    JointValueSeqInfo& info = p->second;
    info.flushed = false;
  }

  virtualRobotRTC->outputDataToSimulator(this);
}


void Controller_impl::control()
{
  if(CONTROLLER_BRIDGE_DEBUG){
    cout << "Controller_impl::control" << endl;
  }

  controlTime += timeStep;

  virtualRobotRTC->writeDataToOutPorts();

  for(RtcInfoMap::iterator p = rtcInfoMap.begin(); p != rtcInfoMap.end(); ++p){
    RtcInfoPtr& rtcInfo = p->second;
    if(!CORBA::is_nil(rtcInfo->execContext)){
      rtcInfo->timeRateCounter += rtcInfo->timeRate;
      if(rtcInfo->timeRateCounter >= 1.0){
	rtcInfo->execContext->tick();
	rtcInfo->timeRateCounter -= 1.0;
      }
    }
  }

  virtualRobotRTC->readDataFromInPorts(this);
}


void Controller_impl::stop()
{

}


void Controller_impl::destroy()
{
  if(CONTROLLER_BRIDGE_DEBUG){
    cout << "Controller_impl::destroy()" << endl;
  }

  PortableServer::POA_var poa = _default_POA();
  PortableServer::ObjectId_var id = poa->servant_to_id(this);
  poa->deactivate_object(id);
}

void Controller_impl::setTimeStep(CORBA::Double _timeStep){
    timeStep = _timeStep;
}

ControllerFactory_impl::ControllerFactory_impl(RTC::Manager* rtcManager, BridgeConf* bridgeConf)
  : rtcManager(rtcManager),
    bridgeConf(bridgeConf)
{
  if(CONTROLLER_BRIDGE_DEBUG){
    cout << "ControllerFactory_impl::ControllerFactory_impl()" << endl;
  }

  VirtualRobotRTC::registerFactory(rtcManager, bridgeConf->getVirtualRobotRtcTypeName());

  currentVirtualRobotRTC = createVirtualRobotRTC();
}


ControllerFactory_impl::~ControllerFactory_impl()
{
  if(CONTROLLER_BRIDGE_DEBUG){
    cout << "ControllerFactory_impl::~ControllerFactory_impl()" << endl;
  }

  PortableServer::POA_var poa = _default_POA();
  PortableServer::ObjectId_var id = poa->servant_to_id(this);
  poa->deactivate_object(id);
}


Controller_ptr ControllerFactory_impl::create(const char* robotName)
{
  if(CONTROLLER_BRIDGE_DEBUG){
    cout << "ControllerFactory_impl::createController()" << endl;
  }

  Controller_impl* controller = 0;

  if(!currentVirtualRobotRTC || currentVirtualRobotRTC->isOwnedByController){
    currentVirtualRobotRTC = createVirtualRobotRTC();
  }

  if(currentVirtualRobotRTC){
    try{
      controller = new Controller_impl(bridgeConf, robotName, currentVirtualRobotRTC);
      _default_POA()->activate_object(controller);
      
    } catch(CORBA_SystemException& ex){
      cerr << ex._rep_id() << endl;
      cerr << "exception in createController" << endl;
    } catch(std::invalid_argument& ex){
      cerr << "invalid argument : " << ex.what() << endl;
    } catch(...){
      cerr << "unknown exception in ControllerFactory_impl::create()" <<  endl;
    }
  }
  
  return controller->_this();
}


VirtualRobotRTC* ControllerFactory_impl::createVirtualRobotRTC()
{
  RTC::RtcBase* rtc = rtcManager->createComponent(bridgeConf->getVirtualRobotRtcTypeName());
  return dynamic_cast<VirtualRobotRTC*>(rtc);
}


void ControllerFactory_impl::shutdown()
{
  if(CONTROLLER_BRIDGE_DEBUG){
    cout << "ControllerFactory_impl::shutdown()" << endl;
  }

  rtcManager->terminate();
}
