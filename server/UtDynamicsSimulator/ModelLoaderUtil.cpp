// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-
/** @file DynamicsSimulator/server/convCORBAUtil.cpp
 *
 */

#include <OpenHRPCommon.h>
#include <stack>
#include "ModelLoaderUtil.h"
#include "Sensor.h"

#include "World.h"
#include "psim.h"

using namespace OpenHRP;
using namespace std;

//#include <fstream>
//static std::ofstream logfile("model.log");
//static std::ofstream logfile;

static void array_to_mat33(DblArray9_slice* a, fMat33& mat)
{
	mat(0,0) = a[0];
	mat(1,0) = a[1];
	mat(2,0) = a[2];
	mat(0,1) = a[3];
	mat(1,1) = a[4];
	mat(2,1) = a[5];
	mat(0,2) = a[6];
	mat(1,2) = a[7];
	mat(2,2) = a[8];
}

static void array_to_vec3(DblArray3_slice* a, fVec3& vec)
{
	vec(0) = a[0];
	vec(1) = a[1];
	vec(2) = a[2];
}


//static void createSensors(OpenHRP::World* world, Joint* jnt,  SensorInfoSequence_var iSensors)
static void createSensors(OpenHRP::World* world, Joint* jnt,  SensorInfoSequence iSensors)
{
	int numSensors = iSensors.length();

	for(int i=0; i < numSensors; ++i)
	{
		SensorInfo iSensor = iSensors[i];

		//int id = iSensor->id();
		int id = iSensor.id;

		if(id < 0){
			std::cerr << "Warning:  sensor ID is not given to sensor " << iSensor.name
					  << "of character " << jnt->CharName() << "." << std::endl;
		} else {

			// センサタイプを判定する
			int sensorType = Sensor::COMMON;

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
			DblArray3_var p = iSensor.translation;
			static fVec3 localPos;
			static fMat33 localR;
			array_to_vec3(p, localPos);
			DblArray9_var rot = iSensor.rotation;
			array_to_mat33(rot, localR);
			world->addSensor(jnt, sensorType, id, name, localPos, localR);
		}
	}
}

static inline double getLimitValue(DblSequence_var limitseq, double defaultValue)
{
	return (limitseq->length() == 0) ? defaultValue : limitseq[0];
}


static Joint* createLink(OpenHRP::World* world, const char* charname, int index, LinkInfoSequence_var iLinks, Joint* pjoint)
{
	Chain* _chain = (Chain*)world->Chain();
	LinkInfo iLink = iLinks[index];

//	logfile << "create: " << iLink->name() << ", jointId = " << iLink->jointId() << endl;
	CORBA::String_var name = iLink.name;
	std::string myname;
	char sep[2];
	sep[0] = charname_separator;
	sep[1] = '\0';
	myname = std::string(name) + std::string(sep) + std::string(charname);
	Joint* jnt = new Joint(myname.c_str());

	_chain->AddJoint(jnt, pjoint);

	int jointId = iLink.jointId;
	jnt->i_joint = jointId;

	CORBA::String_var jointType = iLink.jointType;
	const std::string jt(jointType);

	if(jt == "fixed")
	{
		jnt->j_type = ::JFIXED;
	}
	else if(jt == "free")
	{
		jnt->j_type = ::JFREE;
	}
	else if(jt == "rotate")
	{
		jnt->j_type = ::JROTATE;
	}
	else if(jt == "slide")
	{
		jnt->j_type = ::JSLIDE;
	}
	else
	{
		jnt->j_type = ::JFREE;
	}

	if(jointId < 0)
	{
		if(jnt->j_type == ::JROTATE || jnt->j_type == ::JSLIDE)
		{
			std::cerr << "Warning:  Joint ID is not given to joint " << jnt->name
					  << " of character " << charname << "." << std::endl;
		}
	}

	DblArray3_var t =iLink.translation;
	static fVec3 rel_pos;
	array_to_vec3(t, rel_pos);

	DblArray9_var r = iLink.rotation;
	static fMat33 rel_att;
	array_to_mat33(r, rel_att);

	// joint axis is always set to z axis; use init_att as the origin
	// of the joint axis
	if(jnt->j_type == ::JROTATE || jnt->j_type == ::JSLIDE)
	{
		DblArray3_var a = iLink.jointAxis;
		static fVec3 loc_axis;
		array_to_vec3(a, loc_axis);
//		logfile << "loc_axis = " << loc_axis << endl;
//		logfile << "rel_att = " << rel_att << endl;
//		logfile << "rel_pos = " << rel_pos << endl;
#if 0
		static fMat33 init_att;
		static fVec3 p_axis;
		p_axis.mul(rel_att, loc_axis);  // joint axis in parent frame -> z axis
		static fVec3 x, y;
		x.set(1.0, 0.0, 0.0);
		y.set(0.0, 1.0, 0.0);
		double zx = p_axis*x;
		x -= zx * p_axis;
		double xlen = x.length();
		if(xlen > 1e-8)
		{
			x /= xlen;
			y.cross(p_axis, x);
		}
		else
		{
			double yz = y*p_axis;
			y -= yz * p_axis;
			double ylen = y.length();
			y /= ylen;
			x.cross(y, p_axis);
		}
		init_att(0,0) = x(0); init_att(1,0) = x(1); init_att(2,0) = x(2);
		init_att(0,1) = y(0); init_att(1,1) = y(1); init_att(2,1) = y(2);
		init_att(0,2) = p_axis(0); init_att(1,2) = p_axis(1); init_att(2,2) = p_axis(2);
		if(jnt->j_type == JROTATE)
			jnt->SetRotateJointType(rel_pos, init_att, AXIS_Z);
		else if(jnt->j_type == JSLIDE)
			jnt->SetSlideJointType(rel_pos, init_att, AXIS_Z);
//		logfile << "init_att = " << init_att << endl;
#else
		AxisIndex axis = AXIS_NULL;
		if(loc_axis(0) > 0.95) axis = AXIS_X;
		else if(loc_axis(1) > 0.95) axis = AXIS_Y;
		else if(loc_axis(2) > 0.95) axis = AXIS_Z;
		assert(axis != AXIS_NULL);
		if(jnt->j_type == JROTATE)
			jnt->SetRotateJointType(rel_pos, rel_att, axis);
		else if(jnt->j_type == JSLIDE)
			jnt->SetSlideJointType(rel_pos, rel_att, axis);
#endif
//		logfile << "n_dof = " << jnt->n_dof << endl;
	}
	else if(jnt->j_type == ::JSPHERE)
	{
		jnt->SetSphereJointType(rel_pos, rel_att);
	}
	else if(jnt->j_type == ::JFIXED)
	{
		jnt->SetFixedJointType(rel_pos, rel_att);
	}
	else if(jnt->j_type == ::JFREE)
	{
//		logfile << "rel_pos = " << rel_pos << endl;
//		logfile << "rel_att = " << rel_att << endl;
		jnt->SetFreeJointType(rel_pos, rel_att);
	}
	
	jnt->mass = iLink.mass;

	//double equivalentInertia = iLink.equivalentInertia();

	//if(equivalentInertia == 0.0){
	//	jnt->rotor_inertia = iLink.rotorInertia;
	//	jnt->gear_ratio = iLink.gearRatio;
	//} else {
	//	//jnt->rotor_inertia = equivalentInertia;
	//	jnt->gear_ratio = 1.0;
	//}
		
	//link->Jm2	        = iLink.equivalentInertia();
	//link->torqueConst	= iLink.torqueConst();
	//if (link->Jm2 == 0){
	//	link->Jm2 = link->Ir * link->gearRatio * link->gearRatio;
	//}
	//link->encoderPulse	= iLink.encoderPulse();

	//DblSequence_var ulimit  = iLink.ulimit();
	//DblSequence_var llimit  = iLink.llimit();
	//DblSequence_var uvlimit = iLink.uvlimit();
	//DblSequence_var lvlimit = iLink.lvlimit();

	//double maxlimit = numeric_limits<double>::max();

	//link->ulimit  = getLimitValue(ulimit,  +maxlimit);
	//link->llimit  = getLimitValue(llimit,  -maxlimit);
	//link->uvlimit = getLimitValue(uvlimit, +maxlimit);
	//link->lvlimit = getLimitValue(lvlimit, -maxlimit);

	DblArray3_var rc = iLink.centerOfMass;
	static fVec3 com;
	array_to_vec3(rc, com);
	jnt->loc_com.set(com);

	DblArray9_var I = iLink.inertia;
	static fMat33 inertia;
	array_to_mat33(I, inertia);
	jnt->inertia.set(inertia);

	//int sindex = iLinks[index].sister();
	//	createLink(world, charname, sindex, iLinks, pjoint);

	for( int i = 0 ; i < iLinks[index].childIndices.length() ; i++ ) 
	{
		if( 0 <= iLinks[index].childIndices[i] )
		{
			createLink(world, charname, iLinks[index].childIndices[i], iLinks, jnt);
		}
	}

	createSensors(world, jnt, iLink.sensors);

	return jnt;
}


int OpenHRP::loadBodyFromBodyInfo(World* world, const char* _name, BodyInfo_ptr bodyInfo)
{
//	logfile << "loadBody(" << _name << ")" << endl;
	pSim* _chain = world->Chain();
	_chain->BeginCreateChain(true);
	// no root
	if(!_chain->Root())
	{
		_chain->AddRoot("space");
	}

	CORBA::String_var name = _name;

	int n = bodyInfo->links()->length();
	LinkInfoSequence_var iLinks = bodyInfo->links();
	int failed = true;

	for(int i=0; i < n; ++i)
	{
		if(iLinks[i].parentIndex < 0)  // root of the character
		{
			static fMat33 Rs;
			Rs.identity();
			Joint* r = createLink(world, name, i, iLinks, _chain->Root());
			world->addCharacter(r, _name, bodyInfo->links());
			failed = false;
			break;
		}
	}
#if 0
	int rootIndex = -1;
	if(body){
		matrix33 Rs(tvmet::identity<matrix33>());
		Link* rootLink = createLink(body, rootIndex, iLinks, Rs, importLinkShape);
		body->setRootLink(rootLink);

		DblArray3_var p = iLinks[rootIndex]->translation();
		vector3 pos(p[0u], p[1u], p[2u]);
		DblArray9_var R = iLinks[rootIndex]->rotation();
		matrix33 att;
		getMatrix33FromRowMajorArray(att, R);
		body->setDefaultRootPosition(pos, att);

		body->installCustomizer();

		body->initializeConfiguration();
	}
#endif
	_chain->EndCreateChain();
//	logfile << "end of loadBody" << endl;
//	logfile << "total dof = " << _chain->NumDOF() << endl;
	return failed;
}


int OpenHRP::loadBodyFromModelLoader(World* world, const char* name, const char *url, CosNaming::NamingContext_var cxt)
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
        return 0;
    } catch(CosNaming::NamingContext::CannotProceed &exc) {
        std::cerr << "Resolve ModelLoader CannotProceed" << std::endl;
    } catch(CosNaming::NamingContext::AlreadyBound &exc) {
        std::cerr << "Resolve ModelLoader InvalidName" << std::endl;
    }

	BodyInfo_var bodyInfo;
	try {
		bodyInfo = modelLoader->getBodyInfo(url);
	} catch(CORBA::SystemException& ex) {
		std::cerr << "CORBA::SystemException raised by ModelLoader: " << ex._rep_id() << std::endl;
		return 0;
	} catch(ModelLoader::ModelLoaderException& ex){
		std::cerr << "ModelLoaderException : " << ex.description << std::endl;
	}

	if(CORBA::is_nil(bodyInfo)){
		return 0;
	}

	return loadBodyFromBodyInfo(world, name, bodyInfo);
}


int OpenHRP::loadBodyFromModelLoader(World* world, const char* name, const char *url, CORBA_ORB_var orb)
{
    CosNaming::NamingContext_var cxt;
    try {
      CORBA::Object_var	nS = orb->resolve_initial_references("NameService");
      cxt = CosNaming::NamingContext::_narrow(nS);
    } catch(CORBA::SystemException& ex) {
		std::cerr << "NameService doesn't exist" << std::endl;
		return 0;
	}

	return loadBodyFromModelLoader(world, name, url, cxt);
}


int OpenHRP::loadBodyFromModelLoader(World* world, const char* name, const char *url, int argc, char *argv[])
{
    CORBA::ORB_var orb = CORBA::ORB_init(argc, argv);
    return loadBodyFromModelLoader(world, name, url, orb);
}


int OpenHRP::loadBodyFromModelLoader(World* world, const char* name, const char *URL, istringstream &strm)
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

	int ret = loadBodyFromModelLoader(world, name, URL, argc, argv);

    delete [] argv;

    return ret;
}
