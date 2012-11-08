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

bool RoadmapNode::removeParent(RoadmapNodePtr node)
{
    std::vector<RoadmapNodePtr>::iterator it
        = find(parents_.begin(), parents_.end(), node);
    if (it != parents_.end()){
        parents_.erase(it);
        return true;
    }else{
        return false;
    }
}

bool RoadmapNode::removeChild(RoadmapNodePtr node)
{
    std::vector<RoadmapNodePtr>::iterator it
        = find(children_.begin(), children_.end(), node);
    if (it != children_.end()){
        children_.erase(it);
        return true;
    }else{
        return false;
    }
}

