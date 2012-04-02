#include <iostream>
#include <hrpModel/Link.h>
#include "ProjectUtil.h"

void initWorld(Project& prj, BodyFactory &factory, 
               hrp::World<hrp::ConstraintForceSolver>& world)
{
    world.setTimeStep(prj.timeStep());
    if(prj.isEuler()){
        world.setEulerMethod();
    } else {
        world.setRungeKuttaMethod();
    }

    // add bodies
    for (std::map<std::string, ModelItem>::iterator it=prj.models().begin();
         it != prj.models().end(); it++){
        hrp::BodyPtr body = factory(it->first, it->second.url);
        if (body){
            body->setName(it->first);
            for (std::map<std::string, JointItem>::iterator it2=it->second.joint.begin();
                 it2 != it->second.joint.end(); it2++){
                hrp::Link *link = body->link(it2->first);
                if (link) link->isHighGainMode = it2->second.isHighGain;
            }
            world.addBody(body);
        }
    }

    for (unsigned int i=0; i<prj.collisionPairs().size(); i++){
        const CollisionPairItem &cpi = prj.collisionPairs()[i];
        int bodyIndex1 = world.bodyIndex(cpi.objectName1);
        int bodyIndex2 = world.bodyIndex(cpi.objectName2);

        if(bodyIndex1 >= 0 && bodyIndex2 >= 0){
            hrp::BodyPtr bodyPtr1 = world.body(bodyIndex1);
            hrp::BodyPtr bodyPtr2 = world.body(bodyIndex2);

            std::vector<hrp::Link*> links1;
            if(cpi.jointName1.empty()){
                const hrp::LinkTraverse& traverse = bodyPtr1->linkTraverse();
                links1.resize(traverse.numLinks());
                std::copy(traverse.begin(), traverse.end(), links1.begin());
            } else {
                links1.push_back(bodyPtr1->link(cpi.jointName1));
            }

            std::vector<hrp::Link*> links2;
            if(cpi.jointName2.empty()){
                const hrp::LinkTraverse& traverse = bodyPtr2->linkTraverse();
                links2.resize(traverse.numLinks());
                std::copy(traverse.begin(), traverse.end(), links2.begin());
            } else {
                links2.push_back(bodyPtr2->link(cpi.jointName2));
            }

            for(size_t j=0; j < links1.size(); ++j){
                for(size_t k=0; k < links2.size(); ++k){
                    hrp::Link* link1 = links1[j];
                    hrp::Link* link2 = links2[k];

                    if(link1 && link2 && link1 != link2){
                        world.constraintForceSolver.addCollisionCheckLinkPair
                            (bodyIndex1, link1, bodyIndex2, link2, 
                             cpi.staticFriction, cpi.slidingFriction, 0.01, 0.0);
                    }
                }
            }
        }
    }

    world.enableSensors(true);

    int nBodies = world.numBodies();
    for(int i=0; i < nBodies; ++i){
        hrp::BodyPtr bodyPtr = world.body(i);
        bodyPtr->initializeConfiguration();
    }
        
    for (std::map<std::string, ModelItem>::iterator it=prj.models().begin();
         it != prj.models().end(); it++){
        hrp::BodyPtr body = world.body(it->first);
        for (std::map<std::string, JointItem>::iterator it2=it->second.joint.begin();
             it2 != it->second.joint.end(); it2++){
            hrp::Link *link = body->link(it2->first);
            if (!link) continue;
            if (link->isRoot()){
                link->p = it2->second.translation;
                link->setAttitude(it2->second.rotation);
            }else{
                link->q = it2->second.angle;
            }
        }
        body->calcForwardKinematics();
    }
    world.initialize();
    world.constraintForceSolver.useBuiltinCollisionDetector(true);
}
