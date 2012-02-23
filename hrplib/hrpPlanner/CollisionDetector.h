#ifndef __PATHENGINE_COLLISION_DETECTOR_H__
#define __PATHENGINE_COLLISION_DETECTOR_H__

namespace PathEngine
{
    class CollisionDetector {
    public:
        virtual void updatePositions()=0;
        virtual bool checkCollision()=0;
    };
};

#endif

