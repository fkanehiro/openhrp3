
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


ColdetModelPair::ColdetModelPair(const ColdetModelPair& org)
{
    impl = new ColdetModelPairImpl();
    impl->model1 = org.impl->model1;
    impl->model2 = org.impl->model2;
}


ColdetModelPair::~ColdetModelPair()
{
    delete impl;
}


void ColdetModelPair::set(ColdetModelPtr model1, ColdetModelPtr model2)
{
    // inverse order because of historical background
    // this should be fixed.(note that the direction of normal is inversed when the order inversed 
    impl->model1 = model2;
    impl->model2 = model1;
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

    if(model1 && model2){

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
    }

    return result;
}


bool ColdetModelPair::checkCollision()
{
    return (impl->detectCollisions(false) != 0);
}
