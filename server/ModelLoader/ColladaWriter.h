// -*- coding: utf-8 -*-
// Copyright (C) 2011 University of Tokyo, General Robotix Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
/*!
   @file ColladaWriter.h
   @brief
   @author Rosen Diankov (rosen.diankov@gmail.com)

   Exports the CORBA BodyInfo to COLLADA format. Used OpenRAVE files for reference.
 */

#ifndef OPENHRP_COLLADA_WRITER_H
#define OPENHRP_COLLADA_WRITER_H

#include "ColladaUtil.h"
#include <hrpUtil/MatrixSolvers.h>

using namespace std;
using namespace ColladaUtil;

#include <hrpCorba/ViewSimulator.hh>

//#include <zip.h> // for saving compressed files
//#ifdef _WIN32
//#include <iowin32.h>
//#else
//#include <unistd.h>
//#endif

struct ManipulatorInfo
{
    ManipulatorInfo() {
        rotation[0] = rotation[1] = rotation[2] = rotation[3] = 0;
        translation[0] = translation[1] = translation[2] = 0;
    }
    std::string name;
    std::string basename, effectorname;
    std::list<std::string> grippernames;
    std::list<std::string> gripperdir;
    // manipulator coordinate system
    DblArray4 rotation;
    DblArray3 translation;
};

class ColladaWriter : public daeErrorHandler
{
    static bool ComparePair(const std::pair<int,int>& p0,const std::pair<int,int>& p1)
    {
        return p0.second < p1.second;
    }

public:
    struct SCENE
    {
        domVisual_sceneRef vscene;
        domKinematics_sceneRef kscene;
        domPhysics_sceneRef pscene;
        domInstance_with_extraRef viscene;
        domInstance_kinematics_sceneRef kiscene;
        domInstance_with_extraRef piscene;
    };

    struct LINKOUTPUT
    {
        list<pair<int,std::string> > listusedlinks;
        daeElementRef plink;
        domNodeRef pnode;
    };

    struct physics_model_output
    {
        domPhysics_modelRef pmodel;
        std::vector<std::string > vrigidbodysids; ///< same ordering as the physics indices
    };

    struct kinematics_model_output
    {
        struct axis_output
        {
            //axis_output(const string& sid, KinBody::JointConstPtr pjoint, int iaxis) : sid(sid), pjoint(pjoint), iaxis(iaxis) {}
            axis_output() : iaxis(0) {
            }
            string sid; // joint/axis
            string nodesid;
            int ijoint;
            int iaxis;
            string jointnodesid;
        };
        domKinematics_modelRef kmodel;
        std::vector<axis_output> vaxissids; ///< no ordering
        std::vector<std::string > vlinksids; ///< same ordering as the link indices
        std::map<std::string, int> maplinknames, mapjointnames;
    };

    struct axis_sids
    {
        axis_sids(const string& axissid, const string& valuesid, const string& jointnodesid) : axissid(axissid), valuesid(valuesid), jointnodesid(jointnodesid) {
        }
        string axissid, valuesid, jointnodesid;
    };

    struct instance_kinematics_model_output
    {
        domInstance_kinematics_modelRef ikm;
        std::vector<axis_sids> vaxissids;
        boost::shared_ptr<kinematics_model_output> kmout;
        std::vector<std::pair<std::string,std::string> > vkinematicsbindings; // node and kinematics model bindings
    };

    struct instance_articulated_system_output
    {
        domInstance_articulated_systemRef ias;
        std::vector<axis_sids> vaxissids;
        //std::vector<std::string > vlinksids;
        //std::map<std::string, int> maplinknames;
        std::vector<std::pair<std::string,std::string> > vkinematicsbindings;
    };

    struct instance_physics_model_output
    {
        domInstance_physics_modelRef ipm;
        boost::shared_ptr<physics_model_output> pmout;
    };

    struct kinbody_models
    {
        std::string xmlfilename, kinematicsgeometryhash;
        boost::shared_ptr<kinematics_model_output> kmout;
        boost::shared_ptr<physics_model_output> pmout;
    };

    ColladaWriter(const std::list<ManipulatorInfo>& listmanipulators, const char* comment_str) : _dom(NULL) {
        _listmanipulators = listmanipulators;
        daeErrorHandler::setErrorHandler(this);
        COLLADALOG_INFO(str(boost::format("init COLLADA writer version: %s, namespace: %s")%COLLADA_VERSION%COLLADA_NAMESPACE));
        _collada.reset(new DAE);
        _collada->setIOPlugin( NULL );
        _collada->setDatabase( NULL );

        const char* documentName = "openrave_snapshot";
        daeInt error = _collada->getDatabase()->insertDocument(documentName, &_doc ); // also creates a collada root
        BOOST_ASSERT( error == DAE_OK && !!_doc );
        _dom = daeSafeCast<domCOLLADA>(_doc->getDomRoot());

        //create the required asset tag
        domAssetRef asset = daeSafeCast<domAsset>( _dom->add( COLLADA_ELEMENT_ASSET ) );
        {
            // facet becomes owned by locale, so no need to explicitly delete
            boost::posix_time::time_facet* facet = new boost::posix_time::time_facet("%Y-%m-%dT%H:%M:%s");
            std::stringstream ss;
            ss.imbue(std::locale(ss.getloc(), facet));
            ss << boost::posix_time::second_clock::local_time();

            domAsset::domCreatedRef created = daeSafeCast<domAsset::domCreated>( asset->add( COLLADA_ELEMENT_CREATED ) );
            created->setValue(ss.str().c_str());
            domAsset::domModifiedRef modified = daeSafeCast<domAsset::domModified>( asset->add( COLLADA_ELEMENT_MODIFIED ) );
            modified->setValue(ss.str().c_str());

            domAsset::domContributorRef contrib = daeSafeCast<domAsset::domContributor>( asset->add( COLLADA_TYPE_CONTRIBUTOR ) );
            domAsset::domContributor::domAuthoring_toolRef authoringtool = daeSafeCast<domAsset::domContributor::domAuthoring_tool>( contrib->add( COLLADA_ELEMENT_AUTHORING_TOOL ) );
            authoringtool->setValue("OpenRAVE Collada Writer");
            domAsset::domContributor::domCommentsRef comments = daeSafeCast<domAsset::domContributor::domComments>( contrib->add( COLLADA_ELEMENT_COMMENTS ) );
            comments->setValue(comment_str);


            domAsset::domUnitRef units = daeSafeCast<domAsset::domUnit>( asset->add( COLLADA_ELEMENT_UNIT ) );
            units->setMeter(1);
            units->setName("meter");

            domAsset::domUp_axisRef zup = daeSafeCast<domAsset::domUp_axis>( asset->add( COLLADA_ELEMENT_UP_AXIS ) );
            zup->setValue(UP_AXIS_Z_UP);
        }

        _globalscene = _dom->getScene();
        if( !_globalscene ) {
            _globalscene = daeSafeCast<domCOLLADA::domScene>( _dom->add( COLLADA_ELEMENT_SCENE ) );
        }

        _visualScenesLib = daeSafeCast<domLibrary_visual_scenes>(_dom->add(COLLADA_ELEMENT_LIBRARY_VISUAL_SCENES));
        _visualScenesLib->setId("vscenes");
        _geometriesLib = daeSafeCast<domLibrary_geometries>(_dom->add(COLLADA_ELEMENT_LIBRARY_GEOMETRIES));
        _geometriesLib->setId("geometries");
        _effectsLib = daeSafeCast<domLibrary_effects>(_dom->add(COLLADA_ELEMENT_LIBRARY_EFFECTS));
        _effectsLib->setId("effects");
        _materialsLib = daeSafeCast<domLibrary_materials>(_dom->add(COLLADA_ELEMENT_LIBRARY_MATERIALS));
        _materialsLib->setId("materials");
        _kinematicsModelsLib = daeSafeCast<domLibrary_kinematics_models>(_dom->add(COLLADA_ELEMENT_LIBRARY_KINEMATICS_MODELS));
        _kinematicsModelsLib->setId("kmodels");
        _articulatedSystemsLib = daeSafeCast<domLibrary_articulated_systems>(_dom->add(COLLADA_ELEMENT_LIBRARY_ARTICULATED_SYSTEMS));
        _articulatedSystemsLib->setId("asystems");
        _kinematicsScenesLib = daeSafeCast<domLibrary_kinematics_scenes>(_dom->add(COLLADA_ELEMENT_LIBRARY_KINEMATICS_SCENES));
        _kinematicsScenesLib->setId("kscenes");
        _physicsScenesLib = daeSafeCast<domLibrary_physics_scenes>(_dom->add(COLLADA_ELEMENT_LIBRARY_PHYSICS_SCENES));
        _physicsScenesLib->setId("pscenes");
        _physicsModelsLib = daeSafeCast<domLibrary_physics_models>(_dom->add(COLLADA_ELEMENT_LIBRARY_PHYSICS_MODELS));
        _physicsModelsLib->setId("pmodels");
        domExtraRef pextra_library_sensors = daeSafeCast<domExtra>(_dom->add(COLLADA_ELEMENT_EXTRA));
        pextra_library_sensors->setId("sensors");
        pextra_library_sensors->setType("library_sensors");
        _sensorsLib = daeSafeCast<domTechnique>(pextra_library_sensors->add(COLLADA_ELEMENT_TECHNIQUE));
        _sensorsLib->setProfile("OpenRAVE");
        _nextsensorid = 0;
        domExtraRef pextra_library_actuators = daeSafeCast<domExtra>(_dom->add(COLLADA_ELEMENT_EXTRA));
        pextra_library_actuators->setId("actuators");
        pextra_library_actuators->setType("library_actuators");
        _actuatorsLib = daeSafeCast<domTechnique>(pextra_library_actuators->add(COLLADA_ELEMENT_TECHNIQUE));
        _actuatorsLib->setProfile("OpenRAVE");
        _nextactuatorid = 0;
    }
    virtual ~ColladaWriter() {
        _collada.reset();
        DAE::cleanup();
    }


    /// Write down a COLLADA file
    virtual void Save(const string& filename)
    {
        bool bcompress = filename.size() >= 4 && filename[filename.size()-4] == '.' && ::tolower(filename[filename.size()-3]) == 'z' && ::tolower(filename[filename.size()-2]) == 'a' && ::tolower(filename[filename.size()-1]) == 'e';
        if( !bcompress ) {
            if(!_collada->writeTo(_doc->getDocumentURI()->getURI(), filename.c_str()) ) {
                throw ModelLoader::ModelLoaderException(str(boost::format("failed to save collada file to %s")%filename).c_str());
            }
            return;
        }
        COLLADALOG_WARN("cannot save as compressed collada file\n");
    }

    virtual bool Write(BodyInfo_impl* bodyInfo) { //, ShapeSetInfo_impl* shapeSetInfo) {
        _CreateScene();
        boost::shared_ptr<instance_articulated_system_output> iasout = _WriteRobot(bodyInfo);
        if( !iasout ) {
            return false;
        }
        _WriteBindingsInstance_kinematics_scene(_scene.kiscene,bodyInfo,iasout->vaxissids,iasout->vkinematicsbindings);
    }

    /// \brief Write kinematic body in a given scene
    virtual boost::shared_ptr<instance_articulated_system_output> _WriteRobot(BodyInfo_impl* bodyInfo)
    {
        COLLADALOG_VERBOSE(str(boost::format("writing robot as instance_articulated_system (%d) %s\n")%_GetRobotId(bodyInfo)%bodyInfo->name()));
        string asid = str(boost::format("robot%d")%_GetRobotId(bodyInfo));
        string askid = str(boost::format("%s_kinematics")%asid);
        string asmid = str(boost::format("%s_motion")%asid);
        string iassid = str(boost::format("%s_inst")%asmid);

        domInstance_articulated_systemRef ias = daeSafeCast<domInstance_articulated_system>(_scene.kscene->add(COLLADA_ELEMENT_INSTANCE_ARTICULATED_SYSTEM));
        ias->setSid(iassid.c_str());
        ias->setUrl((string("#")+asmid).c_str());
        ias->setName(bodyInfo->name());

        boost::shared_ptr<instance_articulated_system_output> iasout(new instance_articulated_system_output());
        iasout->ias = ias;

        // motion info
        domArticulated_systemRef articulated_system_motion = daeSafeCast<domArticulated_system>(_articulatedSystemsLib->add(COLLADA_ELEMENT_ARTICULATED_SYSTEM));
        articulated_system_motion->setId(asmid.c_str());
        domMotionRef motion = daeSafeCast<domMotion>(articulated_system_motion->add(COLLADA_ELEMENT_MOTION));
        domMotion_techniqueRef mt = daeSafeCast<domMotion_technique>(motion->add(COLLADA_ELEMENT_TECHNIQUE_COMMON));
        domInstance_articulated_systemRef ias_motion = daeSafeCast<domInstance_articulated_system>(motion->add(COLLADA_ELEMENT_INSTANCE_ARTICULATED_SYSTEM));
        ias_motion->setUrl(str(boost::format("#%s")%askid).c_str());

        // kinematics info
        domArticulated_systemRef articulated_system_kinematics = daeSafeCast<domArticulated_system>(_articulatedSystemsLib->add(COLLADA_ELEMENT_ARTICULATED_SYSTEM));
        articulated_system_kinematics->setId(askid.c_str());
        domKinematicsRef kinematics = daeSafeCast<domKinematics>(articulated_system_kinematics->add(COLLADA_ELEMENT_KINEMATICS));
        domKinematics_techniqueRef kt = daeSafeCast<domKinematics_technique>(kinematics->add(COLLADA_ELEMENT_TECHNIQUE_COMMON));

        boost::shared_ptr<instance_kinematics_model_output> ikmout = _WriteInstance_kinematics_model(bodyInfo,kinematics,askid);
        LinkInfoSequence_var links = bodyInfo->links();
        for(size_t idof = 0; idof < ikmout->vaxissids.size(); ++idof) {
            string axis_infosid = str(boost::format("axis_info_inst%d")%idof);
            LinkInfo& pjoint = links[ikmout->kmout->vaxissids.at(idof).ijoint];
            string jointType(CORBA::String_var(pjoint.jointType));
            int iaxis = ikmout->kmout->vaxissids.at(idof).iaxis;

            //  Kinematics axis info
            domKinematics_axis_infoRef kai = daeSafeCast<domKinematics_axis_info>(kt->add(COLLADA_ELEMENT_AXIS_INFO));
            kai->setAxis(str(boost::format("%s/%s")%ikmout->kmout->kmodel->getID()%ikmout->kmout->vaxissids.at(idof).sid).c_str());
            kai->setSid(axis_infosid.c_str());
            domCommon_bool_or_paramRef active = daeSafeCast<domCommon_bool_or_param>(kai->add(COLLADA_ELEMENT_ACTIVE));
            daeSafeCast<domCommon_bool_or_param::domBool>(active->add(COLLADA_ELEMENT_BOOL))->setValue(jointType != "fixed");
            domCommon_bool_or_paramRef locked = daeSafeCast<domCommon_bool_or_param>(kai->add(COLLADA_ELEMENT_LOCKED));
            daeSafeCast<domCommon_bool_or_param::domBool>(locked->add(COLLADA_ELEMENT_BOOL))->setValue(false);

            dReal fmult = jointType == "rotate" ? (180.0/M_PI) : 1.0;
            vector<dReal> vmin, vmax;
            if( jointType == "fixed" ) {
                vmin.push_back(0);
                vmax.push_back(0);
            }
            else {
                if( pjoint.llimit.length() > 0 ) {
                    vmin.push_back(fmult*pjoint.llimit[0]);
                }
                if( pjoint.ulimit.length() > 0 ) {
                    vmax.push_back(fmult*pjoint.ulimit[0]);
                }
            }

            if( vmin.size() > 0 || vmax.size() > 0 ) {
                domKinematics_limitsRef plimits = daeSafeCast<domKinematics_limits>(kai->add(COLLADA_ELEMENT_LIMITS));
                if( vmin.size() > 0 ) {
                    daeSafeCast<domCommon_float_or_param::domFloat>(plimits->add(COLLADA_ELEMENT_MIN)->add(COLLADA_ELEMENT_FLOAT))->setValue(vmin[0]);
                }
                if( vmax.size() > 0 ) {
                    daeSafeCast<domCommon_float_or_param::domFloat>(plimits->add(COLLADA_ELEMENT_MAX)->add(COLLADA_ELEMENT_FLOAT))->setValue(vmax[0]);
                }
            }

            //  Motion axis info
            domMotion_axis_infoRef mai = daeSafeCast<domMotion_axis_info>(mt->add(COLLADA_ELEMENT_AXIS_INFO));
            mai->setAxis(str(boost::format("%s/%s")%askid%axis_infosid).c_str());
            if( pjoint.uvlimit.length() > 0 ) {
                domCommon_float_or_paramRef speed = daeSafeCast<domCommon_float_or_param>(mai->add(COLLADA_ELEMENT_SPEED));
                daeSafeCast<domCommon_float_or_param::domFloat>(speed->add(COLLADA_ELEMENT_FLOAT))->setValue(pjoint.uvlimit[0]);
            }
            //domCommon_float_or_paramRef accel = daeSafeCast<domCommon_float_or_param>(mai->add(COLLADA_ELEMENT_ACCELERATION));
            //daeSafeCast<domCommon_float_or_param::domFloat>(accel->add(COLLADA_ELEMENT_FLOAT))->setValue(0);
        }

        // write the bindings
        string asmsym = str(boost::format("%s_%s")%asmid%ikmout->ikm->getSid());
        string assym = str(boost::format("%s_%s")%_scene.kscene->getID()%ikmout->ikm->getSid());
        for(std::vector<std::pair<std::string,std::string> >::iterator it = ikmout->vkinematicsbindings.begin(); it != ikmout->vkinematicsbindings.end(); ++it) {
            domKinematics_newparamRef abm = daeSafeCast<domKinematics_newparam>(ias_motion->add(COLLADA_ELEMENT_NEWPARAM));
            abm->setSid(asmsym.c_str());
            daeSafeCast<domKinematics_newparam::domSIDREF>(abm->add(COLLADA_ELEMENT_SIDREF))->setValue(str(boost::format("%s/%s")%askid%it->first).c_str());
            domKinematics_newparamRef ab = daeSafeCast<domKinematics_newparam>(ias->add(COLLADA_ELEMENT_NEWPARAM));
            ab->setSid(assym.c_str());
            daeSafeCast<domKinematics_newparam::domSIDREF>(ab->add(COLLADA_ELEMENT_SIDREF))->setValue(str(boost::format("%s/%s")%asmid%asmsym).c_str());
            iasout->vkinematicsbindings.push_back(make_pair(string(ab->getSid()), it->second));
        }
        for(size_t idof = 0; idof < ikmout->vaxissids.size(); ++idof) {
            const axis_sids& kas = ikmout->vaxissids.at(idof);
            domKinematics_newparamRef abm = daeSafeCast<domKinematics_newparam>(ias_motion->add(COLLADA_ELEMENT_NEWPARAM));
            abm->setSid(str(boost::format("%s_%s")%asmid%kas.axissid).c_str());
            daeSafeCast<domKinematics_newparam::domSIDREF>(abm->add(COLLADA_ELEMENT_SIDREF))->setValue(str(boost::format("%s/%s")%askid%kas.axissid).c_str());
            domKinematics_newparamRef ab = daeSafeCast<domKinematics_newparam>(ias->add(COLLADA_ELEMENT_NEWPARAM));
            ab->setSid(str(boost::format("%s_%s")%assym%kas.axissid).c_str());
            daeSafeCast<domKinematics_newparam::domSIDREF>(ab->add(COLLADA_ELEMENT_SIDREF))->setValue(str(boost::format("%s/%s_%s")%asmid%asmid%kas.axissid).c_str());
            string valuesid;
            if( kas.valuesid.size() > 0 ) {
                domKinematics_newparamRef abmvalue = daeSafeCast<domKinematics_newparam>(ias_motion->add(COLLADA_ELEMENT_NEWPARAM));
                abmvalue->setSid(str(boost::format("%s_%s")%asmid%kas.valuesid).c_str());
                daeSafeCast<domKinematics_newparam::domSIDREF>(abmvalue->add(COLLADA_ELEMENT_SIDREF))->setValue(str(boost::format("%s/%s")%askid%kas.valuesid).c_str());
                domKinematics_newparamRef abvalue = daeSafeCast<domKinematics_newparam>(ias->add(COLLADA_ELEMENT_NEWPARAM));
                valuesid = str(boost::format("%s_%s")%assym%kas.valuesid);
                abvalue->setSid(valuesid.c_str());
                daeSafeCast<domKinematics_newparam::domSIDREF>(abvalue->add(COLLADA_ELEMENT_SIDREF))->setValue(str(boost::format("%s/%s_%s")%asmid%asmid%kas.valuesid).c_str());
            }
            iasout->vaxissids.push_back(axis_sids(string(ab->getSid()),valuesid,kas.jointnodesid));
        }

        boost::shared_ptr<kinematics_model_output> kmout = _GetKinematics_model(bodyInfo);
        string kmodelid = kmout->kmodel->getID(); kmodelid += "/";

        // write manipulators
        for(std::list<ManipulatorInfo>::const_iterator itmanip = _listmanipulators.begin(); itmanip != _listmanipulators.end(); ++itmanip) {
            domExtraRef pextra = daeSafeCast<domExtra>(articulated_system_motion->add(COLLADA_ELEMENT_EXTRA));
            pextra->setName(itmanip->name.c_str());
            pextra->setType("manipulator");
            domTechniqueRef ptec = daeSafeCast<domTechnique>(pextra->add(COLLADA_ELEMENT_TECHNIQUE));
            ptec->setProfile("OpenRAVE");
            daeElementRef frame_origin = ptec->add("frame_origin");
            frame_origin->setAttribute("link",(kmodelid+_GetLinkSid(kmout->maplinknames[itmanip->basename])).c_str());
            daeElementRef frame_tip = ptec->add("frame_tip");
            frame_tip->setAttribute("link",(kmodelid+_GetLinkSid(kmout->maplinknames[itmanip->effectorname])).c_str());
            _WriteTransformation(frame_tip,itmanip->rotation, itmanip->translation);
            BOOST_ASSERT(itmanip->gripperdir.size() == itmanip->grippernames.size());
            std::list<std::string>::const_iterator itgripperdir = itmanip->gripperdir.begin();
            std::list<std::string>::const_iterator itgrippername = itmanip->grippernames.begin();
            while(itgrippername != itmanip->grippernames.end() ) {
                daeElementRef gripper_joint = ptec->add("gripper_joint");
                gripper_joint->setAttribute("joint", str(boost::format("%sjointsid%d")%kmodelid%kmout->mapjointnames[*itgrippername]).c_str());
                daeElementRef closing_direction = gripper_joint->add("closing_direction");
                closing_direction->setAttribute("axis","./axis0");
                closing_direction->add("float")->setCharData(*itgripperdir); // might be -1.0
                ++itgrippername;
                ++itgripperdir;
            }
        }

        boost::shared_ptr<instance_physics_model_output> ipmout = _WriteInstance_physics_model(bodyInfo,_scene.pscene,_scene.pscene->getID());

        // interface type
        //        {
        //            domExtraRef pextra = daeSafeCast<domExtra>(articulated_system_motion->add(COLLADA_ELEMENT_EXTRA));
        //            pextra->setType("interface_type");
        //            domTechniqueRef ptec = daeSafeCast<domTechnique>(pextra->add(COLLADA_ELEMENT_TECHNIQUE));
        //            ptec->setProfile("OpenRAVE");
        //            ptec->add("interface")->setCharData(probot->GetXMLId());
        //        }

        // sensors
        for(size_t ilink = 0; ilink < links->length(); ++ilink) {
            LinkInfo& linkInfo = links[ilink];
            for(size_t isensor = 0; isensor < linkInfo.sensors.length(); ++isensor) {
                SensorInfo& sensor = linkInfo.sensors[isensor];
                daeElementRef domsensor = WriteSensor(sensor, bodyInfo->name());

                domExtraRef extra_attach_sensor = daeSafeCast<domExtra>(articulated_system_motion->add(COLLADA_ELEMENT_EXTRA));
                extra_attach_sensor->setName(sensor.name);
                extra_attach_sensor->setType("attach_sensor");
                domTechniqueRef attach_sensor = daeSafeCast<domTechnique>(extra_attach_sensor->add(COLLADA_ELEMENT_TECHNIQUE));
                attach_sensor->setProfile("OpenRAVE");

                string strurl = str(boost::format("#%s")%domsensor->getID());
                daeElementRef isensor0 = attach_sensor->add("instance_sensor");
                isensor0->setAttribute("url",strurl.c_str());

                daeElementRef frame_origin = attach_sensor->add("frame_origin");
                frame_origin->setAttribute("link",(kmodelid+_GetLinkSid(kmout->maplinknames[string(linkInfo.segments[0].name)])).c_str());
                if( string(domsensor->getAttribute("type")).find("camera") != string::npos ) {
                    // rotate camera coord system by 180 on x-axis since camera direction is toward -z
                    DblArray4 rotation; rotation[0] = 1; rotation[1] = 0; rotation[2] = 0; rotation[3] = M_PI;
                    _SetRotate(daeSafeCast<domRotate>(frame_origin->add(COLLADA_ELEMENT_ROTATE,0)), rotation);
                }
                _WriteTransformation(frame_origin,sensor.rotation, sensor.translation);
            }
        }

        // actuators, create in the order specified by jointId!
        std::vector< pair<int, int> > vjointids(links->length());
        for(size_t ilink = 0; ilink < links->length(); ++ilink) {
            vjointids[ilink].first = ilink;
            vjointids[ilink].second = links[ilink].jointId;
        }
        sort(vjointids.begin(),vjointids.end(),ComparePair);
        for(size_t ipair = 0; ipair < vjointids.size(); ++ipair) {
            size_t ilink = vjointids[ipair].first;
            LinkInfo& linkInfo = links[ilink];
            daeElementRef domactuator = WriteActuator(linkInfo, bodyInfo->name());

            domExtraRef extra_attach_actuator = daeSafeCast<domExtra>(articulated_system_motion->add(COLLADA_ELEMENT_EXTRA));
            extra_attach_actuator->setName(linkInfo.name);
            extra_attach_actuator->setType("attach_actuator");
            domTechniqueRef attach_actuator = daeSafeCast<domTechnique>(extra_attach_actuator->add(COLLADA_ELEMENT_TECHNIQUE));
            attach_actuator->setProfile("OpenRAVE");

            string strurl = str(boost::format("#%s")%domactuator->getID());
            daeElementRef iactuator = attach_actuator->add("instance_actuator");
            iactuator->setAttribute("url",strurl.c_str());
            attach_actuator->add("bind_actuator")->setAttribute("joint",str(boost::format("%sjointsid%d")%kmodelid%kmout->mapjointnames[string(linkInfo.name)]).c_str());
        }

        return iasout;
    }

    /// \brief Write kinematic body in a given scene
    virtual boost::shared_ptr<instance_kinematics_model_output> _WriteInstance_kinematics_model(BodyInfo_impl* bodyInfo, daeElementRef parent, const string& sidscope)
    {
        COLLADALOG_VERBOSE(str(boost::format("writing instance_kinematics_model (%d) %s\n")%_GetRobotId(bodyInfo)%bodyInfo->name()));
        boost::shared_ptr<kinematics_model_output> kmout = WriteKinematics_model(bodyInfo);

        boost::shared_ptr<instance_kinematics_model_output> ikmout(new instance_kinematics_model_output());
        ikmout->kmout = kmout;
        ikmout->ikm = daeSafeCast<domInstance_kinematics_model>(parent->add(COLLADA_ELEMENT_INSTANCE_KINEMATICS_MODEL));

        string symscope, refscope;
        if( sidscope.size() > 0 ) {
            symscope = sidscope+string("_");
            refscope = sidscope+string("/");
        }
        string ikmsid = str(boost::format("%s_inst")%kmout->kmodel->getID());
        ikmout->ikm->setUrl(str(boost::format("#%s")%kmout->kmodel->getID()).c_str());
        ikmout->ikm->setSid(ikmsid.c_str());

        domKinematics_newparamRef kbind = daeSafeCast<domKinematics_newparam>(ikmout->ikm->add(COLLADA_ELEMENT_NEWPARAM));
        kbind->setSid((symscope+ikmsid).c_str());
        daeSafeCast<domKinematics_newparam::domSIDREF>(kbind->add(COLLADA_ELEMENT_SIDREF))->setValue((refscope+ikmsid).c_str());
        ikmout->vkinematicsbindings.push_back(make_pair(string(kbind->getSid()), str(boost::format("visual%d/node0")%_GetRobotId(bodyInfo))));
        LinkInfoSequence_var links = bodyInfo->links();
        ikmout->vaxissids.reserve(kmout->vaxissids.size());
        int i = 0;
        for(std::vector<kinematics_model_output::axis_output>::iterator it = kmout->vaxissids.begin(); it != kmout->vaxissids.end(); ++it) {
            domKinematics_newparamRef kbind = daeSafeCast<domKinematics_newparam>(ikmout->ikm->add(COLLADA_ELEMENT_NEWPARAM));
            string ref = it->sid;
            size_t index = ref.find("/");
            while(index != string::npos) {
                ref[index] = '.';
                index = ref.find("/",index+1);
            }
            string sid = symscope+ikmsid+"_"+ref;
            kbind->setSid(sid.c_str());
            daeSafeCast<domKinematics_newparam::domSIDREF>(kbind->add(COLLADA_ELEMENT_SIDREF))->setValue((refscope+ikmsid+"/"+it->sid).c_str());
            domKinematics_newparamRef pvalueparam = daeSafeCast<domKinematics_newparam>(ikmout->ikm->add(COLLADA_ELEMENT_NEWPARAM));
            pvalueparam->setSid((sid+string("_value")).c_str());
            daeSafeCast<domKinematics_newparam::domFloat>(pvalueparam->add(COLLADA_ELEMENT_FLOAT))->setValue(links[it->ijoint].jointValue);
            ikmout->vaxissids.push_back(axis_sids(sid,pvalueparam->getSid(),kmout->vaxissids.at(i).jointnodesid));
            ++i;
        }

        return ikmout;
    }

    virtual boost::shared_ptr<instance_physics_model_output> _WriteInstance_physics_model(BodyInfo_impl* bodyInfo, daeElementRef parent, const string& sidscope)
    {
        boost::shared_ptr<physics_model_output> pmout = WritePhysics_model(bodyInfo);
        boost::shared_ptr<instance_physics_model_output> ipmout(new instance_physics_model_output());
        ipmout->pmout = pmout;
        ipmout->ipm = daeSafeCast<domInstance_physics_model>(parent->add(COLLADA_ELEMENT_INSTANCE_PHYSICS_MODEL));
        ipmout->ipm->setParent(xsAnyURI(*ipmout->ipm,string("#")+_GetNodeId(bodyInfo)));
        string symscope, refscope;
        if( sidscope.size() > 0 ) {
            symscope = sidscope+string("_");
            refscope = sidscope+string("/");
        }
        string ipmsid = str(boost::format("%s_inst")%pmout->pmodel->getID());
        ipmout->ipm->setUrl(str(boost::format("#%s")%pmout->pmodel->getID()).c_str());
        ipmout->ipm->setSid(ipmsid.c_str());
        for(size_t i = 0; i < pmout->vrigidbodysids.size(); ++i) {
            domInstance_rigid_bodyRef pirb = daeSafeCast<domInstance_rigid_body>(ipmout->ipm->add(COLLADA_ELEMENT_INSTANCE_RIGID_BODY));
            pirb->setBody(pmout->vrigidbodysids[i].c_str());
            pirb->setTarget(xsAnyURI(*pirb,str(boost::format("#%s")%_GetNodeId(bodyInfo,i))));
        }

        return ipmout;
    }

    virtual boost::shared_ptr<kinematics_model_output> WriteKinematics_model(BodyInfo_impl* bodyInfo) {
        boost::shared_ptr<kinematics_model_output> kmout = _GetKinematics_model(bodyInfo);
        if( !!kmout ) {
            return kmout;
        }

        domKinematics_modelRef kmodel = daeSafeCast<domKinematics_model>(_kinematicsModelsLib->add(COLLADA_ELEMENT_KINEMATICS_MODEL));
        string kmodelid = str(boost::format("kmodel%d")%_GetRobotId(bodyInfo));
        kmodel->setId(kmodelid.c_str());
        kmodel->setName(bodyInfo->name());

        domKinematics_model_techniqueRef ktec = daeSafeCast<domKinematics_model_technique>(kmodel->add(COLLADA_ELEMENT_TECHNIQUE_COMMON));

        //  Create root node for the visual scene
        domNodeRef pnoderoot = daeSafeCast<domNode>(_scene.vscene->add(COLLADA_ELEMENT_NODE));
        string bodyid = _GetNodeId(bodyInfo);
        pnoderoot->setId(bodyid.c_str());
        pnoderoot->setSid(bodyid.c_str());
        pnoderoot->setName(bodyInfo->name());

        LinkInfoSequence_var links = bodyInfo->links();
        vector<domJointRef> vdomjoints(links->length());
        kmout.reset(new kinematics_model_output());
        kmout->kmodel = kmodel;
        kmout->vaxissids.resize(0);
        kmout->vlinksids.resize(links->length());

        for(size_t ilink = 0; ilink < vdomjoints.size(); ++ilink) {
            LinkInfo& linkInfo = links[ilink];
            kmout->maplinknames[std::string(linkInfo.segments[0].name)] = ilink;
            string jointType(CORBA::String_var(linkInfo.jointType));
            daeString colladaelement;
            int dof = 1;
            dReal fmult = 1;
            vector<dReal> lmin, lmax;
            if( jointType == "fixed" ) {
                colladaelement = COLLADA_ELEMENT_REVOLUTE;
                lmin.push_back(0);
                lmax.push_back(0);
            }
            else {
                if( jointType == "rotate" ) {
                    colladaelement = COLLADA_ELEMENT_REVOLUTE;
                    fmult = 180.0f/M_PI;
                }
                else if( jointType == "slide" ) {
                    colladaelement = COLLADA_ELEMENT_PRISMATIC;
                }
                else {
                    COLLADALOG_INFO(str(boost::format("joint type %s not supported")%jointType));
                    continue;
                }
                if( linkInfo.llimit.length() > 0 ) {
                    lmin.push_back(fmult*linkInfo.llimit[0]);
                }
                if( linkInfo.ulimit.length() > 0 ) {
                    lmax.push_back(fmult*linkInfo.ulimit[0]);
                }
            }

            domJointRef pdomjoint = daeSafeCast<domJoint>(ktec->add(COLLADA_ELEMENT_JOINT));
            string jointsid = str(boost::format("jointsid%d")%linkInfo.jointId);
            pdomjoint->setSid( jointsid.c_str() );
            //pdomjoint->setName(str(boost::format("joint%d")%linkInfo.jointId).c_str());
            pdomjoint->setName(linkInfo.name); // changed
            kmout->mapjointnames[std::string(linkInfo.name)] = ilink;
            vector<domAxis_constraintRef> vaxes(dof);
            for(int ia = 0; ia < dof; ++ia) {
                vaxes[ia] = daeSafeCast<domAxis_constraint>(pdomjoint->add(colladaelement));
                string axisid = str(boost::format("axis%d")%ia);
                vaxes[ia]->setSid(axisid.c_str());
                kinematics_model_output::axis_output axissid;
                axissid.ijoint = ilink;
                axissid.sid = jointsid+string("/")+axisid;
                axissid.iaxis = ia;
                axissid.jointnodesid = str(boost::format("%s/%s")%bodyid%_GetJointNodeSid(ilink,ia));
                kmout->vaxissids.push_back(axissid);
                domAxisRef paxis = daeSafeCast<domAxis>(vaxes.at(ia)->add(COLLADA_ELEMENT_AXIS));
                paxis->getValue().setCount(3);
                paxis->getValue()[0] = linkInfo.jointAxis[0];
                paxis->getValue()[1] = linkInfo.jointAxis[1];
                paxis->getValue()[2] = linkInfo.jointAxis[2];
                if( lmin.size() > 0 || lmax.size() > 0 ) {
                    domJoint_limitsRef plimits = daeSafeCast<domJoint_limits>(vaxes[ia]->add(COLLADA_TYPE_LIMITS));
                    if( ia < (int)lmin.size() ) {
                        daeSafeCast<domMinmax>(plimits->add(COLLADA_ELEMENT_MIN))->getValue() = lmin.at(ia);
                    }
                    if( ia < (int)lmax.size() ) {
                        daeSafeCast<domMinmax>(plimits->add(COLLADA_ELEMENT_MAX))->getValue() = lmax.at(ia);
                    }
                }
            }
            vdomjoints.at(ilink) = pdomjoint;
        }

        list<int> listunusedlinks;
        for(int i = 0; i < links->length(); ++i) {
            listunusedlinks.push_back(i);
        }

        while(listunusedlinks.size()>0) {
            int ilink = listunusedlinks.front();
            LINKOUTPUT childinfo = _WriteLink(bodyInfo, ilink, ktec, pnoderoot, kmodel->getID());
            _WriteTransformation(childinfo.plink, links[ilink].rotation, links[ilink].translation);
            _WriteTransformation(childinfo.pnode, links[ilink].rotation, links[ilink].translation);
            for(list<pair<int,std::string> >::iterator itused = childinfo.listusedlinks.begin(); itused != childinfo.listusedlinks.end(); ++itused) {
                kmout->vlinksids.at(itused->first) = itused->second;
                listunusedlinks.remove(itused->first);
            }
        }

        //        // interface type
        //        {
        //            domExtraRef pextra = daeSafeCast<domExtra>(kmout->kmodel->add(COLLADA_ELEMENT_EXTRA));
        //            pextra->setType("interface_type");
        //            domTechniqueRef ptec = daeSafeCast<domTechnique>(pextra->add(COLLADA_ELEMENT_TECHNIQUE));
        //            ptec->setProfile("OpenRAVE");
        //            ptec->add("interface")->setCharData(pbody->GetXMLId());
        //        }

        // collision data
        //        {
        //            domExtraRef pextra = daeSafeCast<domExtra>(kmout->kmodel->add(COLLADA_ELEMENT_EXTRA));
        //            pextra->setType("collision");
        //            domTechniqueRef ptec = daeSafeCast<domTechnique>(pextra->add(COLLADA_ELEMENT_TECHNIQUE));
        //            ptec->setProfile("OpenRAVE");
        //            FOREACHC(itadjacent,pbody->_vForcedAdjacentLinks) {
        //                KinBody::LinkPtr plink0 = pbody->GetLink(itadjacent->first);
        //                KinBody::LinkPtr plink1 = pbody->GetLink(itadjacent->second);
        //                if( !!plink0 && !!plink1 ) {
        //                    daeElementRef pignore = ptec->add("ignore_link_pair");
        //                    pignore->setAttribute("link0",(kmodelid + string("/") + kmout->vlinksids.at(plink0->GetIndex())).c_str());
        //                    pignore->setAttribute("link1",(kmodelid + string("/") + kmout->vlinksids.at(plink1->GetIndex())).c_str());
        //                }
        //            }
        //        }

        _AddKinematics_model(bodyInfo,kmout);
        return kmout;
    }

    virtual boost::shared_ptr<physics_model_output> WritePhysics_model(BodyInfo_impl* bodyInfo) {
        boost::shared_ptr<physics_model_output> pmout = _GetPhysics_model(bodyInfo);
        if( !!pmout ) {
            return pmout;
        }
        pmout.reset(new physics_model_output());
        pmout->pmodel = daeSafeCast<domPhysics_model>(_physicsModelsLib->add(COLLADA_ELEMENT_PHYSICS_MODEL));
        string pmodelid = str(boost::format("pmodel%d")%_GetRobotId(bodyInfo));
        pmout->pmodel->setId(pmodelid.c_str());
        pmout->pmodel->setName(bodyInfo->name());
        LinkInfoSequence_var links = bodyInfo->links();
        for(int ilink = 0; ilink < links->length(); ++ilink) {
            LinkInfo& link = links[ilink];
            domRigid_bodyRef rigid_body = daeSafeCast<domRigid_body>(pmout->pmodel->add(COLLADA_ELEMENT_RIGID_BODY));
            string rigidsid = str(boost::format("rigid%d")%ilink);
            pmout->vrigidbodysids.push_back(rigidsid);
            rigid_body->setSid(rigidsid.c_str());
            rigid_body->setName(link.segments[0].name);
            domRigid_body::domTechnique_commonRef ptec = daeSafeCast<domRigid_body::domTechnique_common>(rigid_body->add(COLLADA_ELEMENT_TECHNIQUE_COMMON));
            domTargetable_floatRef mass = daeSafeCast<domTargetable_float>(ptec->add(COLLADA_ELEMENT_MASS));
            mass->setValue(link.mass);
            dmatrix inertia, evec;
            dvector eval;
            inertia.resize(3,3);
            inertia(0,0) = link.inertia[0]; inertia(0,1) = link.inertia[1]; inertia(0,2) = link.inertia[2];
            inertia(1,0) = link.inertia[3]; inertia(1,1) = link.inertia[4]; inertia(1,2) = link.inertia[5];
            inertia(2,0) = link.inertia[6]; inertia(2,1) = link.inertia[7]; inertia(2,2) = link.inertia[8];
            evec.resize(3,3);
            eval.resize(3);
            hrp::calcEigenVectors(inertia,evec,eval);
            if (det(evec) < 0.0) /* fix for right-handed coordinates */
                evec(0,2) *= -1.0; evec(1,2) *= -1.0; evec(2,2) *= -1.0;
            DblArray12 tinertiaframe;
            for(int j = 0; j < 3; ++j) {
                tinertiaframe[4*0+j] = evec(0,j);
                tinertiaframe[4*1+j] = evec(1,j);
                tinertiaframe[4*2+j] = evec(2,j);
            }
            DblArray4 quat, rotation;
            DblArray3 translation;
            QuatFromMatrix(quat,tinertiaframe);
            AxisAngleFromQuat(rotation,quat);
            domTargetable_float3Ref pdominertia = daeSafeCast<domTargetable_float3>(ptec->add(COLLADA_ELEMENT_INERTIA));
            pdominertia->getValue().setCount(3);
            pdominertia->getValue()[0] = eval[0]; pdominertia->getValue()[1] = eval[1]; pdominertia->getValue()[2] = eval[2];
            daeElementRef mass_frame = ptec->add(COLLADA_ELEMENT_MASS_FRAME);
            _WriteTransformation(mass_frame, rotation, link.centerOfMass);
            // add all the parents
            int icurlink = ilink;
            while(icurlink >= 0) {
                _WriteTransformation(mass_frame, links[icurlink].rotation, links[icurlink].translation);
                icurlink = links[icurlink].parentIndex;
            }
            //daeSafeCast<domRigid_body::domTechnique_common::domDynamic>(ptec->add(COLLADA_ELEMENT_DYNAMIC))->setValue(xsBoolean(dynamic));
            // create a shape for every geometry
            for(int igeom = 0; igeom < link.shapeIndices.length(); ++igeom) {
                const TransformedShapeIndex& tsi = link.shapeIndices[igeom];
                DblArray12 transformMatrix;
                if( tsi.inlinedShapeTransformMatrixIndex >= 0 ) {
                    PoseMult(transformMatrix, link.inlinedShapeTransformMatrices[tsi.inlinedShapeTransformMatrixIndex],tsi.transformMatrix);
                }
                else {
                    for(int i = 0; i < 12; ++i) {
                        transformMatrix[i] = tsi.transformMatrix[i];
                    }
                }
                domRigid_body::domTechnique_common::domShapeRef pdomshape = daeSafeCast<domRigid_body::domTechnique_common::domShape>(ptec->add(COLLADA_ELEMENT_SHAPE));
                // there is a weird bug here where _WriteTranformation will fail to create rotate/translate elements in instance_geometry is created first... (is this part of the spec?)
                QuatFromMatrix(quat,transformMatrix);
                AxisAngleFromQuat(rotation,quat);
                translation[0] = transformMatrix[4*0+3]; translation[1] = transformMatrix[4*1+3]; translation[2] = transformMatrix[4*2+3];
                _WriteTransformation(pdomshape,rotation,translation);
                icurlink = ilink;
                while(icurlink >= 0) {
                    _WriteTransformation(pdomshape, links[icurlink].rotation, links[icurlink].translation);
                    icurlink = links[icurlink].parentIndex;
                }
                domInstance_geometryRef pinstgeom = daeSafeCast<domInstance_geometry>(pdomshape->add(COLLADA_ELEMENT_INSTANCE_GEOMETRY));
                pinstgeom->setUrl(xsAnyURI(*pinstgeom,string("#")+_GetGeometryId(bodyInfo, ilink,igeom)));
            }
        }
        return pmout;
    }

    /// \brief Write geometry properties
    /// \param geom Link geometry
    /// \param parentid Parent Identifier
    virtual domGeometryRef WriteGeometry(BodyInfo_impl* bodyInfo, const ShapeInfo& shapeInfo, const DblArray12& transformMatrix, const string& parentid)
    {
        const FloatSequence& vertices = shapeInfo.vertices;
        const LongSequence& triangles = shapeInfo.triangles;
        const int numTriangles = triangles.length() / 3;
        string effid = parentid+string("_eff");
        string matid = parentid+string("_mat");

        AppearanceInfo& appearanceInfo = (*bodyInfo->appearances())[shapeInfo.appearanceIndex];
        domEffectRef pdomeff = WriteEffect((*bodyInfo->materials())[appearanceInfo.materialIndex]);
        pdomeff->setId(effid.c_str());

        domMaterialRef pdommat = daeSafeCast<domMaterial>(_materialsLib->add(COLLADA_ELEMENT_MATERIAL));
        pdommat->setId(matid.c_str());
        domInstance_effectRef pdominsteff = daeSafeCast<domInstance_effect>(pdommat->add(COLLADA_ELEMENT_INSTANCE_EFFECT));
        pdominsteff->setUrl((string("#")+effid).c_str());

        //check shapeInfo.primitiveType: SP_MESH, SP_BOX, SP_CYLINDER, SP_CONE, SP_SPHERE
        if( shapeInfo.primitiveType != SP_MESH ) {
            COLLADALOG_WARN("shape index is not SP_MESH type, could result in inaccuracies");
        }
        domGeometryRef pdomgeom = daeSafeCast<domGeometry>(_geometriesLib->add(COLLADA_ELEMENT_GEOMETRY));
        {
            pdomgeom->setId(parentid.c_str());
            domMeshRef pdommesh = daeSafeCast<domMesh>(pdomgeom->add(COLLADA_ELEMENT_MESH));
            {
                domSourceRef pvertsource = daeSafeCast<domSource>(pdommesh->add(COLLADA_ELEMENT_SOURCE));
                {
                    pvertsource->setId((parentid+string("_positions")).c_str());

                    domFloat_arrayRef parray = daeSafeCast<domFloat_array>(pvertsource->add(COLLADA_ELEMENT_FLOAT_ARRAY));
                    parray->setId((parentid+string("_positions-array")).c_str());
                    parray->setCount(vertices.length()/3);
                    parray->setDigits(6); // 6 decimal places
                    parray->getValue().setCount(vertices.length());

                    for(size_t ind = 0; ind < vertices.length(); ind += 3) {
                        DblArray3 v, vnew;
                        v[0] = vertices[ind]; v[1] = vertices[ind+1]; v[2] = vertices[ind+2];
                        PoseMultVector(vnew, transformMatrix, v);
                        parray->getValue()[ind+0] = vnew[0];
                        parray->getValue()[ind+1] = vnew[1];
                        parray->getValue()[ind+2] = vnew[2];
                    }

                    domSource::domTechnique_commonRef psourcetec = daeSafeCast<domSource::domTechnique_common>(pvertsource->add(COLLADA_ELEMENT_TECHNIQUE_COMMON));
                    domAccessorRef pacc = daeSafeCast<domAccessor>(psourcetec->add(COLLADA_ELEMENT_ACCESSOR));
                    pacc->setCount(vertices.length());
                    pacc->setSource(xsAnyURI(*pacc, string("#")+parentid+string("_positions-array")));
                    pacc->setStride(3);

                    domParamRef px = daeSafeCast<domParam>(pacc->add(COLLADA_ELEMENT_PARAM));
                    px->setName("X"); px->setType("float");
                    domParamRef py = daeSafeCast<domParam>(pacc->add(COLLADA_ELEMENT_PARAM));
                    py->setName("Y"); py->setType("float");
                    domParamRef pz = daeSafeCast<domParam>(pacc->add(COLLADA_ELEMENT_PARAM));
                    pz->setName("Z"); pz->setType("float");
                }

                domVerticesRef pverts = daeSafeCast<domVertices>(pdommesh->add(COLLADA_ELEMENT_VERTICES));
                {
                    pverts->setId("vertices");
                    domInput_localRef pvertinput = daeSafeCast<domInput_local>(pverts->add(COLLADA_ELEMENT_INPUT));
                    pvertinput->setSemantic("POSITION");
                    pvertinput->setSource(domUrifragment(*pvertsource, string("#")+parentid+string("_positions")));
                }

                domTrianglesRef ptris = daeSafeCast<domTriangles>(pdommesh->add(COLLADA_ELEMENT_TRIANGLES));
                {
                    ptris->setCount(triangles.length()/3);
                    ptris->setMaterial("mat0");

                    domInput_local_offsetRef pvertoffset = daeSafeCast<domInput_local_offset>(ptris->add(COLLADA_ELEMENT_INPUT));
                    pvertoffset->setSemantic("VERTEX");
                    pvertoffset->setOffset(0);
                    pvertoffset->setSource(domUrifragment(*pverts, string("#")+parentid+string("/vertices")));
                    domPRef pindices = daeSafeCast<domP>(ptris->add(COLLADA_ELEMENT_P));
                    pindices->getValue().setCount(triangles.length());
                    for(size_t ind = 0; ind < triangles.length(); ++ind)
                        pindices->getValue()[ind] = triangles[ind];
                }
            }
        }

        return pdomgeom;
    }

    /// Write light effect
    /// vambient    Ambient light color
    /// vdiffuse    Diffuse light color
    virtual domEffectRef WriteEffect(const MaterialInfo& material)
    {
        domEffectRef pdomeff = daeSafeCast<domEffect>(_effectsLib->add(COLLADA_ELEMENT_EFFECT));

        domProfile_commonRef pprofile = daeSafeCast<domProfile_common>(pdomeff->add(COLLADA_ELEMENT_PROFILE_COMMON));
        domProfile_common::domTechniqueRef ptec = daeSafeCast<domProfile_common::domTechnique>(pprofile->add(COLLADA_ELEMENT_TECHNIQUE));

        domProfile_common::domTechnique::domPhongRef pphong = daeSafeCast<domProfile_common::domTechnique::domPhong>(ptec->add(COLLADA_ELEMENT_PHONG));

        domFx_common_color_or_textureRef pambient = daeSafeCast<domFx_common_color_or_texture>(pphong->add(COLLADA_ELEMENT_AMBIENT));
        domFx_common_color_or_texture::domColorRef pambientcolor = daeSafeCast<domFx_common_color_or_texture::domColor>(pambient->add(COLLADA_ELEMENT_COLOR));
        pambientcolor->getValue().setCount(4);
        pambientcolor->getValue()[0] = material.ambientIntensity;
        pambientcolor->getValue()[1] = material.ambientIntensity;
        pambientcolor->getValue()[2] = material.ambientIntensity;
        pambientcolor->getValue()[3] = 1;

        domFx_common_color_or_textureRef pdiffuse = daeSafeCast<domFx_common_color_or_texture>(pphong->add(COLLADA_ELEMENT_DIFFUSE));
        domFx_common_color_or_texture::domColorRef pdiffusecolor = daeSafeCast<domFx_common_color_or_texture::domColor>(pdiffuse->add(COLLADA_ELEMENT_COLOR));
        pdiffusecolor->getValue().setCount(4);
        pdiffusecolor->getValue()[0] = material.diffuseColor[0];
        pdiffusecolor->getValue()[1] = material.diffuseColor[1];
        pdiffusecolor->getValue()[2] = material.diffuseColor[2];
        pdiffusecolor->getValue()[3] = 1;

        domFx_common_color_or_textureRef pspecular = daeSafeCast<domFx_common_color_or_texture>(pphong->add(COLLADA_ELEMENT_SPECULAR));
        domFx_common_color_or_texture::domColorRef pspecularcolor = daeSafeCast<domFx_common_color_or_texture::domColor>(pspecular->add(COLLADA_ELEMENT_COLOR));
        pspecularcolor->getValue().setCount(4);
        pspecularcolor->getValue()[0] = material.specularColor[0];
        pspecularcolor->getValue()[1] = material.specularColor[1];
        pspecularcolor->getValue()[2] = material.specularColor[2];
        pspecularcolor->getValue()[3] = 1;

        domFx_common_color_or_textureRef pemission = daeSafeCast<domFx_common_color_or_texture>(pphong->add(COLLADA_ELEMENT_EMISSION));
        domFx_common_color_or_texture::domColorRef pemissioncolor = daeSafeCast<domFx_common_color_or_texture::domColor>(pemission->add(COLLADA_ELEMENT_COLOR));
        pemissioncolor->getValue().setCount(4);
        pemissioncolor->getValue()[0] = material.emissiveColor[0];
        pemissioncolor->getValue()[1] = material.emissiveColor[1];
        pemissioncolor->getValue()[2] = material.emissiveColor[2];
        pemissioncolor->getValue()[3] = 1;

        //domFx_common_color_or_textureRef ptransparency = daeSafeCast<domFx_common_color_or_texture>(pphong->add(COLLADA_ELEMENT_TRANSPARENCY));
        //ptransparency->setAttribute("opage","A_ZERO");  // 0 is opaque
        return pdomeff;
    }

    virtual daeElementRef WriteSensor(const SensorInfo& sensor, const string& parentid)
    {
        daeElementRef domsensor = _sensorsLib->add("sensor");
        _nextsensorid++; domsensor->setAttribute("id",str(boost::format("sensor%d")%_nextsensorid).c_str());

        string vrmltype = tolowerstring(string(sensor.type));
        if( vrmltype == "force" ) {
            domsensor->setAttribute("type","base_force6d");
        }
        else if( vrmltype == "rategyro") {
            COLLADALOG_WARN("rategyro is treated as an IMU\n");
        }
        else if( vrmltype == "acceleration" ) {
            domsensor->setAttribute("type","base_imu");
        }
        else if( vrmltype == "vision" ) {
            domsensor->setAttribute("type","base_pinhole_camera");
            // frontClipDistance, backClipDistance, fieldOfView, type, width, height, frameRate
            if( sensor.specValues.length() != 7 ) {
                COLLADALOG_WARN(str(boost::format("vision sensor has wrong number of values! %d!=7")%sensor.specValues.length()));
            }
            domsensor->add("focal_length")->setCharData(str(boost::format("%f")%sensor.specValues[0]));
            double fieldOfView = sensor.specValues[2], width = sensor.specValues[4], height = sensor.specValues[5];
            stringstream sintrinsic; sintrinsic << std::setprecision(15);
            double fx = 0.5/(tanf(fieldOfView*0.5));
            sintrinsic << fx*width << " 0 " << 0.5*width << " 0 " << fx*height << " " << 0.5*height;
            domsensor->add("intrinsic")->setCharData(sintrinsic.str());
            stringstream simage_dimensions; simage_dimensions << (int)width << " " << (int)height << " ";
            string format = "uint8";
            Camera::CameraType cameratype = Camera::CameraType((int)sensor.specValues[3]);
            switch(cameratype) {
            case Camera::NONE:
                COLLADALOG_WARN("no camera type specified!");
                break;
            case Camera::COLOR:
                simage_dimensions << 3;
                break;
            case Camera::MONO:
                simage_dimensions << 1;
                break;
            case Camera::DEPTH:
                simage_dimensions << 1;
                format = "float32";
                break;
            case Camera::COLOR_DEPTH:
                format = "float32";
                simage_dimensions << 4;
                break;
            case Camera::MONO_DEPTH:
                format = "float32";
                simage_dimensions << 2;
                break;
            }
            domsensor->add("image_dimensions")->setCharData(simage_dimensions.str());
            domsensor->add("measurement_time")->setCharData(str(boost::format("%f")%(1.0/sensor.specValues[6])));
        }
        else if( vrmltype == "range" ) {
            domsensor->setAttribute("type","base_laser2d");
        }
        return domsensor;
    }

    virtual daeElementRef WriteActuator(const LinkInfo& plink, const string& parentid)
    {
        daeElementRef domactuator = _actuatorsLib->add("actuator");
        _nextactuatorid++; domactuator->setAttribute("id",str(boost::format("actuator%d")%_nextactuatorid).c_str());
        domactuator->setAttribute("type","electric_motor");
        domactuator->add("assigned_power_rating")->setCharData("1.0");
        double max_speed = plink.uvlimit.length()/2*M_PI > 0 ? plink.uvlimit[0] : 0;
        domactuator->add("max_speed")->setCharData(str(boost::format("%f")%max_speed));
        domactuator->add("no_load_speed")->setCharData(str(boost::format("%f")%max_speed));
        domactuator->add("nominal_torque")->setCharData("0");
        domactuator->add("nominal_voltage")->setCharData("0");
        domactuator->add("rotor_inertia")->setCharData(str(boost::format("%f")%(plink.rotorInertia)));
        domactuator->add("speed_constant")->setCharData("0");
        domactuator->add("speed_torque_gradient")->setCharData("0");
        domactuator->add("starting_current")->setCharData("0");
        domactuator->add("terminal_resistance")->setCharData(str(boost::format("%f")%(plink.rotorResistor)));
        domactuator->add("torque_constant")->setCharData(str(boost::format("%f")%(plink.torqueConst)));
        return domactuator;
    }

private:

    /// \brief save all the loaded scene models and their current state.
    virtual void _CreateScene()
    {
        // Create visual scene
        _scene.vscene = daeSafeCast<domVisual_scene>(_visualScenesLib->add(COLLADA_ELEMENT_VISUAL_SCENE));
        _scene.vscene->setId("vscene");
        _scene.vscene->setName("OpenRAVE Visual Scene");

        // Create kinematics scene
        _scene.kscene = daeSafeCast<domKinematics_scene>(_kinematicsScenesLib->add(COLLADA_ELEMENT_KINEMATICS_SCENE));
        _scene.kscene->setId("kscene");
        _scene.kscene->setName("OpenRAVE Kinematics Scene");

        // Create physic scene
        _scene.pscene = daeSafeCast<domPhysics_scene>(_physicsScenesLib->add(COLLADA_ELEMENT_PHYSICS_SCENE));
        _scene.pscene->setId("pscene");
        _scene.pscene->setName("OpenRAVE Physics Scene");

        // Create instance visual scene
        _scene.viscene = daeSafeCast<domInstance_with_extra>(_globalscene->add( COLLADA_ELEMENT_INSTANCE_VISUAL_SCENE ));
        _scene.viscene->setUrl( (string("#") + string(_scene.vscene->getID())).c_str() );

        // Create instance kinematics scene
        _scene.kiscene = daeSafeCast<domInstance_kinematics_scene>(_globalscene->add( COLLADA_ELEMENT_INSTANCE_KINEMATICS_SCENE ));
        _scene.kiscene->setUrl( (string("#") + string(_scene.kscene->getID())).c_str() );

        // Create instance physics scene
        _scene.piscene = daeSafeCast<domInstance_with_extra>(_globalscene->add( COLLADA_ELEMENT_INSTANCE_PHYSICS_SCENE ));
        _scene.piscene->setUrl( (string("#") + string(_scene.pscene->getID())).c_str() );
    }

    /** \brief Write link of a kinematic body

        \param link Link to write
        \param pkinparent Kinbody parent
        \param pnodeparent Node parent
        \param strModelUri
        \param vjoints Vector of joints
     */
    virtual LINKOUTPUT _WriteLink(BodyInfo_impl* bodyInfo, int ilink, daeElementRef pkinparent, domNodeRef pnodeparent, const string& strModelUri)
    {
        LinkInfo& plink = (*bodyInfo->links())[ilink];
        LINKOUTPUT out;
        string linksid = _GetLinkSid(ilink);
        domLinkRef pdomlink = daeSafeCast<domLink>(pkinparent->add(COLLADA_ELEMENT_LINK));
        pdomlink->setName(plink.segments[0].name);
        pdomlink->setSid(linksid.c_str());

        domNodeRef pnode = daeSafeCast<domNode>(pnodeparent->add(COLLADA_ELEMENT_NODE));
        std::string nodeid = _GetNodeId(bodyInfo,ilink);
        pnode->setId( nodeid.c_str() );
        string nodesid = str(boost::format("node%d")%ilink);
        pnode->setSid(nodesid.c_str());
        pnode->setName(plink.segments[0].name);

        for(int igeom = 0; igeom < plink.shapeIndices.length(); ++igeom) {
            string geomid = _GetGeometryId(bodyInfo, ilink,igeom);
            const TransformedShapeIndex& tsi = plink.shapeIndices[igeom];
            DblArray12 transformMatrix;
            //this matrix is already multiplied in tsi.transformMatrix plink.inlinedShapeTransformMatrices[tsi.inlinedShapeTransformMatrixIndex]
            for(int i = 0; i < 12; ++i) {
                transformMatrix[i] = tsi.transformMatrix[i];
            }

            domGeometryRef pdomgeom = WriteGeometry(bodyInfo,(*bodyInfo->shapes())[tsi.shapeIndex], transformMatrix, geomid);
            domInstance_geometryRef pinstgeom = daeSafeCast<domInstance_geometry>(pnode->add(COLLADA_ELEMENT_INSTANCE_GEOMETRY));
            pinstgeom->setUrl((string("#")+geomid).c_str());

            domBind_materialRef pmat = daeSafeCast<domBind_material>(pinstgeom->add(COLLADA_ELEMENT_BIND_MATERIAL));
            domBind_material::domTechnique_commonRef pmattec = daeSafeCast<domBind_material::domTechnique_common>(pmat->add(COLLADA_ELEMENT_TECHNIQUE_COMMON));
            domInstance_materialRef pinstmat = daeSafeCast<domInstance_material>(pmattec->add(COLLADA_ELEMENT_INSTANCE_MATERIAL));
            pinstmat->setTarget(xsAnyURI(*pinstmat, string("#")+geomid+string("_mat")));
            pinstmat->setSymbol("mat0");
        }

        // go through all children
        for(int _ichild = 0; _ichild < plink.childIndices.length(); ++_ichild) {
            int ichild = plink.childIndices[_ichild];
            LinkInfo& childlink = (*bodyInfo->links())[ichild];
            domLink::domAttachment_fullRef pattfull = daeSafeCast<domLink::domAttachment_full>(pdomlink->add(COLLADA_TYPE_ATTACHMENT_FULL));
            string jointid = str(boost::format("%s/jointsid%d")%strModelUri%childlink.jointId);
            pattfull->setJoint(jointid.c_str());

            LINKOUTPUT childinfo = _WriteLink(bodyInfo, ichild, pattfull, pnode, strModelUri);
            out.listusedlinks.insert(out.listusedlinks.end(),childinfo.listusedlinks.begin(),childinfo.listusedlinks.end());

            _WriteTransformation(pattfull, childlink.rotation, childlink.translation);
            //_WriteTransformation(childinfo.plink, pjoint->GetInternalHierarchyRightTransform());
            //_WriteTransformation(childinfo.pnode, pjoint->GetInternalHierarchyRightTransform());

            string jointnodesid = _GetJointNodeSid(ichild,0);
            string jointType(CORBA::String_var(childlink.jointType));
            if( jointType == "rotate" || jointType == "fixed" ) {
                domRotateRef protate = daeSafeCast<domRotate>(childinfo.pnode->add(COLLADA_ELEMENT_ROTATE,0));
                protate->setSid(jointnodesid.c_str());
                protate->getValue().setCount(4);
                protate->getValue()[0] = childlink.jointAxis[0];
                protate->getValue()[1] = childlink.jointAxis[1];
                protate->getValue()[2] = childlink.jointAxis[2];
                protate->getValue()[3] = 0;
            }
            else if( jointType == "slide" ) {
                domTranslateRef ptrans = daeSafeCast<domTranslate>(childinfo.pnode->add(COLLADA_ELEMENT_TRANSLATE,0));
                ptrans->setSid(jointnodesid.c_str());
                ptrans->getValue().setCount(3);
                ptrans->getValue()[0] = 0;
                ptrans->getValue()[1] = 0;
                ptrans->getValue()[2] = 0;
            }
            else {
                COLLADALOG_WARN(str(boost::format("unsupported joint type specified %s")%jointType));
            }
            _WriteTransformation(childinfo.pnode, childlink.rotation, childlink.translation);
        }

        out.listusedlinks.push_back(make_pair(ilink,linksid));
        out.plink = pdomlink;
        out.pnode = pnode;
        return out;
    }

    void _SetRotate(domTargetable_float4Ref prot, const DblArray4& rotation)
    {
        prot->getValue().setCount(4);
        prot->getValue()[0] = rotation[0];
        prot->getValue()[1] = rotation[1];
        prot->getValue()[2] = rotation[2];
        prot->getValue()[3] = rotation[3]*(180.0/M_PI);
    }

    /// \brief Write transformation
    /// \param pelt Element to transform
    /// \param t Transform to write
    void _WriteTransformation(daeElementRef pelt, const DblArray4& rotation, const DblArray3& translation)
    {
        _SetRotate(daeSafeCast<domRotate>(pelt->add(COLLADA_ELEMENT_ROTATE,0)), rotation);
        _SetVector3(daeSafeCast<domTranslate>(pelt->add(COLLADA_ELEMENT_TRANSLATE,0))->getValue(),translation);
    }

    // binding in instance_kinematics_scene
    void _WriteBindingsInstance_kinematics_scene(domInstance_kinematics_sceneRef ikscene, BodyInfo_impl* bodyInfo, const std::vector<axis_sids>& vaxissids, const std::vector<std::pair<std::string,std::string> >& vkinematicsbindings)
    {
        for(std::vector<std::pair<std::string,std::string> >::const_iterator it = vkinematicsbindings.begin(); it != vkinematicsbindings.end(); ++it) {
            domBind_kinematics_modelRef pmodelbind = daeSafeCast<domBind_kinematics_model>(ikscene->add(COLLADA_ELEMENT_BIND_KINEMATICS_MODEL));
            pmodelbind->setNode(it->second.c_str());
            daeSafeCast<domCommon_param>(pmodelbind->add(COLLADA_ELEMENT_PARAM))->setValue(it->first.c_str());
        }
        for(std::vector<axis_sids>::const_iterator it = vaxissids.begin(); it != vaxissids.end(); ++it) {
            domBind_joint_axisRef pjointbind = daeSafeCast<domBind_joint_axis>(ikscene->add(COLLADA_ELEMENT_BIND_JOINT_AXIS));
            pjointbind->setTarget(it->jointnodesid.c_str());
            daeSafeCast<domCommon_param>(pjointbind->add(COLLADA_ELEMENT_AXIS)->add(COLLADA_TYPE_PARAM))->setValue(it->axissid.c_str());
            daeSafeCast<domCommon_param>(pjointbind->add(COLLADA_ELEMENT_VALUE)->add(COLLADA_TYPE_PARAM))->setValue(it->valuesid.c_str());
        }
    }

    /// Set vector of three elements
    template <typename T> static void _SetVector3(T& t, const DblArray3& v) {
        t.setCount(3);
        t[0] = v[0];
        t[1] = v[1];
        t[2] = v[2];
    }

    virtual void _AddKinematics_model(BodyInfo_impl* bodyInfo, boost::shared_ptr<kinematics_model_output> kmout) {
        string xmlfilename;
        if( !!bodyInfo->url() ) {
            xmlfilename = bodyInfo->url();
        }
        for(std::list<kinbody_models>::iterator it = _listkinbodies.begin(); it != _listkinbodies.end(); ++it) {
            if( it->xmlfilename == xmlfilename ) {
                BOOST_ASSERT(!it->kmout);
                it->kmout = kmout;
                return;
            }
        }
        kinbody_models cache;
        cache.xmlfilename = xmlfilename;
        cache.kinematicsgeometryhash = "";
        cache.kmout = kmout;
        _listkinbodies.push_back(cache);
    }

    virtual boost::shared_ptr<kinematics_model_output> _GetKinematics_model(BodyInfo_impl* bodyInfo) {
        for(std::list<kinbody_models>::iterator it = _listkinbodies.begin(); it != _listkinbodies.end(); ++it) {
            if( !bodyInfo->url() || it->xmlfilename == bodyInfo->url() ) {
                return it->kmout;
            }
        }
        return boost::shared_ptr<kinematics_model_output>();
    }

    virtual void _AddPhysics_model(BodyInfo_impl* bodyInfo, boost::shared_ptr<physics_model_output> pmout) {
        string xmlfilename;
        if( !!bodyInfo->url() ) {
            xmlfilename = bodyInfo->url();
        }
        for(std::list<kinbody_models>::iterator it = _listkinbodies.begin(); it != _listkinbodies.end(); ++it) {
            if( it->xmlfilename == xmlfilename ) {
                BOOST_ASSERT(!it->pmout);
                it->pmout = pmout;
                return;
            }
        }
        kinbody_models cache;
        cache.xmlfilename = xmlfilename;
        cache.kinematicsgeometryhash = "";
        cache.pmout = pmout;
        _listkinbodies.push_back(cache);
    }

    virtual boost::shared_ptr<physics_model_output> _GetPhysics_model(BodyInfo_impl* bodyInfo) {
        for(std::list<kinbody_models>::iterator it = _listkinbodies.begin(); it != _listkinbodies.end(); ++it) {
            if( !bodyInfo->url() || it->xmlfilename == bodyInfo->url() ) {
                return it->pmout;
            }
        }
        return boost::shared_ptr<physics_model_output>();
    }

    virtual int _GetRobotId(BodyInfo_impl* bodyInfo) {
        return 1;
    }
    virtual std::string _GetNodeId(BodyInfo_impl* bodyInfo) {
        return str(boost::format("visual%d")%_GetRobotId(bodyInfo));
    }
    virtual std::string _GetNodeId(BodyInfo_impl* bodyInfo, int ilink) {
        return str(boost::format("v%d.node%d")%_GetRobotId(bodyInfo)%ilink);
    }

    virtual std::string _GetLinkSid(int ilink) {
        return str(boost::format("link%d")%ilink);
    }

    virtual std::string _GetGeometryId(BodyInfo_impl* bodyInfo, int ilink, int igeom) {
        return str(boost::format("g%d_%d_geom%d")%_GetRobotId(bodyInfo)%ilink%igeom);
    }
    virtual std::string _GetJointNodeSid(int ijoint, int iaxis) {
        return str(boost::format("node_joint%d_axis%d")%ijoint%iaxis);
    }

    virtual void handleError( daeString msg )
    {
        cerr << "COLLADA error: " << msg << endl;
    }

    virtual void handleWarning( daeString msg )
    {
        cout << "COLLADA warning: " << msg << endl;
    }

    boost::shared_ptr<DAE> _collada;
    domCOLLADA* _dom;
    daeDocument* _doc;
    domCOLLADA::domSceneRef _globalscene;
    domLibrary_visual_scenesRef _visualScenesLib;
    domLibrary_kinematics_scenesRef _kinematicsScenesLib;
    domLibrary_kinematics_modelsRef _kinematicsModelsLib;
    domLibrary_articulated_systemsRef _articulatedSystemsLib;
    domLibrary_physics_scenesRef _physicsScenesLib;
    domLibrary_physics_modelsRef _physicsModelsLib;
    domLibrary_materialsRef _materialsLib;
    domLibrary_effectsRef _effectsLib;
    domLibrary_geometriesRef _geometriesLib;
    domTechniqueRef _sensorsLib; ///< custom sensors library
    domTechniqueRef _actuatorsLib; ///< custom actuators library
    int _nextsensorid, _nextactuatorid;
    SCENE _scene;
    std::list<kinbody_models> _listkinbodies;
    std::list<ManipulatorInfo> _listmanipulators;
};

#endif
