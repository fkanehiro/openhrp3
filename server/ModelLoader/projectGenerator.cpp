// -*- C++ -*-
/*!
 * @file ProjectGeneratorComp.cpp
 * @brief Standalone component
 * @date $Date$
 *
 * $Id$
 */

#include <rtm/Manager.h>
#include <rtm/CorbaNaming.h>
#include <hrpModel/Body.h>
#include <hrpModel/Link.h>
#include <hrpModel/Sensor.h>
#include <hrpModel/ModelLoaderUtil.h>
#include <libxml/encoding.h>
#include <libxml/xmlwriter.h>
#include <string>
#include <sstream>
#include <fstream>
#include <stack>
#include "BodyInfo_impl.h"
#include <sys/stat.h>

using namespace std;
void xmlTextWriterWriteProperty(const xmlTextWriterPtr writer, const std::string name, const std::string value) {
  xmlTextWriterStartElement(writer, BAD_CAST "property");
  xmlTextWriterWriteAttribute(writer, BAD_CAST "name", BAD_CAST name.c_str());
  xmlTextWriterWriteAttribute(writer, BAD_CAST "value", BAD_CAST value.c_str());
  xmlTextWriterEndElement(writer);
}

std::string basename(const std::string name){
  std::string ret = name.substr(name.find_last_of('/')+1);
  return ret.substr(0,ret.find_last_of('.'));
}

// COPY FROM openhrp3/hrplib/hrpModel/ModelLoaderUtil.cpp
class ModelLoaderHelper2
{
public:
  ModelLoaderHelper2() {
  }

  void createSensors(Link* link, const SensorInfoSequence& sensorInfoSeq, const Matrix33& Rs)
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
#if 0
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
#endif
        }
    }
  }

  inline double getLimitValue(DblSequence limitseq, double defaultValue)
  {
        return (limitseq.length() == 0) ? defaultValue : limitseq[0];
  }

  Link* createLink(int index, const Matrix33& parentRs)
  {
    const LinkInfo& linkInfo = linkInfoSeq[index];
    int jointId = linkInfo.jointId;
        
    Link* link = new Link(); //(*createLinkFunc)();
        
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
        link->jointType = Link::FIXED_JOINT;
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
    DblSequence climit = linkInfo.climit;

    double maxlimit = (numeric_limits<double>::max)();

    link->ulimit  = getLimitValue(ulimit,  +maxlimit);
    link->llimit  = getLimitValue(llimit,  -maxlimit);
    link->uvlimit = getLimitValue(uvlimit, +maxlimit);
    link->lvlimit = getLimitValue(lvlimit, -maxlimit);
    link->climit  = getLimitValue(climit,  +maxlimit);

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
    //createLights(link, linkInfo.lights, Rs);
#if 0
    if(collisionDetectionModelLoading){
        createColdetModel(link, linkInfo);
    }
#endif
    return link;
  }

  bool createBody(BodyPtr& body, BodyInfo_impl& bodyInfo)
  {
    this->body = body;

    const char* name = bodyInfo.name();
    body->setModelName(name);
    body->setName(name);

    int n = bodyInfo.links()->length();
    linkInfoSeq = bodyInfo.links();
    shapeInfoSeq = bodyInfo.shapes();
	extraJointInfoSeq = bodyInfo.extraJoints();

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
#if 0
		setExtraJoints();
#endif
        return true;
    }

    return false;
  }
private:
  BodyPtr body;
  LinkInfoSequence_var linkInfoSeq;
  ShapeInfoSequence_var shapeInfoSeq;
  ExtraJointInfoSequence_var extraJointInfoSeq;
};

int main (int argc, char** argv)
{
  std::string output;
  std::vector<std::string> inputs, filenames; // filenames is for conf file
  std::string conf_file_option, robothardware_conf_file_option, integrate("true"), dt("0.005"), timeStep(dt), joint_properties, method("EULER");
  bool use_highgain_mode(true);
  struct stat st;
  bool file_exist_flag = false;

  for (int i = 1; i < argc; ++ i) {
    std::string arg(argv[i]);
    coil::normalize(arg);
    if ( arg == "--output" ) {
      if (++i < argc) output = argv[i];
    } else if ( arg == "--integrate" ) {
      if (++i < argc) integrate = argv[i];
    } else if ( arg == "--dt" ) {
      if (++i < argc) dt = argv[i];
    } else if ( arg == "--timestep" ) {
      if (++i < argc) timeStep = argv[i];
    } else if ( arg == "--conf-file-option" ) {
      if (++i < argc) conf_file_option += std::string("\n") + argv[i];
    } else if ( arg == "--robothardware-conf-file-option" ) {
      if (++i < argc) robothardware_conf_file_option += std::string("\n") + argv[i];
    } else if ( arg == "--joint-properties" ) {
      if (++i < argc) joint_properties = argv[i];
    } else if ( arg == "--use-highgain-mode" ) {
      if (++i < argc) use_highgain_mode = (std::string(argv[i])==std::string("true")?true:false);
    } else if ( arg == "--method" ) {
      if (++i < argc) method = std::string(argv[i]);
    } else if ( arg.find("--gtest_output") == 0  ||arg.find("--text") == 0 || arg.find("__log") == 0 || arg.find("__name") == 0 ) { // skip
    } else {
      inputs.push_back(argv[i]);
    }
  }

  CORBA::ORB_var orb = CORBA::ORB::_nil();

  try
    {
      orb = CORBA::ORB_init(argc, argv);

      CORBA::Object_var obj;

      obj = orb->resolve_initial_references("RootPOA");
      PortableServer::POA_var poa = PortableServer::POA::_narrow(obj);
      if(CORBA::is_nil(poa))
	{
	  throw string("error: failed to narrow root POA.");
	}

      PortableServer::POAManager_var poaManager = poa->the_POAManager();
      if(CORBA::is_nil(poaManager))
	{
	  throw string("error: failed to narrow root POA manager.");
	}

  xmlTextWriterPtr writer;
  if (stat(output.c_str(), &st) == 0) {
    file_exist_flag = true;
  }
  writer = xmlNewTextWriterFilename(output.c_str(), 0);
  xmlTextWriterSetIndent(writer, 4);
  xmlTextWriterStartElement(writer, BAD_CAST "grxui");
  {
    xmlTextWriterStartElement(writer, BAD_CAST "mode");
    xmlTextWriterWriteAttribute(writer, BAD_CAST "name", BAD_CAST "Simulation");
    {
      xmlTextWriterStartElement(writer, BAD_CAST "item");
      xmlTextWriterWriteAttribute(writer, BAD_CAST "class", BAD_CAST "com.generalrobotix.ui.item.GrxSimulationItem");
      xmlTextWriterWriteAttribute(writer, BAD_CAST "name", BAD_CAST "simulationItem");
      {
	xmlTextWriterWriteProperty(writer, "integrate", integrate);
	xmlTextWriterWriteProperty(writer, "timeStep", timeStep);
        xmlTextWriterWriteProperty(writer, "totalTime", "2000000.0");
	xmlTextWriterWriteProperty(writer, "method", method);
      }
      xmlTextWriterEndElement(writer); // item
      // default WAIST offset
      hrp::Vector3 WAIST_offset_pos = hrp::Vector3::Zero();
      Eigen::AngleAxis<double> WAIST_offset_rot = Eigen::AngleAxis<double>(0, hrp::Vector3(0,0,1)); // rotation in VRML is represented by axis + angle
      for (std::vector<std::string>::iterator it = inputs.begin();
	   it != inputs.end();
	   it++) {
        // argument for VRML supports following two mode:
        //  1. xxx.wrl
        //    To specify VRML file. WAIST offsets is 0 transformation.
        //  2. xxx.wrl,0,0,0,0,0,1,0
        //    To specify both VRML and WAIST offsets.
        //    WAIST offset: for example, "0,0,0,0,0,1,0" -> position offset (3dof) + axis for rotation offset (3dof) + angle for rotation offset (1dof)
        coil::vstring filename_arg_str = coil::split(*it, ",");
	std::string filename = filename_arg_str[0];
        filenames.push_back(filename);
        if ( filename_arg_str.size () > 1 ){ // if WAIST offset is specified 
          for (size_t i = 0; i < 3; i++) {
            coil::stringTo(WAIST_offset_pos(i), filename_arg_str[i+1].c_str());
            coil::stringTo(WAIST_offset_rot.axis()(i), filename_arg_str[i+1+3].c_str());
          }
          coil::stringTo(WAIST_offset_rot.angle(), filename_arg_str[1+3+3].c_str());
        }
	hrp::BodyPtr body(new hrp::Body());

        BodyInfo_impl bI(poa);
        bI.loadModelFile(filename.c_str());
        ModelLoaderHelper2 helper;
        helper.createBody(body, bI);

	std::string name = body->name();

	xmlTextWriterStartElement(writer, BAD_CAST "item");
	xmlTextWriterWriteAttribute(writer, BAD_CAST "class", BAD_CAST "com.generalrobotix.ui.item.GrxRTSItem");
	xmlTextWriterWriteAttribute(writer, BAD_CAST "name", BAD_CAST name.c_str());
	xmlTextWriterWriteAttribute(writer, BAD_CAST "select", BAD_CAST "true");

	xmlTextWriterWriteProperty(writer, name+"(Robot)0.period", dt);
        if (use_highgain_mode) {
          xmlTextWriterWriteProperty(writer, "HGcontroller0.period", dt);
          xmlTextWriterWriteProperty(writer, "HGcontroller0.factory", "HGcontroller");
          if (it==inputs.begin()) {
            xmlTextWriterWriteProperty(writer, "connection", "HGcontroller0.qOut:"+name+"(Robot)0.qRef");
            xmlTextWriterWriteProperty(writer, "connection", "HGcontroller0.dqOut:"+name+"(Robot)0.dqRef");
            xmlTextWriterWriteProperty(writer, "connection", "HGcontroller0.ddqOut:"+name+"(Robot)0.ddqRef");
          }
        } else {
          xmlTextWriterWriteProperty(writer, "PDcontroller0.period", dt);
          xmlTextWriterWriteProperty(writer, "PDcontroller0.factory", "PDcontroller");
          if (it==inputs.begin()) {
            xmlTextWriterWriteProperty(writer, "connection", "PDcontroller0.torque:"+name+"(Robot)0.tauRef");
            xmlTextWriterWriteProperty(writer, "connection", ""+name+"(Robot)0.q:PDcontroller0.angle");
          }
        }
	xmlTextWriterEndElement(writer); // item


	xmlTextWriterStartElement(writer, BAD_CAST "item");
	xmlTextWriterWriteAttribute(writer, BAD_CAST "class", BAD_CAST "com.generalrobotix.ui.item.GrxModelItem");
        xmlTextWriterWriteAttribute(writer, BAD_CAST "name", BAD_CAST (basename(*it).c_str()));
	xmlTextWriterWriteAttribute(writer, BAD_CAST "url", BAD_CAST filename.c_str());

	xmlTextWriterWriteProperty(writer, "rtcName", name + "(Robot)0");
        if (use_highgain_mode) {
          xmlTextWriterWriteProperty(writer, "inport", "qRef:JOINT_VALUE");
          xmlTextWriterWriteProperty(writer, "inport", "dqRef:JOINT_VELOCITY");
          xmlTextWriterWriteProperty(writer, "inport", "ddqRef:JOINT_ACCELERATION");
          if (integrate == "false") { // For kinematics only simultion
              xmlTextWriterWriteProperty(writer, "inport", "basePoseRef:"+body->rootLink()->name+":ABS_TRANSFORM");
          }
        } else {
          xmlTextWriterWriteProperty(writer, "inport", "tauRef:JOINT_TORQUE");
        }
	xmlTextWriterWriteProperty(writer, "outport", "q:JOINT_VALUE");
    xmlTextWriterWriteProperty(writer, "outport", "dq:JOINT_VELOCITY");
    xmlTextWriterWriteProperty(writer, "outport", "tau:JOINT_TORQUE");
    xmlTextWriterWriteProperty(writer, "outport", body->rootLink()->name+":"+body->rootLink()->name+":ABS_TRANSFORM");

    // set outport for sensros
    int nforce = body->numSensors(hrp::Sensor::FORCE);
    if ( nforce > 0 ) std::cerr << "hrp::Sensor::FORCE";
    for (int i=0; i<nforce; i++){
        hrp::Sensor *s = body->sensor(hrp::Sensor::FORCE, i);
        // port name and sensor name is same in case of ForceSensor
        xmlTextWriterWriteProperty(writer, "outport", s->name + ":" + s->name + ":FORCE_SENSOR");
        std::cerr << " " << s->name;
    }
    if ( nforce > 0 ) std::cerr << std::endl;
    int ngyro = body->numSensors(hrp::Sensor::RATE_GYRO);
    if ( ngyro > 0 ) std::cerr << "hrp::Sensor::GYRO";
    if(ngyro == 1){
      // port is named with no number when there is only one gyro
      hrp::Sensor *s = body->sensor(hrp::Sensor::RATE_GYRO, 0);
      xmlTextWriterWriteProperty(writer, "outport", s->name + ":" + s->name + ":RATE_GYRO_SENSOR");
      std::cerr << " " << s->name;
    }else{
      for (int i=0; i<ngyro; i++){
        hrp::Sensor *s = body->sensor(hrp::Sensor::RATE_GYRO, i);
        std::stringstream str_strm;
        str_strm << s->name << i << ":" + s->name << ":RATE_GYRO_SENSOR";
        xmlTextWriterWriteProperty(writer, "outport", str_strm.str());
        std::cerr << " " << s->name;
      }
    }
    if ( ngyro > 0 ) std::cerr << std::endl;
    int nacc = body->numSensors(hrp::Sensor::ACCELERATION);
    if ( nacc > 0 ) std::cerr << "hrp::Sensor::ACCELERATION";
    if(nacc == 1){
      // port is named with no number when there is only one acc
      hrp::Sensor *s = body->sensor(hrp::Sensor::ACCELERATION, 0);      
      xmlTextWriterWriteProperty(writer, "outport", s->name + ":" + s->name + ":ACCELERATION_SENSOR");
      std::cerr << " " << s->name;
    }else{
      for (int i=0; i<nacc; i++){
        hrp::Sensor *s = body->sensor(hrp::Sensor::ACCELERATION, i);
        std::stringstream str_strm;
        str_strm << s->name << i << ":" << s->name << ":ACCELERATION_SENSOR";
        xmlTextWriterWriteProperty(writer, "outport", str_strm.str());
        std::cerr << " " << s->name;
      }
    }
    if ( nacc > 0 ) std::cerr << std::endl;
    
	//
	std::string root_name = body->rootLink()->name;
	xmlTextWriterWriteProperty(writer, root_name+".NumOfAABB", "1");

        // write waist pos and rot by considering both VRML original WAIST and WAIST_offset_pos and WAIST_offset_rot from arguments
	std::ostringstream os;
	os << body->rootLink()->p[0] + WAIST_offset_pos(0) << "  "
	   << body->rootLink()->p[1] + WAIST_offset_pos(1) << "  "
	   << body->rootLink()->p[2] + WAIST_offset_pos(2); // 10cm margin
	xmlTextWriterWriteProperty(writer, root_name+".translation", os.str());
        os.str(""); // reset ostringstream
        Eigen::AngleAxis<double> tmpAA = Eigen::AngleAxis<double>(hrp::Matrix33(body->rootLink()->R * WAIST_offset_rot.toRotationMatrix()));
        os << tmpAA.axis()(0) << " "
           << tmpAA.axis()(1) << " "
           << tmpAA.axis()(2) << " "
           << tmpAA.angle();
        xmlTextWriterWriteProperty(writer, root_name+".rotation", os.str());

	if ( ! body->isStaticModel() ) {
	  xmlTextWriterWriteProperty(writer, root_name+".mode", "Torque");
	  xmlTextWriterWriteProperty(writer, "controller", basename(output));
	}

        // store joint properties
        //   [property 1],[value 1],....
        //   ex. --joint-properties RARM_JOINT0.angle,0.0,RARM_JOINT2.mode,HighGain,...
        coil::vstring joint_properties_arg_str = coil::split(joint_properties, ",");
        std::map <std::string, std::string> joint_properties_map;
        for (size_t i = 0; i < joint_properties_arg_str.size()/2; i++) {
          joint_properties_map.insert(std::pair<std::string, std::string>(joint_properties_arg_str[i*2], joint_properties_arg_str[i*2+1]));
        }
        if ( body->numJoints() > 0 ) std::cerr << "hrp::Joint";
	for(unsigned int i = 0; i < body->numJoints(); i++){
	  if ( body->joint(i)->index > 0 ) {
	    std::cerr << " " << body->joint(i)->name << "(" << body->joint(i)->jointId << ")";
	    std::string joint_name = body->joint(i)->name;
            std::string j_property = joint_name+".angle";
	    xmlTextWriterWriteProperty(writer, j_property,
                                       (joint_properties_map.find(j_property) != joint_properties_map.end()) ? joint_properties_map[j_property] : "0.0");
            j_property = joint_name+".mode";
	    xmlTextWriterWriteProperty(writer, j_property,
                                       (joint_properties_map.find(j_property) != joint_properties_map.end()) ? joint_properties_map[j_property] : (use_highgain_mode?"HighGain":"Torque"));
            j_property = joint_name+".NumOfAABB";
	    xmlTextWriterWriteProperty(writer, j_property,
                                       (joint_properties_map.find(j_property) != joint_properties_map.end()) ? joint_properties_map[j_property] : "1");
	  }
	}
	xmlTextWriterEndElement(writer); // item
        if ( body->numJoints() > 0 ) std::cerr << std::endl;

	//
        // comment out self collision settings according to issues at https://github.com/fkanehiro/hrpsys-base/issues/122
	// xmlTextWriterStartElement(writer, BAD_CAST "item");
	// xmlTextWriterWriteAttribute(writer, BAD_CAST "class", BAD_CAST "com.generalrobotix.ui.item.GrxCollisionPairItem");
	// xmlTextWriterWriteAttribute(writer, BAD_CAST "name", BAD_CAST std::string("CP#"+name).c_str());
	// xmlTextWriterWriteAttribute(writer, BAD_CAST "select", BAD_CAST "true");
	// {
	//   xmlTextWriterWriteProperty(writer, "springConstant", "0 0 0 0 0 0");
	//   xmlTextWriterWriteProperty(writer, "slidingFriction", "0.5");
	//   xmlTextWriterWriteProperty(writer, "jointName2", "");
	//   xmlTextWriterWriteProperty(writer, "jointName1", "");
	//   xmlTextWriterWriteProperty(writer, "damperConstant", "0 0 0 0 0 0");
	//   xmlTextWriterWriteProperty(writer, "objectName2", name);
	//   xmlTextWriterWriteProperty(writer, "objectName1", name);
	//   xmlTextWriterWriteProperty(writer, "springDamperModel", "false");
	//   xmlTextWriterWriteProperty(writer, "staticFriction", "0.5");
	// }
	// xmlTextWriterEndElement(writer); // item

	xmlTextWriterStartElement(writer, BAD_CAST "view");
	xmlTextWriterWriteAttribute(writer, BAD_CAST "class", BAD_CAST "com.generalrobotix.ui.view.GrxRobotHardwareClientView");
	xmlTextWriterWriteAttribute(writer, BAD_CAST "name", BAD_CAST "RobotHardware RTC Client");
	xmlTextWriterWriteProperty(writer, "robotHost", "localhost");
	xmlTextWriterWriteProperty(writer, "StateHolderRTC", "StateHolder0");
	xmlTextWriterWriteProperty(writer, "interval", "100");
	xmlTextWriterWriteProperty(writer, "RobotHardwareServiceRTC", "RobotHardware0");
	xmlTextWriterWriteProperty(writer, "robotPort", "2809");
	xmlTextWriterWriteProperty(writer, "ROBOT", name.c_str());
	xmlTextWriterEndElement(writer); // item

	xmlTextWriterStartElement(writer, BAD_CAST "view");
	xmlTextWriterWriteAttribute(writer, BAD_CAST "class", BAD_CAST "com.generalrobotix.ui.view.Grx3DView");
	xmlTextWriterWriteAttribute(writer, BAD_CAST "name", BAD_CAST "3DView");
	xmlTextWriterWriteProperty(writer, "view.mode", "Room");
	xmlTextWriterWriteProperty(writer, "showCoM", "false");
	xmlTextWriterWriteProperty(writer, "showCoMonFloor", "false");
	xmlTextWriterWriteProperty(writer, "showDistance", "false");
	xmlTextWriterWriteProperty(writer, "showIntersection", "false");
	xmlTextWriterWriteProperty(writer, "eyeHomePosition", "-0.70711 -0 0.70711 2 0.70711 -0 0.70711 2 0 1 0 0.8 0 0 0 1 ");
	xmlTextWriterWriteProperty(writer, "showCollision", "true");
	xmlTextWriterWriteProperty(writer, "showActualState", "true");
	xmlTextWriterWriteProperty(writer, "showScale", "true");
	xmlTextWriterEndElement(writer); // view
      }

      {
	for (std::vector<std::string>::iterator it1 = inputs.begin(); it1 != inputs.end(); it1++) {
	  std::string name1 = basename(*it1);
	  for (std::vector<std::string>::iterator it2 = it1+1; it2 != inputs.end(); it2++) {
	    std::string name2 = basename(*it2);

	    xmlTextWriterStartElement(writer, BAD_CAST "item");
	    xmlTextWriterWriteAttribute(writer, BAD_CAST "class", BAD_CAST "com.generalrobotix.ui.item.GrxCollisionPairItem");
	    xmlTextWriterWriteAttribute(writer, BAD_CAST "name", BAD_CAST std::string("CP#"+name2+"_#"+name1+"_").c_str());
	    {
	      xmlTextWriterWriteProperty(writer, "springConstant", "0 0 0 0 0 0");
	      xmlTextWriterWriteProperty(writer, "slidingFriction", "0.5");
	      xmlTextWriterWriteProperty(writer, "jointName2", "");
	      xmlTextWriterWriteProperty(writer, "jointName1", "");
	      xmlTextWriterWriteProperty(writer, "damperConstant", "0 0 0 0 0 0");
	      xmlTextWriterWriteProperty(writer, "objectName2", name2);
	      xmlTextWriterWriteProperty(writer, "objectName1", name1);
	      xmlTextWriterWriteProperty(writer, "springDamperModel", "false");
	      xmlTextWriterWriteProperty(writer, "staticFriction", "0.5");
	    }
	    xmlTextWriterEndElement(writer); // item
	  }
	}
      }
    }
    xmlTextWriterEndElement(writer); // mode
  }
  xmlTextWriterEndElement(writer); // grxui

  xmlFreeTextWriter(writer);

  if (file_exist_flag) {
    std::cerr << "\033[1;31mOver\033[0m";
  }
  std::cerr << "Writing project files to .... " << output << std::endl;
  {
      std::string conf_file = output.substr(0,output.find_last_of('.'))+".conf";
      if (stat(conf_file.c_str(), &st) == 0) {
        std::cerr << "\033[1;31mOver\033[0m";
      }
      std::fstream s(conf_file.c_str(), std::ios::out);
  
      s << "model: file://" << filenames[0] << std::endl;
      s << "exec_cxt.periodic.rate: " << static_cast<size_t>(1/atof(dt.c_str())+0.5) << std::endl; // rounding to specify integer rate value
      s << "dt: " << dt << std::endl;
      s << conf_file_option << std::endl;
      std::cerr << "Writing conf files to ....... " << conf_file << std::endl;
  }

  {
      std::string conf_file = output.substr(0,output.find_last_of('.'))+".RobotHardware.conf";
      if (stat(conf_file.c_str(), &st) == 0) {
        std::cerr << "\033[1;31mOver\033[0m";
      }
      std::fstream s(conf_file.c_str(), std::ios::out);
  
      s << "model: file://" << filenames[0] << std::endl;
      s << "exec_cxt.periodic.type: hrpExecutionContext" << std::endl;
      s << "exec_cxt.periodic.rate: " << static_cast<size_t>(1/atof(dt.c_str())+0.5) << std::endl; // rounding to specify integer rate value
      s << robothardware_conf_file_option << std::endl;
      std::cerr << "Writing hardware files to ... " << conf_file << std::endl;
  }

    }
  catch (CORBA::SystemException& ex)
    {
      cerr << ex._rep_id() << endl;
    }
  catch (const string& error)
    {
      cerr << error << endl;
    }

  try
    {
      orb->destroy();
    }
  catch(...)
    {

    }

  return 0;
}
