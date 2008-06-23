// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-
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
#include <rtm/CorbaConsumer.h>
#include <rtm/idl/RTCStub.h>

#include <Controller.h>
#include <ViewSimulator.h>
#include <DynamicsSimulator.h>

#include "BridgeConf.h"

namespace OpenHRP {

	namespace ControllerBridge {

		class BridgeConf;
		class VirtualRobotRTC;

		class Controller_impl
			: virtual public POA_OpenHRP::Controller,
			  virtual public PortableServer::RefCountServantBase
		{
		public:
			Controller_impl(BridgeConf* bridgeConf, const char* robotName, VirtualRobotRTC* virtualRobotRTC);
			~Controller_impl();

			SensorState& getCurrentSensorState();
			DblSequence* getLinkDataFromSimulator
			(const std::string& linkName, DynamicsSimulator::LinkDataType linkDataType);
			DblSequence* getSensorDataFromSimulator(const std::string& sensorName);
			ImageData* getCameraImageFromSimulator(int cameraId);
			DblSequence& getJointDataSeqRef(DynamicsSimulator::LinkDataType linkDataType);
			void flushJointDataSeqToSimulator(DynamicsSimulator::LinkDataType linkDataType);
  
			virtual void setDynamicsSimulator(DynamicsSimulator_ptr dynamicsSimulator);
			virtual void setViewSimulator(ViewSimulator_ptr viewSimulator);
		
			virtual void start();
			virtual void control();
			virtual void input();
			virtual void output();
			virtual void stop();

			virtual void destroy();
  
		private:
			BridgeConf* bridgeConf;

			std::string robotName;
			VirtualRobotRTC* virtualRobotRTC;

			typedef std::map<std::string, RTC::Port_var> PortMap;

			struct RtcInfo
			{
				RTC::CorbaConsumer<RTC::RTObject> rtcRef;
				PortMap portMap;
				RTC::CorbaConsumer<RTC::ExtTrigExecutionContextService> execContext;
				double timeRate;
				double timeRateCounter;
			};
			typedef boost::shared_ptr<RtcInfo> RtcInfoPtr;

			typedef std::map<std::string, RtcInfoPtr> RtcInfoMap;
			RtcInfoMap rtcInfoMap;

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

			void makePortMap(PortMap& portMap, RTC::RTObject_ptr rtc);
			void setupRtcs();
			void setupRtcConnections();
			bool connectPorts(RTC::Port_ptr outPort, RTC::Port_ptr inPort);
		};


		class ControllerFactory_impl : virtual public POA_OpenHRP::ControllerFactory
		{
		public:
			ControllerFactory_impl(RTC::Manager* rtcManager, BridgeConf* bridgeConf);
			~ControllerFactory_impl();

			virtual Controller_ptr create(const char* robotName);
			virtual void shutdown();

		private:
			RTC::Manager* rtcManager;
			BridgeConf* bridgeConf;

			VirtualRobotRTC* createVirtualRobotRTC();
			VirtualRobotRTC* currentVirtualRobotRTC;
		};
	}
}


#endif
