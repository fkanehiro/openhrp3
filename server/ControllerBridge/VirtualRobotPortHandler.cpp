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

#include "hrpUtil/Eigen3d.h"
#include "VirtualRobotPortHandler.h"

#include "Controller_impl.h"

using namespace RTC;

namespace {

  template <typename TSrc>
  void copyDblArraySeqToTimedDoubleSeq(const TSrc& src, int arraySize, TimedDoubleSeq& dest)
  {
    CORBA::ULong n = src.length();
    CORBA::ULong m = n * arraySize;
    dest.data.length(m);
    CORBA::ULong destPos = 0;
    for(CORBA::ULong i=0; i < n; ++i){
      for(int j=0; j < arraySize; ++j){
	dest.data[destPos++] = src[i][j];
      }
    }
  }

  template <typename TSrcSeq, typename TDestSeq>
  void copyImageData();


  DynamicsSimulator::LinkDataType toDynamicsSimulatorLinkDataType(DataTypeId id)
  {
    switch(id){
    case JOINT_VALUE:        return DynamicsSimulator::JOINT_VALUE;
    case JOINT_VELOCITY:     return DynamicsSimulator::JOINT_VELOCITY;
    case JOINT_ACCELERATION: return DynamicsSimulator::JOINT_ACCELERATION;
    case JOINT_TORQUE:       return DynamicsSimulator::JOINT_TORQUE;
    case EXTERNAL_FORCE:     return DynamicsSimulator::EXTERNAL_FORCE;
    case ABS_TRANSFORM:      return DynamicsSimulator::ABS_TRANSFORM;
    case ABS_TRANSFORM2:     return DynamicsSimulator::ABS_TRANSFORM;
    case ABS_VELOCITY:       return DynamicsSimulator::ABS_VELOCITY;
    case ABS_ACCELERATION:   return DynamicsSimulator::ABS_ACCELERATION;
    case CONSTRAINT_FORCE:   return DynamicsSimulator::CONSTRAINT_FORCE;
    default:                 return DynamicsSimulator::INVALID_DATA_TYPE;
    }
  }
}

PortHandler::~PortHandler()
{

}


SensorStateOutPortHandler::SensorStateOutPortHandler(PortInfo& info) : 
  OutPortHandler(info), 
  outPort(info.portName.c_str(), values)
{
  dataTypeId = info.dataTypeId;
  stepTime = info.stepTime;
}


void SensorStateOutPortHandler::inputDataFromSimulator(Controller_impl* controller)
{
  SensorState& state = controller->getCurrentSensorState();

  switch(dataTypeId) {
  case JOINT_VALUE:
    values.data = state.q;
    break;
  case JOINT_VELOCITY:
    values.data = state.dq;
    break;
  case JOINT_TORQUE:
    values.data = state.u;
    break;
  case FORCE_SENSOR:
    copyDblArraySeqToTimedDoubleSeq(state.force, 6, values);
    break;
  case RATE_GYRO_SENSOR:
    copyDblArraySeqToTimedDoubleSeq(state.rateGyro, 3, values);
    break;
  case ACCELERATION_SENSOR:
    copyDblArraySeqToTimedDoubleSeq(state.accel, 3, values);
    break;
  default:
    break;
  }
   setTime(values, controller->controlTime);
}


void SensorStateOutPortHandler::writeDataToPort()
{
  outPort.write();
}


LinkDataOutPortHandler::LinkDataOutPortHandler(PortInfo& info) :
  OutPortHandler(info),
  outPort(info.portName.c_str(), value),
  linkName(info.dataOwnerName)
{
  linkDataType = toDynamicsSimulatorLinkDataType(info.dataTypeId);
  stepTime = info.stepTime;
}


void LinkDataOutPortHandler::inputDataFromSimulator(Controller_impl* controller)
{
    size_t n;
    CORBA::ULong m=0;
    n = linkName.size();
    for(size_t i=0, k=0; i<n; i++){
        DblSequence_var data = controller->getLinkDataFromSimulator(linkName[i], linkDataType);       
        if(!i){
            m = data->length();
            value.data.length(n*m);
        }
        for(CORBA::ULong j=0; j < m; j++)
            value.data[k++] = data[j];  
    }  
    setTime(value, controller->controlTime);
}


void LinkDataOutPortHandler::writeDataToPort()
{
  outPort.write();
}

AbsTransformOutPortHandler::AbsTransformOutPortHandler(PortInfo& info) :
  OutPortHandler(info),
  outPort(info.portName.c_str(), value),
  linkName(info.dataOwnerName)
{
  linkDataType = toDynamicsSimulatorLinkDataType(info.dataTypeId);
  stepTime = info.stepTime;
}


void AbsTransformOutPortHandler::inputDataFromSimulator(Controller_impl* controller)
{
    DblSequence_var data = controller->getLinkDataFromSimulator(linkName[0], linkDataType);       
    value.data.position.x = data[0];
    value.data.position.y = data[1];
    value.data.position.z = data[2];
    hrp::Matrix33 R;
    R << data[3], data[4], data[5], 
        data[6], data[7], data[8], 
        data[9], data[10], data[11];
    hrp::Vector3 rpy = hrp::rpyFromRot(R);
    value.data.orientation.r = rpy[0];
    value.data.orientation.p = rpy[1];
    value.data.orientation.y = rpy[2];
    setTime(value, controller->controlTime);
}


void AbsTransformOutPortHandler::writeDataToPort()
{
  outPort.write();
}


SensorDataOutPortHandler::SensorDataOutPortHandler(PortInfo& info) :
  OutPortHandler(info),
  outPort(info.portName.c_str(), value),
  sensorName(info.dataOwnerName)
{
    stepTime = info.stepTime;
}


void SensorDataOutPortHandler::inputDataFromSimulator(Controller_impl* controller)
{
    size_t n;
    CORBA::ULong m=0;
    n = sensorName.size();
    for(size_t i=0, k=0; i<n; i++){
        DblSequence_var data = controller->getSensorDataFromSimulator(sensorName[i]);    
        if(!i){
            m = data->length();
            value.data.length(n*m);
        }
        for(CORBA::ULong j=0; j < m; j++)
            value.data[k++] = data[j];  
    }  
    setTime(value, controller->controlTime);
}


void SensorDataOutPortHandler::writeDataToPort()
{
  outPort.write();
}


GyroSensorOutPortHandler::GyroSensorOutPortHandler(PortInfo& info) :
  OutPortHandler(info),
  outPort(info.portName.c_str(), value),
  sensorName(info.dataOwnerName)
{
    stepTime = info.stepTime;
}


void GyroSensorOutPortHandler::inputDataFromSimulator(Controller_impl* controller)
{
    size_t n;
    n = sensorName.size();
    for(size_t i=0; i<n; i++){
        DblSequence_var data = controller->getSensorDataFromSimulator(sensorName[i]);    
	value.data.avx = data[0];
	value.data.avy = data[1];
	value.data.avz = data[2];
    }  
    setTime(value, controller->controlTime);
}


void GyroSensorOutPortHandler::writeDataToPort()
{
  outPort.write();
}


AccelerationSensorOutPortHandler::AccelerationSensorOutPortHandler(PortInfo& info) :
  OutPortHandler(info),
  outPort(info.portName.c_str(), value),
  sensorName(info.dataOwnerName)
{
    stepTime = info.stepTime;
}


void AccelerationSensorOutPortHandler::inputDataFromSimulator(Controller_impl* controller)
{
    size_t n;
    n = sensorName.size();
    for(size_t i=0; i<n; i++){
        DblSequence_var data = controller->getSensorDataFromSimulator(sensorName[i]);    
	value.data.ax = data[0];
	value.data.ay = data[1];
	value.data.az = data[2];
    }  
    setTime(value, controller->controlTime);
}


void AccelerationSensorOutPortHandler::writeDataToPort()
{
  outPort.write();
}


ColorImageOutPortHandler::ColorImageOutPortHandler(PortInfo& info) :
  OutPortHandler(info),
  outPort(info.portName.c_str(), image),
  cameraId(info.dataOwnerId)
{
    stepTime = info.stepTime;
}


void ColorImageOutPortHandler::inputDataFromSimulator(Controller_impl* controller)
{
  ImageData_var imageInput = controller->getCameraImageFromSimulator(cameraId);
  image.data = imageInput->longData;
  setTime(image, controller->controlTime);
}


void ColorImageOutPortHandler::writeDataToPort()
{
  outPort.write();
}


GrayScaleImageOutPortHandler::GrayScaleImageOutPortHandler(PortInfo& info) : 
  OutPortHandler(info),
  outPort(info.portName.c_str(), image),
  cameraId(info.dataOwnerId)
{
    stepTime = info.stepTime;
}


void GrayScaleImageOutPortHandler::inputDataFromSimulator(Controller_impl* controller)
{
  ImageData_var imageInput = controller->getCameraImageFromSimulator(cameraId);
  image.data = imageInput->octetData;
  setTime(image, controller->controlTime);
}


void GrayScaleImageOutPortHandler::writeDataToPort()
{
  outPort.write();
}


DepthImageOutPortHandler::DepthImageOutPortHandler(PortInfo& info) :
  OutPortHandler(info),
  outPort(info.portName.c_str(), image),
  cameraId(info.dataOwnerId)
{
    stepTime = info.stepTime;
}


void DepthImageOutPortHandler::inputDataFromSimulator(Controller_impl* controller)
{
  ImageData_var imageInput = controller->getCameraImageFromSimulator(cameraId);
  image.data = imageInput->floatData;
  setTime(image, controller->controlTime);
}


void DepthImageOutPortHandler::writeDataToPort()
{
  outPort.write();
}


JointDataSeqInPortHandler::JointDataSeqInPortHandler(PortInfo& info) :
  InPortHandler(info),
  inPort(info.portName.c_str(), values)
{
  linkDataType = toDynamicsSimulatorLinkDataType(info.dataTypeId);
}


void JointDataSeqInPortHandler::outputDataToSimulator(Controller_impl* controller)
{
  controller->flushJointDataSeqToSimulator(linkDataType);
}


void JointDataSeqInPortHandler::readDataFromPort(Controller_impl* controller)
{
  if( inPort.isNew() == false ){
	  DblSequence& data = controller->getJointDataSeqRef(linkDataType);
	  data.length(0);
      return;
  }
  inPort.read();

  DblSequence& data = controller->getJointDataSeqRef(linkDataType);
  
  CORBA::ULong n = values.data.length();
  data.length(n);
  for(CORBA::ULong i=0; i < n; ++i){
    data[i] = values.data[i];
  }
}

LinkDataInPortHandler::LinkDataInPortHandler(PortInfo& info) :
  InPortHandler(info),
  inPort(info.portName.c_str(), values),
  linkName(info.dataOwnerName)
{
  linkDataType = toDynamicsSimulatorLinkDataType(info.dataTypeId);
}


void LinkDataInPortHandler::outputDataToSimulator(Controller_impl* controller)
{
    if(!data.length())
        return;
    size_t n=linkName.size();
    CORBA::ULong m=data.length()/n;
    for(size_t i=0, k=0; i< n; i++){
        DblSequence data0;
        data0.length(m);
        for(CORBA::ULong j=0; j<m; j++)
            data0[j] = data[k++];
        controller->flushLinkDataToSimulator(linkName[i], linkDataType, data0);
    }

}


void LinkDataInPortHandler::readDataFromPort(Controller_impl* controller)
{
  if( inPort.isNew() == false ){
      return;
  }
  inPort.read();

  CORBA::ULong n = values.data.length();
  data.length(n);
  for(CORBA::ULong i=0; i < n; ++i){
    data[i] = values.data[i];
  }
}

AbsTransformInPortHandler::AbsTransformInPortHandler(PortInfo& info) :
  InPortHandler(info),
  inPort(info.portName.c_str(), values),
  linkName(info.dataOwnerName)
{
  linkDataType = toDynamicsSimulatorLinkDataType(info.dataTypeId);
}


void AbsTransformInPortHandler::outputDataToSimulator(Controller_impl* controller)
{
    if(!data.length())
        return;
    controller->flushLinkDataToSimulator(linkName[0], linkDataType, data);
}


void AbsTransformInPortHandler::readDataFromPort(Controller_impl* controller)
{
  if( inPort.isNew() == false ){
      return;
  }
  inPort.read();

  data.length(12);
  data[0] = values.data.position.x;
  data[1] = values.data.position.y;
  data[2] = values.data.position.z;
  hrp::Matrix33 R = hrp::rotFromRpy(values.data.orientation.r,
                                    values.data.orientation.p,
                                    values.data.orientation.y);
  for (int i=0; i<3; i++){
      for (int j=0; j<3; j++){
          data[3+i*3+j] = R(i,j);
      }
  }
}
