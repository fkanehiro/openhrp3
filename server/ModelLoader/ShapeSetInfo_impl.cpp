/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

/*!
  @file ShapeSetInfo_impl.cpp
  @author Shin'ichiro Nakaoka
*/

#include "ShapeSetInfo_impl.h"

#include <map>
#include <vector>
#include <iostream>
#include <boost/bind.hpp>
#include <sys/stat.h>

#include <hrpCorba/ViewSimulator.hh>
#include <hrpUtil/VrmlNodes.h>
#include <hrpUtil/ImageConverter.h>

#include "VrmlUtil.h"



using namespace std;
using namespace boost;

    

ShapeSetInfo_impl::ShapeSetInfo_impl(PortableServer::POA_ptr poa) :
    poa(PortableServer::POA::_duplicate(poa))
{
    triangleMeshShaper.setNormalGenerationMode(true);
    triangleMeshShaper.sigMessage.connect(boost::bind(&putMessage, _1));
}


ShapeSetInfo_impl::~ShapeSetInfo_impl()
{
    
}


PortableServer::POA_ptr ShapeSetInfo_impl::_default_POA()
{
    return PortableServer::POA::_duplicate(poa);
}


void ShapeSetInfo_impl::applyTriangleMeshShaper(VrmlNodePtr node)
{
    triangleMeshShaper.apply(node);
}


void ShapeSetInfo_impl::putMessage(const std::string& message)
{
    cout << message;
}


/**
   @if jp
   @brief 文字列置換
   @return str 内の 特定文字列　sb を 別の文字列　sa に置換
   @endif
*/
string& ShapeSetInfo_impl::replace(string& str, const string& sb, const string& sa)
{
    string::size_type n, nb = 0;
    
    while ((n = str.find(sb,nb)) != string::npos){
        str.replace(n,sb.size(),sa);
        nb = n + sa.size();
    }
    
    return str;
}


ShapeInfoSequence* ShapeSetInfo_impl::shapes()
{
    return new ShapeInfoSequence(shapes_);
}


AppearanceInfoSequence* ShapeSetInfo_impl::appearances()
{
    return new AppearanceInfoSequence(appearances_);
}


MaterialInfoSequence* ShapeSetInfo_impl::materials()
{
    return new MaterialInfoSequence(materials_);
}


TextureInfoSequence* ShapeSetInfo_impl::textures()
{
    return new TextureInfoSequence(textures_);
}


/*!
  @if jp
  Shape ノード探索のための再帰関数

  node以下のシーングラフをたどり、ShapeInfoを生成していく。
  生成したShapeInfoはshapes_に追加する。
  shapes_に追加した位置(index)を io_shapeIndicesに追加する。
  io_shapeIndices は、BodyInfo構築時は LinkInfoのshapeIndices になり、
  SceneInfo 構築時は SceneInfo のshapeIndicesになる。

  @endif
*/
void ShapeSetInfo_impl::traverseShapeNodes
(VrmlNode* node, const Matrix44& T, TransformedShapeIndexSequence& io_shapeIndices, DblArray12Sequence& inlinedShapeM, const SFString* url)
{
    static int inline_count=0;   
    SFString url_ = *url;
    if(node->isCategoryOf(PROTO_INSTANCE_NODE)){
        VrmlProtoInstance* protoInstance = static_cast<VrmlProtoInstance*>(node);
        if(protoInstance->actualNode){
            traverseShapeNodes(protoInstance->actualNode.get(), T, io_shapeIndices, inlinedShapeM, url);
        }

    } else if(node->isCategoryOf(GROUPING_NODE)) {
        VrmlGroup* groupNode = static_cast<VrmlGroup*>(node);
        VrmlTransform* transformNode = dynamic_cast<VrmlTransform*>(groupNode);
        VrmlInline* inlineNode = NULL;
        
        inlineNode = dynamic_cast<VrmlInline*>(groupNode);
        const Matrix44* pT;
        if(inlineNode){
            if(!inline_count){
                int inlinedShapeMIndex = inlinedShapeM.length();
                inlinedShapeM.length(inlinedShapeMIndex+1);
                int p = 0;
                for(int row=0; row < 3; ++row){
                    for(int col=0; col < 4; ++col){
                        inlinedShapeM[inlinedShapeMIndex][p++] = T(row, col);
                    }
                }  
                url_ = inlineNode->urls[0];
            }
            inline_count++;
            for( MFString::iterator ite = inlineNode->urls.begin(); ite != inlineNode->urls.end(); ++ite ){
                string filename(deleteURLScheme(*ite));
                struct stat statbuff;
                time_t mtime = 0;
                if( stat( filename.c_str(), &statbuff ) == 0 ){
                    mtime = statbuff.st_mtime;
                }
                fileTimeMap.insert(make_pair(filename, mtime));
            }
        }
        Matrix44 T2;
        if( transformNode ){
            Matrix44 Tlocal;
            calcTransformMatrix(transformNode, Tlocal);
            T2 = T * Tlocal;
            pT = &T2;
        } else {
            pT = &T;
        }

        VrmlSwitch* switchNode = dynamic_cast<VrmlSwitch*>(node);
        if(switchNode){
            int whichChoice = switchNode->whichChoice;
            if( whichChoice >= 0 && whichChoice < switchNode->countChildren() )
                traverseShapeNodes(switchNode->getChild(whichChoice), *pT, io_shapeIndices, inlinedShapeM, &url_);
        }else{
            for(size_t i=0; i < groupNode->countChildren(); ++i){
                traverseShapeNodes(groupNode->getChild(i), *pT, io_shapeIndices, inlinedShapeM, &url_);
            }
        }
        if(inlineNode)
            inline_count--;

        
    } else if(node->isCategoryOf(SHAPE_NODE)) {

        VrmlShape* shapeNode = static_cast<VrmlShape*>(node);
        short shapeInfoIndex;
        
        ShapeNodeToShapeInfoIndexMap::iterator p = shapeInfoIndexMap.find(shapeNode);
        if(p != shapeInfoIndexMap.end()){
            shapeInfoIndex = p->second;
        } else {
            shapeInfoIndex = createShapeInfo(shapeNode, url);
        }
        
        if(shapeInfoIndex >= 0){
            long length = io_shapeIndices.length();
            io_shapeIndices.length(length + 1);
            TransformedShapeIndex& tsi = io_shapeIndices[length];
            tsi.shapeIndex = shapeInfoIndex;
            int p = 0;
            for(int row=0; row < 3; ++row){
                for(int col=0; col < 4; ++col){
                    tsi.transformMatrix[p++] = T(row, col);
                }
            }
            if(inline_count)
                tsi.inlinedShapeTransformMatrixIndex=inlinedShapeM.length()-1;
            else
                tsi.inlinedShapeTransformMatrixIndex=-1;
        }
    }
}

/**
   @return the index of a created ShapeInfo object. The return value is -1 if the creation fails.
*/
int ShapeSetInfo_impl::createShapeInfo(VrmlShape* shapeNode, const SFString* url)
{
    int shapeInfoIndex = -1;

    VrmlIndexedFaceSet* triangleMesh = dynamic_node_cast<VrmlIndexedFaceSet>(shapeNode->geometry).get();

    if(triangleMesh){

        shapeInfoIndex = shapes_.length();
        shapes_.length(shapeInfoIndex + 1);
        ShapeInfo& shapeInfo = shapes_[shapeInfoIndex];

        if ( url ) shapeInfo.url = CORBA::string_dup( url->c_str() );
        setTriangleMesh(shapeInfo, triangleMesh);
        setPrimitiveProperties(shapeInfo, shapeNode);
        shapeInfo.appearanceIndex = createAppearanceInfo(shapeInfo, shapeNode, triangleMesh, url);
        shapeInfoIndexMap.insert(make_pair(shapeNode, shapeInfoIndex));
    }
        
    return shapeInfoIndex;
}


void ShapeSetInfo_impl::setTriangleMesh(ShapeInfo& shapeInfo, VrmlIndexedFaceSet* triangleMesh)
{
    const MFVec3f& vertices = triangleMesh->coord->point;
    size_t numVertices = vertices.size();
    shapeInfo.vertices.length(numVertices * 3);

    size_t pos = 0;
    for(size_t i=0; i < numVertices; ++i){
        const SFVec3f& v = vertices[i];
        shapeInfo.vertices[pos++] = v[0];
        shapeInfo.vertices[pos++] = v[1];
        shapeInfo.vertices[pos++] = v[2];
    }

    const MFInt32& indices = triangleMesh->coordIndex;
    const size_t numTriangles = indices.size() / 4;
    shapeInfo.triangles.length(numTriangles * 3);
	
    int dpos = 0;
    int spos = 0;
    for(size_t i=0; i < numTriangles; ++i){
        shapeInfo.triangles[dpos++] = indices[spos++];
        shapeInfo.triangles[dpos++] = indices[spos++];
        shapeInfo.triangles[dpos++] = indices[spos++];
        spos++; // skip a terminater '-1'
    }
}


void ShapeSetInfo_impl::setPrimitiveProperties(ShapeInfo& shapeInfo, VrmlShape* shapeNode)
{
    shapeInfo.primitiveType = SP_MESH;
    FloatSequence& param = shapeInfo.primitiveParameters;
    
    VrmlNode *node = triangleMeshShaper.getOriginalGeometry(shapeNode).get();
    VrmlGeometry* originalGeometry = dynamic_cast<VrmlGeometry *>(node);

    if(originalGeometry){

        VrmlIndexedFaceSet* faceSet = dynamic_cast<VrmlIndexedFaceSet*>(originalGeometry);

        if(!faceSet){
            
            if(VrmlBox* box = dynamic_cast<VrmlBox*>(originalGeometry)){
                shapeInfo.primitiveType = SP_BOX;
                param.length(3);
                for(int i=0; i < 3; ++i){
                    param[i] = box->size[i];
                }

            } else if(VrmlCone* cone = dynamic_cast<VrmlCone*>(originalGeometry)){
                shapeInfo.primitiveType = SP_CONE;
                param.length(4);
                param[0] = cone->bottomRadius;
                param[1] = cone->height;
                param[2] = cone->bottom ? 1.0 : 0.0;
                param[3] = cone->side ? 1.0 : 0.0;
                
            } else if(VrmlCylinder* cylinder = dynamic_cast<VrmlCylinder*>(originalGeometry)){
                shapeInfo.primitiveType = SP_CYLINDER;
                param.length(5);
                param[0] = cylinder->radius;
                param[1] = cylinder->height;
                param[2] = cylinder->top    ? 1.0 : 0.0;
                param[3] = cylinder->bottom ? 1.0 : 0.0;
                param[4] = cylinder->side   ? 1.0 : 0.0;
                
            
            } else if(VrmlSphere* sphere = dynamic_cast<VrmlSphere*>(originalGeometry)){
                shapeInfo.primitiveType = SP_SPHERE;
                param.length(1);
                param[0] = sphere->radius;
            }
        }
    }else{
    
        VrmlProtoInstance *protoInstance = dynamic_cast<VrmlProtoInstance *>(node);
        if (protoInstance && protoInstance->proto->protoName == "Plane"){
            VrmlBox *box = dynamic_cast<VrmlBox *>(protoInstance->actualNode.get());
            shapeInfo.primitiveType = SP_PLANE;
            param.length(3);
            for (int i=0; i<3; i++){
                param[i] = box->size[i];
            }
        }
    }
}


/**
   @return the index of a created AppearanceInfo object. The return value is -1 if the creation fails.
*/
int ShapeSetInfo_impl::createAppearanceInfo
(ShapeInfo& shapeInfo, VrmlShape* shapeNode, VrmlIndexedFaceSet* faceSet,
 const SFString *url)
{
    int appearanceIndex = appearances_.length();
    appearances_.length(appearanceIndex + 1);
    AppearanceInfo& appInfo = appearances_[appearanceIndex];

    appInfo.normalPerVertex = faceSet->normalPerVertex;
    appInfo.colorPerVertex = faceSet->colorPerVertex;
    appInfo.solid = faceSet->solid;
    appInfo.creaseAngle = faceSet->creaseAngle;
    appInfo.materialIndex = -1;
    appInfo.textureIndex = -1;

    if(faceSet->color){
        setColors(appInfo, faceSet);
    }

    if(faceSet->normal){
        setNormals(appInfo, faceSet);
    } 
    
    VrmlAppearancePtr& appNode = shapeNode->appearance;

    if(appNode) {
        appInfo.materialIndex = createMaterialInfo(appNode->material);
        appInfo.textureIndex  = createTextureInfo (appNode->texture, url);
		if(appInfo.textureIndex != -1 ){
			createTextureTransformMatrix(appInfo, appNode->textureTransform);
            if(!faceSet->texCoord){   // default Texture Mapping
                triangleMeshShaper.defaultTextureMapping(shapeNode);
            }
			setTexCoords(appInfo, faceSet);
		}
    }

    return appearanceIndex;
}


void ShapeSetInfo_impl::setColors(AppearanceInfo& appInfo, VrmlIndexedFaceSet* triangleMesh)
{
    const MFColor& colors = triangleMesh->color->color;
    int numColors = colors.size();
    appInfo.colors.length(numColors * 3);

    int pos = 0;
    for(int i=0; i < numColors; ++i){
        const SFColor& color = colors[i];
        for(int j=0; j < 3; ++j){
            appInfo.colors[pos++] = color[j];
        }
    }

    const MFInt32& orgIndices = triangleMesh->colorIndex;
    const int numOrgIndices = orgIndices.size();
    if(numOrgIndices > 0){
        if(triangleMesh->colorPerVertex){
            const int numTriangles = numOrgIndices / 4; // considering delimiter element '-1'
            appInfo.colorIndices.length(numTriangles * 3);
            int dpos = 0;
            int spos = 0;
            for(int i=0; i < numTriangles; ++i){
                appInfo.colorIndices[dpos++] = orgIndices[spos++];
                appInfo.colorIndices[dpos++] = orgIndices[spos++];
                appInfo.colorIndices[dpos++] = orgIndices[spos++];
                spos++; // skip delimiter '-1'
            }
        } else { // color per face
            appInfo.colorIndices.length(numOrgIndices);
            for(int i=0; i < numOrgIndices; ++i){
                appInfo.colorIndices[i] = orgIndices[i];
            }
        }
    }
}


void ShapeSetInfo_impl::setNormals(AppearanceInfo& appInfo, VrmlIndexedFaceSet* triangleMesh)
{
    const MFVec3f& normals = triangleMesh->normal->vector;
    int numNormals = normals.size();
    appInfo.normals.length(numNormals * 3);

    int pos = 0;
    for(int i=0; i < numNormals; ++i){
        const SFVec3f& n = normals[i];
        for(int j=0; j < 3; ++j){
            appInfo.normals[pos++] = n[j];
        }
    }

    const MFInt32& orgIndices = triangleMesh->normalIndex;
    const int numOrgIndices = orgIndices.size();
    if(numOrgIndices > 0){
        if(triangleMesh->normalPerVertex){
            const int numTriangles = numOrgIndices / 4; // considering delimiter element '-1'
            appInfo.normalIndices.length(numTriangles * 3);
            int dpos = 0;
            int spos = 0;
            for(int i=0; i < numTriangles; ++i){
                appInfo.normalIndices[dpos++] = orgIndices[spos++];
                appInfo.normalIndices[dpos++] = orgIndices[spos++];
                appInfo.normalIndices[dpos++] = orgIndices[spos++];
                spos++; // skip delimiter '-1'
            }
        } else { // normal per face
            appInfo.normalIndices.length(numOrgIndices);
            for(int i=0; i < numOrgIndices; ++i){
                appInfo.normalIndices[i] = orgIndices[i];
            }
        }
    }
}

void ShapeSetInfo_impl::setTexCoords(AppearanceInfo& appInfo, VrmlIndexedFaceSet* triangleMesh )
{
	int numCoords = triangleMesh->texCoord->point.size();
    appInfo.textureCoordinate.length(numCoords * 2);

    Matrix33 m;
    for(int i=0,k=0; i<3; i++)
        for(int j=0; j<3; j++)
             m(i,j) = appInfo.textransformMatrix[k++];
    
    for(int i=0, pos=0; i < numCoords; i++ ){
        Vector3 point(triangleMesh->texCoord->point[i][0], triangleMesh->texCoord->point[i][1], 1);
        Vector3 texCoordinate(m * point);
        appInfo.textureCoordinate[pos++] = texCoordinate(0);
        appInfo.textureCoordinate[pos++] = texCoordinate(1);
    }

    int numIndex = triangleMesh->texCoordIndex.size();
    if(numIndex > 0){
        appInfo.textureCoordIndices.length(numIndex * 3 / 4);
         for(int i=0, j=0; i < numIndex; i++){
            if(triangleMesh->texCoordIndex[i] != -1)
                appInfo.textureCoordIndices[j++] = triangleMesh->texCoordIndex[i];
        }
    }
}

void ShapeSetInfo_impl::createTextureTransformMatrix(AppearanceInfo& appInfo, VrmlTextureTransformPtr& textureTransform ){

    Matrix33 m;
    if(textureTransform){
        Matrix33 matCT;
        matCT << 1, 0, textureTransform->translation[0]+textureTransform->center[0],
                0, 1, textureTransform->translation[1]+textureTransform->center[1],
                0,0,1 ;
        Matrix33 matR;
        matR << cos(textureTransform->rotation), -sin(textureTransform->rotation), 0,
               sin(textureTransform->rotation), cos(textureTransform->rotation), 0,
               0,0,1;
        Matrix33 matCS;
        matCS << textureTransform->scale[0], 0, -textureTransform->center[0],
                0, textureTransform->scale[1], -textureTransform->center[1],
                0,0,1 ;
        
        m = matCS * matR * matCT;
    }else{
        m = Matrix33::Identity();
    }
 
    for(int i=0,k=0; i<3; i++)
        for(int j=0; j<3; j++)
            appInfo.textransformMatrix[k++] = m(i,j);
}

/*!
  @if jp
  materialノードが存在すれば，MaterialInfoを生成，materials_に追加する。
  materials_に追加した位置(インデックス)を戻り値として返す。

  @return long MaterialInfo (materials_)のインデックス，materialノードが存在しない場合は -1
  @endif
*/
int ShapeSetInfo_impl::createMaterialInfo(VrmlMaterialPtr& materialNode)
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

        // materials_に追加する //
        materialInfoIndex = materials_.length();
        materials_.length(materialInfoIndex + 1 );
        materials_[materialInfoIndex] = material;

    }

    return materialInfoIndex;
}


/*!
  @if jp
  textureノードが存在すれば，TextureInfoを生成，textures_ に追加する。
  なお，ImageTextureノードの場合は，画像のurlと、readImageフラグが真ならimageデータと両方を持つ。
　いまのところ、movieTextureノードには対応しいない。

  @return long TextureInfo(textures_)のインデックス，textureノードが存在しない場合は -1
  @endif
*/
int ShapeSetInfo_impl::createTextureInfo(VrmlTexturePtr& textureNode, const SFString *currentUrl)
{
    int textureInfoIndex = -1;

    if(textureNode){

        TextureInfo_var texture(new TextureInfo());
       
        VrmlPixelTexturePtr pixelTextureNode = dynamic_pointer_cast<VrmlPixelTexture>(textureNode);
        
        if(!pixelTextureNode){
            VrmlImageTexturePtr imageTextureNode = dynamic_pointer_cast<VrmlImageTexture>(textureNode);
            if(imageTextureNode){
                string url = setTexturefileUrl(getModelFileDirPath(*currentUrl), imageTextureNode->url);
                texture->url = CORBA::string_dup(url.c_str());
                texture->repeatS = imageTextureNode->repeatS;
                texture->repeatT = imageTextureNode->repeatT;
                if(readImage && !url.empty()){
                    ImageConverter  converter;
                    SFImage* image = converter.convert(url);          
                    texture->height = image->height;
                    texture->width = image->width;
                    texture->numComponents = image->numComponents;
                    unsigned long pixelsLength = image->pixels.size();
                    texture->image.length( pixelsLength );
                    for(unsigned long j = 0 ; j < pixelsLength ; j++ ){
                        texture->image[j] = image->pixels[j];
                    }
                }else{
                    texture->height = 0;
                    texture->width = 0;
                    texture->numComponents = 0;
                }
            }
        }else if(pixelTextureNode){
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
        }
        
        textureInfoIndex = textures_.length();
        textures_.length(textureInfoIndex + 1);
        textures_[textureInfoIndex] = texture;
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
std::string ShapeSetInfo_impl::getModelFileDirPath(const std::string& url)
{
    //  BodyInfo::url_ から URLスキームを削除する //
    string filepath = deleteURLScheme(url);

    //  '/' または '\' の最後の位置を取得する //
    size_t pos = filepath.find_last_of("/\\");

    string dirPath = "";

    //  存在すれば， //
    if(pos != string::npos){
        //  ディレクトリパス文字列 //
        dirPath = filepath;
        dirPath.resize(pos + 1);
    }

    return dirPath;
}

void ShapeSetInfo_impl::setColdetModel(ColdetModelPtr& coldetModel, TransformedShapeIndexSequence shapeIndices, const Matrix44& Tparent, int& vertexIndex, int& triangleIndex){
    for(unsigned int i=0; i < shapeIndices.length(); i++){
        setColdetModelTriangles(coldetModel, shapeIndices[i], Tparent, vertexIndex, triangleIndex);
    }
}

void ShapeSetInfo_impl::setColdetModelTriangles
(ColdetModelPtr& coldetModel, const TransformedShapeIndex& tsi, const Matrix44& Tparent, int& vertexIndex, int& triangleIndex)
{
    short shapeIndex = tsi.shapeIndex;
    const DblArray12& M = tsi.transformMatrix;;
    Matrix44 T, Tlocal;
    Tlocal << M[0], M[1], M[2],  M[3],
             M[4], M[5], M[6],  M[7],
             M[8], M[9], M[10], M[11],
             0.0,  0.0,  0.0,   1.0;
    T = Tparent * Tlocal;
    
    const ShapeInfo& shapeInfo = shapes_[shapeIndex];
    int vertexIndexBase = coldetModel->getNumVertices();
    const FloatSequence& vertices = shapeInfo.vertices;
    const int numVertices = vertices.length() / 3;
    coldetModel->setNumVertices(coldetModel->getNumVertices()+numVertices);
    for(int j=0; j < numVertices; ++j){
        Vector4 v(T * Vector4(vertices[j*3], vertices[j*3+1], vertices[j*3+2], 1.0));
        coldetModel->setVertex(vertexIndex++, v[0], v[1], v[2]);
    }
    const LongSequence& triangles = shapeInfo.triangles;
    const int numTriangles = triangles.length() / 3;
    coldetModel->setNumTriangles(coldetModel->getNumTriangles()+numTriangles);
    for(int j=0; j < numTriangles; ++j){
        int t0 = triangles[j*3] + vertexIndexBase;
        int t1 = triangles[j*3+1] + vertexIndexBase;
        int t2 = triangles[j*3+2] + vertexIndexBase;
        coldetModel->setTriangle( triangleIndex++, t0, t1, t2);
    }

}

void ShapeSetInfo_impl::saveOriginalData(){
    originShapes_ = shapes_;
    originAppearances_ = appearances_;
    originMaterials_ = materials_;
}

void ShapeSetInfo_impl::setBoundingBoxData(const Vector3& boxSize, int shapeIndex){
    VrmlBox* box = new VrmlBox();
    VrmlIndexedFaceSetPtr triangleMesh;
    box->size[0] = boxSize[0]*2;
    box->size[1] = boxSize[1]*2;
    box->size[2] = boxSize[2]*2;
    triangleMesh = new VrmlIndexedFaceSet();
    triangleMesh->coord = new VrmlCoordinate();
    triangleMeshShaper.convertBox(box, triangleMesh);
       
    shapes_.length(shapeIndex+1);
    ShapeInfo& shapeInfo = shapes_[shapeIndex];
    VrmlIndexedFaceSet* faceSet = triangleMesh.get();
    setTriangleMesh(shapeInfo, faceSet);
    shapeInfo.primitiveType = SP_BOX;
    FloatSequence& param = shapeInfo.primitiveParameters;
    param.length(3);
    for(int i=0; i < 3; ++i)
        param[i] = box->size[i];
    shapeInfo.appearanceIndex = 0;
    
    delete box;
}

void ShapeSetInfo_impl::createAppearanceInfo(){
    appearances_.length(1);
    AppearanceInfo& appInfo = appearances_[0];
    appInfo.normalPerVertex = false;
    appInfo.colorPerVertex = false;
    appInfo.solid = false;
    appInfo.creaseAngle = 0.0;
    appInfo.textureIndex = -1;
    appInfo.normals.length(0);
    appInfo.normalIndices.length(0);
    appInfo.colors.length(0);
    appInfo.colorIndices.length(0);
    appInfo.materialIndex = 0;
    appInfo.textureIndex = -1;

    materials_.length(1);
    MaterialInfo& material = materials_[0];
    material.ambientIntensity = 0.2;
    material.shininess = 0.2;
    material.transparency = 0.0;
    for(int j = 0 ; j < 3 ; j++){
        material.diffuseColor[j]  = 0.8;
        material.emissiveColor[j] = 0.0;
        material.specularColor[j] = 0.0;
    }
}

void ShapeSetInfo_impl::restoreOriginalData(){
    shapes_ = originShapes_;
    appearances_ = originAppearances_;
    materials_ = originMaterials_;
}

bool ShapeSetInfo_impl::checkFileUpdateTime(){
    bool ret=true;
    for( map<std::string, time_t>::iterator it=fileTimeMap.begin(); it != fileTimeMap.end(); ++it){
        struct stat statbuff;
        time_t mtime = 0;
        if( stat( it->first.c_str(), &statbuff ) == 0 ){
            mtime = statbuff.st_mtime;
        }  
        if(it->second!=mtime){
            ret=false;
            break;
        }
    }
    return ret;
}
