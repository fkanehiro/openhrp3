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
 * @file  SamplePD_HG.cpp
 * @brief Sample PD component
 * $Date$
 *
 * $Id$
 */

#include "SamplePD_HG.h"

#include <iostream>

#define DOF (29)
#define TIMESTEP 0.002

#define WAIST_P             26
#define WAIST_R             27
#define CHEST               28
#define LARM_SHOULDER_P     19
#define LARM_SHOULDER_R     20
#define LARM_SHOULDER_Y     21  
#define LARM_ELBOW          22
#define LARM_WRIST_Y        23
#define LARM_WRIST_P        24  
#define LARM_WRIST_R        25
#define RARM_SHOULDER_P     6
#define RARM_SHOULDER_R     7
#define RARM_SHOULDER_Y     8
#define RARM_ELBOW          9
#define RARM_WRIST_Y        10
#define RARM_WRIST_P        11  
#define RARM_WRIST_R        12
#define LLEG_HIP_R          13
#define LLEG_HIP_P          14
#define LLEG_HIP_Y          15
#define LLEG_KNEE           16
#define LLEG_ANKLE_P        17
#define LLEG_ANKLE_R        18
#define RLEG_HIP_R          0
#define RLEG_HIP_P          1   
#define RLEG_HIP_Y          2
#define RLEG_KNEE           3
#define RLEG_ANKLE_P        4
#define RLEG_ANKLE_R        5

#define ANGLE_FILE "etc/angle.dat"
#define VEL_FILE   "etc/vel.dat"
#define ACC_FILE   "etc/acc.dat"

#define GAIN_FILE  "etc/PDgain.dat"

namespace {
  const bool CONTROLLER_BRIDGE_DEBUG = false;
}


// Module specification
// <rtc-template block="module_spec">
static const char* SamplePD_HG_spec[] =
  {
    "implementation_id", "SamplePD_HG",
    "type_name",         "SamplePD_HG",
    "description",       "Sample PD component",
    "version",           "0.1",
    "vendor",            "AIST",
    "category",          "Generic",
    "activity_type",     "DataFlowComponent",
    "max_instance",      "10",
    "language",          "C++",
    "lang_type",         "compile",
    // Configuration variables

    ""
  };
// </rtc-template>

SamplePD_HG::SamplePD_HG(RTC::Manager* manager)
  : RTC::DataFlowComponentBase(manager),
    // <rtc-template block="initializer">
    m_angle_inIn("angle_in", m_angle_in),
    m_torque0Out("torque0", m_torque0),
    m_torque1Out("torque1", m_torque1),
    m_torque2Out("torque2", m_torque2),
    m_torque3Out("torque3", m_torque3),
    m_torque4Out("torque4", m_torque4),
    m_torque5Out("torque5", m_torque5),
    m_torque6Out("torque6", m_torque6),
    m_torque7Out("torque7", m_torque7),
    m_torque8Out("torque8", m_torque8),
    m_torque9Out("torque9", m_torque9),
    m_torque10Out("torque10", m_torque10),
    m_torque11Out("torque11", m_torque11),
    m_torque12Out("torque12", m_torque12),
    m_torque13Out("torque13", m_torque13),
    m_torque14Out("torque14", m_torque14),
    m_torque15Out("torque15", m_torque15),
    m_torque16Out("torque16", m_torque16),
    m_angle_out0Out("angle_out0", m_angle_out0),
    m_angle_out1Out("angle_out1", m_angle_out1),
    m_angle_out2Out("angle_out2", m_angle_out2),
    m_angle_out3Out("angle_out3", m_angle_out3),
    m_angle_out4Out("angle_out4", m_angle_out4),
    m_angle_out5Out("angle_out5", m_angle_out5),
    m_angle_out6Out("angle_out6", m_angle_out6),
    m_angle_out7Out("angle_out7", m_angle_out7),
    m_angle_out8Out("angle_out8", m_angle_out8),
    m_angle_out9Out("angle_out9", m_angle_out9),
    m_angle_out10Out("angle_out10", m_angle_out10),
    m_angle_out11Out("angle_out11", m_angle_out11),
    m_vel0Out("vel0", m_vel0),
    m_vel1Out("vel1", m_vel1),
    m_vel2Out("vel2", m_vel2),
    m_vel3Out("vel3", m_vel3),
    m_vel4Out("vel4", m_vel4),
    m_vel5Out("vel5", m_vel5),
    m_vel6Out("vel6", m_vel6),
    m_vel7Out("vel7", m_vel7),
    m_vel8Out("vel8", m_vel8),
    m_vel9Out("vel9", m_vel9),
    m_vel10Out("vel10", m_vel10),
    m_vel11Out("vel11", m_vel11),
    m_acc0Out("acc0", m_acc0),
    m_acc1Out("acc1", m_acc1),
    m_acc2Out("acc2", m_acc2),
    m_acc3Out("acc3", m_acc3),
    m_acc4Out("acc4", m_acc4),
    m_acc5Out("acc5", m_acc5),
    m_acc6Out("acc6", m_acc6),
    m_acc7Out("acc7", m_acc7),
    m_acc8Out("acc8", m_acc8),
    m_acc9Out("acc9", m_acc9),
    m_acc10Out("acc10", m_acc10),
    m_acc11Out("acc11", m_acc11),
    
    // </rtc-template>
    dummy(0),
    qold(DOF)
{
  if( CONTROLLER_BRIDGE_DEBUG )
  {
    std::cout << "SamplePD_HG::SamplePD_HG" << std::endl;
  }

  // Registration: InPort/OutPort/Service
  // <rtc-template block="registration">
  // Set InPort buffers
  registerInPort("angle_in", m_angle_inIn);
  
  // Set OutPort buffer
  registerOutPort("torque0", m_torque0Out);
  registerOutPort("torque1", m_torque1Out);
  registerOutPort("torque2", m_torque2Out);
  registerOutPort("torque3", m_torque3Out);
  registerOutPort("torque4", m_torque4Out);
  registerOutPort("torque5", m_torque5Out);
  registerOutPort("torque6", m_torque6Out);
  registerOutPort("torque7", m_torque7Out);
  registerOutPort("torque8", m_torque8Out);
  registerOutPort("torque9", m_torque9Out);
  registerOutPort("torque10", m_torque10Out);
  registerOutPort("torque11", m_torque11Out);
  registerOutPort("torque12", m_torque12Out);
  registerOutPort("torque13", m_torque13Out);
  registerOutPort("torque14", m_torque14Out);
  registerOutPort("torque15", m_torque15Out);
  registerOutPort("torque16", m_torque16Out);
  registerOutPort("angle_out0", m_angle_out0Out);
  registerOutPort("angle_out1", m_angle_out1Out);
  registerOutPort("angle_out2", m_angle_out2Out);
  registerOutPort("angle_out3", m_angle_out3Out);
  registerOutPort("angle_out4", m_angle_out4Out);
  registerOutPort("angle_out5", m_angle_out5Out);
  registerOutPort("angle_out6", m_angle_out6Out);
  registerOutPort("angle_out7", m_angle_out7Out);
  registerOutPort("angle_out8", m_angle_out8Out);
  registerOutPort("angle_out9", m_angle_out9Out);
  registerOutPort("angle_out10", m_angle_out10Out);
  registerOutPort("angle_out11", m_angle_out11Out);
  registerOutPort("vel0", m_vel0Out);
  registerOutPort("vel1", m_vel1Out);
  registerOutPort("vel2", m_vel2Out);
  registerOutPort("vel3", m_vel3Out);
  registerOutPort("vel4", m_vel4Out);
  registerOutPort("vel5", m_vel5Out);
  registerOutPort("vel6", m_vel6Out);
  registerOutPort("vel7", m_vel7Out);
  registerOutPort("vel8", m_vel8Out);
  registerOutPort("vel9", m_vel9Out);
  registerOutPort("vel10", m_vel10Out);
  registerOutPort("vel11", m_vel11Out);
  registerOutPort("acc0", m_acc0Out);
  registerOutPort("acc1", m_acc1Out);
  registerOutPort("acc2", m_acc2Out);
  registerOutPort("acc3", m_acc3Out);
  registerOutPort("acc4", m_acc4Out);
  registerOutPort("acc5", m_acc5Out);
  registerOutPort("acc6", m_acc6Out);
  registerOutPort("acc7", m_acc7Out);
  registerOutPort("acc8", m_acc8Out);
  registerOutPort("acc9", m_acc9Out);
  registerOutPort("acc10", m_acc10Out);
  registerOutPort("acc11", m_acc11Out);

  // Set service provider to Ports
  
  // Set service consumers to Ports
  
  // Set CORBA Service Ports
  
  // </rtc-template>

  Pgain = new double[DOF];
  Dgain = new double[DOF];

  if (access(ANGLE_FILE, 0)){
    std::cerr << ANGLE_FILE << " not found" << std::endl;
  }else{
    angle.open(ANGLE_FILE);
  }

  if (access(VEL_FILE, 0)){
    std::cerr << VEL_FILE << " not found" << std::endl;
  }else{
    vel.open(VEL_FILE);
  }

   if (access(ACC_FILE, 0))
  {
    std::cerr << ACC_FILE << " not found" << std::endl;
  }else{
    acc.open(ACC_FILE);
  }

  if (access(GAIN_FILE, 0)){
    std::cerr << GAIN_FILE << " not found" << std::endl;
  }else{
    gain.open(GAIN_FILE);
    for (int i=0; i<DOF; i++){
      gain >> Pgain[i];
      gain >> Dgain[i];
    }
    gain.close();
  }
  
  m_angle_in.data.length(DOF);
  m_angle_out0.data.length(1);
  m_angle_out1.data.length(1);
  m_angle_out2.data.length(1);
  m_angle_out3.data.length(1);
  m_angle_out4.data.length(1);
  m_angle_out5.data.length(1);
  m_angle_out6.data.length(1);
  m_angle_out7.data.length(1);
  m_angle_out8.data.length(1);
  m_angle_out9.data.length(1);
  m_angle_out10.data.length(1);
  m_angle_out11.data.length(1);
  m_vel0.data.length(1);
  m_vel1.data.length(1);
  m_vel2.data.length(1);
  m_vel3.data.length(1);
  m_vel4.data.length(1);
  m_vel5.data.length(1);
  m_vel6.data.length(1);
  m_vel7.data.length(1);
  m_vel8.data.length(1);
  m_vel9.data.length(1);
  m_vel10.data.length(1);
  m_vel11.data.length(1);
  m_acc0.data.length(1);
  m_acc1.data.length(1);
  m_acc2.data.length(1);
  m_acc3.data.length(1);
  m_acc4.data.length(1);
  m_acc5.data.length(1);
  m_acc6.data.length(1);
  m_acc7.data.length(1);
  m_acc8.data.length(1);
  m_acc9.data.length(1);
  m_acc10.data.length(1);
  m_acc11.data.length(1);
  m_torque0.data.length(1);
  m_torque1.data.length(1);
  m_torque2.data.length(1);
  m_torque3.data.length(1);
  m_torque4.data.length(1);
  m_torque5.data.length(1);
  m_torque6.data.length(1);
  m_torque7.data.length(1);
  m_torque8.data.length(1);
  m_torque9.data.length(1);
  m_torque10.data.length(1);
  m_torque11.data.length(1);
  m_torque12.data.length(1);
  m_torque13.data.length(1);
  m_torque14.data.length(1);
  m_torque15.data.length(1);
  m_torque16.data.length(1);

}

SamplePD_HG::~SamplePD_HG()
{
  if (angle.is_open()) angle.close();
  if (vel.is_open()) vel.close();
  if (acc.is_open())    acc.close();
  delete [] Pgain;
  delete [] Dgain;
}


RTC::ReturnCode_t SamplePD_HG::onInitialize()
{
  // <rtc-template block="bind_config">
  // Bind variables and configuration variable
  if( CONTROLLER_BRIDGE_DEBUG )
  {
    std::cout << "onInitialize" << std::endl;
  }
  // </rtc-template>
  return RTC::RTC_OK;
}



/*
RTC::ReturnCode_t SamplePD_HG::onFinalize()
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t SamplePD_HG::onStartup(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t SamplePD_HG::onShutdown(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

RTC::ReturnCode_t SamplePD_HG::onActivated(RTC::UniqueId ec_id)
{
	std::cout << "on Activated" << std::endl;
	angle.seekg(0);
	vel.seekg(0);
   	acc.seekg(0);
	
	m_angle_inIn.update();
	
	for(int i=0; i < DOF; ++i){
		qold[i] = m_angle_in.data[i];
	}
	
	return RTC::RTC_OK;
}

/*
RTC::ReturnCode_t SamplePD_HG::onDeactivated(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/


RTC::ReturnCode_t SamplePD_HG::onExecute(RTC::UniqueId ec_id)
{
  if( CONTROLLER_BRIDGE_DEBUG )
  {
    std::cout << "onExecute" << std::endl;
		std::string	localStr;
		std::cin >> localStr; 
  }

  m_angle_inIn.update();

  static double q_ref[DOF], dq_ref[DOF], ddq_ref[DOF];
  if(!angle.eof()){
	angle >> q_ref[0]; vel >> dq_ref[0]; acc >> ddq_ref[0];// skip time
	for (int i=0; i<DOF; i++){
		angle >> q_ref[i];
		vel >> dq_ref[i];
        acc >> ddq_ref[i];
	}
  }

  double tor_ref[DOF];
  for(int i=0; i<DOF; i++){
    double q = m_angle_in.data[i];
    double dq = (q - qold[i]) / TIMESTEP;
    qold[i] = q;
    
    tor_ref[i] = -(q - q_ref[i]) * Pgain[i] - (dq - dq_ref[i]) * Dgain[i];
  }

  m_torque0.data[0] = tor_ref[WAIST_P];
  m_torque1.data[0] = tor_ref[WAIST_R];
  m_torque2.data[0] = tor_ref[CHEST];
  m_torque3.data[0] = tor_ref[LARM_SHOULDER_P];
  m_torque4.data[0] = tor_ref[LARM_SHOULDER_R];
  m_torque5.data[0] = tor_ref[LARM_SHOULDER_Y];
  m_torque6.data[0] = tor_ref[LARM_ELBOW];
  m_torque7.data[0] = tor_ref[LARM_WRIST_Y];
  m_torque8.data[0] = tor_ref[LARM_WRIST_P];
  m_torque9.data[0] = tor_ref[LARM_WRIST_R];
  m_torque10.data[0] = tor_ref[RARM_SHOULDER_P];
  m_torque11.data[0] = tor_ref[RARM_SHOULDER_R];
  m_torque12.data[0] = tor_ref[RARM_SHOULDER_Y];
  m_torque13.data[0] = tor_ref[RARM_ELBOW];
  m_torque14.data[0] = tor_ref[RARM_WRIST_Y];
  m_torque15.data[0] = tor_ref[RARM_WRIST_P];
  m_torque16.data[0] = tor_ref[RARM_WRIST_R];

  m_angle_out0.data[0] = q_ref[LLEG_HIP_R];
  m_angle_out1.data[0] = q_ref[LLEG_HIP_P];
  m_angle_out2.data[0] = q_ref[LLEG_HIP_Y];
  m_angle_out3.data[0] = q_ref[LLEG_KNEE];
  m_angle_out4.data[0] = q_ref[LLEG_ANKLE_P];
  m_angle_out5.data[0] = q_ref[LLEG_ANKLE_R];
  m_angle_out6.data[0] = q_ref[RLEG_HIP_R];
  m_angle_out7.data[0] = q_ref[RLEG_HIP_P];
  m_angle_out8.data[0] = q_ref[RLEG_HIP_Y];
  m_angle_out9.data[0] = q_ref[RLEG_KNEE];
  m_angle_out10.data[0] = q_ref[RLEG_ANKLE_P];
  m_angle_out11.data[0] = q_ref[RLEG_ANKLE_R];
  m_vel0.data[0] = dq_ref[LLEG_HIP_R];
  m_vel1.data[0] = dq_ref[LLEG_HIP_P];
  m_vel2.data[0] = dq_ref[LLEG_HIP_Y];
  m_vel3.data[0] = dq_ref[LLEG_KNEE];
  m_vel4.data[0] = dq_ref[LLEG_ANKLE_P];
  m_vel5.data[0] = dq_ref[LLEG_ANKLE_R];
  m_vel6.data[0] = dq_ref[RLEG_HIP_R];
  m_vel7.data[0] = dq_ref[RLEG_HIP_P];
  m_vel8.data[0] = dq_ref[RLEG_HIP_Y];
  m_vel9.data[0] = dq_ref[RLEG_KNEE];
  m_vel10.data[0] = dq_ref[RLEG_ANKLE_P];
  m_vel11.data[0] = dq_ref[RLEG_ANKLE_R];
  m_acc0.data[0] = ddq_ref[LLEG_HIP_R];
  m_acc1.data[0] = ddq_ref[LLEG_HIP_P];
  m_acc2.data[0] = ddq_ref[LLEG_HIP_Y];
  m_acc3.data[0] = ddq_ref[LLEG_KNEE];
  m_acc4.data[0] = ddq_ref[LLEG_ANKLE_P];
  m_acc5.data[0] = ddq_ref[LLEG_ANKLE_R];
  m_acc6.data[0] = ddq_ref[RLEG_HIP_R];
  m_acc7.data[0] = ddq_ref[RLEG_HIP_P];
  m_acc8.data[0] = ddq_ref[RLEG_HIP_Y];
  m_acc9.data[0] = ddq_ref[RLEG_KNEE];
  m_acc10.data[0] = ddq_ref[RLEG_ANKLE_P];
  m_acc11.data[0] = ddq_ref[RLEG_ANKLE_R];

  m_torque0Out.write();
  m_torque1Out.write();
  m_torque2Out.write();
  m_torque3Out.write();
  m_torque4Out.write();
  m_torque5Out.write();
  m_torque6Out.write();
  m_torque7Out.write();
  m_torque8Out.write();
  m_torque9Out.write();
  m_torque10Out.write();
  m_torque11Out.write();
  m_torque12Out.write();
  m_torque13Out.write();
  m_torque14Out.write();
  m_torque15Out.write();
  m_torque16Out.write();

  m_angle_out0Out.write();
  m_angle_out1Out.write();
  m_angle_out2Out.write();
  m_angle_out3Out.write();
  m_angle_out4Out.write();
  m_angle_out5Out.write();
  m_angle_out6Out.write();
  m_angle_out7Out.write();
  m_angle_out8Out.write();
  m_angle_out9Out.write();
  m_angle_out10Out.write();
  m_angle_out11Out.write();

  m_vel0Out.write();
  m_vel1Out.write();
  m_vel2Out.write();
  m_vel3Out.write();
  m_vel4Out.write();
  m_vel5Out.write();
  m_vel6Out.write();
  m_vel7Out.write();
  m_vel8Out.write();
  m_vel9Out.write();
  m_vel10Out.write();
  m_vel11Out.write();

  m_acc0Out.write();
  m_acc1Out.write();
  m_acc2Out.write();
  m_acc3Out.write();
  m_acc4Out.write();
  m_acc5Out.write();
  m_acc6Out.write();
  m_acc7Out.write();
  m_acc8Out.write();
  m_acc9Out.write();
  m_acc10Out.write();
  m_acc11Out.write();
  

  return RTC::RTC_OK;
}


/*
  RTC::ReturnCode_t SamplePD_HG::onAborting(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t SamplePD_HG::onError(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t SamplePD_HG::onReset(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t SamplePD_HG::onStateUpdate(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t SamplePD_HG::onRateChanged(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/



extern "C"
{

	DLL_EXPORT void SamplePD_HGInit(RTC::Manager* manager)
	{
		RTC::Properties profile(SamplePD_HG_spec);
		manager->registerFactory(profile,
								 RTC::Create<SamplePD_HG>,
								 RTC::Delete<SamplePD_HG>);
	}

};

