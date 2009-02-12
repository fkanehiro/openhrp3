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
 * @file  PA10Controller.cpp
 * @brief Sample PD component
 * $Date$
 *
 * $Id$
 */

#include "PA10Controller.h"

#include <iostream>

#define TIMESTEP 0.001

#define ANGLE_FILE "etc/angle.dat"
#define VEL_FILE   "etc/vel.dat"
#define GAIN_FILE  "etc/PDgain.dat"

// Module specification
// <rtc-template block="module_spec">
static const char* PA10Controller_spec[] =
  {
    "implementation_id", "PA10Controller",
    "type_name",         "PA10Controller",
    "description",       "PA10Controller component",
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

PA10Controller::PA10Controller(RTC::Manager* manager)
  : RTC::DataFlowComponentBase(manager),
    // <rtc-template block="initializer">
    m_angleIn("angle", m_angle),
    m_torqueOut("torque", m_torque),
    
    // </rtc-template>
    dummy(0),
    qold(DOF)
{
  // Registration: InPort/OutPort/Service
  // <rtc-template block="registration">
  // Set InPort buffers
  registerInPort("angle", m_angleIn);
  
  // Set OutPort buffer
  registerOutPort("torque", m_torqueOut);
  
  // Set service provider to Ports
  
  // Set service consumers to Ports
  
  // Set CORBA Service Ports
  
  // </rtc-template>

  Pgain = new double[DOF];
  Dgain = new double[DOF];

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
  m_torque.data.length(DOF);
  m_angle.data.length(DOF);

}

PA10Controller::~PA10Controller()
{
  closeFiles();
  delete [] Pgain;
  delete [] Dgain;
}


RTC::ReturnCode_t PA10Controller::onInitialize()
{
  // <rtc-template block="bind_config">
  // Bind variables and configuration variable

  // </rtc-template>
  return RTC::RTC_OK;
}



/*
RTC::ReturnCode_t PA10Controller::onFinalize()
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t PA10Controller::onStartup(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t PA10Controller::onShutdown(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

RTC::ReturnCode_t PA10Controller::onActivated(RTC::UniqueId ec_id)
{
	std::cout << "on Activated" << std::endl;
    openFiles();
	
	m_angleIn.update();
	
	for(int i=0; i < DOF; ++i){
		qold[i] = m_angle.data[i];
        q_ref[i] = dq_ref[i] = 0.0;
	}
	
	return RTC::RTC_OK;
}

RTC::ReturnCode_t PA10Controller::onDeactivated(RTC::UniqueId ec_id)
{
  std::cout << "on Deactivated" << std::endl;
  closeFiles();
  return RTC::RTC_OK;
}


RTC::ReturnCode_t PA10Controller::onExecute(RTC::UniqueId ec_id)
{
  m_angleIn.update();

  if(!angle.eof()){
	angle >> q_ref[0]; vel >> dq_ref[0];// skip time
	for (int i=0; i<DOF; i++){
		angle >> q_ref[i];
		vel >> dq_ref[i];
	}
  }

  for(int i=0; i<DOF; i++){
    double q = m_angle.data[i];
    double dq = (q - qold[i]) / TIMESTEP;
    qold[i] = q;
    
    m_torque.data[i] = -(q - q_ref[i]) * Pgain[i] - (dq - dq_ref[i]) * Dgain[i];
  }
  
  m_torqueOut.write();
  
  return RTC::RTC_OK;
}


/*
  RTC::ReturnCode_t PA10Controller::onAborting(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t PA10Controller::onError(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t PA10Controller::onReset(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t PA10Controller::onStateUpdate(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t PA10Controller::onRateChanged(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

void PA10Controller::openFiles()
{
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
}

void PA10Controller::closeFiles()
{
    if(angle.is_open()){
        angle.close();
        angle.clear();
    }

    if(vel.is_open()){
        vel.close();
        angle.clear();
    }
}

extern "C"
{
	DLL_EXPORT void PA10ControllerInit(RTC::Manager* manager)
	{
		RTC::Properties profile(PA10Controller_spec);
		manager->registerFactory(profile,
								 RTC::Create<PA10Controller>,
								 RTC::Delete<PA10Controller>);
	}
  
};


