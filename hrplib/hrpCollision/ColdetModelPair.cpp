
/**
   @author Shin'ichiro Nakaoka
*/

#include "ColdetModelPair.h"
#include "ColdetModelSharedDataSet.h"
#include "Opcode.h"

using namespace hrp;

namespace hrp {

    class ColdetModelPairImpl
    {
    public:
        collision_data* detectCollisions(bool detectAllContacts);
        
        ColdetModelPtr model1;
        ColdetModelPtr model2;
    };
}


ColdetModelPair::ColdetModelPair()
{
    impl = new ColdetModelPairImpl();
}


ColdetModelPair::ColdetModelPair(ColdetModelPtr model1, ColdetModelPtr model2)
{
    impl = new ColdetModelPairImpl();
    impl->model1 = model1;
    impl->model2 = model2;
}


ColdetModelPair::~ColdetModelPair()
{

}


collision_data* ColdetModelPair::detectCollisions()
{
    return impl->detectCollisions(true);
}


collision_data* ColdetModelPairImpl::detectCollisions(bool detectAllContacts)
{
    collision_data* result = 0;
    
    if(cdContact){
        delete[] cdContact;
        cdContact = 0;
        cdContactsCount = 0;
    }

    Opcode::BVTCache colCache;
    colCache.Model0 = &model1->dataSet->model;
    colCache.Model1 = &model2->dataSet->model;

    Opcode::AABBTreeCollider collider;

    if(!detectAllContacts){
        collider.SetFirstContact(true);
    }
    
    bool isOK = collider.Collide(colCache, model1->transform, model2->transform);

    cdBoxTestsCount = collider.GetNbBVBVTests();
    cdTriTestsCount = collider.GetNbPrimPrimTests();
    
    if(isOK){
        result = cdContact;
    }

    return result;
}


bool ColdetModelPair::checkCollision()
{
    return (impl->detectCollisions(false) != 0);
}
