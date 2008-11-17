/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */


#include "DynamicsSimulator_impl.h"

#include <hrpUtil/Tvmet3d.h>
#include <hrpBase/Body.h>
#include <hrpBase/Link.h>
#include <hrpBase/LinkTraverse.h>
#include <hrpBase/LinkPath.h>
#include <hrpBase/Sensor.h>
#include <hrpBase/ModelLoaderUtil.h>

#include <vector>
#include <map>
#include <algorithm>

using namespace std;
using namespace hrp;


static const bool USE_INTERNAL_COLLISION_DETECTOR = false;
static const int debugMode = false;
static const bool enableTimeMeasure = false;

namespace {

    struct IdLabel {
        int id;
        const char *label;
    };
    typedef map<int, const char*> IdToLabelMap;

    IdToLabelMap commandLabelMap;

    IdLabel commandLabels[] = {

        { DynamicsSimulator::POSITION_GIVEN,    "Position Given (High Gain Mode)" },
        { DynamicsSimulator::JOINT_VALUE,       "Joint Value" },
        { DynamicsSimulator::JOINT_VELOCITY,	"Joint Velocity"},
        { DynamicsSimulator::JOINT_ACCELERATION,"Joint Acceleration"},
        { DynamicsSimulator::JOINT_TORQUE,		"Joint Torque"},
        { DynamicsSimulator::ABS_TRANSFORM,		"Absolute Transform"},
        { DynamicsSimulator::ABS_VELOCITY,		"Absolute Velocity"},
        { DynamicsSimulator::EXTERNAL_FORCE,	"External Force"},
        { -1, "" },
    };

    void initializeCommandLabelMaps()
    {
        if(commandLabelMap.empty()){
            int i = 0;
            while(true){
                IdLabel& idLabel = commandLabels[i++];
                if(idLabel.id == -1){
                    break;
                }
                commandLabelMap[idLabel.id] = idLabel.label;
            }
        }
    }

    const char* getLabelOfLinkDataType(DynamicsSimulator::LinkDataType type)
    {
        IdToLabelMap::iterator p = commandLabelMap.find(type);
        return (p != commandLabelMap.end()) ? p->second : "Requesting Unknown Data Type";
    }
};


template<typename X, typename X_ptr>
X_ptr checkCorbaServer(std::string n, CosNaming::NamingContext_var &cxt)
{
    CosNaming::Name ncName;
    ncName.length(1);
    ncName[0].id = CORBA::string_dup(n.c_str());
    ncName[0].kind = CORBA::string_dup("");
    X_ptr srv = NULL;
    try {
        srv = X::_narrow(cxt->resolve(ncName));
    } catch(const CosNaming::NamingContext::NotFound &exc) {
        std::cerr << n << " not found: ";
        switch(exc.why) {
        case CosNaming::NamingContext::missing_node:
            std::cerr << "Missing Node" << std::endl;
        case CosNaming::NamingContext::not_context:
            std::cerr << "Not Context" << std::endl;
            break;
        case CosNaming::NamingContext::not_object:
            std::cerr << "Not Object" << std::endl;
            break;
        }
        return (X_ptr)NULL;
    } catch(CosNaming::NamingContext::CannotProceed &exc) {
        std::cerr << "Resolve " << n << " CannotProceed" << std::endl;
    } catch(CosNaming::NamingContext::AlreadyBound &exc) {
        std::cerr << "Resolve " << n << " InvalidName" << std::endl;
    }
    return srv;
}


DynamicsSimulator_impl::DynamicsSimulator_impl(CORBA::ORB_ptr orb)
    : orb_(CORBA::ORB::_duplicate(orb))
{
    if(debugMode){
        cout << "DynamicsSimulator_impl::DynamicsSimulator_impl()" << endl;
    }

    // default integration method
    world.setRungeKuttaMethod();

    if(!USE_INTERNAL_COLLISION_DETECTOR){
        // resolve collisionchecker object
        CollisionDetectorFactory_var collisionDetectorFactory;
        
        CORBA::Object_var nS = orb->resolve_initial_references("NameService");
        CosNaming::NamingContext_var cxT;
        cxT = CosNaming::NamingContext::_narrow(nS);
        collisionDetectorFactory =
            checkCorbaServer<CollisionDetectorFactory,
            CollisionDetectorFactory_var>("CollisionDetectorFactory", cxT);
        if (CORBA::is_nil(collisionDetectorFactory)) {
            std::cerr << "CollisionDetectorFactory not found" << std::endl;
        }
        
        collisionDetector = collisionDetectorFactory->create();
    }

    collisions = new CollisionSequence;
    collidingLinkPairs = new LinkPairSequence;
    allCharacterPositions = new CharacterPositionSequence;
    allCharacterSensorStates = new SensorStateSequence;

    needToUpdatePositions = true;
    needToUpdateSensorStates = true;
}


DynamicsSimulator_impl::~DynamicsSimulator_impl()
{
    if(debugMode){
        cout << "DynamicsSimulator_impl::~DynamicsSimulator_impl()" << endl;
    }
}


void DynamicsSimulator_impl::destroy()
{
    if(debugMode){
        cout << "DynamicsSimulator_impl::destroy()" << endl;
    }

    PortableServer::POA_var poa_ = _default_POA();
    PortableServer::ObjectId_var id = poa_->servant_to_id(this);
    poa_->deactivate_object(id);
}


void DynamicsSimulator_impl::registerCharacter
(
    const char *name,
    BodyInfo_ptr bodyInfo
    )
{
    if(debugMode){
        cout << "DynamicsSimulator_impl::registerCharacter("
             << name << ", " << bodyInfo << " )" << std::endl;
    }

    BodyPtr body = loadBodyFromBodyInfo(bodyInfo, USE_INTERNAL_COLLISION_DETECTOR);

    if(body){
        body->name = name;
        if(debugMode){
            std::cout << "Loaded Model:\n" << *body << std::endl;
        }
        if(!USE_INTERNAL_COLLISION_DETECTOR){
            collisionDetector->registerCharacter(name, bodyInfo);
        }
        world.addBody(body);
    }
}


void DynamicsSimulator_impl::init
(
    CORBA::Double timeStep,
    OpenHRP::DynamicsSimulator::IntegrateMethod integrateOpt,
    OpenHRP::DynamicsSimulator::SensorOption sensorOpt
    )
{
    if(debugMode){
        cout << "DynamicsSimulator_impl::init(" << timeStep << ", ";
        cout << (integrateOpt == OpenHRP::DynamicsSimulator::EULER ? "EULER, " : "RUNGE_KUTTA, ");
        cout << (sensorOpt == OpenHRP::DynamicsSimulator::DISABLE_SENSOR ? "DISABLE_SENSOR" : "ENABLE_SENSOR");
        std::cout << ")" << std::endl;
    }

    world.setTimeStep(timeStep);
    world.setCurrentTime(0.0);

    if(integrateOpt == OpenHRP::DynamicsSimulator::EULER){
        world.setEulerMethod();
    } else {
        world.setRungeKuttaMethod();
    }

    world.enableSensors((sensorOpt == OpenHRP::DynamicsSimulator::ENABLE_SENSOR));

    int n = world.numBodies();
    for(int i=0; i < n; ++i){
        world.body(i)->initializeConfiguration();
    }

    _setupCharacterData();

#ifdef MEASURE_TIME
    processTime = 0;
#endif
}


void DynamicsSimulator_impl::registerCollisionCheckPair
(
    const char *charName1,
    const char *linkName1,
    const char *charName2,
    const char *linkName2,
    const CORBA::Double staticFriction,
    const CORBA::Double slipFriction,
    const DblSequence6 & K,
    const DblSequence6 & C
    )
{
    const double epsilon = 0.0;

    if(debugMode){
        cout << "DynamicsSimulator_impl::registerCollisionCheckPair("
             << charName1 << ", " << linkName1 << ", "
             << charName2 << ", " << linkName2 << ", "
             << staticFriction << ", " << slipFriction;
        if((K.length() == 6) && (C.length() == 6)){
            cout << ",\n"
                 << "{ "
                 << K[CORBA::ULong(0)] << ", "
                 << K[CORBA::ULong(1)] << ", "
                 << K[CORBA::ULong(2)] << " }\n"
                 << "{ "
                 << K[CORBA::ULong(3)] << ", "
                 << K[CORBA::ULong(4)] << ", "
                 << K[CORBA::ULong(5)] << " }\n"
                 << ",\n"
                 << "{ "
                 << C[CORBA::ULong(0)] << ", "
                 << C[CORBA::ULong(1)] << ", "
                 << C[CORBA::ULong(2)] << " }\n"
                 << "{ "
                 << C[CORBA::ULong(3)] << ", "
                 << C[CORBA::ULong(4)] << ", "
                 << C[CORBA::ULong(5)] << " }\n"
                 << ")" << endl;
        } else {
            cout << ", NULL, NULL)" << endl;
        }
    }

    int bodyIndex1 = world.bodyIndex(charName1);
    int bodyIndex2 = world.bodyIndex(charName2);

    if(bodyIndex1 >= 0 && bodyIndex2 >= 0){

        BodyPtr body1 = world.body(bodyIndex1);
        BodyPtr body2 = world.body(bodyIndex2);

        std::string emptyString = "";
        vector<Link*> links1;
        if(emptyString == linkName1){
            const LinkTraverse& traverse = body1->linkTraverse();
            links1.resize(traverse.numLinks());
            std::copy(traverse.begin(), traverse.end(), links1.begin());
        } else {
            links1.push_back(body1->link(linkName1));
        }

        vector<Link*> links2;
        if(emptyString == linkName2){
            const LinkTraverse& traverse = body2->linkTraverse();
            links2.resize(traverse.numLinks());
            std::copy(traverse.begin(), traverse.end(), links2.begin());
        } else {
            links2.push_back(body2->link(linkName2));
        }

        for(size_t i=0; i < links1.size(); ++i){
            for(size_t j=0; j < links2.size(); ++j){
                Link* link1 = links1[i];
                Link* link2 = links2[j];

                if(link1 && link2 && link1 != link2){
                    bool ok = world.contactForceSolver.addCollisionCheckLinkPair
                        (bodyIndex1, link1, bodyIndex2, link2, staticFriction, slipFriction, epsilon);

                    if(ok && !USE_INTERNAL_COLLISION_DETECTOR){
                        LinkPair_var linkPair = new LinkPair();
                        linkPair->charName1  = CORBA::string_dup(charName1);
                        linkPair->linkName1 = CORBA::string_dup(link1->name.c_str());
                        linkPair->charName2  = CORBA::string_dup(charName2);
                        linkPair->linkName2 = CORBA::string_dup(link2->name.c_str());
                        collisionDetector->addCollisionPair(linkPair, false, false);
                    }
                }
            }
        }
    }
}


//! \todo implement this method
void DynamicsSimulator_impl::registerVirtualLink
(
    const char*			char1,
    const char*			link1,
    const char*			char2,
    const char*			link2,
    const LinkPosition&	relTransform,
    CORBA::Short			transformDefined,
    const DblSequence9&		constraint,
    const char*			connectionName
    )
{
    if(debugMode){
        cout << "DynamicsSimulator_impl::registerVirtualLink("
             << char1 << ", " << link1 << ", "
             << char2 << ", " << link2 << ", ("
             << relTransform.p[0] << ", "
             << relTransform.p[1] << ", "
             << relTransform.p[2] << ",\n"
             << relTransform.R[0] << ", " << relTransform.R[1] << ", " << relTransform.R[2] << "\n "
             << relTransform.R[3] << ", " << relTransform.R[4] << ", " << relTransform.R[5] << "\n "
             << relTransform.R[6] << ", " << relTransform.R[7] << ", " << relTransform.R[8] << "),\n"
             << transformDefined << ",\n{"
             << constraint[CORBA::ULong(0)] << ", "
             << constraint[CORBA::ULong(1)] << ", "
             << constraint[CORBA::ULong(2)] << "}\n{"
             << constraint[CORBA::ULong(3)] << ", "
             << constraint[CORBA::ULong(4)] << ", "
             << constraint[CORBA::ULong(5)] << "}\n{"
             << constraint[CORBA::ULong(6)] << ", "
             << constraint[CORBA::ULong(7)] << ", "
             << constraint[CORBA::ULong(8)] << "}\n"
             << connectionName << ")\n";
        cout << "To Be Implemented" << endl;
    }
}


//! \todo implement this method
void DynamicsSimulator_impl::getConnectionConstraintForce
(
    const char * characterName,
    const char * connectionName,
    DblSequence6_out contactForce
    )
{
    if(debugMode){
        cout << "DynamicsSimulator_impl::getConnectionConstraintForce("
             << characterName << ", " << connectionName << ")" << endl;
    }
}


void DynamicsSimulator_impl::getCharacterSensorValues
(
    const char *characterName,
    const char *sensorName,
    DblSequence_out sensorOutput
    )
{
    if(debugMode){
        cout << "DynamicsSimulator_impl::getCharacterSensorData("
             << characterName << ", " << sensorName << ")";
    }

    BodyPtr body = world.body(characterName);
    assert(body);

    sensorOutput = new DblSequence;
    Sensor* sensor = body->sensor<Sensor>(sensorName);

    if(sensor){
        switch(sensor->type){

        case Sensor::FORCE:
        {
            ForceSensor* forceSensor = static_cast<ForceSensor*>(sensor);
            sensorOutput->length(6);
#ifndef __WIN32__
            setVector3(forceSensor->f, sensorOutput);
            setVector3(forceSensor->tau, sensorOutput, 3);
#else
            sensorOutput[CORBA::ULong(0)] = forceSensor->f(0); sensorOutput[CORBA::ULong(1)] = forceSensor->f(1); sensorOutput[CORBA::ULong(2)] = forceSensor->f(2);
            sensorOutput[CORBA::ULong(3)] = forceSensor->tau(0); sensorOutput[CORBA::ULong(4)] = forceSensor->tau(1); sensorOutput[CORBA::ULong(5)] = forceSensor->tau(2);
#endif
                
        }
        break;

        case Sensor::RATE_GYRO:
        {
            RateGyroSensor* gyro = static_cast<RateGyroSensor*>(sensor);
            sensorOutput->length(3);
#ifndef __WIN32__
            setVector3(gyro->w, sensorOutput);
#else	
            sensorOutput[CORBA::ULong(0)] = gyro->w(0); sensorOutput[CORBA::ULong(1)] = gyro->w(1); sensorOutput[CORBA::ULong(2)] = gyro->w(2);
#endif
        }
        break;

        case Sensor::ACCELERATION:
        {
            AccelSensor* accelSensor = static_cast<AccelSensor*>(sensor);
            sensorOutput->length(3);
#ifndef __WIN32__
            setVector3(accelSensor->dv, sensorOutput);
#else
            sensorOutput[CORBA::ULong(0)] = accelSensor->dv(0); sensorOutput[CORBA::ULong(1)] = accelSensor->dv(1); sensorOutput[CORBA::ULong(2)] = accelSensor->dv(2);
#endif
        }
        break;

        default:
            break;
        }
    }

    if(debugMode){
        cout << "DynamicsSimulator_impl::getCharacterSensorData - output\n"
             << "( " << sensorOutput[CORBA::ULong(0)];

        CORBA::ULong i = 0;
        while(true){
            cout << sensorOutput[i++];
            if(i == sensorOutput->length()) break;
            cout << ",";
        }

        cout << ")" << endl;
    }
}


void DynamicsSimulator_impl::initSimulation()
{
    if(debugMode){
        cout << "DynamicsSimulator_impl::initSimulation()" << endl;
    }

    world.initialize();

    _updateCharacterPositions();

    if(!USE_INTERNAL_COLLISION_DETECTOR){
        collisionDetector->queryContactDeterminationForDefinedPairs(allCharacterPositions.in(), collisions.out());
    }

    if(enableTimeMeasure){
        timeMeasureFinished = false;
        timeMeasureStarted = false;
    }
}


void DynamicsSimulator_impl::stepSimulation()
{
    if(enableTimeMeasure){
        if(!timeMeasureStarted){
            timeMeasure1.begin();
            timeMeasureStarted = true;
        }
    }

    if(debugMode){
        cout << "DynamicsSimulator_impl::stepSimulation()" << endl;
    }

    if(enableTimeMeasure) timeMeasure2.begin();
    world.calcNextState(collisions);

    needToUpdateSensorStates = true;

    _updateCharacterPositions();
    if(enableTimeMeasure) timeMeasure2.end();

    if(enableTimeMeasure) timeMeasure3.begin();
    if(!USE_INTERNAL_COLLISION_DETECTOR){
        collisionDetector->queryContactDeterminationForDefinedPairs(allCharacterPositions.in(), collisions.out());
    }
    if(enableTimeMeasure) timeMeasure3.end();

    world.contactForceSolver.clearExternalForces();


    if(enableTimeMeasure){
        if(world.currentTime() > 10.0 && !timeMeasureFinished){
            timeMeasureFinished = true;
            timeMeasure1.end();
            cout << "Total elapsed time = " << timeMeasure1.totalTime() << "\n"
                 << "Internal elapsed time = " << timeMeasure2.totalTime()
                 << ", the avarage = " << timeMeasure2.avarageTime() << endl;
            cout << "Collision check time = " << timeMeasure3.totalTime() << endl;
        }
    }
}


void DynamicsSimulator_impl::setCharacterLinkData
(
    const char* characterName,
    const char* linkName,
    OpenHRP::DynamicsSimulator::LinkDataType type,
    const DblSequence& wdata
    )
{
    if(debugMode){
        cout << "DynamicsSimulator_impl::setCharacterLinkData("
             << characterName << ", " << linkName << ", "
             << getLabelOfLinkDataType(type) << ", ";
        switch(type) {

        case OpenHRP::DynamicsSimulator::POSITION_GIVEN:
        case OpenHRP::DynamicsSimulator::JOINT_VALUE:
        case OpenHRP::DynamicsSimulator::JOINT_VELOCITY:
        case OpenHRP::DynamicsSimulator::JOINT_ACCELERATION:
        case OpenHRP::DynamicsSimulator::JOINT_TORQUE:
            cout << wdata[CORBA::ULong(0)] << ")\n";
            break;

        case OpenHRP::DynamicsSimulator::ABS_TRANSFORM: // 12x1
            cout << wdata[CORBA::ULong(0)] << ", "
                 << wdata[CORBA::ULong(1)] << ", "
                 << wdata[CORBA::ULong(2)] << ",\n"
                 << wdata[CORBA::ULong(3)] << ", "
                 << wdata[CORBA::ULong(4)] << ", "
                 << wdata[CORBA::ULong(5)] << ",\n"
                 << wdata[CORBA::ULong(6)] << ","
                 << wdata[CORBA::ULong(7)] << ", "
                 << wdata[CORBA::ULong(8)] << ",\n "
                 << wdata[CORBA::ULong(9)] << ","
                 << wdata[CORBA::ULong(10)] << ", "
                 << wdata[CORBA::ULong(11)] << ")" << endl;
            break;

        default: // 3x1
            cout << wdata[CORBA::ULong(0)] << ", "
                 << wdata[CORBA::ULong(1)] << ", "
                 << wdata[CORBA::ULong(2)] << ")" << endl;
        }
    }

    BodyPtr body = world.body(characterName);
    assert(body);
    Link* link = body->link(linkName);
    assert(link);

    switch(type) {

    case OpenHRP::DynamicsSimulator::POSITION_GIVEN:
        link->isHighGainMode = (wdata[0] > 0.0);
        break;

    case OpenHRP::DynamicsSimulator::JOINT_VALUE:
        if(link->jointType != Link::FIXED_JOINT)
            link->q = wdata[0];
        break;

    case OpenHRP::DynamicsSimulator::JOINT_VELOCITY:
        if(link->jointType != Link::FIXED_JOINT)
            link->dq = wdata[0];
        break;

    case OpenHRP::DynamicsSimulator::JOINT_ACCELERATION:
        if(link->jointType != Link::FIXED_JOINT)
            link->ddq = wdata[0];
        break;

    case OpenHRP::DynamicsSimulator::JOINT_TORQUE:
        if(link->jointType != Link::FIXED_JOINT)
            link->u = wdata[0];
        break;

    case OpenHRP::DynamicsSimulator::ABS_TRANSFORM:
    {
        link->p(0) = wdata[0];
        link->p(1) = wdata[1];
        link->p(2) = wdata[2];
        Matrix33 R;
        getMatrix33FromRowMajorArray(R, wdata.get_buffer(), 3);
        link->setSegmentAttitude(R);
    }
    break;
	
    case OpenHRP::DynamicsSimulator::ABS_VELOCITY:
    {
        link->v(0) = wdata[0];
        link->v(1) = wdata[1];
        link->v(2) = wdata[2];
        link->w(0) = wdata[3];
        link->w(1) = wdata[4];
        link->w(2) = wdata[5];
    }
    break;

    case OpenHRP::DynamicsSimulator::EXTERNAL_FORCE:
    {
        link->fext(0)   = wdata[0];
        link->fext(1)   = wdata[1];
        link->fext(2)   = wdata[2];
        link->tauext(0) = wdata[3];
        link->tauext(1) = wdata[4];
        link->tauext(2) = wdata[5];
        break;
    }
	
    default:
        return;
    }

    needToUpdatePositions = true;
    needToUpdateSensorStates = true;
}


void DynamicsSimulator_impl::getCharacterLinkData
(
    const char * characterName,
    const char * linkName,
    OpenHRP::DynamicsSimulator::LinkDataType type,
    DblSequence_out out_rdata
    )
{
    if(debugMode){
        cout << "DynamicsSimulator_impl::getCharacterLinkData("
             << characterName << ", " << linkName << ", "
             << getLabelOfLinkDataType(type) << ")" << endl;
    }

    BodyPtr body = world.body(characterName);
    assert(body);
    Link* link = body->link(linkName);
    assert(link);

    DblSequence_var rdata = new DblSequence;

    switch(type) {

    case OpenHRP::DynamicsSimulator::JOINT_VALUE:
        rdata->length(1);
        rdata[0] = link->q;
        break;

    case OpenHRP::DynamicsSimulator::JOINT_VELOCITY:
        rdata->length(1);
        rdata[0] = link->dq;
        break;

    case OpenHRP::DynamicsSimulator::JOINT_ACCELERATION:
        rdata->length(1);
        rdata[0] = link->ddq;
        break;

    case OpenHRP::DynamicsSimulator::JOINT_TORQUE:
        rdata->length(1);
        rdata[0] = link->u;
        break;

    case OpenHRP::DynamicsSimulator::ABS_TRANSFORM:
    {
        rdata->length(12);
        rdata[0] = link->p(0);
        rdata[1] = link->p(1);
        rdata[2] = link->p(2);
        double* buf = rdata->get_buffer();
        setMatrix33ToRowMajorArray(link->segmentAttitude(), buf, 3);
    }
    break;

    case OpenHRP::DynamicsSimulator::ABS_VELOCITY:
        rdata->length(6);
        rdata[0] = link->v(0);
        rdata[1] = link->v(1);
        rdata[2] = link->v(2);
        rdata[3] = link->w(0);
        rdata[4] = link->w(1);
        rdata[5] = link->w(2);
        break;

    case OpenHRP::DynamicsSimulator::EXTERNAL_FORCE:
        rdata->length(6);
        rdata[0] = link->fext(0);
        rdata[1] = link->fext(1);
        rdata[2] = link->fext(2);
        rdata[3] = link->tauext(0);
        rdata[4] = link->tauext(1);
        rdata[5] = link->tauext(2);
        break;

    default:
        break;
    }

    out_rdata = rdata._retn();
}


void DynamicsSimulator_impl::getCharacterAllLinkData
(
    const char * characterName,
    OpenHRP::DynamicsSimulator::LinkDataType type,
    DblSequence_out rdata
    )
{
    if(debugMode){
        cout << "DynamicsSimulator_impl::getCharacterAllLinkData("
             << characterName << ", "
             << getLabelOfLinkDataType(type) << ")" << endl;
    }

    BodyPtr body = world.body(characterName);
    assert(body);
    int n = body->numJoints();
    rdata = new DblSequence();
    rdata->length(n);

    switch(type) {

    case OpenHRP::DynamicsSimulator::JOINT_VALUE:
        for(int i=0; i < n; ++i){
            (*rdata)[i] = body->joint(i)->q;
        }
        break;

    case OpenHRP::DynamicsSimulator::JOINT_VELOCITY:
        for(int i=0; i < n; ++i){
            (*rdata)[i] = body->joint(i)->dq;
        }
        break;

    case OpenHRP::DynamicsSimulator::JOINT_ACCELERATION:
        for(int i=0; i < n; ++i){
            (*rdata)[i] = body->joint(i)->ddq;
        }
        break;

    case OpenHRP::DynamicsSimulator::JOINT_TORQUE:
        for(int i=0; i < n; ++i){
            (*rdata)[i] = body->joint(i)->u;
        }
        break;

    default:
        cerr << "ERROR - Invalid type: " << getLabelOfLinkDataType(type) << endl;
        break;
    }
}


void DynamicsSimulator_impl::setCharacterAllLinkData
(
    const char * characterName,
    OpenHRP::DynamicsSimulator::LinkDataType type,
    const DblSequence & wdata
    )
{
    if(debugMode){
        cout << "DynamicsSimulator_impl::setCharacterAllLinkData("
             << characterName << ", "
             << getLabelOfLinkDataType(type) << ",\n(";
        if(wdata.length()) cout << wdata[0];
        for(CORBA::ULong i=0; i<wdata.length(); ++i){
            cout << ", " << wdata[i];
        }
        cout << "))" << endl;
    }

    BodyPtr body = world.body(characterName);
    assert(body);

    int n = wdata.length();
    if(n > body->numJoints()){
        n = body->numJoints();
    }

    switch(type) {

    case OpenHRP::DynamicsSimulator::JOINT_VALUE:
        for(int i=0; i < n; ++i){
            if(body->joint(i)->jointType != Link::FIXED_JOINT)
                body->joint(i)->q = wdata[i];
        }
        break;

    case OpenHRP::DynamicsSimulator::JOINT_VELOCITY:
        for(int i=0; i < n; ++i){
            if(body->joint(i)->jointType != Link::FIXED_JOINT)
                body->joint(i)->dq = wdata[i];
        }
        break;

    case OpenHRP::DynamicsSimulator::JOINT_ACCELERATION:
        for(int i=0; i < n; ++i){
            if(body->joint(i)->jointType != Link::FIXED_JOINT)
                body->joint(i)->ddq = wdata[i];
        }
        break;

    case OpenHRP::DynamicsSimulator::JOINT_TORQUE:
        for(int i=0; i < n; ++i){
            if(body->joint(i)->jointType != Link::FIXED_JOINT)
                body->joint(i)->u = wdata[i];
        }
        break;

    default:
        std::cerr << "ERROR - Invalid type: " << getLabelOfLinkDataType(type) << endl;
    }
}


void DynamicsSimulator_impl::setGVector
(
    const DblSequence3& wdata
    )
{
    assert(wdata.length() == 3);

    Vector3 g;
    getVector3(g, wdata);
    world.setGravityAcceleration(g);

    if(debugMode){
        cout << "DynamicsSimulator_impl::setGVector("
             << wdata[CORBA::ULong(0)] << ", "
             << wdata[CORBA::ULong(1)] << ", "
             << wdata[CORBA::ULong(2)] << ")" << endl;
    }
}


void DynamicsSimulator_impl::getGVector
(
    DblSequence3_out wdata
    )
{
    wdata->length(3);
    Vector3 g = world.getGravityAcceleration();
    (*wdata)[0] = g[0];
    (*wdata)[1] = g[1];
    (*wdata)[2] = g[2];

    if(debugMode){
        cout << "DynamicsSimulator_impl::getGVector(";
        cout << wdata[CORBA::ULong(0)] << ", "
             << wdata[CORBA::ULong(1)] << ", "
             << wdata[CORBA::ULong(2)] << ")" << endl;
    }
}


void DynamicsSimulator_impl::setCharacterAllJointModes
(
    const char * characterName,
    OpenHRP::DynamicsSimulator::JointDriveMode jointMode
    )
{
    bool isHighGainMode = (jointMode == OpenHRP::DynamicsSimulator::HIGH_GAIN_MODE);

    BodyPtr body = world.body(characterName);

    for(int i=1; i < body->numLinks(); ++i){
        body->link(i)->isHighGainMode = isHighGainMode;
    }

    if(debugMode){
        cout << "DynamicsSimulator_impl::setCharacterAllJointModes(";
        cout << characterName << ", ";
        cout << (isHighGainMode ? "HIGH_GAIN_MODE" : "TORQUE_MODE");
        cout << ")" << endl;
    }
}


CORBA::Boolean DynamicsSimulator_impl::calcCharacterInverseKinematics
(
    const char * characterName,
    const char * fromLink, const char * toLink,
    const LinkPosition& target
    )
{
    if(debugMode){
        cout << "DynamicsSimulator_impl::calcCharacterInverseKinematics("
             << characterName << ", " << fromLink << ", " << toLink << ",\n"
             << "( "
             << target.p[0] << ", " << target.p[1] << ", " << target.p[2] << ",\n\n"
             << target.R[0] << ", " << target.R[1] << ", " << target.R[2] << ",\n"
             << target.R[3] << ", " << target.R[4] << ", " << target.R[5] << ",\n"
             << target.R[6] << ", " << target.R[7] << ", " << target.R[8] << endl;
    }

    bool solved = false;

    BodyPtr body = world.body(characterName);
    assert(body);

    JointPath path(body->link(fromLink), body->link(toLink));

    if(path){
        Vector3 p(target.p[0], target.p[1], target.p[2]);
        Matrix33 R;
        for (int i=0; i<3; i++) {
            for (int j=0; j<3; j++){
                R(i,j) = target.R[3*i+j];
            }
        }

        solved = path.calcInverseKinematics(p, R);

        if(solved) {
            needToUpdatePositions = true;
            needToUpdateSensorStates = true;
        }
    }

    return solved;
}


void DynamicsSimulator_impl::calcCharacterForwardKinematics
(
    const char * characterName
    )
{
    if(debugMode){
        cout << "DynamicsSimulator_impl::calcCharacterForwardKinematics( "
             << characterName << endl;
    }

    BodyPtr body = world.body(characterName);
    assert(body);
    body->calcForwardKinematics(true, true);

    needToUpdatePositions = true;
    needToUpdateSensorStates = true;
}


void DynamicsSimulator_impl::calcWorldForwardKinematics()
{
    for(int i=0; i < world.numBodies(); ++i){
        world.body(i)->calcForwardKinematics(true, true);
    }

    needToUpdatePositions = true;
    needToUpdateSensorStates = true;
}


bool DynamicsSimulator_impl::checkCollision(bool checkAll) 
{
    calcWorldForwardKinematics();
    _updateCharacterPositions();
    if(!USE_INTERNAL_COLLISION_DETECTOR){
        if (checkAll){
            return collisionDetector->queryContactDeterminationForDefinedPairs(allCharacterPositions.in(), collisions.out());
        }else{
            return collisionDetector->queryIntersectionForDefinedPairs(checkAll, allCharacterPositions.in(), collidingLinkPairs.out());
        }
    }
}


void DynamicsSimulator_impl::getWorldState(WorldState_out wstate)
{
    if(debugMode){
        cout << "DynamicsSimulator_impl::getWorldState()\n";
    }

    if (needToUpdatePositions) _updateCharacterPositions();

    wstate = new WorldState;

    wstate->time = world.currentTime();
    wstate->characterPositions = allCharacterPositions;
    wstate->collisions = collisions;

    if(debugMode){
        cout << "getWorldState - exit" << endl;
    }
}


void DynamicsSimulator_impl::getCharacterSensorState(const char* characterName, SensorState_out sstate)
{
    int bodyIndex = world.bodyIndex(characterName);

    if(bodyIndex >= 0){
        if(needToUpdateSensorStates){
            _updateSensorStates();
        }
        sstate = new SensorState(allCharacterSensorStates[bodyIndex]);
    } else {
        sstate = new SensorState;
    }
}


void DynamicsSimulator_impl::_setupCharacterData()
{
    if(debugMode){
        cout << "DynamicsSimulator_impl::_setupCharacterData()\n";
    }

    int n = world.numBodies();
    allCharacterPositions->length(n);
    allCharacterSensorStates->length(n);

    for(int i=0; i < n; ++i){
        BodyPtr body = world.body(i);

        int numLinks = body->numLinks();
        CharacterPosition& characterPosition = allCharacterPositions[i];
        characterPosition.characterName = CORBA::string_dup(body->name.c_str());
        LinkPositionSequence& linkPositions = characterPosition.linkPositions;
        linkPositions.length(numLinks);

        int numJoints = body->numJoints();
        SensorState& sensorState = allCharacterSensorStates[i];
        sensorState.q.length(numJoints);
        sensorState.dq.length(numJoints);
        sensorState.u.length(numJoints);
        sensorState.force.length(body->numSensors(Sensor::FORCE));
        sensorState.rateGyro.length(body->numSensors(Sensor::RATE_GYRO));
        sensorState.accel.length(body->numSensors(Sensor::ACCELERATION));

        if(debugMode){
            std::cout << "character[" << i << "], nlinks = " << numLinks << "\n";
        }
    }

    if(debugMode){
        cout << "_setupCharacterData() - exit" << endl;;
    }
}


void DynamicsSimulator_impl::_updateCharacterPositions()
{
    if(debugMode){
        cout << "DynamicsSimulator_impl::_updateCharacterPositions()\n";
    }

    int n = world.numBodies();

    {	
#pragma omp parallel for num_threads(3)
        for(int i=0; i < n; ++i){
            BodyPtr body = world.body(i);
            int numLinks = body->numLinks();
			
            CharacterPosition& characterPosition = allCharacterPositions[i];
			
            if(debugMode){
                cout << "character[" << i << "], nlinks = " << numLinks << "\n";
            }
			
            for(int j=0; j < numLinks; ++j) {
                Link* link = body->link(j);
                LinkPosition& linkPosition = characterPosition.linkPositions[j];
                setVector3(link->p, linkPosition.p);
                setMatrix33ToRowMajorArray(link->segmentAttitude(), linkPosition.R);
            }
        }
    }
    needToUpdatePositions = false;

    if(debugMode){
        cout << "_updateCharacterData() - exit" << endl;
    }
}


void DynamicsSimulator_impl::_updateSensorStates()
{
    if(debugMode){
        cout << "DynamicsSimulator_impl::_updateSensorStates()\n";
    }

    int numBodies = world.numBodies();
    for(int i=0; i < numBodies; ++i){

        SensorState& state = allCharacterSensorStates[i];

        BodyPtr body = world.body(i);
        int numJoints = body->numJoints();

        for(int j=0; j < numJoints; j++){
            Link* joint = body->joint(j);
            state.q [j] = joint->q;
            state.dq[j] = joint->dq;
            state.u [j] = joint->u;
        }

        int n = body->numSensors(Sensor::FORCE);
        for(int id = 0; id < n; ++id){
            ForceSensor* sensor = body->sensor<ForceSensor>(id);
            setVector3(sensor->f,   state.force[id], 0);
            setVector3(sensor->tau, state.force[id], 3);
            if(debugMode){
                std::cout << "Force Sensor: f:" << sensor->f << "tau:" << sensor->tau << "\n";
            }
        }

        n = body->numSensors(Sensor::RATE_GYRO);
        for(int id=0; id < n; ++id){
            RateGyroSensor* sensor = body->sensor<RateGyroSensor>(id);
            setVector3(sensor->w, state.rateGyro[id]);
            if(debugMode){
                std::cout << "Rate Gyro:" << sensor->w << "\n";
            }
        }

        n = body->numSensors(Sensor::ACCELERATION);
        for(int id=0; id < n; ++id){
            AccelSensor* sensor = body->sensor<AccelSensor>(id);
            setVector3(sensor->dv, state.accel[id]);
            if(debugMode){
                std::cout << "Accel:" << sensor->dv << std::endl;
            }
        }		
    }
    needToUpdateSensorStates = false;

    if(debugMode){
        cout << "_updateCharacterData() - exit" << endl;
    }
}



/**
   \note S L O W. If CORBA sequence resize does not fiddle with the memory
   allocation one loop will do. Two to be on the safe side.
*/
CORBA::Boolean DynamicsSimulator_impl::getCharacterCollidingPairs
(
    const char *characterName,
    LinkPairSequence_out pairs
    )
{
    if(debugMode){
        cout << "DynamicsSimulator_impl::getCharacterCollidingPairs("
             << characterName << ")" << endl;
    }

    assert(world.body(characterName));

    std::vector<unsigned int> locations;

    for(unsigned int i=0; i < collisions->length(); ++i) {

        if(  (strcmp(characterName, collisions[i].pair.charName1) == 0)  ||
             (strcmp(characterName, collisions[i].pair.charName2) == 0))
            locations.push_back(i);
    }

    pairs->length(locations.size());

    unsigned long n=0;
    for(std::vector<unsigned int>::iterator iter = locations.begin();
        iter != locations.end(); ++iter) {

        strcpy(pairs[n].charName1, collisions[*iter].pair.charName1);
        strcpy(pairs[n].charName2, collisions[*iter].pair.charName2);
        strcpy(pairs[n].linkName1, collisions[*iter].pair.linkName1);
        strcpy(pairs[n].linkName2, collisions[*iter].pair.linkName2);
    }

    return true;
}


void DynamicsSimulator_impl::calcCharacterJacobian
(
    const char *characterName,
    const char *baseLink,
    const char *targetLink,
    DblSequence_out jacobian
    )
{
    if(debugMode){
        cout << "DynamicsSimulator_impl::calcCharacterJacobian("
             << characterName << ", "
             << baseLink << ", "
             << targetLink << ")" << endl;
    }

    BodyPtr body = world.body(characterName);
    assert(body);

    JointPath path(body->link(baseLink), body->link(targetLink));
    int height = 6;
    int width = path.numJoints();
    dmatrix J(height, width);
    path.calcJacobian(J);

    jacobian->length(height * width);
    int i = 0;
    for(int r=0; r < height; ++r){
        for(int c=0; c < width; ++c){
            (*jacobian)[i++] = J(r, c);
        }
    }
}


/**
 * constructor
 * @param   orb     reference to ORB
 */
DynamicsSimulatorFactory_impl::DynamicsSimulatorFactory_impl(CORBA::ORB_ptr	orb) :
    orb_(CORBA::ORB::_duplicate(orb))
{
    initializeCommandLabelMaps();

    if(debugMode){
        cout << "DynamicsSimulatorFactory_impl::DynamicsSimulatorFactory_impl()" << endl;
    }
}


DynamicsSimulatorFactory_impl::~DynamicsSimulatorFactory_impl()
{
    if(debugMode){
        cout << "DynamicsSimulatorFactory_impl::~DynamicsSimulatorFactory_impl()" << endl;
    }

    PortableServer::POA_var poa = _default_POA();
    PortableServer::ObjectId_var id = poa -> servant_to_id(this);
    poa -> deactivate_object(id);
}


DynamicsSimulator_ptr DynamicsSimulatorFactory_impl::create()
{
    if(debugMode){
        cout << "DynamicsSimulatorFactory_impl::create()" << endl;
    }

    DynamicsSimulator_impl* integratorImpl = new DynamicsSimulator_impl(orb_);

    PortableServer::ServantBase_var integratorrServant = integratorImpl;
    PortableServer::POA_var poa_ = _default_POA();
    PortableServer::ObjectId_var id =
        poa_ -> activate_object(integratorImpl);

    return integratorImpl -> _this();
}


void DynamicsSimulatorFactory_impl::shutdown()
{
    orb_->shutdown(false);
}
