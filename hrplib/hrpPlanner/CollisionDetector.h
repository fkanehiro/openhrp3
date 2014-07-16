#ifndef __PATHENGINE_COLLISION_DETECTOR_H__
#define __PATHENGINE_COLLISION_DETECTOR_H__
#include <string>

namespace PathEngine
{
    class CollisionDetector {
    public:
        virtual void updatePositions()=0;
        virtual bool checkCollision()=0;
	virtual const std::pair<std::string, std::string>& collidingPair()=0; 
    };
};

#endif

