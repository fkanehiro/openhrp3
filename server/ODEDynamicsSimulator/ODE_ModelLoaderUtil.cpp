#include "ODE_ModelLoaderUtil.h"
#include <stack>

using namespace hrp;
using namespace OpenHRP;

namespace ODESim
{
    class ModelLoaderHelper
    {
    public:
        bool createBody(BodyPtr body, ODE_World* worldId, BodyInfo_ptr bodyInfo);
        
    private:
        BodyPtr body;
        dWorldID worldId;
        dSpaceID spaceId;
        LinkInfoSequence_var linkInfoSeq;
        ShapeInfoSequence_var shapeInfoSeq;

        ODE_Link* createLink(int index, dBodyID parentBodyId, const Matrix44& parentT);
        void createSensors(Link* link, const SensorInfoSequence& sensorInfoSeq, const Matrix33& Rs);
        void createGeometry(ODE_Link* link, const LinkInfo& linkInfo);
        void addLinkVerticesAndTriangles(ODE_Link* link, const LinkInfo& linkInfo);
    };
};

    inline double getLimitValue(DblSequence limitseq, double defaultValue)
    {
        return (limitseq.length() == 0) ? defaultValue : limitseq[0];
    }
    

using namespace ODESim;
using namespace std;
bool ModelLoaderHelper::createBody(BodyPtr body, ODE_World* world, BodyInfo_ptr bodyInfo)
{
    worldId = world->getWorldID();
    spaceId = world->getSpaceID();
    this->body = body;
    const char* name = bodyInfo->name();
    body->setModelName(name);
    body->setName(name);

    int n = bodyInfo->links()->length();
    linkInfoSeq = bodyInfo->links();
    shapeInfoSeq = bodyInfo->shapes();

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
	Matrix44 T(Matrix44::Identity());
        ODE_Link* rootLink = createLink(rootIndex, 0, T);
        body->setRootLink(rootLink);

        body->setDefaultRootPosition(rootLink->b, rootLink->Rs);

        body->initializeConfiguration();

        return true;
    }

    return false;
}

ODE_Link* ModelLoaderHelper::createLink(int index, dBodyID parentBodyId, const Matrix44& parentT)
{
    const LinkInfo& linkInfo = linkInfoSeq[index];
    int jointId = linkInfo.jointId;
        
    ODE_Link* link = new ODE_Link();
    dBodyID bodyId = dBodyCreate(worldId);
    link->bodyId = bodyId;

    CORBA::String_var name0 = linkInfo.name;
    link->name = string( name0 );
    link->jointId = jointId;
    
    Matrix33 parentRs;
    parentRs << parentT(0,0), parentT(0,1), parentT(0,2),
                parentT(1,0), parentT(1,1), parentT(1,2),
                parentT(2,0), parentT(2,1), parentT(2,2);
    Vector3 relPos(linkInfo.translation[0], linkInfo.translation[1], linkInfo.translation[2]);
    link->b = parentRs * relPos;

    Vector3 rotAxis(linkInfo.rotation[0], linkInfo.rotation[1], linkInfo.rotation[2]);
    Matrix44 localT;
    calcRodrigues(localT, rotAxis,linkInfo.rotation[3]);
    localT(0,3) = linkInfo.translation[0];
    localT(1,3) = linkInfo.translation[1];
    localT(2,3) = linkInfo.translation[2];
    Matrix44 T(parentT*localT);
    
    link->Rs << T(0,0), T(0,1), T(0,2),
                T(1,0), T(1,1), T(1,2),
                T(2,0), T(2,1), T(2,2);
    Vector3 p(T(0,3), T(1,3), T(2,3));
    const Matrix33& Rs = link->Rs;
    link->C = Vector3(linkInfo.centerOfMass[0], linkInfo.centerOfMass[1], linkInfo.centerOfMass[2]);

    link->setTransform(p, Rs);

    CORBA::String_var jointType = linkInfo.jointType;
    const string jt( jointType );
    if(jt == "fixed" ){
        link->jointType = ODE_Link::FIXED_JOINT;
        dJointID djointId = dJointCreateFixed(worldId, 0);
        dJointAttach(djointId, bodyId, 0);
        link->odeJointId = djointId;
    } else if(jt == "free" ){
        link->jointType = ODE_Link::FREE_JOINT;
    } else if(jt == "rotate" ){
        link->jointType = ODE_Link::ROTATIONAL_JOINT;
        dJointID djointId = dJointCreateHinge(worldId, 0);
        dJointAttach(djointId, bodyId, parentBodyId);
        dJointSetHingeAnchor(djointId, T(0,3), T(1,3), T(2,3));
        Vector4 axis( T * Vector4(linkInfo.jointAxis[0], linkInfo.jointAxis[1], linkInfo.jointAxis[2], 0));
        dJointSetHingeAxis(djointId, axis(0), axis(1), axis(2));
        link->odeJointId = djointId;
    } else if(jt == "slide" ){
        link->jointType = ODE_Link::SLIDE_JOINT;
        dJointID djointId = dJointCreateSlider(worldId, 0);
        dJointAttach(djointId, bodyId, parentBodyId);
        Vector4 axis( T * Vector4(linkInfo.jointAxis[0], linkInfo.jointAxis[1], linkInfo.jointAxis[2], 0));
        dJointSetSliderAxis(djointId, axis(0), axis(1), axis(2));
        link->odeJointId = djointId;
    } else {
        link->jointType = ODE_Link::FREE_JOINT;
    }

    link->a.setZero();
    link->d.setZero();
    Vector3 axis( Rs * Vector3(linkInfo.jointAxis[0], linkInfo.jointAxis[1], linkInfo.jointAxis[2]));

    if(link->jointType == Link::ROTATIONAL_JOINT){
        link->a = axis;
    } else if(link->jointType == Link::SLIDE_JOINT){
        link->d = axis;
    }

    if(jointId < 0){
        if(link->jointType == Link::ROTATIONAL_JOINT || link->jointType == Link::SLIDE_JOINT){
            cerr << "Warning:  Joint ID is not given to joint " << link->name
                      << " of model " << body->modelName() << "." << endl;
        }
    }

    link->m  = linkInfo.mass;
    link->Ir = linkInfo.rotorInertia;

    if(jt != "fixed" ){
        dMass mass;
        dMassSetZero(&mass);
        dMassSetParameters(&mass, linkInfo.mass,
            0,0,0,
            linkInfo.inertia[0], linkInfo.inertia[4], linkInfo.inertia[8],
            linkInfo.inertia[1], linkInfo.inertia[2], linkInfo.inertia[5] );
        dBodySetMass(bodyId, &mass);
    }

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
    stack<Link*> children;
	
    //##### [Changed] Link Structure (convert NaryTree to BinaryTree).
    int childNum = linkInfo.childIndices.length();
    for(int i = 0 ; i < childNum ; i++) {
        int childIndex = linkInfo.childIndices[i];
        Link* childLink = createLink(childIndex, bodyId, T);
        if(childLink) {
            children.push(childLink);
        }
    }
    while(!children.empty()){
        link->addChild(children.top());
        children.pop();
    }
        
    createSensors(link, linkInfo.sensors, Rs);

    createGeometry(link, linkInfo);

    return link;
}

void ModelLoaderHelper::createGeometry(ODE_Link* link, const LinkInfo& linkInfo)
{
    int totalNumTriangles = 0;
    const TransformedShapeIndexSequence& shapeIndices = linkInfo.shapeIndices;
    int numofGeom = 0;
    for(unsigned int i=0; i < shapeIndices.length(); i++){
        const TransformedShapeIndex& tsi = shapeIndices[i];
        const DblArray12& M = tsi.transformMatrix;
        dMatrix3 R = {  M[0], M[1], M[2],  0,
                        M[4], M[5], M[6],  0,
                        M[8], M[9], M[10], 0 };
        short shapeIndex = shapeIndices[i].shapeIndex;
        const ShapeInfo& shapeInfo = shapeInfoSeq[shapeIndex];
        Matrix33 R0;
        R0 << M[0],M[1],M[2],
              M[4],M[5],M[6],
              M[8],M[9],M[10];
        if(isOrthogonalMatrix(R0)){
            switch(shapeInfo.primitiveType){
                case OpenHRP::SP_BOX :{
                        dReal x = shapeInfo.primitiveParameters[0];
                        dReal y = shapeInfo.primitiveParameters[1];
                        dReal z = shapeInfo.primitiveParameters[2];
                        link->geomIds.push_back(dCreateBox( spaceId, x, y, z));
                        dGeomSetBody(link->geomIds.at(numofGeom), link->bodyId);
                        dGeomSetOffsetRotation(link->geomIds.at(numofGeom), R);
                        dGeomSetOffsetPosition(link->geomIds.at(numofGeom), M[3]-link->C(0), M[7]-link->C(1), M[11]-link->C(2));
                        numofGeom++;
                    }
                    break;
                case OpenHRP::SP_CYLINDER :{
                        dReal radius = shapeInfo.primitiveParameters[0];
                        dReal height = shapeInfo.primitiveParameters[1];
                        link->geomIds.push_back(dCreateCylinder( spaceId, radius, height));
                        dGeomSetBody(link->geomIds.at(numofGeom), link->bodyId);
                        dGeomSetOffsetRotation(link->geomIds.at(numofGeom), R);
                        dGeomSetOffsetPosition(link->geomIds.at(numofGeom), M[3]-link->C(0), M[7]-link->C(1), M[11]-link->C(2));
                        numofGeom++;
                                                           }
                    break;
                case OpenHRP::SP_SPHERE :{
                        dReal radius = shapeInfo.primitiveParameters[0];
                        link->geomIds.push_back(dCreateSphere( spaceId, radius ));
                        dGeomSetBody(link->geomIds.at(numofGeom), link->bodyId);
                        dGeomSetOffsetRotation(link->geomIds.at(numofGeom), R);
                        dGeomSetOffsetPosition(link->geomIds.at(numofGeom), M[3]-link->C(0), M[7]-link->C(1), M[11]-link->C(2));
                        numofGeom++;
                                                           }
                    break;
                default :
                    totalNumTriangles += shapeInfo.triangles.length() / 3;
            }
        }else
            totalNumTriangles += shapeInfo.triangles.length() / 3;
    }
    int totalNumVertices = totalNumTriangles * 3;

    link->vertices.resize(totalNumVertices*3);
    link->indices.resize(totalNumTriangles*3);
    addLinkVerticesAndTriangles( link, linkInfo );
    if(totalNumTriangles){
        link->triMeshDataId = dGeomTriMeshDataCreate();
        dGeomTriMeshDataBuildSingle(link->triMeshDataId, &link->vertices[0], 3 * sizeof(dReal), 
            totalNumVertices, &link->indices[0], totalNumTriangles*3, 3*sizeof(int));
        link->geomIds.push_back( dCreateTriMesh( spaceId, link->triMeshDataId, 0, 0, 0) );
        dGeomSetBody(link->geomIds.at(numofGeom), link->bodyId);
        dGeomSetOffsetPosition (link->geomIds.at(numofGeom), -link->C(0), -link->C(1), -link->C(2));
    }else{
       link->triMeshDataId = 0;
    }
}

void ModelLoaderHelper::addLinkVerticesAndTriangles(ODE_Link* link, const LinkInfo& linkInfo)
{
    int vertexIndex = 0;
    int triangleIndex = 0;

    const TransformedShapeIndexSequence& shapeIndices = linkInfo.shapeIndices;
    
    for(unsigned int i=0; i < shapeIndices.length(); i++){
        const TransformedShapeIndex& tsi = shapeIndices[i];
        short shapeIndex = tsi.shapeIndex;
        const ShapeInfo& shapeInfo = shapeInfoSeq[shapeIndex];
        const DblArray12& M = tsi.transformMatrix;
        Matrix33 R0;
        R0 << M[0],M[1],M[2],
              M[4],M[5],M[6],
              M[8],M[9],M[10];
        if(isOrthogonalMatrix(R0) && 
            (shapeInfo.primitiveType == OpenHRP::SP_BOX ||
            shapeInfo.primitiveType == OpenHRP::SP_CYLINDER ||
            shapeInfo.primitiveType == OpenHRP::SP_SPHERE ) )
            continue;

        Matrix44 T;
        T << M[0], M[1], M[2],  M[3],
             M[4], M[5], M[6],  M[7],
             M[8], M[9], M[10], M[11],
             0.0,  0.0,  0.0,   1.0;
        const FloatSequence& vertices = shapeInfo.vertices;
        const LongSequence& triangles = shapeInfo.triangles;
        const int numTriangles = triangles.length() / 3;

        for(int j=0; j < numTriangles; ++j){
           int vertexIndexTop = vertexIndex;
           for(int k=0; k < 3; ++k){
                long orgVertexIndex = shapeInfo.triangles[j * 3 + k];
                int p = orgVertexIndex * 3;
                Vector4 v(T * Vector4(vertices[p+0], vertices[p+1], vertices[p+2], 1.0));
                link->vertices[vertexIndex*3] = v[0];
                link->vertices[vertexIndex*3+1] = v[1];
                link->vertices[vertexIndex*3+2] = v[2];
                vertexIndex++;
            }
            link->indices[triangleIndex++] = vertexIndexTop;
            link->indices[triangleIndex++] = vertexIndexTop+1;
            link->indices[triangleIndex++] = vertexIndexTop+2;
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
            cerr << "Warning:  sensor ID is not given to sensor " << sensorInfo.name
                      << "of model " << body->modelName() << "." << endl;
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

            Sensor* sensor = 0;
            if(sensorType == Sensor::FORCE){
                sensor = new ODE_ForceSensor();
            }else
                sensor = Sensor::create(sensorType);
            if(sensor){
                sensor->id = id;
                sensor->link = link;
                sensor->name = name;
                body->addSensor(sensor, sensorType, id);

                
                const DblArray3& p = sensorInfo.translation;
                sensor->localPos = Rs * Vector3(p[0], p[1], p[2]);

                const Vector3 axis(sensorInfo.rotation[0], sensorInfo.rotation[1], sensorInfo.rotation[2]);
                const Matrix33 R(rodrigues(axis, sensorInfo.rotation[3]));
                sensor->localR = Rs * R;

                if(sensorType == Sensor::FORCE){
                    dJointSetFeedback(((ODE_Link*)link)->odeJointId, &((ODE_ForceSensor*)sensor)->feedback);
                }
            }
            
            if ( sensorType == Sensor::RANGE ) {
                RangeSensor *range = dynamic_cast<RangeSensor *>(sensor);
                range->scanAngle = sensorInfo.specValues[0];
                range->scanStep = sensorInfo.specValues[1];
                range->scanRate = sensorInfo.specValues[2];
                range->maxDistance = sensorInfo.specValues[3];
            } 
        }
    }
}

bool ODE_loadBodyFromBodyInfo(BodyPtr body, ODE_World* world, OpenHRP::BodyInfo_ptr bodyInfo)
{
    if(!CORBA::is_nil(bodyInfo)){
        ODESim::ModelLoaderHelper helper;
        return helper.createBody(body, world, bodyInfo);
    }
    return false;
}
