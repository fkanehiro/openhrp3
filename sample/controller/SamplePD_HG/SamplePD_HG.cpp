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

#define PD_DOF (17)
#define HG_DOF (12)
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
    m_torqueOut("torque", m_torque),
    m_angle_outOut("angle_out", m_angle_out),
    m_velOut("vel", m_vel),
    m_accOut("acc", m_acc),
      
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
  
  // Set service provider to Ports
  
  // Set service consumers to Ports
  
  // Set CORBA Service Ports
  
  // </rtc-template>
}

SamplePD_HG::~SamplePD_HG()
{
  closeFiles();
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

  // Set InPort buffers
  addInPort("angle_in", m_angle_inIn);
  
  // Set OutPort buffer
  addOutPort("torque", m_torqueOut);
  addOutPort("angle_out", m_angle_outOut);
  addOutPort("vel", m_velOut);
  addOutPort("acc", m_accOut);

  Pgain = new double[DOF];
  Dgain = new double[DOF];

  gain.open(GAIN_FILE);
  if (gain.is_open()){
    for (int i=0; i<DOF; i++){
      gain >> Pgain[i];
      gain >> Dgain[i];
    }
    gain.close();
  }else{
    std::cerr << GAIN_FILE << " not opened" << std::endl;
  }
  // </rtc-template>

  m_angle_in.data.length(DOF);
  m_angle_out.data.length(HG_DOF);
  m_vel.data.length(HG_DOF);
  m_acc.data.length(HG_DOF);
  m_torque.data.length(PD_DOF);

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
  openFiles();

  if(m_angle_inIn.isNew()){
    m_angle_inIn.read();
  }

  for(int i=0; i < DOF; ++i){
    qold[i] = m_angle_in.data[i];
    q_ref[i] = dq_ref[i] = ddq_ref[i] = 0.0;
  }

  return RTC::RTC_OK;
}


RTC::ReturnCode_t SamplePD_HG::onDeactivated(RTC::UniqueId ec_id)
{
  std::cout << "on Deactivated" << std::endl;
  closeFiles();
  return RTC::RTC_OK;
}



RTC::ReturnCode_t SamplePD_HG::onExecute(RTC::UniqueId ec_id)
{
  if( CONTROLLER_BRIDGE_DEBUG )
  {
    std::cout << "onExecute" << std::endl;
    std::string	localStr;
    std::cin >> localStr; 
  }

  if(m_angle_inIn.isNew()){
    m_angle_inIn.read();
  }

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

  m_torque.data[0] = tor_ref[WAIST_P];
  m_torque.data[1] = tor_ref[WAIST_R];
  m_torque.data[2] = tor_ref[CHEST];
  m_torque.data[3] = tor_ref[LARM_SHOULDER_P];
  m_torque.data[4] = tor_ref[LARM_SHOULDER_R];
  m_torque.data[5] = tor_ref[LARM_SHOULDER_Y];
  m_torque.data[6] = tor_ref[LARM_ELBOW];
  m_torque.data[7] = tor_ref[LARM_WRIST_Y];
  m_torque.data[8] = tor_ref[LARM_WRIST_P];
  m_torque.data[9] = tor_ref[LARM_WRIST_R];
  m_torque.data[10] = tor_ref[RARM_SHOULDER_P];
  m_torque.data[11] = tor_ref[RARM_SHOULDER_R];
  m_torque.data[12] = tor_ref[RARM_SHOULDER_Y];
  m_torque.data[13] = tor_ref[RARM_ELBOW];
  m_torque.data[14] = tor_ref[RARM_WRIST_Y];
  m_torque.data[15] = tor_ref[RARM_WRIST_P];
  m_torque.data[16] = tor_ref[RARM_WRIST_R];

  m_angle_out.data[0] = q_ref[LLEG_HIP_R];
  m_angle_out.data[1] = q_ref[LLEG_HIP_P];
  m_angle_out.data[2] = q_ref[LLEG_HIP_Y];
  m_angle_out.data[3] = q_ref[LLEG_KNEE];
  m_angle_out.data[4] = q_ref[LLEG_ANKLE_P];
  m_angle_out.data[5] = q_ref[LLEG_ANKLE_R];
  m_angle_out.data[6] = q_ref[RLEG_HIP_R];
  m_angle_out.data[7] = q_ref[RLEG_HIP_P];
  m_angle_out.data[8] = q_ref[RLEG_HIP_Y];
  m_angle_out.data[9] = q_ref[RLEG_KNEE];
  m_angle_out.data[10] = q_ref[RLEG_ANKLE_P];
  m_angle_out.data[11] = q_ref[RLEG_ANKLE_R];
  m_vel.data[0] = dq_ref[LLEG_HIP_R];
  m_vel.data[1] = dq_ref[LLEG_HIP_P];
  m_vel.data[2] = dq_ref[LLEG_HIP_Y];
  m_vel.data[3] = dq_ref[LLEG_KNEE];
  m_vel.data[4] = dq_ref[LLEG_ANKLE_P];
  m_vel.data[5] = dq_ref[LLEG_ANKLE_R];
  m_vel.data[6] = dq_ref[RLEG_HIP_R];
  m_vel.data[7] = dq_ref[RLEG_HIP_P];
  m_vel.data[8] = dq_ref[RLEG_HIP_Y];
  m_vel.data[9] = dq_ref[RLEG_KNEE];
  m_vel.data[10] = dq_ref[RLEG_ANKLE_P];
  m_vel.data[11] = dq_ref[RLEG_ANKLE_R];
  m_acc.data[0] = ddq_ref[LLEG_HIP_R];
  m_acc.data[1] = ddq_ref[LLEG_HIP_P];
  m_acc.data[2] = ddq_ref[LLEG_HIP_Y];
  m_acc.data[3] = ddq_ref[LLEG_KNEE];
  m_acc.data[4] = ddq_ref[LLEG_ANKLE_P];
  m_acc.data[5] = ddq_ref[LLEG_ANKLE_R];
  m_acc.data[6] = ddq_ref[RLEG_HIP_R];
  m_acc.data[7] = ddq_ref[RLEG_HIP_P];
  m_acc.data[8] = ddq_ref[RLEG_HIP_Y];
  m_acc.data[9] = ddq_ref[RLEG_KNEE];
  m_acc.data[10] = ddq_ref[RLEG_ANKLE_P];
  m_acc.data[11] = ddq_ref[RLEG_ANKLE_R];

  m_torqueOut.write();
  m_angle_outOut.write();
  m_velOut.write();
  m_accOut.write();

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

void SamplePD_HG::openFiles()
{
  angle.open(ANGLE_FILE);
  if (!angle.is_open()){
    std::cerr << ANGLE_FILE << " not opened" << std::endl;
  }

  vel.open(VEL_FILE);
  if (!vel.is_open()){
    std::cerr << VEL_FILE << " not opened" << std::endl;
  }

  acc.open(ACC_FILE);
  if (!acc.is_open())
  {
    std::cerr << ACC_FILE << " not opend" << std::endl;
  }
}

void SamplePD_HG::closeFiles()
{
    if( angle.is_open() ){
        angle.close();
        angle.clear();
    }
    if( vel.is_open() ){
        vel.close();
        vel.clear();
    }
    if( acc.is_open() ){
        acc.close();
        acc.clear();
    }
}


extern "C"
{

  DLL_EXPORT void SamplePD_HGInit(RTC::Manager* manager)
  {
    coil::Properties profile(SamplePD_HG_spec);
    manager->registerFactory(profile,
                             RTC::Create<SamplePD_HG>,
                             RTC::Delete<SamplePD_HG>);
  }

};

