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
#include <boost/format.hpp>

#include <hrpCorba/ViewSimulator.hh>
#include <hrpUtil/EasyScanner.h>
#include <hrpUtil/VrmlNodes.h>
#include <hrpUtil/VrmlParser.h>
#include <hrpUtil/ImageConverter.h>

#include "VrmlUtil.h"



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

ExtraJointInfoSequence* BodyInfo_impl::extraJoints()
{
	return new ExtraJointInfoSequence(extraJoints_);
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
    url_ = CORBA::string_dup(url2.c_str());
    

    ModelNodeSet modelNodeSet;
    modelNodeSet.sigMessage.connect(boost::bind(&putMessage, _1));

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
        cout << "Retrying to load the file as a standard VRML file" << endl;
        try {
            VrmlParser parser;
            parser.load(filename);

            links_.length(1);
            LinkInfo &linfo = links_[0];
            linfo.name = CORBA::string_dup("root");
            linfo.parentIndex = -1;
            linfo.jointId = -1;
            linfo.jointType = CORBA::string_dup("fixed");
            linfo.jointValue = 0;
            for (int i=0; i<3; i++){
                linfo.jointAxis[i] = 0;
                linfo.translation[i] = 0;
                linfo.rotation[i] = 0;
		linfo.centerOfMass[i] = 0;
            }
            linfo.jointAxis[2] = 1; 
            linfo.rotation[2] = 1; linfo.rotation[3] = 0;
	    linfo.mass = 0;
	    for (int i=0; i<9; i++) linfo.inertia[i] = 0;

            
            Matrix44 E(Matrix44::Identity());
            
            while(VrmlNodePtr node = parser.readNode()){
                if(!node->isCategoryOf(PROTO_DEF_NODE)){
                    applyTriangleMeshShaper(node);
                    traverseShapeNodes(node.get(), E, linfo.shapeIndices, linfo.inlinedShapeTransformMatrices, &topUrl());
                }
            }
            return;
        } catch(EasyScanner::Exception& ex){
            cout << ex.getFullMessage() << endl;
            throw ModelLoader::ModelLoaderException(ex.getFullMessage().c_str());
        }
    }

    if(!result){
        throw ModelLoader::ModelLoaderException("The model file cannot be loaded.");
    }

    const string& humanoidName = modelNodeSet.humanoidNode()->defName;
    name_ = CORBA::string_dup(humanoidName.c_str());
    const MFString& info = modelNodeSet.humanoidNode()->fields["info"].mfString();
    info_.length(info.size());
    for (unsigned int i=0; i<info_.length(); i++){
        info_[i] = CORBA::string_dup(info[i].c_str());
    }

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
    
    // build coldetModels 
    linkColdetModels.resize(numJointNodes);    
    for(int linkIndex = 0; linkIndex < numJointNodes ; ++linkIndex){
        ColdetModelPtr coldetModel(new ColdetModel());
        coldetModel->setName(std::string(links_[linkIndex].name));
        int vertexIndex = 0;
        int triangleIndex = 0;
        
        Matrix44 E(Matrix44::Identity());
        const TransformedShapeIndexSequence& shapeIndices = linkShapeIndices_[linkIndex];
        setColdetModel(coldetModel, shapeIndices, E, vertexIndex, triangleIndex);

        Matrix44 T(Matrix44::Identity());
        const SensorInfoSequence& sensors = links_[linkIndex].sensors;
        for (unsigned int i=0; i<sensors.length(); i++){
            const SensorInfo& sensor = sensors[i];
            calcRodrigues(T, Vector3(sensor.rotation[0], sensor.rotation[1], 
                                 sensor.rotation[2]), sensor.rotation[3]);
            T(0,3) = sensor.translation[0];
            T(1,3) = sensor.translation[1];
            T(2,3) = sensor.translation[2];
            const TransformedShapeIndexSequence& sensorShapeIndices = sensor.shapeIndices;
            setColdetModel(coldetModel, sensorShapeIndices, T, vertexIndex, triangleIndex);
        }
                       
        if(triangleIndex>0)    
            coldetModel->build();

        linkColdetModels[linkIndex] = coldetModel;
        links_[linkIndex].AABBmaxDepth = coldetModel->getAABBTreeDepth();
        links_[linkIndex].AABBmaxNum = coldetModel->getAABBmaxNum();
    }
    //saveOriginalData();
    //originlinkShapeIndices_ = linkShapeIndices_;

	int n = modelNodeSet.numExtraJointNodes();
	extraJoints_.length(n);
    for(int i=0; i < n; ++i){
		
        TProtoFieldMap& f = modelNodeSet.extraJointNode(i)->fields;
        ExtraJointInfo_var extraJointInfo(new ExtraJointInfo());
		extraJointInfo->name =  CORBA::string_dup( modelNodeSet.extraJointNode(i)->defName.c_str() );

        string link1Name, link2Name;
		copyVrmlField( f, "link1Name", link1Name );
		copyVrmlField( f, "link2Name", link2Name );
		extraJointInfo->link[0] = CORBA::string_dup(link1Name.c_str());
		extraJointInfo->link[1] = CORBA::string_dup(link2Name.c_str());
 
		string jointType;
		copyVrmlField( f, "jointType", jointType);
        if(jointType == "xy"){
		   extraJointInfo->jointType = EJ_XY;
        } else if(jointType == "xyz"){
           extraJointInfo->jointType = EJ_XYZ;
        } else if(jointType == "z"){
           extraJointInfo->jointType = EJ_Z;
        }else {
            throw ModelNodeSet::Exception(str(format("JointType \"%1%\" is not supported.") % jointType));
        }
        copyVrmlField( f, "jointAxis", extraJointInfo->axis );    
		copyVrmlField( f, "link1LocalPos", extraJointInfo->point[0] );
		copyVrmlField( f, "link2LocalPos", extraJointInfo->point[1] );
	
		extraJoints_[i] = extraJointInfo;
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
        vector<VrmlProtoInstancePtr>& segmentNodes = jointNodeSet->segmentNodes;
        int numSegment = segmentNodes.size();
        links_[index].segments.length(numSegment);
        for(int i = 0 ; i < numSegment ; ++i){
            SegmentInfo_var segmentInfo(new SegmentInfo());
            Matrix44 T = jointNodeSet->transforms.at(i);
            long s = links_[index].shapeIndices.length();
            int p = 0;
            for(int row=0; row < 3; ++row){
                for(int col=0; col < 4; ++col){
                    segmentInfo->transformMatrix[p++] = T(row, col);
                }
            }
            traverseShapeNodes(segmentNodes[i].get(), T, links_[index].shapeIndices, links_[index].inlinedShapeTransformMatrices, &topUrl());
            long e =links_[index].shapeIndices.length();
            segmentInfo->shapeIndices.length(e-s);
            for(int j=0, k=s; k<e; k++)
                segmentInfo->shapeIndices[j++] = k;
            links_[index].segments[i] = segmentInfo;
        }
        setJointParameters(index, jointNodeSet->jointNode);
        setSegmentParameters(index, jointNodeSet);
        setSensors(index, jointNodeSet);
        setHwcs(index, jointNodeSet);
        setLights(index, jointNodeSet);
    }
    catch( ModelLoader::ModelLoaderException& ex ) {
        string name(linkInfo->name);
        string error = name.empty() ? "Unnamed JointNode" : name;
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

    if(fmap["climit"].typeId() != UNDETERMINED_FIELD_TYPE){
        copyVrmlField( fmap, "climit", linkInfo.climit );
    }else{
        //std::cout << "No climit type. climit was ignored." << std::endl;        
        linkInfo.climit.length((CORBA::ULong)0); // dummy
    }

    copyVrmlField( fmap, "gearRatio",     linkInfo.gearRatio );
    copyVrmlField( fmap, "rotorInertia",  linkInfo.rotorInertia );
    copyVrmlField( fmap, "rotorResistor", linkInfo.rotorResistor );
    copyVrmlField( fmap, "torqueConst",   linkInfo.torqueConst );
    copyVrmlField( fmap, "encoderPulse",  linkInfo.encoderPulse );
    copyVrmlField( fmap, "jointValue",    linkInfo.jointValue );
}

void BodyInfo_impl::setSegmentParameters(int linkInfoIndex, JointNodeSetPtr jointNodeSet)
{
    LinkInfo& linkInfo = links_[linkInfoIndex];

    vector<VrmlProtoInstancePtr>& segmentNodes = jointNodeSet->segmentNodes;
    int numSegment = segmentNodes.size();

    linkInfo.mass = 0.0;
    for( int i = 0 ; i < 3 ; ++i ) {
        linkInfo.centerOfMass[i] = 0.0;
        for( int j = 0 ; j < 3 ; ++j ) {
            linkInfo.inertia[i*3 + j] = 0.0;
        }
    }

    //  Mass = Σmass                 //
    //  C = (Σmass * T * c) / Mass   //
    //  I = Σ(R * I * Rt + G)       //
    //  R = Tの回転行列               //
    //  G = y*y+z*z, -x*y, -x*z, -y*x, z*z+x*x, -y*z, -z*x, -z*y, x*x+y*y    //
    //  (x, y, z ) = T * c - C        //
    std::vector<Vector4, Eigen::aligned_allocator<Vector4> > centerOfMassArray;
    std::vector<double> massArray;
    for(int i = 0 ; i < numSegment ; ++i){
        SegmentInfo& segmentInfo = linkInfo.segments[i];
        Matrix44 T = jointNodeSet->transforms.at(i);
        DblArray3& centerOfMass = segmentInfo.centerOfMass;
        CORBA::Double& mass =segmentInfo.mass;
        DblArray9& inertia = segmentInfo.inertia;
        TProtoFieldMap& fmap = segmentNodes[i]->fields;
        copyVrmlField( fmap, "centerOfMass",     centerOfMass );
        copyVrmlField( fmap, "mass",             mass );
        copyVrmlField( fmap, "momentsOfInertia", inertia );
        Vector4 c0(centerOfMass[0], centerOfMass[1], centerOfMass[2], 1.0);
        Vector4 c1(T * c0);
        centerOfMassArray.push_back(c1);
        massArray.push_back(mass);
        for(int j=0; j<3; j++){
            linkInfo.centerOfMass[j] = c1(j) * mass + linkInfo.centerOfMass[j] * linkInfo.mass;
        }
        linkInfo.mass += mass;
        if(linkInfo.mass > 0.0){
            for(int j=0; j<3; j++){
                linkInfo.centerOfMass[j] /= linkInfo.mass;
            }
        }
        Matrix33 I;
        I << inertia[0], inertia[1], inertia[2], inertia[3], inertia[4], inertia[5], inertia[6], inertia[7], inertia[8];
        Matrix33 R;
        R << T(0,0), T(0,1), T(0,2), T(1,0), T(1,1), T(1,2), T(2,0), T(2,1), T(2,2);
        Matrix33 I1(R * I * R.transpose());
        for(int j=0; j<3; j++){
            for(int k=0; k<3; k++)
                linkInfo.inertia[j*3+k] += I1(j,k);    
        }
        segmentInfo.name = CORBA::string_dup( segmentNodes[i]->defName.c_str() );
    }
    if(linkInfo.mass <=0.0 )
        std::cerr << "Warning: Mass is zero. <Model>" << this->name() << " <Link>" << linkInfo.name << std::endl;

    for(int i = 0 ; i < numSegment ; ++i){
        Vector4 c( centerOfMassArray.at(i) );
        double x = c(0) - linkInfo.centerOfMass[0];
        double y = c(1) - linkInfo.centerOfMass[1];
        double z = c(2) - linkInfo.centerOfMass[2];
        double m = massArray.at(i);

        linkInfo.inertia[0] += m * (y*y + z*z);
        linkInfo.inertia[1] += -m * x * y;
        linkInfo.inertia[2] += -m * x * z;
        linkInfo.inertia[3] += -m * y * x;
        linkInfo.inertia[4] += m * (z*z + x*x);
        linkInfo.inertia[5] += -m * y * z;
        linkInfo.inertia[6] += -m * z * x;
        linkInfo.inertia[7] += -m * z * y;
        linkInfo.inertia[8] += m * (x*x + y*y);
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


void BodyInfo_impl::setHwcs(int linkInfoIndex, JointNodeSetPtr jointNodeSet)
{
    LinkInfo& linkInfo = links_[linkInfoIndex];

    vector<VrmlProtoInstancePtr>& hwcNodes = jointNodeSet->hwcNodes;

    int numHwcs = hwcNodes.size();
    linkInfo.hwcs.length(numHwcs);

    for(int i = 0 ; i < numHwcs ; ++i) {
        HwcInfo_var hwcInfo( new HwcInfo() );
        readHwcNode( linkInfoIndex, hwcInfo, hwcNodes[i] );
        linkInfo.hwcs[i] = hwcInfo;
    }
}

void BodyInfo_impl::setLights(int linkInfoIndex, JointNodeSetPtr jointNodeSet)
{
    LinkInfo& linkInfo = links_[linkInfoIndex];

    vector<pair<Matrix44, VrmlNodePtr>,
	   Eigen::aligned_allocator<pair<Matrix44, VrmlNodePtr> > >& lightNodes = jointNodeSet->lightNodes;

    int numLights = lightNodes.size();
    linkInfo.lights.length(numLights);

    for(int i = 0 ; i < numLights ; ++i) {
        LightInfo_var lightInfo( new LightInfo() );
        readLightNode( linkInfoIndex, lightInfo, lightNodes[i] );
        linkInfo.lights[i] = lightInfo;
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
        sensorTypeMap["RangeSensor"]	    = "Range";
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
        } else if( sensorType == "Range" ){
            sensorInfo.specValues.length( CORBA::ULong(4) );
            CORBA::Double v;
            copyVrmlField(fmap, "scanAngle", v);
            sensorInfo.specValues[0] = v;
            copyVrmlField(fmap, "scanStep", v);
            sensorInfo.specValues[1] = v;
            copyVrmlField(fmap, "scanRate", v);
            sensorInfo.specValues[2] = v;
            copyVrmlField(fmap, "maxDistance", v);
            sensorInfo.specValues[3] = v;
        }

        /*
        Matrix44 T;
        const DblArray4& r = sensorInfo.rotation;
        calcRodrigues(T, Vector3(r[0], r[1], r[2]), r[3]);
        for(int i=0; i < 3; ++i){
            T(i,3) = sensorInfo.translation[i];
        }
        */

        Matrix44 E(Matrix44::Identity());
        VrmlVariantField *field = sensorNode->getField("children");
        if (field){
            MFNode &children = field->mfNode();
            for (unsigned int i=0; i<children.size(); i++){
                traverseShapeNodes(children[i].get(), E, 
                                   sensorInfo.shapeIndices, sensorInfo.inlinedShapeTransformMatrices, &topUrl());
            }
        }
    } catch(ModelLoader::ModelLoaderException& ex) {
        string error = name_.empty() ? "Unnamed sensor node" : name_;
        error += ": ";
        error += ex.description;
        throw ModelLoader::ModelLoaderException( error.c_str() );
    }
}

void BodyInfo_impl::readHwcNode(int linkInfoIndex, HwcInfo& hwcInfo, VrmlProtoInstancePtr hwcNode )
{
    try	{
        hwcInfo.name = CORBA::string_dup( hwcNode->defName.c_str() );

        TProtoFieldMap& fmap = hwcNode->fields;
        
        copyVrmlField(fmap, "id", hwcInfo.id );
        copyVrmlField(fmap, "translation", hwcInfo.translation );
        copyVrmlRotationFieldToDblArray4( fmap, "rotation", hwcInfo.rotation );
        std::string url;
        copyVrmlField( fmap, "url", url );
        hwcInfo.url = CORBA::string_dup( url.c_str() );

        Matrix44 E(Matrix44::Identity());
        VrmlVariantField *field = hwcNode->getField("children");
        if (field){
            MFNode &children = field->mfNode();
            for (unsigned int i=0; i<children.size(); i++){
                traverseShapeNodes(children[i].get(), E, 
                                   hwcInfo.shapeIndices, hwcInfo.inlinedShapeTransformMatrices, &topUrl());
            }
        }
    } catch(ModelLoader::ModelLoaderException& ex) {
        throw ModelLoader::ModelLoaderException( ex.description );
    }
}

void BodyInfo_impl::readLightNode(int linkInfoIndex, LightInfo& lightInfo, 
                                  std::pair<Matrix44, VrmlNodePtr> &transformedLight )
{
    VrmlNode *lightNode = transformedLight.second.get();
    Matrix44 &T = transformedLight.first;
    for (int i=0; i<3; i++){
        for (int j=0; j<4; j++){
            lightInfo.transformMatrix[i*4+j] =  T(i,j);
        }
    }
    try	{
        lightInfo.name = CORBA::string_dup( lightNode->defName.c_str() );
        VrmlPointLight *plight = dynamic_cast<VrmlPointLight *>(lightNode);
        VrmlDirectionalLight *dlight = dynamic_cast<VrmlDirectionalLight *>(lightNode);
        VrmlSpotLight *slight = dynamic_cast<VrmlSpotLight *>(lightNode);
        if (plight){
            lightInfo.type = OpenHRP::POINT;
            lightInfo.ambientIntensity = plight->ambientIntensity;
            lightInfo.intensity = plight->intensity;
            lightInfo.on = plight->on;
            lightInfo.radius = plight->radius;
            for (int i=0; i<3; i++){
                lightInfo.attenuation[i] = plight->attenuation[i];
                lightInfo.color[i] = plight->color[i];
                lightInfo.location[i] = plight->location[i];
            }
        }else if(dlight){
            lightInfo.type = OpenHRP::DIRECTIONAL;
            lightInfo.ambientIntensity = dlight->ambientIntensity;
            lightInfo.intensity = dlight->intensity;
            lightInfo.on = dlight->on;
            for (int i=0; i<3; i++){
                lightInfo.color[i] = dlight->color[i];
                lightInfo.direction[i] = dlight->direction[i];
            }
        }else if(slight){
            lightInfo.type = OpenHRP::SPOT;
            lightInfo.ambientIntensity = slight->ambientIntensity;
            lightInfo.intensity = slight->intensity;
            lightInfo.on = slight->on;
            lightInfo.radius = slight->radius;
            lightInfo.beamWidth = slight->beamWidth;
            lightInfo.cutOffAngle = slight->cutOffAngle;
            for (int i=0; i<3; i++){
                lightInfo.attenuation[i] = slight->attenuation[i];
                lightInfo.color[i] = slight->color[i];
                lightInfo.location[i] = slight->location[i];
                lightInfo.direction[i] = slight->direction[i];
            }
        }else{
            throw ModelLoader::ModelLoaderException("unknown light type");
        }
    } catch(ModelLoader::ModelLoaderException& ex) {
        throw ModelLoader::ModelLoaderException( ex.description );
    }
}

void BodyInfo_impl::setParam(std::string param, bool value){
    if(param == "readImage")
        readImage = value;
    else
        ;
}

bool BodyInfo_impl::getParam(std::string param){
    // FIXME: should this be an assert?
    // pros for assert: can be turned off on release builds
    // cons for assert: can lead to unpredictable behavior in
    // release builds if the assertion is violated
    if(param == "readImage")
        return readImage;
    else
        abort ();
}

void BodyInfo_impl::setParam(std::string param, int value){
    if(param == "AABBType")
        AABBdataType_ = (OpenHRP::ModelLoader::AABBdataType)value;
    else
        ;
}

void BodyInfo_impl::changetoBoundingBox(unsigned int* inputData){
    const double EPS = 1.0e-6;
    createAppearanceInfo();
    std::vector<Vector3> boxSizeMap;
    std::vector<Vector3> boundingBoxData;
    
    for(int i=0; i<links_.length(); i++){
        int _depth;
        if( AABBdataType_ == OpenHRP::ModelLoader::AABB_NUM )
            _depth = linkColdetModels[i]->numofBBtoDepth(inputData[i]);
        else
            _depth = inputData[i];
        if( _depth >= links_[i].AABBmaxDepth)
            _depth = links_[i].AABBmaxDepth-1;
        if(_depth >= 0 ){
            linkColdetModels[i]->getBoundingBoxData(_depth, boundingBoxData);
            std::vector<TransformedShapeIndex> tsiMap;
            links_[i].shapeIndices.length(0);
            SensorInfoSequence& sensors = links_[i].sensors;
            for (unsigned int j=0; j<sensors.length(); j++){
                SensorInfo& sensor = sensors[j];
                sensor.shapeIndices.length(0);
            }

            for(int j=0; j<boundingBoxData.size()/2; j++){

                bool flg=false;
                int k=0;
                for( ; k<boxSizeMap.size(); k++)
                    if((boxSizeMap[k] - boundingBoxData[j*2+1]).norm() < EPS)
                        break;
                if( k<boxSizeMap.size() )
                    flg=true;
                else{
                    boxSizeMap.push_back(boundingBoxData[j*2+1]);
                    setBoundingBoxData(boundingBoxData[j*2+1],k);
                }

                if(flg){
                    int l=0;
                    for( ; l<tsiMap.size(); l++){
                        Vector3 p(tsiMap[l].transformMatrix[3],tsiMap[l].transformMatrix[7],tsiMap[l].transformMatrix[11]);
                        if((p - boundingBoxData[j*2]).norm() < EPS && tsiMap[l].shapeIndex == k)
                            break;
                    }
                    if( l==tsiMap.size() )
                        flg=false;
                }

                if(!flg){
                    int num = links_[i].shapeIndices.length();
                    links_[i].shapeIndices.length(num+1);
                    TransformedShapeIndex& tsi = links_[i].shapeIndices[num];
                    tsi.inlinedShapeTransformMatrixIndex = -1;
                    tsi.shapeIndex = k;
                    Matrix44 T(Matrix44::Identity());
                    for(int p = 0,row=0; row < 3; ++row)
                       for(int col=0; col < 4; ++col)
                            if(col==3){
                                switch(row){
                                    case 0:
                                        tsi.transformMatrix[p++] = boundingBoxData[j*2][0];
                                        break;
                                     case 1:
                                        tsi.transformMatrix[p++] = boundingBoxData[j*2][1];
                                        break;
                                     case 2:
                                        tsi.transformMatrix[p++] = boundingBoxData[j*2][2];
                                        break;
                                     default:
                                        ;
                                }
                            }else
                                tsi.transformMatrix[p++] = T(row, col);

                    tsiMap.push_back(tsi);
                }
            }
        }   
        linkShapeIndices_[i] = links_[i].shapeIndices;
    }
}

void BodyInfo_impl::changetoOriginData(){
    linkShapeIndices_ = originlinkShapeIndices_;
    for(size_t i = 0 ; i < links_.length() ; ++i) {
        links_[i].shapeIndices = linkShapeIndices_[i];
    }
    restoreOriginalData();
}
