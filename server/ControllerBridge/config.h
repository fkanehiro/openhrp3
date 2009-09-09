/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */

/*! @file
  @author Takafumi Tawara
*/

#ifndef OPENHRP_CONTROLLERBRIDGE_CONFIG_H_INCLUDED
#define OPENHRP_CONTROLLERBRIDGE_CONFIG_H_INCLUDED

#ifdef OPENRTM_VERSION_042
typedef RTC::Port                                      Port_Service_Type;
typedef RTC::Port_var                                  Port_Service_Var_Type;
typedef RTC::Port_ptr                                  Port_Service_Ptr_Type;
typedef RTC::ExtTrigExecutionContextService            ExtTrigExecutionContextService_Type;
typedef RTC::ExtTrigExecutionContextService_var        ExtTrigExecutionContextService_Var_Type;
typedef RTC::PortList                                  Port_Service_List_Type;
typedef RTC::PortList_var                              Port_Service_List_Var_Type;
#else
typedef RTC::PortService                               Port_Service_Type;
typedef RTC::PortService_var                           Port_Service_Var_Type;
typedef RTC::PortService_ptr                           Port_Service_Ptr_Type;
typedef OpenRTM::ExtTrigExecutionContextService        ExtTrigExecutionContextService_Type;
typedef OpenRTM::ExtTrigExecutionContextService_var    ExtTrigExecutionContextService_Var_Type;
typedef RTC::PortServiceList                           Port_Service_List_Type;
typedef RTC::PortServiceList_var                       Port_Service_List_Var_Type;
#endif

#endif
