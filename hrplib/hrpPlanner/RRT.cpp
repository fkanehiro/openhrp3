// -*- mode: c++; indent-tabs-mode: nil; c-basic-offset: 4; tab-width: 4; -*-
#include "Roadmap.h"
#include "RoadmapNode.h"
#include "RRT.h"

using namespace PathEngine;

static bool debug=false;
//static bool debug=true;

RRT::RRT(PathPlanner* plan) : Algorithm(plan) 
{
    // set default properties
    properties_["max-trials"] = "10000";
    properties_["eps"] = "0.1";

    Ta_ = roadmap_;
    Tb_ = new Roadmap(planner_);

    extendFromStart_ = true;
    extendFromGoal_ = false;
}

RRT::~RRT() 
{
    delete Tb_;
}

int RRT::extend(Roadmap *tree, Position& qRand, bool reverse) {
    if (debug) std::cout << "RRT::extend("<< qRand << ", " << reverse << ")" 
                         << std::endl;

    RoadmapNode* minNode;
    double min;
    tree->findNearestNode(qRand, minNode, min);
    if (debug) std::cout << "nearest : pos = (" << minNode->position() 
                         << "), d = " << min << std::endl;

    if (minNode != NULL) {
        Mobility* mobility = planner_->getMobility();

        if (min > eps_){
            Position qRandOrg = qRand;
            qRand = mobility->interpolate(minNode->position(), qRand, eps_/min);
            if (debug) std::cout << "qRand = (" << qRand << ")" << std::endl;
            if (mobility->distance(minNode->position(), qRand) > min){
                std::cout << "distance didn't decrease" << std::endl;
                std::cout << "qRandOrg : (" << qRandOrg << "), d = " << min
                          << std::endl;
                std::cout << "qRand    : (" << qRand << "), d = " 
                          << mobility->distance(minNode->position(), qRand)
                          << std::endl;
                getchar();
            }
        }

        if (reverse){
            if (mobility->isReachable(qRand, minNode->position())){
                RoadmapNode* newNode = new RoadmapNode(qRand);
                tree->addNode(newNode);
                tree->addEdge(newNode, minNode);
                if (min <= eps_) {
                    if (debug) std::cout << "reached(" << qRand << ")"<< std::endl;
                    return Reached;
                }
                else {
                    if (debug) std::cout << "advanced(" << qRand << ")" << std::endl;
                    return Advanced;
                }
            } else {
                if (debug) std::cout << "trapped" << std::endl;
                return Trapped;
            }
        }else{
            if (mobility->isReachable(minNode->position(), qRand)){
                RoadmapNode* newNode = new RoadmapNode(qRand);
                tree->addNode(newNode);
                tree->addEdge(minNode, newNode);
                if (min <= eps_) {
                    if (debug) std::cout << "reached(" << qRand << ")"<< std::endl;
                    return Reached;
                }
                else {
                    if (debug) std::cout << "advanced(" << qRand << ")" << std::endl;
                    return Advanced;
                }
            } else {
                if (debug) std::cout << "trapped" << std::endl;
                return Trapped;
            }
        }
    }

    return Trapped;
}

int RRT::connect(Roadmap *tree,const Position &qNew, bool reverse) {
    if (debug) std::cout << "RRT::connect(" << qNew << ")" << std::endl;

    int ret = Reached;
    Position q = qNew;

    do {
        ret = extend(tree, q, reverse);
        q = qNew;
    } while (ret == Advanced);
    return ret;
}

void RRT::path() {
    //std::cout << "RRT::path" << std::endl;
    RoadmapNode* startMidNode = Ta_->lastAddedNode();
    RoadmapNode* goalMidNode  = Tb_->lastAddedNode();

    path_.clear();

    RoadmapNode* node = startMidNode;
    do {
        path_.insert(path_.begin(), node->position());
        node = node->parent(0);
    } while (node != NULL);

    node = goalMidNode;
    do {
        path_.push_back(node->position());
        node = node->child(0);
    } while (node != NULL);

#if 0
    startMidNode->children_.push_back(goalMidNode);
    goalMidNode->parent_ = startMidNode;
#endif
}

bool RRT::calcPath() 
{
    std::cout << "RRT::calcPath" << std::endl;

    // 回数
    times_ = atoi(properties_["max-trials"].c_str());

    // eps
    eps_ = atof(properties_["eps"].c_str());

    std::cout << "times:" << times_ << std::endl;
    std::cout << "eps:" << eps_ << std::endl;

    RoadmapNode* startNode = new RoadmapNode(start_);
    RoadmapNode* goalNode  = new RoadmapNode(goal_);

    Ta_->addNode(startNode);
    Tb_->addNode(goalNode);

    bool isTaStart = true;
    bool isSucceed = false;
  
    for (int i=0; i<times_; i++) {
        if (!isRunning_) break;
        printf("%5d/%5d\r", i+1, times_); fflush(stdout);
    
        Position qNew = Position::random();
        if (extendFromStart_ && extendFromGoal_){
            
            if (isTaStart){
                if (extend(Ta_, qNew) != Trapped) {
                    if (connect(Tb_, qNew, true) == Reached) {
                        isSucceed = true;
                        break;
                    }
                }
            }else{
                if (extend(Tb_, qNew, true) != Trapped) {
                    if (connect(Ta_, qNew) == Reached) {
                        isSucceed = true;
                        break;
                    }
                }
            }
            isTaStart = !isTaStart;
        }else if (extendFromStart_ && !extendFromGoal_){
            if (extend(Ta_, qNew) != Trapped) {
                Position p = goal_;
                int ret;
                do {
                    ret = extend(Ta_, p);
                    p = goal_;
                }while (ret == Advanced);
                if (ret == Reached){
                    isSucceed = true;
                    break;
                }
            }
        }else if (!extendFromStart_ && extendFromGoal_){
        }
    }
  
    path();
    Tb_->integrate(Ta_);

    std::cout << std::endl << "fin.(calcPath), retval = " << isSucceed << std::endl;
    return isSucceed;
}

void RRT::swapTrees()
{
    Roadmap *tmp = Ta_;
    Ta_ = Tb_;
    Tb_ = tmp;
}
