#include <hrpModel/Link.h>
#include "OnlineViewerUtil.h"

using namespace hrp;
using namespace OpenHRP;

void  setupCharacterPosition(CharacterPosition &characterPosition, BodyPtr body) 
{
    characterPosition.characterName = CORBA::string_dup(body->name().c_str());
    int numLinks = body->numLinks();
    characterPosition.linkPositions.length(numLinks);
}

void  updateCharacterPosition(CharacterPosition &characterPosition, BodyPtr body) 
{
    for(unsigned int j=0; j < body->numLinks(); ++j) {
        LinkPosition &linkPosition = characterPosition.linkPositions[j];
        Link* link = body->link(j);

        setVector3(link->p, linkPosition.p);
        setMatrix33ToRowMajorArray(link->attitude(), linkPosition.R);
    }
}

void getWorldState(WorldState& state, WorldBase& world)
{
    state.time = world.currentTime();
    for (unsigned int i=0; i<world.numBodies(); i++){
        updateCharacterPosition(state.characterPositions[i], world.body(i));
    }
}

void initWorldState(WorldState& state,  WorldBase& world)
{
    state.characterPositions.length(world.numBodies());
    for (unsigned int i=0; i<world.numBodies(); i++){
        setupCharacterPosition(state.characterPositions[i], world.body(i));
    }
}

void initWorldState(WorldState& state,  WorldBase& world,
                    std::vector<ColdetLinkPairPtr>& pairs)
{
    initWorldState(state, world);

    OpenHRP::CollisionSequence& collisions = state.collisions;

    collisions.length(pairs.size());
    for(size_t colIndex=0; colIndex < pairs.size(); ++colIndex){
        hrp::ColdetLinkPairPtr linkPair = pairs[colIndex];
        hrp::Link *link0 = linkPair->link(0);
        hrp::Link *link1 = linkPair->link(1);
        OpenHRP::LinkPair& pair = collisions[colIndex].pair;
        pair.charName1 = CORBA::string_dup(link0->body->name().c_str());
        pair.charName2 = CORBA::string_dup(link1->body->name().c_str());
        pair.linkName1 = CORBA::string_dup(link0->name.c_str());
        pair.linkName2 = CORBA::string_dup(link1->name.c_str());
    }
}
