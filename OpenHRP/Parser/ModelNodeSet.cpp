/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */

/*! @file
  @author Shin'ichiro Nakaoka
*/

#include "ModelNodeSet.h"
#include "VrmlParser.h"
#include "EasyScanner.h"

#include <iostream>
#include <algorithm>


using namespace OpenHRP;
using namespace std;
using namespace boost;

namespace {

    typedef void (ModelNodeSet::*ProtoCheckFunc)(void);
    
    struct ProtoInfo
    {
	ProtoInfo() { }
	ProtoInfo(int id, ProtoCheckFunc func) : id(id), protoCheckFunc(func) { }
	int id;
	ProtoCheckFunc protoCheckFunc;
    };
    
    typedef map<string,ProtoInfo> ProtoNameToInfoMap;
    ProtoNameToInfoMap protoNameToInfoMap;

}




ModelNodeSet::ModelNodeSet()
{
    numJointNodes_ = 0;
    humanoidNode_ = 0;
    rootJointNodeSet_.reset();
    messageIndent_ = 0;
    flgMessageOutput_ = true;
}


void ModelNodeSet::putMessage(const std::string& message)
{
    if(flgMessageOutput_) {
        string space(messageIndent_, ' ');
        signalOnStatusMessage(space + message + "\n");
    }
}

    
    
bool ModelNodeSet::loadModelFile(const std::string& filename)
{
    if(protoNameToInfoMap.empty()){
        protoNameToInfoMap["Humanoid"]           = ProtoInfo( PROTO_HUMANOID,          &ModelNodeSet::checkHumanoidProto );
        protoNameToInfoMap["Joint"]              = ProtoInfo( PROTO_JOINT,             &ModelNodeSet::checkJointProto );
        protoNameToInfoMap["Segment"]            = ProtoInfo( PROTO_SEGMENT,           &ModelNodeSet::checkSegmentProto );
        protoNameToInfoMap["ForceSensor"]        = ProtoInfo( PROTO_SENSOR,            &ModelNodeSet::checkSensorProtoCommon );
        protoNameToInfoMap["Gyro"]               = ProtoInfo( PROTO_SENSOR,            &ModelNodeSet::checkSensorProtoCommon );
        protoNameToInfoMap["AccelerationSensor"] = ProtoInfo( PROTO_SENSOR,            &ModelNodeSet::checkSensorProtoCommon );
        protoNameToInfoMap["VisionSensor"]       = ProtoInfo( PROTO_SENSOR,            &ModelNodeSet::checkSensorProtoCommon );
        protoNameToInfoMap["HardwareComponent"]  = ProtoInfo( PROTO_HARDWARECOMPONENT, &ModelNodeSet::checkHardwareComponentProto );
    }

    numJointNodes_ = 0;
    humanoidNode_ = 0;
    rootJointNodeSet_.reset();
    messageIndent_ = 0;

    try {
	VRMLParser parser;
	parser.load(filename);
	extractHumanoidNode(parser);

    } catch(EasyScanner::Exception& ex){
	throw ModelNodeSet::Exception(ex.getFullMessage());
    }

    return (humanoidNode_ && rootJointNodeSet_);
}


void ModelNodeSet::extractHumanoidNode(VRMLParser& parser)
{
    while(VrmlNodePtr node = parser.readNode()){
		
        if(node->isCategoryOf(PROTO_DEF_NODE)){
			
            protoToCheck = static_pointer_cast<VrmlProto>(node);
			
            ProtoNameToInfoMap::iterator p = protoNameToInfoMap.find(protoToCheck->protoName);
            if(p != protoNameToInfoMap.end()){
                ProtoInfo& info = p->second;
                (this->*info.protoCheckFunc)();
            }
			
        } else if(node->isCategoryOf(PROTO_INSTANCE_NODE)){
			
            VrmlProtoInstancePtr instance = static_pointer_cast<VrmlProtoInstance>(node);
            if(instance->proto->protoName == "Humanoid") {
                humanoidNode_ = instance;
            }
        }
    }
	
    if(humanoidNode_){

        putMessage("Humanoid node");
		
        extractJointNodes();
		
    } else {
        throw string("Humanoid node is not found");
    }
}


void ModelNodeSet::throwExceptionOfIllegalField(const std::string& name, VrmlFieldTypeId typeId)
{
    string message = "Proto \"";
    message += protoToCheck->protoName + "\" must have field \"" + name + "\" of " +
        VrmlNode::getLabelOfFieldType(typeId) + " type";
    throw Exception(message);
}


void ModelNodeSet::requireField(const std::string& name, VrmlFieldTypeId typeId)
{
    VrmlVariantField* field = protoToCheck->getField(name);
    if(!field || field->typeId() != typeId){
        throwExceptionOfIllegalField(name, typeId);
    }
}


void ModelNodeSet::checkFieldType(const std::string& name, VrmlFieldTypeId typeId)
{
    VrmlVariantField* field = protoToCheck->getField(name);
    if(field && field->typeId() != typeId){
        throwExceptionOfIllegalField(name, typeId);
    }
}


VrmlVariantField* ModelNodeSet::addField(const std::string& name, VrmlFieldTypeId typeId)
{
    VrmlVariantField* field = protoToCheck->getField(name);
    if(!field){
        field = protoToCheck->addField(name, typeId);
    } else if(field->typeId() != typeId){
        throwExceptionOfIllegalField(name, typeId);
    }
    return field;
}


void ModelNodeSet::addFloatField(const std::string& name, double defaultValue)
{
    VrmlVariantField* field = protoToCheck->getField(name);
    if(!field){
        field = protoToCheck->addField(name, SFFLOAT);
        field->sfFloat() = defaultValue;
    } else if(field->typeId() != SFFLOAT){
        throwExceptionOfIllegalField(name, SFFLOAT);
    }
}


void ModelNodeSet::checkHumanoidProto()
{
    // necessary fields
    requireField("center", SFVEC3F);
    requireField("humanoidBody", MFNODE);
    requireField("rotation", SFROTATION);
    requireField("translation", SFVEC3F);

    // optional fields
    addField("info", MFSTRING);
    addField("name", SFSTRING);
    addField("version", SFSTRING);
    addField("scaleOrientation", SFROTATION);

    VrmlVariantField* field;

    if( (field = addField("scale", SFVEC3F)) != 0){
        SFVec3f& scale = field->sfVec3f();
        std::fill(scale.begin(), scale.end(), 1.0);
    }
}


void ModelNodeSet::checkJointProto()
{
    // necessary fields
    requireField("center", SFVEC3F);
    requireField("children", MFNODE);
    requireField("rotation", SFROTATION);
    requireField("translation", SFVEC3F);
    requireField("jointType", SFSTRING);
    requireField("jointId", SFINT32);

    VrmlVariantField* field;

    field = protoToCheck->getField("jointAxis");
    if(!field){
        throw Exception("Prototype of Humanoid does not have \"jointAxis\" field");
    }
    if(field->typeId() != SFSTRING && field->typeId() != SFVEC3F){
        throw Exception("The type of \"jointAxis\" field in \"Humanoid\" prototype must be SFString or SFVec3f");
    }

    // optional fields
    addField("llimit", MFFLOAT);
    addField("ulimit", MFFLOAT);
    addField("lvlimit", MFFLOAT);
    addField("uvlimit", MFFLOAT);
    addField("limitOrientation", SFROTATION);
    addField("name", SFSTRING);

    addFloatField("gearRatio", 1.0);
    addFloatField("rotorInertia", 0.0);
    addFloatField("rotorResistor", 0.0);
    addFloatField("torqueConst", 1.0);
    addFloatField("encoderPulse", 1.0);

    //#####
    addFloatField("jointValue", 0.0);

    if( (field = addField("scale", SFVEC3F)) != 0){
        SFVec3f& scale = field->sfVec3f();
        std::fill(scale.begin(), scale.end(), 1.0);
    }

    // ##### [Changed] ######
    //checkFieldType("equivalentInertia", SFFLOAT);
}


void ModelNodeSet::checkSegmentProto()
{
    requireField("centerOfMass", SFVEC3F);
    requireField("mass", SFFLOAT);
    requireField("momentsOfInertia", MFFLOAT);
    addField("name", SFSTRING);
}


void ModelNodeSet::checkSensorProtoCommon()
{
    requireField("sensorId", SFINT32);
    requireField("translation", SFVEC3F);
    requireField("rotation", SFROTATION);
}


void ModelNodeSet::checkHardwareComponentProto()
{
    requireField("id", SFINT32);
    requireField("translation", SFVEC3F);
    requireField("rotation", SFROTATION);
    requireField("url", SFSTRING);
}


void ModelNodeSet::extractJointNodes()
{
    MFNode& nodes = humanoidNode_->fields["humanoidBody"].mfNode();

    if(nodes.empty()){
	throw string("No Joint node in Humanoid node");

    } else if(nodes.size() == 1){

        if(nodes[0]->isCategoryOf(PROTO_INSTANCE_NODE)){
	    VrmlProtoInstancePtr jointNode = dynamic_pointer_cast<VrmlProtoInstance>(nodes[0]);
	    if(jointNode && jointNode->proto->protoName == "Joint"){
                rootJointNodeSet_ = addJointNodeSet(jointNode);
	    }
	}
    }

    if(!rootJointNodeSet_){
	throw string("Invalid entiy of humanoidBody field");
    }
}


JointNodeSetPtr ModelNodeSet::addJointNodeSet(VrmlProtoInstancePtr jointNode)
{
    numJointNodes_++;

    putMessage(string("Joint node") + jointNode->defName);

    JointNodeSetPtr jointNodeSet(new JointNodeSet());

    jointNodeSet->jointNode = jointNode;

    MFNode& childNodes = jointNode->fields["children"].mfNode();

    ProtoIdSet acceptableProtoIds;
    acceptableProtoIds.set(PROTO_JOINT);
    acceptableProtoIds.set(PROTO_SEGMENT);
    acceptableProtoIds.set(PROTO_SENSOR);
    acceptableProtoIds.set(PROTO_HARDWARECOMPONENT);
    
    extractChildNodes(jointNodeSet, childNodes, acceptableProtoIds);

    return jointNodeSet;
}


void ModelNodeSet::extractChildNodes
(JointNodeSetPtr jointNodeSet, MFNode& childNodes, const ProtoIdSet acceptableProtoIds)
{
    for(size_t i = 0; i < childNodes.size(); i++){
        VrmlNode* childNode = childNodes[i].get();

        if(childNode->isCategoryOf(GROUPING_NODE)){
            VrmlGroup* groupNode = static_cast<VrmlGroup*>(childNode);
            extractChildNodes(jointNodeSet, groupNode->children, acceptableProtoIds);

        } else if(childNode->isCategoryOf(PROTO_INSTANCE_NODE)){
            VrmlProtoInstance* protoInstance = static_cast<VrmlProtoInstance*>(childNode);

            int id = PROTO_UNDEFINED;
            const string& protoName = protoInstance->proto->protoName;
            ProtoNameToInfoMap::iterator p = protoNameToInfoMap.find(protoName);
			
            if(p != protoNameToInfoMap.end()){
                id = p->second.id;
            }

            if(!acceptableProtoIds.test(id)){
                throw Exception(protoName + " node is not in a correct place.");
            } else {

                messageIndent_ += 2;

                switch(id){
                
                case PROTO_JOINT:
                    jointNodeSet->childJointNodeSets.push_back(addJointNodeSet(protoInstance));
                    break;

                case PROTO_SENSOR:
                    jointNodeSet->sensorNodes.push_back(protoInstance);
                    putMessage(protoName + protoInstance->defName);
                    break;

                case PROTO_SEGMENT:
                    {
                        if(jointNodeSet->segmentNode){
                            const string& jointName = jointNodeSet->jointNode->defName;
                            throw Exception((string("Joint node ") + jointName +
                                             "includes multipe segment nodes, which is not supported."));
                        }
                        jointNodeSet->segmentNode = protoInstance;
                        putMessage(string("Segment node ") + protoInstance->defName);

                        MFNode& childNodes = protoInstance->fields["children"].mfNode();
                        ProtoIdSet acceptableChildProtoIds;
                        acceptableChildProtoIds.set(PROTO_SENSOR);
                        extractChildNodes(jointNodeSet, childNodes, acceptableChildProtoIds);
                    }
                    break;

                default:
                    break;
                }

                messageIndent_ -= 2;
            }
        }
    }
}
