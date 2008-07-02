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
#ifndef NEW_DYNAMICSSIMULATOR_IMPL_HEADER
#define NEW_DYNAMICSSIMULATOR_IMPL_HEADER

/** @file DynamicsSimulator/server/DynamicsSimulator_impl.h
 *
 */

#include <boost/scoped_ptr.hpp>

#include <ORBwrap.h>
#include <ModelLoader.h>
#include <CollisionDetector.h>
#include <DynamicsSimulator.h>

#include "World.h"
#include "TimeMeasure.h"


namespace OpenHRP {

	/**
	 * DynamicsSimulator_impl class
	 */
	class DynamicsSimulator_impl : virtual public POA_OpenHRP::DynamicsSimulator,
								   virtual public PortableServer::RefCountServantBase
	{
		/**
		 * ORB
		 */
		CORBA::ORB_var orb_;

		OpenHRP::World world;

		CollisionDetector_var collisionDetector;

		CollisionSequence_var collisions;

		CharacterPositionSequence_var allCharacterPositions;
		bool needToUpdatePositions;

		SensorStateSequence_var allCharacterSensorStates;
		bool needToUpdateSensorStates;

		bool isFirstSimulationLoop;

		TimeMeasure timeMeasure1;
		TimeMeasure timeMeasure2;
		TimeMeasure timeMeasure3;
		bool timeMeasureFinished;

		void _setupCharacterData();
		void _updateCharacterPositions();
		void _updateSensorStates();

		void registerCollisionPair2CD(
				const std::string &, const std::string &,
				const std::string &, const std::string &);

	public:

		DynamicsSimulator_impl(CORBA::ORB_ptr orb);

		~DynamicsSimulator_impl();


		virtual void destroy();

		virtual void registerCharacter(
				const char *name,
				BodyInfo_ptr cinfo);

		virtual void init(
				CORBA::Double timeStep,
				OpenHRP::DynamicsSimulator::IntegrateMethod integrateOpt,
				OpenHRP::DynamicsSimulator::SensorOption sensorOpt);
    
		virtual void registerCollisionCheckPair(
				const char* char1, 
				const char* name1, 
				const char* char2,
				const char* name2,
				CORBA::Double staticFriction, 
				CORBA::Double slipFriction,
				const DblSequence6& K,
				const DblSequence6& C);

		virtual void registerVirtualLink(
				const char* char1,
				const char* link1,
				const char* char2,
				const char* link2,
				const LinkPosition& relTransform,
				CORBA::Short transformDefined,
				const DblSequence9& constraint,
				const char* connectionName);

		// TODO
		virtual void getConnectionConstraintForce(
				const char* characterName, 
				const char* connectionName, 
				DblSequence6_out contactForce);

		virtual void getCharacterSensorValues(
				const char* characterName,
				const char* sensorName,
				DblSequence_out values);

		virtual void initSimulation();

		virtual void stepSimulation();

		virtual void setCharacterLinkData(
				const char* characterName, 
				const char* link, 
				OpenHRP::DynamicsSimulator::LinkDataType type, 
				const DblSequence& data);

		virtual void getCharacterLinkData(
				const char* characterName,
				const char* link,
				OpenHRP::DynamicsSimulator::LinkDataType type,
				DblSequence_out rdata);

		virtual void getCharacterAllLinkData(
				const char* characterName,
				OpenHRP::DynamicsSimulator::LinkDataType type,
				DblSequence_out wdata);
   
		virtual void setCharacterAllLinkData(
				const char* characterName, 
				OpenHRP::DynamicsSimulator::LinkDataType type,
				const DblSequence& wdata);

		virtual void setGVector(const DblSequence3& wdata);

		virtual void getGVector(DblSequence3_out wdata);

		virtual void setCharacterAllJointModes(
				const char* characterName, 
				OpenHRP::DynamicsSimulator::JointDriveMode jointMode);

		//! not implemented as a server function (08/13/2007)
		virtual CORBA::Boolean calcCharacterInverseKinematics(
				const char* characterName,
				const char* baseLink,
				const char* targetLink,
				const LinkPosition& target);

		virtual void calcCharacterForwardKinematics(
				const char* characterName);

		virtual void calcWorldForwardKinematics();
		
		virtual void getWorldState(WorldState_out state);

		virtual void getCharacterSensorState(const char* characterName, SensorState_out sstate);

		virtual CORBA::Boolean getCharacterCollidingPairs(
				const char* characterName, 
				LinkPairSequence_out pairs);

		virtual void calcCharacterJacobian(
				const char* characterName,
				const char* baseLink,
				const char* targetLink,
				DblSequence_out jacobian);

		virtual void checkCollision();
	};

	/**
	 * DynamicsSimulatorFactory class
	 *
	 * @author Ichitaro Kohara (Kernel Co.,Ltd.)
	 * @version 	0.1
	 * @date 	2000/03/22
	 */
	class DynamicsSimulatorFactory_impl: virtual public POA_OpenHRP::DynamicsSimulatorFactory
	{

	private:
		/**
		 * ORB
		 */
		CORBA::ORB_var orb_;
    
	public:

		/**
		 * constructor
		 * @param orb     reference to ORB
		 * @param collisionCheckerFactory reference to collision detector
		 */
		DynamicsSimulatorFactory_impl(CORBA::ORB_ptr orb);

		/**
		 * destructor
		 */
		~DynamicsSimulatorFactory_impl();
		
		/**
		 * create integrator
		 */
		DynamicsSimulator_ptr create();
		
		void shutdown();
		
	};
}

#endif
