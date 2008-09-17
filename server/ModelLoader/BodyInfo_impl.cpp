/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

/*!
  @file BodyInfo_impl.h
  @author Shin'ichiro Nakaoka
  @author Y.TSUNODA
*/

#include "BodyInfo_impl.h"

#include <map>
#include <vector>
#include <iostream>
#include <boost/bind.hpp>

#include <hrpCorba/ViewSimulator.h>
#include <hrpParser/VrmlNodes.h>
#include <hrpParser/ImageConverter.h>

#include "UtilFunctions.h"


using namespace std;
using namespace boost;

namespace {
    typedef map<string, string> SensorTypeMap;
    SensorTypeMap sensorTypeMap;
}
    

BodyInfo_impl::BodyInfo_impl(PortableServer::POA_ptr poa) :
    ShapeSetInfo_impl(poa)
{
    lastUpdate_ = 0;
}


BodyInfo_impl::~BodyInfo_impl()
{
    
}


const std::string& BodyInfo_impl::topUrl()
{
    return url_;
}


char* BodyInfo_impl::name()
{
    return CORBA::string_dup(name_.c_str());
}


char* BodyInfo_impl::url()
{
    return CORBA::string_dup(url_.c_str());
}


StringSequence* BodyInfo_impl::info()
{
    return new StringSequence(info_);
}


LinkInfoSequence* BodyInfo_impl::links()
{
    return new LinkInfoSequence(links_);
}


AllLinkShapeIndexSequence* BodyInfo_impl::linkShapeIndices()
{
    return new AllLinkShapeIndexSequence(linkShapeIndices_);
}


/*!
  @if jp
  @brief モデルファイルをロードし、BodyInfoを構築する。
  @else
  @brief This function loads a model file and creates a BodyInfo object.
  @param url The url to a model file
  @endif
*/
void BodyInfo_impl::loadModelFile(const std::string& url)
{
    string filename( deleteURLScheme( url ) );

    // URL文字列の' \' 区切り子を'/' に置き換え  Windows ファイルパス対応 
    string url2;
    url2 = filename;
    replace( url2, string("\\"), string("/") );
    filename = url2;

    ModelNodeSet modelNodeSet;
    modelNodeSet.sigMessage.connect(bind(&putMessage, _1));

    bool result = false;

    try	{
        result = modelNodeSet.loadModelFile( filename );

        if(result){
            applyTriangleMeshShaper(modelNodeSet.humanoidNode());
        }
        cout.flush();
    }
    catch(const ModelNodeSet::Exception& ex) {
        cout << ex.what() << endl;
        throw ModelLoader::ModelLoaderException(ex.what());
    }

    if(!result){
        throw ModelLoader::ModelLoaderException("The model file cannot be loaded.");
    }

    url_ = CORBA::string_dup(url2.c_str());
    
    const string& humanoidName = modelNodeSet.humanoidNode()->defName;
    name_ = CORBA::string_dup(humanoidName.c_str());

    int numJointNodes = modelNodeSet.numJointNodes();

    links_.length(numJointNodes);
    if( 0 < numJointNodes ) {
        int currentIndex = 0;
        JointNodeSetPtr rootJointNodeSet = modelNodeSet.rootJointNodeSet();
        readJointNodeSet(rootJointNodeSet, currentIndex, -1);
    }

    linkShapeIndices_.length(numJointNodes); 
    for(size_t i = 0 ; i < numJointNodes ; ++i) {
        linkShapeIndices_[i] = links_[i].shapeIndices;
    }
}


int BodyInfo_impl::readJointNodeSet(JointNodeSetPtr jointNodeSet, int& currentIndex, int parentIndex)
{
    int index = currentIndex;
    currentIndex++;

    LinkInfo_var linkInfo(new LinkInfo());
    linkInfo->parentIndex = parentIndex;

    size_t numChildren = jointNodeSet->childJointNodeSets.size();

    for( size_t i = 0 ; i < numChildren ; ++i ){
        JointNodeSetPtr childJointNodeSet = jointNodeSet->childJointNodeSets[i];
        int childIndex = readJointNodeSet(childJointNodeSet, currentIndex, index);

        long childIndicesLength = linkInfo->childIndices.length();
        linkInfo->childIndices.length( childIndicesLength + 1 );
        linkInfo->childIndices[childIndicesLength] = childIndex;
    }

    links_[index] = linkInfo;

    try	{
        Matrix44 unit4d(tvmet::identity<Matrix44>());
        traverseShapeNodes(jointNodeSet->segmentNode.get(), unit4d, links_[index].shapeIndices);

        setJointParameters(index, jointNodeSet->jointNode);
        setSegmentParameters(index, jointNodeSet->segmentNode);
        setSensors(index, jointNodeSet);
    }
    catch( ModelLoader::ModelLoaderException& ex ) {
        string name(linkInfo->name);
        string error = name.empty() ? "Unnamed JoitNode" : name;
        error += ": ";
        error += ex.description;
        throw ModelLoader::ModelLoaderException( error.c_str() );
    }

    return index;
}


void BodyInfo_impl::setJointParameters(int linkInfoIndex, VrmlProtoInstancePtr jointNode)
{
    LinkInfo& linkInfo = links_[linkInfoIndex];

    linkInfo.name =  CORBA::string_dup( jointNode->defName.c_str() );

    TProtoFieldMap& fmap = jointNode->fields;

    CORBA::Long jointId;
    copyVrmlField( fmap, "jointId", jointId );
    linkInfo.jointId = (CORBA::Short)jointId; 

    linkInfo.jointAxis[0] = 0.0;
    linkInfo.jointAxis[1] = 0.0;
    linkInfo.jointAxis[2] = 0.0;
    
    VrmlVariantField& fJointAxis = fmap["jointAxis"];

    switch( fJointAxis.typeId() ) {

    case SFSTRING:
    {
        SFString& axisLabel = fJointAxis.sfString();
            if( axisLabel == "X" )		{ linkInfo.jointAxis[0] = 1.0; }
            else if( axisLabel == "Y" )	{ linkInfo.jointAxis[1] = 1.0; }
            else if( axisLabel == "Z" ) { linkInfo.jointAxis[2] = 1.0; }
    }
    break;
		
    case SFVEC3F:
        copyVrmlField( fmap, "jointAxis", linkInfo.jointAxis );
        break;

    default:
        break;
    }

    std::string jointType;
    copyVrmlField( fmap, "jointType", jointType );
    linkInfo.jointType = CORBA::string_dup( jointType.c_str() );

    copyVrmlField( fmap, "translation", linkInfo.translation );
    copyVrmlRotationFieldToDblArray4( fmap, "rotation", linkInfo.rotation );

    copyVrmlField( fmap, "ulimit",  linkInfo.ulimit );
    copyVrmlField( fmap, "llimit",  linkInfo.llimit );
    copyVrmlField( fmap, "uvlimit", linkInfo.uvlimit );
    copyVrmlField( fmap, "lvlimit", linkInfo.lvlimit );

    copyVrmlField( fmap, "gearRatio",     linkInfo.gearRatio );
    copyVrmlField( fmap, "rotorInertia",  linkInfo.rotorInertia );
    copyVrmlField( fmap, "rotorResistor", linkInfo.rotorResistor );
    copyVrmlField( fmap, "torqueConst",   linkInfo.torqueConst );
    copyVrmlField( fmap, "encoderPulse",  linkInfo.encoderPulse );
    copyVrmlField( fmap, "jointValue",    linkInfo.jointValue );
}


void BodyInfo_impl::setSegmentParameters(int linkInfoIndex, VrmlProtoInstancePtr segmentNode)
{
    LinkInfo& linkInfo = links_[linkInfoIndex];

    if(segmentNode) {
        TProtoFieldMap& fmap = segmentNode->fields;
        copyVrmlField( fmap, "centerOfMass",     linkInfo.centerOfMass );
        copyVrmlField( fmap, "mass",             linkInfo.mass );
        copyVrmlField( fmap, "momentsOfInertia", linkInfo.inertia );
    } else {
        linkInfo.mass = 0.0;
        // set zero to centerOfMass and inertia
        for( int i = 0 ; i < 3 ; ++i ) {
            linkInfo.centerOfMass[i] = 0.0;
            for( int j = 0 ; j < 3 ; ++j ) {
                linkInfo.inertia[i*3 + j] = 0.0;
            }
        }
    }
}


void BodyInfo_impl::setSensors(int linkInfoIndex, JointNodeSetPtr jointNodeSet)
{
    LinkInfo& linkInfo = links_[linkInfoIndex];

    vector<VrmlProtoInstancePtr>& sensorNodes = jointNodeSet->sensorNodes;

    int numSensors = sensorNodes.size();
    linkInfo.sensors.length(numSensors);

    for(int i = 0 ; i < numSensors ; ++i) {
        SensorInfo_var sensorInfo( new SensorInfo() );
        readSensorNode( linkInfoIndex, sensorInfo, sensorNodes[i] );
        linkInfo.sensors[i] = sensorInfo;
    }
}


void BodyInfo_impl::readSensorNode(int linkInfoIndex, SensorInfo& sensorInfo, VrmlProtoInstancePtr sensorNode)
{
    if(sensorTypeMap.empty()) {
        sensorTypeMap["ForceSensor"]        = "Force";
        sensorTypeMap["Gyro"]               = "RateGyro";
        sensorTypeMap["AccelerationSensor"] = "Acceleration";
        sensorTypeMap["PressureSensor"]     = "";
        sensorTypeMap["PhotoInterrupter"]   = "";
        sensorTypeMap["VisionSensor"]       = "Vision";
        sensorTypeMap["TorqueSensor"]       = "";
    }

    try	{
        sensorInfo.name = CORBA::string_dup( sensorNode->defName.c_str() );

        TProtoFieldMap& fmap = sensorNode->fields;
        
        copyVrmlField(fmap, "sensorId", sensorInfo.id );
        copyVrmlField(fmap, "translation", sensorInfo.translation );
        copyVrmlRotationFieldToDblArray4( fmap, "rotation", sensorInfo.rotation );
        
        SensorTypeMap::iterator p = sensorTypeMap.find( sensorNode->proto->protoName );
        std::string sensorType;
        if(p != sensorTypeMap.end()){
            sensorType = p->second;
            sensorInfo.type = CORBA::string_dup( sensorType.c_str() );
        } else {
            throw ModelLoader::ModelLoaderException("Unknown Sensor Node");
        }

        if(sensorType == "Force") {
            sensorInfo.specValues.length( CORBA::ULong(6) );
            DblArray3 maxForce, maxTorque;
            copyVrmlField(fmap, "maxForce", maxForce );
            copyVrmlField(fmap, "maxTorque", maxTorque );
            sensorInfo.specValues[0] = maxForce[0];
            sensorInfo.specValues[1] = maxForce[1];
            sensorInfo.specValues[2] = maxForce[2];
            sensorInfo.specValues[3] = maxTorque[0];
            sensorInfo.specValues[4] = maxTorque[1];
            sensorInfo.specValues[5] = maxTorque[2];
            
        } else if(sensorType == "RateGyro") {
            sensorInfo.specValues.length( CORBA::ULong(3) );
            DblArray3 maxAngularVelocity;
            copyVrmlField(fmap, "maxAngularVelocity", maxAngularVelocity);
            sensorInfo.specValues[0] = maxAngularVelocity[0];
            sensorInfo.specValues[1] = maxAngularVelocity[1];
            sensorInfo.specValues[2] = maxAngularVelocity[2];
            
        } else if( sensorType == "Acceleration" ){
            sensorInfo.specValues.length( CORBA::ULong(3) );
            DblArray3 maxAcceleration;
            copyVrmlField(fmap, "maxAcceleration", maxAcceleration);
            sensorInfo.specValues[0] = maxAcceleration[0];
            sensorInfo.specValues[1] = maxAcceleration[1];
            sensorInfo.specValues[2] = maxAcceleration[2];
            
        } else if( sensorType == "Vision" ){
            sensorInfo.specValues.length( CORBA::ULong(7) );

            CORBA::Double specValues[3];
            copyVrmlField(fmap, "frontClipDistance", specValues[0] );
            copyVrmlField(fmap, "backClipDistance", specValues[1] );
            copyVrmlField(fmap, "fieldOfView", specValues[2] );
            sensorInfo.specValues[0] = specValues[0];
            sensorInfo.specValues[1] = specValues[1];
            sensorInfo.specValues[2] = specValues[2];
            
            std::string sensorTypeString;
            copyVrmlField(fmap, "type", sensorTypeString );
            
            if(sensorTypeString=="NONE" ) {
                sensorInfo.specValues[3] = Camera::NONE;
            } else if(sensorTypeString=="COLOR") {
                sensorInfo.specValues[3] = Camera::COLOR;
            } else if(sensorTypeString=="MONO") {
                sensorInfo.specValues[3] = Camera::MONO;
            } else if(sensorTypeString=="DEPTH") {
                sensorInfo.specValues[3] = Camera::DEPTH;
            } else if(sensorTypeString=="COLOR_DEPTH") {
                sensorInfo.specValues[3] = Camera::COLOR_DEPTH;
            } else if(sensorTypeString=="MONO_DEPTH") {
                sensorInfo.specValues[3] = Camera::MONO_DEPTH;
            } else {
                throw ModelLoader::ModelLoaderException("Sensor node has unkown type string");
            }

            CORBA::Long width, height;
            copyVrmlField(fmap, "width", width);
            copyVrmlField(fmap, "height", height);

            sensorInfo.specValues[4] = static_cast<CORBA::Double>(width);
            sensorInfo.specValues[5] = static_cast<CORBA::Double>(height);
	    
	    double frameRate;
            copyVrmlField(fmap, "frameRate", frameRate);
            sensorInfo.specValues[6] = frameRate;
        }

        /*
        Matrix44 T;
        const DblArray4& r = sensorInfo.rotation;
        calcRodrigues(T, Vector3(r[0], r[1], r[2]), r[3]);
        for(int i=0; i < 3; ++i){
            T(i,3) = sensorInfo.translation[i];
        }
        */

        Matrix44 E(tvmet::identity<Matrix44>());
        traverseShapeNodes(sensorNode.get(), E, links_[linkInfoIndex].shapeIndices);

    } catch(ModelLoader::ModelLoaderException& ex) {
        string error = name_.empty() ? "Unnamed sensor node" : name_;
        error += ": ";
        error += ex.description;
        throw ModelLoader::ModelLoaderException( error.c_str() );
    }
}
