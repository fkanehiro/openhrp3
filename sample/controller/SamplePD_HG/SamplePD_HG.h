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
/*!
 * @file  SamplePD_HG.h
 * @brief Sample PD component
 * @date  $Date$
 *
 * $Id$
 */

#ifndef SamplePD_HG_H
#define SamplePD_HG_H

#include <rtm/Manager.h>
#include <rtm/DataFlowComponentBase.h>
#include <rtm/CorbaPort.h>
#include <rtm/DataInPort.h>
#include <rtm/DataOutPort.h>
#include <rtm/idl/BasicDataTypeSkel.h>

#include <vector>

// Service implementation headers
// <rtc-template block="service_impl_h">

// </rtc-template>

// Service Consumer stub headers
// <rtc-template block="consumer_stub_h">

// </rtc-template>

using namespace RTC;

class SamplePD_HG
  : public RTC::DataFlowComponentBase
{
 public:
  SamplePD_HG(RTC::Manager* manager);
  ~SamplePD_HG();

  // The initialize action (on CREATED->ALIVE transition)
  // formaer rtc_init_entry() 
 virtual RTC::ReturnCode_t onInitialize();

  // The finalize action (on ALIVE->END transition)
  // formaer rtc_exiting_entry()
  // virtual RTC::ReturnCode_t onFinalize();

  // The startup action when ExecutionContext startup
  // former rtc_starting_entry()
  // virtual RTC::ReturnCode_t onStartup(RTC::UniqueId ec_id);

  // The shutdown action when ExecutionContext stop
  // former rtc_stopping_entry()
  // virtual RTC::ReturnCode_t onShutdown(RTC::UniqueId ec_id);

  // The activated action (Active state entry action)
  // former rtc_active_entry()
  virtual RTC::ReturnCode_t onActivated(RTC::UniqueId ec_id);

  // The deactivated action (Active state exit action)
  // former rtc_active_exit()
  // virtual RTC::ReturnCode_t onDeactivated(RTC::UniqueId ec_id);

  // The execution action that is invoked periodically
  // former rtc_active_do()
  virtual RTC::ReturnCode_t onExecute(RTC::UniqueId ec_id);

  // The aborting action when main logic error occurred.
  // former rtc_aborting_entry()
  // virtual RTC::ReturnCode_t onAborting(RTC::UniqueId ec_id);

  // The error action in ERROR state
  // former rtc_error_do()
  // virtual RTC::ReturnCode_t onError(RTC::UniqueId ec_id);

  // The reset action that is invoked resetting
  // This is same but different the former rtc_init_entry()
  // virtual RTC::ReturnCode_t onReset(RTC::UniqueId ec_id);
  
  // The state update action that is invoked after onExecute() action
  // no corresponding operation exists in OpenRTm-aist-0.2.0
  // virtual RTC::ReturnCode_t onStateUpdate(RTC::UniqueId ec_id);

  // The action that is invoked when execution context's rate is changed
  // no corresponding operation exists in OpenRTm-aist-0.2.0
  // virtual RTC::ReturnCode_t onRateChanged(RTC::UniqueId ec_id);


 protected:
  // Configuration variable declaration
  // <rtc-template block="config_declare">
  
  // </rtc-template>

  // DataInPort declaration
  // <rtc-template block="inport_declare">
  TimedDoubleSeq m_angle_in;
  InPort<TimedDoubleSeq> m_angle_inIn;
  
  // </rtc-template>

  // DataOutPort declaration
  // <rtc-template block="outport_declare">
  TimedDoubleSeq m_angle_out0;
  OutPort<TimedDoubleSeq> m_angle_out0Out;
  TimedDoubleSeq m_angle_out1;
  OutPort<TimedDoubleSeq> m_angle_out1Out;
  TimedDoubleSeq m_angle_out2;
  OutPort<TimedDoubleSeq> m_angle_out2Out;
  TimedDoubleSeq m_angle_out3;
  OutPort<TimedDoubleSeq> m_angle_out3Out;
  TimedDoubleSeq m_angle_out4;
  OutPort<TimedDoubleSeq> m_angle_out4Out;
  TimedDoubleSeq m_angle_out5;
  OutPort<TimedDoubleSeq> m_angle_out5Out;
  TimedDoubleSeq m_angle_out6;
  OutPort<TimedDoubleSeq> m_angle_out6Out;
  TimedDoubleSeq m_angle_out7;
  OutPort<TimedDoubleSeq> m_angle_out7Out;
  TimedDoubleSeq m_angle_out8;
  OutPort<TimedDoubleSeq> m_angle_out8Out;
  TimedDoubleSeq m_angle_out9;
  OutPort<TimedDoubleSeq> m_angle_out9Out;
  TimedDoubleSeq m_angle_out10;
  OutPort<TimedDoubleSeq> m_angle_out10Out;
  TimedDoubleSeq m_angle_out11;
  OutPort<TimedDoubleSeq> m_angle_out11Out;

  TimedDoubleSeq m_vel0;
  OutPort<TimedDoubleSeq> m_vel0Out;
  TimedDoubleSeq m_vel1;
  OutPort<TimedDoubleSeq> m_vel1Out;
  TimedDoubleSeq m_vel2;
  OutPort<TimedDoubleSeq> m_vel2Out;
  TimedDoubleSeq m_vel3;
  OutPort<TimedDoubleSeq> m_vel3Out;
  TimedDoubleSeq m_vel4;
  OutPort<TimedDoubleSeq> m_vel4Out;
  TimedDoubleSeq m_vel5;
  OutPort<TimedDoubleSeq> m_vel5Out;
  TimedDoubleSeq m_vel6;
  OutPort<TimedDoubleSeq> m_vel6Out;
  TimedDoubleSeq m_vel7;
  OutPort<TimedDoubleSeq> m_vel7Out;
  TimedDoubleSeq m_vel8;
  OutPort<TimedDoubleSeq> m_vel8Out;
  TimedDoubleSeq m_vel9;
  OutPort<TimedDoubleSeq> m_vel9Out;
  TimedDoubleSeq m_vel10;
  OutPort<TimedDoubleSeq> m_vel10Out;
  TimedDoubleSeq m_vel11;
  OutPort<TimedDoubleSeq> m_vel11Out;

  TimedDoubleSeq m_acc0;
  OutPort<TimedDoubleSeq> m_acc0Out;
  TimedDoubleSeq m_acc1;
  OutPort<TimedDoubleSeq> m_acc1Out;
  TimedDoubleSeq m_acc2;
  OutPort<TimedDoubleSeq> m_acc2Out;
  TimedDoubleSeq m_acc3;
  OutPort<TimedDoubleSeq> m_acc3Out;
  TimedDoubleSeq m_acc4;
  OutPort<TimedDoubleSeq> m_acc4Out;
  TimedDoubleSeq m_acc5;
  OutPort<TimedDoubleSeq> m_acc5Out;
  TimedDoubleSeq m_acc6;
  OutPort<TimedDoubleSeq> m_acc6Out;
  TimedDoubleSeq m_acc7;
  OutPort<TimedDoubleSeq> m_acc7Out;
  TimedDoubleSeq m_acc8;
  OutPort<TimedDoubleSeq> m_acc8Out;
  TimedDoubleSeq m_acc9;
  OutPort<TimedDoubleSeq> m_acc9Out;
  TimedDoubleSeq m_acc10;
  OutPort<TimedDoubleSeq> m_acc10Out;
  TimedDoubleSeq m_acc11;
  OutPort<TimedDoubleSeq> m_acc11Out;

  TimedDoubleSeq m_torque0;
  OutPort<TimedDoubleSeq> m_torque0Out;
  TimedDoubleSeq m_torque1;
  OutPort<TimedDoubleSeq> m_torque1Out;
  TimedDoubleSeq m_torque2;
  OutPort<TimedDoubleSeq> m_torque2Out;
  TimedDoubleSeq m_torque3;
  OutPort<TimedDoubleSeq> m_torque3Out;
  TimedDoubleSeq m_torque4;
  OutPort<TimedDoubleSeq> m_torque4Out;
  TimedDoubleSeq m_torque5;
  OutPort<TimedDoubleSeq> m_torque5Out;
  TimedDoubleSeq m_torque6;
  OutPort<TimedDoubleSeq> m_torque6Out;
  TimedDoubleSeq m_torque7;
  OutPort<TimedDoubleSeq> m_torque7Out;
  TimedDoubleSeq m_torque8;
  OutPort<TimedDoubleSeq> m_torque8Out;
  TimedDoubleSeq m_torque9;
  OutPort<TimedDoubleSeq> m_torque9Out;
  TimedDoubleSeq m_torque10;
  OutPort<TimedDoubleSeq> m_torque10Out;
  TimedDoubleSeq m_torque11;
  OutPort<TimedDoubleSeq> m_torque11Out;
  TimedDoubleSeq m_torque12;
  OutPort<TimedDoubleSeq> m_torque12Out;
  TimedDoubleSeq m_torque13;
  OutPort<TimedDoubleSeq> m_torque13Out;
  TimedDoubleSeq m_torque14;
  OutPort<TimedDoubleSeq> m_torque14Out;
  TimedDoubleSeq m_torque15;
  OutPort<TimedDoubleSeq> m_torque15Out;
  TimedDoubleSeq m_torque16;
  OutPort<TimedDoubleSeq> m_torque16Out;
  
  // </rtc-template>

  // CORBA Port declaration
  // <rtc-template block="corbaport_declare">
  
  // </rtc-template>

  // Service declaration
  // <rtc-template block="service_declare">
  
  // </rtc-template>

  // Consumer declaration
  // <rtc-template block="consumer_declare">
  
  // </rtc-template>

 private:
  int dummy;
  std::ifstream angle, vel, acc, gain;
  double *Pgain;
  double *Dgain;
  std::vector<double> qold;
};


extern "C"
{
DLL_EXPORT void SamplePD_HGInit(RTC::Manager* manager);
};

#endif // SamplePD_HG_H
