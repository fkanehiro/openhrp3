/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

/**
   @author Shin'ichiro Nakaoka
   @author Ergovision
*/

#include "ModelLoaderUtil.h"
#include "Link.h"
#include "Sensor.h"
#include "Light.h"
#include <hrpUtil/Eigen3d.h>
#include <hrpUtil/Eigen4d.h>
#include <hrpCorba/OpenHRPCommon.hh>
#include <hrpCorba/ViewSimulator.hh>
#include <hrpCollision/ColdetModel.h>
#include <stack>

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
        cout << "<<< BodyInfo >>>\n";

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
    
    static Link *createNewLink() { return new Link(); }
    class ModelLoaderHelper
    {
    public:
        ModelLoaderHelper() {
            collisionDetectionModelLoading = false;
            createLinkFunc = createNewLink;
        }

        void enableCollisionDetectionModelLoading(bool isEnabled) {
            collisionDetectionModelLoading = isEnabled;
        };
        void setLinkFactory(Link *(*f)()) { createLinkFunc = f; }

        bool createBody(BodyPtr& body,  BodyInfo_ptr bodyInfo);
        
    private:
        BodyPtr body;
        LinkInfoSequence_var linkInfoSeq;
        ShapeInfoSequence_var shapeInfoSeq;
        ExtraJointInfoSequence_var extraJointInfoSeq;
        bool collisionDetectionModelLoading;
        Link *(*createLinkFunc)();

        Link* createLink(int index, const Matrix33& parentRs);
        void createSensors(Link* link, const SensorInfoSequence& sensorInfoSeq, const Matrix33& Rs);
        void createLights(Link* link, const LightInfoSequence& lightInfoSeq, const Matrix33& Rs);
        void createColdetModel(Link* link, const LinkInfo& linkInfo);
        void addLinkPrimitiveInfo(ColdetModelPtr& coldetModel, 
                                  const double *R, const double *p,
                                  const ShapeInfo& shapeInfo);
        void addLinkVerticesAndTriangles(ColdetModelPtr& coldetModel, const LinkInfo& linkInfo);
        void addLinkVerticesAndTriangles(ColdetModelPtr& coldetModel, const TransformedShapeIndex& tsi, const Matrix44& Tparent, ShapeInfoSequence_var& shapes, int& vertexIndex, int& triangleIndex);
		void setExtraJoints();
    };
}


bool ModelLoaderHelper::createBody(BodyPtr& body, BodyInfo_ptr bodyInfo)
{
    this->body = body;

    const char* name = bodyInfo->name();
    body->setModelName(name);
    body->setName(name);

    int n = bodyInfo->links()->length();
    linkInfoSeq = bodyInfo->links();
    shapeInfoSeq = bodyInfo->shapes();
	extraJointInfoSeq = bodyInfo->extraJoints();

    int rootIndex = -1;

    for(int i=0; i < n; ++i){
        if(linkInfoSeq[i].parentIndex < 0){
            if(rootIndex < 0){
                rootIndex = i;
            } else {
                 // more than one root !
                rootIndex = -1;
                break;
            }
        }
    }

    if(rootIndex >= 0){ // root exists

        Matrix33 Rs(Matrix33::Identity());
        Link* rootLink = createLink(rootIndex, Rs);
        body->setRootLink(rootLink);
        body->setDefaultRootPosition(rootLink->b, rootLink->Rs);

        body->installCustomizer();
        body->initializeConfiguration();

		setExtraJoints();

        return true;
    }

    return false;
}


Link* ModelLoaderHelper::createLink(int index, const Matrix33& parentRs)
{
    const LinkInfo& linkInfo = linkInfoSeq[index];
    int jointId = linkInfo.jointId;
        
    Link* link = (*createLinkFunc)();
        
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
    } else if(jt == "crawler"){
        link->jointType == Link::FIXED_JOINT;
        link->isCrawler = true;
    } else {
        link->jointType = Link::FREE_JOINT;
    }

    if(jointId < 0){
        if(link->jointType == Link::ROTATIONAL_JOINT || link->jointType == Link::SLIDE_JOINT){
            std::cerr << "Warning:  Joint ID is not given to joint " << link->name
                      << " of model " << body->modelName() << "." << std::endl;
        }
    }

    link->a.setZero();
    link->d.setZero();

    Vector3 axis( Rs * Vector3(linkInfo.jointAxis[0], linkInfo.jointAxis[1], linkInfo.jointAxis[2]));

    if(link->jointType == Link::ROTATIONAL_JOINT || jt == "crawler"){
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

    double maxlimit = (numeric_limits<double>::max)();

    link->ulimit  = getLimitValue(ulimit,  +maxlimit);
    link->llimit  = getLimitValue(llimit,  -maxlimit);
    link->uvlimit = getLimitValue(uvlimit, +maxlimit);
    link->lvlimit = getLimitValue(lvlimit, -maxlimit);

    link->c = Rs * Vector3(linkInfo.centerOfMass[0], linkInfo.centerOfMass[1], linkInfo.centerOfMass[2]);

    Matrix33 Io;
    getMatrix33FromRowMajorArray(Io, linkInfo.inertia);
    link->I = Rs * Io * Rs.transpose();

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
    createLights(link, linkInfo.lights, Rs);

    if(collisionDetectionModelLoading){
        createColdetModel(link, linkInfo);
    }

    return link;
}


void ModelLoaderHelper::createLights(Link* link, const LightInfoSequence& lightInfoSeq, const Matrix33& Rs)
{
    int numLights = lightInfoSeq.length();

    for(int i=0 ; i < numLights ; ++i ) {
        const LightInfo& lightInfo = lightInfoSeq[i];
        std::string name(lightInfo.name);
        Light *light = body->createLight(link, lightInfo.type, name);
        const double *T = lightInfo.transformMatrix;
        light->localPos << T[3], T[7], T[11];
        light->localR << T[0], T[1], T[2], T[4], T[5], T[6], T[8], T[9], T[10]; 
        switch (lightInfo.type){
        case Light::POINT:
            light->ambientIntensity = lightInfo.ambientIntensity;
            getVector3(light->attenuation, lightInfo.attenuation);
            getVector3(light->color, lightInfo.color);
            light->intensity = lightInfo.intensity;
            getVector3(light->location, lightInfo.location);
            light->on = lightInfo.on;
            light->radius = lightInfo.radius;
            break;
        case Light::DIRECTIONAL:
            light->ambientIntensity = lightInfo.ambientIntensity;
            getVector3(light->color, lightInfo.color);
            light->intensity = lightInfo.intensity;
            light->on = lightInfo.on;
            getVector3(light->direction, lightInfo.color); 
            break;
        case Light::SPOT:
            light->ambientIntensity = lightInfo.ambientIntensity;
            getVector3(light->attenuation, lightInfo.attenuation);
            getVector3(light->color, lightInfo.color);
            light->intensity = lightInfo.intensity;
            getVector3(light->location, lightInfo.location);
            light->on = lightInfo.on;
            light->radius = lightInfo.radius;
            getVector3(light->direction, lightInfo.direction);
            light->beamWidth = lightInfo.beamWidth;
            light->cutOffAngle = lightInfo.cutOffAngle;
            break;
        default:
            std::cerr << "unknown light type" << std::endl;
        }
    }    
}

void ModelLoaderHelper::createSensors(Link* link, const SensorInfoSequence& sensorInfoSeq, const Matrix33& Rs)
{
    int numSensors = sensorInfoSeq.length();

    for(int i=0 ; i < numSensors ; ++i ) {
        const SensorInfo& sensorInfo = sensorInfoSeq[i];

        int id = sensorInfo.id;
        if(id < 0) {
            std::cerr << "Warning:  sensor ID is not given to sensor " << sensorInfo.name
                      << "of model " << body->modelName() << "." << std::endl;
        } else {
            int sensorType = Sensor::COMMON;

            CORBA::String_var type0 = sensorInfo.type;
            string type(type0);

            if(type == "Force")             { sensorType = Sensor::FORCE; }
            else if(type == "RateGyro")     { sensorType = Sensor::RATE_GYRO; }
            else if(type == "Acceleration")	{ sensorType = Sensor::ACCELERATION; }
            else if(type == "Vision")       { sensorType = Sensor::VISION; }
            else if(type == "Range")        { sensorType = Sensor::RANGE; }

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
            
            if ( sensorType == Sensor::RANGE ) {
                RangeSensor *range = dynamic_cast<RangeSensor *>(sensor);
                range->scanAngle = sensorInfo.specValues[0];
                range->scanStep = sensorInfo.specValues[1];
                range->scanRate = sensorInfo.specValues[2];
                range->maxDistance = sensorInfo.specValues[3];
            }else if (sensorType == Sensor::VISION) {
                VisionSensor *vision = dynamic_cast<VisionSensor *>(sensor);
                vision->near   = sensorInfo.specValues[0];
                vision->far    = sensorInfo.specValues[1];
                vision->fovy   = sensorInfo.specValues[2];
                vision->width  = sensorInfo.specValues[4];
                vision->height = sensorInfo.specValues[5];
                int npixel = vision->width*vision->height;
                switch((int)sensorInfo.specValues[3]){
                case Camera::NONE: 
                    vision->imageType = VisionSensor::NONE; 
                    break;
                case Camera::COLOR:
                    vision->imageType = VisionSensor::COLOR;
                    vision->image.resize(npixel*3);
                    break;
                case Camera::MONO:
                    vision->imageType = VisionSensor::MONO;
                    vision->image.resize(npixel);
                    break;
                case Camera::DEPTH:
                    vision->imageType = VisionSensor::DEPTH;
                    break;
                case Camera::COLOR_DEPTH:
                    vision->imageType = VisionSensor::COLOR_DEPTH;
                    vision->image.resize(npixel*3);
                    break;
                case Camera::MONO_DEPTH:
                    vision->imageType = VisionSensor::MONO_DEPTH;
                    vision->image.resize(npixel);
                    break;
                }
                vision->frameRate = sensorInfo.specValues[6];
            }
        }
    }
}


void ModelLoaderHelper::createColdetModel(Link* link, const LinkInfo& linkInfo)
{
    int totalNumVertices = 0;
    int totalNumTriangles = 0;
    const TransformedShapeIndexSequence& shapeIndices = linkInfo.shapeIndices;
    unsigned int nshape = shapeIndices.length();
    short shapeIndex;
    double R[9], p[3];
    for(unsigned int i=0; i < shapeIndices.length(); i++){
        shapeIndex = shapeIndices[i].shapeIndex;
        const DblArray12 &tform = shapeIndices[i].transformMatrix;
        R[0] = tform[0]; R[1] = tform[1]; R[2] = tform[2]; p[0] = tform[3];
        R[3] = tform[4]; R[4] = tform[5]; R[5] = tform[6]; p[1] = tform[7];
        R[6] = tform[8]; R[7] = tform[9]; R[8] = tform[10]; p[2] = tform[11];
        const ShapeInfo& shapeInfo = shapeInfoSeq[shapeIndex];
        totalNumVertices += shapeInfo.vertices.length() / 3;
        totalNumTriangles += shapeInfo.triangles.length() / 3;
    }

    const SensorInfoSequence& sensors = linkInfo.sensors;
    for (unsigned int i=0; i<sensors.length(); i++){
        const SensorInfo &sinfo = sensors[i];
        const TransformedShapeIndexSequence tsis = sinfo.shapeIndices;
        nshape += tsis.length();
        for (unsigned int j=0; j<tsis.length(); j++){
            short shapeIndex = tsis[j].shapeIndex;
            const ShapeInfo& shapeInfo = shapeInfoSeq[shapeIndex];
            totalNumTriangles += shapeInfo.triangles.length() / 3;
            totalNumVertices += shapeInfo.vertices.length() / 3 ;
        }
    }

    ColdetModelPtr coldetModel(new ColdetModel());
    coldetModel->setName(linkInfo.name);
    if(totalNumTriangles > 0){
        coldetModel->setNumVertices(totalNumVertices);
        coldetModel->setNumTriangles(totalNumTriangles);
        if (nshape == 1){
            addLinkPrimitiveInfo(coldetModel, R, p, shapeInfoSeq[shapeIndex]);
        }
        addLinkVerticesAndTriangles(coldetModel, linkInfo);
        coldetModel->build();
    }
    link->coldetModel = coldetModel;
}

void ModelLoaderHelper::addLinkVerticesAndTriangles
(ColdetModelPtr& coldetModel, const TransformedShapeIndex& tsi, const Matrix44& Tparent, ShapeInfoSequence_var& shapes, int& vertexIndex, int& triangleIndex)
{
    short shapeIndex = tsi.shapeIndex;
    const DblArray12& M = tsi.transformMatrix;;
    Matrix44 T, Tlocal;
    Tlocal << M[0], M[1], M[2],  M[3],
             M[4], M[5], M[6],  M[7],
             M[8], M[9], M[10], M[11],
             0.0,  0.0,  0.0,   1.0;
    T = Tparent * Tlocal;
    
    const ShapeInfo& shapeInfo = shapes[shapeIndex];
    int vertexIndexBase = vertexIndex;
    int triangleIndexBase = triangleIndex;
    const FloatSequence& vertices = shapeInfo.vertices;
    const int numVertices = vertices.length() / 3;
    for(int j=0; j < numVertices; ++j){
        Vector4 v(T * Vector4(vertices[j*3], vertices[j*3+1], vertices[j*3+2], 1.0));
        coldetModel->setVertex(vertexIndex++, v[0], v[1], v[2]);
    }

    const LongSequence& triangles = shapeInfo.triangles;
    const int numTriangles = triangles.length() / 3;
    coldetModel->initNeighbor(numTriangles);
    for(int j=0; j < numTriangles; ++j){
       int t0 = triangles[j*3] + vertexIndexBase;
       int t1 = triangles[j*3+1] + vertexIndexBase;
       int t2 = triangles[j*3+2] + vertexIndexBase;
       coldetModel->setTriangle( triangleIndex, t0, t1, t2);
       coldetModel->setNeighborTriangle(triangleIndex++, t0, t1, t2);
    }
    
}

void ModelLoaderHelper::addLinkVerticesAndTriangles(ColdetModelPtr& coldetModel, const LinkInfo& linkInfo)
{
    int vertexIndex = 0;
    int triangleIndex = 0;

    const TransformedShapeIndexSequence& shapeIndices = linkInfo.shapeIndices;
    
    Matrix44 E(Matrix44::Identity());
    for(unsigned int i=0; i < shapeIndices.length(); i++){
        addLinkVerticesAndTriangles(coldetModel, shapeIndices[i], E, shapeInfoSeq,
                                    vertexIndex, triangleIndex);
    }

    Matrix44 T(Matrix44::Identity());
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
                                        shapeInfoSeq,
                                        vertexIndex, triangleIndex);
        }
    }
}

void ModelLoaderHelper::addLinkPrimitiveInfo(ColdetModelPtr& coldetModel, 
                                             const double *R, const double *p,
                                             const ShapeInfo& shapeInfo)
{
    switch(shapeInfo.primitiveType){
    case SP_BOX:
        coldetModel->setPrimitiveType(ColdetModel::SP_BOX);
        break;
    case SP_CYLINDER:
        coldetModel->setPrimitiveType(ColdetModel::SP_CYLINDER);
        break;
    case SP_CONE:
        coldetModel->setPrimitiveType(ColdetModel::SP_CONE);
        break;
    case SP_SPHERE:
        coldetModel->setPrimitiveType(ColdetModel::SP_SPHERE);
        break;
    case SP_PLANE:
        coldetModel->setPrimitiveType(ColdetModel::SP_PLANE);
        break;
    default:
        break;
    }
    coldetModel->setNumPrimitiveParams(shapeInfo.primitiveParameters.length());
    for (unsigned int i=0; i<shapeInfo.primitiveParameters.length(); i++){
        coldetModel->setPrimitiveParam(i, shapeInfo.primitiveParameters[i]);
    }
    coldetModel->setPrimitivePosition(R, p);
}

void ModelLoaderHelper::setExtraJoints()
{
    body->extraJoints.clear();
    int n = extraJointInfoSeq->length();
    
    for(int i=0; i < n; ++i){
		const ExtraJointInfo& extraJointInfo = extraJointInfoSeq[i];
        Body::ExtraJoint joint;
		joint.link[0] = body->link(string(extraJointInfo.link[0]));
        joint.link[1] = body->link(string(extraJointInfo.link[1]));

		ExtraJointType jointType = extraJointInfo.jointType;
        if(jointType == OpenHRP::EJ_PISTON){
            joint.type = Body::EJ_PISTON;
			joint.axis = Vector3(extraJointInfo.axis[0], extraJointInfo.axis[1], extraJointInfo.axis[2] );
        } else if(jointType == OpenHRP::EJ_BALL){
            joint.type = Body::EJ_BALL;
        }

		joint.point[0] = Vector3(extraJointInfo.point[0][0], extraJointInfo.point[0][1], extraJointInfo.point[0][2] );
		joint.point[1] = Vector3(extraJointInfo.point[1][0], extraJointInfo.point[1][1], extraJointInfo.point[1][2] );
            
        body->extraJoints.push_back(joint);
    }
}

bool hrp::loadBodyFromBodyInfo(BodyPtr body, OpenHRP::BodyInfo_ptr bodyInfo, bool loadGeometryForCollisionDetection, Link *(*f)())
{
    if(!CORBA::is_nil(bodyInfo)){
        ModelLoaderHelper helper;
        if (f) helper.setLinkFactory(f);
        if(loadGeometryForCollisionDetection){
            helper.enableCollisionDetectionModelLoading(true);
        }
        return helper.createBody(body, bodyInfo);
    }
    return false;
}

BodyInfo_var hrp::loadBodyInfo(const char* url, int& argc, char* argv[])
{
    CORBA::ORB_var orb = CORBA::ORB_init(argc, argv);
    return loadBodyInfo(url, orb);
}

BodyInfo_var hrp::loadBodyInfo(const char* url, CORBA_ORB_var orb)
{
    CosNaming::NamingContext_var cxt;
    try {
        CORBA::Object_var nS = orb->resolve_initial_references("NameService");
        cxt = CosNaming::NamingContext::_narrow(nS);
    } catch(CORBA::SystemException& ex) {
        std::cerr << "NameService doesn't exist" << std::endl;
        return false;
    }
    return loadBodyInfo(url, cxt);
}

ModelLoader_var hrp::getModelLoader(CORBA_ORB_var orb)
{
    CosNaming::NamingContext_var cxt;
    try {
        CORBA::Object_var nS = orb->resolve_initial_references("NameService");
        cxt = CosNaming::NamingContext::_narrow(nS);
    } catch(CORBA::SystemException& ex) {
        std::cerr << "NameService doesn't exist" << std::endl;
        return NULL;
    }
    return getModelLoader(cxt);
}

ModelLoader_var hrp::getModelLoader(CosNaming::NamingContext_var cxt)
{
    CosNaming::Name ncName;
    ncName.length(1);
    ncName[0].id = CORBA::string_dup("ModelLoader");
    ncName[0].kind = CORBA::string_dup("");
    ModelLoader_var modelLoader = NULL;
    try {
        modelLoader = ModelLoader::_narrow(cxt->resolve(ncName));
        modelLoader->_non_existent();
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
        modelLoader = ModelLoader::_nil();
    } catch(CosNaming::NamingContext::CannotProceed &exc) {
        std::cerr << "Resolve ModelLoader CannotProceed" << std::endl;
        modelLoader = ModelLoader::_nil();
    } catch(CosNaming::NamingContext::AlreadyBound &exc) {
        std::cerr << "Resolve ModelLoader InvalidName" << std::endl;
        modelLoader = ModelLoader::_nil();
    } catch(...){
        modelLoader = ModelLoader::_nil();
    }
    return modelLoader;
}

BodyInfo_var hrp::loadBodyInfo(const char* url, CosNaming::NamingContext_var cxt)
{
    ModelLoader_var modelLoader = getModelLoader(cxt);

    BodyInfo_var bodyInfo;
    try {        
        bodyInfo = modelLoader->getBodyInfo(url);
    } catch(CORBA::SystemException& ex) {
        std::cerr << "CORBA::SystemException raised by ModelLoader: " << ex._rep_id() << std::endl;
    } catch(ModelLoader::ModelLoaderException& ex){
        std::cerr << "ModelLoaderException : " << ex.description << std::endl;
    }
    return bodyInfo;
}

bool hrp::loadBodyFromModelLoader(BodyPtr body, const char* url, CosNaming::NamingContext_var cxt,  bool loadGeometryForCollisionDetection)
{
    BodyInfo_var bodyInfo = loadBodyInfo(url, cxt);

    if(!CORBA::is_nil(bodyInfo)){
        ModelLoaderHelper helper;
        if(loadGeometryForCollisionDetection){
            helper.enableCollisionDetectionModelLoading(true);
        }
        return helper.createBody(body, bodyInfo);
    }

    return false;
}


bool hrp::loadBodyFromModelLoader(BodyPtr body, const char* url, CORBA_ORB_var orb, bool loadGeometryForCollisionDetection)
{
    CosNaming::NamingContext_var cxt;
    try {
        CORBA::Object_var nS = orb->resolve_initial_references("NameService");
        cxt = CosNaming::NamingContext::_narrow(nS);
    } catch(CORBA::SystemException& ex) {
        std::cerr << "NameService doesn't exist" << std::endl;
        return false;
    }

    return loadBodyFromModelLoader(body, url, cxt, loadGeometryForCollisionDetection);
}


bool hrp::loadBodyFromModelLoader(BodyPtr body, const char* url, int& argc, char* argv[], bool loadGeometryForCollisionDetection)
{
    CORBA::ORB_var orb = CORBA::ORB_init(argc, argv);
    return loadBodyFromModelLoader(body, url,  orb, loadGeometryForCollisionDetection);
}
