/**
   @author Shin'ichiro Nakaoka
*/

#include "ColdetBody.h"
#include <hrpUtil/Tvmet4d.h>
#include <iostream>


ColdetBody::ColdetBody(BodyInfo_ptr bodyInfo)
{
    LinkInfoSequence_var links = bodyInfo->links();
    ShapeInfoSequence_var shapes = bodyInfo->shapes();

    int numLinks = links->length();

    linkColdetModels.resize(numLinks);
		
    for(int linkIndex = 0; linkIndex < numLinks ; ++linkIndex){

        LinkInfo& linkInfo = links[linkIndex];
			
        int totalNumVertices = 0;
        int totalNumTriangles = 0;
        const TransformedShapeIndexSequence& shapeIndices = linkInfo.shapeIndices;
	for(int i=0; i < shapeIndices.length(); i++){
            short shapeIndex = shapeIndices[i].shapeIndex;
            const ShapeInfo& shapeInfo = shapes[shapeIndex];
            totalNumVertices += shapeInfo.vertices.length() / 3;
            totalNumTriangles += shapeInfo.triangles.length() / 3;
        }

        ColdetModelPtr coldetModel(new ColdetModel());
        coldetModel->setNumVertices(totalNumVertices);
        coldetModel->setNumTriangles(totalNumTriangles);

        addLinkVerticesAndTriangles(coldetModel, linkInfo, shapes);

        coldetModel->update();

        linkColdetModels[linkIndex] = coldetModel;
        linkNameToColdetModelMap.insert(make_pair(linkInfo.name, coldetModel));

        cout << linkInfo.name << " has "<< totalNumTriangles << " triangles." << endl;
    }
}


void ColdetBody::addLinkVerticesAndTriangles
(ColdetModelPtr& coldetModel, LinkInfo& linkInfo, ShapeInfoSequence_var& shapes)
{
    int vertexIndex = 0;
    int triangleIndex = 0;

    const TransformedShapeIndexSequence& shapeIndices = linkInfo.shapeIndices;
    
    for(int i=0; i < shapeIndices.length(); i++){
        const TransformedShapeIndex& tsi = shapeIndices[i];
        short shapeIndex = tsi.shapeIndex;
        const DblArray12& M = tsi.transformMatrix;;
        Matrix44 T;
        T = M[0], M[1], M[2],  M[3],
            M[4], M[5], M[6],  M[7],
            M[8], M[9], M[10], M[11],
            0.0,  0.0,  0.0,   1.0;

        const ShapeInfo& shapeInfo = shapes[shapeIndex];

        const FloatSequence& vertices = shapeInfo.vertices;
        const int numVertices = vertices.length() / 3;
        for(int j=0; j < numVertices; ++j){
            Vector4 v(T * Vector4(vertices[j*3], vertices[j*3+1], vertices[j*3+2]));
            coldetModel->setVertex(vertexIndex++, v[0], v[1], v[2]);
        }

        const LongSequence& triangles = shapeInfo.triangles;
        const int numTriangles = triangles.length() / 3;

        for(int j=0; j < numTriangles; ++j){
            coldetModel->setTriangle(triangleIndex++, triangles[j*3], triangles[j*3+1], triangles[j*3+2]);
        }
    }
}


ColdetBody::ColdetBody(const ColdetBody& org)
{
    linkColdetModels = org.linkColdetModels;
    linkNameToColdetModelMap = org.linkNameToColdetModelMap;
}
