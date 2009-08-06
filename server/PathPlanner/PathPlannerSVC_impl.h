// -*-C++-*-
/*!
 * @file  PathPlannerSVC_impl.h
 * @brief Service implementation header of PathPlanner.idl
 *
 */
#ifndef PATHPLANNERSVC_IMPL_H
#define PATHPLANNERSVC_IMPL_H

#include <rtm/RTC.h>
#include <map>
#include <hrpCorba/PathPlanner.hh>
#include <hrpPlanner/PathPlanner.h>
 
/*
 * Example class implementing IDL interface OpenHRP::PathPlanner
 */
class OpenHRP_PathPlannerSVC_impl
    : public virtual POA_OpenHRP::PathPlanner,
      public virtual PortableServer::RefCountServantBase
{
private:
    // Make sure all instances are built on the heap by making the
    // destructor non-public
    //virtual ~OpenHRP_PathPlannerSVC_impl();

    /**
     * 計画経路エンジン
     */
    PathEngine::PathPlanner* path_;

    /**
     * OpenHRP が使用しているネームサーバ
     */
    std::string nameServer_;

public:
    // standard constructor
    OpenHRP_PathPlannerSVC_impl();
    virtual ~OpenHRP_PathPlannerSVC_impl();

    // attributes and operations
    void stopPlanning();
    void getRoadmap(OpenHRP::PathPlanner::Roadmap_out graph);
    void clearRoadmap();
    void getMobilityNames(OpenHRP::StringSequence_out mobilities);
    void getOptimizerNames(OpenHRP::StringSequence_out optimizers);
    void setRobotName(const char* model);
    void setAlgorithmName(const char* algorithm);
    bool setMobilityName(const char* mobility);
    void getAlgorithmNames(OpenHRP::StringSequence_out algos);
    bool getProperties(const char* alg, OpenHRP::StringSequence_out props, OpenHRP::StringSequence_out defaults);
    void initPlanner();
    void setStartPosition(CORBA::Double x, CORBA::Double y, CORBA::Double theta);
    void setGoalPosition(CORBA::Double x, CORBA::Double y, CORBA::Double theta);
    void setProperties(const OpenHRP::PathPlanner::Property& properites);
    CORBA::Boolean calcPath();
    void getPath(OpenHRP::PathPlanner::PointArray_out path);
    CORBA::Boolean optimize(const char *optimizer);
    void registerIntersectionCheckPair(const char* char1, const char* name1, const char* char2, const char* name2, CORBA::Double tolerance);
    void registerCharacter(const char* name, OpenHRP::BodyInfo_ptr cInfo);
    void registerCharacterByURL(const char* name, const char* url);
    void setCharacterPosition(const char* character,
                              const OpenHRP::DblSequence& pos); 
    void initSimulation();
    void setNameServer(std::string nameServer) {nameServer_ = nameServer;}

};



#endif // PATHPLANNERSVC_IMPL_H


