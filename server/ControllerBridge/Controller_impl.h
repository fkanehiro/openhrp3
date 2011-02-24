// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-
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

#ifndef OPENHRP_CONTROLLER_BRIDGE_CONTROLLER_IMPL_H_INCLUDED
#define OPENHRP_CONTROLLER_BRIDGE_CONTROLLER_IMPL_H_INCLUDED

#include <string>
#include <map>
#include <rtm/RTC.h>
#include <rtm/RTObject.h>
#include <rtm/CorbaNaming.h>
#include <rtm/idl/RTCStub.h>

#include <hrpCorba/Controller.hh>
#include <hrpCorba/ViewSimulator.hh>
#include <hrpCorba/DynamicsSimulator.hh>

#include "BridgeConf.h"

#include "config.h"

using namespace OpenHRP;

class BridgeConf;
class VirtualRobotRTC;

class Controller_impl
	: virtual public POA_OpenHRP::Controller
{
public:
	Controller_impl(RTC::Manager* rtcManager, BridgeConf* bridgeConf);
	~Controller_impl();

	SensorState& getCurrentSensorState();
	DblSequence* getLinkDataFromSimulator
	(const std::string& linkName, DynamicsSimulator::LinkDataType linkDataType);
	DblSequence* getSensorDataFromSimulator(const std::string& sensorName);
	ImageData* getCameraImageFromSimulator(int cameraId);
	DblSequence& getJointDataSeqRef(DynamicsSimulator::LinkDataType linkDataType);
	void flushJointDataSeqToSimulator(DynamicsSimulator::LinkDataType linkDataType);
    void flushLinkDataToSimulator(const std::string& linkName,
	                                DynamicsSimulator::LinkDataType linkDataType,
									const DblSequence& linkData);

	virtual void setDynamicsSimulator(DynamicsSimulator_ptr dynamicsSimulator);
	virtual void setViewSimulator(ViewSimulator_ptr viewSimulator);
	void setTimeStep(CORBA::Double _timeStep){
        timeStep = _timeStep;
    }
    double getTimeStep(){
        return timeStep;
    }

	virtual void start();
	virtual void control();
	virtual void input();
	virtual void output();
	virtual void stop();
	virtual void destroy();

    virtual void shutdown();
    virtual omniObjRef* _do_get_interface(){return _this();}
    virtual void setModelName(const char* localModelName){ modelName = localModelName;}
    virtual void initialize();
    double controlTime;

private:
	BridgeConf* bridgeConf;
	RTC::Manager* rtcManager;

	std::string modelName;
	VirtualRobotRTC* virtualRobotRTC;

	typedef std::map<std::string, Port_Service_Var_Type> PortMap;

	struct RtcInfo
	{
		RTC::RTObject_var rtcRef;
		PortMap portMap;
		ExtTrigExecutionContextService_Var_Type execContext;
		double timeRate;
		double timeRateCounter;
	};
	typedef boost::shared_ptr<RtcInfo> RtcInfoPtr;

	typedef std::map<std::string, RtcInfoPtr> RtcInfoMap;
	RtcInfoMap rtcInfoMap;
    typedef std::vector<RtcInfoPtr> RtcInfoVector;
    RtcInfoVector rtcInfoVector;

	RTC::CorbaNaming* naming;

	DynamicsSimulator_var dynamicsSimulator;
	ViewSimulator_var viewSimulator;

	SensorState_var sensorState;
	bool sensorStateUpdated;

	struct JointValueSeqInfo {
		bool flushed;
		DblSequence values;
	};

	typedef std::map<DynamicsSimulator::LinkDataType, JointValueSeqInfo> JointValueSeqInfoMap;
	JointValueSeqInfoMap outputJointValueSeqInfos;

	CameraSequence_var cameras;
	Camera::CameraParameter_var cparam;

	void detectRtcs();
	void makePortMap(RtcInfoPtr& rtcInfo);
    Controller_impl::RtcInfoPtr addRtcVectorWithConnection(RTC::RTObject_var rtcRef);
	void setupRtcConnections();
	int connectPorts(Port_Service_Ptr_Type outPort, Port_Service_Ptr_Type inPort);

    void activeComponents();
    void deactiveComponents();
    void disconnectRtcConnections(PortMap& refPortMap);
    double timeStep;
    bool bRestart;
    void restart();
};

#endif
