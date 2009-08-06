#include "Mobility.h"
#include "PathPlanner.h"
#include "Algorithm.h"
#include "Roadmap.h"

using namespace PathEngine;

Algorithm::Algorithm(PathPlanner* planner) 
  : isRunning_(false), planner_(planner)
{
  properties_["weight-x"] = "1.0";
  properties_["weight-y"] = "1.0";
  properties_["weight-theta"] = "1.0";

  properties_["min-x"] = "-2";
  properties_["min-y"] = "-2";
  properties_["max-x"] = "2";
  properties_["max-y"] = "2";

  properties_["interpolation-distance"] = "0.1"; 
  roadmap_ = new Roadmap(planner_);
}

Algorithm::~Algorithm()
{
}

void Algorithm::setProperties(const std::map<std::string, std::string> &properties) 
{
  std::map<std::string, std::string>::const_iterator it;
  it = properties.begin();
  while (it != properties.end()) {
    properties_[it->first] = it->second;
    it++;
  }
}

void Algorithm::getProperties(std::vector<std::string> &names,
			      std::vector<std::string> &values) {
  names.clear();
  values.clear();

  std::map<std::string, std::string>::iterator it;
  it = properties_.begin();
  while (it != properties_.end()) {
    names.push_back((*it).first);
    values.push_back((*it).second);
    it++;
  }
}


bool Algorithm::tryDirectConnection()
{
  Mobility *mobility = planner_->getMobility();

  if (mobility->isReachable(start_, goal_)){
    path_.push_back(start_);
    path_.push_back(goal_);
    return true;
  }else{
    return false;
  }
}

bool Algorithm::preparePlanning()
{
  isRunning_ = true;

  double minX = atof(properties_["min-x"].c_str());
  double maxX = atof(properties_["max-x"].c_str());
  double minY = atof(properties_["min-y"].c_str());
  double maxY = atof(properties_["max-y"].c_str());

  Position::setBoundsX(minX, maxX);
  Position::setBoundsY(minY, maxY);

  Position::setWeightX(atof(properties_["weight-x"].c_str()));
  Position::setWeightY(atof(properties_["weight-y"].c_str()));
  Position::setWeightTh(atof(properties_["weight-theta"].c_str()));

  Mobility::interpolationDistance(atof(properties_["interpolation-distance"].c_str()));

#if 1
  std::map<std::string, std::string>::iterator it;
  it = properties_.begin();
  std::cout << "properties:" << std::endl;
  while (it != properties_.end()) {
      std::cout << "  " << it->first << " = " << it->second << std::endl;
      it++;
  }
  std::cout << std::endl;
#endif

  path_.clear();

  std::cout << "start:" << start_ << std::endl;
  std::cout << "goal:" << goal_ << std::endl;

  // validity checks of start&goal configurations
  if (!start_.isValid() || planner_->checkCollision(start_)) return false;
  if (!goal_.isValid() || planner_->checkCollision(goal_)) return false;
  
  return true;
}
