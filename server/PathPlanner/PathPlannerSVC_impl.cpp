// -*-C++-*-
/*!
 * @file  PathPlannerSVC_impl.cpp
 * @brief Service implementation code of PathPlanner.idl
 *
 */

#include "PathPlannerSVC_impl.h"

#include <hrpPlanner/Roadmap.h>
#include <hrpPlanner/RoadmapNode.h>
#include <hrpPlanner/TimeUtil.h>

/*
 * Implementational code for IDL interface OpenHRP::PathPlanner
 */
OpenHRP_PathPlannerSVC_impl::OpenHRP_PathPlannerSVC_impl()
{
    path_ = new PathEngine::PathPlanner(false);
    PathEngine::Configuration::size(3);
    PathEngine::Configuration::unboundedRotation(2, true);
    PathEngine::Configuration::bounds(0,-2,2);
    PathEngine::Configuration::bounds(1,-2,2);
}


OpenHRP_PathPlannerSVC_impl::~OpenHRP_PathPlannerSVC_impl()
{
    delete path_;
}

void OpenHRP_PathPlannerSVC_impl::stopPlanning()
{
    path_->stopPlanning();
}

/*
 * Methods corresponding to IDL attributes and operations
 */
void OpenHRP_PathPlannerSVC_impl::getRoadmap(OpenHRP::PathPlanner::Roadmap_out graph)
{
    std::cout << "getRoadmap()" << std::endl;

    PathEngine::Roadmap *roadmap = path_->getRoadmap();

    graph = new OpenHRP::PathPlanner::Roadmap;
    std::cout << "the number of nodes = " << roadmap->nNodes() << std::endl;
    graph->length(roadmap->nNodes());
  
    for (unsigned int i=0; i<roadmap->nNodes(); i++) {
        PathEngine::RoadmapNode *node = roadmap->node(i);
        const PathEngine::Configuration& pos = node->position(); 
        graph[i].cfg[0] = pos.value(0);
        graph[i].cfg[1] = pos.value(1);
        graph[i].cfg[2] = pos.value(2);

        graph[i].neighbors.length(node->nChildren());
        for (unsigned int j=0; j<node->nChildren(); j++) {
            graph[i].neighbors[j] = roadmap->indexOfNode(node->child(j));
        }
    }
}

void OpenHRP_PathPlannerSVC_impl::clearRoadmap()
{
    PathEngine::Roadmap *rm = path_->getRoadmap();
    rm->clear();
}

void OpenHRP_PathPlannerSVC_impl::getMobilityNames(OpenHRP::StringSequence_out mobilities)
{
    mobilities = new OpenHRP::StringSequence;
    std::vector<std::string> mobilityNames;
    path_->getMobilityNames(mobilityNames);
    mobilities->length(mobilityNames.size());

    for (unsigned int i=0; i<mobilityNames.size(); i++) {
        mobilities[i] = CORBA::string_dup(mobilityNames[i].c_str());
    }
}

void OpenHRP_PathPlannerSVC_impl::getOptimizerNames(OpenHRP::StringSequence_out optimizers)
{
    optimizers = new OpenHRP::StringSequence;
    std::vector<std::string> optimizerNames;
    path_->getOptimizerNames(optimizerNames);
    optimizers->length(optimizerNames.size());

    for (unsigned int i=0; i<optimizerNames.size(); i++) {
        optimizers[i] = CORBA::string_dup(optimizerNames[i].c_str());
    }
}

void OpenHRP_PathPlannerSVC_impl::setRobotName(const char* model)
{
    path_->setRobotName(model);
}

void OpenHRP_PathPlannerSVC_impl::setAlgorithmName(const char* algorithm)
{
    path_->setAlgorithmName(algorithm);
}

bool OpenHRP_PathPlannerSVC_impl::setMobilityName(const char* mobility)
{
    return path_->setMobilityName(mobility);
}

void OpenHRP_PathPlannerSVC_impl::getAlgorithmNames(OpenHRP::StringSequence_out algos)
{
    algos = new OpenHRP::StringSequence;
    std::vector<std::string> algoNames;
    path_->getAlgorithmNames(algoNames);
    algos->length(algoNames.size());

    for (unsigned int i=0; i<algoNames.size(); i++) {
        algos[i] = CORBA::string_dup(algoNames[i].c_str());
    }
}

bool OpenHRP_PathPlannerSVC_impl::getProperties(const char* alg, OpenHRP::StringSequence_out props, OpenHRP::StringSequence_out defaults)
{
    props = new OpenHRP::StringSequence;
    defaults = new OpenHRP::StringSequence;


    std::vector<std::string> propNames;
    std::vector<std::string> defaultValues;

    propNames.push_back("weight-x");
    defaultValues.push_back("1.0");
    propNames.push_back("weight-y");
    defaultValues.push_back("1.0");
    propNames.push_back("weight-theta");
    defaultValues.push_back("1.0");
    
    propNames.push_back("min-x");
    defaultValues.push_back("-2");
    propNames.push_back("min-y");
    defaultValues.push_back("-2");
    propNames.push_back("max-x");
    defaultValues.push_back("2");
    propNames.push_back("max-y");
    defaultValues.push_back("2");
    
    if (!path_->getProperties(alg, propNames, defaultValues)) return false;

    props->length(propNames.size());
    defaults->length(propNames.size());

    for (unsigned int i=0; i<propNames.size(); i++) {
        props[i] = CORBA::string_dup(propNames[i].c_str());
        defaults[i] = CORBA::string_dup(defaultValues[i].c_str());
    }
    return true;
}

void OpenHRP_PathPlannerSVC_impl::initPlanner()
{
    std::cout << "initPlanner()" << std::endl;
    path_->initPlanner(nameServer_);
    std::cout << "fin. " << std::endl;
}

void OpenHRP_PathPlannerSVC_impl::setStartPosition(CORBA::Double x, CORBA::Double y, CORBA::Double theta)
{
    std::cout << "setStartPosition(" << x << ", " << y << ", " << theta << ")" << std::endl;
    PathEngine::Configuration pos;
    pos.value(0) = x;
    pos.value(1) = y;
    pos.value(2) = theta;
    path_->setStartConfiguration(pos);
    std::cout << "fin. " << std::endl;
}

void OpenHRP_PathPlannerSVC_impl::setGoalPosition(CORBA::Double x, CORBA::Double y, CORBA::Double theta)
{
    std::cout << "setGoalPosition(" << x << ", " << y << ", " << theta << ")" << std::endl;
    PathEngine::Configuration pos;
    pos.value(0) = x;
    pos.value(1) = y;
    pos.value(2) = theta;
    path_->setGoalConfiguration(pos);
    std::cout << "fin. " << std::endl;
}

void OpenHRP_PathPlannerSVC_impl::setProperties(const OpenHRP::PathPlanner::Property& properites)
{
    std::cout << "setProperties()" << std::endl;
    std::map<std::string, std::string> prop;
    for (unsigned int i=0; i<properites.length(); i++) {
        std::string name(properites[i][0]);
        std::string value(properites[i][1]);
        //std::cout << name << ": " << value << std::endl;
        if (name == "min-x"){
            PathEngine::Configuration::lbound(0) = atof(value.c_str());
        }else if (name == "max-x"){
            PathEngine::Configuration::ubound(0) = atof(value.c_str());
        }else if (name == "min-y"){
            PathEngine::Configuration::lbound(1) = atof(value.c_str());
        }else if (name == "max-y"){
            PathEngine::Configuration::ubound(1) = atof(value.c_str());
        }else if (name == "weight-x"){
            PathEngine::Configuration::weight(0) = atof(value.c_str());
        }else if (name == "weight-y"){
            PathEngine::Configuration::weight(1) = atof(value.c_str());
        }else if (name == "weight-theta"){
            PathEngine::Configuration::weight(2) = atof(value.c_str());
        }else{
            prop.insert(std::map<std::string, std::string>::value_type(name, value));
        }
    }
    path_->setProperties(prop);

    std::cout << "fin. " << std::endl;
}

CORBA::Boolean OpenHRP_PathPlannerSVC_impl::calcPath()
{
    std::cout << "OpenHRP_PathPlannerSVC_impl::calcPath()" << std::endl;
    tick_t t1 = get_tick(); 
    bool status = path_->calcPath();
    std::cout << "OpenHRP_PathPlannerSVC_impl::fin." << std::endl;
    std::cout << "total computation time = " << tick2sec(get_tick()-t1) << "[s]" 
              << std::endl;
    std::cout << "computation time for collision check = " 
              << path_->timeCollisionCheck() << "[s]" << std::endl;
    std::cout << "computation time for forward kinematics = " 
              << path_->timeForwardKinematics() << "[s]" << std::endl;
    std::cout << "collision check function was called " 
              << path_->countCollisionCheck() << " times" << std::endl;
    return status;
}


void OpenHRP_PathPlannerSVC_impl::getPath(OpenHRP::PathPlanner::PointArray_out path)
{
    std::cerr << "OpenHRP_PathPlannerSVC_impl::getPath()" << std::endl;
    const std::vector<PathEngine::Configuration>& p = path_->getPath();

    path = new OpenHRP::PathPlanner::PointArray;
    path->length(p.size());
    std::cout << "length of path = " << p.size() << std::endl;
    for (unsigned int i=0; i<p.size(); i++) {
        //std::cerr << i << " : " << p[i] << std::endl;
        path[i].length(3);
        path[i][0] = p[i].value(0);
        path[i][1] = p[i].value(1);
        path[i][2] = p[i].value(2);
    }
    std::cerr << "OpenHRP_PathPlannerSVC_impl::fin. length of path = " 
              << p.size() << std::endl;
}

void OpenHRP_PathPlannerSVC_impl::registerIntersectionCheckPair(const char* char1, const char* name1, const char* char2, const char* name2, CORBA::Double tolerance)
{
    path_->registerIntersectionCheckPair(char1,
                                         name1,
                                         char2,
                                         name2,
                                         tolerance);
}

void OpenHRP_PathPlannerSVC_impl::registerCharacter(const char* name, OpenHRP::BodyInfo_ptr cInfo)
{
    path_->registerCharacter(name, cInfo);
}

void OpenHRP_PathPlannerSVC_impl::registerCharacterByURL(const char* name, const char* url)
{
    path_->registerCharacterByURL(name, url);
}

void OpenHRP_PathPlannerSVC_impl::setCharacterPosition(const char* character,
						       const OpenHRP::DblSequence& pos)
{
    path_->setCharacterPosition(character, pos);
} 


void OpenHRP_PathPlannerSVC_impl::initSimulation()
{
    std::cout << "initSimulation()" << std::endl;
    path_->initSimulation();
    std::cout << "fin. " << std::endl;
}

// End of example implementational code


CORBA::Boolean OpenHRP_PathPlannerSVC_impl::optimize(const char *optimizer)
{
    return path_->optimize(optimizer);
}
