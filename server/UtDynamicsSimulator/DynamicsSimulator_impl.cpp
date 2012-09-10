// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-
/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */
/** @file DynamicsSimulator/server/DynamicsSimulator_impl.cpp
 *
 */

#include <vector>
#include <map>

#include "DynamicsSimulator_impl.h"
#include "ModelLoaderUtil.h"
#include "Sensor.h"
#include <psim.h>

using namespace OpenHRP;
using namespace std;


//#define INTEGRATOR_DEBUG
static const bool enableTimeMeasure = false;

//#include <fstream>
//static std::ofstream logfile("impl.log");
//static std::ofstream logfile;

namespace {

	struct IdLabel {
		int id;
		const char* label;
	};
	typedef map<int, const char*> IdToLabelMap;

	IdToLabelMap commandLabelMaps[2];

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
	world.setCurrentTime(0.0);
	world.setRungeKuttaMethod();

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

	collisions = new CollisionSequence;
	collidingLinkPairs = new LinkPairSequence;
	allCharacterPositions = new CharacterPositionSequence;
	allCharacterSensorStates = new SensorStateSequence;

	needToUpdatePositions = true;
	needToUpdateSensorStates = true;
}


DynamicsSimulator_impl::~DynamicsSimulator_impl()
{
}


void DynamicsSimulator_impl::destroy()
{
	PortableServer::POA_var poa_ = _default_POA();
	PortableServer::ObjectId_var id = poa_->servant_to_id(this);
	poa_->deactivate_object(id);
}


void DynamicsSimulator_impl::registerCharacter(
		const char *name,
		BodyInfo_ptr body)
{
//	logfile << "registerCharacter(" << name << ", " << body->name() << ")" << endl;
	loadBodyFromBodyInfo(&world, name, body);
	collisionDetector->registerCharacter(name, body);
//	logfile << "total dof = " << world.Chain()->NumDOF() << endl;
}


void DynamicsSimulator_impl::init(
		CORBA::Double _timestep,
		OpenHRP::DynamicsSimulator::IntegrateMethod integrateOpt,
		OpenHRP::DynamicsSimulator::SensorOption sensorOpt)
{
//	logfile << "init" << endl;

	world.setTimeStep(_timestep);
    world.setCurrentTime(0.0);

	if(integrateOpt == OpenHRP::DynamicsSimulator::EULER){
		world.setEulerMethod();
	} else {
		world.setRungeKuttaMethod();
	}

	world.enableSensors((sensorOpt == OpenHRP::DynamicsSimulator::ENABLE_SENSOR));

	_setupCharacterData();

	isFirstSimulationLoop = true;

#ifdef MEASURE_TIME
	processTime = 0;
#endif
}


static void joint_traverse_sub(Joint* cur, std::vector<Joint*>& jlist)
{
	if(!cur) return;
//	logfile << "joint_traverse_sub: " << cur->name << ", n_dof = " << cur->n_dof << endl;
	jlist.push_back(cur);
	joint_traverse_sub(cur->brother, jlist);
	joint_traverse_sub(cur->child, jlist);
}

static void joint_traverse(Joint* r, std::vector<Joint*>& jlist)
{
	jlist.push_back(r);
	joint_traverse_sub(r->child, jlist);
}


void DynamicsSimulator_impl::registerCollisionCheckPair(
		const char *charName1,
		const char *linkName1,
		const char *charName2,
		const char *linkName2,
		const CORBA::Double staticFriction,
		const CORBA::Double slipFriction,
		const DblSequence6 & K,
		const DblSequence6 & C,
        const double culling_thresh,
		const double restitution)
{
	const double epsilon = 0.0;
//	logfile << "registerCollisionCheckPair" << endl;

	std::string emptyString = "";
	std::vector<Joint*> joints1;
	std::vector<Joint*> joints2;
	pSim* chain = world.Chain();

	if(emptyString == linkName1)
	{
		Joint* r = chain->FindCharacterRoot(charName1);
		if(r) joint_traverse(r, joints1);
	}
	else
	{
		Joint* jnt1 = chain->FindJoint(linkName1, charName1);
		if(jnt1) joints1.push_back(jnt1);
	}
	if(emptyString == linkName2)
	{
		Joint* r = chain->FindCharacterRoot(charName2);
		if(r) joint_traverse(r, joints2);
	}
	else
	{
		Joint* jnt2 = chain->FindJoint(linkName2, charName2);
		if(jnt2) joints2.push_back(jnt2);
	}

	for(size_t i=0; i < joints1.size(); ++i)
	{
		Joint* j1 = joints1[i];
		for(size_t j=0; j < joints2.size(); ++j)
		{
			Joint* j2 = joints2[j];
			if(j1 && j2 && j1 != j2)
			{
//				logfile << "pair = " << j1->name << ", " << j2->name << endl;
				world.addCollisionCheckLinkPair(j1, j2, staticFriction, slipFriction, epsilon);
				LinkPair_var linkPair = new LinkPair();
				linkPair->charName1  = CORBA::string_dup(charName1);
				linkPair->linkName1 = CORBA::string_dup(j1->basename);
				linkPair->charName2  = CORBA::string_dup(charName2);
				linkPair->linkName2 = CORBA::string_dup(j2->basename);
				linkPair->tolerance = 0;
				collisionDetector->addCollisionPair(linkPair);
			}
		}
	}
}

void DynamicsSimulator_impl::registerIntersectionCheckPair(
		const char *charName1,
		const char *linkName1,
		const char *charName2,
		const char *linkName2,
		const double tolerance)
{
	const double epsilon = 0.0;
//	logfile << "registerCollisionCheckPair" << endl;

	std::string emptyString = "";
	std::vector<Joint*> joints1;
	std::vector<Joint*> joints2;
	pSim* chain = world.Chain();

	if(emptyString == linkName1)
	{
		Joint* r = chain->FindCharacterRoot(charName1);
		if(r) joint_traverse(r, joints1);
	}
	else
	{
		Joint* jnt1 = chain->FindJoint(linkName1, charName1);
		if(jnt1) joints1.push_back(jnt1);
	}
	if(emptyString == linkName2)
	{
		Joint* r = chain->FindCharacterRoot(charName2);
		if(r) joint_traverse(r, joints2);
	}
	else
	{
		Joint* jnt2 = chain->FindJoint(linkName2, charName2);
		if(jnt2) joints2.push_back(jnt2);
	}

	for(size_t i=0; i < joints1.size(); ++i)
	{
		Joint* j1 = joints1[i];
		for(size_t j=0; j < joints2.size(); ++j)
		{
			Joint* j2 = joints2[j];
			if(j1 && j2 && j1 != j2)
			{
				LinkPair_var linkPair = new LinkPair();
				linkPair->charName1  = CORBA::string_dup(charName1);
				linkPair->linkName1 = CORBA::string_dup(j1->basename);
				linkPair->charName2  = CORBA::string_dup(charName2);
				linkPair->linkName2 = CORBA::string_dup(j2->basename);
				linkPair->tolerance = tolerance;
				collisionDetector->addCollisionPair(linkPair);
			}
		}
	}
}

void DynamicsSimulator_impl::registerExtraJoint
		(
			const char*	charName1,
			const char*	linkName1,
			const char*	charName2,
			const char*	linkName2,
			const DblSequence3&	link1LocalPos,
			const DblSequence3&	link2LocalPos,
			const ExtraJointType jointType,
			const DblSequence3&	jointAxis,
			const char*			extraJointName)
{
}

//! \todo implement this method
void DynamicsSimulator_impl::registerVirtualLink(
		const char*			char1,
		const char*			link1,
		const char*			char2,
		const char*			link2,
		const LinkPosition&	relTransform,
		CORBA::Short			transformDefined,
		const DblSequence9&		constraint,
		const char*			connectionName)
{
}


//! \todo implement this method
void DynamicsSimulator_impl::getConnectionConstraintForce(
		const char * characterName,
		const char * connectionName,
		DblSequence6_out contactForce)
{
}


static void vec3_to_seq(const fVec3& vec, DblSequence_out& seq, size_t offset = 0)
{
	seq[CORBA::ULong(offset++)] = vec(0);
	seq[CORBA::ULong(offset++)] = vec(1);
	seq[CORBA::ULong(offset)] = vec(2);
}

static void seq_to_vec3(const DblSequence3& seq, fVec3& vec)
{
	vec(0) = seq[0];
	vec(1) = seq[1];
	vec(2) = seq[2];
}

void DynamicsSimulator_impl::getCharacterSensorValues(
		const char *characterName,
		const char *sensorName,
		DblSequence_out sensorOutput)
{
	sensorOutput = new DblSequence;
	Sensor* sensor = world.findSensor(sensorName, characterName);

	if(sensor)
	{
		switch(sensor->type)
		{
		case Sensor::FORCE:
			{
				ForceSensor* forceSensor = static_cast<ForceSensor*>(sensor);
                sensorOutput->length(6);
#ifndef __WIN32__
                vec3_to_seq(forceSensor->f, sensorOutput);
				vec3_to_seq(forceSensor->tau, sensorOutput, 3);
#else
				sensorOutput[CORBA::ULong(0)] = forceSensor->f(0);
				sensorOutput[CORBA::ULong(1)] = forceSensor->f(1);
				sensorOutput[CORBA::ULong(2)] = forceSensor->f(2);
				sensorOutput[CORBA::ULong(3)] = forceSensor->tau(0);
				sensorOutput[CORBA::ULong(4)] = forceSensor->tau(1);
				sensorOutput[CORBA::ULong(5)] = forceSensor->tau(2);
#endif
			}
			break;

		case Sensor::RATE_GYRO:
			{
				RateGyroSensor* gyro = static_cast<RateGyroSensor*>(sensor);
                sensorOutput->length(3);
#ifndef __WIN32__
                vec3_to_seq(gyro->w, sensorOutput);
#else	
				sensorOutput[CORBA::ULong(0)] = gyro->w(0);
				sensorOutput[CORBA::ULong(1)] = gyro->w(1);
				sensorOutput[CORBA::ULong(2)] = gyro->w(2);
#endif
			}
			break;

		case Sensor::ACCELERATION:
			{
				AccelSensor* accelSensor = static_cast<AccelSensor*>(sensor);
                sensorOutput->length(3);
#ifndef __WIN32__
                vec3_to_seq(accelSensor->dv, sensorOutput);
#else
				sensorOutput[CORBA::ULong(0)] = accelSensor->dv(0);
				sensorOutput[CORBA::ULong(1)] = accelSensor->dv(1);
				sensorOutput[CORBA::ULong(2)] = accelSensor->dv(2);
#endif
			}
			break;

		default:
			break;
		}
	}
}

void DynamicsSimulator_impl::initSimulation()
{
	world.initialize();

	_updateCharacterPositions();
	collisionDetector->queryContactDeterminationForDefinedPairs(allCharacterPositions.in(), collisions.out());

	if(enableTimeMeasure){
		timeMeasureFinished = false;
		timeMeasure1.begin();
	}
}

void DynamicsSimulator_impl::stepSimulation()
{
	if(enableTimeMeasure) timeMeasure2.begin();
	world.calcNextState(collisions);
	if(enableTimeMeasure) timeMeasure2.end();

	if(enableTimeMeasure){
		cout << " " << timeMeasure2.time() << "\n";
	}

	needToUpdateSensorStates = true;

	_updateCharacterPositions();

	collisionDetector->queryContactDeterminationForDefinedPairs(allCharacterPositions.in(), collisions.out());

	if(enableTimeMeasure)
	{
		if(world.currentTime() > 10.0 && !timeMeasureFinished)
		{
			timeMeasureFinished = true;
			timeMeasure1.end();
			cout << "Total elapsed time = " << timeMeasure1.totalTime() << "\n";
			cout << "Internal elapsed time = " << timeMeasure2.totalTime();
			cout << ", the avarage = " << timeMeasure2.avarageTime() << endl;;
			//cout << "Collision check time = " << timeMeasure3.totalTime() << endl;
		}
	}
}


void DynamicsSimulator_impl::setCharacterLinkData(
		const char* characterName,
		const char* linkName,
		OpenHRP::DynamicsSimulator::LinkDataType type,
		const DblSequence& wdata)
{
//	logfile << "setCharacterLinkData(" << characterName << ", " << linkName << ")" << endl;
	Joint* joint = world.Chain()->FindJoint(linkName, characterName);
	if(!joint) return;

	switch(type) {

	case OpenHRP::DynamicsSimulator::POSITION_GIVEN:
		joint->t_given = !(wdata[0] > 0.0);
		break;

	case OpenHRP::DynamicsSimulator::JOINT_VALUE:
		joint->SetJointValue(wdata[0]);
		break;

	case OpenHRP::DynamicsSimulator::JOINT_VELOCITY:
		joint->SetJointVel(wdata[0]);
		break;

	case OpenHRP::DynamicsSimulator::JOINT_ACCELERATION:
		joint->SetJointAcc(wdata[0]);
		break;

	case OpenHRP::DynamicsSimulator::JOINT_TORQUE:
		joint->SetJointForce(wdata[0]);
		break;

	case OpenHRP::DynamicsSimulator::ABS_TRANSFORM:
	{
		fVec3 abs_pos(wdata[0], wdata[1], wdata[2]);
		fMat33 abs_att(wdata[3], wdata[4], wdata[5], wdata[6], wdata[7], wdata[8], wdata[9], wdata[10], wdata[11]);  // wdata is in row major order
		joint->SetJointValue(abs_pos, abs_att);
		break;
	}
	
	case OpenHRP::DynamicsSimulator::ABS_VELOCITY:
	{
		fVec3 abs_lin_vel(wdata[0], wdata[1], wdata[2]);
		fVec3 abs_ang_vel(wdata[3], wdata[4], wdata[5]);
		joint->rel_lin_vel.mul(abs_lin_vel, joint->abs_att);
		joint->rel_ang_vel.mul(abs_ang_vel, joint->abs_att);
		break;
	}

	case OpenHRP::DynamicsSimulator::EXTERNAL_FORCE:
	{
		// original: local frame?, around world origin
		// new: local frame, around joint origin
		joint->ext_force(0) = wdata[0];
		joint->ext_force(1) = wdata[1];
		joint->ext_force(2) = wdata[2];
		fVec3 n0(wdata[3], wdata[4], wdata[5]);
		fVec3 np;
		np.cross(joint->abs_pos, joint->ext_force);
		joint->ext_moment.sub(n0, np);
		break;
	}

	default:
		return;
	}

	needToUpdatePositions = true;
	needToUpdateSensorStates = true;
}


void DynamicsSimulator_impl::getCharacterLinkData(
		const char * characterName,
		const char * linkName,
		OpenHRP::DynamicsSimulator::LinkDataType type,
		DblSequence_out out_rdata)
{
//	logfile << "getCharacterLinkData" << endl;
	Joint* joint = world.Chain()->FindJoint(linkName, characterName);
	assert(joint);

	DblSequence_var rdata = new DblSequence;

	switch(type) {

	case OpenHRP::DynamicsSimulator::JOINT_VALUE:
		rdata->length(1);
		rdata[0] = joint->q;
		break;

	case OpenHRP::DynamicsSimulator::JOINT_VELOCITY:
		rdata->length(1);
		rdata[0] = joint->qd;
		break;

	case OpenHRP::DynamicsSimulator::JOINT_ACCELERATION:
		rdata->length(1);
		rdata[0] = joint->qdd;
		break;

	case OpenHRP::DynamicsSimulator::JOINT_TORQUE:
		rdata->length(1);
		rdata[0] = joint->tau;
		break;

	case OpenHRP::DynamicsSimulator::ABS_TRANSFORM:
	{
		fEulerPara ep;
		ep.set(joint->abs_att);
		rdata->length(7);
		rdata[0] = joint->abs_pos(0);
		rdata[1] = joint->abs_pos(1);
		rdata[2] = joint->abs_pos(2);
		rdata[3] = ep.Ang();
		rdata[4] = ep.Axis()(0);
		rdata[5] = ep.Axis()(1);
		rdata[6] = ep.Axis()(2);
		break;
	}

	case OpenHRP::DynamicsSimulator::ABS_VELOCITY:
	{
		fVec3 v0, w0;
		v0.mul(joint->abs_att, joint->loc_lin_vel);
		w0.mul(joint->abs_att, joint->loc_ang_vel);
		rdata->length(6);
		rdata[0] = v0(0);
		rdata[1] = v0(1);
		rdata[2] = v0(2);
		rdata[3] = w0(0);
		rdata[4] = w0(1);
		rdata[5] = w0(2);
		break;
	}
	
	case OpenHRP::DynamicsSimulator::EXTERNAL_FORCE:
	{
		// original: local frame, around joint origin
		// new: local frame?, around world origin
		rdata->length(6);
		rdata[0] = joint->ext_force(0);
		rdata[1] = joint->ext_force(1);
		rdata[2] = joint->ext_force(2);
		fVec3 np, n0;
		np.cross(joint->abs_pos, joint->ext_force);
		n0.add(joint->ext_moment, np);
		rdata[3] = n0(0);
		rdata[4] = n0(1);
		rdata[5] = n0(2);
		break;
	}
	
	default:
		break;
	}

	out_rdata = rdata._retn();
}


void DynamicsSimulator_impl::getCharacterAllLinkData(
		const char * characterName,
		OpenHRP::DynamicsSimulator::LinkDataType type,
		DblSequence_out rdata)
{
//	logfile << "getCharacterAllLinkData" << endl;
	rdata = new DblSequence();

	world.getAllCharacterData(characterName, type, rdata);
}


void DynamicsSimulator_impl::setCharacterAllLinkData(
		const char * characterName,
		OpenHRP::DynamicsSimulator::LinkDataType type,
		const DblSequence & wdata)
{
//	logfile << "setCharacterAllLinkData: " << getLabelOfLinkDataType(type) << endl;
	world.setAllCharacterData(characterName, type, wdata);
}


void DynamicsSimulator_impl::setGVector(
		const DblSequence3& wdata)
{
	assert(wdata.length() == 3);

//	logfile << "setGVector" << endl;
    fVec3 g;
	seq_to_vec3(wdata, g);
	world.setGravityAcceleration(g);

}


void DynamicsSimulator_impl::getGVector(
		DblSequence3_out wdata)
{
	wdata->length(3);
	fVec3 g = world.getGravityAcceleration();
	(*wdata)[0] = g(0);
	(*wdata)[1] = g(1);
	(*wdata)[2] = g(2);
}


void DynamicsSimulator_impl::setCharacterAllJointModes(
		const char * characterName,
		OpenHRP::DynamicsSimulator::JointDriveMode jointMode)
{
//	logfile << "setCharacterAllJointModes" << endl;
	bool isTorqueMode = (jointMode != OpenHRP::DynamicsSimulator::HIGH_GAIN_MODE);

	world.Chain()->SetCharacterTorqueGiven(characterName, isTorqueMode);

}


CORBA::Boolean DynamicsSimulator_impl::calcCharacterInverseKinematics(
		const char * characterName,
		const char * fromLink, const char * toLink,
		const LinkPosition& target)
{
	bool solved = false;

	// TODO

	return solved;
}


void DynamicsSimulator_impl::calcCharacterForwardKinematics(
		const char * characterName)
{
//	logfile << "calcCharacterForwardKinematics" << endl;
	world.Chain()->CalcPosition();

	needToUpdatePositions = true;
	needToUpdateSensorStates = true;
}


void DynamicsSimulator_impl::calcWorldForwardKinematics()
{
//	logfile << "calcWorldForwardKinematics" << endl;
	world.Chain()->CalcPosition();

	needToUpdatePositions = true;
	needToUpdateSensorStates = true;
}


void DynamicsSimulator_impl::getWorldState(WorldState_out wstate)
{
//	logfile << "getWorldState" << endl;
	if (needToUpdatePositions) _updateCharacterPositions();

	wstate = new WorldState;

	wstate->time = world.currentTime();
	wstate->characterPositions = allCharacterPositions;
	wstate->collisions = collisions;
}


void DynamicsSimulator_impl::getCharacterSensorState(const char* characterName, SensorState_out sstate)
{
//	logfile << "getCharacterSensorState(" << characterName << ")" << endl;
	int index = -1;
	pSim* chain = world.Chain();
	int n_char = world.numCharacter();
	for(int i=0; i<n_char; i++)
	{
		Joint* j = world.rootJoint(i);
		if(!strcmp(j->CharName(), characterName))
		{
			index = i;
			break;
		}
	}
	if(index >= 0)
	{
		if(needToUpdateSensorStates)
		{
			_updateSensorStates();
		}
		sstate = new SensorState(allCharacterSensorStates[index]);
	}
	else
	{
		sstate = new SensorState;
	}
}


void DynamicsSimulator_impl::_setupCharacterData()
{
	int nchar = world.numCharacter();
	pSim* chain = world.Chain();

	allCharacterPositions->length(nchar);
	allCharacterSensorStates->length(nchar);

//	logfile << "_setupCharacterData" << endl;
//	logfile << "total DOF = " << chain->NumDOF() << endl;
	for(int i=0; i<nchar; i++)
	{
		Joint* j = world.rootJoint(i);
//		logfile << "root = " << j->name << endl;
		CharacterPosition& characterPosition = allCharacterPositions[i];
		characterPosition.characterName = CORBA::string_dup(j->CharName());
		int n_links = world.numLinks(i);
//		logfile << "n_links = " << n_links << endl;
		LinkPositionSequence& linkPositions = characterPosition.linkPositions;
		linkPositions.length(n_links);
		SensorState& sensorState = allCharacterSensorStates[i];
		int n_joints = world.numJoints(i);
//		logfile << "n_joints = " << n_joints << endl;
		sensorState.q.length(n_joints);
		sensorState.dq.length(n_joints);
		sensorState.u.length(n_joints);
		sensorState.force.length(world.numSensors(Sensor::FORCE, j->CharName()));
		sensorState.rateGyro.length(world.numSensors(Sensor::RATE_GYRO, j->CharName()));
		sensorState.accel.length(world.numSensors(Sensor::ACCELERATION, j->CharName()));
	}
}


void DynamicsSimulator_impl::_updateCharacterPositions()
{
	world.getAllCharacterPositions(allCharacterPositions.inout());
	needToUpdatePositions = false;

#if 0
	int n_char = allCharacterPositions->length();
	for(int i=0; i<n_char; i++)
	{
		CharacterPosition& cpos = allCharacterPositions[i];
		int n_link = cpos.linkPositions.length();
		for(int j=0; j<n_link; j++)
		{
			LinkPosition& lpos = cpos.linkPositions[j];
		}
	}
#endif
}

void DynamicsSimulator_impl::_updateSensorStates()
{
	world.getAllSensorStates(allCharacterSensorStates);
	needToUpdateSensorStates = false;
}


/**
 \note S L O W. If CORBA sequence resize does not fiddle with the memory
 allocation one loop will do. Two to be on the safe side.
*/
CORBA::Boolean DynamicsSimulator_impl::getCharacterCollidingPairs(
		const char *characterName,
		LinkPairSequence_out pairs)
{
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


void DynamicsSimulator_impl::calcCharacterJacobian(
		const char *characterName,
		const char *baseLink,
		const char *targetLink,
		DblSequence_out jacobian)
{
	fMat J;
	world.calcCharacterJacobian(characterName, baseLink, targetLink, J);

	int height = J.row();
	int width = J.col();

	jacobian->length(height * width);
	int i = 0;
	for(int r=0; r < height; ++r){
		for(int c=0; c < width; ++c){
			(*jacobian)[i++] = J(r, c);
		}
	}
}

bool DynamicsSimulator_impl::checkCollision(bool checkAll) 
{
	calcWorldForwardKinematics();	
    _updateCharacterPositions();
	if (checkAll){
		return collisionDetector->queryContactDeterminationForDefinedPairs(allCharacterPositions.in(), collisions.out());
	}else{
		return collisionDetector->queryIntersectionForDefinedPairs(checkAll, allCharacterPositions.in(), collidingLinkPairs.out());
	}
}

DistanceSequence *DynamicsSimulator_impl::checkDistance()
{
    calcWorldForwardKinematics();
    _updateCharacterPositions();
	DistanceSequence_var distances = new DistanceSequence;
	collisionDetector->queryDistanceForDefinedPairs(allCharacterPositions.in(), distances);
	return distances._retn();
}

LinkPairSequence *DynamicsSimulator_impl::checkIntersection(CORBA::Boolean checkAll)
{
    calcWorldForwardKinematics();
    _updateCharacterPositions();
	LinkPairSequence_var pairs = new LinkPairSequence;
	collisionDetector->queryIntersectionForDefinedPairs(checkAll, allCharacterPositions.in(), pairs);
	return pairs._retn();
}

/**
 * constructor
 * @param   orb     reference to ORB
 */
DynamicsSimulatorFactory_impl::DynamicsSimulatorFactory_impl(CORBA::ORB_ptr	orb) :
	orb_(CORBA::ORB::_duplicate(orb))
{
	initializeCommandLabelMaps();
}


DynamicsSimulatorFactory_impl::~DynamicsSimulatorFactory_impl()
{
	PortableServer::POA_var poa = _default_POA();
	PortableServer::ObjectId_var id = poa -> servant_to_id(this);
	poa -> deactivate_object(id);
}


DynamicsSimulator_ptr DynamicsSimulatorFactory_impl::create()
{
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
