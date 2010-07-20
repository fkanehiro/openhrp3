#include <hrpModel/BodyCustomizerInterface.h>

#if defined(WIN32) || defined(_WIN32) || defined(__WIN32__) || defined(__NT__)
#define DLL_EXPORT __declspec(dllexport)
#else 
#define DLL_EXPORT 
#endif /* Windows */

#if defined(HRPMODEL_VERSION_MAJOR) && defined(HRPMODEL_VERSION_MINOR)
#if HRPMODEL_VERSION_MAJOR >= 3 && HRPMODEL_VERSION_MINOR >= 1
#define NS_HRPMODEL hrp
#endif
#endif

#ifndef NS_HRPMODEL
#define NS_HRPMODEL OpenHRP
#endif

using namespace std;
using namespace NS_HRPMODEL;

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

static BodyCustomizerHandle create(BodyHandle bodyHandle, const char* modelName)
{
    Customizer* customizer = 0;
	
    string name(modelName);
    if(name == "springJoint"){
        customizer = new Customizer;
        customizer->bodyHandle = bodyHandle;
        customizer->springT = 1.0e3;    
        customizer->dampingT = 1.0e1;
        int jointIndex = bodyInterface->getLinkIndexFromName(bodyHandle, "SPRING_JOINT");
        if(jointIndex >=0 ){
            JointValSet& jointValSet = customizer->jointValSet;
            jointValSet.valuePtr = bodyInterface->getJointValuePtr(bodyHandle, jointIndex);
            jointValSet.velocityPtr = bodyInterface->getJointVelocityPtr(bodyHandle, jointIndex);
            jointValSet.torqueForcePtr = bodyInterface->getJointForcePtr(bodyHandle, jointIndex);
        }
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
