/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */
/**
   \file
   \author Shin'ichiro Nakaoka
*/

#ifndef HRPMODEL_SENSOR_H_INCLUDED
#define HRPMODEL_SENSOR_H_INCLUDED

#include <string>
#include <iostream>
#include <vector>
#include <hrpUtil/Eigen3d.h>
#include "Config.h"

namespace hrp {

    class Link;

    class HRPMODEL_API Sensor
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
            RANGE,
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
        Matrix33		localR;
        Vector3			localPos;

        virtual void putInformation(std::ostream& os);

    };


    class HRPMODEL_API ForceSensor : public Sensor
    {
      public:
        static const int TYPE = FORCE;
		
        ForceSensor();
        Vector3 f;
        Vector3 tau;

        virtual void clear();
        virtual void putInformation(std::ostream& os);
    };


    class HRPMODEL_API RateGyroSensor : public Sensor
    {
      public:
        static const int TYPE = RATE_GYRO;

        RateGyroSensor();
        Vector3 w;

        virtual void clear();
        virtual void putInformation(std::ostream& os);
    };


    class HRPMODEL_API AccelSensor : public Sensor
    {
      public:
        EIGEN_MAKE_ALIGNED_OPERATOR_NEW

        static const int TYPE = ACCELERATION;

        AccelSensor();

        Vector3 dv;

        virtual void clear();
        virtual void putInformation(std::ostream& os);

        // The following members are used in the ForwardDynamics class
        typedef Eigen::Vector2d vector2;
        vector2 x[3]; 
        bool isFirstUpdate;
    };

    class HRPMODEL_API RangeSensor : public Sensor
    {
      public:
        static const int TYPE = RANGE;

        RangeSensor();

        double scanAngle, scanStep, scanRate, maxDistance;  
        std::vector<double> distances;
        double nextUpdateTime;
    };

    class HRPMODEL_API VisionSensor : public Sensor
    {
      public:
        static const int TYPE = VISION;

        VisionSensor();
        int width, height;
        double far, near, fovy;
    };
};


#endif
