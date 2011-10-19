#include "Mobility.h"
#include "PathPlanner.h"
#include "RandomShortcutOptimizer.h"

using namespace PathEngine;

std::vector<Configuration> RandomShortcutOptimizer::optimize(const std::vector<Configuration> &path)
{
    if (path.size() < 3) return path;

    Mobility *mobility = planner_->getMobility(); 
    int nSegment = path.size()-1;
    int index1 = ((float)random())/RAND_MAX*nSegment; 
    int index2;
    do {
        index2 = ((float)random())/RAND_MAX*nSegment; 
    }while(index1 == index2);
    int tmp;
    if (index2 < index1) std::swap(index1, index2);

    double ratio1 = ((double)random())/RAND_MAX;
    double ratio2 = ((double)random())/RAND_MAX;
    Configuration cfg1 = mobility->interpolate(path[index1], path[index1+1],
                                               ratio1);
    Configuration cfg2 = mobility->interpolate(path[index2], path[index2+1],
                                               ratio2);
    if (mobility->isReachable(cfg1, cfg2)){
        std::vector<Configuration> optimized;
        for (int i=0; i<=index1; i++) optimized.push_back(path[i]);
        optimized.push_back(cfg1);
        optimized.push_back(cfg2);
        for (int i=index2+1; i<path.size(); i++) optimized.push_back(path[i]);
        return optimized;
    }else{
        return path;
    }
}
