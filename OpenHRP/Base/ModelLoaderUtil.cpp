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
/** @file DynamicsSimulator/server/convCORBAUtil.cpp
 *
 */

#include <OpenHRPCommon.h>
#include <stack>
#include "ModelLoaderUtil.h"
#include "Link.h"
#include "Sensor.h"
#include "tvmet3d.h"
#include "quaternion.h"

using namespace OpenHRP;
using namespace std;

static const bool debugMode = false;


static ostream& operator<<(ostream& os, DblSequence_var& data)
{
	int size = data->length();
	for(int i=0; i < size-1; ++i){
		cout << data[i] << ", ";
	}
	cout << data[size-1];

	return os;
}


static ostream& operator<<(ostream& os, DblArray3_var& data)
{
	cout << data[CORBA::ULong(0)] << ", " << data[CORBA::ULong(1)] << ", " << data[CORBA::ULong(2)];
	return os;
}


static ostream& operator<<(ostream& os, DblArray9_var& data)
{
	for(CORBA::ULong i=0; i < 8; ++i){
		cout << data[i] << ", ";
	}
	cout << data[CORBA::ULong(9)];
	return os;
}



static void dumpBodyInfo(BodyInfo_ptr bodyInfo)
{
	cout << "<<< CharacterInfo >>>\n";

	CORBA::String_var charaName = bodyInfo->name();

	cout << "name: " << charaName << "\n";

	LinkInfoSequence_var linkInfoSeq = bodyInfo->links();

	int numLinks = linkInfoSeq->length();
	cout << "num links: " << numLinks << "\n";

	for(int i=0; i < numLinks; ++i){

		LinkInfo linkInfo = linkInfoSeq[i];
		CORBA::String_var linkName = linkInfo.name;

		cout << "<<< LinkInfo: " << linkName << " (index " << i << ") >>>\n";
		cout << "parentIndex: " << linkInfo.parentIndex << "\n";
		cout << "childIndices: " << linkInfo.childIndices[0] << "\n";

		SensorInfoSequence sensorInfoSeq = linkInfo.sensors;

		int numSensors = sensorInfoSeq.length();
		cout << "num sensors: " << numSensors << "\n";

		for(int j=0; j < numSensors; ++j){
			cout << "<<< SensorInfo >>>\n";
			SensorInfo sensorInfo = sensorInfoSeq[j];
			cout << "id: " << sensorInfo.id << "\n";
			cout << "type: " << sensorInfo.type << "\n";

			CORBA::String_var sensorName = sensorInfo.name;
			cout << "name: \"" << sensorName << "\"\n";

			// ##### [TODO] #####
			// maxValue が新IDLから削除されている
			//DblSequence_var maxValue = sensorInfo->maxValue();
			//新IDLでは、maxValue   SensorInfoから削除されている。
			//cout << "maxValue: " << maxValue << "\n";

			DblArray3 translation;
			for( int k = 0 ; k < 3 ; ++k ) translation[k] = sensorInfo.translation[k];
			cout << "translation: " << translation << "\n";

			DblArray4 rotation;
			for( int k = 0 ; k < 4 ; ++k ) rotation[k] = sensorInfo.rotation[k];
			cout << "rotation: " << rotation << "\n";
			
		}
	}
}


static void createSensors(BodyPtr body, Link* link,  SensorInfoSequence iSensors, const matrix33& Rs)
{
	int numSensors = iSensors.length();

	for(int i=0 ; i < numSensors ; ++i )
	{
		SensorInfo iSensor = iSensors[i];

		int id = iSensor.id;
		if(id < 0)
		{
			std::cerr << "Warning:  sensor ID is not given to sensor " << iSensor.name
					  << "of model " << body->modelName << "." << std::endl;
		}
		else
		{
			// センサタイプを判定する
			int sensorType = Sensor::COMMON;

			//switch(iSensor->type()) {
			//switch(iSensor.type) {
			//case ::FORCE_SENSOR:		sensorType = Sensor::FORCE;				break;
			//case ::RATE_GYRO:			sensorType = Sensor::RATE_GYRO;			break;
			//case ::ACCELERATION_SENSOR: sensorType = Sensor::ACCELERATION;		break;
			//case ::PRESSURE_SENSOR:		sensorType = Sensor::PRESSURE;			break;
			//case ::PHOTO_INTERRUPTER:	sensorType = Sensor::PHOTO_INTERRUPTER; break;
			//case ::VISION_SENSOR:		sensorType = Sensor::VISION;			break;
			//case ::TORQUE_SENSOR:		sensorType = Sensor::TORQUE;			break;
			//}

			CORBA::String_var type0 = iSensor.type;
			string type(type0);

			if( type == "Force" )				{ sensorType = Sensor::FORCE; }			// 6軸力センサ
			else if( type == "RateGyro" )		{ sensorType = Sensor::RATE_GYRO; }		// レートジャイロセンサ
			else if( type == "Acceleration" )	{ sensorType = Sensor::ACCELERATION; }	// 加速度センサ
			else if( type == "Vision" )			{ sensorType = Sensor::VISION; }		// ビジョンセンサ

			CORBA::String_var name0 = iSensor.name;
			string name(name0);

			Sensor* sensor = body->createSensor(link, sensorType, id, name);

			if(sensor)
			{
				DblArray3 p;
				for( int j = 0 ; j < 3 ; ++j ) p[j]= iSensor.translation[j];
				sensor->localPos = Rs * vector3( p[0u], p[1u], p[2u] );

				//DblArray4 rot;
				//for( int j = 0 ; j < 4 ; ++j ) rot[j] = iSensor.rotation[j];
				//matrix33 R;
				//getMatrix33FromRowMajorArray( R, rot );

				vector3 rot( iSensor.rotation[0], iSensor.rotation[1], iSensor.rotation[2] );
				matrix33 R = rodrigues( rot, iSensor.rotation[3] );

				sensor->localR = Rs * R;
 			}
		}
	}
}


static inline double getLimitValue(DblSequence limitseq, double defaultValue)
{
	return (limitseq.length() == 0) ? defaultValue : limitseq[0];
}


static OpenHRP::Link* createLink
(BodyPtr body, int index, LinkInfoSequence_var iLinks, const matrix33& parentRs)
{
	LinkInfo iLink = iLinks[index];
	int jointId = iLink.jointId;

	OpenHRP::Link* link = NULL;
	link = new Link;

	CORBA::String_var name0 = iLink.name;
	link->name = string( name0 );
	link->jointId = jointId;

	DblArray3 b;
	for( int i = 0 ; i < 3 ; ++i ) 	b[i] = iLink.translation[i];

	vector3 relPos( b[0u], b[1u], b[2u] );
	link->b = parentRs * relPos;

	vector3 rotAxis( iLink.rotation[0], iLink.rotation[1], iLink.rotation[2] );
	matrix33 R = rodrigues( rotAxis, iLink.rotation[3] );
	link->Rs = (parentRs * R);
	const matrix33& Rs = link->Rs;

	CORBA::String_var jointType = iLink.jointType;
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

	DblArray3 a;
	for( int i = 0 ; i < 3 ; i++ ) a[i] = iLink.jointAxis[i];
	vector3 axis( Rs * vector3( a[0u], a[1u], a[2u] ) );

	if(link->jointType == Link::ROTATIONAL_JOINT){
		link->a = axis;
	} else if(link->jointType == Link::SLIDE_JOINT){
		link->d = axis;
	}

	link->m             = iLink.mass;
	link->Ir            = iLink.rotorInertia;

	//equivalentInertia は、新IDLから削除
	//link->Jm2	        = iLink->equivalentInertia();

	link->gearRatio		= iLink.gearRatio;
	link->rotorResistor	= iLink.rotorResistor;
	link->torqueConst	= iLink.torqueConst;
	
	if (link->Jm2 == 0){
		link->Jm2 = link->Ir * link->gearRatio * link->gearRatio;
	}
	link->encoderPulse	= iLink.encoderPulse;

	DblSequence ulimit  = iLink.ulimit;
	DblSequence llimit  = iLink.llimit;
	DblSequence uvlimit = iLink.uvlimit;
	DblSequence lvlimit = iLink.lvlimit;

	double maxlimit = numeric_limits<double>::max();

	link->ulimit  = getLimitValue(ulimit,  +maxlimit);
	link->llimit  = getLimitValue(llimit,  -maxlimit);
	link->uvlimit = getLimitValue(uvlimit, +maxlimit);
	link->lvlimit = getLimitValue(lvlimit, -maxlimit);

	DblArray3 rc;
	for( int i = 0 ; i < 3 ; ++i ) rc[i] = iLink.centerOfMass[i];
	link->c = Rs * vector3( rc[0u], rc[1u], rc[2u] );

	DblArray9 I;
	for( int i = 0 ; i < 9 ; ++i ) I[i]= iLink.inertia[i];

	matrix33 Io;
	getMatrix33FromRowMajorArray(Io, I);
	link->I = Rs * Io;

	// a stack is used for keeping the same order of children
	std::stack<Link*> children;
	
	//##### [Changed] Link Structure (convert NaryTree to BinaryTree).
	int childNum = iLink.childIndices.length();
	for( int i = 0 ; i < childNum ; i++ )
	{
		int childIndex = iLink.childIndices[i];
		Link* childLink = createLink( body, childIndex, iLinks, Rs );
	    if( childLink )
		{
			children.push( childLink );
		}
	}
	while(!children.empty()){
		link->addChild(children.top());
		children.pop();
	}

	createSensors( body, link, iLink.sensors, Rs );

	return link;
}



BodyPtr OpenHRP::loadBodyFromBodyInfo( BodyInfo_ptr bodyInfo )
{
	if( debugMode )
	{
		dumpBodyInfo( bodyInfo );
	}
	
	BodyPtr body(new Body());

	CORBA::String_var name = bodyInfo->name();
	body->modelName = name;

	int n = bodyInfo->links()->length();
	LinkInfoSequence_var iLinks = bodyInfo->links();

	int rootIndex = -1;

	for(int i=0; i < n; ++i)
	{
		if(iLinks[i].parentIndex < 0)
		{
			if( rootIndex < 0 )
			{
				rootIndex = i;
			} else {
				body = 0; // more than one root !
			}
		}
	}
	if(rootIndex < 0){
		body = 0; // no root !
	}

	if( body ){
		matrix33 Rs( tvmet::identity<matrix33>() );
		Link* rootLink = createLink(body, rootIndex, iLinks, Rs);
		body->setRootLink(rootLink);

		DblArray3 p;
		for( int i = 0 ; i < 3 ; ++i ) p[i] = iLinks[rootIndex].translation[i];
		vector3 pos( p[0u], p[1u], p[2u] );

		DblArray9 R;
		for( int i = 0 ; i < 9 ; ++i ) R[i] = iLinks[rootIndex].rotation[i];

		matrix33 att;
		getMatrix33FromRowMajorArray(att, R);
		body->setDefaultRootPosition(pos, att);

		body->installCustomizer();

		body->initializeConfiguration();
	}

	return body;
}


BodyPtr OpenHRP::loadBodyFromModelLoader(const char *url, CosNaming::NamingContext_var cxt)
{
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
        return BodyPtr();
    } catch(CosNaming::NamingContext::CannotProceed &exc) {
        std::cerr << "Resolve ModelLoader CannotProceed" << std::endl;
    } catch(CosNaming::NamingContext::AlreadyBound &exc) {
        std::cerr << "Resolve ModelLoader InvalidName" << std::endl;
    }

	BodyInfo_var bodyInfo;

	try
	{
		bodyInfo = modelLoader->getBodyInfo( url );
	} catch( CORBA::SystemException& ex ) {
		std::cerr << "CORBA::SystemException raised by ModelLoader: " << ex._rep_id() << std::endl;
		return BodyPtr();
	} catch(ModelLoader::ModelLoaderException& ex){
		std::cerr << "ModelLoaderException : " << ex.description << std::endl;
	}

	if( CORBA::is_nil( bodyInfo ) )
	{
		return BodyPtr();
	}

	return loadBodyFromBodyInfo( bodyInfo );
}


BodyPtr OpenHRP::loadBodyFromModelLoader(const char *url, CORBA_ORB_var orb)
{
    CosNaming::NamingContext_var cxt;
    try {
      CORBA::Object_var	nS = orb->resolve_initial_references("NameService");
      cxt = CosNaming::NamingContext::_narrow(nS);
    } catch(CORBA::SystemException& ex) {
		std::cerr << "NameService doesn't exist" << std::endl;
		return BodyPtr();
	}

	return loadBodyFromModelLoader(url, cxt);
}


BodyPtr OpenHRP::loadBodyFromModelLoader(const char *url, int argc, char *argv[])
{
    CORBA::ORB_var orb = CORBA::ORB_init(argc, argv);
    return loadBodyFromModelLoader(url, orb);
}


BodyPtr OpenHRP::loadBodyFromModelLoader(const char *URL, istringstream &strm)
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
