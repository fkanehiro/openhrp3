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

#ifndef OPENHRP_CONTROLLER_BRIDGE_VIRTUAL_ROBOT_PORT_HANDLER_H_INCLUDED
#define OPENHRP_CONTROLLER_BRIDGE_VIRTUAL_ROBOT_PORT_HANDLER_H_INCLUDED

#include <boost/shared_ptr.hpp>

#include <rtm/RTC.h>
#include <rtm/PortBase.h>
#include <rtm/OutPort.h>
#include <rtm/InPort.h>
#include <rtm/idl/BasicDataTypeStub.h>
#include <rtm/idl/ExtendedDataTypes.hh>

#include <hrpCorba/DynamicsSimulator.hh>

#include "BridgeConf.h"

#include "config.h"

using namespace OpenHRP;


class Controller_impl;

class PortHandler
{
public:
    PortHandler(PortInfo& info) : portName(info.portName){} 
    virtual ~PortHandler();
    Port_Service_Var_Type portRef;
    std::string portName;
};

typedef boost::shared_ptr<PortHandler> PortHandlerPtr;


class OutPortHandler : public PortHandler
{
public:
    OutPortHandler(PortInfo& info) : PortHandler(info){}
    virtual void inputDataFromSimulator(Controller_impl* controller) = 0;
    virtual void writeDataToPort() = 0;
    template<class T> void setTime(T& value, double _time)
    {
        value.tm.sec = (unsigned long)_time;
        value.tm.nsec = (unsigned long)((_time-value.tm.sec)*1000000000.0 + 0.5);
		if( value.tm.nsec >= 1000000000 ){
			value.tm.sec++;
			value.tm.nsec -= 1000000000;
		}
    }
    double stepTime;
};

typedef boost::shared_ptr<OutPortHandler> OutPortHandlerPtr;


class InPortHandler : public PortHandler
{
public:
    InPortHandler(PortInfo& info) : PortHandler(info){} 
    virtual void outputDataToSimulator(Controller_impl* controller) = 0;
    virtual void readDataFromPort(Controller_impl* controller) = 0;
};

typedef boost::shared_ptr<InPortHandler> InPortHandlerPtr;


class SensorStateOutPortHandler : public OutPortHandler
{
public:
    SensorStateOutPortHandler(PortInfo& info);
    virtual void inputDataFromSimulator(Controller_impl* controller);
    virtual void writeDataToPort();
    RTC::OutPort<RTC::TimedDoubleSeq> outPort;
private:
    RTC::TimedDoubleSeq values;
    DataTypeId dataTypeId;
};


class LinkDataOutPortHandler : public OutPortHandler
{
public:
    LinkDataOutPortHandler(PortInfo& info);
    virtual void inputDataFromSimulator(Controller_impl* controller);
    virtual void writeDataToPort();
    RTC::OutPort<RTC::TimedDoubleSeq> outPort;
private:
    std::vector<std::string> linkName;
    DynamicsSimulator::LinkDataType linkDataType;
    RTC::TimedDoubleSeq value;
};


class SensorDataOutPortHandler : public OutPortHandler
{
public:
    SensorDataOutPortHandler(PortInfo& info);
    virtual void inputDataFromSimulator(Controller_impl* controller);
    virtual void writeDataToPort();
    RTC::OutPort<RTC::TimedDoubleSeq> outPort;
private:
    RTC::TimedDoubleSeq value;
    std::vector<std::string> sensorName;
};

class GyroSensorOutPortHandler : public OutPortHandler
{
public:
    GyroSensorOutPortHandler(PortInfo& info);
    virtual void inputDataFromSimulator(Controller_impl* controller);
    virtual void writeDataToPort();
    RTC::OutPort<RTC::TimedAngularVelocity3D> outPort;
private:
    RTC::TimedAngularVelocity3D value;
    std::vector<std::string> sensorName;
};

class AccelerationSensorOutPortHandler : public OutPortHandler
{
public:
    AccelerationSensorOutPortHandler(PortInfo& info);
    virtual void inputDataFromSimulator(Controller_impl* controller);
    virtual void writeDataToPort();
    RTC::OutPort<RTC::TimedAcceleration3D> outPort;
private:
    RTC::TimedAcceleration3D value;
    std::vector<std::string> sensorName;
};


class ColorImageOutPortHandler : public OutPortHandler
{
public:
    ColorImageOutPortHandler(PortInfo& info);
    virtual void inputDataFromSimulator(Controller_impl* controller);
    virtual void writeDataToPort();
    RTC::OutPort<RTC::TimedLongSeq> outPort;
private:
    RTC::TimedLongSeq image;
    int cameraId;
};


class GrayScaleImageOutPortHandler : public OutPortHandler
{
public:
    GrayScaleImageOutPortHandler(PortInfo& info);
    virtual void inputDataFromSimulator(Controller_impl* controller);
    virtual void writeDataToPort();
    RTC::OutPort<RTC::TimedOctetSeq> outPort;
private:
    RTC::TimedOctetSeq image;
    int cameraId;
};


class DepthImageOutPortHandler : public OutPortHandler
{
public:
    DepthImageOutPortHandler(PortInfo& info);
    virtual void inputDataFromSimulator(Controller_impl* controller);
    virtual void writeDataToPort();
    RTC::OutPort<RTC::TimedFloatSeq> outPort;
private:
    RTC::TimedFloatSeq image;
    int cameraId;
};


class JointDataSeqInPortHandler : public InPortHandler
{
public:
    JointDataSeqInPortHandler(PortInfo& info);
    virtual void outputDataToSimulator(Controller_impl* controller);
    virtual void readDataFromPort(Controller_impl* controller);
    RTC::InPort<RTC::TimedDoubleSeq> inPort;
private:
    RTC::TimedDoubleSeq values;
    DynamicsSimulator::LinkDataType linkDataType;
};

class LinkDataInPortHandler : public InPortHandler
    {
public:
    LinkDataInPortHandler(PortInfo& info);
    virtual void outputDataToSimulator(Controller_impl* controller);
    virtual void readDataFromPort(Controller_impl* controller);
    RTC::InPort<RTC::TimedDoubleSeq> inPort;
private:
    RTC::TimedDoubleSeq values;
    std::vector<std::string> linkName;
    DynamicsSimulator::LinkDataType linkDataType;
    DblSequence data;
};

#endif
