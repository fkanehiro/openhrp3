/**
   \file
   \author Shin'ichiro Nakaoka
*/

#ifndef OPENHRP_BRIDGE_CONF_H_INCLUDED
#define OPENHRP_BRIDGE_CONF_H_INCLUDED

#include <map>
#include <list>
#include <vector>
#include <string>
#include <boost/program_options.hpp>
#include <rtm/Manager.h>

namespace OpenHRP {

  namespace ControllerBridge {

    enum DataTypeId {
      INVALID_DATA_TYPE = 0,
      JOINT_VALUE,
      JOINT_VELOCITY,
      JOINT_ACCELERATION,
      JOINT_TORQUE,
      EXTERNAL_FORCE,
      ABS_TRANSFORM,
      FORCE_SENSOR,
      RATE_GYRO_SENSOR,
      ACCELERATION_SENSOR,
      COLOR_IMAGE,
      GRAYSCALE_IMAGE,
      DEPTH_IMAGE
    };

    struct PortInfo {
      std::string portName;
      DataTypeId dataTypeId;
      std::string dataOwnerName; // link name or sensor name
      int dataOwnerId; // sensor id
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
      const char* getControllerFactoryName();
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
      std::string controllerFactoryName;
      std::string nameServerIdentifier;

      void initOptionsDescription();
      void initLabelToDataTypeMap();

      void parseCommandLineOptions(int argc, char* argv[]);
      void parseOptions();
      void setPortInfos(const char* optionLabel, PortInfoMap& portInfos);
      void addPortConnection(const std::string& value);
      
      void addModuleInfo(const std::string& value);
      
      std::vector<std::string> extractParameters(const std::string& str);
      std::string expandEnvironmentVariables(std::string str);
    };

  }
    
}


#endif
