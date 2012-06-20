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
 * @file  PD_HGtest.cpp
 * @brief Sample PD component
 * $Date$
 *
 * $Id$
 */

#include "PD_HGtest.h"

#include <iostream>

#define DOF (2)
#define TIMESTEP 0.002

#define WAIST_FILE "etc/root.dat"

namespace {
  const bool CONTROLLER_BRIDGE_DEBUG = false;
}


// Module specification
// <rtc-template block="module_spec">
static const char* PD_HGtest_spec[] =
  {
    "implementation_id", "PD_HGtest",
    "type_name",         "PD_HGtest",
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

PD_HGtest::PD_HGtest(RTC::Manager* manager)
  : RTC::DataFlowComponentBase(manager),
    // <rtc-template block="initializer">
    m_torque0Out("torque0", m_torque0),
    m_torque1Out("torque1", m_torque1),
    m_root_transOut("root_trans", m_root_trans),
    m_root_velOut("root_vel", m_root_vel),
    m_root_accOut("root_acc", m_root_acc)
    
{
  if( CONTROLLER_BRIDGE_DEBUG )
  {
    std::cout << "PD_HGtest::PD_HGtest" << std::endl;
  }

  // Registration: InPort/OutPort/Service
  // <rtc-template block="registration">

  // Set service provider to Ports
  
  // Set service consumers to Ports
  
  // Set CORBA Service Ports
  
  // </rtc-template>
}

PD_HGtest::~PD_HGtest()
{
  closeFiles();
}


RTC::ReturnCode_t PD_HGtest::onInitialize()
{
  // <rtc-template block="bind_config">
  // Bind variables and configuration variable
  if( CONTROLLER_BRIDGE_DEBUG )
  {
    std::cout << "onInitialize" << std::endl;
  }

  // Set InPort buffers
  
  // Set OutPort buffer
  addOutPort("torque0", m_torque0Out);
  addOutPort("torque1", m_torque1Out);
  addOutPort("root_trans", m_root_transOut);
  addOutPort("root_vel", m_root_velOut);
  addOutPort("root_acc", m_root_accOut);

  // </rtc-template>

  m_torque0.data.length(1);
  m_torque1.data.length(1);
  m_root_vel.data.length(6);
  m_root_acc.data.length(6);

  return RTC::RTC_OK;
}



/*
RTC::ReturnCode_t PD_HGtest::onFinalize()
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t PD_HGtest::onStartup(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t PD_HGtest::onShutdown(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

RTC::ReturnCode_t PD_HGtest::onActivated(RTC::UniqueId ec_id)
{
  std::cout << "on Activated" << std::endl;
  openFiles();

  return RTC::RTC_OK;
}


RTC::ReturnCode_t PD_HGtest::onDeactivated(RTC::UniqueId ec_id)
{
  std::cout << "on Deactivated" << std::endl;
  closeFiles();
  return RTC::RTC_OK;
}



RTC::ReturnCode_t PD_HGtest::onExecute(RTC::UniqueId ec_id)
{
  if( CONTROLLER_BRIDGE_DEBUG )
  {
    std::cout << "onExecute" << std::endl;
    std::string	localStr;
    std::cin >> localStr; 
  }

  static double root_x_p, root_x_v, root_x_a;
  if(!waist.eof()){
    waist >> root_x_p;  //skip time
    waist >> root_x_a;
    waist >> root_x_v;
    waist >> root_x_p;
  }

  m_torque0.data[0] = 0.0;
  m_torque1.data[0] = 0.0;
  
  m_root_trans.data.position.x = root_x_p;
  m_root_trans.data.position.y = 0;
  m_root_trans.data.position.z = 1;
  m_root_trans.data.orientation.r = 0;
  m_root_trans.data.orientation.p = 0;
  m_root_trans.data.orientation.y = 0;
  for(int i=0; i<6; i++)
    m_root_vel.data[i] = 0.0;
  m_root_vel.data[0] = root_x_v;
  for(int i=0; i<6; i++)
    m_root_acc.data[i] = 0.0;
  m_root_acc.data[0] = root_x_a;

  m_torque0Out.write();
  m_torque1Out.write();
  m_root_transOut.write();
  m_root_velOut.write();
  m_root_accOut.write();

  return RTC::RTC_OK;
}


/*
  RTC::ReturnCode_t PD_HGtest::onAborting(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t PD_HGtest::onError(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t PD_HGtest::onReset(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t PD_HGtest::onStateUpdate(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t PD_HGtest::onRateChanged(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

void PD_HGtest::openFiles()
{
  waist.open(WAIST_FILE);
  if (!waist.is_open())
  {
    std::cerr << WAIST_FILE << " not opened" << std::endl;
  }
}

void PD_HGtest::closeFiles()
{
  if(waist.is_open()){
    waist.close();
    waist.clear();
  }
}


extern "C"
{

  DLL_EXPORT void PD_HGtestInit(RTC::Manager* manager)
  {
    coil::Properties profile(PD_HGtest_spec);
    manager->registerFactory(profile,
                             RTC::Create<PD_HGtest>,
                             RTC::Delete<PD_HGtest>);
  }

};

