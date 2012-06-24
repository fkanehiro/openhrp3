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
 * @file  SampleCrawler.cpp
 * @brief Sample PD component
 * $Date$
 *
 * $Id$
 */

#include "SampleCrawler.h"

#include <iostream>

namespace {
  const bool CONTROLLER_BRIDGE_DEBUG = false;
}


// Module specification
// <rtc-template block="module_spec">
static const char* samplepd_spec[] =
  {
    "implementation_id", "SampleCrawler",
    "type_name",         "SampleCrawler",
    "description",       "Sample Crawler component",
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

SampleCrawler::SampleCrawler(RTC::Manager* manager)
  : RTC::DataFlowComponentBase(manager),
    // <rtc-template block="initializer">
    m_torqueOut("torque", m_torque),
    
    // </rtc-template>
    dummy(0)
{
  if( CONTROLLER_BRIDGE_DEBUG )
  {
    std::cout << "SampleCrawler::SampleCrawler" << std::endl;
  }

  // Registration: InPort/OutPort/Service
  // <rtc-template block="registration">
  
  // Set service provider to Ports
  
  // Set service consumers to Ports
  
  // Set CORBA Service Ports
  
  // </rtc-template>
}

SampleCrawler::~SampleCrawler()
{
}


RTC::ReturnCode_t SampleCrawler::onInitialize()
{
  // <rtc-template block="bind_config">
  // Bind variables and configuration variable
  if( CONTROLLER_BRIDGE_DEBUG )
  {
    std::cout << "onInitialize" << std::endl;
  }

  // Set InPort buffers
  
  // Set OutPort buffer
  addOutPort("torque", m_torqueOut);
  // </rtc-template>

  m_torque.data.length(DOF);

  return RTC::RTC_OK;
}


/*
RTC::ReturnCode_t SampleCrawler::onFinalize()
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t SampleCrawler::onStartup(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t SampleCrawler::onShutdown(RTC::UniqueId ec_id)
{
    log("SampleCrawler::onShutdown");
  return RTC::RTC_OK;
}
*/

RTC::ReturnCode_t SampleCrawler::onActivated(RTC::UniqueId ec_id)
{
  std::cout << "onActivated" << std::endl;
  cnt = 0;
  return RTC::RTC_OK;
}


RTC::ReturnCode_t SampleCrawler::onDeactivated(RTC::UniqueId ec_id)
{
  std::cout << "onDeactivated" << std::endl;
  return RTC::RTC_OK;
}



RTC::ReturnCode_t SampleCrawler::onExecute(RTC::UniqueId ec_id)
{
  if (cnt < 500){
    m_torque.data[0] = m_torque.data[1] = 0.0;	
  }else if (cnt < 1900){
    m_torque.data[0] = m_torque.data[1] = 1.0;	
  }else if (cnt < 2100){
    m_torque.data[0] = 1.0; m_torque.data[1] = -1.0;	
  }else{
    m_torque.data[0] = m_torque.data[1] = -1.0;	
  }
  m_torqueOut.write();
  cnt++;
  
  return RTC::RTC_OK;
}

/*
RTC::ReturnCode_t SampleCrawler::onAborting(RTC::UniqueId ec_id)
{
    return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t SampleCrawler::onError(RTC::UniqueId ec_id)
{
    return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t SampleCrawler::onReset(RTC::UniqueId ec_id)
{
    return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t SampleCrawler::onStateUpdate(RTC::UniqueId ec_id)
{
    return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t SampleCrawler::onRateChanged(RTC::UniqueId ec_id)
{
    return RTC::RTC_OK;
}
*/

extern "C"
{

  DLL_EXPORT void SampleCrawlerInit(RTC::Manager* manager)
  {
    coil::Properties profile(samplepd_spec);
    manager->registerFactory(profile,
                             RTC::Create<SampleCrawler>,
                             RTC::Delete<SampleCrawler>);
  }

};

