/**
   \file
   \author Shin'ichiro Nakaoka
*/

#include "VirtualRobotPortHandler.h"

#include "Controller_impl.h"

using namespace RTC;
using namespace OpenHRP;
using namespace OpenHRP::ControllerBridge;

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
    default:                 return DynamicsSimulator::INVALID_DATA_TYPE;
    }
  }
}


PortHandler::~PortHandler()
{

}


SensorStateOutPortHandler::SensorStateOutPortHandler(PortInfo& info)
  : outPort(info.portName.c_str(), values)
{
  dataTypeId = info.dataTypeId;
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
}


void SensorStateOutPortHandler::writeDataToPort()
{
  outPort.write();
}


LinkDataOutPortHandler::LinkDataOutPortHandler(PortInfo& info) :
  outPort(info.portName.c_str(), value),
  linkName(info.dataOwnerName)
{
  linkDataType = toDynamicsSimulatorLinkDataType(info.dataTypeId);
}


void LinkDataOutPortHandler::inputDataFromSimulator(Controller_impl* controller)
{
  DblSequence_var data = controller->getLinkDataFromSimulator(linkName, linkDataType);
  value.data = data;
}


void LinkDataOutPortHandler::writeDataToPort()
{
  outPort.write();
}


SensorDataOutPortHandler::SensorDataOutPortHandler(PortInfo& info) :
  outPort(info.portName.c_str(), value),
  sensorName(info.dataOwnerName)
{

}


void SensorDataOutPortHandler::inputDataFromSimulator(Controller_impl* controller)
{
  DblSequence_var data = controller->getSensorDataFromSimulator(sensorName);
  value.data = data;
}


void SensorDataOutPortHandler::writeDataToPort()
{
  outPort.write();
}


ColorImageOutPortHandler::ColorImageOutPortHandler(PortInfo& info) :
  outPort(info.portName.c_str(), image),
  cameraId(info.dataOwnerId)
{

}


void ColorImageOutPortHandler::inputDataFromSimulator(Controller_impl* controller)
{
  ImageData_var imageInput = controller->getCameraImageFromSimulator(cameraId);
  image.data = imageInput->longData;
}


void ColorImageOutPortHandler::writeDataToPort()
{
  outPort.write();
}


GrayScaleImageOutPortHandler::GrayScaleImageOutPortHandler(PortInfo& info)
  : outPort(info.portName.c_str(), image),
    cameraId(info.dataOwnerId)
{

}


void GrayScaleImageOutPortHandler::inputDataFromSimulator(Controller_impl* controller)
{
  ImageData_var imageInput = controller->getCameraImageFromSimulator(cameraId);
  image.data = imageInput->octetData;
}


void GrayScaleImageOutPortHandler::writeDataToPort()
{
  outPort.write();
}


DepthImageOutPortHandler::DepthImageOutPortHandler(PortInfo& info) :
  outPort(info.portName.c_str(), image),
  cameraId(info.dataOwnerId)
{

}


void DepthImageOutPortHandler::inputDataFromSimulator(Controller_impl* controller)
{
  ImageData_var imageInput = controller->getCameraImageFromSimulator(cameraId);
  image.data = imageInput->floatData;
}


void DepthImageOutPortHandler::writeDataToPort()
{
  outPort.write();
}


JointDataSeqInPortHandler::JointDataSeqInPortHandler(PortInfo& info) :
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
  inPort.read();

  DblSequence& data = controller->getJointDataSeqRef(linkDataType);
  
  CORBA::ULong n = values.data.length();
  data.length(n);
  for(CORBA::ULong i=0; i < n; ++i){
    data[i] = values.data[i];
  }
}
