/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

#ifndef OPENHRP_UTIL_TIME_MEASURE_H_INCLUDED
#define OPENHRP_UTIL_TIME_MEASURE_H_INCLUDED


#ifndef __WIN32__

#include <sys/time.h>

class TimeMeasure
{
    struct timeval tv;
    double time_;
    double maxTime_;
    double totalTime_;
    int numCalls_;
    
public:
    TimeMeasure() {
	totalTime_ = 0.0;
        maxTime_ = 0.0;
	numCalls_ = 0;
    }

    void begin() {
	gettimeofday(&tv, 0);
    }

    void end(){
	double beginTime = tv.tv_sec + (double)tv.tv_usec * 1.0e-6;
	gettimeofday(&tv, 0);
	double endTime = tv.tv_sec + (double)tv.tv_usec * 1.0e-6;
	time_ = endTime - beginTime;
        if (time_ > maxTime_) maxTime_ = time_; 
	totalTime_ += time_;
	numCalls_++;
    }

    double time() const { return time_; }
    double totalTime() const { return totalTime_; }
    double maxTime() const { return maxTime_; }
    double averageTime() const { return totalTime_ / numCalls_; }
    int numCalls() const { return numCalls_; }
};


#else
#include <windows.h>
typedef unsigned __int64    ulonglong;

class TimeMeasure
{
    ulonglong iTimerScale;
    ulonglong beginTime;
    ulonglong endTime;
    double time_;
    double maxTime_;
    double totalTime_;
    int numCalls_;
 
public:
    TimeMeasure() { 
        totalTime_ = 0.0;
        maxTime_ = 0.0;
        numCalls_ = 0;
        BOOL iDummyBool = QueryPerformanceFrequency ((LARGE_INTEGER *) &iTimerScale);
        if(!iDummyBool)
            iTimerScale=1;
    }

    void begin() { 
        BOOL iDummyBool = QueryPerformanceCounter ((LARGE_INTEGER *) &beginTime);
        if(!iDummyBool)
            beginTime=1;
    }

    void end(){ 
        BOOL iDummyBool = QueryPerformanceCounter ((LARGE_INTEGER *) &endTime);
        if(!iDummyBool)
            endTime=0;
        time_ = (double)(endTime - beginTime) / iTimerScale;
        if (time_ > maxTime_) maxTime_ = time_; 
        totalTime_ += time_;
        numCalls_++;
    }
    double time() const { return time_; }
    double totalTime() const { return totalTime_; }
    double maxTime() const { return maxTime_; }
    double averageTime() const { return totalTime_ / numCalls_; }
    int numCalls() const { return numCalls_; }
};

#endif


#endif
