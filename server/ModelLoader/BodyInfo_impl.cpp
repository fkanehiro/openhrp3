/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */

/*!
  @file BodyInfo_impl.h
  @author Shin'ichiro Nakaoka
  @author Y.TSUNODA (Ergovision)
*/

#include "BodyInfo_impl.h"

#include <map>
#include <vector>
#include <iostream>
#include <boost/bind.hpp>

#include <OpenHRP/Corba/ViewSimulator.h>

#include <OpenHRP/Parser/VrmlNodes.h>
#include <OpenHRP/Parser/CalculateNormal.h>
#include <OpenHRP/Parser/ImageConverter.h>

#include "UtilFunctions.h"


using namespace std;
using namespace boost;
using namespace OpenHRP;


namespace {

    typedef map<string, string> SensorTypeMap;
    SensorTypeMap sensorTypeMap;
    
    bool operator != (const matrix44d& a, const matrix44d& b)
    {
        for(int i = 0; i < 4; i++) {
            for(int j = 0; j < 4; j++) {
                if(a( i, j ) != b( i, j ))
                    return false;
            }
        }
        return true;
    }
    
    /**
       This function was imported from tvmet3d.cpp in the Base library
    */
    static void rodrigues(matrix33d& out_R, const vector3d& axis, double q)
    {
        const double sth = sin(q);
        const double vth = 1.0 - cos(q);
        
	vector3d a = static_cast<vector3d>( tvmet::normalize( axis ) );
        
        double ax = a(0);
        double ay = a(1);
        double az = a(2);
        
        const double axx = ax*ax*vth;
        const double ayy = ay*ay*vth;
        const double azz = az*az*vth;
        const double axy = ax*ay*vth;
        const double ayz = ay*az*vth;
        const double azx = az*ax*vth;
        
        ax *= sth;
        ay *= sth;
        az *= sth;
        
        out_R = 1.0 - azz - ayy, -az + axy,       ay + azx,
            az + axy,        1.0 - azz - axx, -ax + ayz,
            -ay + azx,       ax + ayz,        1.0 - ayy - axx;
    }

}
    


BodyInfo_impl::BodyInfo_impl( PortableServer::POA_ptr poa ) :
    poa(PortableServer::POA::_duplicate( poa ))
{
    lastUpdate_ = 0;
}


BodyInfo_impl::~BodyInfo_impl()
{
    
}


PortableServer::POA_ptr BodyInfo_impl::_default_POA()
{
    return PortableServer::POA::_duplicate( poa );
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


AllLinkShapeIndices* BodyInfo_impl::linkShapeIndices()
{
    return new AllLinkShapeIndices( linkShapeIndices_ );
}


ShapeInfoSequence* BodyInfo_impl::shapes()
{
    return new ShapeInfoSequence( shapes_ );
}


AppearanceInfoSequence* BodyInfo_impl::appearances()
{
    return new AppearanceInfoSequence( appearances_ );
}


MaterialInfoSequence* BodyInfo_impl::materials()
{
    return new MaterialInfoSequence( materials_ );
}


TextureInfoSequence* BodyInfo_impl::textures()
{
    return new TextureInfoSequence( textures_ );
}


void BodyInfo_impl::putMessage( const std::string& message )
{
    cout << message;
}


/**
  @if jp
  @brief 文字列置換
  @return str 内の 特定文字列　sb を 別の文字列　sa に置換
  @endif
*/
string& BodyInfo_impl::replace(string& str, const string sb, const string sa)
{
    string::size_type n, nb = 0;
	
    while ((n = str.find(sb,nb)) != string::npos){
        str.replace(n,sb.size(),sa);
        nb = n + sa.size();
    }
	
    return str;
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
    modelNodeSet.signalOnStatusMessage.connect(bind(&BodyInfo_impl::putMessage, this, _1));
    modelNodeSet.setMessageOutput( true );

    try	{
        modelNodeSet.loadModelFile( filename );
        cout.flush();
    }
    catch(ModelNodeSet::Exception& ex) {
        throw ModelLoader::ModelLoaderException(ex.message.c_str());
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

    LinkInfo_var linkInfo( new LinkInfo() );
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
        matrix44d unit4d( tvmet::identity<matrix44d>() );
        traverseShapeNodes(index, jointNodeSet->segmentNode->fields["children"].mfNode(), unit4d);

        setJointParameters(index, jointNodeSet->jointNode);
        setSegmentParameters(index, jointNodeSet->segmentNode);
        setSensors(index, jointNodeSet);
    }
    catch( ModelLoader::ModelLoaderException& ex ) {
        //CORBA::String_var cName = linkInfo->name;
        //string name( cName );
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

    // equivalentInertia は廃止
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
        // initSensorTypeMap();
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
            sensorInfo.specValues.length( CORBA::ULong(6) );

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
        }

        matrix33d mRotation;
        vector3d axis(sensorInfo.rotation[0], sensorInfo.rotation[1], sensorInfo.rotation[2]);
        rodrigues(mRotation, axis, sensorInfo.rotation[3]);

        matrix44d mTransform;
        mTransform =
            mRotation(0,0), mRotation(0,1), mRotation(0,2), sensorInfo.translation[0],
            mRotation(1,0), mRotation(1,1), mRotation(1,2), sensorInfo.translation[1],
            mRotation(2,0), mRotation(2,1), mRotation(2,2), sensorInfo.translation[2],
            0.0,            0.0,            0.0,		    1.0;

        if(NULL != sensorNode->getField("children")){
            traverseShapeNodes( linkInfoIndex, sensorNode->fields["children"].mfNode(), mTransform );
        }

    } catch(ModelLoader::ModelLoaderException& ex) {
        string error = name_.empty() ? "Unnamed sensor node" : name_;
        error += ": ";
        error += ex.description;
        throw ModelLoader::ModelLoaderException( error.c_str() );
    }
}


/*!
  @if jp
  Shape ノード探索のための再帰関数

  子ノードオブジェクトを辿り ShapeInfoを生成する。
  生成したShapeInfoはBodyInfoのshapes_に追加する。
  shapes_に追加した位置(index)を LinkInfoのshapeIndicesに追加する。
  @endif
*/
void BodyInfo_impl::traverseShapeNodes(int linkInfoIndex, MFNode& childNodes, const matrix44d& transform)
{
    LinkInfo& linkInfo = links_[linkInfoIndex];

    for(size_t i = 0; i < childNodes.size(); ++i) {
        VrmlNodePtr node = childNodes[i];

        if(node->isCategoryOf(GROUPING_NODE)) {
            VrmlGroupPtr groupNode = static_pointer_cast<VrmlGroup>(node);
            VrmlTransformPtr transformNode = dynamic_pointer_cast<VrmlTransform>(groupNode);
            if(!transformNode){
                traverseShapeNodes(linkInfoIndex, groupNode->children, transform);
            } else {
                matrix44d localTransform;
                calcTransform(transformNode, localTransform); // このノードで設定された transform (scaleも含む)
                traverseShapeNodes(linkInfoIndex, groupNode->children, matrix44d(transform * localTransform));
            }

        } else if(node->isCategoryOf(SHAPE_NODE)) {

            VrmlShapePtr shapeNode = static_pointer_cast<VrmlShape>(node);
            
            short shapeInfoIndex;

            // すでに生成済みのShapeノードか？
            //! \todo Transform の比較をファジーにした方がよい
            SharedShapeInfoMap::iterator itr = sharedShapeInfoMap.find(shapeNode);
            if((itr != sharedShapeInfoMap.end()) && (itr->second.transform != transform)){
                shapeInfoIndex = itr->second.index;
            } else {
                shapeInfoIndex = createShapeInfo(shapeNode, transform);
            }

            if(shapeInfoIndex >= 0){
                // indexを LinkInfo の shapeIndices に追加する
                long shapeIndicesLength = linkInfo.shapeIndices.length();
                linkInfo.shapeIndices.length( shapeIndicesLength + 1 );
                linkInfo.shapeIndices[shapeIndicesLength] = shapeInfoIndex;
            }
        }
    }
}


/**
   @return the index of a created ShapeInfo object. The return value is -1 if the creation fails.
*/
int BodyInfo_impl::createShapeInfo(VrmlShapePtr shapeNode, const matrix44d& transform)
{
    int shapeInfoIndex = -1;
    
    // 整形処理.ここの処理は OpenHRP/Parser ライブラリに移すべき
    UniformedShape uniformShape;
    uniformShape.signalOnStatusMessage.connect(bind(&BodyInfo_impl::putMessage, this, _1));
    uniformShape.setMessageOutput(true);

    if(uniformShape.uniform(shapeNode)) {
        // 整形処理結果を格納
        ShapeInfo_var shapeInfo(new ShapeInfo);
        
        // 頂点・メッシュを代入する
        setVertices(shapeInfo, uniformShape.getVertexList(), transform);
        setTriangles(shapeInfo, uniformShape.getTriangleList());
        
        // PrimitiveTypeを代入する
        setShapeInfoType(shapeInfo, uniformShape.getShapeType());
        
        // AppearanceInfo
        shapeInfo->appearanceIndex = createAppearanceInfo(shapeNode, uniformShape, transform);
        
        // shapes_の最後に追加する
        shapeInfoIndex = shapes_.length();
        shapes_.length(shapeInfoIndex + 1);
        shapes_[shapeInfoIndex] = shapeInfo;
        
        // shapeInfo共有マップに このshapeInfo(node)とindex,transformの情報を登録(挿入)する
        ShapeObject shapeObject;
        shapeObject.index = shapeInfoIndex;
        shapeObject.transform = transform;
        sharedShapeInfoMap.insert(make_pair(shapeNode, shapeObject));
    }
        
    return shapeInfoIndex;
}


/**
   @return the index of a created AppearanceInfo object. The return value is -1 if the creation fails.
*/
int BodyInfo_impl::createAppearanceInfo(VrmlShapePtr shapeNode, UniformedShape& uniformedShape, const matrix44d& transform)
{
    int appearanceIndex = -1;
    
    VrmlAppearancePtr appearanceNode = shapeNode->appearance;

    if(appearanceNode) {

        AppearanceInfo_var appearance( new AppearanceInfo() );
        appearance->creaseAngle = 3.14 / 2.0;

        switch(uniformedShape.getShapeType()){

        case UniformedShape::S_INDEXED_FACE_SET:
            {
                VrmlIndexedFaceSetPtr faceSet = static_pointer_cast<VrmlIndexedFaceSet>(shapeNode->geometry);
                                
                appearance->coloerPerVertex = faceSet->colorPerVertex;
                
                if(faceSet->color){
                    size_t colorNum = faceSet->color->color.size();
                    appearance->colors.length( colorNum * 3 );
                    for(size_t i = 0 ; i < colorNum ; ++i){
                        SFColor color = faceSet->color->color[i];
                        appearance->colors[3*i+0] = color[0];
                        appearance->colors[3*i+1] = color[1];
                        appearance->colors[3*i+2] = color[2];
                    }
                    
                    size_t colorIndexNum = faceSet->colorIndex.size();
                    appearance->colorIndices.length( colorIndexNum );
                    for( size_t i = 0 ; i < colorIndexNum ; ++i ){
                        appearance->colorIndices[i] = faceSet->colorIndex[i];
                    }
                    
                    appearance->normalPerVertex = faceSet->normalPerVertex;
                    appearance->solid = faceSet->solid;
                    appearance->creaseAngle = faceSet->creaseAngle;
                    
                    // ##### [TODO] #####
                    //appearance->textureCoordinate = faceSet->texCood;
                    
                    setNormals(appearance, uniformedShape.getVertexList(), uniformedShape.getTriangleList(), transform);
                }
            }
            break;

        case UniformedShape::S_ELEVATION_GRID:
            {
                VrmlElevationGridPtr elevationGrid = static_pointer_cast<VrmlElevationGrid>(shapeNode->geometry);
                
                appearance->coloerPerVertex = elevationGrid->colorPerVertex;
                
                if(elevationGrid->color) {
                    size_t colorNum = elevationGrid->color->color.size();
                    appearance->colors.length( colorNum * 3 );
                    for(size_t i = 0 ; i < colorNum ; ++i) {
                        SFColor color = elevationGrid->color->color[i];
                        appearance->colors[3*i+0] = color[0];
                        appearance->colors[3*i+1] = color[1];
                        appearance->colors[3*i+2] = color[2];
                    }
                }
                
                // appearance->colorIndices // ElevationGrid のメンバには無し
                
                appearance->normalPerVertex = elevationGrid->normalPerVertex;
                appearance->solid = elevationGrid->solid;
                appearance->creaseAngle = elevationGrid->creaseAngle;
                
                // ##### [TODO] #####
                //appearance->textureCoordinate = elevationGrid->texCood;
                
                setNormals(appearance, uniformedShape.getVertexList(), uniformedShape.getTriangleList(), transform);
            }
            break;
        }
        
        appearance->materialIndex = createMaterialInfo(appearanceNode->material);
        appearance->textureIndex  = createTextureInfo (appearanceNode->texture);

        appearanceIndex	= appearances_.length();
        appearances_.length(appearanceIndex + 1);
        appearances_[appearanceIndex] = appearance;
    }

    return appearanceIndex;
}

    
/*!
  @if jp
  頂点リストに格納されている頂点座標をShapeInfo.verticesに代入する.
  transformとして与えられた回転・並進成分を全ての頂点に反映する.
  @endif
*/
void BodyInfo_impl::setVertices(ShapeInfo_var& shape, const vector<vector3d>& vertices, const matrix44d& transform)
{
    size_t numVertices = vertices.size();
    shape->vertices.length(numVertices * 3);

    int i = 0;
    for(size_t v = 0 ; v < numVertices ; v++){
        const vector3d& vorg = vertices[v];
        vector4d v(vorg(0), vorg(1), vorg(2), 1.0);
        vector4d transformed(transform * v);
        shape->vertices[i++] = transformed[0];
        shape->vertices[i++] = transformed[1];
        shape->vertices[i++] = transformed[2];
    }
}


/*!
  @if jp
  三角メッシュ情報をShapeInfo.trianglesに代入
  @endif
*/
void BodyInfo_impl::setTriangles(ShapeInfo_var& shape, const vector<vector3i>& triangles)
{
    const size_t numTriangles = triangles.size();
    shape->triangles.length(numTriangles * 3);
	
    int i = 0;
    for(size_t t = 0 ; t < numTriangles ; t++){
        shape->triangles[i++] = triangles[t][0];
        shape->triangles[i++] = triangles[t][1];
        shape->triangles[i++] = triangles[t][2];
    }
}


/*!
  @if jp
  頂点リスト・三角メッシュリストから法線を計算し，AppearanceInfoに代入する
  @retval appearance 計算結果の法線を代入するAppearanceInfo
  @todo この機能は整形部に移すべし
  @endif
*/
void BodyInfo_impl::setNormals(AppearanceInfo_var& appearance, const vector<vector3d>& vertexList, const vector<vector3i>& traiangleList, const matrix44d& transform)
{
    // ここの処理ってsetVerticesとダブってるよ。廃止！
    vector<vector3d> transformedVertexList;
    vector4d vertex4;				
    vector4d transformed4;			

    for(size_t v = 0; v < vertexList.size(); ++v){
        vertex4 = vertexList[v][0], vertexList[v][1], vertexList[v][2], 1; 
        transformed4 = transform * vertex4;
        transformedVertexList.push_back(vector3d(transformed4[0], transformed4[1], transformed4[2]));
    }


    CalculateNormal calculateNormal;

    // メッシュの法線(面の法線)を計算する
    calculateNormal.calculateNormalsOfMesh(transformedVertexList, traiangleList);

    // 頂点の法線
    if(appearance->normalPerVertex) {
        calculateNormal.calculateNormalsOfVertex(transformedVertexList, traiangleList, appearance->creaseAngle);

        const vector<vector3d>& normalsVertex = calculateNormal.getNormalsOfVertex();
        const vector<vector3i>& normalIndex = calculateNormal.getNormalIndex();

        // 法線データを代入する
        size_t normalsVertexNum = normalsVertex.size();
        appearance->normals.length(normalsVertexNum * 3);

        // AppearanceInfo のメンバに代入する
        for(size_t i = 0; i < normalsVertexNum; ++i) {
            vector3d normal(tvmet::normalize(normalsVertex[i]));
            appearance->normals[3*i+0] = normal[0];
            appearance->normals[3*i+1] = normal[1];
            appearance->normals[3*i+2] = normal[2];
        }

        // 法線対応付けデータ(インデックス列)を代入する
        size_t normalIndexNum = normalIndex.size();
        appearance->normalIndices.length( normalIndexNum * 4 );

        for(size_t i = 0; i < normalIndexNum; ++i){
            appearance->normalIndices[4*i+0] = normalIndex[i][0];
            appearance->normalIndices[4*i+1] = normalIndex[i][1];
            appearance->normalIndices[4*i+2] = normalIndex[i][2];
            appearance->normalIndices[4*i+3] = -1;
        }
    } else { // 面の法線
        // 算出した面の法線(のvector:配列)を取得する
        const vector<vector3d>& normalsMesh = calculateNormal.getNormalsOfMesh();
        
        // 面の法線データ数を取得する
        size_t normalsMeshNum = normalsMesh.size();
        
        // 代入する法線，法線インデックスのvector(配列)サイズを指定する
        appearance->normals.length( normalsMeshNum * 3 );
        appearance->normalIndices.length( normalsMeshNum );
        
        for(size_t i = 0 ; i < normalsMeshNum ; ++i){
            // 法線ベクトルを正規化する
            vector3d normal(tvmet::normalize(normalsMesh[i]));
            // AppearanceInfo のメンバに代入する
            appearance->normals[3*i+0] = normal[0];
            appearance->normals[3*i+1] = normal[1];
            appearance->normals[3*i+2] = normal[2];
            appearance->normalIndices[i] = i;
        }
    }
}


/*!
  @if jp
  ShapeInfoにPrimitiveTypeを代入
  @endif
*/
void BodyInfo_impl::setShapeInfoType(ShapeInfo_var& shapeInfo, UniformedShape::ShapePrimitiveType type)
{
    switch(type) {

    case UniformedShape::S_BOX:
        shapeInfo->type = BOX;
        break;

    case UniformedShape::S_CONE:
        shapeInfo->type = CONE;
        break;

    case UniformedShape::S_CYLINDER:
        shapeInfo->type = CYLINDER;
        break;

    case UniformedShape::S_SPHERE:
        shapeInfo->type = SPHERE;
        break;

    case UniformedShape::S_INDEXED_FACE_SET:
    case UniformedShape::S_ELEVATION_GRID:
    case UniformedShape::S_EXTRUSION:
        shapeInfo->type = MESH;
        break;
    }
}


/*!
  @if jp
  textureノードが存在すれば，TextureInfoを生成，textures_ に追加する。
  なお，ImageTextureノードの場合は，PixelTextureに変換し TextureInfoを生成する。

  @return long TextureInfo(textures_)のインデックス，textureノードが存在しない場合は -1
  @endif
*/
int BodyInfo_impl::createTextureInfo(VrmlTexturePtr textureNode)
{
    int textureInfoIndex = -1;

    if(textureNode){

        VrmlPixelTexturePtr pixelTextureNode = dynamic_pointer_cast<VrmlPixelTexture>(textureNode);

        if(!pixelTextureNode){
            VrmlImageTexturePtr imageTextureNode = dynamic_pointer_cast<VrmlImageTexture>(textureNode);
            if(imageTextureNode){
                ImageConverter  converter;
                VrmlPixelTexture* tempTexture = new VrmlPixelTexture;
                if(converter.convert(*imageTextureNode, *tempTexture, getModelFileDirPath())){
                    pixelTextureNode = tempTexture;
                }
            }
        }

        if(pixelTextureNode){
            TextureInfo_var texture(new TextureInfo());

            texture->height = pixelTextureNode->image.height;
            texture->width = pixelTextureNode->image.width;
            texture->numComponents = pixelTextureNode->image.numComponents;
		
            size_t pixelsLength =  pixelTextureNode->image.pixels.size();
            texture->image.length( pixelsLength );
            for(size_t j = 0 ; j < pixelsLength ; j++ ){
                texture->image[j] = pixelTextureNode->image.pixels[j];
            }
            texture->repeatS = pixelTextureNode->repeatS;
            texture->repeatT = pixelTextureNode->repeatT;

            textureInfoIndex = textures_.length();
            textures_.length(textureInfoIndex + 1);
            textures_[textureInfoIndex] = texture;
	}
    }

    return textureInfoIndex;
}


/*!
  @if jp
  materialノードが存在すれば，MaterialInfoを生成，materials_に追加する。
  materials_に追加した位置(インデックス)を戻り値として返す。

  @return long MaterialInfo (materials_)のインデックス，materialノードが存在しない場合は -1
  @endif
*/
int BodyInfo_impl::createMaterialInfo(VrmlMaterialPtr materialNode)
{
    int materialInfoIndex = -1;

    if(materialNode){
        MaterialInfo_var material(new MaterialInfo());

        material->ambientIntensity = materialNode->ambientIntensity;
        material->shininess = materialNode->shininess;
        material->transparency = materialNode->transparency;

        for(int j = 0 ; j < 3 ; j++){
            material->diffuseColor[j] = materialNode->diffuseColor[j];
            material->emissiveColor[j] = materialNode->emissiveColor[j];
            material->specularColor[j] = materialNode->specularColor[j];
        }

        // materials_に追加する
        materialInfoIndex = materials_.length();
        materials_.length(materialInfoIndex + 1 );
        materials_[materialInfoIndex] = material;

    }

    return materialInfoIndex;
}


/*!
  @if jp
  transformノードで指定されたrotation,translation,scaleを計算し，4x4行列に代入する。
  計算結果は第2引数に代入する。
  @endif
*/
void BodyInfo_impl::calcTransform(VrmlTransformPtr transform, matrix44d& out_matrix)
{
    // rotationロドリゲスの回転軸
    vector3d axis(transform->rotation[0], transform->rotation[1], transform->rotation[2]);

    // ロドリゲスrotationを3x3行列に変換する
    matrix33d mRotation;
    rodrigues(mRotation, axis, transform->rotation[3]);

    // rotation, translation を4x4行列に代入する
    matrix44d mTransform;
    mTransform =
        mRotation(0,0), mRotation(0,1), mRotation(0,2), transform->translation[0],
        mRotation(1,0), mRotation(1,1), mRotation(1,2), transform->translation[1],
        mRotation(2,0), mRotation(2,1), mRotation(2,2), transform->translation[2],
        0.0,            0.0,            0.0,		    1.0;


    // ScaleOrientation
    vector3d scaleOrientation(transform->scaleOrientation[0], transform->scaleOrientation[1], transform->scaleOrientation[2]);

    // ScaleOrientationを3x3行列に変換する
    matrix33d mSO;
    rodrigues(mSO, scaleOrientation, transform->scaleOrientation[3]);

    // スケーリング中心 平行移動
    matrix44d mTranslation;
    mTranslation =
        1.0, 0.0, 0.0, transform->center[0],
        0.0, 1.0, 0.0, transform->center[1],
        0.0, 0.0, 1.0, transform->center[2],
        0.0, 0.0, 0.0, 1.0;

    // スケーリング中心 逆平行移動
    matrix44d mTranslationInv;
    mTranslationInv =
        1.0, 0.0, 0.0, -transform->center[0],
        0.0, 1.0, 0.0, -transform->center[1],
        0.0, 0.0, 1.0, -transform->center[2],
        0.0, 0.0, 0.0, 1.0;

    // ScaleOrientation 回転
    matrix44d mScaleOrientation;
    mScaleOrientation =
        mSO(0,0), mSO(0,1), mSO(0,2), 0,
        mSO(1,0), mSO(1,1), mSO(1,2), 0,
        mSO(2,0), mSO(2,1), mSO(2,2), 0,
        0,        0,        0,        1;

    // スケール(拡大・縮小率)
    matrix44d mScale;
    mScale =
        transform->scale[0],    0.0,                    0.0,                    0.0,
        0.0,                    transform->scale[1],    0.0,                    0.0,
        0.0,                    0.0,                    transform->scale[2],    0.0,
        0.0,                    0.0,                    0.0,                    1.0;

    // ScaleOrientation 逆回転
    matrix44d mScaleOrientationInv;
    mScaleOrientationInv =
	mSO(0,0), mSO(1,0), mSO(2,0), 0,
        mSO(0,1), mSO(1,1), mSO(2,1), 0,
        mSO(0,2), mSO(1,2), mSO(2,2), 0,
        0,        0,        0,        1; 

    // transform, scale, scaleOrientation で設定された回転・並進成分を合成する
    out_matrix = mTransform * mScaleOrientation * mTranslationInv * mScale * mTranslation * mScaleOrientationInv;
}


/*!
  @if jp
  @note url_のパスからURLスキーム，ファイル名を除去したディレクトリパス文字列を返す。
  @todo boost::filesystem で実装しなおす
  @return string ModelFile(.wrl)のディレクトリパス文字列
  @endif
*/
string BodyInfo_impl::getModelFileDirPath()
{
    // BodyInfo::url_ から URLスキームを削除する
    string filepath = deleteURLScheme( url_ );

    // '/' または '\' の最後の位置を取得する
    size_t pos = filepath.find_last_of( "/\\" );

    string dirPath = "";

    // 存在すれば，
    if( pos != string::npos )
	{
            // ディレクトリパス文字列
            dirPath = filepath;
            dirPath.resize( pos + 1 );
	}

    return dirPath;
}

