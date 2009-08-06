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
        const PathEngine::Position& pos = node->position(); 
        graph[i].cfg[0] = pos.getX();
        graph[i].cfg[1] = pos.getY();
        graph[i].cfg[2] = pos.getTheta();

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
    PathEngine::Position pos(x, y, theta);
    path_->setStartPosition(pos);
    std::cout << "fin. " << std::endl;
}

void OpenHRP_PathPlannerSVC_impl::setGoalPosition(CORBA::Double x, CORBA::Double y, CORBA::Double theta)
{
    std::cout << "setGoalPosition(" << x << ", " << y << ", " << theta << ")" << std::endl;
    PathEngine::Position pos(x, y, theta);
    path_->setGoalPosition(pos);
    std::cout << "fin. " << std::endl;
}

void OpenHRP_PathPlannerSVC_impl::setProperties(const OpenHRP::PathPlanner::Property& properites)
{
    std::cout << "setProperties()" << std::endl;
    std::map<std::string, std::string> prop;
    for (unsigned int i=0; i<properites.length(); i++) {
        std::string name(properites[i][0]);
        std::string value(properites[i][1]);
        std::cout << name << ": " << value << std::endl;
        prop.insert(std::map<std::string, std::string>::value_type(name, value));
    }
    path_->setProperties(prop);

    if (prop.count("z-pos") > 0) {
        path_->setZPos(atof(prop["z-pos"].c_str()));
    }
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
    const std::vector<PathEngine::Position>& p = path_->getPath();

    path = new OpenHRP::PathPlanner::PointArray;
    path->length(p.size());
    std::cout << "length of path = " << p.size() << std::endl;
    for (unsigned int i=0; i<p.size(); i++) {
        //std::cerr << i << " : " << p[i] << std::endl;
        path[i].length(3);
        path[i][0] = p[i].getX();
        path[i][1] = p[i].getY();
        path[i][2] = p[i].getTheta();
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
