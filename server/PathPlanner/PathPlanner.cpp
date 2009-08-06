// -*- C++ -*-
/*!
 * @file  Path.cpp * @brief Path Planner Component * $Date$ 
 *
 * $Id$ 
 */
#include "PathPlanner.h"

// Module specification
// <rtc-template block="module_spec">
static const char* path_spec[] =
  {
    "implementation_id", "Path",
    "type_name",         "Path",
    "description",       "Path Planner Component",
    "version",           "0.1",
    "vendor",            "S-cubed, Inc.",
    "category",          "Generic",
    "activity_type",     "SPORADIC",
    "kind",              "DataFlowComponent",
    "max_instance",      "10",
    "language",          "C++",
    "lang_type",         "compile",
    // Configuration variables
    "conf.default.NameServer", "localhost",
    ""
  };
// </rtc-template>

Path::Path(RTC::Manager* manager)
    // <rtc-template block="initializer">
  : RTC::DataFlowComponentBase(manager),
    m_PathPort("Path")

    // </rtc-template>
{
  // Registration: InPort/OutPort/Service
  // <rtc-template block="registration">
  // Set InPort buffers

  // Set OutPort buffer

  // Set service provider to Ports
  m_PathPort.registerProvider("Path", "PathPlanner", m_Path);

  // Set service consumers to Ports

  // Set CORBA Service Ports
  registerPort(m_PathPort);

  // </rtc-template>

}

Path::~Path()
{
}


RTC::ReturnCode_t Path::onInitialize()
{
  // <rtc-template block="bind_config">
  // Bind variables and configuration variable
  bindParameter("NameServer", m_NameServer, "localhost");
  m_Path.setNameServer(m_NameServer);

  // </rtc-template>
  return RTC::RTC_OK;
}


/*
RTC::ReturnCode_t Path::onFinalize()
{
  return RTC::RTC_OK;
}
*/
/*
RTC::ReturnCode_t Path::onStartup(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/
/*
RTC::ReturnCode_t Path::onShutdown(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/
/*
RTC::ReturnCode_t Path::onActivated(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/
/*
RTC::ReturnCode_t Path::onDeactivated(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/
/*
RTC::ReturnCode_t Path::onExecute(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/
/*
RTC::ReturnCode_t Path::onAborting(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/
/*
RTC::ReturnCode_t Path::onError(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/
/*
RTC::ReturnCode_t Path::onReset(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/
/*
RTC::ReturnCode_t Path::onStateUpdate(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/
/*
RTC::ReturnCode_t Path::onRateChanged(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/


extern "C"
{
 
  void PathInit(RTC::Manager* manager)
  {
    RTC::Properties profile(path_spec);
    manager->registerFactory(profile,
                             RTC::Create<Path>,
                             RTC::Delete<Path>);
  }
  
};



