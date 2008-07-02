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

#ifndef OPENHRP_TIME_MEASURE_H_INCLUDED
#define OPENHRP_TIME_MEASURE_H_INCLUDED


#ifndef __WIN32__

#include <sys/time.h>

class TimeMeasure
{
    struct timeval tv;
    double time_;
    double totalTime_;
    int numCalls;
    
 public:
    TimeMeasure() {
	totalTime_ = 0.0;
	numCalls = 0;
    }

    void begin() {
	gettimeofday(&tv, 0);
    }

    void end(){
	double beginTime = tv.tv_sec + (double)tv.tv_usec * 1.0e-6;
	gettimeofday(&tv, 0);
	double endTime = tv.tv_sec + (double)tv.tv_usec * 1.0e-6;
	time_ = endTime - beginTime;
	totalTime_ += time_;
	numCalls++;
    }

    double time() { return time_; }
    double totalTime() { return totalTime_; }
    double avarageTime() { return totalTime_ / numCalls; }

};


#else

class TimeMeasure
{
 public:
    TimeMeasure() { }
    void begin() { }
    void end(){ }
	double time() { return 0.0; }
    double totalTime() { return 0.0; }
    double avarageTime() { return 0.0; }
};

#endif


#endif
