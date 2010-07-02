// -*- C++ -*-
/*!
 * @file  SampleSimulationEC.cpp
 * @brief SampleSimulationEC
 * @date $Date$
 *
 * $Id$
 */

#include "SampleSimulationEC.h"

// Module specification
// <rtc-template block="module_spec">
static const char* samplesimulationec_spec[] =
  {
    "implementation_id", "SampleSimulationEC",
    "type_name",         "SampleSimulationEC",
    "description",       "SampleSimulationEC",
    "version",           "1.0.0",
    "vendor",            "VenderName",
    "category",          "Category",
    "activity_type",     "PERIODIC",
    "kind",              "DataFlowComponent",
    "max_instance",      "1",
    "language",          "C++",
    "lang_type",         "compile",
    "exec_cxt.periodic.rate", "10.0",
    ""
  };
// </rtc-template>

/*!
 * @brief constructor
 * @param manager Maneger Object
 */
SampleSimulationEC::SampleSimulationEC(RTC::Manager* manager)
    // <rtc-template block="initializer">
  : RTC::DataFlowComponentBase(manager)

    // </rtc-template>
{
}

/*!
 * @brief destructor
 */
SampleSimulationEC::~SampleSimulationEC()
{
}


/*
RTC::ReturnCode_t SampleSimulationEC::onInitialize()
{
  // Registration: InPort/OutPort/Service
  // <rtc-template block="registration">
  // Set InPort buffers
  
  // Set OutPort buffer
  
  // Set service provider to Ports
  
  // Set service consumers to Ports
  
  // Set CORBA Service Ports
  
  // </rtc-template>

  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t SampleSimulationEC::onFinalize()
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t SampleSimulationEC::onStartup(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t SampleSimulationEC::onShutdown(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/


RTC::ReturnCode_t SampleSimulationEC::onActivated(RTC::UniqueId ec_id)
{
    i = 0;
    return RTC::RTC_OK;
}


/*
RTC::ReturnCode_t SampleSimulationEC::onDeactivated(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/


RTC::ReturnCode_t SampleSimulationEC::onExecute(RTC::UniqueId ec_id)
{
  std::cout << "onExecute  " << i << std::endl;
  i++;
  return RTC::RTC_OK;
}

/*
RTC::ReturnCode_t SampleSimulationEC::onAborting(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t SampleSimulationEC::onError(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t SampleSimulationEC::onReset(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t SampleSimulationEC::onStateUpdate(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t SampleSimulationEC::onRateChanged(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/



extern "C"
{
 
  void SampleSimulationECInit(RTC::Manager* manager)
  {
    coil::Properties profile(samplesimulationec_spec);
    manager->registerFactory(profile,
                             RTC::Create<SampleSimulationEC>,
                             RTC::Delete<SampleSimulationEC>);
  }
  
};


