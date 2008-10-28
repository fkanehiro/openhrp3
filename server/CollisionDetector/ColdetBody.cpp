/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */
/**
   @author Shin'ichiro Nakaoka
*/

#include "ColdetBody.h"
#include <iostream>


ColdetBody::ColdetBody(BodyInfo_ptr bodyInfo)
{
    LinkInfoSequence_var links = bodyInfo->links();
    ShapeInfoSequence_var shapes = bodyInfo->shapes();

    int numLinks = links->length();

    linkColdetModels.resize(numLinks);
		
    for(int linkIndex = 0; linkIndex < numLinks ; ++linkIndex){

        LinkInfo& linkInfo = links[linkIndex];
			
        int totalNumTriangles = 0;
        const TransformedShapeIndexSequence& shapeIndices = linkInfo.shapeIndices;
	for(int i=0; i < shapeIndices.length(); i++){
            short shapeIndex = shapeIndices[i].shapeIndex;
            const ShapeInfo& shapeInfo = shapes[shapeIndex];
            totalNumTriangles += shapeInfo.triangles.length() / 3;
        }

        const SensorInfoSequence& sensors = linkInfo.sensors;
        for (unsigned int i=0; i<sensors.length(); i++){
            const SensorInfo &sinfo = sensors[i];
            const TransformedShapeIndexSequence tsis = sinfo.shapeIndices;
            for (unsigned int j=0; j<tsis.length(); j++){
                short shapeIndex = tsis[j].shapeIndex;
                const ShapeInfo& shapeInfo = shapes[shapeIndex];
                totalNumTriangles += shapeInfo.triangles.length() / 3;
            }
        }

        int totalNumVertices = totalNumTriangles * 3;

        ColdetModelPtr coldetModel(new ColdetModel());
        coldetModel->setName(linkInfo.name);
        
        if(totalNumTriangles > 0){
            coldetModel->setNumVertices(totalNumVertices);
            coldetModel->setNumTriangles(totalNumTriangles);
            addLinkVerticesAndTriangles(coldetModel, linkInfo, shapes);
            coldetModel->build();
        }

        linkColdetModels[linkIndex] = coldetModel;
        linkNameToColdetModelMap.insert(make_pair(linkInfo.name, coldetModel));

        cout << linkInfo.name << " has "<< totalNumTriangles << " triangles." << endl;
    }
}


void ColdetBody::addLinkVerticesAndTriangles
(ColdetModelPtr& coldetModel, const TransformedShapeIndex& tsi, const Matrix44& Tparent, ShapeInfoSequence_var& shapes, int& vertexIndex, int& triangleIndex)
{
    short shapeIndex = tsi.shapeIndex;
    const DblArray12& M = tsi.transformMatrix;;
    Matrix44 T, Tlocal;
    Tlocal = M[0], M[1], M[2],  M[3],
             M[4], M[5], M[6],  M[7],
             M[8], M[9], M[10], M[11],
             0.0,  0.0,  0.0,   1.0;
    T = Tparent * Tlocal;
    
    const ShapeInfo& shapeInfo = shapes[shapeIndex];
    
    const FloatSequence& vertices = shapeInfo.vertices;
    const LongSequence& triangles = shapeInfo.triangles;
    const int numTriangles = triangles.length() / 3;
    
    for(int j=0; j < numTriangles; ++j){
        int vertexIndexTop = vertexIndex;
        for(int k=0; k < 3; ++k){
            long orgVertexIndex = shapeInfo.triangles[j * 3 + k];
            int p = orgVertexIndex * 3;
            Vector4 v(T * Vector4(vertices[p+0], vertices[p+1], vertices[p+2], 1.0));
            coldetModel->setVertex(vertexIndex++, v[0], v[1], v[2]);
        }
        coldetModel->setTriangle(triangleIndex++, vertexIndexTop, vertexIndexTop + 1, vertexIndexTop + 2);
    }
    
}

void ColdetBody::addLinkVerticesAndTriangles
(ColdetModelPtr& coldetModel, LinkInfo& linkInfo, ShapeInfoSequence_var& shapes)
{
    int vertexIndex = 0;
    int triangleIndex = 0;

    const TransformedShapeIndexSequence& shapeIndices = linkInfo.shapeIndices;

    Matrix44 E(tvmet::identity<Matrix44>());
    for(int i=0; i < shapeIndices.length(); i++){
        addLinkVerticesAndTriangles(coldetModel, shapeIndices[i], E, shapes,
                                    vertexIndex, triangleIndex);
    }

    Matrix44 T(tvmet::identity<Matrix44>());
    const SensorInfoSequence& sensors = linkInfo.sensors;
    for (unsigned int i=0; i<sensors.length(); i++){
        const SensorInfo& sensor = sensors[i]; 
        calcRodrigues(T, Vector3(sensor.rotation[0], sensor.rotation[1], 
                                 sensor.rotation[2]), sensor.rotation[3]);
        T(0,3) = sensor.translation[0];
        T(1,3) = sensor.translation[1];
        T(2,3) = sensor.translation[2];
        const TransformedShapeIndexSequence& shapeIndices = sensor.shapeIndices;
        for (unsigned int j=0; j<shapeIndices.length(); j++){
            addLinkVerticesAndTriangles(coldetModel, shapeIndices[j], T, 
                                        shapes,
                                        vertexIndex, triangleIndex);
        }
    }
}


ColdetBody::ColdetBody(const ColdetBody& org)
{
    linkColdetModels = org.linkColdetModels;
    linkNameToColdetModelMap = org.linkNameToColdetModelMap;
}


void ColdetBody::setLinkPositions(const LinkPositionSequence& linkPositions)
{
    const int srcNumLinks = linkPositions.length();
    const int selfNumLinks = linkColdetModels.size();
    for(int i=0; i < srcNumLinks && i < selfNumLinks; ++i){
        const LinkPosition& linkPosition = linkPositions[i];
        if(linkColdetModels[i]){
            linkColdetModels[i]->setPosition(linkPosition.R, linkPosition.p);
        }
    }
}
