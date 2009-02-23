/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */
/**
   \file
   \author Shin'ichiro Nakaoka
*/

#ifndef OPENHRP_BRIDGE_CONF_H_INCLUDED
#define OPENHRP_BRIDGE_CONF_H_INCLUDED

#if ( defined ( WIN32 ) || defined ( _WIN32 ) || defined(__WIN32__) || defined(__NT__) )
#define SUFFIX_SHARED_EXT   ".dll"
#elif defined(__APPLE__)
#define SUFFIX_SHARED_EXT   ".dylib"
#else
#define SUFFIX_SHARED_EXT   ".so"
#endif

#include <map>
#include <list>
#include <vector>
#include <string>
#include <boost/program_options.hpp>
#include <rtm/Manager.h>


enum DataTypeId {
    INVALID_DATA_TYPE = 0,
    JOINT_VALUE,
    JOINT_VELOCITY,
    JOINT_ACCELERATION,
    JOINT_TORQUE,
    EXTERNAL_FORCE,
    ABS_TRANSFORM,
    ABS_VELOCITY,
    ABS_ACCELERATION,
    FORCE_SENSOR,
    RATE_GYRO_SENSOR,
    ACCELERATION_SENSOR,
    COLOR_IMAGE,
    GRAYSCALE_IMAGE,
    DEPTH_IMAGE,
    RANGE_SENSOR,
    CONSTRAINT_FORCE
};

struct PortInfo {
    std::string portName;
    DataTypeId dataTypeId;
    std::vector<std::string> dataOwnerName; // link name or sensor name
    int dataOwnerId;           // sensor id
    double stepTime;
};
    
typedef std::map<std::string, PortInfo> PortInfoMap;
    

struct PortConnection {
    std::string robotPortName;
    std::string controllerInstanceName;
    std::string controllerPortName;
};
typedef std::vector<PortConnection> PortConnectionList;
    
    
struct ModuleInfo {
    std::string fileName;
    std::string componentName;
    std::string initFuncName;
    bool isLoaded;
    RTC::RtcBase* rtcServant;
};
typedef std::list<ModuleInfo> ModuleInfoList;
    

class BridgeConf
{
    BridgeConf(int argc, char* argv[]);
      
public:
      
    static BridgeConf* initialize(int argc, char* argv[]);
    static BridgeConf* instance();
      
    ~BridgeConf();

    bool isReady() { return isReady_; }
      
    const char* getOpenHRPNameServerIdentifier();
    const char* getControllerName();
    const char* getVirtualRobotRtcTypeName();

    void setupModules();

    typedef std::map<std::string, DataTypeId> LabelToDataTypeIdMap;
    LabelToDataTypeIdMap labelToDataTypeIdMap;
      
    PortInfoMap outPortInfos;
    PortInfoMap inPortInfos;
      
    ModuleInfoList moduleInfoList;

    PortConnectionList portConnections;

private:
      
    boost::program_options::variables_map vmap;
    boost::program_options::options_description options;
    boost::program_options::options_description commandLineOptions;
      
    bool isReady_;
    bool isProcessingConfigFile;
      
    std::string virtualRobotRtcTypeName;
    std::string controllerName;
    std::string nameServerIdentifier;

    void initOptionsDescription();
    void initLabelToDataTypeMap();

    void parseCommandLineOptions(int argc, char* argv[]);
    void parseOptions();
    void setPortInfos(const char* optionLabel, PortInfoMap& portInfos);
    void addPortConnection(const std::string& value);
      
    void addModuleInfo(const std::string& value);
    
    std::vector<std::string> extractParameters(const std::string& str, const char delimiter=':');
    std::string expandEnvironmentVariables(std::string str);
};


#endif 
