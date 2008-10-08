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
   @author Ergovision
*/

#include "ModelLoaderUtil.h"

#include <stack>
#include <hrpUtil/Tvmet3d.h>
#include <hrpUtil/Tvmet4d.h>
#include <hrpCorba/OpenHRPCommon.hh>
#include <hrpCollision/ColdetModel.h>

#include "Link.h"
#include "Sensor.h"

using namespace std;
using namespace hrp;
using namespace OpenHRP;


namespace {

    const bool debugMode = false;

    ostream& operator<<(ostream& os, DblSequence_var& data)
    {
        int size = data->length();
        for(int i=0; i < size-1; ++i){
            cout << data[i] << ", ";
        }
        cout << data[size-1];

        return os;
    }


    ostream& operator<<(ostream& os, DblArray3_var& data)
    {
        cout << data[CORBA::ULong(0)] << ", " << data[CORBA::ULong(1)] << ", " << data[CORBA::ULong(2)];
        return os;
    }


    ostream& operator<<(ostream& os, DblArray9_var& data)
    {
        for(CORBA::ULong i=0; i < 8; ++i){
            cout << data[i] << ", ";
        }
        cout << data[CORBA::ULong(9)];
        return os;
    }


    void dumpBodyInfo(BodyInfo_ptr bodyInfo)
    {
        cout << "<<< CharacterInfo >>>\n";

        CORBA::String_var charaName = bodyInfo->name();

        cout << "name: " << charaName << "\n";

        LinkInfoSequence_var linkInfoSeq = bodyInfo->links();

        int numLinks = linkInfoSeq->length();
        cout << "num links: " << numLinks << "\n";

        for(int i=0; i < numLinks; ++i){

            const LinkInfo& linkInfo = linkInfoSeq[i];
            CORBA::String_var linkName = linkInfo.name;

            cout << "<<< LinkInfo: " << linkName << " (index " << i << ") >>>\n";
            cout << "parentIndex: " << linkInfo.parentIndex << "\n";

            const ShortSequence& childIndices = linkInfo.childIndices;
            if(childIndices.length() > 0){
                cout << "childIndices: ";
                for(CORBA::ULong i=0; i < childIndices.length(); ++i){
                    cout << childIndices[i] << " ";
                }
                cout << "\n";
            }

            const SensorInfoSequence& sensorInfoSeq = linkInfo.sensors;

            int numSensors = sensorInfoSeq.length();
            cout << "num sensors: " << numSensors << "\n";

            for(int j=0; j < numSensors; ++j){
                cout << "<<< SensorInfo >>>\n";
                const SensorInfo& sensorInfo = sensorInfoSeq[j];
                cout << "id: " << sensorInfo.id << "\n";
                cout << "type: " << sensorInfo.type << "\n";

                CORBA::String_var sensorName = sensorInfo.name;
                cout << "name: \"" << sensorName << "\"\n";

                const DblArray3& p = sensorInfo.translation;
                cout << "translation: " << p[0] << ", " << p[1] << ", " << p[2] << "\n";

                const DblArray4& r = sensorInfo.rotation;
                cout << "rotation: " << r[0] << ", " << r[1] << ", " << r[2] << ", " << r[3] << "\n";
			
            }
        }

        cout.flush();
    }

    inline double getLimitValue(DblSequence limitseq, double defaultValue)
    {
        return (limitseq.length() == 0) ? defaultValue : limitseq[0];
    }

    class ModelLoaderHelper
    {
    public:
        ModelLoaderHelper() {
            collisionDetectionModelLoading = false;
        }

        void enableCollisionDetectionModelLoading(bool isEnabled) {
            collisionDetectionModelLoading = isEnabled;
        };

        BodyPtr createBody(BodyInfo_ptr bodyInfo);

    private:

        BodyPtr body;
        LinkInfoSequence_var linkInfoSeq;
        ShapeInfoSequence_var shapeInfoSeq;
        bool collisionDetectionModelLoading;

        Link* createLink(int index, const Matrix33& parentRs);
        void createSensors(Link* link, const SensorInfoSequence& sensorInfoSeq, const Matrix33& Rs);
        void createColdetModel(Link* link, const LinkInfo& linkInfo);
        void addLinkVerticesAndTriangles(ColdetModelPtr& coldetModel, const LinkInfo& linkInfo);
    };
}


BodyPtr ModelLoaderHelper::createBody(BodyInfo_ptr bodyInfo)
{
    if(debugMode){
        dumpBodyInfo(bodyInfo);
    }
	
    body = new Body();

    CORBA::String_var name = bodyInfo->name();
    body->modelName = name;

    int n = bodyInfo->links()->length();
    linkInfoSeq = bodyInfo->links();
    shapeInfoSeq = bodyInfo->shapes();

    int rootIndex = -1;

    for(int i=0; i < n; ++i){
        if(linkInfoSeq[i].parentIndex < 0){
            if(rootIndex < 0){
                rootIndex = i;
            } else {
                body = 0; // more than one root !
            }
        }
    }
    if(rootIndex < 0){
        body = 0; // no root !
    }

    if(body){
        Matrix33 Rs(tvmet::identity<Matrix33>());
        Link* rootLink = createLink(rootIndex, Rs);
        body->setRootLink(rootLink);
        body->setDefaultRootPosition(rootLink->b, rootLink->Rs);

        body->installCustomizer();
        body->initializeConfiguration();
    }

    return body;
}


Link* ModelLoaderHelper::createLink(int index, const Matrix33& parentRs)
{
    const LinkInfo& linkInfo = linkInfoSeq[index];
    int jointId = linkInfo.jointId;
        
    Link* link = new Link();
        
    CORBA::String_var name0 = linkInfo.name;
    link->name = string( name0 );
    link->jointId = jointId;
        
    Vector3 relPos(linkInfo.translation[0], linkInfo.translation[1], linkInfo.translation[2]);
    link->b = parentRs * relPos;

    Vector3 rotAxis(linkInfo.rotation[0], linkInfo.rotation[1], linkInfo.rotation[2]);
    Matrix33 R = rodrigues(rotAxis, linkInfo.rotation[3]);
    link->Rs = (parentRs * R);
    const Matrix33& Rs = link->Rs;

    CORBA::String_var jointType = linkInfo.jointType;
    const std::string jt( jointType );

    if(jt == "fixed" ){
        link->jointType = Link::FIXED_JOINT;
    } else if(jt == "free" ){
        link->jointType = Link::FREE_JOINT;
    } else if(jt == "rotate" ){
        link->jointType = Link::ROTATIONAL_JOINT;
    } else if(jt == "slide" ){
        link->jointType = Link::SLIDE_JOINT;
    } else {
        link->jointType = Link::FREE_JOINT;
    }

    if(jointId < 0){
        if(link->jointType == Link::ROTATIONAL_JOINT || link->jointType == Link::SLIDE_JOINT){
            std::cerr << "Warning:  Joint ID is not given to joint " << link->name
                      << " of model " << body->modelName << "." << std::endl;
        }
    }

    link->a = 0.0;
    link->d = 0.0;

    Vector3 axis( Rs * Vector3(linkInfo.jointAxis[0], linkInfo.jointAxis[1], linkInfo.jointAxis[2]));

    if(link->jointType == Link::ROTATIONAL_JOINT){
        link->a = axis;
    } else if(link->jointType == Link::SLIDE_JOINT){
        link->d = axis;
    }

    link->m  = linkInfo.mass;
    link->Ir = linkInfo.rotorInertia;

    link->gearRatio     = linkInfo.gearRatio;
    link->rotorResistor = linkInfo.rotorResistor;
    link->torqueConst   = linkInfo.torqueConst;
    link->encoderPulse  = linkInfo.encoderPulse;

    link->Jm2 = link->Ir * link->gearRatio * link->gearRatio;

    DblSequence ulimit  = linkInfo.ulimit;
    DblSequence llimit  = linkInfo.llimit;
    DblSequence uvlimit = linkInfo.uvlimit;
    DblSequence lvlimit = linkInfo.lvlimit;

    double maxlimit = numeric_limits<double>::max();

    link->ulimit  = getLimitValue(ulimit,  +maxlimit);
    link->llimit  = getLimitValue(llimit,  -maxlimit);
    link->uvlimit = getLimitValue(uvlimit, +maxlimit);
    link->lvlimit = getLimitValue(lvlimit, -maxlimit);

    link->c = Rs * Vector3(linkInfo.centerOfMass[0], linkInfo.centerOfMass[1], linkInfo.centerOfMass[2]);

    Matrix33 Io;
    getMatrix33FromRowMajorArray(Io, linkInfo.inertia);
    link->I = Rs * Io;

    // a stack is used for keeping the same order of children
    std::stack<Link*> children;
	
    //##### [Changed] Link Structure (convert NaryTree to BinaryTree).
    int childNum = linkInfo.childIndices.length();
    for(int i = 0 ; i < childNum ; i++) {
        int childIndex = linkInfo.childIndices[i];
        Link* childLink = createLink(childIndex, Rs);
        if(childLink) {
            children.push(childLink);
        }
    }
    while(!children.empty()){
        link->addChild(children.top());
        children.pop();
    }
        
    createSensors(link, linkInfo.sensors, Rs);

    if(collisionDetectionModelLoading){
        createColdetModel(link, linkInfo);
    }

    return link;
}


void ModelLoaderHelper::createSensors(Link* link, const SensorInfoSequence& sensorInfoSeq, const Matrix33& Rs)
{
    int numSensors = sensorInfoSeq.length();

    for(int i=0 ; i < numSensors ; ++i ) {
        const SensorInfo& sensorInfo = sensorInfoSeq[i];

        int id = sensorInfo.id;
        if(id < 0) {
            std::cerr << "Warning:  sensor ID is not given to sensor " << sensorInfo.name
                      << "of model " << body->modelName << "." << std::endl;
        } else {
            int sensorType = Sensor::COMMON;

            CORBA::String_var type0 = sensorInfo.type;
            string type(type0);

            if(type == "Force")             { sensorType = Sensor::FORCE; }
            else if(type == "RateGyro")     { sensorType = Sensor::RATE_GYRO; }
            else if(type == "Acceleration")	{ sensorType = Sensor::ACCELERATION; }
            else if(type == "Vision")       { sensorType = Sensor::VISION; }

            CORBA::String_var name0 = sensorInfo.name;
            string name(name0);

            Sensor* sensor = body->createSensor(link, sensorType, id, name);

            if(sensor) {
                const DblArray3& p = sensorInfo.translation;
                sensor->localPos = Rs * Vector3(p[0], p[1], p[2]);

                const Vector3 axis(sensorInfo.rotation[0], sensorInfo.rotation[1], sensorInfo.rotation[2]);
                const Matrix33 R(rodrigues(axis, sensorInfo.rotation[3]));
                sensor->localR = Rs * R;
            }
        }
    }
}


#if 0
void ModelLoaderHelper::createColdetModel(Link* link, const LinkInfo& linkInfo)
{
    int totalNumVertices = 0;
    int totalNumTriangles = 0;
    const TransformedShapeIndexSequence& shapeIndices = linkInfo.shapeIndices;
    for(int i=0; i < shapeIndices.length(); i++){
        short shapeIndex = shapeIndices[i].shapeIndex;
        const ShapeInfo& shapeInfo = shapeInfoSeq[shapeIndex];
        totalNumVertices += shapeInfo.vertices.length() / 3;
        totalNumTriangles += shapeInfo.triangles.length() / 3;
    }

    ColdetModelPtr coldetModel(new ColdetModel());
    coldetModel->setName(linkInfo.name);
    if(totalNumTriangles > 0){
        coldetModel->setNumVertices(totalNumVertices);
        coldetModel->setNumTriangles(totalNumTriangles);
        addLinkVerticesAndTriangles(coldetModel, linkInfo);
        coldetModel->build();
    }
    link->coldetModel = coldetModel;
}


void ModelLoaderHelper::addLinkVerticesAndTriangles(ColdetModelPtr& coldetModel, const LinkInfo& linkInfo)
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

        const ShapeInfo& shapeInfo = shapeInfoSeq[shapeIndex];

        const FloatSequence& vertices = shapeInfo.vertices;
        const int numVertices = vertices.length() / 3;
        for(int j=0; j < numVertices; ++j){
            Vector4 v(T * Vector4(vertices[j*3], vertices[j*3+1], vertices[j*3+2], 1.0));
            coldetModel->setVertex(vertexIndex++, v[0], v[1], v[2]);
        }

        const LongSequence& triangles = shapeInfo.triangles;
        const int numTriangles = triangles.length() / 3;

        for(int j=0; j < numTriangles; ++j){
            coldetModel->setTriangle(triangleIndex++, triangles[j*3], triangles[j*3+1], triangles[j*3+2]);
        }
    }
}
#else
// duplicated vertices version
void ModelLoaderHelper::createColdetModel(Link* link, const LinkInfo& linkInfo)
{
    int totalNumTriangles = 0;
    const TransformedShapeIndexSequence& shapeIndices = linkInfo.shapeIndices;
    for(int i=0; i < shapeIndices.length(); i++){
        short shapeIndex = shapeIndices[i].shapeIndex;
        const ShapeInfo& shapeInfo = shapeInfoSeq[shapeIndex];
        totalNumTriangles += shapeInfo.triangles.length() / 3;
    }
    int totalNumVertices = totalNumTriangles * 3;

    ColdetModelPtr coldetModel(new ColdetModel());
    coldetModel->setName(linkInfo.name);
    if(totalNumTriangles > 0){
        coldetModel->setNumVertices(totalNumVertices);
        coldetModel->setNumTriangles(totalNumTriangles);
        addLinkVerticesAndTriangles(coldetModel, linkInfo);
        coldetModel->build();
    }
    link->coldetModel = coldetModel;
}


void ModelLoaderHelper::addLinkVerticesAndTriangles(ColdetModelPtr& coldetModel, const LinkInfo& linkInfo)
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

        const ShapeInfo& shapeInfo = shapeInfoSeq[shapeIndex];

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
}
#endif


BodyPtr hrp::loadBodyFromBodyInfo(OpenHRP::BodyInfo_ptr bodyInfo, bool loadGeometryForCollisionDetection)
{
    BodyPtr body;
    if(!CORBA::is_nil(bodyInfo)){
        ModelLoaderHelper helper;
        if(loadGeometryForCollisionDetection){
            helper.enableCollisionDetectionModelLoading(true);
        }
        body = helper.createBody(bodyInfo);
    }
    return body;
}


BodyPtr hrp::loadBodyFromModelLoader(const char *url, CosNaming::NamingContext_var cxt)
{
    BodyPtr body;
    
    CosNaming::Name ncName;
    ncName.length(1);
    ncName[0].id = CORBA::string_dup("ModelLoader");
    ncName[0].kind = CORBA::string_dup("");
    ModelLoader_var modelLoader = NULL;
    try {
        modelLoader = ModelLoader::_narrow(cxt->resolve(ncName));
    } catch(const CosNaming::NamingContext::NotFound &exc) {
        std::cerr << "ModelLoader not found: ";
        switch(exc.why) {
        case CosNaming::NamingContext::missing_node:
            std::cerr << "Missing Node" << std::endl;
        case CosNaming::NamingContext::not_context:
            std::cerr << "Not Context" << std::endl;
            break;
        case CosNaming::NamingContext::not_object:
            std::cerr << "Not Object" << std::endl;
            break;
        }
        return body;
    } catch(CosNaming::NamingContext::CannotProceed &exc) {
        std::cerr << "Resolve ModelLoader CannotProceed" << std::endl;
        return body;
    } catch(CosNaming::NamingContext::AlreadyBound &exc) {
        std::cerr << "Resolve ModelLoader InvalidName" << std::endl;
        return body;
    }

    BodyInfo_var bodyInfo;
    try {        
        bodyInfo = modelLoader->getBodyInfo(url);
    } catch(CORBA::SystemException& ex) {
        std::cerr << "CORBA::SystemException raised by ModelLoader: " << ex._rep_id() << std::endl;
    } catch(ModelLoader::ModelLoaderException& ex){
        std::cerr << "ModelLoaderException : " << ex.description << std::endl;
    }

    if(!CORBA::is_nil(bodyInfo)){
        ModelLoaderHelper helper;
        body = helper.createBody(bodyInfo);
    }

    return body;
}


BodyPtr hrp::loadBodyFromModelLoader(const char *url, CORBA_ORB_var orb)
{
    CosNaming::NamingContext_var cxt;
    try {
        CORBA::Object_var nS = orb->resolve_initial_references("NameService");
        cxt = CosNaming::NamingContext::_narrow(nS);
    } catch(CORBA::SystemException& ex) {
        std::cerr << "NameService doesn't exist" << std::endl;
        return BodyPtr();
    }

    return loadBodyFromModelLoader(url, cxt);
}


BodyPtr hrp::loadBodyFromModelLoader(const char *url, int argc, char *argv[])
{
    CORBA::ORB_var orb = CORBA::ORB_init(argc, argv);
    return loadBodyFromModelLoader(url, orb);
}


BodyPtr hrp::loadBodyFromModelLoader(const char *URL, istringstream &strm)
{
    vector<string> argvec;
    while (!strm.eof()){
        string arg;
        strm >> arg;
        argvec.push_back(arg);
    }
    int argc = argvec.size();
    char **argv = new char *[argc];
    for (int i=0; i<argc; i++){
        argv[i] = (char *)argvec[i].c_str();
    }

    BodyPtr body = loadBodyFromModelLoader(URL, argc, argv);

    delete [] argv;

    return body;
}
