#include "RoadmapNode.h"

using namespace PathEngine;

RoadmapNode *RoadmapNode::parent(unsigned int index)
{
  if (index >= parents_.size()) return NULL;
  return parents_[index];
}

RoadmapNode *RoadmapNode::child(unsigned int index)
{
  if (index >= children_.size()) return NULL;
  return children_[index];
}


