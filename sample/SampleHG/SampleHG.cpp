// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-
/*!
 * @file  SampleHG.cpp
 * @brief Sample LF component
 * $Date$
 *
 * $Id$
 */

#include "SampleHG.h"

#include <iostream>

#define DOF (29)

#define ANGLE_FILE "etc/angle.dat"
#define VEL_FILE   "etc/vel.dat"
#define ACC_FILE   "etc/acc.dat"

namespace {
  const bool CONTROLLER_BRIDGE_DEBUG = false;
}


// Module specification
// <rtc-template block="module_spec">
static const char* samplepd_spec[] =
  {
    "implementation_id", "SampleHG",
    "type_name",         "SampleHG",
    "description",       "Sample HG component",
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

SampleHG::SampleHG(RTC::Manager* manager)
  : RTC::DataFlowComponentBase(manager),
    // <rtc-template block="initializer">
    m_angleOut("angle", m_angle),
    m_velOut("vel", m_vel),
    m_accOut("acc", m_acc)
    
    // </rtc-template>
{
  if( CONTROLLER_BRIDGE_DEBUG )
  {
    std::cout << "SampleHG::SampleHG" << std::endl;
  }
  // Registration: InPort/OutPort/Service
  // <rtc-template block="registration">
  // Set InPort buffers

  // Set OutPort buffer
  registerOutPort("angle", m_angleOut);
  registerOutPort("vel", m_velOut);
  registerOutPort("acc", m_accOut);
  // Set service provider to Ports
  
  // Set service consumers to Ports
  
  // Set CORBA Service Ports
  
  // </rtc-template>

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

  m_angle.data.length(DOF);
  m_vel.data.length(DOF);
  m_acc.data.length(DOF);

}

SampleHG::~SampleHG()
{
	if (angle.is_open())  angle.close();
	if (vel.is_open())    vel.close();
	if (acc.is_open())    acc.close();
}


RTC::ReturnCode_t SampleHG::onInitialize()
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
RTC::ReturnCode_t SampleHG::onFinalize()
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t SampleHG::onStartup(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t SampleHG::onShutdown(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

RTC::ReturnCode_t SampleHG::onActivated(RTC::UniqueId ec_id)
{

	std::cout << "on Activated" << std::endl;
	angle.seekg(0);
	vel.seekg(0);
	acc.seekg(0);
	
	return RTC::RTC_OK;
}

/*
RTC::ReturnCode_t SampleHG::onDeactivated(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/


RTC::ReturnCode_t SampleHG::onExecute(RTC::UniqueId ec_id)
{
  if( CONTROLLER_BRIDGE_DEBUG )
  {
    std::cout << "SampleHG::onExecute" << std::endl;
  }
  // この関数の振る舞いはController_impl::controlの派生先仮想関数に対応する
  double dummy;
  angle >> dummy; vel >> dummy; acc >> dummy; // skip time
  int i;

  //各ファイルからデータを一行読み込んでポートに流す
  for (i=0; i<DOF; i++)
  {
      angle >> m_angle.data[i];
      vel   >> m_vel.data[i];
      acc   >> m_acc.data[i];
  }

  m_angleOut.write();
  m_velOut.write();
  m_accOut.write();

  return RTC::RTC_OK;
}


/*
  RTC::ReturnCode_t SampleHG::onAborting(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t SampleHG::onError(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t SampleHG::onReset(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t SampleHG::onStateUpdate(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t SampleHG::onRateChanged(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/



extern "C"
{

	DllExport void SampleHGInit(RTC::Manager* manager)
	{
		RTC::Properties profile(samplepd_spec);
		manager->registerFactory(profile,
								 RTC::Create<SampleHG>,
								 RTC::Delete<SampleHG>);
	}

};

