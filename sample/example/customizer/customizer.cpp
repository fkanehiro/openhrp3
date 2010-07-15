
#include <cmath>
#include <cstring>
#include <hrpModel/BodyCustomizerInterface.h>

#if defined(WIN32) || defined(_WIN32) || defined(__WIN32__) || defined(__NT__)
#define DLL_EXPORT __declspec(dllexport)
#else 
#define DLL_EXPORT 
#endif /* Windows */

#if defined(HRPMODEL_VERSION_MAJOR) && defined(HRPMODEL_VERSION_MINOR)
#if HRPMODEL_VERSION_MAJOR >= 3 && HRPMODEL_VERSION_MINOR >= 1
#define NS_HRPMODEL hrp
typedef hrp::Vector3 vector3;
typedef hrp::Matrix33 matrix33;
#endif
#endif

#ifndef NS_HRPMODEL
#define NS_HRPMODEL OpenHRP
#endif

using namespace std;
using namespace NS_HRPMODEL;

static const bool debugMode = false;

static BodyInterface* bodyInterface = 0;

static BodyCustomizerInterface bodyCustomizerInterface;

struct JointValSet
{
    double* valuePtr;
    double* velocityPtr;
    double* torqueForcePtr;
};

struct Customizer
{
    BodyHandle bodyHandle;
    JointValSet jointValSet;

    double springT;
    double dampingT;
};

static const char** getTargetModelNames()
{
    static const char* names[] = { 
        "springJoint",
        0 };
	
    return names;
}


static void getVirtualbushJoints(Customizer* customizer, BodyHandle body)
{
    int bushIndex = bodyInterface->getLinkIndexFromName(body, "SPRING_JOINT");
    if(bushIndex >=0 ){
        JointValSet& jointValSet = customizer->jointValSet;
        jointValSet.valuePtr = bodyInterface->getJointValuePtr(body, bushIndex);
        jointValSet.velocityPtr = bodyInterface->getJointVelocityPtr(body, bushIndex);
        jointValSet.torqueForcePtr = bodyInterface->getJointForcePtr(body, bushIndex);
    }
}
	

static BodyCustomizerHandle create(BodyHandle bodyHandle, const char* modelName)
{
    Customizer* customizer = 0;
	
    string name(modelName);
    if(name == "springJoint"){
        customizer = new Customizer;
        customizer->bodyHandle = bodyHandle;
        customizer->springT = 1.0e3;    
        customizer->dampingT = 1.0e1;
        getVirtualbushJoints(customizer, bodyHandle);
    }

    return static_cast<BodyCustomizerHandle>(customizer);
}


static void destroy(BodyCustomizerHandle customizerHandle)
{
    Customizer* customizer = static_cast<Customizer*>(customizerHandle);
    if(customizer){
        delete customizer;
    }
}

static void setVirtualJointForces(BodyCustomizerHandle customizerHandle)
{
    Customizer* customizer = static_cast<Customizer*>(customizerHandle);
    JointValSet& trans = customizer->jointValSet;
    *(trans.torqueForcePtr) = - customizer->springT * (*trans.valuePtr) - customizer->dampingT * (*trans.velocityPtr);
}


extern "C" DLL_EXPORT
NS_HRPMODEL::BodyCustomizerInterface* getHrpBodyCustomizerInterface(NS_HRPMODEL::BodyInterface* bodyInterface_)
{
    bodyInterface = bodyInterface_;

    bodyCustomizerInterface.version = NS_HRPMODEL::BODY_CUSTOMIZER_INTERFACE_VERSION;
    bodyCustomizerInterface.getTargetModelNames = getTargetModelNames;
    bodyCustomizerInterface.create = create;
    bodyCustomizerInterface.destroy = destroy;
    bodyCustomizerInterface.initializeAnalyticIk = 0;
    bodyCustomizerInterface.calcAnalyticIk = 0;
    bodyCustomizerInterface.setVirtualJointForces = setVirtualJointForces;

    return &bodyCustomizerInterface;
}
