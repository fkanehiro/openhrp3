#include "Mobility.h"
#include "PathPlanner.h"
#include "ShortcutOptimizer.h"

using namespace PathEngine;

std::vector<Position> ShortcutOptimizer::optimize(const std::vector<Position> &path)
{
  std::vector<Position> optimized = path;
  if (path.size() < 3) return optimized;

  Mobility *mobility = planner_->getMobility(); 
  unsigned int index = 1;
  while(index != optimized.size() -1 ){
    if (mobility->isReachable(optimized[index-1], optimized[index+1])){
      optimized.erase(optimized.begin()+index); 
    }else{
      index++;
    }
  }
  return optimized;
}
