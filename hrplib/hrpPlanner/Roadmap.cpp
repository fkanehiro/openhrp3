// -*- mode: c++; indent-tabs-mode: nil; c-basic-offset: 4; tab-width: 4; -*-
#include "PathPlanner.h"
#include "Mobility.h"
#include "RoadmapNode.h"
#include "Roadmap.h"

using namespace PathEngine;

void Roadmap::clear()
{
    for (unsigned int i=0; i<nodes_.size(); i++){
        delete nodes_[i];
    }
    nodes_.clear();
    m_nEdges = 0;
}

Roadmap::~Roadmap()
{
    clear();
}

void Roadmap::addEdge(RoadmapNode *from, RoadmapNode *to)
{
    from->addChild(to);
    to->addParent(from); 
    m_nEdges++;
}

void Roadmap::integrate(Roadmap *rdmp)
{
    for (unsigned int i=0; i<nodes_.size(); i++){
        rdmp->addNode(nodes_[i]);
    }
    nodes_.clear();
}

RoadmapNode *Roadmap::node(unsigned int index)
{
    if (index >= nodes_.size()) return NULL;

    return nodes_[index];
}

void Roadmap::findNearestNode(const Configuration& pos,
                              RoadmapNode *& node, double &distance)
{
    if (nodes_.size() == 0){
        node = NULL;
        return;
    }

    node = nodes_[0];
    Mobility *mobility = planner_->getMobility();
    distance = mobility->distance(node->position(), pos);
    
    double d;
    for (unsigned int i=1; i<nodes_.size(); i++) {
        d = mobility->distance(nodes_[i]->position(), pos);
        if (d < distance) {
            distance = d;
            node = nodes_[i];
        }
    }
}

RoadmapNode *Roadmap::lastAddedNode()
{
    if (nodes_.size() == 0) return NULL;
    return nodes_[nodes_.size()-1];
}

int Roadmap::indexOfNode(RoadmapNode *node)
{
    for (unsigned int i=0; i<nodes_.size(); i++){
        if (nodes_[i] == node) return (int)i; 
    }
    return -1;
}

std::vector<RoadmapNode *> Roadmap::DFS(RoadmapNode* startNode, RoadmapNode* goalNode) 
{
    for (unsigned int i=0; i<nodes_.size(); i++) {
        nodes_[i]->visited(false);
    }

    std::vector<RoadmapNode*> path;
    startNode->visited(true);
    path.push_back(startNode);
    while (path.size() > 0) {
        RoadmapNode* node = path.back();

        if (node == goalNode) {
            break;
        } else {
            RoadmapNode* child = NULL;
            for (unsigned int i=0; i<node->nChildren(); i++) {
                if (!node->child(i)->visited()) {
                    child = node->child(i);
                    break;
                }
            }
            if (child == NULL) {
                path.pop_back();
            } else {
                child->visited(true);
                path.push_back(child);
            }
        }
    }

    return path;
}

void Roadmap::tryConnection(RoadmapNode *from, RoadmapNode *to, bool tryReverse)
{
    Mobility *mobility = planner_->getMobility();
    if (mobility->isReachable(from->position(), to->position())){
        addEdge(from, to);
        if (tryReverse && mobility->isReversible()) addEdge(to, from);
    }
    if (tryReverse && !mobility->isReversible()){
        if (mobility->isReachable(to->position(), from->position())){
            addEdge(to, from);
        }
    }
}


