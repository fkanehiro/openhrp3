#include "PathPlanner.h"
#include "Mobility.h"

using namespace PathEngine;

double Mobility::interpolationDistance_ = 0.1;

bool Mobility::isReachable(const Configuration& from, const Configuration& to) const
{
  std::vector<Configuration> path = getPath(from, to);
#if 0
  std::cout << "isReachable(" << from << ", " << to << ")" << std::endl;
  for (unsigned int  i = 0; i<path.size(); i++){
    std::cout << i << ":" << path[i] << std::endl;
  }
#endif
  return !planner_->checkCollision(path);
}

std::vector<Configuration> Mobility::getPath(const Configuration &from, const Configuration &to) const
{
    std::vector<Configuration> path;
    path.push_back(from);
    
    unsigned int n = (unsigned int)(distance(from, to)/interpolationDistance())+1;
    Configuration pos;
    for (unsigned int i=1; i<n; i++){
        pos = interpolate(from, to, ((double)i)/n);
        path.push_back(pos);
    }
    path.push_back(to);
      
    return path;
}



