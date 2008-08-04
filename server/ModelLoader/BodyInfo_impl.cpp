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

#include <OpenHRP/Corba/ViewSimulator.h>
#include <OpenHRP/Parser/VrmlNodes.h>
#include <OpenHRP/Parser/ImageConverter.h>

#include "UtilFunctions.h"


using namespace std;
using namespace boost;
using namespace OpenHRP;


namespace {

    typedef map<string, string> SensorTypeMap;
    SensorTypeMap sensorTypeMap;
    
    bool operator != (const Matrix44& a, const Matrix44& b)
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
       @if jp
       @brief 文字列置換
       @return str 内の 特定文字列　sb を 別の文字列　sa に置換
       @endif
    */
    string& replace(string& str, const string sb, const string sa)
    {
        string::size_type n, nb = 0;
	
        while ((n = str.find(sb,nb)) != string::npos){
            str.replace(n,sb.size(),sa);
            nb = n + sa.size();
        }
	
        return str;
    }

    void putMessage(const std::string& message)
    {
        cout << message;
    }

}
    

BodyInfo_impl::BodyInfo_impl( PortableServer::POA_ptr poa ) :
    poa(PortableServer::POA::_duplicate( poa ))
{
    triangleMeshShaper.setNormalGenerationMode(true);
    triangleMeshShaper.sigMessage.connect(bind(&putMessage, _1));
    
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
            triangleMeshShaper.apply(modelNodeSet.humanoidNode());
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

        Vector3 axis(sensorInfo.rotation[0], sensorInfo.rotation[1], sensorInfo.rotation[2]);
        Matrix33 mRotation(rodrigues(axis, sensorInfo.rotation[3]));

        Matrix44 mTransform;
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
void BodyInfo_impl::traverseShapeNodes(int linkInfoIndex, MFNode& childNodes, const Matrix44& transform)
{
    LinkInfo& linkInfo = links_[linkInfoIndex];

    for(size_t i = 0; i < childNodes.size(); ++i) {
        VrmlNodePtr& node = childNodes[i];

        if(node->isCategoryOf(GROUPING_NODE)) {
            VrmlGroupPtr groupNode = static_pointer_cast<VrmlGroup>(node);
            VrmlTransformPtr transformNode = dynamic_pointer_cast<VrmlTransform>(groupNode);
            if(!transformNode){
                traverseShapeNodes(linkInfoIndex, groupNode->children, transform);
            } else {
                Matrix44 localTransform;
                calcTransform(transformNode, localTransform); // このノードで設定された transform (scaleも含む)
                traverseShapeNodes(linkInfoIndex, groupNode->children, Matrix44(transform * localTransform));
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


/*!
  @if jp
  transformノードで指定されたrotation,translation,scaleを計算し，4x4行列に代入する。
  計算結果は第2引数に代入する。
  @endif
*/
void BodyInfo_impl::calcTransform(VrmlTransformPtr transform, Matrix44& out_T)
{
    Matrix44 R;
    const SFRotation& r = transform->rotation;
    rodrigues(R, Vector3(r[0], r[1], r[2]), r[3]);

    const SFVec3f& center = transform->center;

    Matrix44 SR;
    const SFRotation& so = transform->scaleOrientation;
    rodrigues(SR, Vector3(so[0], so[1], so[2]), so[3]);

    const SFVec3f& s = transform->scale;

    Matrix44 SinvSR;
    SinvSR =
        s[0] * SR(0,0), s[0] * SR(1,0), s[0] * SR(2,0), 0.0,
        s[1] * SR(0,1), s[1] * SR(1,1), s[1] * SR(2,1), 0.0,
        s[2] * SR(0,2), s[2] * SR(1,2), s[2] * SR(2,2), 0.0,
        0.0,             0.0,           0.0,            1.0;
    
    const Vector4 c(center[0], center[1], center[2], 1.0);

    out_T = R * Matrix44(SR * SinvSR);

    const Vector4 c2(out_T * c);

    for(int i=0; i < 3; ++i){
        out_T(i, 3) -= c2(i);
    }

    for(int i=0; i < 3; ++i){
        out_T(i, 3) += transform->translation[i] + center[i];
    }
}


/**
   @return the index of a created ShapeInfo object. The return value is -1 if the creation fails.
*/
int BodyInfo_impl::createShapeInfo(VrmlShapePtr shapeNode, const Matrix44& transform)
{
    int shapeInfoIndex = -1;

    VrmlIndexedFaceSet* triangleMesh = dynamic_cast<VrmlIndexedFaceSet*>(shapeNode->geometry.get());

    if(triangleMesh){
    
        ShapeInfo_var shapeInfo(new ShapeInfo);

        setTriangleMesh(shapeInfo, triangleMesh, transform);

        setPrimitiveProperties(shapeInfo, shapeNode);
        
        shapeInfo->appearanceIndex = createAppearanceInfo(shapeInfo, shapeNode, triangleMesh, transform);
        
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


void BodyInfo_impl::setTriangleMesh(ShapeInfo_var& shapeInfo, VrmlIndexedFaceSet* triangleMesh, const Matrix44& transform)
{
    const MFVec3f& vertices = triangleMesh->coord->point;
    size_t numVertices = vertices.size();
    shapeInfo->vertices.length(numVertices * 3);

    size_t pos = 0;
    for(size_t i=0; i < numVertices; ++i){
        const SFVec3f& v = vertices[i];
        const Vector4 v4(v[0], v[1], v[2], 1.0);
        const Vector4 vdash(transform * v4);
        shapeInfo->vertices[pos++] = vdash[0];
        shapeInfo->vertices[pos++] = vdash[1];
        shapeInfo->vertices[pos++] = vdash[2];
    }

    const MFInt32& indices = triangleMesh->coordIndex;
    const size_t numTriangles = indices.size() / 4;
    shapeInfo->triangles.length(numTriangles * 3);
	
    int dpos = 0;
    int spos = 0;
    for(size_t i=0; i < numTriangles; ++i){
        shapeInfo->triangles[dpos++] = indices[spos++];
        shapeInfo->triangles[dpos++] = indices[spos++];
        shapeInfo->triangles[dpos++] = indices[spos++];
        spos++; // skip a terminater '-1'
    }
}


void BodyInfo_impl::setPrimitiveProperties(ShapeInfo_var& shapeInfo, VrmlShapePtr shapeNode)
{
    shapeInfo->primitiveType = SP_MESH;
    FloatSequence& param = shapeInfo->primitiveParameters;
    
    VrmlGeometry* originalGeometry = triangleMeshShaper.getOriginalGeometry(shapeNode).get();

    if(originalGeometry){

        VrmlIndexedFaceSet* faceSet = dynamic_cast<VrmlIndexedFaceSet*>(originalGeometry);

        if(!faceSet){
            
            if(VrmlBox* box = dynamic_cast<VrmlBox*>(originalGeometry)){
                shapeInfo->primitiveType = SP_BOX;
                param.length(3);
                for(int i=0; i < 3; ++i){
                    param[i] = box->size[i];
                }

            } else if(VrmlCone* cone = dynamic_cast<VrmlCone*>(originalGeometry)){
                shapeInfo->primitiveType = SP_CONE;
                param.length(4);
                param[0] = cone->bottomRadius;
                param[1] = cone->height;
                param[2] = cone->bottom ? 1.0 : 0.0;
                param[3] = cone->side ? 1.0 : 0.0;
                
            } else if(VrmlCylinder* cylinder = dynamic_cast<VrmlCylinder*>(originalGeometry)){
                shapeInfo->primitiveType = SP_CYLINDER;
                param.length(5);
                param[0] = cylinder->radius;
                param[1] = cylinder->height;
                param[2] = cylinder->top    ? 1.0 : 0.0;
                param[3] = cylinder->bottom ? 1.0 : 0.0;
                param[4] = cylinder->side   ? 1.0 : 0.0;
                
            
            } else if(VrmlSphere* sphere = dynamic_cast<VrmlSphere*>(originalGeometry)){
                shapeInfo->primitiveType = SP_SPHERE;
                param.length(1);
                param[0] = sphere->radius;
            }
        }
    }
}


/**
   @return the index of a created AppearanceInfo object. The return value is -1 if the creation fails.
*/
int BodyInfo_impl::createAppearanceInfo
(ShapeInfo_var& shapeInfo, VrmlShapePtr& shapeNode, VrmlIndexedFaceSet* faceSet, const Matrix44& transform)
{
    int appearanceIndex = -1;
    
    AppearanceInfo_var appInfo(new AppearanceInfo());

    appInfo->normalPerVertex = faceSet->normalPerVertex;
    appInfo->colorPerVertex = faceSet->colorPerVertex;
    appInfo->solid = faceSet->solid;
    appInfo->creaseAngle = faceSet->creaseAngle;
    appInfo->materialIndex = -1;
    appInfo->textureIndex = -1;

    if(faceSet->color){
        setColors(appInfo, faceSet);
    }

    if(faceSet->normal){
        setNormals(appInfo, faceSet, transform);
    }
    
    VrmlAppearancePtr& appNode = shapeNode->appearance;

    if(appNode) {
        // todo
        //appInfo->textureCoordinate = faceSet->texCood;
        
        appInfo->materialIndex = createMaterialInfo(appNode->material);
        appInfo->textureIndex  = createTextureInfo (appNode->texture);
    }

    appearanceIndex = appearances_.length();
    appearances_.length(appearanceIndex + 1);
    appearances_[appearanceIndex] = appInfo;

    return appearanceIndex;
}


void BodyInfo_impl::setColors(AppearanceInfo_var& appInfo, VrmlIndexedFaceSet* triangleMesh)
{
    const MFColor& colors = triangleMesh->color->color;
    int numColors = colors.size();
    appInfo->colors.length(numColors * 3);

    int pos = 0;
    for(int i=0; i < numColors; ++i){
        const SFColor& color = colors[i];
        for(int j=0; j < 3; ++j){
            appInfo->colors[pos++] = color[j];
        }
    }

    const MFInt32& orgIndices = triangleMesh->colorIndex;
    const int numOrgIndices = orgIndices.size();
    if(numOrgIndices > 0){
        if(triangleMesh->colorPerVertex){
            const int numTriangles = numOrgIndices / 4; // considering delimiter element '-1'
            appInfo->colorIndices.length(numTriangles * 3);
            int dpos = 0;
            int spos = 0;
            for(int i=0; i < numTriangles; ++i){
                appInfo->colorIndices[dpos++] = orgIndices[spos++];
                appInfo->colorIndices[dpos++] = orgIndices[spos++];
                appInfo->colorIndices[dpos++] = orgIndices[spos++];
                spos++; // skip delimiter '-1'
            }
        } else { // color per face
            appInfo->colorIndices.length(numOrgIndices);
            for(int i=0; i < numOrgIndices; ++i){
                appInfo->colorIndices[i] = orgIndices[i];
            }
        }
    }
}


void BodyInfo_impl::setNormals(AppearanceInfo_var& appInfo, VrmlIndexedFaceSet* triangleMesh, const Matrix44& transform)
{
    const MFVec3f& normals = triangleMesh->normal->vector;
    int numNormals = normals.size();

    appInfo->normals.length(numNormals * 3);

    int pos = 0;
    for(int i=0; i < numNormals; ++i){
        const SFVec3f& v = normals[i];
        const Vector4 v4(v[0], v[1], v[2], 1.0);
        const Vector4 vdash(transform * v4);
        for(int j=0; j < 3; ++j){
            appInfo->normals[pos++] = vdash[j];
        }
    }

    const MFInt32& orgIndices = triangleMesh->normalIndex;
    const int numOrgIndices = orgIndices.size();
    if(numOrgIndices > 0){
        if(triangleMesh->normalPerVertex){
            const int numTriangles = numOrgIndices / 4; // considering delimiter element '-1'
            appInfo->normalIndices.length(numTriangles * 3);
            int dpos = 0;
            int spos = 0;
            for(int i=0; i < numTriangles; ++i){
                appInfo->normalIndices[dpos++] = orgIndices[spos++];
                appInfo->normalIndices[dpos++] = orgIndices[spos++];
                appInfo->normalIndices[dpos++] = orgIndices[spos++];
                spos++; // skip delimiter '-1'
            }
        } else { // normal per face
            appInfo->normalIndices.length(numOrgIndices);
            for(int i=0; i < numOrgIndices; ++i){
                appInfo->normalIndices[i] = orgIndices[i];
            }
        }
    }
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
            material->diffuseColor[j]  = materialNode->diffuseColor[j];
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
    if(pos != string::npos){
        // ディレクトリパス文字列
        dirPath = filepath;
        dirPath.resize(pos + 1);
    }

    return dirPath;
}
