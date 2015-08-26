#include <cmath>
#include <cstring>
#include <boost/function.hpp>
#include <boost/bind.hpp>
#include <fstream>
#include <coil/stringutil.h>

//#define CNOID_BODY_CUSTOMIZER
#ifdef CNOID_BODY_CUSTOMIZER
#include <cnoid/BodyCustomizerInterface>
#else
#include <hrpModel/BodyCustomizerInterface.h>
#endif

#include <iostream>

#if defined(WIN32) || defined(_WIN32) || defined(__WIN32__) || defined(__NT__)
#define DLL_EXPORT __declspec(dllexport)
#else 
#define DLL_EXPORT 
#endif /* Windows */

#if defined(HRPMODEL_VERSION_MAJOR) && defined(HRPMODEL_VERSION_MINOR)
#if HRPMODEL_VERSION_MAJOR >= 3 && HRPMODEL_VERSION_MINOR >= 1
#include <hrpUtil/EigenTypes.h>
#define NS_HRPMODEL hrp
#endif
#endif

#ifdef CNOID_BODY_CUSTOMIZER
#define NS_HRPMODEL cnoid
cnoid::Matrix3 trans(const cnoid::Matrix3& M) { return M.transpose(); }
double dot(const cnoid::Vector3& a, const cnoid::Vector3& b) { return a.dot(b); }
typedef cnoid::Matrix3 Matrix33;
#endif


#ifndef NS_HRPMODEL
#define NS_HRPMODEL OpenHRP
typedef OpenHRP::vector3 Vector3;
typedef OpenHRP::matrix33 Matrix33;
#endif

using namespace std;
using namespace boost;
using namespace NS_HRPMODEL;

static BodyInterface* bodyInterface = 0;

static BodyCustomizerInterface bodyCustomizerInterface;

struct JointValSet
{
  double* valuePtr;
  double* velocityPtr;
  double* torqueForcePtr;
};

struct BushCustomizerParam
{
  JointValSet jointValSets;
  std::string name;
  // Spring coefficient for bush. For slide joint, [N/m], for rotate joint, [Nm/rad]
  double spring;
  // Damping coefficient for bush. For slide joint, [N/(m/s)], for rotate joint, [Nm/(rad/s)]
  double damping;
  int index;
};

struct BushCustomizer
{
  BodyHandle bodyHandle;
  bool hasVirtualBushJoints;
  std::vector<BushCustomizerParam> params;
};

// Parameters should be specified by configure file using BUSH_CUSTOMIZER_CONFIG_PATH environment.

// Robot model name using bush
static std::string robot_model_name;

// Bush configuration parameters such as:
//   bush_config: joint1,spring1,damping1 joint2,spring2,damping2 joint3,spring3,damping3 ...
//   joint* should be included in VRML file.
static coil::vstring bush_config;

static const char** getTargetModelNames()
{
  static const char* names[] = {
    robot_model_name.c_str(),
    0 };
  return names;
}

static void getVirtualbushJoints(BushCustomizer* customizer, BodyHandle body)
{
  std::cerr << "[Bush customizer] Bush params" << std::endl;
  customizer->hasVirtualBushJoints = true;
  for (size_t i = 0; i < bush_config.size(); i++) {
    // tmp_config <= temp bush config, e.g., "joint*,spring*,damping*"
    coil::vstring tmp_config = coil::split(bush_config[i], ",");
    // Check size
    if ( tmp_config.size() != 3 ) {
      std::cerr << "[Bush customizer]   Parameter size mismatch (" << i << ") (" << tmp_config.size() << ")" << std::endl;
      return;
    }
    int bushIndex = bodyInterface->getLinkIndexFromName(body, tmp_config[0].c_str());
    if(bushIndex < 0){
      std::cerr << "[Bush customizer]   No such joint name (" << tmp_config[0] << ")" << std::endl;
      customizer->hasVirtualBushJoints = false;
    } else {
      BushCustomizerParam p;
      p.index = bushIndex;
      p.name = tmp_config[0];
      p.spring = atof(tmp_config[1].c_str());
      p.damping= atof(tmp_config[2].c_str());
      p.jointValSets.valuePtr = bodyInterface->getJointValuePtr(body, bushIndex);
      p.jointValSets.velocityPtr = bodyInterface->getJointVelocityPtr(body, bushIndex);
      p.jointValSets.torqueForcePtr = bodyInterface->getJointForcePtr(body, bushIndex);
      customizer->params.push_back(p);
      std::cerr << "[Bush customizer]   name = " << p.name << ", index = " << p.index << ", spring = " << p.spring << ", damping = " << p.damping << std::endl;
    }
  }
}

static BodyCustomizerHandle create(BodyHandle bodyHandle, const char* modelName)
{
  BushCustomizer* customizer = 0;

  std::cerr << "[Bush customizer] Create " << std::string(modelName) << std::endl;
  customizer = new BushCustomizer;

  customizer->bodyHandle = bodyHandle;
  //customizer->hasVirtualBushJoints = false;
  // customizer->springT  = 5.0e5; // N/m
  // customizer->dampingT = 1.0e3; // N/(m/s)
  // customizer->springR  = 1e3; // Nm / rad
  // customizer->dampingR = 2.5e1;   // Nm / (rad/s)

  getVirtualbushJoints(customizer, bodyHandle);

  return static_cast<BodyCustomizerHandle>(customizer);
}


static void destroy(BodyCustomizerHandle customizerHandle)
{
  BushCustomizer* customizer = static_cast<BushCustomizer*>(customizerHandle);
  if(customizer){
    delete customizer;
  }
}

static void setVirtualJointForces(BodyCustomizerHandle customizerHandle)
{
  BushCustomizer* customizer = static_cast<BushCustomizer*>(customizerHandle);

  if(customizer->hasVirtualBushJoints){
    for(int i=0; i < customizer->params.size(); ++i){
      BushCustomizerParam& param = customizer->params[i];
      *(param.jointValSets.torqueForcePtr) = - param.spring * (*param.jointValSets.valuePtr) - param.damping * (*param.jointValSets.velocityPtr);
      //std::cerr << "Bush " << i << " " << 0 << " " << *(param.jointValSets.torqueForcePtr) << " = " << *(param.jointValSets.valuePtr) << " + " << *(param.jointValSets.velocityPtr) << std::endl;
    }
  }
}

extern "C" DLL_EXPORT
NS_HRPMODEL::BodyCustomizerInterface* getHrpBodyCustomizerInterface(NS_HRPMODEL::BodyInterface* bodyInterface_)
{
  bodyInterface = bodyInterface_;
  char* tmpenv = getenv("BUSH_CUSTOMIZER_CONFIG_PATH");
  if (tmpenv) {
    std::ifstream ifs(tmpenv);
    if (ifs.fail() ) {
      std::cerr << "[Bush customizer] Could not open [" << tmpenv << "]" << std::endl;
    } else {
      std::string tmpstr;
      std::cerr << "[Bush customizer] Open [" << tmpenv << "]" << std::endl;
      while (std::getline(ifs,tmpstr)) {
        coil::vstring config_params = coil::split(tmpstr, ":");
        if (config_params[0] == "robot_model_name") {
          robot_model_name = config_params[1];
          std::cerr << "[Bush customizer]   robot_model_name = [" << robot_model_name << "]" << std::endl;
        } else if ( config_params[0] == "bush_config" ) {
          bush_config = coil::split(config_params[1], " ");
          std::cerr << "[Bush customizer]   bush_config = [" << config_params[1] << "]" << std::endl;
        }
      }
    }
  }

  bodyCustomizerInterface.version = NS_HRPMODEL::BODY_CUSTOMIZER_INTERFACE_VERSION;
  bodyCustomizerInterface.getTargetModelNames = getTargetModelNames;
  bodyCustomizerInterface.create = create;
  bodyCustomizerInterface.destroy = destroy;
  bodyCustomizerInterface.initializeAnalyticIk = NULL;
  bodyCustomizerInterface.calcAnalyticIk = NULL;
  bodyCustomizerInterface.setVirtualJointForces = setVirtualJointForces;

  return &bodyCustomizerInterface;
}
