#include <iostream>
#include <cstdio>
#include "Roadmap.h"
#include "RoadmapNode.h"
#include "ConfigurationSpace.h"
#include "PRM.h"

using namespace PathEngine;

PRM::PRM(PathPlanner* path)
  : Algorithm(path)
{
  // デフォルト値セット
  properties_["max-dist"] = "1.0";
  properties_["max-points"] = "100";
}

PRM::~PRM() {
}

bool PRM::buildRoadmap()
{
  std::cerr << "new Roadmap is created" << std::endl;
  
  // 現在の点の数
  unsigned long numPoints = 0, numTotalPoints = 0;
  ConfigurationSpace *cspace = planner_->getConfigurationSpace();
  
  while (numPoints < maxPoints_) {
    if (!isRunning_) {
      return false;
    }
    
    // ランダムに与えた点
    Configuration pos = cspace->random();
    numTotalPoints++;
    
    // 干渉する位置でなければ追加
    if (!planner_->checkCollision(pos)) {
        RoadmapNodePtr node = RoadmapNodePtr(new RoadmapNode(pos));
      roadmap_->addNode(node);
      numPoints++;
    }
    printf("creating nodes, registered : %ld / tested : %ld\r", numPoints, numTotalPoints); 
  }
  printf("\n");
  
  // エッジを作成
  Mobility* mobility = planner_->getMobility();
  RoadmapNodePtr from, to;
  unsigned int n = roadmap_->nNodes();
  for (unsigned long i=0; i<n; i++) {
    if (!isRunning_) {
      return false;
    }
    from = roadmap_->node(i);
    for (unsigned long j=i+1; j<n; j++) {
      to = roadmap_->node(j);
      if (mobility->distance(from->position(), to->position()) < maxDist_) {
	roadmap_->tryConnection(from, to);
      }
    }
  }

  return true;
}

bool PRM::calcPath() 
{
    std::cout << "PRM::calcPath()" << std::endl;
  // Max Dists
  maxDist_  = atof(properties_["max-dist"].c_str());

  // Max Points
  maxPoints_ = atoi(properties_["max-points"].c_str());

  std::cerr << "maxDist:" << maxDist_ << std::endl;
  std::cerr << "maxPoints:" << maxPoints_ << std::endl;

  if (roadmap_->nNodes() == 0) buildRoadmap();

  // スタートとゴールを追加
  Mobility *mobility = planner_->getMobility();
  RoadmapNodePtr startNode = RoadmapNodePtr(new RoadmapNode(start_));
  RoadmapNodePtr goalNode = RoadmapNodePtr(new RoadmapNode(goal_));
  roadmap_->addNode(startNode);
  roadmap_->addNode(goalNode);

  RoadmapNodePtr node;
  for (unsigned long i=0; i<roadmap_->nNodes(); i++) {
    node = roadmap_->node(i);
    const Configuration& pos = node->position();
    if (mobility->distance(start_, pos) < maxDist_) {
      roadmap_->tryConnection(startNode, node);
    }
    if (mobility->distance(goal_, pos) < maxDist_) {
      roadmap_->tryConnection(goalNode, node);
    }
  }

  std::cout << "start node has " << startNode->nChildren()
	    << " childrens" << std::endl;
  std::cout << "goal node has " << goalNode->nParents()
	    << " parents" << std::endl;

  std::vector<RoadmapNodePtr> nodePath;
  nodePath = roadmap_->DFS(startNode, goalNode);
  for (unsigned int i=0; i<nodePath.size(); i++){
    path_.push_back(nodePath[i]->position());
  }

  std::cout << "path_.size() = " << path_.size() << std::endl; 

  return path_.size() != 0;
}

