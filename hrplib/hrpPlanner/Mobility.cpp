#include "PathPlanner.h"
#include "Mobility.h"

using namespace PathEngine;

double Mobility::interpolationDistance_ = 0.1;

bool Mobility::isReachable(const Position& from, const Position& to) const
{
  std::vector<Position> path = getPath(from, to);
#if 0
  std::cout << "isReachable(" << from << ", " << to << ")" << std::endl;
  for (unsigned int  i = 0; i<path.size(); i++){
    std::cout << i << ":" << path[i] << std::endl;
  }
  getchar();
#endif
  return !planner_->checkCollision(path);
}

std::vector<Position> Mobility::getPath(const Position &from, const Position &to) const
{
    std::vector<Position> path;
    path.push_back(from);
    
    unsigned int n = (unsigned int)(distance(from, to)/interpolationDistance())+1;
    Position pos;
    for (unsigned int i=1; i<n; i++){
        pos = interpolate(from, to, ((double)i)/n);
        path.push_back(pos);
    }
    path.push_back(to);
      
    return path;
}



