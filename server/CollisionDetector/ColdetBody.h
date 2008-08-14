/**
   @author Shin'ichiro Nakaoka
*/

#ifndef OPENHRP_COLLISION_DETECTOR_COLDET_BODY_H_INCLUDED
#define OPENHRP_COLLISION_DETECTOR_COLDET_BODY_H_INCLUDED

#include <map>
#include <vector>
#include <string>
#include <hrpCorba/ModelLoader.h>
#include <hrpCollision/ColdetModel.h>

using namespace std;
using namespace hrp;
using namespace OpenHRP;


class ColdetBody
{
public:
    ColdetBody(BodyInfo_ptr bodyInfo);

    /**
       do shallow copy (sharing the same ColdetModel instances)
    */
    ColdetBody(const ColdetBody& org);

    ColdetModelPtr linkColdetModel(int linkIndex) {
        return linkColdetModels[linkIndex];
    }
    
    ColdetModelPtr linkColdetModel(const string& linkName){
        return linkNameToColdetModelMap[linkName];
    }

  private:
    void addLinkVerticesAndTriangles
        (ColdetModelPtr& coldetModel, LinkInfo& linkInfo, ShapeInfoSequence_var& shapes);
    
    vector<ColdetModelPtr> linkColdetModels;
    map<string, ColdetModelPtr> linkNameToColdetModelMap;
};
    


#endif
