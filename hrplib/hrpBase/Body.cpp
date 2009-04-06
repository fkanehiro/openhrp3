// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-
/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */
/** \file
	\brief The implementation of the body class 
	\author S.NAKAOKA
*/

#include "Body.h"
#include "Link.h"
#include "LinkPath.h"
#include "Sensor.h"
#include "BodyCustomizerInterface.h"
#include <hrpCollision/ColdetModel.h>

#include <map>
#include <cstdlib>


using namespace hrp;
using namespace tvmet;
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
		
		virtual void onJointPathUpdated();
		
	public:
		
		CustomizedJointPath(Body* body, Link* baseLink, Link* targetLink);
		virtual ~CustomizedJointPath();
		//virtual bool moveLinkTo(const vector3& p, const matrix33& R);
		virtual bool calcInverseKinematics(const vector3& from_p, const matrix33& from_R, const vector3& to_p, const matrix33& to_R);
		virtual bool calcInverseKinematics(const vector3& to_p, const matrix33& to_R);

		virtual bool hasAnalyticalIK();
	};
}


Body::~Body()
{
	if(customizerHandle){
		customizerInterface->destroy(customizerHandle);
	}
	
    delete rootLink_;
    if(invalidLink){
        delete invalidLink;
    }

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
	refCounter = 0;

	rootLink_ = 0;

	customizerHandle = 0;
	customizerInterface = 0;
	bodyHandleEntity.body = this;
	bodyHandle = &bodyHandleEntity;
	
    invalidLink = 0;
}
	

Body::Body() : allSensors(Sensor::NUM_SENSOR_TYPES)
{
	initialize();
	
    rootLink_ = new Link;

    defaultRootPosition = 0.0;
    defaultRootAttitude = identity<matrix33>();
}


Body::Body(const Body& org) : allSensors(Sensor::NUM_SENSOR_TYPES)
{
	initialize();
	
	modelName = org.modelName;

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
			int linkIndex = linkToIndexMap[orgSensor->link];
			Link* newLink = linkTraverse_[linkIndex];
			Sensor* cloneSensor = createSensor(newLink, sensorType, orgSensor->id, orgSensor->name);
			*cloneSensor = *orgSensor;
		}
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


void Body::setDefaultRootPosition(const vector3& p, const matrix33& R)
{
    defaultRootPosition = p;
    defaultRootAttitude = R;
}


void Body::getDefaultRootPosition(vector3& out_p, matrix33& out_R)
{
	out_p = defaultRootPosition;
	out_R = defaultRootAttitude;
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
            if(!invalidLink){
                invalidLink = new Link;
            }
            jointIdToLinkArray[i] = invalidLink;
            std::cerr << "Warning: Model " << modelName <<
                " has empty joint ID in the valid IDs." << std::endl;
        }
    }

    calcTotalMass();

    isStatic_ = (rootLink_->jointType == Link::FIXED_JOINT && numJoints() == 0);
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

    rootLink_->v = 0.0;
    rootLink_->dv = 0.0;
    rootLink_->w = 0.0;
    rootLink_->dw = 0.0;
    rootLink_->vo = 0.0;
    rootLink_->dvo = 0.0;
    
    int n = linkTraverse_.numLinks();
    for(int i=0; i < n; ++i){
        Link* link = linkTraverse_[i];
        link->u = 0.0;
        link->q = 0.0;
        link->dq = 0.0;
        link->ddq = 0.0;
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


vector3 Body::calcCM()
{
    totalMass_ = 0.0;
    
    vector3 mc(0.0);

    int n = linkTraverse_.numLinks();
    for(int i=0; i < n; i++){
        Link* link = linkTraverse_[i];
		link->wc = link->p + link->R * link->c;
        mc += link->m * link->wc;
        totalMass_ += link->m;
    }

    return vector3(mc / totalMass_);
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
	vector3 dvoorg;
	vector3 dworg;
	vector3 root_w_x_v;
	vector3 g(0, 0, 9.8);

	uint nJ = numJoints();
	int totaldof = nJ;
	if( !isStatic_ ) totaldof += 6;

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
	root_w_x_v = cross(rootLink_->w, vector3(rootLink_->vo + cross(rootLink_->w, rootLink_->p)));
	rootLink_->dvo = g - root_w_x_v;   // dv = g, dw = 0
	rootLink_->dw  = 0.0;
	
	setColumnOfMassMatrix(b1, 0);

	if( !isStatic_ ){
		for(int i=0; i < 3; ++i){
			rootLink_->dvo[i] += 1.0;
			setColumnOfMassMatrix(out_M, i);
			rootLink_->dvo[i] -= 1.0;
		}
		for(int i=0; i < 3; ++i){
			rootLink_->dw[i] = 1.0;
			vector3 dw_x_p = cross(rootLink_->dw, rootLink_->p);	//  spatial acceleration caused by ang. acc.
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
	ublas::matrix_column<dmatrix> vb1(b1, 0);
	for(size_t i = 0; i < out_M.size2(); ++i){
		ublas::matrix_column<dmatrix>(out_M, i) -= vb1;
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
    vector3 f;
	vector3 tau;

    calcInverseDynamics(rootLink_, f, tau);

	if( !isStatic_ ){
		tau -= cross(rootLink_->p, f);
		setVector3(f,   out_M, 0, column);
		setVector3(tau, out_M, 3, column);
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
void Body::calcInverseDynamics(Link* ptr, vector3& out_f, vector3& out_tau)
{	
    Link* parent = ptr->parent;
    if(parent){
		vector3 dsv,dsw,sv,sw;

        if(ptr->jointType != Link::FIXED_JOINT){
    		sw  = parent->R * ptr->a;
	    	sv  = cross(ptr->p, sw);
        }else{
            sw = 0.0;
            sv = 0.0;
        }
		dsv = cross(parent->w, sv) + cross(parent->vo, sw);
		dsw = cross(parent->w, sw);

		ptr->dw  = parent->dw  + dsw * ptr->dq + sw * ptr->ddq;
		ptr->dvo = parent->dvo + dsv * ptr->dq + sv * ptr->ddq;

		ptr->sw = sw;
		ptr->sv = sv;
    }
	
	vector3  c,P,L;
	matrix33 I,c_hat;

	c = ptr->R * ptr->c + ptr->p;
	I = ptr->R * ptr->I * trans(ptr->R);
	c_hat = hat(c);
	I += ptr->m * c_hat * trans(c_hat);
	P = ptr->m * (ptr->vo + cross(ptr->w, c));
	L = ptr->m * cross(c, ptr->vo) + I * ptr->w;

    out_f   = ptr->m * (ptr->dvo + cross(ptr->dw, c)) + cross(ptr->w, P);
    out_tau = ptr->m * cross(c, ptr->dvo) + I * ptr->dw + cross(ptr->vo,P) + cross(ptr->w,L);

    if(ptr->child){
		vector3 f_c;
		vector3 tau_c;
		calcInverseDynamics(ptr->child, f_c, tau_c);
		out_f   += f_c;
		out_tau += tau_c;
    }

    ptr->u = dot(ptr->sv, out_f) + dot(ptr->sw, out_tau);

    if(ptr->sibling){
		vector3 f_s;
		vector3 tau_s;
		calcInverseDynamics(ptr->sibling, f_s, tau_s);
		out_f   += f_s;
		out_tau += tau_s;
    }
}


/**
   assuming Link::v,w is already computed by calcForwardKinematics(true);
   assuming Link::wc is already computed by calcCM();
*/
void Body::calcTotalMomentum(vector3& out_P, vector3& out_L)
{
	out_P = 0.0;
	out_L = 0.0;

	vector3 dwc;	// Center of mass speed in world frame
	vector3 P;		// Linear momentum of the link
	vector3 L;		// Angular momentum with respect to the world frame origin 
	vector3 Llocal; // Angular momentum with respect to the center of mass of the link

	int n = linkTraverse_.numLinks();
    for(int i=0; i < n; i++){
        Link* link = linkTraverse_[i];

		dwc = link->v + cross(link->w, vector3(link->R * link->c));

		P   = link->m * dwc;

		//L   = cross(link->wc, P) + link->R * link->I * trans(link->R) * link->w; 
		Llocal = link->I * Mtx_prod(link->R, link->w);
		L      = cross(link->wc, P) + link->R * Llocal; 

		out_P += P;
		out_L += L;
    }
}

void Body::calcForwardKinematics(bool calcVelocity, bool calcAcceleration)
{
    linkTraverse_.calcForwardKinematics(calcVelocity, calcAcceleration);
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
		}
	}
		
	return sensor;
}


void Body::clearSensorValues()
{
    for(int i=0; i < numSensorTypes(); ++i){
        for(int j=0; j < numSensors(i); ++j){
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
        link->fext = 0.0;
        link->tauext = 0.0;
        link->constraintForceArray.clear();
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
    out << "Body: model name = " << modelName
		<< " name = " << name << "\n\n";

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
		loadBodyCustomizersInDefaultDirectories(bodyInterface);
		pluginsInDefaultDirectoriesLoaded = true;
	}
		
	BodyCustomizerInterface* interface = findBodyCustomizer(modelName);

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
		customizerHandle = customizerInterface->create(bodyHandle, modelName.c_str());
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


static BodyInterface bodyInterfaceEntity = {
	hrp::BODY_INTERFACE_VERSION,
	getLinkIndexFromName,
	getLinkName,
	getJointValuePtr,
	getJointVelocityPtr,
	getJointTorqueForcePtr,
};


BodyInterface* Body::bodyInterface = &bodyInterfaceEntity;



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
	ikTypeId = body->customizerInterface->initializeAnalyticIk
		(body->customizerHandle, LinkPath::rootLink()->index, LinkPath::endLink()->index);
}

bool CustomizedJointPath::calcInverseKinematics
(const vector3& from_p, const matrix33& from_R, const vector3& to_p, const matrix33& to_R)
{
	Link* baseLink = LinkPath::rootLink();
	baseLink->p = from_p;
	baseLink->R = from_R;
	if(ikTypeId == 0){
		calcForwardKinematics();
	}
	return calcInverseKinematics(to_p, to_R);
}

//bool CustomizedJointPath::moveLinkTo(const vector3& p, const matrix33& R0)
bool CustomizedJointPath::calcInverseKinematics(const vector3& to_p, const matrix33& to_R0)
{
	bool solved;
	
	if(ikTypeId == 0){

		solved = JointPath::calcInverseKinematics(to_p, to_R0);

	} else {

		std::vector<double> qorg(numJoints());
		
		for(int i=0; i < numJoints(); ++i){
			qorg[i] = joint(i)->q;
		}

		Link* targetLink = LinkPath::endLink();
		Link* baseLink   = LinkPath::rootLink();
		matrix33 to_R(to_R0 * trans(targetLink->Rs));
		vector3 p_relative(trans(baseLink->R) * vector3(to_p - baseLink->p));
		matrix33 R_relative(trans(baseLink->R) * to_R);

		solved = body->customizerInterface->
			calcAnalyticIk(body->customizerHandle, ikTypeId, p_relative, R_relative);

		if(solved){

			calcForwardKinematics();

			vector3 dp(to_p - targetLink->p);
			vector3 omega(omegaFromRot(matrix33(trans(targetLink->R) * to_R)));
			
			double errsqr = dot(dp, dp) + dot(omega, omega);
			
			if(errsqr < maxIkErrorSqr){
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
