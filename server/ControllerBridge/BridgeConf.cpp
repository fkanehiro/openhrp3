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

#include "BridgeConf.h"

#include <iostream>
#include <fstream>
#include <boost/regex.hpp>

#if (BOOST_VERSION <= 103301)
#include <boost/filesystem/path.hpp>
#include <boost/filesystem/operations.hpp>
#include <boost/filesystem/convenience.hpp> 
#else
#include <boost/filesystem.hpp>
#endif


using namespace std;
using namespace boost;


namespace {
  BridgeConf* bridgeConf = 0;
}


BridgeConf* BridgeConf::initialize(int argc, char* argv[])
{
  bridgeConf = new BridgeConf(argc ,argv);
  return bridgeConf;
}

  
BridgeConf* BridgeConf::instance()
{
  return bridgeConf;
}


BridgeConf::BridgeConf(int argc, char* argv[]) :
  options("Allowed options"),
  commandLineOptions("Allowed options")
{
  isReady_ = false;
  initOptionsDescription();
  initLabelToDataTypeMap();

  parseCommandLineOptions(argc, argv);
}


BridgeConf::~BridgeConf()
{

}


void BridgeConf::initOptionsDescription()
{
  options.add_options()

    ("name-server",
     program_options::value<string>()->default_value("localhost:2809"),
     "Nameserver used by OpenHRP (hostname:port)")

    ("server-name",
     program_options::value<string>()->default_value("ControllerBridge"),
     "Name of the OpenHRP controller factory server")

    ("robot-name",
     program_options::value<string>()->default_value("VirtualRobot"),
     "Name of the virtual robot RTC type")

    ("module",
     program_options::value<vector<string> >(),
     "Module filename")

    ("out-port",
     program_options::value<vector<string> >(),
     "Set an out-port that transfers data from the simulator to the controller")

    ("in-port",
     program_options::value<vector<string> >(),
     "Set an in-port transfers data from the controller to the simulator")

    ("connection",
     program_options::value<vector<string> >(),
     "Port connection setting");

  commandLineOptions.add(options).add_options()

    ("config-file",
     program_options::value<string>(), 
     "configuration file of the controller bridge")
    
    ("help,h", "Show this help");
}


void BridgeConf::initLabelToDataTypeMap()
{
  LabelToDataTypeIdMap& m = labelToDataTypeIdMap;
  
  m["JOINT_VALUE"]         = JOINT_VALUE;
  m["JOINT_VELOCITY"]      = JOINT_VELOCITY;
  m["JOINT_ACCELERATION"]  = JOINT_ACCELERATION;
  m["JOINT_TORQUE"]        = JOINT_TORQUE;
  m["EXTERNAL_FORCE"]      = EXTERNAL_FORCE;
  m["ABS_TRANSFORM"]       = ABS_TRANSFORM;
  m["FORCE_SENSOR"]        = FORCE_SENSOR;
  m["RATE_GYRO_SENSOR"]    = RATE_GYRO_SENSOR;
  m["ACCELERATION_SENSOR"] = ACCELERATION_SENSOR;
  m["COLOR_IMAGE"]         = COLOR_IMAGE;
  m["GRAYSCALE_IMAGE"]     = GRAYSCALE_IMAGE;
  m["DEPTH_IMAGE"]         = DEPTH_IMAGE;
}


void BridgeConf::parseCommandLineOptions(int argc, char* argv[])
{
  isProcessingConfigFile = false;

  program_options::store(program_options::parse_command_line(argc, argv, commandLineOptions), vmap);

  if(vmap.count("help")){
    cout << commandLineOptions << endl;
  } else {

    if(vmap.count("bridge-config-file")){
      string fileName(vmap["bridge-config-file"].as<string>());
      ifstream ifs(fileName.c_str());
      
      if (ifs.fail()) {
	throw invalid_argument(string("cannot open the config file"));
      }
      program_options::store(program_options::parse_config_file(ifs, options), vmap);
      
      isProcessingConfigFile = true;
    }
    program_options::notify(vmap);
    
    parseOptions();

    isReady_ = true;
  }
}


void BridgeConf::parseOptions()
{
  if(vmap.count("module")){
    vector<string> values = vmap["module"].as<vector<string> >();
    for(size_t i=0; i < values.size(); ++i){
      addModuleInfo(values[i]);
    }
  }

  if(vmap.count("out-port")){
    setPortInfos("out-port", outPortInfos);
  }

  if(vmap.count("in-port")){
    setPortInfos("in-port", inPortInfos);
  }

  if(vmap.count("connection")){
    vector<string> values = vmap["connection"].as<vector<string> >();
    for(size_t i=0; i < values.size(); ++i){
      addPortConnection(values[i]);
    }
  }

  string server(expandEnvironmentVariables(vmap["name-server"].as<string>()));
  nameServerIdentifier = string("corbaloc:iiop:") + server + "/NameService";

  controllerFactoryName = expandEnvironmentVariables(vmap["server-name"].as<string>());

  virtualRobotRtcTypeName = expandEnvironmentVariables(vmap["robot-name"].as<string>());

}


void BridgeConf::setPortInfos(const char* optionLabel, PortInfoMap& portInfos)
{
  vector<string> ports = vmap[optionLabel].as<vector<string> >();
  for(size_t i=0; i < ports.size(); ++i){
    vector<string> parameters = extractParameters(ports[i]);
    int n = parameters.size();
    if(n < 2 || n > 3){
      throw invalid_argument(string("invalid in port setting"));
    }
    PortInfo info;

    info.portName = parameters[0];

    int dataTypeParameterPos;
    if(n == 2){
      dataTypeParameterPos = 1;
    } else {
      info.dataOwnerName = parameters[1];
      dataTypeParameterPos = 2;
    }
    LabelToDataTypeIdMap::iterator it = labelToDataTypeIdMap.find(parameters[dataTypeParameterPos]);
    if(it == labelToDataTypeIdMap.end()){
      throw invalid_argument(string("invalid data type"));
    }
    info.dataTypeId = it->second;
    
    portInfos.insert(make_pair(info.portName, info));
  }
}


void BridgeConf::addPortConnection(const std::string& value)
{
  vector<string> parameters = extractParameters(value);
  int n = parameters.size();
  if(n < 2 || n > 3){
    throw std::invalid_argument(string("Invalied port connection"));
  }
  PortConnection connection;
  connection.robotPortName = parameters[0];
  
  if(parameters.size() == 2){
    if(moduleInfoList.size() != 1){
      throw std::invalid_argument(string("RTC instance name must be specified in a connection option"));
    }
    connection.controllerPortName = parameters[1];
  } else {
    connection.controllerInstanceName = parameters[1];
    connection.controllerPortName = parameters[2];
  }
  portConnections.push_back(connection);
}


void BridgeConf::addModuleInfo(const std::string& value)
{
  vector<string> parameters = extractParameters(value);

  if(parameters.size() == 0 || parameters.size() > 2){
    throw std::invalid_argument(std::string("invalid module set"));
  } else {
    ModuleInfo info;
    info.fileName = parameters[0];
    info.componentName = filesystem::basename(filesystem::path(info.fileName));
    if(parameters.size() == 1){
      info.initFuncName = info.componentName + "Init";
    } else {
      info.initFuncName = parameters[1];
    }
    info.isLoaded = false;
    
    moduleInfoList.push_back(info);
  }
}


void BridgeConf::setupModules()
{
  RTC::Manager& rtcManager = RTC::Manager::instance();
  ModuleInfoList::iterator moduleInfo = moduleInfoList.begin();
  while(moduleInfo != moduleInfoList.end()){
    if(!moduleInfo->isLoaded){
      rtcManager.load(moduleInfo->fileName.c_str(), moduleInfo->initFuncName.c_str());
      moduleInfo->isLoaded = true;
      moduleInfo->rtcServant = rtcManager.createComponent(moduleInfo->componentName.c_str());
    }
    ++moduleInfo;
  }
}


const char* BridgeConf::getOpenHRPNameServerIdentifier()
{
  return nameServerIdentifier.c_str();
}


const char* BridgeConf::getControllerFactoryName()
{
  return controllerFactoryName.c_str();
}


const char* BridgeConf::getVirtualRobotRtcTypeName()
{
  return virtualRobotRtcTypeName.c_str();
}


std::vector<std::string> BridgeConf::extractParameters(const std::string& str)
{
  vector<string> result;
  string::size_type sepPos = 0;
  string::size_type nowPos = 0;
  
  while (sepPos != string::npos) {
    sepPos = str.find(":", nowPos);

    if(isProcessingConfigFile){
      result.push_back(expandEnvironmentVariables(str.substr(nowPos, sepPos-nowPos)));
    } else {
      result.push_back(str.substr(nowPos, sepPos-nowPos));
    }
      
    nowPos = sepPos+1;
  }
  
  return result;
}


std::string BridgeConf::expandEnvironmentVariables(std::string str)
{
  regex variablePattern("\\$([A-z][A-z_0-9]*)");
    
  match_results<string::const_iterator> result; 
  match_flag_type flags = match_default; 
    
  string::const_iterator start, end;
  start = str.begin();
  end = str.end();
  int pos = 0;

  vector< pair< int, int > > results;

  while ( regex_search(start, end, result, variablePattern, flags) ) {
    results.push_back(std::make_pair(pos+result.position(1), result.length(1)));

    // seek to the remaining part
    start = result[0].second;
    pos += result.length(0);
		
    flags |= boost::match_prev_avail; 
    flags |= boost::match_not_bob; 
  }

  // replace the variables in reverse order
  while (!results.empty()) {
    int begin = results.back().first;
    int length = results.back().second;

    string envName = str.substr(begin, length);
    char* envValue = getenv(envName.c_str());

    if(envValue){
      str.replace(begin-1, length+1, string(envValue));
    }
    results.pop_back();
  }

  return str;
}
