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
/*!
 * @file  JoystickController.cpp
 * @brief Controller to drive a mobile robot
 * @date $Date$
 *
 * $Id$
 */

#include "JoystickController.h"

// Module specification
// <rtc-template block="module_spec">
static const char* joystickcontroller_spec[] =
  {
    "implementation_id", "JoystickController",
    "type_name",         "JoystickController",
    "description",       "Controller to drive a mobile robot",
    "version",           "1.0.0",
    "vendor",            "AIST HRG",
    "category",          "OpenHRP Controller",
    "activity_type",     "DataFlowComponent",
    "max_instance",      "1",
    "language",          "C++",
    "lang_type",         "compile",
    ""
  };
// </rtc-template>

JoystickController::JoystickController(RTC::Manager* manager)
  : RTC::DataFlowComponentBase(manager),
    // <rtc-template block="initializer">
    m_angleIn("angle", m_angle),
    m_velocityIn("velocity", m_velocity),
    m_commandIn("command", m_command),
    m_torqueOut("torque", m_torque),
    
    // </rtc-template>
	dummy(0)
{
  // Registration: InPort/OutPort/Service
  // <rtc-template block="registration">
  
  // Set service provider to Ports
  
  // Set service consumers to Ports
  
  // Set CORBA Service Ports
  
  // </rtc-template>

}

JoystickController::~JoystickController()
{
}


RTC::ReturnCode_t JoystickController::onInitialize()
{
  // Set InPort buffers
  addInPort("angle", m_angleIn);
  addInPort("velocity", m_velocityIn);
  addInPort("command", m_commandIn);
  
  // Set OutPort buffer
  addOutPort("torque", m_torqueOut);

  // ポート初期化 //
  m_command.data.length(2);
  m_command.data[0] = m_command.data[1] = 0.0;
  m_angle.data.length(1);
  m_angle.data[0] = 0.0;
  m_velocity.data.length(2);
  m_velocity.data[0] = m_velocity.data[1] = 0.0;
  m_torque.data.length(4);
  return RTC::RTC_OK;
}

/*
RTC::ReturnCode_t JoystickController::onFinalize()
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t JoystickController::onStartup(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t JoystickController::onShutdown(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t JoystickController::onActivated(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t JoystickController::onDeactivated(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

RTC::ReturnCode_t JoystickController::onExecute(RTC::UniqueId ec_id)
{
  // ロボットからのデータ入力 //
  m_angleIn.read();
  m_velocityIn.read();
  double steerAngle = m_angle.data[0];
  double steerVel = m_velocity.data[0];
  double tireVel = m_velocity.data[1];

  // ジョイスティック（ユーザ）からのデータ入力 //
  m_commandIn.read();
  double steerCommandAngle = 3.14159 * -0.5 * m_command.data[0] / 180.0;
  double tireCommandVel = m_command.data[1] / 10;

  // ステアリングトルク計算 //
  double steerCommandTorque = 20.0 * (steerCommandAngle - steerAngle) - 2.0 * steerVel;

  // 駆動トルク計算 //
  double tireCommandTorque = 1.0 * (tireCommandVel - tireVel);

  // ロボットへのトルク出力 //
  m_torque.data[0] = steerCommandTorque;
  m_torque.data[1] = tireCommandTorque;
  m_torque.data[2] = tireCommandTorque; 
  m_torque.data[3] = tireCommandTorque; 
  m_torqueOut.write(); 

  return RTC::RTC_OK;
}

/*
RTC::ReturnCode_t JoystickController::onAborting(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t JoystickController::onError(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t JoystickController::onReset(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t JoystickController::onStateUpdate(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t JoystickController::onRateChanged(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/



extern "C"
{
 
  void JoystickControllerInit(RTC::Manager* manager)
  {
    RTC::Properties profile(joystickcontroller_spec);
    manager->registerFactory(profile,
                             RTC::Create<JoystickController>,
                             RTC::Delete<JoystickController>);
  }
  
};


