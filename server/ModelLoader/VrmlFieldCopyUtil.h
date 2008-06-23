/*! @file
  @author S.NAKAOKA
*/

#ifndef VRML_FIELD_COPY_UTIL_H_INCLUDED
#define VRML_FIELD_COPY_UTIL_H_INCLUDED

#include "ModelParserConfig.h"

#include <string>
#include <ModelLoader.h>
#include "VrmlNodes.h"


namespace OpenHRP {

    MODELPARSER_EXPORT void copyVrmlField(TProtoFieldMap& fmap, const std::string& name, std::string& out_s);
    MODELPARSER_EXPORT void copyVrmlField(TProtoFieldMap& fmap, const std::string& name, DblSequence& out_v);
    MODELPARSER_EXPORT void copyVrmlField(TProtoFieldMap& fmap, const std::string& name, CORBA::Double& out_v);
    MODELPARSER_EXPORT void copyVrmlField(TProtoFieldMap& fmap, const std::string& name, CORBA::Long& out_v);

    //void copyVrmlField(TProtoFieldMap& fmap, const std::string& name, DblSequence3& out_v);
    //void copyVrmlField(TProtoFieldMap& fmap, const std::string& name, DblSequence9& out_m);

    MODELPARSER_EXPORT void copyVrmlField(TProtoFieldMap& fmap, const std::string& name, DblArray3& out_v);
    MODELPARSER_EXPORT void copyVrmlField(TProtoFieldMap& fmap, const std::string& name, DblArray9& out_m);

     MODELPARSER_EXPORT void copyVrmlRotationFieldToDblArray9
	 (TProtoFieldMap& fieldMap, const std::string name, DblArray9& out_R);

     MODELPARSER_EXPORT void copyVrmlRotationFieldToDblArray4
	 (TProtoFieldMap& fieldMap, const std::string name, DblArray4& out_R);
};


#endif
