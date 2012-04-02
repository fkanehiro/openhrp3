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
    for(int j=0; j < body->numLinks(); ++j) {
        LinkPosition &linkPosition = characterPosition.linkPositions[j];
        Link* link = body->link(j);

        setVector3(link->p, linkPosition.p);
        setMatrix33ToRowMajorArray(link->attitude(), linkPosition.R);
    }
}

void getWorldState(WorldState& state, WorldBase& world)
{
    state.time = world.currentTime();
    for (int i=0; i<world.numBodies(); i++){
        updateCharacterPosition(state.characterPositions[i], world.body(i));
    }
}

void initWorldState(WorldState& state,  WorldBase& world)
{
    state.characterPositions.length(world.numBodies());
    for (int i=0; i<world.numBodies(); i++){
        setupCharacterPosition(state.characterPositions[i], world.body(i));
    }
}

