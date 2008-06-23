// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-
#ifndef OPENHRP_SENSOR_HEADER
#define OPENHRP_SENSOR_HEADER

/**
   @file DynamicsSimulator/server/Sensor.h
 */

#include <string>
#include <iostream>
#include "tvmet3d.h"

#include "hrpModelExportDef.h"

namespace OpenHRP {

	class Link;

	class HRPMODEL_EXPORT Sensor
	{
	public:

		enum SensorType {
			COMMON = 0,
			FORCE,
			RATE_GYRO,
			ACCELERATION,
			PRESSURE,
			PHOTO_INTERRUPTER,
			VISION,
			TORQUE,
			NUM_SENSOR_TYPES
		};

		static const int TYPE = COMMON;
		
        Sensor(); 
        virtual ~Sensor();

        static Sensor* create(int type);
		static void destroy(Sensor* sensor);

		virtual void operator=(const Sensor& org);

        virtual void clear();
		
		std::string		name;
		int				type;
		int				id;
		Link*			link;
		matrix33		localR;
		vector3			localPos;

		virtual void putInformation(std::ostream& os);

	};


	class HRPMODEL_EXPORT ForceSensor : public Sensor
	{
	public:
		static const int TYPE = FORCE;
		
        ForceSensor();
		vector3 f;
		vector3 tau;

        virtual void clear();
		virtual void putInformation(std::ostream& os);
	};


	class HRPMODEL_EXPORT RateGyroSensor : public Sensor
	{
	public:
		static const int TYPE = RATE_GYRO;

        RateGyroSensor();
		vector3 w;

        virtual void clear();
		virtual void putInformation(std::ostream& os);
	};


	class HRPMODEL_EXPORT AccelSensor : public Sensor
	{
	public:
		static const int TYPE = ACCELERATION;

        AccelSensor();

		vector3 dv;

        virtual void clear();
		virtual void putInformation(std::ostream& os);

		// The following members are used in the ForwardDynamics class
		typedef tvmet::Vector<double, 2> vector2;
		vector2 x[3]; 
		bool isFirstUpdate;
	};

};


#endif
