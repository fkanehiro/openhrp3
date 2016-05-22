/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

/** \file
    \brief The implementation of the body class 
    \author Shin'ichiro Nakaoka
*/

#include "Body.h"
#include "Link.h"
#include "JointPath.h"
#include "Sensor.h"
#include "Light.h"
#include "BodyCustomizerInterface.h"
#include <hrpCollision/ColdetModel.h>
#include <map>
#include <cstdlib>

using namespace hrp;
using namespace std;

static const bool PUT_DEBUG_MESSAGE = true;

static bool pluginsInDefaultDirectoriesLoaded = false;

#ifndef uint
typedef unsigned int uint;
#endif

namespace hrp {
	
    class CustomizedJointPath : public JointPath
    {
        Body* body;
        int ikTypeId;
        bool isCustomizedIkPathReversed;
        virtual void onJointPathUpdated();
    public:
        CustomizedJointPath(Body* body, Link* baseLink, Link* targetLink);
        virtual ~CustomizedJointPath();
        virtual bool calcInverseKinematics(const Vector3& end_p, const Matrix33& end_R);
        virtual bool hasAnalyticalIK();
    };
}


Body::~Body()
{
    if(customizerHandle){
        customizerInterface->destroy(customizerHandle);
    }
	
    delete rootLink_;

    // delete sensors
    for(int sensorType =0; sensorType < numSensorTypes(); ++sensorType){
        int n = numSensors(sensorType);
        for(int i=0; i < n; ++i){
            Sensor* s = sensor(sensorType, i);
            if(s){
                Sensor::destroy(s);
            }
        }
    }
}


void Body::initialize()
{
    rootLink_ = 0;

    customizerHandle = 0;
    customizerInterface = 0;
    bodyHandleEntity.body = this;
    bodyHandle = &bodyHandleEntity;
}
	

Body::Body()
  : allSensors(Sensor::NUM_SENSOR_TYPES)
{
    initialize();
	
    rootLink_ = new Link;
    rootLink_->body = this;

    defaultRootPosition.setZero();
    defaultRootAttitude.setIdentity();
}


Body::Body(const Body& org)
    : modelName_(org.modelName_),
      name_(org.name_),
      allSensors(Sensor::NUM_SENSOR_TYPES)
{
    initialize();
	
    setRootLink(new Link(*org.rootLink()));

    defaultRootPosition = org.defaultRootPosition;
    defaultRootAttitude = org.defaultRootAttitude;
	
    // copy sensors
    std::map<Link*, int> linkToIndexMap;

    for(int i=0; i < org.linkTraverse_.numLinks(); ++i){
        Link* lnk = org.linkTraverse_[i];
        linkToIndexMap[lnk] = i;
    }

    int n = org.numSensorTypes();
    for(int sensorType = 0; sensorType < n; ++sensorType){
        for(int i=0; i < org.numSensors(sensorType); ++i){
            Sensor* orgSensor = org.sensor(sensorType, i);
	    if (orgSensor){
	        int linkIndex = linkToIndexMap[orgSensor->link];
		Link* newLink = linkTraverse_[linkIndex];
		Sensor* cloneSensor = createSensor(newLink, sensorType, orgSensor->id, orgSensor->name);
		*cloneSensor = *orgSensor;
	    }
        }
    }

    // do deep copy of extraJoint
	for(size_t i=0; i < org.extraJoints.size(); ++i){
		const ExtraJoint& orgExtraJoint = org.extraJoints[i];
        ExtraJoint extraJoint(orgExtraJoint);
        for(int j=0; j < 2; ++j){
			extraJoint.link[j] = link(orgExtraJoint.link[j]->index);
        }
    }

    if(org.customizerInterface){
        installCustomizer(org.customizerInterface);
    }
}


void Body::setRootLink(Link* link)
{
    if(rootLink_){
        delete rootLink_;
    }
    rootLink_ = link;

    updateLinkTree();
}


void Body::setDefaultRootPosition(const Vector3& p, const Matrix33& R)
{
    defaultRootPosition = p;
    defaultRootAttitude = R;
}


void Body::getDefaultRootPosition(Vector3& out_p, Matrix33& out_R)
{
    out_p = defaultRootPosition;
    out_R = defaultRootAttitude;
}


Link* Body::createEmptyJoint(int jointId)
{
    Link* empty = new Link;
    empty->body = this;
    empty->jointId = jointId;
    empty->p.setZero();
    empty->R.setIdentity();
    empty->v.setZero();
    empty->w.setZero();
    empty->dv.setZero();
    empty->dw.setZero();
    empty->q = 0.0;
    empty->dq = 0.0;
    empty->ddq = 0.0;
    empty->u = 0.0;
    empty->a.setZero();
    empty->d.setZero();
    empty->b.setZero();
    empty->Rs.setIdentity();
    empty->m = 0.0;
    empty->I.setZero();
    empty->c.setZero();
    empty->wc.setZero();
    empty->vo.setZero();
    empty->dvo.setZero();
    empty->fext.setZero();
    empty->tauext.setZero();
    empty->Jm2 = 0.0;
    empty->ulimit = 0.0;
    empty->llimit = 0.0;
    empty->uvlimit = 0.0;
    empty->lvlimit = 0.0;
    empty->defaultJointValue = 0.0;
    empty->Ir = 0.0;
    empty->gearRatio = 1.0;

    return empty;
}


void Body::updateLinkTree()
{
    nameToLinkMap.clear();
    linkTraverse_.find(rootLink());

    int n = linkTraverse_.numLinks();
    jointIdToLinkArray.clear();
    jointIdToLinkArray.resize(n, 0);
    int maxJointID = -1;
    
    for(int i=0; i < n; ++i){
        Link* link = linkTraverse_[i];
        link->body = this;
        link->index = i;
        nameToLinkMap[link->name] = link;

        int id = link->jointId;
        if(id >= 0){
            int size = jointIdToLinkArray.size();
            if(id >= size){
                jointIdToLinkArray.resize(id + 1, 0);
            }
            jointIdToLinkArray[id] = link;
            if(id > maxJointID){
                maxJointID = id;
            }
        }
    }

    jointIdToLinkArray.resize(maxJointID + 1);

    for(size_t i=0; i < jointIdToLinkArray.size(); ++i){
        if(!jointIdToLinkArray[i]){
            jointIdToLinkArray[i] = createEmptyJoint(i);
            std::cerr << "Warning: Model " << modelName_ <<
                " has empty joint ID in the valid IDs." << std::endl;
        }
    }

    calcTotalMass();

    isStaticModel_ = (rootLink_->jointType == Link::FIXED_JOINT && numJoints() == 0);
}


/**
   This function returns a link object whose name of Joint node matches a given name.
   Null is returned when the body has no joint of the given name.
*/
Link* Body::link(const std::string& name) const
{
    NameToLinkMap::const_iterator p = nameToLinkMap.find(name);
    return (p != nameToLinkMap.end()) ? p->second : 0;
}


void Body::initializeConfiguration()
{
    rootLink_->p = defaultRootPosition;
    rootLink_->setAttitude(defaultRootAttitude);

    rootLink_->v.setZero();
    rootLink_->dv.setZero();
    rootLink_->w.setZero();
    rootLink_->dw.setZero();
    rootLink_->vo.setZero();
    rootLink_->dvo.setZero();
    rootLink_->constraintForces.clear();
    
    int n = linkTraverse_.numLinks();
    for(int i=0; i < n; ++i){
        Link* link = linkTraverse_[i];
        link->u = 0.0;
        link->q = 0.0;
        link->dq = 0.0;
        link->ddq = 0.0;
        link->constraintForces.clear();
    }
 
    calcForwardKinematics(true, true);

    clearExternalForces();
}
 


double Body::calcTotalMass()
{
    totalMass_ = 0.0;

    int n = linkTraverse_.numLinks();
    for(int i=0; i < n; ++i){
        totalMass_ += linkTraverse_[i]->m;
    }

    return totalMass_;
}


Vector3 Body::calcCM()
{
    totalMass_ = 0.0;
    
    Vector3 mc(Vector3::Zero());

    int n = linkTraverse_.numLinks();
    for(int i=0; i < n; i++){
        Link* link = linkTraverse_[i];
        link->wc = link->p + link->R * link->c;
        mc += link->m * link->wc;
        totalMass_ += link->m;
    }

    return mc / totalMass_;
}


/**
   calculate the mass matrix using the unit vector method
   \todo replace the unit vector method here with
   a more efficient method that only requires O(n) computation time

   The motion equation (dv != dvo)
   |       |   | dv   |   |    |   | fext      |
   | out_M | * | dw   | + | b1 | = | tauext    |
   |       |   |ddq   |   |    |   | u         |
*/
void Body::calcMassMatrix(dmatrix& out_M)
{
    // buffers for the unit vector method
    dmatrix b1;
    dvector ddqorg;
    dvector uorg;
    Vector3 dvoorg;
    Vector3 dworg;
    Vector3 root_w_x_v;
    Vector3 g(0, 0, 9.8);

    uint nJ = numJoints();
    int totaldof = nJ;
    if( !isStaticModel_ ) totaldof += 6;

    out_M.resize(totaldof,totaldof);
    b1.resize(totaldof, 1);

    // preserve and clear the joint accelerations
    ddqorg.resize(nJ);
    uorg.resize(nJ);
    for(uint i = 0; i < nJ; ++i){
        Link* ptr = joint(i);
        ddqorg[i] = ptr->ddq;
        uorg  [i] = ptr->u;
        ptr->ddq = 0.0;
    }

    // preserve and clear the root link acceleration
    dvoorg = rootLink_->dvo;
    dworg  = rootLink_->dw;
    root_w_x_v = rootLink_->w.cross(rootLink_->vo + rootLink_->w.cross(rootLink_->p));
    rootLink_->dvo = g - root_w_x_v;   // dv = g, dw = 0
    rootLink_->dw.setZero();
	
    setColumnOfMassMatrix(b1, 0);

    if( !isStaticModel_ ){
        for(int i=0; i < 3; ++i){
            rootLink_->dvo[i] += 1.0;
            setColumnOfMassMatrix(out_M, i);
            rootLink_->dvo[i] -= 1.0;
        }
        for(int i=0; i < 3; ++i){
            rootLink_->dw[i] = 1.0;
            Vector3 dw_x_p = rootLink_->dw.cross(rootLink_->p);	//  spatial acceleration caused by ang. acc.
            rootLink_->dvo -= dw_x_p;
            setColumnOfMassMatrix(out_M, i + 3);
            rootLink_->dvo += dw_x_p;
            rootLink_->dw[i] = 0.0;
        }
    }

    for(uint i = 0; i < nJ; ++i){
        Link* ptr = joint(i);
        ptr->ddq = 1.0;
        int j = i + 6;
        setColumnOfMassMatrix(out_M, j);
        out_M(j, j) += ptr->Jm2; // motor inertia
        ptr->ddq = 0.0;
    }

    // subtract the constant term
    for(size_t i = 0; i < out_M.cols(); ++i){
        out_M.col(i) -= b1;
    }

    // recover state
    for(uint i = 0; i < nJ; ++i){
        Link* ptr = joint(i);
        ptr->ddq  = ddqorg[i];
        ptr->u    = uorg  [i];
    }
    rootLink_->dvo = dvoorg;
    rootLink_->dw  = dworg;
}


void Body::setColumnOfMassMatrix(dmatrix& out_M, int column)
{
    Vector3 f;
    Vector3 tau;

    calcInverseDynamics(rootLink_, f, tau);

    if( !isStaticModel_ ){
        tau -= rootLink_->p.cross(f);
        out_M.block<6,1>(0, column) << f, tau;
    }

    int n = numJoints();
    for(int i = 0; i < n; ++i){
        Link* ptr = joint(i);
        out_M(i + 6, column) = ptr->u;
    }
}


/*
 *  see Kajita et al. Humanoid Robot Ohm-sha,  p.210
 */
void Body::calcInverseDynamics(Link* ptr, Vector3& out_f, Vector3& out_tau)
{	
    Link* parent = ptr->parent;
    if(parent){
        Vector3 dsv,dsw,sv,sw;

        if(ptr->jointType != Link::FIXED_JOINT){
            sw.noalias()  = parent->R * ptr->a;
            sv  = ptr->p.cross(sw);
        }else{
            sw.setZero();
            sv.setZero();
        }
        dsv = parent->w.cross(sv) + parent->vo.cross(sw);
        dsw = parent->w.cross(sw);

        ptr->dw  = parent->dw  + dsw * ptr->dq + sw * ptr->ddq;
        ptr->dvo = parent->dvo + dsv * ptr->dq + sv * ptr->ddq;

        ptr->sw = sw;
        ptr->sv = sv;
    }
	
    Vector3  c,P,L;
    Matrix33 I,c_hat;

    c = ptr->R * ptr->c + ptr->p;
    I.noalias() = ptr->R * ptr->I * ptr->R.transpose();
    c_hat = hat(c);
    I.noalias() += ptr->m * c_hat * c_hat.transpose();
    P.noalias() = ptr->m * (ptr->vo + ptr->w.cross(c));
    L = ptr->m * c.cross(ptr->vo) + I * ptr->w;

    out_f   = ptr->m * (ptr->dvo + ptr->dw.cross(c)) + ptr->w.cross(P);
    out_tau = ptr->m * c.cross(ptr->dvo) + I * ptr->dw + ptr->vo.cross(P) + ptr->w.cross(L);

    if(ptr->child){
        Vector3 f_c;
        Vector3 tau_c;
        calcInverseDynamics(ptr->child, f_c, tau_c);
        out_f   += f_c;
        out_tau += tau_c;
    }

    ptr->u = ptr->sv.dot(out_f) + ptr->sw.dot(out_tau);

    if(ptr->sibling){
        Vector3 f_s;
        Vector3 tau_s;
        calcInverseDynamics(ptr->sibling, f_s, tau_s);
        out_f   += f_s;
        out_tau += tau_s;
    }
}


/**
   assuming Link::v,w is already computed by calcForwardKinematics(true);
   assuming Link::wc is already computed by calcCM();
*/
void Body::calcTotalMomentum(Vector3& out_P, Vector3& out_L)
{
    out_P.setZero();
    out_L.setZero();

    Vector3 dwc;	// Center of mass speed in world frame
    Vector3 P;		// Linear momentum of the link
    Vector3 L;		// Angular momentum with respect to the world frame origin 
    Vector3 Llocal; // Angular momentum with respect to the center of mass of the link

    int n = linkTraverse_.numLinks();
    for(int i=0; i < n; i++){
        Link* link = linkTraverse_[i];

        dwc = link->v + link->w.cross(link->R * link->c);

        P   = link->m * dwc;

        //L   = cross(link->wc, P) + link->R * link->I * trans(link->R) * link->w; 
        Llocal.noalias() = link->I * link->R.transpose()*link->w;
        L                = link->wc.cross(P) + link->R * Llocal; 

        out_P += P;
        out_L += L;
    }
}

void Body::calcTotalMomentumFromJacobian(Vector3& out_P, Vector3& out_L)
{
    out_P.setZero();
    out_L.setZero();

    dmatrix J,H;
    calcCMJacobian(NULL,J);
    calcAngularMomentumJacobian(NULL,H);

    dvector dq;
    int n = numJoints();
    dq.resize(n);
    for(int i=0; i < n; i++){
      Link* link = joint(i);
      dq[i] = link->dq;
    }
    dvector v;
    v.resize(n+3+3);
    v << dq, rootLink_->v, rootLink_->w;

    out_P = totalMass_ * J * v;
    out_L = H * v;
}

void Body::calcForwardKinematics(bool calcVelocity, bool calcAcceleration)
{
    linkTraverse_.calcForwardKinematics(calcVelocity, calcAcceleration);
}


Light* Body::createLight(Link* link, int lightType, const std::string& name)
{
    Light *light =  new Light(link, lightType, name);
    nameToLightMap[name] = light;
    return light;
}

Sensor* Body::createSensor(Link* link, int sensorType, int id, const std::string& name)
{
    Sensor* sensor = 0;

    if(sensorType < Sensor::NUM_SENSOR_TYPES && id >= 0){

        SensorArray& sensors = allSensors[sensorType];
        int n = sensors.size();
        if(id >= n){
            sensors.resize(id + 1, 0);
        }
        sensor = sensors[id];
        if(sensor){
            std::cerr << "duplicated sensor Id is specified(id = "
                      << id << ", name = " << name << ")" << std::endl;
                
            nameToSensorMap.erase(sensor->name);
        } else {
            sensor = Sensor::create(sensorType);
        }
        if(sensor){
            sensor->id = id;
            sensors[id] = sensor;
            sensor->link = link;
            sensor->name = name;
            nameToSensorMap[name] = sensor;
            link->sensors.push_back(sensor);
        }
    }
		
    return sensor;
}

void Body::addSensor(Sensor* sensor, int sensorType, int id ){
    if(sensorType < Sensor::NUM_SENSOR_TYPES && id >= 0){
        SensorArray& sensors = allSensors[sensorType];
        int n = sensors.size();
        if(id >= n){
            sensors.resize(id + 1, 0);
        }
        Sensor* sameId = sensors[id];
        if(sameId){
            std::cerr << "duplicated sensor Id is specified(id = "
                      << id << ", name = " << sensor->name << ")" << std::endl;
                
            nameToSensorMap.erase(sameId->name);
        }
        sensors[id] = sensor;
        nameToSensorMap[sensor->name] = sensor;
    }
}


void Body::clearSensorValues()
{
    for(int i=0; i < numSensorTypes(); ++i){
        for(int j=0; j < numSensors(i); ++j){
            if(sensor(i,j))
                sensor(i, j)->clear();
        }
    }
}


JointPathPtr Body::getJointPath(Link* baseLink, Link* targetLink)
{
    if(customizerInterface && customizerInterface->initializeAnalyticIk){
        return JointPathPtr(new CustomizedJointPath(this, baseLink, targetLink));
    } else {
        return JointPathPtr(new JointPath(baseLink, targetLink));
    }
}


void Body::setVirtualJointForcesSub()
{
    if(customizerInterface->setVirtualJointForces){
        customizerInterface->setVirtualJointForces(customizerHandle);
    }
}


void Body::clearExternalForces()
{
    int n = linkTraverse_.numLinks();
    for(int i=0; i < n; ++i){
        Link* link = linkTraverse_[i];
        link->fext.setZero();
        link->tauext.setZero();
    }
}


void Body::updateLinkColdetModelPositions()
{
    const int n = linkTraverse_.numLinks();
    for(int i=0; i < n; ++i){
        Link* link = linkTraverse_[i];
        if(link->coldetModel){
            link->coldetModel->setPosition(link->segmentAttitude(), link->p);
        }
    }
}


void Body::putInformation(std::ostream &out)
{
    out << "Body: model name = " << modelName_ << " name = " << name_ << "\n\n";

    int n = numLinks();
    for(int i=0; i < n; ++i){
        out << *link(i);
    }
    out << std::endl;
}


/**
   The function installs the pre-loaded customizer corresponding to the model name.
*/
bool Body::installCustomizer()
{
    if(!pluginsInDefaultDirectoriesLoaded){
        loadBodyCustomizers(bodyInterface());
        pluginsInDefaultDirectoriesLoaded = true;
    }
		
    BodyCustomizerInterface* interface = findBodyCustomizer(modelName_);

    return interface ? installCustomizer(interface) : false;
}


bool Body::installCustomizer(BodyCustomizerInterface * customizerInterface)
{
    if(this->customizerInterface){
        if(customizerHandle){
            this->customizerInterface->destroy(customizerHandle);
            customizerHandle = 0;
        }
        this->customizerInterface = 0;
    }
	
    if(customizerInterface){
        customizerHandle = customizerInterface->create(bodyHandle, modelName_.c_str());
        if(customizerHandle){
            this->customizerInterface = customizerInterface;
        }
    }

    return (customizerHandle != 0);
}


std::ostream& operator<< (std::ostream& out, Body& body)
{
    body.putInformation(out);
    return out;
}


static inline Link* extractLink(BodyHandle bodyHandle, int linkIndex)
{
    return static_cast<BodyHandleEntity*>(bodyHandle)->body->link(linkIndex);
}


static int getLinkIndexFromName(BodyHandle bodyHandle, const char* linkName)
{
    Body* body = static_cast<BodyHandleEntity*>(bodyHandle)->body;
    Link* link = body->link(linkName);
    return (link ? link->index : -1);
}


static const char* getLinkName(BodyHandle bodyHandle, int linkIndex)
{
    return extractLink(bodyHandle, linkIndex)->name.c_str();
}


static double* getJointValuePtr(BodyHandle bodyHandle, int linkIndex)
{
    return &(extractLink(bodyHandle,linkIndex)->q);
}


static double* getJointVelocityPtr(BodyHandle bodyHandle, int linkIndex)
{
    return &(extractLink(bodyHandle, linkIndex)->dq);
}


static double* getJointTorqueForcePtr(BodyHandle bodyHandle, int linkIndex)
{
    return &(extractLink(bodyHandle, linkIndex)->u);
}


BodyInterface* Body::bodyInterface()
{
    static BodyInterface interface = {
        hrp::BODY_INTERFACE_VERSION,
        getLinkIndexFromName,
        getLinkName,
        getJointValuePtr,
        getJointVelocityPtr,
        getJointTorqueForcePtr,
    };

    return &interface;
}


CustomizedJointPath::CustomizedJointPath(Body* body, Link* baseLink, Link* targetLink) :
    JointPath(baseLink, targetLink),
    body(body)
{
    onJointPathUpdated();
}


CustomizedJointPath::~CustomizedJointPath()
{

}


void CustomizedJointPath::onJointPathUpdated()
{
    ikTypeId = body->customizerInterface->initializeAnalyticIk(
        body->customizerHandle, baseLink()->index, endLink()->index);
    if(ikTypeId){
        isCustomizedIkPathReversed = false;
    } else {
        // try reversed path
        ikTypeId = body->customizerInterface->initializeAnalyticIk(
            body->customizerHandle, endLink()->index, baseLink()->index);
        if(ikTypeId){
            isCustomizedIkPathReversed = true;
        }
    }
}


bool CustomizedJointPath::calcInverseKinematics(const Vector3& end_p, const Matrix33& end_R)
{
    bool solved;
	
    if(ikTypeId == 0 || isBestEffortIKMode){

        solved = JointPath::calcInverseKinematics(end_p, end_R);

    } else {

        std::vector<double> qorg(numJoints());
		
        for(int i=0; i < numJoints(); ++i){
            qorg[i] = joint(i)->q;
        }

        const Link* targetLink = endLink();
        const Link* baseLink_ = baseLink();

        Vector3 p_relative;
        Matrix33 R_relative;
        if(!isCustomizedIkPathReversed){
            p_relative.noalias() = baseLink_->R.transpose() * (end_p - baseLink_->p);
            R_relative.noalias() = baseLink_->R.transpose() * end_R;
        } else {
            p_relative.noalias() = end_R.transpose() * (baseLink_->p - end_p);
            R_relative.noalias() = end_R.transpose() * baseLink_->R;
        }
        solved = body->customizerInterface->
            calcAnalyticIk(body->customizerHandle, ikTypeId, p_relative, R_relative);

        if(solved){

            calcForwardKinematics();

            Vector3 dp(end_p - targetLink->p);
            Vector3 omega(omegaFromRot((targetLink->R*targetLink->Rs).transpose() * end_R));
			
            double errsqr = dp.dot(dp) + omega.dot(omega);
			
            if(errsqr < maxIKErrorSqr){
                solved = true;
            } else {
                solved = false;
                for(int i=0; i < numJoints(); ++i){
                    joint(i)->q = qorg[i];
                }
                calcForwardKinematics();
            }
        }
    }

    return solved;
}


bool CustomizedJointPath::hasAnalyticalIK()
{
    return (ikTypeId != 0);
}

void Body::calcCMJacobian(Link *base, dmatrix &J)
{
    // prepare subm, submwc
    JointPathPtr jp;
    if (base){
        jp = getJointPath(rootLink(), base);
        Link *skip = jp->joint(0);
        skip->subm = rootLink()->m;
        skip->submwc = rootLink()->m*rootLink()->wc;
        Link *l = rootLink()->child;
        if (l){
            if (l != skip) {
                l->calcSubMassCM();
                skip->subm += l->subm;
                skip->submwc += l->submwc;
            }
            l = l->sibling;
            while(l){
                if (l != skip){
                    l->calcSubMassCM();
                    skip->subm += l->subm;
                    skip->submwc += l->submwc;
                }
                l = l->sibling;
            }
        }
        
        // assuming there is no branch between base and root
        for (int i=1; i<jp->numJoints(); i++){
            l = jp->joint(i);
            l->subm = l->parent->m + l->parent->subm;
            l->submwc = l->parent->m*l->parent->wc + l->parent->submwc;
        }
        
        J.resize(3, numJoints());
    }else{
        rootLink()->calcSubMassCM();
        J.resize(3, numJoints()+6);
    }
    
    // compute Jacobian
    std::vector<int> sgn(numJoints(), 1);
    if (jp) {
        for (int i=0; i<jp->numJoints(); i++) sgn[jp->joint(i)->jointId] = -1;
    }
    
    for (int i=0; i<numJoints(); i++){
        Link *j = joint(i);
        switch(j->jointType){
        case Link::ROTATIONAL_JOINT:
        {
            Vector3 omega(sgn[j->jointId]*j->R*j->a);
            Vector3 arm((j->submwc-j->subm*j->p)/totalMass_);
            Vector3 dp(omega.cross(arm));
            J.col(j->jointId) = dp;
            break;
        }
	case Link::SLIDE_JOINT:
	{
	    Vector3 dp((j->subm/totalMass_)*sgn[j->jointId]*j->R*j->d);
	    J.col(j->jointId) = dp;
	    break;
	}
        default:
            std::cerr << "calcCMJacobian() : unsupported jointType("
                      << j->jointType << ")" << std::endl;
        }
    }
    if (!base){
        int c = numJoints();
        J(0, c  ) = 1.0; J(0, c+1) = 0.0; J(0, c+2) = 0.0;
        J(1, c  ) = 0.0; J(1, c+1) = 1.0; J(1, c+2) = 0.0;
        J(2, c  ) = 0.0; J(2, c+1) = 0.0; J(2, c+2) = 1.0;

        Vector3 dp(rootLink()->submwc/totalMass_ - rootLink()->p);
        J(0, c+3) =    0.0; J(0, c+4) =  dp(2); J(0, c+5) = -dp(1);
        J(1, c+3) = -dp(2); J(1, c+4) =    0.0; J(1, c+5) =  dp(0);
        J(2, c+3) =  dp(1); J(2, c+4) = -dp(0); J(2, c+5) =    0.0;
    }
}

void Body::calcAngularMomentumJacobian(Link *base, dmatrix &H)
{
    // prepare subm, submwc
    JointPathPtr jp;

    dmatrix M;
    calcCMJacobian(base, M);
    M.conservativeResize(3, numJoints());
    M *= totalMass();

    if (base){
        jp = getJointPath(rootLink(), base);
        Link *skip = jp->joint(0);
        skip->subm = rootLink()->m;
        skip->submwc = rootLink()->m*rootLink()->wc;
        Link *l = rootLink()->child;
        if (l){
            if (l != skip) {
                l->calcSubMassCM();
                skip->subm += l->subm;
                skip->submwc += l->submwc;
            }
            l = l->sibling;
            while(l){
                if (l != skip){
                    l->calcSubMassCM();
                    skip->subm += l->subm;
                    skip->submwc += l->submwc;
                }
                l = l->sibling;
            }
        }
        
        // assuming there is no branch between base and root
        for (int i=1; i<jp->numJoints(); i++){
            l = jp->joint(i);
            l->subm = l->parent->m + l->parent->subm;
            l->submwc = l->parent->m*l->parent->wc + l->parent->submwc;
        }
        
        H.resize(3, numJoints());
    }else{
        rootLink()->calcSubMassCM();
        H.resize(3, numJoints()+6);
    }
    
    // compute Jacobian
    std::vector<int> sgn(numJoints(), 1);
    if (jp) {
        for (int i=0; i<jp->numJoints(); i++) sgn[jp->joint(i)->jointId] = -1;
    }
    
    for (int i=0; i<numJoints(); i++){
        Link *j = joint(i);
        switch(j->jointType){
        case Link::ROTATIONAL_JOINT:
        {
            Vector3 omega(sgn[j->jointId]*j->R*j->a);
            Vector3 Mcol = M.col(j->jointId);
            Matrix33 jsubIw;
            j->calcSubMassInertia(jsubIw);
            Vector3 dp = jsubIw*omega;
            if (j->subm!=0) dp += (j->submwc/j->subm).cross(Mcol);
            H.col(j->jointId) = dp;
            break;
        }
        case Link::SLIDE_JOINT:
        {
          if(j->subm!=0){
            Vector3 Mcol =M.col(j->jointId);
            Vector3 dp = (j->submwc/j->subm).cross(Mcol);
            H.col(j->jointId) = dp;
          }
          break;
        }
        default:
            std::cerr << "calcCMJacobian() : unsupported jointType("
                      << j->jointType << ")" << std::endl;
        }
    }
    if (!base){
        int c = numJoints();
        H.block(0, c, 3, 3).setZero();
        Matrix33 Iw;
        rootLink_->calcSubMassInertia(Iw);
        H.block(0, c+3, 3, 3) = Iw;
        Vector3 cm = calcCM();
        Matrix33 cm_cross;
        cm_cross <<
          0.0, -cm(2), cm(1),
          cm(2), 0.0, -cm(0),
          -cm(1), cm(0), 0.0;
        H.block(0,0,3,c) -= cm_cross * M;
    }
}
