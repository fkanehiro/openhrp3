#include "RoadmapNode.h"

using namespace PathEngine;

RoadmapNodePtr RoadmapNode::parent(unsigned int index)
{
  if (index >= parents_.size()) return RoadmapNodePtr();
  return parents_[index];
}

RoadmapNodePtr RoadmapNode::child(unsigned int index)
{
  if (index >= children_.size()) return RoadmapNodePtr();
  return children_[index];
}


