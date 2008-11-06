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
#ifndef OPENHRP_WORLD_H_INCLUDED
#define OPENHRP_WORLD_H_INCLUDED

#include <vector>
#include <map>
#include <list>
#include <vector>
#include <fMatrix3.h>
#include <hrpCorba/DynamicsSimulator.hh>
#include "hrpModelExportDef.h"

class pSim;
class Joint;
class SDContactPair;

namespace OpenHRP {

	class CollisionSequence;
	class Sensor;
	class ForceSensor;
	class RateGyroSensor;
	class AccelSensor;
    

    class HRPMODEL_EXPORT World
    {
//		friend class CharacterInfo;
		class CharacterInfo
		{
			friend class World;
			CharacterInfo(Joint* _root, const std::string& _name): name(_name) {
				root = _root;
				n_joints = 0;
			}
			std::string name;
			std::vector<Joint*> links;
			std::vector<int> jointIDs;
			int n_joints;
			Joint* root;
		public:
			~CharacterInfo() {
			}
			
		};
    public:
        World();
        ~World();

		void clearCollisionPairs();

		void setTimeStep(double);
		double timeStep(void) const { return timeStep_; }
	
		void setCurrentTime(double);
		double currentTime(void) const { return currentTime_; }
	
		void setGravityAcceleration(const fVec3& g);
		const fVec3& getGravityAcceleration() { return g; }

		void enableSensors(bool on);
		
		void setEulerMethod();
		void setRungeKuttaMethod();

		void initialize();
		void calcNextState(CollisionSequence& corbaCollisionSequence);

//		std::pair<int,bool> getIndexOfLinkPairs(BodyPtr body1, Link* link1, BodyPtr body2, Link* link2);

		pSim* Chain() {
			return chain;
		}

		int addSensor(Joint* jnt, int sensorType, int id, const std::string name, const fVec3& _localPos, const fMat33& _localR);

		Sensor* findSensor(const char* sensorName, const char* charName);

		int numSensors(int sensorType, const char* charName);
		int numSensors() {
			return sensors.size();
		}

		void getAllCharacterData(const char* name, OpenHRP::DynamicsSimulator::LinkDataType type, DblSequence_out& rdata);
		void setAllCharacterData(const char* name, OpenHRP::DynamicsSimulator::LinkDataType type, const DblSequence& wdata);
		void getAllCharacterPositions(CharacterPositionSequence& all_char_pos);
		void getAllSensorStates(SensorStateSequence& all_sensor_states);
		void calcCharacterJacobian(const char* characterName, const char* baseLink, const char* targetLink, fMat& J);

		void addCollisionCheckLinkPair(Joint* jnt1, Joint* jnt2, double staticFriction, double slipFriction, double epsilon);

		void addCharacter(Joint* rjoint, const std::string& _name, LinkInfoSequence_var links);
		Joint* rootJoint(int index);
		int numLinks(int index) {
			return characters[index].links.size();
		}
		int numJoints(int index) {
			return characters[index].n_joints;
		}
		
		int numCharacter() {
			return characters.size();
		}

	protected:
		pSim* chain;
		std::vector<SDContactPair*> contact_pairs;
		std::vector<OpenHRP::Sensor*> sensors;

		std::vector<CharacterInfo> characters;

	private:
		
		void _get_all_character_data_sub(Joint* cur, int index, OpenHRP::DynamicsSimulator::LinkDataType type, DblSequence_out& rdata);
		void _set_all_character_data_sub(Joint* cur, int index, OpenHRP::DynamicsSimulator::LinkDataType type, const DblSequence& wdata);
		void _get_all_sensor_states_sub(Joint* cur, int& count, SensorState& state);

        double currentTime_;
        double timeStep_;

		void update_force_sensor(ForceSensor* fs);
		void update_rate_gyro_sensor(RateGyroSensor* rgs);
		void update_accel_sensor(AccelSensor* as);
#if 0
        typedef std::map<std::string, int> NameToIndexMap;
        NameToIndexMap nameToBodyIndexMap;

		typedef std::map<BodyPtr, int> BodyToIndexMap;
        BodyToIndexMap bodyToIndexMap;
		
        struct LinkPairKey {
			BodyPtr body1;
			BodyPtr body2;
			Link* link1;
			Link* link2;
			bool operator<(const LinkPairKey& pair2) const;
		};
		typedef std::map<LinkPairKey, int> LinkPairKeyToIndexMap;
		LinkPairKeyToIndexMap linkPairKeyToIndexMap;
#endif

		int numRegisteredLinkPairs;
		
        fVec3 g;

        bool isEulerMethod; // Euler or Runge Kutta ?

		bool sensorsAreEnabled;
		
	};


};

#endif
