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
 * @file  SampleRH2.cpp
 * @brief Sample RH2 component
 * $Date$
 *
 * $Id$
 */

#include "SampleRH2.h"

#include <iostream>
#include <hrpUtil/Tvmet3d.h>

#define ROOT_FILE "etc/body.dat"

namespace {
  const bool CONTROLLER_BRIDGE_DEBUG = false;
}


// Module specification
// <rtc-template block="module_spec">
static const char* samplepd_spec[] =
  {
    "implementation_id", "SampleRH2",
    "type_name",         "SampleRH2",
    "description",       "Sample RH2 component",
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

SampleRH2::SampleRH2(RTC::Manager* manager)
  : RTC::DataFlowComponentBase(manager),
    // <rtc-template block="initializer">
    m_root_transOut("root_trans", m_root_trans)
    // </rtc-template>
{
  if( CONTROLLER_BRIDGE_DEBUG )
  {
    std::cout << "SampleRH2::SampleRH2" << std::endl;
  }
  // Registration: InPort/OutPort/Service
  // <rtc-template block="registration">

  // Set service provider to Ports
  
  // Set service consumers to Ports
  
  // Set CORBA Service Ports
  
  // </rtc-template>
}

SampleRH2::~SampleRH2()
{
    closeFiles();
}


RTC::ReturnCode_t SampleRH2::onInitialize()
{
  // <rtc-template block="bind_config">
  // Bind variables and configuration variable
  if( CONTROLLER_BRIDGE_DEBUG )
  {
    std::cout << "onInitialize" << std::endl;
  }

  // Set InPort buffers

  // Set OutPort buffer
  addOutPort("root_trans", m_root_transOut);
  // </rtc-template>

  m_root_trans.data.length(12);

  return RTC::RTC_OK;
}



/*
RTC::ReturnCode_t SampleRH2::onFinalize()
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t SampleRH2::onStartup(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t SampleRH2::onShutdown(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

RTC::ReturnCode_t SampleRH2::onActivated(RTC::UniqueId ec_id)
{

  std::cout << "on Activated" << std::endl;
  openFiles();
 // for(int i = 0; i < DOF; ++i){
 //   angle_ref[i] = vel_ref[i] = acc_ref[i] = 0.0;
 // }

  return RTC::RTC_OK;
}


RTC::ReturnCode_t SampleRH2::onDeactivated(RTC::UniqueId ec_id)
{
  std::cout << "on Deactivated" << std::endl;
  closeFiles();
  return RTC::RTC_OK;
}


RTC::ReturnCode_t SampleRH2::onExecute(RTC::UniqueId ec_id)
{
  if( CONTROLLER_BRIDGE_DEBUG )
  {
    std::cout << "SampleRH2::onExecute" << std::endl;
  }
  
  if(!root.eof()){
    double time;
    root >> time;
    for(int i=0; i<3; i++)
        root >> m_root_trans.data[i]; 
    double rotation[4];
    for(int i=0; i<4; i++)
        root >> rotation[i];
    hrp::Matrix33 T;
    hrp::calcRodrigues(T, hrp::Vector3(rotation[0], rotation[1], rotation[2]), rotation[3]);
    for(int i=0; i<9; i++)
        m_root_trans.data[i+3] = T(i/3,i%3);
  }

  m_root_transOut.write();
 
  return RTC::RTC_OK;
}


/*
  RTC::ReturnCode_t SampleRH2::onAborting(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t SampleRH2::onError(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t SampleRH2::onReset(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t SampleRH2::onStateUpdate(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t SampleRH2::onRateChanged(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

void SampleRH2::openFiles()
{
 root.open(ROOT_FILE);
  if (!root.is_open())
  {
    std::cerr << ROOT_FILE << " not opened" << std::endl;
  }
}

void SampleRH2::closeFiles()
{
  if(root.is_open()){
    root.close();
    root.clear();
  }
}


extern "C"
{

  DLL_EXPORT void SampleRH2Init(RTC::Manager* manager)
  {
    coil::Properties profile(samplepd_spec);
    manager->registerFactory(profile,
                             RTC::Create<SampleRH2>,
                             RTC::Delete<SampleRH2>);
  }

};

