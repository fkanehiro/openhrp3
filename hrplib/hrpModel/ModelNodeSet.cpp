/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

/*! @file
  @author Shin'ichiro Nakaoka
*/

#include "ModelNodeSet.h"

#include <bitset>
#include <iostream>
#include <algorithm>
#include <hrpUtil/EasyScanner.h>
#include <hrpUtil/VrmlParser.h>


using namespace hrp;
using namespace std;
using namespace boost;


namespace {

    typedef void (ModelNodeSetImpl::*ProtoCheckFunc)(void);
    
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


namespace hrp {

    class ModelNodeSetImpl
    {
    public:
        ModelNodeSetImpl(ModelNodeSet* self);

        bool loadModelFile(const std::string& filename);

        ModelNodeSet* self;
        
        int numJointNodes;
        VrmlProtoInstancePtr humanoidNode;
        JointNodeSetPtr rootJointNodeSet;
        int messageIndent;

        VrmlProtoPtr protoToCheck;

        enum {
            PROTO_UNDEFINED = 0,
            PROTO_HUMANOID,
            PROTO_JOINT,
            PROTO_SEGMENT,
            PROTO_SENSOR,
            PROTO_HARDWARECOMPONENT,
            NUM_PROTOS
        };

        typedef std::bitset<NUM_PROTOS> ProtoIdSet;
    
        void extractHumanoidNode(VrmlParser& parser);

        void throwExceptionOfIllegalField(const std::string& name, VrmlFieldTypeId typeId);
        void requireField(const std::string& name, VrmlFieldTypeId type);
        void checkFieldType(const std::string& name, VrmlFieldTypeId type);
        VrmlVariantField* addField(const std::string& name, VrmlFieldTypeId type);
        void addFloatField(const std::string& name, double defaultValue);
		
        void checkHumanoidProto();
        void checkJointProto();
        void checkSegmentProto();
        void checkSensorProtoCommon();
        void checkHardwareComponentProto();
        void extractJointNodes();
        JointNodeSetPtr addJointNodeSet(VrmlProtoInstancePtr jointNode);
        void extractChildNodes
            (JointNodeSetPtr jointNodeSet, MFNode& childNodes, const ProtoIdSet acceptableProtoIds, const Matrix44& T);

        void putMessage(const std::string& message);
    };
}


ModelNodeSet::ModelNodeSet()
{
    impl = new ModelNodeSetImpl(this);
}


ModelNodeSetImpl::ModelNodeSetImpl(ModelNodeSet* self) : self(self)
{
    numJointNodes = 0;
    messageIndent = 0;

    if(protoNameToInfoMap.empty()){

        protoNameToInfoMap["Humanoid"]
            = ProtoInfo(PROTO_HUMANOID, &ModelNodeSetImpl::checkHumanoidProto);

        protoNameToInfoMap["Joint"]
            = ProtoInfo(PROTO_JOINT, &ModelNodeSetImpl::checkJointProto);
        
        protoNameToInfoMap["Segment"]
            = ProtoInfo(PROTO_SEGMENT, &ModelNodeSetImpl::checkSegmentProto);
        
        protoNameToInfoMap["ForceSensor"]
            = ProtoInfo(PROTO_SENSOR, &ModelNodeSetImpl::checkSensorProtoCommon);
        
        protoNameToInfoMap["Gyro"]
            = ProtoInfo(PROTO_SENSOR, &ModelNodeSetImpl::checkSensorProtoCommon);
        
        protoNameToInfoMap["AccelerationSensor"]
            = ProtoInfo(PROTO_SENSOR, &ModelNodeSetImpl::checkSensorProtoCommon);
        
        protoNameToInfoMap["VisionSensor"]
            = ProtoInfo(PROTO_SENSOR, &ModelNodeSetImpl::checkSensorProtoCommon);
        
        protoNameToInfoMap["RangeSensor"]
            = ProtoInfo(PROTO_SENSOR, &ModelNodeSetImpl::checkSensorProtoCommon);
        
        protoNameToInfoMap["HardwareComponent"]
            = ProtoInfo(PROTO_HARDWARECOMPONENT, &ModelNodeSetImpl::checkHardwareComponentProto);
    }
}


ModelNodeSet::~ModelNodeSet()
{
    delete impl;
}


int ModelNodeSet::numJointNodes()
{
    return impl->numJointNodes;
}


VrmlProtoInstancePtr ModelNodeSet::humanoidNode()
{
    return impl->humanoidNode;
}


JointNodeSetPtr ModelNodeSet::rootJointNodeSet()
{
    return impl->rootJointNodeSet;
}


bool ModelNodeSet::loadModelFile(const std::string& filename)
{
    return impl->loadModelFile(filename);
}


bool ModelNodeSetImpl::loadModelFile(const std::string& filename)
{
    numJointNodes = 0;
    humanoidNode = 0;
    rootJointNodeSet.reset();
    messageIndent = 0;

    try {
	VrmlParser parser;
	parser.load(filename);
	extractHumanoidNode(parser);

    } catch(EasyScanner::Exception& ex){
	    throw ModelNodeSet::Exception(ex.getFullMessage());
    }

    return (humanoidNode && rootJointNodeSet);
}


void ModelNodeSetImpl::extractHumanoidNode(VrmlParser& parser)
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
                humanoidNode = instance;
            }
        }
    }
	
    if(humanoidNode){
        putMessage("Humanoid node");
        extractJointNodes();
    } else {
        throw ModelNodeSet::Exception("Humanoid node is not found");
    }
}


void ModelNodeSetImpl::throwExceptionOfIllegalField(const std::string& name, VrmlFieldTypeId typeId)
{
    string message = "Proto \"";
    message += protoToCheck->protoName + "\" must have the \"" + name + "\" field of " +
        VrmlNode::getLabelOfFieldType(typeId) + " type";
    throw ModelNodeSet::Exception(message);
}


void ModelNodeSetImpl::requireField(const std::string& name, VrmlFieldTypeId typeId)
{
    VrmlVariantField* field = protoToCheck->getField(name);
    if(!field || field->typeId() != typeId){
        throwExceptionOfIllegalField(name, typeId);
    }
}


void ModelNodeSetImpl::checkFieldType(const std::string& name, VrmlFieldTypeId typeId)
{
    VrmlVariantField* field = protoToCheck->getField(name);
    if(field && field->typeId() != typeId){
        throwExceptionOfIllegalField(name, typeId);
    }
}


VrmlVariantField* ModelNodeSetImpl::addField(const std::string& name, VrmlFieldTypeId typeId)
{
    VrmlVariantField* field = protoToCheck->getField(name);
    if(!field){
        field = protoToCheck->addField(name, typeId);
    } else if(field->typeId() != typeId){
        throwExceptionOfIllegalField(name, typeId);
    }
    return field;
}


void ModelNodeSetImpl::addFloatField(const std::string& name, double defaultValue)
{
    VrmlVariantField* field = protoToCheck->getField(name);
    if(!field){
        field = protoToCheck->addField(name, SFFLOAT);
        field->sfFloat() = defaultValue;
    } else if(field->typeId() != SFFLOAT){
        throwExceptionOfIllegalField(name, SFFLOAT);
    }
}


void ModelNodeSetImpl::checkHumanoidProto()
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


void ModelNodeSetImpl::checkJointProto()
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
        throw ModelNodeSet::Exception
            ("Prototype of Humanoid must have the \"jointAxis\" field");
    }
    if(field->typeId() != SFSTRING && field->typeId() != SFVEC3F){
        throw ModelNodeSet::Exception
            ("The type of \"jointAxis\" field in \"Humanoid\" prototype must be SFString or SFVec3f");
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

    addFloatField("jointValue", 0.0);

    if( (field = addField("scale", SFVEC3F)) != 0){
        SFVec3f& scale = field->sfVec3f();
        std::fill(scale.begin(), scale.end(), 1.0);
    }

    if(protoToCheck->getField("equivalentInertia")){
        putMessage("The \"equivalentInertia\" field of the Joint node is obsolete.");
    }
}


void ModelNodeSetImpl::checkSegmentProto()
{
    requireField("centerOfMass", SFVEC3F);
    requireField("mass", SFFLOAT);
    requireField("momentsOfInertia", MFFLOAT);
    addField("name", SFSTRING);
}


void ModelNodeSetImpl::checkSensorProtoCommon()
{
    requireField("sensorId", SFINT32);
    requireField("translation", SFVEC3F);
    requireField("rotation", SFROTATION);
}


void ModelNodeSetImpl::checkHardwareComponentProto()
{
    requireField("id", SFINT32);
    requireField("translation", SFVEC3F);
    requireField("rotation", SFROTATION);
    requireField("url", SFSTRING);
}


void ModelNodeSetImpl::extractJointNodes()
{
    MFNode& nodes = humanoidNode->fields["humanoidBody"].mfNode();

    if(nodes.size() > 1){
        throw ModelNodeSet::Exception
            ("The Humanoid node must have a unique Joint node in its \"humanoidBody\" field.");

    } else if(nodes.size() == 1){
        if(nodes[0]->isCategoryOf(PROTO_INSTANCE_NODE)){
	    VrmlProtoInstancePtr jointNode = dynamic_pointer_cast<VrmlProtoInstance>(nodes[0]);
	    if(jointNode && jointNode->proto->protoName == "Joint"){
                rootJointNodeSet = addJointNodeSet(jointNode);
	    }
	}
    }

    if(!rootJointNodeSet){
        throw ModelNodeSet::Exception
            ("The Humanoid node does not have a Joint node in its \"humanoidBody\" field.");
    }
}


JointNodeSetPtr ModelNodeSetImpl::addJointNodeSet(VrmlProtoInstancePtr jointNode)
{
    numJointNodes++;

    putMessage(string("Joint node") + jointNode->defName);

    JointNodeSetPtr jointNodeSet(new JointNodeSet());

    jointNodeSet->jointNode = jointNode;

    MFNode& childNodes = jointNode->fields["children"].mfNode();

    ProtoIdSet acceptableProtoIds;
    acceptableProtoIds.set(PROTO_JOINT);
    acceptableProtoIds.set(PROTO_SEGMENT);
    acceptableProtoIds.set(PROTO_SENSOR);
    acceptableProtoIds.set(PROTO_HARDWARECOMPONENT);
    
    Matrix44 T(Matrix44::Identity());
    extractChildNodes(jointNodeSet, childNodes, acceptableProtoIds, T);

    return jointNodeSet;
}

void ModelNodeSetImpl::extractChildNodes
(JointNodeSetPtr jointNodeSet, MFNode& childNodes, const ProtoIdSet acceptableProtoIds, const Matrix44& T)
{
    for(size_t i = 0; i < childNodes.size(); i++){
        VrmlNode* childNode = childNodes[i].get();
        const Matrix44* pT;
        if(childNode->isCategoryOf(GROUPING_NODE)){
            VrmlGroup* groupNode = static_cast<VrmlGroup*>(childNode);
            VrmlTransform* transformNode = dynamic_cast<VrmlTransform*>(groupNode);
            Matrix44 T2;
            if( transformNode ){
                Matrix44 Tlocal;
                calcTransformMatrix(transformNode, Tlocal);
                T2 = T * Tlocal;
                pT = &T2;
            } else {
                pT = &T;
            }
            extractChildNodes(jointNodeSet, groupNode->getChildren(), acceptableProtoIds, *pT);

        } else if(childNode->isCategoryOf(LIGHT_NODE)){
            jointNodeSet->lightNodes.push_back(std::make_pair(T,childNode));
        } else if(childNode->isCategoryOf(PROTO_INSTANCE_NODE)){

            VrmlProtoInstance* protoInstance = static_cast<VrmlProtoInstance*>(childNode);
            int id = PROTO_UNDEFINED;
            bool doTraverseChildren = false;
            ProtoIdSet acceptableChildProtoIds(acceptableProtoIds);

            const string& protoName = protoInstance->proto->protoName;
            ProtoNameToInfoMap::iterator p = protoNameToInfoMap.find(protoName);

            if(p == protoNameToInfoMap.end()){
                doTraverseChildren = true;
            } else {
                id = p->second.id;
                if(!acceptableProtoIds.test(id)){
                    throw ModelNodeSet::Exception(protoName + " node is not in a correct place.");
                }
            }

            messageIndent += 2;

            switch(id){
                
            case PROTO_JOINT:
                if(T != Matrix44::Identity())
                    throw ModelNodeSet::Exception(protoName + " node is not in a correct place.");
                jointNodeSet->childJointNodeSets.push_back(addJointNodeSet(protoInstance));
                break;
                
            case PROTO_SENSOR:
                if(T != Matrix44::Identity())
                    throw ModelNodeSet::Exception(protoName + " node is not in a correct place.");
                jointNodeSet->sensorNodes.push_back(protoInstance);
                putMessage(protoName + protoInstance->defName);
                break;
                
            case PROTO_HARDWARECOMPONENT:
                if(T != Matrix44::Identity())
                    throw ModelNodeSet::Exception(protoName + " node is not in a correct place.");
                jointNodeSet->hwcNodes.push_back(protoInstance);
                putMessage(protoName + protoInstance->defName);
                break;
                
            case PROTO_SEGMENT:
                {
                    jointNodeSet->segmentNodes.push_back(protoInstance);
                    jointNodeSet->transforms.push_back(T);
                    putMessage(string("Segment node ") + protoInstance->defName);

                    doTraverseChildren = true;
                    acceptableChildProtoIds.reset();
                    acceptableChildProtoIds.set(PROTO_SENSOR);
                }
                break;
                
            default:
                break;
            }

            if(doTraverseChildren){
                MFNode& childNodes = protoInstance->fields["children"].mfNode();
                extractChildNodes(jointNodeSet, childNodes, acceptableChildProtoIds, T);
            }

            messageIndent -= 2;
        }
    }
}


void ModelNodeSetImpl::putMessage(const std::string& message)
{
    if(!self->sigMessage.empty()) {
        string space(messageIndent, ' ');
        self->sigMessage(space + message + "\n");
    }
}
