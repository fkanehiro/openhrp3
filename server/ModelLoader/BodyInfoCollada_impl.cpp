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
  @file BodyInfoCollada_impl.cpp
  @brief 
  @author Rosen Diankov (rosen.diankov@gmail.com)

  Used OpenRAVE files for reference.
*/
#include "ColladaUtil.h"
#include "BodyInfoCollada_impl.h"
#include "hrpUtil/UrlUtil.h"
#include "hrpCorba/ViewSimulator.hh"

#include <sys/stat.h> // for checkInlineFileUpdateTime

#define FOREACH(it, v) for(typeof((v).begin())it = (v).begin(); it != (v).end(); (it)++)

using namespace std;
using namespace ColladaUtil;

namespace {
    typedef map<string, string> SensorTypeMap;
    SensorTypeMap sensorTypeMap;
}

/// \brief The colladadom reader that fills in the BodyInfoCollada_impl class.
///
/// It is separate from BodyInfoCollada_impl so that the colladadom resources do not have to remain allocated.
class ColladaReader : public daeErrorHandler
{
    class JointAxisBinding
    {
    public:
        JointAxisBinding(daeElementRef pvisualtrans, domAxis_constraintRef pkinematicaxis, domCommon_float_or_paramRef jointvalue, domKinematics_axis_infoRef kinematics_axis_info, domMotion_axis_infoRef motion_axis_info) : pvisualtrans(pvisualtrans), pkinematicaxis(pkinematicaxis), jointvalue(jointvalue), kinematics_axis_info(kinematics_axis_info), motion_axis_info(motion_axis_info) {
            COLLADA_ASSERT( !!pkinematicaxis );
            daeElement* pae = pvisualtrans->getParentElement();
            while (!!pae) {
                visualnode = daeSafeCast<domNode> (pae);
                if (!!visualnode) {
                    break;
                }
                pae = pae->getParentElement();
            }
        
            if (!visualnode) {
                COLLADALOG_WARN(str(boost::format("couldn't find parent node of element id %s, sid %s")%pkinematicaxis->getID()%pkinematicaxis->getSid()));
            }
        }
        
        daeElementRef pvisualtrans;
        domAxis_constraintRef   pkinematicaxis;
        domCommon_float_or_paramRef jointvalue;
        domNodeRef visualnode;
        domKinematics_axis_infoRef kinematics_axis_info;
        domMotion_axis_infoRef motion_axis_info;
    };

    class LinkBinding
    {
    public:
	domNodeRef node;
	domLinkRef domlink;
	domInstance_rigid_bodyRef irigidbody;
	domRigid_bodyRef rigidbody;
	domNodeRef nodephysicsoffset;
    };

    class ConstraintBinding
    {
    public:
	domNodeRef node;
	domInstance_rigid_constraintRef irigidconstraint;
	domRigid_constraintRef rigidconstraint;
    };

    /// \brief inter-collada bindings for a kinematics scene
    class KinematicsSceneBindings
    {
    public:
        std::list< std::pair<domNodeRef,domInstance_kinematics_modelRef> > listKinematicsVisualBindings;
        std::list<JointAxisBinding> listAxisBindings;
        std::list<LinkBinding> listLinkBindings;
        std::list<ConstraintBinding> listConstraintBindings;

        bool AddAxisInfo(const domInstance_kinematics_model_Array& arr, domKinematics_axis_infoRef kinematics_axis_info, domMotion_axis_infoRef motion_axis_info)
        {
            if( !kinematics_axis_info ) {
                return false;
            }
            for(size_t ik = 0; ik < arr.getCount(); ++ik) {
                daeElement* pelt = daeSidRef(kinematics_axis_info->getAxis(), arr[ik]->getUrl().getElement()).resolve().elt;
                if( !!pelt ) {
                    // look for the correct placement
                    bool bfound = false;
                    for(std::list<JointAxisBinding>::iterator itbinding = listAxisBindings.begin(); itbinding != listAxisBindings.end(); ++itbinding) {
                        if( itbinding->pkinematicaxis.cast() == pelt ) {
                            itbinding->kinematics_axis_info = kinematics_axis_info;
                            if( !!motion_axis_info ) {
                                itbinding->motion_axis_info = motion_axis_info;
                            }
                            bfound = true;
                            break;
                        }
                    }
                    if( !bfound ) {
                        COLLADALOG_WARN(str(boost::format("could not find binding for axis: %s, %s")%kinematics_axis_info->getAxis()%pelt->getAttribute("sid")));
                        return false;
                    }
                    return true;
                }
            }
            COLLADALOG_WARN(str(boost::format("could not find kinematics axis target: %s")%kinematics_axis_info->getAxis()));
            return false;
        }
    };

 public:
    ColladaReader() : _dom(NULL), _nGlobalSensorId(0), _nGlobalActuatorId(0), _nGlobalManipulatorId(0), _nGlobalIndex(0) {
        daeErrorHandler::setErrorHandler(this);
        _bOpeningZAE = false;
    }
    virtual ~ColladaReader() {
        _dae.reset();
        //DAE::cleanup();
    }

    bool InitFromURL(const string& url)
    {
        string filename( deleteURLScheme( url ) );

        // URL文字列の' \' 区切り子を'/' に置き換え  Windows ファイルパス対応  
        string url2;
        url2 = filename;
        for(size_t i = 0; i < url2.size(); ++i) {
            if( url2[i] == '\\' ) {
                url2[i] = '/';
            }
        }
        filename = url2;

        COLLADALOG_VERBOSE(str(boost::format("init COLLADA reader version: %s, namespace: %s, filename: %s")%COLLADA_VERSION%COLLADA_NAMESPACE%filename));

        _dae.reset(new DAE);
        _bOpeningZAE = filename.find(".zae") == filename.size()-4;
        _dom = _dae->open(filename);
        _bOpeningZAE = false;
        if (!_dom) {
            return false;
        }
        _filename=filename;
        return _Init();
    }

    bool InitFromData(const string& pdata)
    {
        COLLADALOG_DEBUG(str(boost::format("init COLLADA reader version: %s, namespace: %s")%COLLADA_VERSION%COLLADA_NAMESPACE));
        _dae.reset(new DAE);
        _dom = _dae->openFromMemory(".",pdata.c_str());
        if (!_dom) {
            return false;
        }
        return _Init();
    }

    bool _Init()
    {
        _fGlobalScale = 1;
        if( !!_dom->getAsset() ) {
            if( !!_dom->getAsset()->getUnit() ) {
                _fGlobalScale = _dom->getAsset()->getUnit()->getMeter();
            }
        }
        return true;
    }

    void _ResetRobotCache()
    {
        _mapJointUnits.clear();
        _mapJointIds.clear();
        _mapLinkNames.clear();
        _veclinks.clear();
    }

    /// \extract the first possible robot in the scene
    bool Extract(BodyInfoCollada_impl* probot)
    {
        std::list< pair<domInstance_kinematics_modelRef, boost::shared_ptr<KinematicsSceneBindings> > > listPossibleBodies;
        domCOLLADA::domSceneRef allscene = _dom->getScene();
        if( !allscene ) {
            return false;
        }

        // fill asset info into robot->info_
        //"Step 2 of the Project S"
        //"Author  : Hajime Saito, General Robotix, Inc."
        //"Date    : 2005.11.01"
        //"Modified: 2007.09.29"
        COLLADALOG_WARN("fill asset info");
        _ResetRobotCache();
        
        //  parse each instance kinematics scene, prioritize robots
        for (size_t iscene = 0; iscene < allscene->getInstance_kinematics_scene_array().getCount(); iscene++) {
            domInstance_kinematics_sceneRef kiscene = allscene->getInstance_kinematics_scene_array()[iscene];
            domKinematics_sceneRef kscene = daeSafeCast<domKinematics_scene> (kiscene->getUrl().getElement().cast());
            if (!kscene) {
                continue;
            }
            boost::shared_ptr<KinematicsSceneBindings> bindings(new KinematicsSceneBindings());
            _ExtractKinematicsVisualBindings(allscene->getInstance_visual_scene(),kiscene,*bindings);
	    _ExtractPhysicsBindings(allscene,*bindings);
            for(size_t ias = 0; ias < kscene->getInstance_articulated_system_array().getCount(); ++ias) {
                if( ExtractArticulatedSystem(probot, kscene->getInstance_articulated_system_array()[ias], *bindings) && !!probot ) {
                    PostProcess(probot);
                    return true;
                }
            }
            for(size_t ikmodel = 0; ikmodel < kscene->getInstance_kinematics_model_array().getCount(); ++ikmodel) {
                listPossibleBodies.push_back(make_pair(kscene->getInstance_kinematics_model_array()[ikmodel], bindings));
            }
        }

        for(std::list< pair<domInstance_kinematics_modelRef, boost::shared_ptr<KinematicsSceneBindings> > >::iterator it = listPossibleBodies.begin(); it != listPossibleBodies.end(); ++it) {
            if( ExtractKinematicsModel(probot, it->first, *it->second) ) {
                PostProcess(probot);
                return true;
            }
        }


	// add left-over visual objects
	if (!!allscene->getInstance_visual_scene()) {
            domVisual_sceneRef viscene = daeSafeCast<domVisual_scene>(allscene->getInstance_visual_scene()->getUrl().getElement().cast());
            for (size_t node = 0; node < viscene->getNode_array().getCount(); node++) {
		boost::shared_ptr<KinematicsSceneBindings> bindings(new KinematicsSceneBindings());
		if ( ExtractKinematicsModel(probot, viscene->getNode_array()[node],*bindings,std::vector<std::string>()) ) {
		    PostProcess(probot);
		    return true;
		}
	    }
        }

        return false;
    }

    void PostProcess(BodyInfoCollada_impl* probot)
    {
        probot->links_.length(_veclinks.size());
        probot->linkShapeIndices_.length(_veclinks.size()); 
        for(size_t i = 0; i < _veclinks.size(); ++i) {
            probot->links_[i] = *_veclinks[i];
            probot->linkShapeIndices_[i] = probot->links_[i].shapeIndices;

	    //
	    // segments
	    int numSegment = 1;
	    probot->links_[i].segments.length(numSegment);
	    for ( int s = 0; s < numSegment; ++s ) {
	      SegmentInfo_var segmentInfo(new SegmentInfo());
	      Matrix44 T;
	      hrp::calcRodrigues(T, Vector3(0,0,1), 0.0);
	      int p = 0;
	      for(int row=0; row < 3; ++row){
		for(int col=0; col < 4; ++col){
		  segmentInfo->transformMatrix[p++] = T(row, col);
		}
	      }
	      segmentInfo->name = CORBA::string_dup(probot->links_[i].name);
	      segmentInfo->shapeIndices.length(probot->links_[i].shapeIndices.length());
	      for(int j = 0; j < probot->links_[i].shapeIndices.length(); j++ ) {
		segmentInfo->shapeIndices[j] = j;
	      }
	      segmentInfo->mass = probot->links_[i].mass;
	      segmentInfo->centerOfMass[0] = probot->links_[i].centerOfMass[0];
	      segmentInfo->centerOfMass[1] = probot->links_[i].centerOfMass[1];
	      segmentInfo->centerOfMass[2] = probot->links_[i].centerOfMass[2];
              for(int j = 0; j < 9 ; j++ ) {
                  segmentInfo->inertia[j] = probot->links_[i].inertia[j];
              }
	      probot->links_[i].segments[s] = segmentInfo;
	    }
	}
        //applyTriangleMeshShaper(modelNodeSet.humanoidNode());
    
        // build coldetModels 
        probot->linkColdetModels.resize(_veclinks.size());
        for(size_t linkIndex = 0; linkIndex < _veclinks.size(); ++linkIndex) {
            ColdetModelPtr coldetModel(new ColdetModel());
            coldetModel->setName(probot->links_[linkIndex].name);
            int vertexIndex = 0;
            int triangleIndex = 0;
        
            Matrix44 E(Matrix44::Identity());
            const TransformedShapeIndexSequence& shapeIndices = probot->linkShapeIndices_[linkIndex];
            probot->setColdetModel(coldetModel, shapeIndices, E, vertexIndex, triangleIndex);

            Matrix44 T(Matrix44::Identity());
            const SensorInfoSequence& sensors = probot->links_[linkIndex].sensors;
            for (unsigned int i=0; i<sensors.length(); i++){
                const SensorInfo& sensor = sensors[i];
                calcRodrigues(T, Vector3(sensor.rotation[0], sensor.rotation[1], 
                                         sensor.rotation[2]), sensor.rotation[3]);
                T(0,3) = sensor.translation[0];
                T(1,3) = sensor.translation[1];
                T(2,3) = sensor.translation[2];
                const TransformedShapeIndexSequence& sensorShapeIndices = sensor.shapeIndices;
                probot->setColdetModel(coldetModel, sensorShapeIndices, T, vertexIndex, triangleIndex);
            }          
            if(triangleIndex>0) {
                coldetModel->build();
            }
            probot->linkColdetModels[linkIndex] = coldetModel;
            probot->links_[linkIndex].AABBmaxDepth = coldetModel->getAABBTreeDepth();
            probot->links_[linkIndex].AABBmaxNum = coldetModel->getAABBmaxNum();
        }
    }

    /// \brief extracts an articulated system. Note that an articulated system can include other articulated systems
    /// \param probot the robot to be created from the system
    bool ExtractArticulatedSystem(BodyInfoCollada_impl*& probot, domInstance_articulated_systemRef ias, KinematicsSceneBindings& bindings)
    {
        if( !ias ) {
            return false;
        }
        //COLLADALOG_DEBUG(str(boost::format("instance articulated system sid %s")%ias->getSid()));
        domArticulated_systemRef articulated_system = daeSafeCast<domArticulated_system> (ias->getUrl().getElement().cast());
        if( !articulated_system ) {
            return false;
        }
        if( !probot ) {
            boost::shared_ptr<std::string> pinterface_type = _ExtractInterfaceType(ias->getExtra_array());
            if( !pinterface_type ) {
                pinterface_type = _ExtractInterfaceType(articulated_system->getExtra_array());
            }
            if( !!pinterface_type ) {
                COLLADALOG_WARN(str(boost::format("need to create a robot with type %s")%*(pinterface_type)));
            }
            _ResetRobotCache();
            return false;
        }
        if( probot->url_.size() == 0 ) {
            probot->url_ = _filename;
        }
        if( probot->name_.size() == 0 && !!ias->getName() ) {
            probot->name_ = CORBA::string_dup(ias->getName());
        }
        if( probot->name_.size() == 0 && !!ias->getSid()) {
            probot->name_ = CORBA::string_dup(ias->getSid());
        }
        if( probot->name_.size() == 0 && !!articulated_system->getName() ) {
            probot->name_ = CORBA::string_dup(articulated_system->getName());
        }
        if( probot->name_.size() == 0 && !!articulated_system->getId()) {
            probot->name_ = CORBA::string_dup(articulated_system->getId());
        }

        if( !!articulated_system->getMotion() ) {
            domInstance_articulated_systemRef ias_new = articulated_system->getMotion()->getInstance_articulated_system();
            if( !!articulated_system->getMotion()->getTechnique_common() ) {
                for(size_t i = 0; i < articulated_system->getMotion()->getTechnique_common()->getAxis_info_array().getCount(); ++i) {
                    domMotion_axis_infoRef motion_axis_info = articulated_system->getMotion()->getTechnique_common()->getAxis_info_array()[i];
                    // this should point to a kinematics axis_info
                    domKinematics_axis_infoRef kinematics_axis_info = daeSafeCast<domKinematics_axis_info>(daeSidRef(motion_axis_info->getAxis(), ias_new->getUrl().getElement()).resolve().elt);
                    if( !!kinematics_axis_info ) {
                        // find the parent kinematics and go through all its instance kinematics models
                        daeElement* pparent = kinematics_axis_info->getParent();
                        while(!!pparent && pparent->typeID() != domKinematics::ID()) {
                            pparent = pparent->getParent();
                        }
                        COLLADA_ASSERT(!!pparent);
                        bindings.AddAxisInfo(daeSafeCast<domKinematics>(pparent)->getInstance_kinematics_model_array(), kinematics_axis_info, motion_axis_info);
                    }
                    else {
                        COLLADALOG_WARN(str(boost::format("failed to find kinematics axis %s")%motion_axis_info->getAxis()));
                    }
                }
            }
            if( !ExtractArticulatedSystem(probot,ias_new,bindings) ) {
                return false;
            }
        }
        else {
            if( !articulated_system->getKinematics() ) {
                COLLADALOG_WARN(str(boost::format("collada <kinematics> tag empty? instance_articulated_system=%s")%ias->getID()));
                return true;
            }

            if( !!articulated_system->getKinematics()->getTechnique_common() ) {
                for(size_t i = 0; i < articulated_system->getKinematics()->getTechnique_common()->getAxis_info_array().getCount(); ++i) {
                    bindings.AddAxisInfo(articulated_system->getKinematics()->getInstance_kinematics_model_array(), articulated_system->getKinematics()->getTechnique_common()->getAxis_info_array()[i], NULL);
                }
            }

            // parse the kinematics information
            if (!probot) {
                // create generic robot?
                return false;
            }

            for(size_t ik = 0; ik < articulated_system->getKinematics()->getInstance_kinematics_model_array().getCount(); ++ik) {
                ExtractKinematicsModel(probot,articulated_system->getKinematics()->getInstance_kinematics_model_array()[ik],bindings);
            }
        }

        ExtractRobotManipulators(probot, articulated_system);
        ExtractRobotAttachedSensors(probot, articulated_system);
        ExtractRobotAttachedActuators(probot, articulated_system);
        return true;
    }

    bool ExtractKinematicsModel(BodyInfoCollada_impl* pkinbody, domInstance_kinematics_modelRef ikm, KinematicsSceneBindings& bindings)
    {
        if( !ikm ) {
            return false;
        }
        COLLADALOG_DEBUG(str(boost::format("instance kinematics model sid %s")%ikm->getSid()));
        domKinematics_modelRef kmodel = daeSafeCast<domKinematics_model> (ikm->getUrl().getElement().cast());
        if (!kmodel) {
            COLLADALOG_WARN(str(boost::format("%s does not reference valid kinematics")%ikm->getSid()));
            return false;
        }
        domPhysics_modelRef pmodel;
        if( !pkinbody ) {
            boost::shared_ptr<std::string> pinterface_type = _ExtractInterfaceType(ikm->getExtra_array());
            if( !pinterface_type ) {
                pinterface_type = _ExtractInterfaceType(kmodel->getExtra_array());
            }
            if( !!pinterface_type ) {
                COLLADALOG_WARN(str(boost::format("need to create a robot with type %s")%*pinterface_type));
            }
            _ResetRobotCache();
            return false;
        }
        if( pkinbody->url_.size() == 0 ) {
            pkinbody->url_ = _filename;
        }

        // find matching visual node
        domNodeRef pvisualnode;
        for(std::list< std::pair<domNodeRef,domInstance_kinematics_modelRef> >::iterator it = bindings.listKinematicsVisualBindings.begin(); it != bindings.listKinematicsVisualBindings.end(); ++it) {
            if( it->second == ikm ) {
                pvisualnode = it->first;
                break;
            }
        }
        if( !pvisualnode ) {
            COLLADALOG_WARN(str(boost::format("failed to find visual node for instance kinematics model %s")%ikm->getSid()));
            return false;
        }

        if( pkinbody->name_.size() == 0 && !!ikm->getName() ) {
            pkinbody->name_ = CORBA::string_dup(ikm->getName());
        }
        if( pkinbody->name_.size() == 0 && !!ikm->getID() ) {
            pkinbody->name_ = CORBA::string_dup(ikm->getID());
        }

        if (!ExtractKinematicsModel(pkinbody, kmodel, pvisualnode, pmodel, bindings)) {
            COLLADALOG_WARN(str(boost::format("failed to load kinbody from kinematics model %s")%kmodel->getID()));
            return false;
        }
        return true;
    }

    /// \brief extract one rigid link composed of the node hierarchy
    bool ExtractKinematicsModel(BodyInfoCollada_impl* pkinbody, domNodeRef pdomnode, const KinematicsSceneBindings& bindings, const std::vector<std::string>& vprocessednodes)
    {
        if( !!pdomnode->getID() && find(vprocessednodes.begin(),vprocessednodes.end(),pdomnode->getID()) != vprocessednodes.end() ) {
            return false;
        }
        _ResetRobotCache();
        string name = !pdomnode->getName() ? "" : _ConvertToValidName(pdomnode->getName());
        if( name.size() == 0 ) {
            name = _ConvertToValidName(pdomnode->getID());
        }
        boost::shared_ptr<LinkInfo> plink(new LinkInfo());
        _veclinks.push_back(plink);
        plink->jointId = -1;
        plink->jointType = CORBA::string_dup("free");
        plink->parentIndex = -1;
        plink->name = CORBA::string_dup(name.c_str());
        DblArray12 tlink; PoseIdentity(tlink);
        
	//  Gets the geometry
        bool bhasgeometry = ExtractGeometry(pkinbody,plink,tlink,pdomnode,bindings.listAxisBindings,vprocessednodes);
        if( !bhasgeometry ) {
            return false;
        }

        COLLADALOG_INFO(str(boost::format("Loading non-kinematics node '%s'")%name));
        pkinbody->name_ = name;
        return pkinbody;
    }

    /// \brief append the kinematics model to the kinbody
    bool ExtractKinematicsModel(BodyInfoCollada_impl* pkinbody, domKinematics_modelRef kmodel, domNodeRef pnode, domPhysics_modelRef pmodel, const KinematicsSceneBindings bindings)
    {
        const std::list<JointAxisBinding>& listAxisBindings = bindings.listAxisBindings;
        vector<domJointRef> vdomjoints;
        if (!pkinbody) {
            _ResetRobotCache();
        }
        if( pkinbody->name_.size() == 0 && !!kmodel->getName() ) {
            pkinbody->name_ = CORBA::string_dup(kmodel->getName());
        }
        if( pkinbody->name_.size() == 0 && !!kmodel->getID() ) {
            pkinbody->name_ = CORBA::string_dup(kmodel->getID());
        }
        COLLADALOG_DEBUG(str(boost::format("kinematics model: %s")%pkinbody->name_));
        if( !!pnode ) {
            COLLADALOG_DEBUG(str(boost::format("node name: %s")%pnode->getId()));
        }
        if( !kmodel->getID() ) {
            COLLADALOG_DEBUG(str(boost::format("kinematics model: %s has no id attribute!")%pkinbody->name_));
        }

        //  Process joint of the kinbody
        domKinematics_model_techniqueRef ktec = kmodel->getTechnique_common();

        //  Store joints
        for (size_t ijoint = 0; ijoint < ktec->getJoint_array().getCount(); ++ijoint) {
            vdomjoints.push_back(ktec->getJoint_array()[ijoint]);
        }

        //  Store instances of joints
        for (size_t ijoint = 0; ijoint < ktec->getInstance_joint_array().getCount(); ++ijoint) {
            domJointRef pelt = daeSafeCast<domJoint> (ktec->getInstance_joint_array()[ijoint]->getUrl().getElement());
            if (!pelt) {
                COLLADALOG_WARN("failed to get joint from instance");
            }
            else {
                vdomjoints.push_back(pelt);
            }
        }

        COLLADALOG_VERBOSE(str(boost::format("Number of root links in the kmodel %d")%ktec->getLink_array().getCount()));
        DblArray12 identity;
        PoseIdentity(identity);
        for (size_t ilink = 0; ilink < ktec->getLink_array().getCount(); ++ilink) {
            domLinkRef pdomlink = ktec->getLink_array()[ilink];
            int linkindex = ExtractLink(pkinbody, pdomlink, ilink == 0 ? pnode : domNodeRef(), identity, vdomjoints, bindings);
            // root link
            DblArray12 tlocallink;
            _ExtractFullTransform(tlocallink,pdomlink);
	    boost::shared_ptr<LinkInfo> plink = _veclinks.at(linkindex);
            AxisAngleTranslationFromPose(plink->rotation,plink->translation,tlocallink);
        }

        for (size_t iform = 0; iform < ktec->getFormula_array().getCount(); ++iform) {
            domFormulaRef pf = ktec->getFormula_array()[iform];
            if (!pf->getTarget()) {
                COLLADALOG_WARN("formula target not valid");
                continue;
            }

            // find the target joint
            boost::shared_ptr<LinkInfo>  pjoint = _getJointFromRef(pf->getTarget()->getParam()->getValue(),pf,pkinbody).first;
            if (!pjoint) {
                continue;
            }

            int iaxis = 0;
            dReal ftargetunit = 1;
            if(_mapJointUnits.find(pjoint) != _mapJointUnits.end() ) {
                ftargetunit = _mapJointUnits[pjoint].at(iaxis);
            }

            daeTArray<daeElementRef> children;
            pf->getTechnique_common()->getChildren(children);

            domTechniqueRef popenravetec = _ExtractOpenRAVEProfile(pf->getTechnique_array());
            if( !!popenravetec ) {
                for(size_t ic = 0; ic < popenravetec->getContents().getCount(); ++ic) {
                    daeElementRef pequation = popenravetec->getContents()[ic];
                    if( pequation->getElementName() == string("equation") ) {
                        if( !pequation->hasAttribute("type") ) {
                            COLLADALOG_WARN("equaiton needs 'type' attribute, ignoring");
                            continue;
                        }
                        if( children.getCount() != 1 ) {
                            COLLADALOG_WARN("equaiton needs exactly one child");
                            continue;
                        }
                        std::string equationtype = pequation->getAttribute("type");
                        boost::shared_ptr<LinkInfo>  pjointtarget;
                        if( pequation->hasAttribute("target") ) {
                            pjointtarget = _getJointFromRef(pequation->getAttribute("target").c_str(),pf,pkinbody).first;
                        }
                        try {
                            std::string eq = _ExtractMathML(pf,pkinbody,children[0]);
                            if( ftargetunit != 1 ) {
                                eq = str(boost::format("%f*(%s)")%ftargetunit%eq);
                            }
                            if( equationtype == "position" ) {
                                COLLADALOG_WARN(str(boost::format("cannot set joint %s position equation: %s!")%pjoint->name%eq));
                            }
                            else if( equationtype == "first_partial" ) {
                                if( !pjointtarget ) {
                                    COLLADALOG_WARN(str(boost::format("first_partial equation '%s' needs a target attribute! ignoring...")%eq));
                                    continue;
                                }
                                COLLADALOG_WARN(str(boost::format("cannot set joint %s first partial equation: d %s=%s!")%pjoint->name%pjointtarget->name%eq));
                            }
                            else if( equationtype == "second_partial" ) {
                                if( !pjointtarget ) {
                                    COLLADALOG_WARN(str(boost::format("second_partial equation '%s' needs a target attribute! ignoring...")%eq));
                                    continue;
                                }
                                COLLADALOG_WARN(str(boost::format("cannot set joint %s second partial equation: d^2 %s = %s!")%pjoint->name%pjointtarget->name%eq));
                            }
                            else {
                                COLLADALOG_WARN(str(boost::format("unknown equation type %s")%equationtype));
                            }
                        }
                        catch(const ModelLoader::ModelLoaderException& ex) {
                            COLLADALOG_WARN(str(boost::format("failed to parse formula %s for target %s")%equationtype%pjoint->name));
                        }
                    }
                }
            }
            else if (!!pf->getTechnique_common()) {
                try {
                    for(size_t ic = 0; ic < children.getCount(); ++ic) {
                        string eq = _ExtractMathML(pf,pkinbody,children[ic]);
                        if( ftargetunit != 1 ) {
                            eq = str(boost::format("%f*(%s)")%ftargetunit%eq);
                        }
                        if( eq.size() > 0 ) {
                            COLLADALOG_WARN(str(boost::format("cannot set joint %s position equation: %s!")%pjoint->name%eq));
                            break;
                        }
                    }
                }
                catch(const ModelLoader::ModelLoaderException& ex) {
                    COLLADALOG_WARN(str(boost::format("failed to parse formula for target %s: %s")%pjoint->name%ex.description));
                }
            }
        }

        // read the collision data
        for(size_t ie = 0; ie < kmodel->getExtra_array().getCount(); ++ie) {
            domExtraRef pextra = kmodel->getExtra_array()[ie];
            if( strcmp(pextra->getType(), "collision") == 0 ) {
                domTechniqueRef tec = _ExtractOpenRAVEProfile(pextra->getTechnique_array());
                if( !!tec ) {
                    for(size_t ic = 0; ic < tec->getContents().getCount(); ++ic) {
                        daeElementRef pelt = tec->getContents()[ic];
                        if( pelt->getElementName() == string("ignore_link_pair") ) {
                            domLinkRef pdomlink0 = daeSafeCast<domLink>(daeSidRef(pelt->getAttribute("link0"), kmodel).resolve().elt);
                            domLinkRef pdomlink1 = daeSafeCast<domLink>(daeSidRef(pelt->getAttribute("link1"), kmodel).resolve().elt);
                            if( !pdomlink0 || !pdomlink1 ) {
                                COLLADALOG_WARN(str(boost::format("failed to reference <ignore_link_pair> links: %s %s")%pelt->getAttribute("link0")%pelt->getAttribute("link1")));
                                continue;
                            }
                            COLLADALOG_INFO(str(boost::format("need to specifying ignore link pair %s:%s")%_ExtractLinkName(pdomlink0)%_ExtractLinkName(pdomlink1)));
                        }
                        else if( pelt->getElementName() == string("bind_instance_geometry") ) {
                            COLLADALOG_WARN("currently do not support bind_instance_geometry");
                        }
                    }
                }
            }
        }
        return true;
    }

    boost::shared_ptr<LinkInfo> GetLink(const std::string& name)
    {
        for(std::map<std::string,boost::shared_ptr<LinkInfo> >::iterator it = _mapJointIds.begin(); it != _mapJointIds.end(); ++it) {
            string linkname(CORBA::String_var(it->second->name));
            if( linkname == name ) {
                return it->second;
            }
        }
        return boost::shared_ptr<LinkInfo>();
    }

    ///  \brief Extract Link info and add it to an existing body
    int  ExtractLink(BodyInfoCollada_impl* pkinbody, const domLinkRef pdomlink,const domNodeRef pdomnode, const DblArray12& tParentLink, const std::vector<domJointRef>& vdomjoints, const KinematicsSceneBindings bindings) {
        const std::list<JointAxisBinding>& listAxisBindings = bindings.listAxisBindings;

        //  Set link name with the name of the COLLADA's Link
        std::string linkname;
        if( !!pdomlink ) {
            linkname = _ExtractLinkName(pdomlink);
            if( linkname.size() == 0 ) {
                COLLADALOG_WARN("<link> has no name or id, falling back to <node>!");
            }
        }
        if( linkname.size() == 0 ) {
            if( !!pdomnode ) {
                if (!!pdomnode->getName()) {
                    linkname = _ConvertToValidName(pdomnode->getName());
                }
                if( linkname.size() == 0 && !!pdomnode->getID()) {
                    linkname = _ConvertToValidName(pdomnode->getID());
                }
            }
        }

        boost::shared_ptr<LinkInfo>  plink(new LinkInfo());
        plink->parentIndex = -1;
        plink->jointId = -1;
        plink->mass = 1;
        plink->centerOfMass[0] = plink->centerOfMass[1] = plink->centerOfMass[2] = 0;
        plink->inertia[0] = plink->inertia[4] = plink->inertia[8] = 1;
        plink->jointValue = 0;
	_mapLinkNames[linkname] = plink;
        plink->name = CORBA::string_dup(linkname.c_str());
        plink->jointType = CORBA::string_dup("free");
        int ilinkindex = (int)_veclinks.size();
        _veclinks.push_back(plink);

        if( !!pdomnode ) {
            COLLADALOG_VERBOSE(str(boost::format("Node Id %s and Name %s")%pdomnode->getId()%pdomnode->getName()));
        }

        // physics
        domInstance_rigid_bodyRef irigidbody;
        domRigid_bodyRef rigidbody;
        domInstance_rigid_constraintRef irigidconstraint;
        bool bFoundBinding = false;
        FOREACH(itlinkbinding, bindings.listLinkBindings) {
            if( !!pdomnode->getID() && !!itlinkbinding->node->getID() && strcmp(pdomnode->getID(),itlinkbinding->node->getID()) == 0 ) {
                bFoundBinding = true;
                irigidbody = itlinkbinding->irigidbody;
                rigidbody = itlinkbinding->rigidbody;
            }
        }
	FOREACH(itconstraintbinding, bindings.listConstraintBindings) {
	    if( !!pdomnode->getID() && !!itconstraintbinding->rigidconstraint->getName() && strcmp(linkname.c_str(),itconstraintbinding->rigidconstraint->getName()) == 0 ) {
		plink->jointType = CORBA::string_dup("fixed");
	    }
	}

        if (!pdomlink) {
            ExtractGeometry(pkinbody,plink,tParentLink,pdomnode,listAxisBindings,std::vector<std::string>());
        }
        else {
            COLLADALOG_DEBUG(str(boost::format("Attachment link elements: %d")%pdomlink->getAttachment_full_array().getCount()));
            // use the kinematics coordinate system for each link
            DblArray12 tlink,tlocallink;
            _ExtractFullTransform(tlocallink,pdomlink);
            PoseMult(tlink,tParentLink,tlocallink);
          
            // Get the geometry
            ExtractGeometry(pkinbody,plink,tlink,pdomnode,listAxisBindings,std::vector<std::string>());
            
            COLLADALOG_DEBUG(str(boost::format("After ExtractGeometry Attachment link elements: %d")%pdomlink->getAttachment_full_array().getCount()));
          
            if( !!rigidbody && !!rigidbody->getTechnique_common() ) {
                domRigid_body::domTechnique_commonRef rigiddata = rigidbody->getTechnique_common();
                if( !!rigiddata->getMass() ) {
		    plink->mass = rigiddata->getMass()->getValue();
                }
                if( !!rigiddata->getInertia() ) {
                    plink->inertia[0] = rigiddata->getInertia()->getValue()[0];
                    plink->inertia[4] = rigiddata->getInertia()->getValue()[1];
                    plink->inertia[8] = rigiddata->getInertia()->getValue()[2];
                }
                if( !!rigiddata->getMass_frame() ) {
                     DblArray12 tmass, tframe, tlocalmass;
                     PoseInverse(tframe,tlink);
                     _ExtractFullTransform(tmass, rigiddata->getMass_frame());
                     PoseMult(tlocalmass,tframe,tmass);
                     plink->centerOfMass[0] = tlocalmass[3];
                     plink->centerOfMass[1] = tlocalmass[7];
                     plink->centerOfMass[2] = tlocalmass[11];
                }
	    }

            //  Process all atached links
            for (size_t iatt = 0; iatt < pdomlink->getAttachment_full_array().getCount(); ++iatt) {
                domLink::domAttachment_fullRef pattfull = pdomlink->getAttachment_full_array()[iatt];

                // get link kinematics transformation
                DblArray12 tatt;
                _ExtractFullTransform(tatt,pattfull);

                // Transform applied to the joint
                // the joint anchor is actually tatt.trans! However, in openhrp3 the link and joint coordinate systems are the same!
                // this means we need to change the coordinate system of the joint and all the attached geometry
                //dReal anchor[3] = {tatt[3],tatt[7],tatt[11]};
                //tatt[3] = 0; tatt[7] = 0; tatt[11] = 0;
                //COLLADALOG_INFO(str(boost::format("tatt: %f %f %f")%anchor[0]%anchor[1]%anchor[2]));

                // process attached links
                daeElement* peltjoint = daeSidRef(pattfull->getJoint(), pattfull).resolve().elt;
                if( !peltjoint ) {
                    COLLADALOG_WARN(str(boost::format("could not find attached joint %s!")%pattfull->getJoint()));
                    continue;
                }
                string jointid;
                if( string(pattfull->getJoint()).find("./") == 0 ) {
                    jointid = str(boost::format("%s/%s")%_ExtractParentId(pattfull)%&pattfull->getJoint()[1]);
                }
                else {
                    jointid = pattfull->getJoint();
                }
                    
                domJointRef pdomjoint = daeSafeCast<domJoint> (peltjoint);
                if (!pdomjoint) {
                    domInstance_jointRef pdomijoint = daeSafeCast<domInstance_joint> (peltjoint);
                    if (!!pdomijoint) {
                        pdomjoint = daeSafeCast<domJoint> (pdomijoint->getUrl().getElement().cast());
                    }
                    else {
                        COLLADALOG_WARN(str(boost::format("could not cast element <%s> to <joint>!")%peltjoint->getElementName()));
                        continue;
                    }
                }
                
                // get direct child link
                if (!pattfull->getLink()) {
                    COLLADALOG_WARN(str(boost::format("joint %s needs to be attached to a valid link")%jointid));
                    continue;
                }
                
                // find the correct joint in the bindings
                daeTArray<domAxis_constraintRef> vdomaxes = pdomjoint->getChildrenByType<domAxis_constraint>();
                domNodeRef pchildnode;
                
                // see if joint has a binding to a visual node
                for(std::list<JointAxisBinding>::const_iterator itaxisbinding = listAxisBindings.begin(); itaxisbinding != listAxisBindings.end(); ++itaxisbinding) {
                    for (size_t ic = 0; ic < vdomaxes.getCount(); ++ic) {
                        //  If the binding for the joint axis is found, retrieve the info
                        if (vdomaxes[ic] == itaxisbinding->pkinematicaxis) {
                            pchildnode = itaxisbinding->visualnode;
                            break;
                        }
                    }
                    if( !!pchildnode ) {
                        break;
                    }
                }
                if (!pchildnode) {
                    COLLADALOG_DEBUG(str(boost::format("joint %s has no visual binding")%jointid));
                }

                
                DblArray12 tnewparent;
                PoseMult(tnewparent,tlink,tatt);
                int ijointindex = ExtractLink(pkinbody, pattfull->getLink(), pchildnode, tnewparent, vdomjoints, bindings);
                boost::shared_ptr<LinkInfo> pjoint = _veclinks.at(ijointindex);
                int cindex = plink->childIndices.length();
                plink->childIndices.length(cindex+1);
                plink->childIndices[cindex] = ijointindex;
                pjoint->parentIndex = ilinkindex;

                AxisAngleTranslationFromPose(pjoint->rotation,pjoint->translation,tatt);

                bool bActive = true; // if not active, put into the passive list

                if( vdomaxes.getCount() > 1 ) {
                    COLLADALOG_WARN(str(boost::format("joint %s has %d degrees of freedom, only 1 DOF is supported")%pjoint->name%vdomaxes.getCount()));
                }
                else if( vdomaxes.getCount() == 0 ) {
                    continue;
                }

                size_t ic = 0;
                std::vector<dReal> vaxisunits(1,dReal(1));
                for(std::list<JointAxisBinding>::const_iterator itaxisbinding = listAxisBindings.begin(); itaxisbinding != listAxisBindings.end(); ++itaxisbinding) {
                    if (vdomaxes[ic] == itaxisbinding->pkinematicaxis) {
                        if( !!itaxisbinding->kinematics_axis_info ) {
                            if( !!itaxisbinding->kinematics_axis_info->getActive() ) {
                                // what if different axes have different active profiles?
                                bActive = resolveBool(itaxisbinding->kinematics_axis_info->getActive(),itaxisbinding->kinematics_axis_info);
                            }
                        }
                        break;
                    }
                }
                domAxis_constraintRef pdomaxis = vdomaxes[ic];
                bool bIsRevolute = false;
                if( strcmp(pdomaxis->getElementName(), "revolute") == 0 ) {
                    pjoint->jointType = CORBA::string_dup("rotate");
                    bIsRevolute = true;
                }
                else if( strcmp(pdomaxis->getElementName(), "prismatic") == 0 ) {
                    pjoint->jointType = CORBA::string_dup("slide");
                    vaxisunits[ic] = _GetUnitScale(pdomaxis,_fGlobalScale);
                }
                else {
                    COLLADALOG_WARN(str(boost::format("unsupported joint type: %s")%pdomaxis->getElementName()));
                }

                _mapJointUnits[pjoint] = vaxisunits;
                string jointname;
                if( !!pdomjoint->getName() ) {
                    jointname = _ConvertToValidName(pdomjoint->getName());
                }
                else {
                    jointname = str(boost::format("dummy%d")%pjoint->jointId);
                }
                pjoint->name = CORBA::string_dup(jointname.c_str());
                
                if( _mapJointIds.find(jointid) != _mapJointIds.end() ) {
                    COLLADALOG_WARN(str(boost::format("jointid '%s' is duplicated!")%jointid));
                }
		
		int jointsid;
		if ( sscanf(jointid.c_str(), "kmodel1/jointsid%d", &jointsid) ) {
		    pjoint->jointId = jointsid;
		} else {
		    pjoint->jointId = ijointindex - 1;
		}
                _mapJointIds[jointid] = pjoint;
                COLLADALOG_DEBUG(str(boost::format("joint %s (%d)")%pjoint->name%pjoint->jointId));

                domKinematics_axis_infoRef kinematics_axis_info;
                domMotion_axis_infoRef motion_axis_info;
                for(std::list<JointAxisBinding>::const_iterator itaxisbinding = listAxisBindings.begin(); itaxisbinding != listAxisBindings.end(); ++itaxisbinding) {
                    bool bfound = false;
                    if (vdomaxes[ic] == itaxisbinding->pkinematicaxis) {
                        kinematics_axis_info = itaxisbinding->kinematics_axis_info;
                        motion_axis_info = itaxisbinding->motion_axis_info;
                        bfound = true;
                        break;
                    }
                }

                //  Axes and Anchor assignment.
                dReal len2 = 0;
                for(int i = 0; i < 3; ++i) {
                    len2 += pdomaxis->getAxis()->getValue()[i] * pdomaxis->getAxis()->getValue()[i];
                }
                if( len2 > 0 ) {
                    len2 = 1/len2;
                    pjoint->jointAxis[0] = pdomaxis->getAxis()->getValue()[0]*len2;
                    pjoint->jointAxis[1] = pdomaxis->getAxis()->getValue()[1]*len2;
                    pjoint->jointAxis[2] = pdomaxis->getAxis()->getValue()[2]*len2;
                }
                else {
                    pjoint->jointAxis[0] = 0;
                    pjoint->jointAxis[1] = 0;
                    pjoint->jointAxis[2] = 1;
                }
                //  Rotate axis from the parent offset
                //PoseRotateVector(pjoint->jointAxis,tatt,pjoint->jointAxis);
                COLLADALOG_DEBUG(str(boost::format("joint %s has axis: %f %f %f")%jointname%pjoint->jointAxis[0]%pjoint->jointAxis[1]%pjoint->jointAxis[2]));

                if( !motion_axis_info ) {
                    COLLADALOG_WARN(str(boost::format("No motion axis info for joint %s")%pjoint->name));
                }

                //  Sets the Speed and the Acceleration of the joint
                if (!!motion_axis_info) {
                    if (!!motion_axis_info->getSpeed()) {
                        pjoint->lvlimit.length(1);
                        pjoint->uvlimit.length(1);
                        pjoint->uvlimit[0] = resolveFloat(motion_axis_info->getSpeed(),motion_axis_info);
                        pjoint->lvlimit[0] = -pjoint->uvlimit[0];
                    }
                    if (!!motion_axis_info->getAcceleration()) {
                        COLLADALOG_DEBUG("robot has max acceleration info");
                    }
                }

                bool joint_locked = false; // if locked, joint angle is static
                bool kinematics_limits = false; 

                if (!!kinematics_axis_info) {
                    if (!!kinematics_axis_info->getLocked()) {
                        joint_locked = resolveBool(kinematics_axis_info->getLocked(),kinematics_axis_info);
                    }
                        
                    if (joint_locked) { // If joint is locked set limits to the static value.
                        COLLADALOG_WARN("lock joint!!");
                        pjoint->llimit.length(1);
                        pjoint->ulimit.length(1);
                        pjoint->llimit[ic] = 0;
                        pjoint->ulimit[ic] = 0;
                    }
                    else if (kinematics_axis_info->getLimits()) { // If there are articulated system kinematics limits
                        kinematics_limits   = true;
                        pjoint->llimit.length(1);
                        pjoint->ulimit.length(1);
                        dReal fscale = bIsRevolute?(M_PI/180.0f):_GetUnitScale(kinematics_axis_info,_fGlobalScale);
                        pjoint->llimit[ic] = fscale*(dReal)(resolveFloat(kinematics_axis_info->getLimits()->getMin(),kinematics_axis_info));
                        pjoint->ulimit[ic] = fscale*(dReal)(resolveFloat(kinematics_axis_info->getLimits()->getMax(),kinematics_axis_info));
                    }
                }
                  
                //  Search limits in the joints section
                if (!kinematics_axis_info || (!joint_locked && !kinematics_limits)) {
                    //  If there are NO LIMITS
                    if( !!pdomaxis->getLimits() ) {
                        dReal fscale = bIsRevolute?(M_PI/180.0f):_GetUnitScale(pdomaxis,_fGlobalScale);
                        pjoint->llimit.length(1);
                        pjoint->ulimit.length(1);
                        pjoint->llimit[ic] = (dReal)pdomaxis->getLimits()->getMin()->getValue()*fscale;
                        pjoint->ulimit[ic] = (dReal)pdomaxis->getLimits()->getMax()->getValue()*fscale;
                    }
                    else {
                        COLLADALOG_VERBOSE(str(boost::format("There are NO LIMITS in joint %s:%d ...")%pjoint->name%kinematics_limits));
                    }
                }
            }
            if( pdomlink->getAttachment_start_array().getCount() > 0 ) {
                COLLADALOG_WARN("openrave collada reader does not support attachment_start");
            }
            if( pdomlink->getAttachment_end_array().getCount() > 0 ) {
                COLLADALOG_WARN("openrave collada reader does not support attachment_end");
            }
        }
        return ilinkindex;
    }

    /// Extract Geometry and apply the transformations of the node
    /// \param pdomnode Node to extract the goemetry
    /// \param plink    Link of the kinematics model
    bool ExtractGeometry(BodyInfoCollada_impl* pkinbody, boost::shared_ptr<LinkInfo>  plink, const DblArray12& tlink, const domNodeRef pdomnode, const std::list<JointAxisBinding>& listAxisBindings,const std::vector<std::string>& vprocessednodes)
    {
        if( !pdomnode ) {
            return false;
        }
        if( !!pdomnode->getID() && find(vprocessednodes.begin(),vprocessednodes.end(),pdomnode->getID()) != vprocessednodes.end() ) {
            return false;
        }

        COLLADALOG_VERBOSE(str(boost::format("ExtractGeometry(node,link) of %s")%pdomnode->getName()));

        bool bhasgeometry = false;
        // For all child nodes of pdomnode
        for (size_t i = 0; i < pdomnode->getNode_array().getCount(); i++) {
            // check if contains a joint
            bool contains=false;
            for(std::list<JointAxisBinding>::const_iterator it = listAxisBindings.begin(); it != listAxisBindings.end(); ++it) {
                // don't check ID's check if the reference is the same!
                if ( (pdomnode->getNode_array()[i])  == (it->visualnode)){
                    contains=true;
                    break;
                }
            }
            if (contains) {
                continue;
            }

            bhasgeometry |= ExtractGeometry(pkinbody, plink, tlink, pdomnode->getNode_array()[i],listAxisBindings,vprocessednodes);
            // Plink stayes the same for all children
            // replace pdomnode by child = pdomnode->getNode_array()[i]
            // hope for the best!
            // put everything in a subroutine in order to process pdomnode too!
        }

        unsigned int nGeomBefore =  plink->shapeIndices.length(); // #of Meshes already associated to this link

        // get the geometry
        for (size_t igeom = 0; igeom < pdomnode->getInstance_geometry_array().getCount(); ++igeom) {
            domInstance_geometryRef domigeom = pdomnode->getInstance_geometry_array()[igeom];
            domGeometryRef domgeom = daeSafeCast<domGeometry> (domigeom->getUrl().getElement());
            if (!domgeom) {
                COLLADALOG_WARN(str(boost::format("link %s does not have valid geometry")%plink->name));
                continue;
            }

            //  Gets materials
            map<string, int> mapmaterials;
            if (!!domigeom->getBind_material() && !!domigeom->getBind_material()->getTechnique_common()) {
                const domInstance_material_Array& matarray = domigeom->getBind_material()->getTechnique_common()->getInstance_material_array();
                for (size_t imat = 0; imat < matarray.getCount(); ++imat) {
                    domMaterialRef pdommat = daeSafeCast<domMaterial>(matarray[imat]->getTarget().getElement());
                    if (!!pdommat) {
                        int mindex = pkinbody->materials_.length();
                        pkinbody->materials_.length(mindex+1);
                        _FillMaterial(pkinbody->materials_[mindex],pdommat);
                        int aindex = pkinbody->appearances_.length();
                        pkinbody->appearances_.length(aindex+1);
                        AppearanceInfo& ainfo = pkinbody->appearances_[aindex];
                        ainfo.materialIndex = mindex;
                        ainfo.normalPerVertex = 0;
                        ainfo.solid = 0;
                        ainfo.creaseAngle = 0;
                        ainfo.colorPerVertex = 0;
                        ainfo.textureIndex = -1;
                        mapmaterials[matarray[imat]->getSymbol()] = aindex;
                    }
                }
            }

            //  Gets the geometry
            bhasgeometry |= ExtractGeometry(pkinbody, plink, domgeom, mapmaterials);
        }

        if( !bhasgeometry ) {
            return false;
        }

        DblArray12 tmnodegeom, ttemp, ttemp2;
        PoseInverse(tmnodegeom,tlink);
        getNodeParentTransform(ttemp2,pdomnode);
        PoseMult(ttemp,tmnodegeom,ttemp2);
        _ExtractFullTransform(ttemp2, pdomnode);
        PoseMult(tmnodegeom,ttemp,ttemp2);

        // there is a weird bug with the viewer, but should try to normalize the rotation!
        DblArray4 quat;
        double x=tmnodegeom[3],y=tmnodegeom[7],z=tmnodegeom[11];
        QuatFromMatrix(quat, tmnodegeom);
        PoseFromQuat(tmnodegeom,quat);
        tmnodegeom[3] = x; tmnodegeom[7] = y; tmnodegeom[11] = z;

        // change only the transformations of the newly found geometries.
        for(int i = nGeomBefore; i < plink->shapeIndices.length(); ++i) {
            memcpy(plink->shapeIndices[i].transformMatrix,tmnodegeom,sizeof(tmnodegeom));
            plink->shapeIndices[i].inlinedShapeTransformMatrixIndex = -1;
        }

        return true;
    }

    /// Extract the Geometry in TRIANGLES and adds it to OpenRave
    /// \param  triRef  Array of triangles of the COLLADA's model
    /// \param  vertsRef    Array of vertices of the COLLADA's model
    /// \param  mapmaterials    Materials applied to the geometry
    /// \param  plink   Link of the kinematics model
    bool _ExtractGeometry(BodyInfoCollada_impl* pkinbody, boost::shared_ptr<LinkInfo> plink, const domTrianglesRef triRef, const domVerticesRef vertsRef, const map<string,int>& mapmaterials)
    {
        if( !triRef ) {
            return false;
        }
        int shapeIndex = pkinbody->shapes_.length();
        pkinbody->shapes_.length(shapeIndex+1);
        ShapeInfo& shape = pkinbody->shapes_[shapeIndex];
        shape.primitiveType = SP_MESH;
	//shape.url = "";
        int lsindex = plink->shapeIndices.length();
        plink->shapeIndices.length(lsindex+1);
        plink->shapeIndices[lsindex].shapeIndex = shapeIndex;

        // resolve the material and assign correct colors to the geometry
        shape.appearanceIndex = -1;
        if( !!triRef->getMaterial() ) {
            map<string,int>::const_iterator itmat = mapmaterials.find(triRef->getMaterial());
            if( itmat != mapmaterials.end() ) {
                shape.appearanceIndex = itmat->second;
            }
        }

        size_t triangleIndexStride = 0, vertexoffset = -1;
        domInput_local_offsetRef indexOffsetRef;

        for (unsigned int w=0;w<triRef->getInput_array().getCount();w++) {
            size_t offset = triRef->getInput_array()[w]->getOffset();
            daeString str = triRef->getInput_array()[w]->getSemantic();
            if (!strcmp(str,"VERTEX")) {
                indexOffsetRef = triRef->getInput_array()[w];
                vertexoffset = offset;
            }
            if (offset> triangleIndexStride) {
                triangleIndexStride = offset;
            }
        }
        triangleIndexStride++;

        const domList_of_uints& indexArray =triRef->getP()->getValue();
        shape.triangles.length(triRef->getCount()*3);
        shape.vertices.length(triRef->getCount()*9);
        int itriangle = 0, ivertex = 0;
        for (size_t i=0;i<vertsRef->getInput_array().getCount();++i) {
            domInput_localRef localRef = vertsRef->getInput_array()[i];
            daeString str = localRef->getSemantic();
            if ( strcmp(str,"POSITION") == 0 ) {
                const domSourceRef node = daeSafeCast<domSource>(localRef->getSource().getElement());
                if( !node ) {
                    continue;
                }
                dReal fUnitScale = _GetUnitScale(node,_fGlobalScale);
                const domFloat_arrayRef flArray = node->getFloat_array();
                if (!!flArray) {
                    const domList_of_floats& listFloats = flArray->getValue();
                    int k=vertexoffset;
                    int vertexStride = 3;//instead of hardcoded stride, should use the 'accessor'
                    for(size_t itri = 0; itri < triRef->getCount(); ++itri) {
                        if(k+2*triangleIndexStride < indexArray.getCount() ) {
                            for (int j=0;j<3;j++) {
                                int index0 = indexArray.get(k)*vertexStride;
                                domFloat fl0 = listFloats.get(index0);
                                domFloat fl1 = listFloats.get(index0+1);
                                domFloat fl2 = listFloats.get(index0+2);
                                k+=triangleIndexStride;
                                shape.triangles[itriangle++] = ivertex/3;
                                shape.vertices[ivertex++] = fl0*fUnitScale;
                                shape.vertices[ivertex++] = fl1*fUnitScale;
                                shape.vertices[ivertex++] = fl2*fUnitScale;
                            }
                        }
                    }
                }
                else {
                    COLLADALOG_WARN("float array not defined!");
                }
                break;
            }
        }

        if( !!triRef->getMaterial() ) {
            map<string,int>::const_iterator itmat = mapmaterials.find(triRef->getMaterial());
            if( itmat != mapmaterials.end() ) {
		AppearanceInfo& ainfo = pkinbody->appearances_[itmat->second];
		ainfo.normals.length(itriangle);
		for(size_t i=0; i < itriangle/3; ++i) {
		    Vector3 a(shape.vertices[shape.triangles[i*3+0]*3+0]-
			      shape.vertices[shape.triangles[i*3+2]*3+0], 
			      shape.vertices[shape.triangles[i*3+0]*3+1]-
			      shape.vertices[shape.triangles[i*3+2]*3+1],
			      shape.vertices[shape.triangles[i*3+0]*3+2]-
			      shape.vertices[shape.triangles[i*3+2]*3+2]);
		    Vector3 b(shape.vertices[shape.triangles[i*3+1]*3+0]-
			      shape.vertices[shape.triangles[i*3+2]*3+0],
			      shape.vertices[shape.triangles[i*3+1]*3+1]-
			      shape.vertices[shape.triangles[i*3+2]*3+1],
			      shape.vertices[shape.triangles[i*3+1]*3+2]-
			      shape.vertices[shape.triangles[i*3+2]*3+2]);
		    a.normalize();
		    b.normalize();
		    Vector3 c = a.cross(b);
		    ainfo.normals[i*3+0] = c[0];
		    ainfo.normals[i*3+1] = c[1];
		    ainfo.normals[i*3+2] = c[2];
		}
            }
        }

        if( itriangle != 3*triRef->getCount() ) {
            COLLADALOG_WARN("triangles declares wrong count!");
        }
        return true;
    }

    /// Extract the Geometry in TRIGLE FANS and adds it to OpenRave
    /// \param  triRef  Array of triangle fans of the COLLADA's model
    /// \param  vertsRef    Array of vertices of the COLLADA's model
    /// \param  mapmaterials    Materials applied to the geometry
    /// \param  plink   Link of the kinematics model
    bool _ExtractGeometry(BodyInfoCollada_impl* pkinbody, boost::shared_ptr<LinkInfo> plink, const domTrifansRef triRef, const domVerticesRef vertsRef, const map<string,int>& mapmaterials)
    {
        if( !triRef ) {
            return false;
        }
        int shapeIndex = pkinbody->shapes_.length();
        pkinbody->shapes_.length(shapeIndex+1);
        ShapeInfo& shape = pkinbody->shapes_[shapeIndex];
        shape.primitiveType = SP_MESH;
        int lsindex = plink->shapeIndices.length();
        plink->shapeIndices.length(lsindex+1);
        plink->shapeIndices[lsindex].shapeIndex = shapeIndex;

        // resolve the material and assign correct colors to the geometry
        shape.appearanceIndex = -1;
        if( !!triRef->getMaterial() ) {
            map<string,int>::const_iterator itmat = mapmaterials.find(triRef->getMaterial());
            if( itmat != mapmaterials.end() ) {
                shape.appearanceIndex = itmat->second;
            }
        }

        size_t triangleIndexStride = 0, vertexoffset = -1;
        domInput_local_offsetRef indexOffsetRef;

        for (unsigned int w=0;w<triRef->getInput_array().getCount();w++) {
            size_t offset = triRef->getInput_array()[w]->getOffset();
            daeString str = triRef->getInput_array()[w]->getSemantic();
            if (!strcmp(str,"VERTEX")) {
                indexOffsetRef = triRef->getInput_array()[w];
                vertexoffset = offset;
            }
            if (offset> triangleIndexStride) {
                triangleIndexStride = offset;
            }
        }
        triangleIndexStride++;
        size_t primitivecount = triRef->getCount();
        if( primitivecount > triRef->getP_array().getCount() ) {
            COLLADALOG_WARN("trifans has incorrect count");
            primitivecount = triRef->getP_array().getCount();
        }
        int itriangle = 0, ivertex = 0;
        for(size_t ip = 0; ip < primitivecount; ++ip) {
            domList_of_uints indexArray =triRef->getP_array()[ip]->getValue();
            for (size_t i=0;i<vertsRef->getInput_array().getCount();++i) {
                domInput_localRef localRef = vertsRef->getInput_array()[i];
                daeString str = localRef->getSemantic();
                if ( strcmp(str,"POSITION") == 0 ) {
                    const domSourceRef node = daeSafeCast<domSource>(localRef->getSource().getElement());
                    if( !node ) {
                        continue;
                    }
                    dReal fUnitScale = _GetUnitScale(node,_fGlobalScale);
                    const domFloat_arrayRef flArray = node->getFloat_array();
                    if (!!flArray) {
                        const domList_of_floats& listFloats = flArray->getValue();
                        int k=vertexoffset;
                        int vertexStride = 3;//instead of hardcoded stride, should use the 'accessor'
                        size_t usedindices = 3*(indexArray.getCount()-2);
                        shape.triangles.length(shape.triangles.length()+usedindices);
                        shape.vertices.length(shape.vertices.length()+3*indexArray.getCount());
                        size_t startoffset = ivertex/3;
                        while(k < (int)indexArray.getCount() ) {
                            int index0 = indexArray.get(k)*vertexStride;
                            domFloat fl0 = listFloats.get(index0);
                            domFloat fl1 = listFloats.get(index0+1);
                            domFloat fl2 = listFloats.get(index0+2);
                            k+=triangleIndexStride;
                            shape.vertices[ivertex++] = fl0*fUnitScale;
                            shape.vertices[ivertex++] = fl1*fUnitScale;
                            shape.vertices[ivertex++] = fl2*fUnitScale;
                        }
                        for(size_t ivert = 2; ivert < indexArray.getCount(); ++ivert) {
                            shape.triangles[itriangle++] = startoffset;
                            shape.triangles[itriangle++] = startoffset+ivert-1;
                            shape.triangles[itriangle++] = startoffset+ivert;
                        }
                    }
                    else {
                        COLLADALOG_WARN("float array not defined!");
                    }
                    break;
                }
            }
        }
        return true;
    }

    /// Extract the Geometry in TRIANGLE STRIPS and adds it to OpenRave
    /// \param  triRef  Array of Triangle Strips of the COLLADA's model
    /// \param  vertsRef    Array of vertices of the COLLADA's model
    /// \param  mapmaterials    Materials applied to the geometry
    /// \param  plink   Link of the kinematics model
    bool _ExtractGeometry(BodyInfoCollada_impl* pkinbody, boost::shared_ptr<LinkInfo> plink, const domTristripsRef triRef, const domVerticesRef vertsRef, const map<string,int>& mapmaterials)
    {
        if( !triRef ) {
            return false;
        }
        int shapeIndex = pkinbody->shapes_.length();
        pkinbody->shapes_.length(shapeIndex+1);
        ShapeInfo& shape = pkinbody->shapes_[shapeIndex];
        shape.primitiveType = SP_MESH;
        int lsindex = plink->shapeIndices.length();
        plink->shapeIndices.length(lsindex+1);
        plink->shapeIndices[lsindex].shapeIndex = shapeIndex;

        // resolve the material and assign correct colors to the geometry
        shape.appearanceIndex = -1;
        if( !!triRef->getMaterial() ) {
            map<string,int>::const_iterator itmat = mapmaterials.find(triRef->getMaterial());
            if( itmat != mapmaterials.end() ) {
                shape.appearanceIndex = itmat->second;
            }
        }

        size_t triangleIndexStride = 0, vertexoffset = -1;
        domInput_local_offsetRef indexOffsetRef;

        for (unsigned int w=0;w<triRef->getInput_array().getCount();w++) {
            size_t offset = triRef->getInput_array()[w]->getOffset();
            daeString str = triRef->getInput_array()[w]->getSemantic();
            if (!strcmp(str,"VERTEX")) {
                indexOffsetRef = triRef->getInput_array()[w];
                vertexoffset = offset;
            }
            if (offset> triangleIndexStride) {
                triangleIndexStride = offset;
            }
        }
        triangleIndexStride++;
        size_t primitivecount = triRef->getCount();
        if( primitivecount > triRef->getP_array().getCount() ) {
            COLLADALOG_WARN("tristrips has incorrect count");
            primitivecount = triRef->getP_array().getCount();
        }
        int itriangle = 0, ivertex = 0;
        for(size_t ip = 0; ip < primitivecount; ++ip) {
            domList_of_uints indexArray = triRef->getP_array()[ip]->getValue();
            for (size_t i=0;i<vertsRef->getInput_array().getCount();++i) {
                domInput_localRef localRef = vertsRef->getInput_array()[i];
                daeString str = localRef->getSemantic();
                if ( strcmp(str,"POSITION") == 0 ) {
                    const domSourceRef node = daeSafeCast<domSource>(localRef->getSource().getElement());
                    if( !node ) {
                        continue;
                    }
                    dReal fUnitScale = _GetUnitScale(node,_fGlobalScale);
                    const domFloat_arrayRef flArray = node->getFloat_array();
                    if (!!flArray) {
                        const domList_of_floats& listFloats = flArray->getValue();
                        int k=vertexoffset;
                        int vertexStride = 3;//instead of hardcoded stride, should use the 'accessor'
                        size_t usedindices = 3*(indexArray.getCount()-2);
                        shape.triangles.length(shape.triangles.length()+usedindices);
                        shape.vertices.length(shape.vertices.length()+3*indexArray.getCount());
                        size_t startoffset = ivertex/3;
                        while(k < (int)indexArray.getCount() ) {
                            int index0 = indexArray.get(k)*vertexStride;
                            domFloat fl0 = listFloats.get(index0);
                            domFloat fl1 = listFloats.get(index0+1);
                            domFloat fl2 = listFloats.get(index0+2);
                            k+=triangleIndexStride;
                            shape.vertices[ivertex++] = fl0*fUnitScale;
                            shape.vertices[ivertex++] = fl1*fUnitScale;
                            shape.vertices[ivertex++] = fl2*fUnitScale;
                        }

                        bool bFlip = false;
                        for(size_t ivert = 2; ivert < indexArray.getCount(); ++ivert) {
                            shape.triangles[itriangle++] = startoffset-2;
                            shape.triangles[itriangle++] = bFlip ? startoffset+ivert : startoffset+ivert-1;
                            shape.triangles[itriangle++] = bFlip ? startoffset+ivert-1 : startoffset+ivert;
                            bFlip = !bFlip;
                        }
                    }
                    else {
                        COLLADALOG_WARN("float array not defined!");
                    }
                    break;
                }
            }
        }
        return true;
    }

    /// Extract the Geometry in TRIANGLE STRIPS and adds it to OpenRave
    /// \param  triRef  Array of Triangle Strips of the COLLADA's model
    /// \param  vertsRef    Array of vertices of the COLLADA's model
    /// \param  mapmaterials    Materials applied to the geometry
    /// \param  plink   Link of the kinematics model
    bool _ExtractGeometry(BodyInfoCollada_impl* pkinbody, boost::shared_ptr<LinkInfo> plink, const domPolylistRef triRef, const domVerticesRef vertsRef, const map<string,int>& mapmaterials)
    {
        if( !triRef ) {
            return false;
        }
        int shapeIndex = pkinbody->shapes_.length();
        pkinbody->shapes_.length(shapeIndex+1);
        ShapeInfo& shape = pkinbody->shapes_[shapeIndex];
        shape.primitiveType = SP_MESH;
        int lsindex = plink->shapeIndices.length();
        plink->shapeIndices.length(lsindex+1);
        plink->shapeIndices[lsindex].shapeIndex = shapeIndex;

        // resolve the material and assign correct colors to the geometry
        shape.appearanceIndex = -1;
        if( !!triRef->getMaterial() ) {
            map<string,int>::const_iterator itmat = mapmaterials.find(triRef->getMaterial());
            if( itmat != mapmaterials.end() ) {
                shape.appearanceIndex = itmat->second;
            }
        }

        size_t triangleIndexStride = 0,vertexoffset = -1;
        domInput_local_offsetRef indexOffsetRef;
        for (unsigned int w=0;w<triRef->getInput_array().getCount();w++) {
            size_t offset = triRef->getInput_array()[w]->getOffset();
            daeString str = triRef->getInput_array()[w]->getSemantic();
            if (!strcmp(str,"VERTEX")) {
                indexOffsetRef = triRef->getInput_array()[w];
                vertexoffset = offset;
            }
            if (offset> triangleIndexStride) {
                triangleIndexStride = offset;
            }
        }
        triangleIndexStride++;
        const domList_of_uints& indexArray =triRef->getP()->getValue();
        int ivertex = 0, itriangle=0;
        for (size_t i=0;i<vertsRef->getInput_array().getCount();++i) {
            domInput_localRef localRef = vertsRef->getInput_array()[i];
            daeString str = localRef->getSemantic();
            if ( strcmp(str,"POSITION") == 0 ) {
                const domSourceRef node = daeSafeCast<domSource>(localRef->getSource().getElement());
                if( !node ) {
                    continue;
                }
                dReal fUnitScale = _GetUnitScale(node,_fGlobalScale);
                const domFloat_arrayRef flArray = node->getFloat_array();
                if (!!flArray) {
                    const domList_of_floats& listFloats = flArray->getValue();
                    size_t k=vertexoffset;
                    int vertexStride = 3;//instead of hardcoded stride, should use the 'accessor'
                    for(size_t ipoly = 0; ipoly < triRef->getVcount()->getValue().getCount(); ++ipoly) {
                        size_t numverts = triRef->getVcount()->getValue()[ipoly];
                        if(numverts > 0 && k+(numverts-1)*triangleIndexStride < indexArray.getCount() ) {
                            size_t startoffset = ivertex/3;
                            shape.vertices.length(shape.vertices.length()+3*numverts);
                            shape.triangles.length(shape.triangles.length()+3*(numverts-2));
                            for (size_t j=0;j<numverts;j++) {
                                int index0 = indexArray.get(k)*vertexStride;
                                domFloat fl0 = listFloats.get(index0);
                                domFloat fl1 = listFloats.get(index0+1);
                                domFloat fl2 = listFloats.get(index0+2);
                                k+=triangleIndexStride;
                                shape.vertices[ivertex++] = fl0*fUnitScale;
                                shape.vertices[ivertex++] = fl1*fUnitScale;
                                shape.vertices[ivertex++] = fl2*fUnitScale;
                            }
                            for(size_t ivert = 2; ivert < numverts; ++ivert) {
                                shape.triangles[itriangle++] = startoffset;
                                shape.triangles[itriangle++] = startoffset+ivert-1;
                                shape.triangles[itriangle++] = startoffset+ivert;
                            }   
                        }
                    }
                }
                else {
                    COLLADALOG_WARN("float array not defined!");
                }
                break;
            }
        }
        return true;
    }

    /// Extract the Geometry and adds it to OpenRave
    /// \param  geom    Geometry to extract of the COLLADA's model
    /// \param  mapmaterials    Materials applied to the geometry
    /// \param  plink   Link of the kinematics model
    bool ExtractGeometry(BodyInfoCollada_impl* pkinbody, boost::shared_ptr<LinkInfo> plink, const domGeometryRef geom, const map<string,int>& mapmaterials)
    {
        if( !geom ) {
            return false;
        }
        std::vector<Vector3> vconvexhull;
        if (geom->getMesh()) {
            const domMeshRef meshRef = geom->getMesh();
            for (size_t tg = 0;tg<meshRef->getTriangles_array().getCount();tg++) {
                _ExtractGeometry(pkinbody, plink, meshRef->getTriangles_array()[tg], meshRef->getVertices(), mapmaterials);
            }
            for (size_t tg = 0;tg<meshRef->getTrifans_array().getCount();tg++) {
                _ExtractGeometry(pkinbody, plink, meshRef->getTrifans_array()[tg], meshRef->getVertices(), mapmaterials);
            }
            for (size_t tg = 0;tg<meshRef->getTristrips_array().getCount();tg++) {
                _ExtractGeometry(pkinbody, plink, meshRef->getTristrips_array()[tg], meshRef->getVertices(), mapmaterials);
            }
            for (size_t tg = 0;tg<meshRef->getPolylist_array().getCount();tg++) {
                _ExtractGeometry(pkinbody, plink, meshRef->getPolylist_array()[tg], meshRef->getVertices(), mapmaterials);
            }
            if( meshRef->getPolygons_array().getCount()> 0 ) {
                COLLADALOG_WARN("openrave does not support collada polygons");
            }

            //            if( alltrimesh.vertices.size() == 0 ) {
            //                const domVerticesRef vertsRef = meshRef->getVertices();
            //                for (size_t i=0;i<vertsRef->getInput_array().getCount();i++) {
            //                    domInput_localRef localRef = vertsRef->getInput_array()[i];
            //                    daeString str = localRef->getSemantic();
            //                    if ( strcmp(str,"POSITION") == 0 ) {
            //                        const domSourceRef node = daeSafeCast<domSource>(localRef->getSource().getElement());
            //                        if( !node )
            //                            continue;
            //                        dReal fUnitScale = _GetUnitScale(node,_fGlobalScale);
            //                        const domFloat_arrayRef flArray = node->getFloat_array();
            //                        if (!!flArray) {
            //                            const domList_of_floats& listFloats = flArray->getValue();
            //                            int vertexStride = 3;//instead of hardcoded stride, should use the 'accessor'
            //                            vconvexhull.reserve(vconvexhull.size()+listFloats.getCount());
            //                            for (size_t vertIndex = 0;vertIndex < listFloats.getCount();vertIndex+=vertexStride) {
            //                                //btVector3 verts[3];
            //                                domFloat fl0 = listFloats.get(vertIndex);
            //                                domFloat fl1 = listFloats.get(vertIndex+1);
            //                                domFloat fl2 = listFloats.get(vertIndex+2);
            //                                vconvexhull.push_back(Vector(fl0*fUnitScale,fl1*fUnitScale,fl2*fUnitScale));
            //                            }
            //                        }
            //                    }
            //                }
            //
            //                _computeConvexHull(vconvexhull,alltrimesh);
            //            }

            return true;
        }
        else if (geom->getConvex_mesh()) {
            {
                const domConvex_meshRef convexRef = geom->getConvex_mesh();
                daeElementRef otherElemRef = convexRef->getConvex_hull_of().getElement();
                if ( !!otherElemRef ) {
                    domGeometryRef linkedGeom = *(domGeometryRef*)&otherElemRef;
                    COLLADALOG_WARN( "otherLinked");
                }
                else {
                    COLLADALOG_WARN(str(boost::format("convexMesh polyCount = %d")%(int)convexRef->getPolygons_array().getCount()));
                    COLLADALOG_WARN(str(boost::format("convexMesh triCount = %d")%(int)convexRef->getTriangles_array().getCount()));
                }
            }

            const domConvex_meshRef convexRef = geom->getConvex_mesh();
            //daeString urlref = convexRef->getConvex_hull_of().getURI();
            daeString urlref2 = convexRef->getConvex_hull_of().getOriginalURI();
            if (urlref2) {
                daeElementRef otherElemRef = convexRef->getConvex_hull_of().getElement();

                // Load all the geometry libraries
                for ( size_t i = 0; i < _dom->getLibrary_geometries_array().getCount(); i++) {
                    domLibrary_geometriesRef libgeom = _dom->getLibrary_geometries_array()[i];
                    for (size_t i = 0; i < libgeom->getGeometry_array().getCount(); i++) {
                        domGeometryRef lib = libgeom->getGeometry_array()[i];
                        if (!strcmp(lib->getId(),urlref2+1)) { // skip the # at the front of urlref2
                            //found convex_hull geometry
                            domMesh *meshElement = lib->getMesh();//linkedGeom->getMesh();
                            if (meshElement) {
                                const domVerticesRef vertsRef = meshElement->getVertices();
                                for (size_t i=0;i<vertsRef->getInput_array().getCount();i++) {
                                    domInput_localRef localRef = vertsRef->getInput_array()[i];
                                    daeString str = localRef->getSemantic();
                                    if ( strcmp(str,"POSITION") == 0) {
                                        const domSourceRef node = daeSafeCast<domSource>(localRef->getSource().getElement());
                                        if( !node ) {
                                            continue;
                                        }
                                        dReal fUnitScale = _GetUnitScale(node,_fGlobalScale);
                                        const domFloat_arrayRef flArray = node->getFloat_array();
                                        if (!!flArray) {
                                            vconvexhull.reserve(vconvexhull.size()+flArray->getCount());
                                            const domList_of_floats& listFloats = flArray->getValue();
                                            for (size_t k=0;k+2<flArray->getCount();k+=3) {
                                                domFloat fl0 = listFloats.get(k);
                                                domFloat fl1 = listFloats.get(k+1);
                                                domFloat fl2 = listFloats.get(k+2);
                                                vconvexhull.push_back(Vector3(fl0*fUnitScale,fl1*fUnitScale,fl2*fUnitScale));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else {
                //no getConvex_hull_of but direct vertices
                const domVerticesRef vertsRef = convexRef->getVertices();
                for (size_t i=0;i<vertsRef->getInput_array().getCount();i++) {
                    domInput_localRef localRef = vertsRef->getInput_array()[i];
                    daeString str = localRef->getSemantic();
                    if ( strcmp(str,"POSITION") == 0 ) {
                        const domSourceRef node = daeSafeCast<domSource>(localRef->getSource().getElement());
                        if( !node ) {
                            continue;
                        }
                        dReal fUnitScale = _GetUnitScale(node,_fGlobalScale);
                        const domFloat_arrayRef flArray = node->getFloat_array();
                        if (!!flArray) {
                            const domList_of_floats& listFloats = flArray->getValue();
                            vconvexhull.reserve(vconvexhull.size()+flArray->getCount());
                            for (size_t k=0;k+2<flArray->getCount();k+=3) {
                                domFloat fl0 = listFloats.get(k);
                                domFloat fl1 = listFloats.get(k+1);
                                domFloat fl2 = listFloats.get(k+2);
                                vconvexhull.push_back(Vector3(fl0*fUnitScale,fl1*fUnitScale,fl2*fUnitScale));
                            }
                        }
                    }
                }
            }

            if( vconvexhull.size()> 0 ) {
                COLLADALOG_ERROR("convex hulls not supported");
                //plink->_listGeomProperties.push_back(KinBody::Link::GEOMPROPERTIES(plink));
                //KinBody::Link::GEOMPROPERTIES& geom = plink->_listGeomProperties.back();
                //KinBody::Link::TRIMESH& trimesh = geom.collisionmesh;
                //geom._type = KinBody::Link::GEOMPROPERTIES::GeomTrimesh;
                //geom.InitCollisionMesh();
            }
            return true;
        }

        return false;
    }

    /// Paint the Geometry with the color material
    /// \param  pmat    Material info of the COLLADA's model
    /// \param  geom    Geometry properties in OpenRAVE
    void _FillMaterial(MaterialInfo& mat, const domMaterialRef pdommat)
    {
        mat.ambientIntensity = 0.1;
        mat.diffuseColor[0] = 0.8; mat.diffuseColor[1] = 0.8; mat.diffuseColor[2] = 0.8;
        mat.emissiveColor[0] = 0; mat.emissiveColor[1] = 0; mat.emissiveColor[2] = 0;
        mat.shininess = 0;
        mat.specularColor[0] = 0; mat.specularColor[1] = 0; mat.specularColor[2] = 0;
        mat.transparency = 0; // 0 is opaque
        if( !!pdommat && !!pdommat->getInstance_effect() ) {
            domEffectRef peffect = daeSafeCast<domEffect>(pdommat->getInstance_effect()->getUrl().getElement().cast());
            if( !!peffect ) {
                domProfile_common::domTechnique::domPhongRef pphong = daeSafeCast<domProfile_common::domTechnique::domPhong>(peffect->getDescendant(daeElement::matchType(domProfile_common::domTechnique::domPhong::ID())));
                if( !!pphong ) {
                    if( !!pphong->getAmbient() && !!pphong->getAmbient()->getColor() ) {
                        domFx_color c = pphong->getAmbient()->getColor()->getValue();
                        mat.ambientIntensity = (fabs(c[0])+fabs(c[1])+fabs(c[2]))/3;
                    }
                    if( !!pphong->getDiffuse() && !!pphong->getDiffuse()->getColor() ) {
                        domFx_color c = pphong->getDiffuse()->getColor()->getValue();
                        mat.diffuseColor[0] = c[0];
                        mat.diffuseColor[1] = c[1];
                        mat.diffuseColor[2] = c[2];
                    }
                }
                domProfile_common::domTechnique::domLambertRef plambert = daeSafeCast<domProfile_common::domTechnique::domLambert>(peffect->getDescendant(daeElement::matchType(domProfile_common::domTechnique::domLambert::ID())));
                if( !!plambert ) {
                    if( !!plambert->getAmbient() && !!plambert->getAmbient()->getColor() ) {
                        domFx_color c = plambert->getAmbient()->getColor()->getValue();
                        mat.ambientIntensity = (fabs(c[0])+fabs(c[1])+fabs(c[2]))/3;
                    }
                    if( !!plambert->getDiffuse() && !!plambert->getDiffuse()->getColor() ) {
                        domFx_color c = plambert->getDiffuse()->getColor()->getValue();
                        mat.diffuseColor[0] = c[0];
                        mat.diffuseColor[1] = c[1];
                        mat.diffuseColor[2] = c[2];
                    }
                }
            }
        }
    }

    /// \brief extract the robot manipulators
    void ExtractRobotManipulators(BodyInfoCollada_impl* probot, const domArticulated_systemRef as)
    {
        for(size_t ie = 0; ie < as->getExtra_array().getCount(); ++ie) {
            domExtraRef pextra = as->getExtra_array()[ie];
            if( strcmp(pextra->getType(), "manipulator") == 0 ) {
                string name = pextra->getAttribute("name");
                if( name.size() == 0 ) {
                    name = str(boost::format("manipulator%d")%_nGlobalManipulatorId++);
                }
                domTechniqueRef tec = _ExtractOpenRAVEProfile(pextra->getTechnique_array());
                if( !!tec ) {
                    
                }
                else {
                    COLLADALOG_WARN(str(boost::format("cannot create robot manipulator %s")%name));
                }
            }
        }
    }

    /// \brief Extract Sensors attached to a Robot
    void ExtractRobotAttachedSensors(BodyInfoCollada_impl* probot, const domArticulated_systemRef as)
    {
        for (size_t ie = 0; ie < as->getExtra_array().getCount(); ie++) {
            domExtraRef pextra = as->getExtra_array()[ie];
            if( strcmp(pextra->getType(), "attach_sensor") == 0 ) {
                string name = pextra->getAttribute("name");
                if( name.size() == 0 ) {
                    name = str(boost::format("sensor%d")%_nGlobalSensorId++);
                }
                domTechniqueRef tec = _ExtractOpenRAVEProfile(pextra->getTechnique_array());
                if( !!tec ) {
                    daeElement* pframe_origin = tec->getChild("frame_origin");
                    if( !!pframe_origin ) {
                        domLinkRef pdomlink = daeSafeCast<domLink>(daeSidRef(pframe_origin->getAttribute("link"), as).resolve().elt);
			SensorInfo_var psensor(new SensorInfo());
			psensor->name = CORBA::string_dup( name.c_str() );
			boost::shared_ptr<LinkInfo> plink;
			if( _mapLinkNames.find(_ExtractLinkName(pdomlink)) != _mapLinkNames.end() ) {
			    plink = _mapLinkNames[_ExtractLinkName(pdomlink)];
			} else {
			    COLLADALOG_WARN(str(boost::format("unknown joint %s")%_ExtractLinkName(pdomlink)));
			}

			if( plink && _ExtractSensor(psensor,tec->getChild("instance_sensor")) ) {

			    DblArray12 ttemp;
			    _ExtractFullTransformFromChildren(ttemp, pframe_origin);
			    //std::cerr << psensor->type << std::endl;
			    if ( string(psensor->type) == "Vision" ) {
				DblArray4 quat;
				DblArray12 rotation, ttemp2;
				QuatFromAxisAngle(quat, Vector4(1,0,0,M_PI));
				PoseFromQuat(rotation,quat);
				PoseMult(ttemp2,ttemp,rotation);
				AxisAngleTranslationFromPose(psensor->rotation,psensor->translation,ttemp2);
			    } else {
				AxisAngleTranslationFromPose(psensor->rotation,psensor->translation,ttemp);
			    }

			    int numSensors = plink->sensors.length();
			    plink->sensors.length(numSensors + 1);
			    plink->sensors[numSensors] = psensor;
			} else {
			    COLLADALOG_WARN(str(boost::format("cannot find instance_sensor for attached sensor %s:%s")%probot->name_%name));
			}
		    }
                }
                else {
                    COLLADALOG_WARN(str(boost::format("cannot create robot attached sensor %s")%name));
                }
            }
        }
    }

    /// \brief Extract Sensors attached to a Robot
    void ExtractRobotAttachedActuators(BodyInfoCollada_impl* probot, const domArticulated_systemRef as)
    {
        for (size_t ie = 0; ie < as->getExtra_array().getCount(); ie++) {
            domExtraRef pextra = as->getExtra_array()[ie];
            if( strcmp(pextra->getType(), "attach_actuator") == 0 ) {
                string name = pextra->getAttribute("name");
                if( name.size() == 0 ) {
                    name = str(boost::format("actuator%d")%_nGlobalActuatorId++);
                }
                domTechniqueRef tec = _ExtractOpenRAVEProfile(pextra->getTechnique_array());
                if( !!tec ) {
		    if ( GetLink(name) && _ExtractActuator(GetLink(name), tec->getChild("instance_actuator"))  ) {
		    } else {
                        COLLADALOG_WARN(str(boost::format("cannot find instance_actuator for attached sensor %s:%s")%probot->name_%name));
		    }
                }
                else {
                    COLLADALOG_WARN(str(boost::format("cannot create robot attached actuators %s")%name));
                }
	    }
	}
    }

    /// \brief Extract an instance of a sensor
    bool _ExtractSensor(SensorInfo &psensor, daeElementRef instance_sensor)
    {
        if( !instance_sensor ) {
            return false;
        }
        if( !instance_sensor->hasAttribute("url") ) {
            COLLADALOG_WARN("instance_sensor has no url");
            return false;
        }

        std::string instance_id = instance_sensor->getAttribute("id");
        std::string instance_url = instance_sensor->getAttribute("url");
        daeElementRef domsensor = _getElementFromUrl(daeURI(*instance_sensor,instance_url));
        if( !domsensor ) {
            COLLADALOG_WARN(str(boost::format("failed to find senor id %s url=%s")%instance_id%instance_url));
            return false;
        }
        if( !domsensor->hasAttribute("type") ) {
            COLLADALOG_WARN("collada <sensor> needs type attribute");
            return false;
        }
	psensor.id = boost::lexical_cast<int>(domsensor->getAttribute("sid"));
        std::string sensortype = domsensor->getAttribute("type");
	if ( sensortype == "base_imu" ) {// AccelerationSensor  // GyroSeesor
            psensor.specValues.length( CORBA::ULong(3) );
	    daeElement *max_angular_velocity = domsensor->getChild("max_angular_velocity");
	    daeElement *max_acceleration = domsensor->getChild("max_acceleration");
	    if ( !! max_angular_velocity ) {
		istringstream ins(max_angular_velocity->getCharData());
		float f0,f1,f2,f3,f4,f5;
		ins >> psensor.specValues[0] >> psensor.specValues[1] >> psensor.specValues[2];
		psensor.type = CORBA::string_dup( "RateGyro" );
	    } else if ( !! max_acceleration ) {
		istringstream ins(max_acceleration->getCharData());
		ins >> psensor.specValues[0] >> psensor.specValues[1] >> psensor.specValues[2];
		psensor.type = CORBA::string_dup( "Acceleration" );
	    } else {
                COLLADALOG_WARN(str(boost::format("couldn't find max_angular_velocity nor max_acceleration%s")%sensortype));
	    }
	    return true;
	}
	if ( sensortype == "base_pinhole_camera" ) { // VisionSensor
            psensor.type = CORBA::string_dup( "Vision" );
	    psensor.specValues.length( CORBA::ULong(7) );
	    //psensor.specValues[0] = frontClipDistance
	    //psensor.specValues[1] = backClipDistance
	    psensor.specValues[1] = 10;
	    //psensor.specValues[2] = fieldOfView
	    //psensor.specValues[3] = OpenHRP::Camera::COLOR; // type
	    //psensor.specValues[4] = width
	    //psensor.specValues[5] = height
	    //psensor.specValues[6] = frameRate
	    daeElement *measurement_time = domsensor->getChild("measurement_time");
	    if ( !! measurement_time ) {
		psensor.specValues[6] = 1.0/(boost::lexical_cast<double>(measurement_time->getCharData()));
	    } else {
                COLLADALOG_WARN(str(boost::format("couldn't find measurement_time %s")%sensortype));
	    }
	    daeElement *focal_length = domsensor->getChild("focal_length");
	    if ( !! focal_length ) {
		psensor.specValues[0] = boost::lexical_cast<double>(focal_length->getCharData());
	    } else {
                COLLADALOG_WARN(str(boost::format("couldn't find focal_length %s")%sensortype));
		psensor.specValues[0] = 0.1;
	    }
	    daeElement *image_format = domsensor->getChild("format");
	    std::string string_format = "uint8";
	    if ( !! image_format ) {
		string_format = image_format->getCharData();
	    }
	    daeElement *intrinsic = domsensor->getChild("intrinsic");
	    if ( !! intrinsic ) {
		istringstream ins(intrinsic->getCharData());
		float f0,f1,f2,f3,f4,f5;
		ins >> f0 >> f1 >> f2 >> f3 >> f4 >> f5;
		psensor.specValues[2] = atan( f2 / f0) * 2.0;
	    } else {
                COLLADALOG_WARN(str(boost::format("couldn't find intrinsic %s")%sensortype));
		psensor.specValues[2] = 0.785398;
	    }
	    daeElement *image_dimensions = domsensor->getChild("image_dimensions");
	    if ( !! image_dimensions ) {
		istringstream ins(image_dimensions->getCharData());
		int ichannel;
		ins >> psensor.specValues[4] >> psensor.specValues[5] >> ichannel;
		switch (ichannel) {
		    case 1:
			if ( string_format == "uint8") {
			    psensor.specValues[3] = OpenHRP::Camera::MONO;
			} else if ( string_format == "float32") {
			    psensor.specValues[3] = OpenHRP::Camera::DEPTH;
			} else {
			    COLLADALOG_WARN(str(boost::format("unknown image format %s %d")%string_format%ichannel));
			}
			break;
		    case 2:
			if ( string_format == "float32") {
			    psensor.specValues[3] = OpenHRP::Camera::MONO_DEPTH;
			} else {
			    COLLADALOG_WARN(str(boost::format("unknown image format %s %d")%string_format%ichannel));
			}
			break;
		    case 3:
			if ( string_format == "uint8") {
			    psensor.specValues[3] = OpenHRP::Camera::COLOR;
			} else {
			    COLLADALOG_WARN(str(boost::format("unknown image format %s %d")%string_format%ichannel));
			}
			break;
		    case 4:
			if ( string_format == "float32") {
			    psensor.specValues[3] = OpenHRP::Camera::COLOR_DEPTH;
			} else {
			    COLLADALOG_WARN(str(boost::format("unknown image format %s %d")%string_format%ichannel));
			}
			break;
		default:
		    COLLADALOG_WARN(str(boost::format("unknown image format %s %d")%string_format%ichannel));
		}

	    } else {
                COLLADALOG_WARN(str(boost::format("couldn't find image_dimensions %s")%sensortype));
		psensor.specValues[4] = 320;
		psensor.specValues[5] = 240;
	    }
	    return true;
	}
	if ( sensortype == "base_force6d" ) { // ForceSensor
            psensor.type = CORBA::string_dup( "Force" );
	    psensor.specValues.length( CORBA::ULong(6) );
	    daeElement *max_force = domsensor->getChild("load_range_force");
	    if ( !! max_force ) {
		istringstream ins(max_force->getCharData());
		ins >> psensor.specValues[0] >> psensor.specValues[1] >> psensor.specValues[2];
	    } else {
                COLLADALOG_WARN(str(boost::format("couldn't find load_range_force %s")%sensortype));
	    }
	    daeElement *max_torque = domsensor->getChild("load_range_torque");
	    if ( !! max_torque ) {
		istringstream ins(max_torque->getCharData());
		ins >> psensor.specValues[3] >> psensor.specValues[4] >> psensor.specValues[5];
	    } else {
                COLLADALOG_WARN(str(boost::format("couldn't findload_range_torque %s")%sensortype));
	    }
	    return true;
	}
	if ( sensortype == "base_laser2d" ) { // RangeSensor
            psensor.type = CORBA::string_dup( "Range" );
	    psensor.specValues.length( CORBA::ULong(4) );
	    daeElement *scan_angle = domsensor->getChild("angle_range");
	    if ( !! scan_angle ) {
		psensor.specValues[0] = boost::lexical_cast<double>(scan_angle->getCharData());
	    } else {
                COLLADALOG_WARN(str(boost::format("couldn't find angle_range %s")%sensortype));
	    }
	    daeElement *scan_step = domsensor->getChild("angle_increment");
	    if ( !! scan_step ) {
		psensor.specValues[1] = boost::lexical_cast<double>(scan_step->getCharData());
	    } else {
                COLLADALOG_WARN(str(boost::format("couldn't find angle_incremnet %s")%sensortype));
	    }
	    daeElement *scan_rate = domsensor->getChild("measurement_time");
	    if ( !! scan_rate ) {
		psensor.specValues[2] = 1.0/boost::lexical_cast<double>(scan_rate->getCharData());
	    } else {
                COLLADALOG_WARN(str(boost::format("couldn't find measurement_time %s")%sensortype));
	    }
	    daeElement *max_distance = domsensor->getChild("distance_range");
	    if ( !! max_distance ) {
		psensor.specValues[3] = boost::lexical_cast<double>(max_distance->getCharData());
	    } else {
                COLLADALOG_WARN(str(boost::format("couldn't find distance_range %s")%sensortype));
	    }
	    return true;
	}
        COLLADALOG_WARN(str(boost::format("need to create sensor type: %s")%sensortype));
	return false;
    }

    /// \brief Extract an instance of a sensor
    bool _ExtractActuator(boost::shared_ptr<LinkInfo> plink, daeElementRef instance_actuator)
    {
        if( !instance_actuator ) {
            return false;
        }
        if( !instance_actuator->hasAttribute("url") ) {
            COLLADALOG_WARN("instance_actuator has no url");
            return false;
        }

        std::string instance_id = instance_actuator->getAttribute("id");
        std::string instance_url = instance_actuator->getAttribute("url");
        daeElementRef domactuator = _getElementFromUrl(daeURI(*instance_actuator,instance_url));
        if( !domactuator ) {
            COLLADALOG_WARN(str(boost::format("failed to find actuator id %s url=%s")%instance_id%instance_url));
            return false;
        }
        if( !domactuator->hasAttribute("type") ) {
            COLLADALOG_WARN("collada <actuator> needs type attribute");
            return false;
        }
        std::string actuatortype = domactuator->getAttribute("type");
        daeElement *rotor_inertia = domactuator->getChild("rotor_inertia");
	if ( !! rotor_inertia ) {
	    plink->rotorInertia =  boost::lexical_cast<double>(rotor_inertia->getCharData());
	}
        daeElement *rotor_resistor = domactuator->getChild("rotor_resistor");
	if ( !! rotor_resistor ) {
	    plink->rotorResistor = boost::lexical_cast<double>(rotor_resistor->getCharData());
	}
        daeElement *gear_ratio = domactuator->getChild("gear_ratio");
	if ( !! gear_ratio ) {
	    plink->gearRatio = boost::lexical_cast<double>(gear_ratio->getCharData());
	}
        daeElement *torque_const = domactuator->getChild("torque_constant");
	if ( !! torque_const ) {
	    plink->torqueConst = boost::lexical_cast<double>(torque_const->getCharData());
	}
        daeElement *encoder_pulse = domactuator->getChild("encoder_pulse");
	if ( !! encoder_pulse ) {
	    plink->encoderPulse = boost::lexical_cast<double>(encoder_pulse->getCharData());
	}
        return true;
    }

    inline daeElementRef _getElementFromUrl(const daeURI &uri)
    {
        return daeStandardURIResolver(*_dae).resolveElement(uri);
    }

    static daeElement* searchBinding(domCommon_sidref_or_paramRef paddr, daeElementRef parent)
    {
        if( !!paddr->getSIDREF() ) {
            return daeSidRef(paddr->getSIDREF()->getValue(),parent).resolve().elt;
        }
        if (!!paddr->getParam()) {
            return searchBinding(paddr->getParam()->getValue(),parent);
        }
        return NULL;
    }

    /// Search a given parameter reference and stores the new reference to search.
    /// \param ref the reference name to search
    /// \param parent The array of parameter where the method searchs.
    static daeElement* searchBinding(daeString ref, daeElementRef parent)
    {
        if( !parent ) {
            return NULL;
        }
        daeElement* pelt = NULL;
        domKinematics_sceneRef kscene = daeSafeCast<domKinematics_scene>(parent.cast());
        if( !!kscene ) {
            pelt = searchBindingArray(ref,kscene->getInstance_articulated_system_array());
            if( !!pelt ) {
                return pelt;
            }
            return searchBindingArray(ref,kscene->getInstance_kinematics_model_array());
        }
        domArticulated_systemRef articulated_system = daeSafeCast<domArticulated_system>(parent.cast());
        if( !!articulated_system ) {
            if( !!articulated_system->getKinematics() ) {
                pelt = searchBindingArray(ref,articulated_system->getKinematics()->getInstance_kinematics_model_array());
                if( !!pelt ) {
                    return pelt;
                }
            }
            if( !!articulated_system->getMotion() ) {
                return searchBinding(ref,articulated_system->getMotion()->getInstance_articulated_system());
            }
            return NULL;
        }
        // try to get a bind array
        daeElementRef pbindelt;
        const domKinematics_bind_Array* pbindarray = NULL;
        const domKinematics_newparam_Array* pnewparamarray = NULL;
        domInstance_articulated_systemRef ias = daeSafeCast<domInstance_articulated_system>(parent.cast());
        if( !!ias ) {
            pbindarray = &ias->getBind_array();
            pbindelt = ias->getUrl().getElement();
            pnewparamarray = &ias->getNewparam_array();
        }
        if( !pbindarray || !pbindelt ) {
            domInstance_kinematics_modelRef ikm = daeSafeCast<domInstance_kinematics_model>(parent.cast());
            if( !!ikm ) {
                pbindarray = &ikm->getBind_array();
                pbindelt = ikm->getUrl().getElement();
                pnewparamarray = &ikm->getNewparam_array();
            }
        }
        if( !!pbindarray && !!pbindelt ) {
            for (size_t ibind = 0; ibind < pbindarray->getCount(); ++ibind) {
                domKinematics_bindRef pbind = (*pbindarray)[ibind];
                if( !!pbind->getSymbol() && strcmp(pbind->getSymbol(), ref) == 0 ) { 
                    // found a match
                    if( !!pbind->getParam() ) {
                        //return searchBinding(pbind->getParam()->getRef(), pbindelt);
                        return daeSidRef(pbind->getParam()->getRef(), pbindelt).resolve().elt;
                    }
                    else if( !!pbind->getSIDREF() ) {
                        return daeSidRef(pbind->getSIDREF()->getValue(), pbindelt).resolve().elt;
                    }
                }
            }
            for(size_t inewparam = 0; inewparam < pnewparamarray->getCount(); ++inewparam) {
                domKinematics_newparamRef newparam = (*pnewparamarray)[inewparam];
                if( !!newparam->getSid() && strcmp(newparam->getSid(), ref) == 0 ) {
                    if( !!newparam->getSIDREF() ) { // can only bind with SIDREF
                        return daeSidRef(newparam->getSIDREF()->getValue(),pbindelt).resolve().elt;
                    }
                    COLLADALOG_WARN(str(boost::format("newparam sid=%s does not have SIDREF")%newparam->getSid()));
                }
            }
        }
        COLLADALOG_WARN(str(boost::format("failed to get binding '%s' for element: %s")%ref%parent->getElementName()));
        return NULL;
    }

    static daeElement* searchBindingArray(daeString ref, const domInstance_articulated_system_Array& paramArray)
    {
        for(size_t iikm = 0; iikm < paramArray.getCount(); ++iikm) {
            daeElement* pelt = searchBinding(ref,paramArray[iikm].cast());
            if( !!pelt ) {
                return pelt;
            }
        }
        return NULL;
    }

    static daeElement* searchBindingArray(daeString ref, const domInstance_kinematics_model_Array& paramArray)
    {
        for(size_t iikm = 0; iikm < paramArray.getCount(); ++iikm) {
            daeElement* pelt = searchBinding(ref,paramArray[iikm].cast());
            if( !!pelt ) {
                return pelt;
            }
        }
        return NULL;
    }

    template <typename U> static xsBoolean resolveBool(domCommon_bool_or_paramRef paddr, const U& parent) {
        if( !!paddr->getBool() ) {
            return paddr->getBool()->getValue();
        }
        if( !paddr->getParam() ) {
            COLLADALOG_WARN("param not specified, setting to 0");
            return false;
        }
        for(size_t iparam = 0; iparam < parent->getNewparam_array().getCount(); ++iparam) {
            domKinematics_newparamRef pnewparam = parent->getNewparam_array()[iparam];
            if( !!pnewparam->getSid() && strcmp(pnewparam->getSid(), paddr->getParam()->getValue()) == 0 ) {
                if( !!pnewparam->getBool() ) {
                    return pnewparam->getBool()->getValue();
                }
                else if( !!pnewparam->getSIDREF() ) {
                    domKinematics_newparam::domBoolRef ptarget = daeSafeCast<domKinematics_newparam::domBool>(daeSidRef(pnewparam->getSIDREF()->getValue(), pnewparam).resolve().elt);
                    if( !ptarget ) {
                        COLLADALOG_WARN(str(boost::format("failed to resolve %s from %s")%pnewparam->getSIDREF()->getValue()%paddr->getID()));
                        continue;
                    }
                    return ptarget->getValue();
                }
            }
        }
        COLLADALOG_WARN(str(boost::format("failed to resolve %s")%paddr->getParam()->getValue()));
        return false;
    }
    template <typename U> static domFloat resolveFloat(domCommon_float_or_paramRef paddr, const U& parent) {
        if( !!paddr->getFloat() ) {
            return paddr->getFloat()->getValue();
        }
        if( !paddr->getParam() ) {
            COLLADALOG_WARN("param not specified, setting to 0");
            return 0;
        }
        for(size_t iparam = 0; iparam < parent->getNewparam_array().getCount(); ++iparam) {
            domKinematics_newparamRef pnewparam = parent->getNewparam_array()[iparam];
            if( !!pnewparam->getSid() && strcmp(pnewparam->getSid(), paddr->getParam()->getValue()) == 0 ) {
                if( !!pnewparam->getFloat() ) {
                    return pnewparam->getFloat()->getValue();
                }
                else if( !!pnewparam->getSIDREF() ) {
                    domKinematics_newparam::domFloatRef ptarget = daeSafeCast<domKinematics_newparam::domFloat>(daeSidRef(pnewparam->getSIDREF()->getValue(), pnewparam).resolve().elt);
                    if( !ptarget ) {
                        COLLADALOG_WARN(str(boost::format("failed to resolve %s from %s")%pnewparam->getSIDREF()->getValue()%paddr->getID()));
                        continue;
                    }
                    return ptarget->getValue();
                }
            }
        }
        COLLADALOG_WARN(str(boost::format("failed to resolve %s")%paddr->getParam()->getValue()));
        return 0;
    }

    static bool resolveCommon_float_or_param(daeElementRef pcommon, daeElementRef parent, float& f)
    {
        daeElement* pfloat = pcommon->getChild("float");
        if( !!pfloat ) {
            stringstream sfloat(pfloat->getCharData());
            sfloat >> f;
            return !!sfloat;
        }
        daeElement* pparam = pcommon->getChild("param");
        if( !!pparam ) {
            if( pparam->hasAttribute("ref") ) {
                COLLADALOG_WARN("cannot process param ref");
            }
            else {
                daeElement* pelt = daeSidRef(pparam->getCharData(),parent).resolve().elt;
                if( !!pelt ) {
                    COLLADALOG_WARN(str(boost::format("found param ref: %s from %s")%pelt->getCharData()%pparam->getCharData()));
                }
            }
        }
        return false;
    }

    /// Gets all transformations applied to the node
    void getTransform(DblArray12& tres, daeElementRef pelt)
    {
        domRotateRef protate = daeSafeCast<domRotate>(pelt);
        if( !!protate ) {
            DblArray4 quat;
            QuatFromAxisAngle(quat, protate->getValue(),M_PI/180.0);
            PoseFromQuat(tres,quat);
            return;
        }

        dReal fscale = _GetUnitScale(pelt,_fGlobalScale);
        domTranslateRef ptrans = daeSafeCast<domTranslate>(pelt);
        if( !!ptrans ) {
            PoseIdentity(tres);
            tres[3] = ptrans->getValue()[0]*fscale;
            tres[7] = ptrans->getValue()[1]*fscale;
            tres[11] = ptrans->getValue()[2]*fscale;
            return;
        }

        domMatrixRef pmat = daeSafeCast<domMatrix>(pelt);
        if( !!pmat ) {
            for(int i = 0; i < 12; ++i) {
                tres[i] = pmat->getValue()[i];
            }
            tres[3] *= fscale;
            tres[7] *= fscale;
            tres[11] *= fscale;
            return;
        }

        PoseIdentity(tres);
        domScaleRef pscale = daeSafeCast<domScale>(pelt);
        if( !!pscale ) {
            tres[0] = pscale->getValue()[0];
            tres[5] = pscale->getValue()[1];
            tres[10] = pscale->getValue()[2];
            return;
        }

        domLookatRef pcamera = daeSafeCast<domLookat>(pelt);
        if( pelt->typeID() == domLookat::ID() ) {
            COLLADALOG_ERROR("lookat not implemented");
            return;
        }

        domSkewRef pskew = daeSafeCast<domSkew>(pelt);
        if( !!pskew ) {
            COLLADALOG_ERROR("skew transform not implemented");
            return;
        }
    }

    /// Travels recursively the node parents of the given one
    /// to extract the Transform arrays that affects the node given
    template <typename T> void getNodeParentTransform(DblArray12& tres, const T pelt) {
        PoseIdentity(tres);
        domNodeRef pnode = daeSafeCast<domNode>(pelt->getParent());
        if( !!pnode ) {
            DblArray12 ttemp, ttemp2;
            _ExtractFullTransform(ttemp, pnode);
            getNodeParentTransform(tres, pnode);
            PoseMult(ttemp2,tres,ttemp);
            memcpy(&tres[0],&ttemp2[0],sizeof(DblArray12));
        }
    }

    /// \brief Travel by the transformation array and calls the getTransform method
    template <typename T> void _ExtractFullTransform(DblArray12& tres, const T pelt) {
        PoseIdentity(tres);
        DblArray12 ttemp, ttemp2;
        for(size_t i = 0; i < pelt->getContents().getCount(); ++i) {
            getTransform(ttemp, pelt->getContents()[i]);
            PoseMult(ttemp2,tres,ttemp);
            memcpy(&tres[0],&ttemp2[0],sizeof(DblArray12));
        }
    }

    /// \brief Travel by the transformation array and calls the getTransform method
    template <typename T> void _ExtractFullTransformFromChildren(DblArray12& tres, const T pelt) {
        PoseIdentity(tres);
        DblArray12 ttemp, ttemp2;
        daeTArray<daeElementRef> children;
        pelt->getChildren(children);
        for(size_t i = 0; i < children.getCount(); ++i) {
            getTransform(ttemp, children[i]);
            PoseMult(ttemp2,tres,ttemp);
            memcpy(&tres[0],&ttemp2[0],sizeof(DblArray12));
        }
    }

    // decompose a matrix into a scale and rigid transform (necessary for model scales)
//    static void _decompose(const DblArray12& tm, DblArray12& tout, DblArray3& vscale)
//    {
//        DblArray4 quat;
//        QuatFromMatrix(quat,tm);
//        PoseFromQuat(tout,quat);
//        tout[3] = tm[3];
//        tout[7] = tm[7];
//        tout[11] = tm[11];
//        for(int i = 0; i < 3; ++i) {
//            vscale[i] = (fabs(tm[0+i])+fabs(tm[4+i])+fabs(tm[8+i]))/(fabs(tout[0+i])+fabs(tout[4+i])+fabs(tout[8+i]));
//        }
//    }

    virtual void handleError( daeString msg )
    {
        if( _bOpeningZAE && (msg == string("Document is empty\n") || msg == string("Error parsing XML in daeLIBXMLPlugin::read\n")) ) {
            return; // collada-dom prints these messages even if no error
        }
        COLLADALOG_ERROR(str(boost::format("COLLADA error: %s")%msg));
    }

    virtual void handleWarning( daeString msg )
    {
        COLLADALOG_WARN(str(boost::format("COLLADA warning: %s")%msg));
    }

 private:

    /// \brief go through all kinematics binds to get a kinematics/visual pair
    ///
    /// \param kiscene instance of one kinematics scene, binds the kinematic and visual models
    /// \param bindings the extracted bindings
    static void _ExtractKinematicsVisualBindings(domInstance_with_extraRef viscene, domInstance_kinematics_sceneRef kiscene, KinematicsSceneBindings& bindings)
    {
        domKinematics_sceneRef kscene = daeSafeCast<domKinematics_scene> (kiscene->getUrl().getElement().cast());
        if (!kscene) {
            return;
        }
        for (size_t imodel = 0; imodel < kiscene->getBind_kinematics_model_array().getCount(); imodel++) {
            domArticulated_systemRef articulated_system; // if filled, contains robot-specific information, so create a robot
            domBind_kinematics_modelRef kbindmodel = kiscene->getBind_kinematics_model_array()[imodel];
            if (!kbindmodel->getNode()) {
                COLLADALOG_WARN("do not support kinematics models without references to nodes");
                continue;
            }
       
            // visual information
            domNodeRef node = daeSafeCast<domNode>(daeSidRef(kbindmodel->getNode(), viscene->getUrl().getElement()).resolve().elt);
            if (!node) {
                COLLADALOG_WARN(str(boost::format("bind_kinematics_model does not reference valid node %sn")%kbindmodel->getNode()));
                continue;
            }

            //  kinematics information
            daeElement* pelt = searchBinding(kbindmodel,kscene);
            domInstance_kinematics_modelRef kimodel = daeSafeCast<domInstance_kinematics_model>(pelt);
            if (!kimodel) {
                if( !pelt ) {
                    COLLADALOG_WARN("bind_kinematics_model does not reference element");
                }
                else {
                    COLLADALOG_WARN(str(boost::format("bind_kinematics_model cannot find reference to %s:%s:n")%kbindmodel->getNode()%pelt->getElementName()));
                }
                continue;
            }
            bindings.listKinematicsVisualBindings.push_back(make_pair(node,kimodel));
        }
        // axis info
        for (size_t ijoint = 0; ijoint < kiscene->getBind_joint_axis_array().getCount(); ++ijoint) {
            domBind_joint_axisRef bindjoint = kiscene->getBind_joint_axis_array()[ijoint];
            daeElementRef pjtarget = daeSidRef(bindjoint->getTarget(), viscene->getUrl().getElement()).resolve().elt;
            if (!pjtarget) {
                COLLADALOG_WARN(str(boost::format("Target Node '%s' not found")%bindjoint->getTarget()));
                continue;
            }
            daeElement* pelt = searchBinding(bindjoint->getAxis(),kscene);
            domAxis_constraintRef pjointaxis = daeSafeCast<domAxis_constraint>(pelt);
            if (!pjointaxis) {
                COLLADALOG_WARN(str(boost::format("joint axis for target %s")%bindjoint->getTarget()));
                continue;
            }
            bindings.listAxisBindings.push_back(JointAxisBinding(pjtarget, pjointaxis, bindjoint->getValue(), NULL, NULL));
        }
    }

    static void _ExtractPhysicsBindings(domCOLLADA::domSceneRef allscene, KinematicsSceneBindings& bindings)
    {
	for(size_t iphysics = 0; iphysics < allscene->getInstance_physics_scene_array().getCount(); ++iphysics) {
	    domPhysics_sceneRef pscene = daeSafeCast<domPhysics_scene>(allscene->getInstance_physics_scene_array()[iphysics]->getUrl().getElement().cast());
	    for(size_t imodel = 0; imodel < pscene->getInstance_physics_model_array().getCount(); ++imodel) {
		domInstance_physics_modelRef ipmodel = pscene->getInstance_physics_model_array()[imodel];
		domPhysics_modelRef pmodel = daeSafeCast<domPhysics_model> (ipmodel->getUrl().getElement().cast());
                domNodeRef nodephysicsoffset = daeSafeCast<domNode>(ipmodel->getParent().getElement().cast());
                for(size_t ibody = 0; ibody < ipmodel->getInstance_rigid_body_array().getCount(); ++ibody) {
                    LinkBinding lb;
                    lb.irigidbody = ipmodel->getInstance_rigid_body_array()[ibody];
                    lb.node = daeSafeCast<domNode>(lb.irigidbody->getTarget().getElement().cast());
                    lb.rigidbody = daeSafeCast<domRigid_body>(daeSidRef(lb.irigidbody->getBody(),pmodel).resolve().elt);
                    lb.nodephysicsoffset = nodephysicsoffset;
                    if( !!lb.rigidbody && !!lb.node ) {
                        bindings.listLinkBindings.push_back(lb);
                    }
                }
                for(size_t iconst = 0; iconst < ipmodel->getInstance_rigid_constraint_array().getCount(); ++iconst) {
                    ConstraintBinding cb;
                    cb.irigidconstraint = ipmodel->getInstance_rigid_constraint_array()[iconst];
                    cb.rigidconstraint = daeSafeCast<domRigid_constraint>(daeSidRef(cb.irigidconstraint->getConstraint(),pmodel).resolve().elt);
		    cb.node = daeSafeCast<domNode>(cb.rigidconstraint->getAttachment()->getRigid_body().getElement());
                    if( !!cb.rigidconstraint ) {
                        bindings.listConstraintBindings.push_back(cb);
                    }
                }
	    }
	}
    }

    domTechniqueRef _ExtractOpenRAVEProfile(const domTechnique_Array& arr)
    {
        for(size_t i = 0; i < arr.getCount(); ++i) {
            if( strcmp(arr[i]->getProfile(), "OpenRAVE") == 0 ) {
                return arr[i];
            }
        }
        return domTechniqueRef();
    }

    daeElementRef _ExtractOpenRAVEProfile(const daeElementRef pelt)
    {
        daeTArray<daeElementRef> children;
        pelt->getChildren(children);
        for(size_t i = 0; i < children.getCount(); ++i) {
            if( children[i]->getElementName() == string("technique") && children[i]->hasAttribute("profile") && children[i]->getAttribute("profile") == string("OpenRAVE") ) {
                return children[i];
            }
        }
        return daeElementRef();
    }

    /// \brief returns an openrave interface type from the extra array
    boost::shared_ptr<std::string> _ExtractInterfaceType(const daeElementRef pelt) {
        daeTArray<daeElementRef> children;
        pelt->getChildren(children);
        for(size_t i = 0; i < children.getCount(); ++i) {
            if( children[i]->getElementName() == string("interface_type") ) {
                daeElementRef ptec = _ExtractOpenRAVEProfile(children[i]);
                if( !!ptec ) {
                    daeElement* ptype = ptec->getChild("interface");
                    if( !!ptype ) {
                        return boost::shared_ptr<std::string>(new std::string(ptype->getCharData()));
                    }
                }
            }
        }
        return boost::shared_ptr<std::string>();
    }

    /// \brief returns an openrave interface type from the extra array
    boost::shared_ptr<std::string> _ExtractInterfaceType(const domExtra_Array& arr) {
        for(size_t i = 0; i < arr.getCount(); ++i) {
            if( strcmp(arr[i]->getType(),"interface_type") == 0 ) {
                domTechniqueRef tec = _ExtractOpenRAVEProfile(arr[i]->getTechnique_array());
                if( !!tec ) {
                    daeElement* ptype = tec->getChild("interface");
                    if( !!ptype ) {
                        return boost::shared_ptr<std::string>(new std::string(ptype->getCharData()));
                    }
                }
            }
        }
        return boost::shared_ptr<std::string>();
    }

    std::string _ExtractLinkName(domLinkRef pdomlink) {
        std::string linkname;
        if( !!pdomlink ) {
            if( !!pdomlink->getName() ) {
                linkname = pdomlink->getName();
            }
            if( linkname.size() == 0 && !!pdomlink->getID() ) {
                linkname = pdomlink->getID();
            }
        }
        return _ConvertToValidName(linkname);
    }

    bool _checkMathML(daeElementRef pelt,const string& type)
    {
        if( pelt->getElementName()==type ) {
            return true;
        }
        // check the substring after ':', the substring before is the namespace set in some parent attribute
        string name = pelt->getElementName();
        size_t pos = name.find_last_of(':');
        if( pos == string::npos ) {
            return false;
        }
        return name.substr(pos+1)==type;
    }

    std::pair<boost::shared_ptr<LinkInfo> ,domJointRef> _getJointFromRef(xsToken targetref, daeElementRef peltref, BodyInfoCollada_impl* pkinbody) {
        daeElement* peltjoint = daeSidRef(targetref, peltref).resolve().elt;
        domJointRef pdomjoint = daeSafeCast<domJoint> (peltjoint);
        if (!pdomjoint) {
            domInstance_jointRef pdomijoint = daeSafeCast<domInstance_joint> (peltjoint);
            if (!!pdomijoint) {
                pdomjoint = daeSafeCast<domJoint> (pdomijoint->getUrl().getElement().cast());
            }
        }

        if (!pdomjoint) {
            COLLADALOG_WARN(str(boost::format("could not find collada joint '%s'!")%targetref));
            return std::make_pair(boost::shared_ptr<LinkInfo>(),domJointRef());
        }

        if( string(targetref).find("./") != 0 ) {
            std::map<std::string,boost::shared_ptr<LinkInfo> >::iterator itjoint = _mapJointIds.find(targetref);
            if( itjoint != _mapJointIds.end() ) {
                return std::make_pair(itjoint->second,pdomjoint);
            }
            COLLADALOG_WARN(str(boost::format("failed to find joint target '%s' in _mapJointIds")%targetref));
        }

        boost::shared_ptr<LinkInfo>  pjoint = GetLink(pdomjoint->getName());
        if(!pjoint) {
            COLLADALOG_WARN(str(boost::format("could not find openrave joint '%s'!")%pdomjoint->getName()));
        }
        return std::make_pair(pjoint,pdomjoint);
    }

    /// \brief get the element name without the namespace
    std::string _getElementName(daeElementRef pelt) {
        std::string name = pelt->getElementName();
        std::size_t pos = name.find_last_of(':');
        if( pos != string::npos ) {
            return name.substr(pos+1);
        }
        return name;
    }

    std::string _ExtractParentId(daeElementRef p) {
        while(!!p) {
            if( p->hasAttribute("id") ) {
                return p->getAttribute("id");
            }
            p = p->getParent();
        }
        return "";
    }

    /// \brief Extracts MathML into fparser equation format
    std::string _ExtractMathML(daeElementRef proot, BodyInfoCollada_impl* pkinbody, daeElementRef pelt)
    {
        std::string name = _getElementName(pelt);
        std::string eq;
        daeTArray<daeElementRef> children;
        pelt->getChildren(children);
        if( name == "math" ) {
            for(std::size_t ic = 0; ic < children.getCount(); ++ic) {
                std::string childname = _getElementName(children[ic]);
                if( childname == "apply" || childname == "csymbol" || childname == "cn" || childname == "ci" ) {
                    eq = _ExtractMathML(proot, pkinbody, children[ic]);
                }
                else {
                    throw ModelLoader::ModelLoaderException(str(boost::format("_ExtractMathML: do not support element %s in mathml")%childname).c_str());
                }
            }
        }
        else if( name == "apply" ) {
            if( children.getCount() == 0 ) {
                return eq;
            }
            string childname = _getElementName(children[0]);
            if( childname == "plus" ) {
                eq += '(';
                for(size_t ic = 1; ic < children.getCount(); ++ic) {
                    eq += _ExtractMathML(proot, pkinbody, children[ic]);
                    if( ic+1 < children.getCount() ) {
                        eq += '+';
                    }
                }
                eq += ')';
            }
            else if( childname == "quotient" ) {
                COLLADA_ASSERT(children.getCount()==3);
                eq += str(boost::format("floor(%s/%s)")%_ExtractMathML(proot,pkinbody,children[1])%_ExtractMathML(proot,pkinbody,children[2]));
            }
            else if( childname == "divide" ) {
                COLLADA_ASSERT(children.getCount()==3);
                eq += str(boost::format("(%s/%s)")%_ExtractMathML(proot,pkinbody,children[1])%_ExtractMathML(proot,pkinbody,children[2]));
            }
            else if( childname == "minus" ) {
                COLLADA_ASSERT(children.getCount()>1 && children.getCount()<=3);
                if( children.getCount() == 2 ) {
                    eq += str(boost::format("(-%s)")%_ExtractMathML(proot,pkinbody,children[1]));
                }
                else {
                    eq += str(boost::format("(%s-%s)")%_ExtractMathML(proot,pkinbody,children[1])%_ExtractMathML(proot,pkinbody,children[2]));
                }
            }
            else if( childname == "power" ) {
                COLLADA_ASSERT(children.getCount()==3);
                std::string sbase = _ExtractMathML(proot,pkinbody,children[1]);
                std::string sexp = _ExtractMathML(proot,pkinbody,children[2]);
//                try {
//                    int degree = boost::lexical_cast<int>(sexp);
//                    if( degree == 1 ) {
//                        eq += str(boost::format("(%s)")%sbase);
//                    }
//                    else if( degree == 2 ) {
//                        eq += str(boost::format("sqr(%s)")%sbase);
//                    }
//                    else {
//                        eq += str(boost::format("pow(%s,%s)")%sbase%sexp);
//                    }
//                }
//                catch(const boost::bad_lexical_cast&) {
                    eq += str(boost::format("pow(%s,%s)")%sbase%sexp);
                    //}
            }
            else if( childname == "rem" ) {
                COLLADA_ASSERT(children.getCount()==3);
                eq += str(boost::format("(%s%%%s)")%_ExtractMathML(proot,pkinbody,children[1])%_ExtractMathML(proot,pkinbody,children[2]));
            }
            else if( childname == "times" ) {
                eq += '(';
                for(size_t ic = 1; ic < children.getCount(); ++ic) {
                    eq += _ExtractMathML(proot, pkinbody, children[ic]);
                    if( ic+1 < children.getCount() ) {
                        eq += '*';
                    }
                }
                eq += ')';
            }
            else if( childname == "root" ) {
                COLLADA_ASSERT(children.getCount()==3);
                string sdegree, snum;
                for(size_t ic = 1; ic < children.getCount(); ++ic) {
                    if( _getElementName(children[ic]) == string("degree") ) {
                        sdegree = _ExtractMathML(proot,pkinbody,children[ic]->getChildren()[0]);
                    }
                    else {
                        snum = _ExtractMathML(proot,pkinbody,children[ic]);
                    }
                }
                try {
                    int degree = boost::lexical_cast<int>(sdegree);
                    if( degree == 1 ) {
                        eq += str(boost::format("(%s)")%snum);
                    }
                    else if( degree == 2 ) {
                        eq += str(boost::format("sqrt(%s)")%snum);
                    }
                    else if( degree == 3 ) {
                        eq += str(boost::format("cbrt(%s)")%snum);
                    }
                    else {
                        eq += str(boost::format("pow(%s,1.0/%s)")%snum%sdegree);
                    }
                }
                catch(const boost::bad_lexical_cast&) {
                    eq += str(boost::format("pow(%s,1.0/%s)")%snum%sdegree);
                }
            }
            else if( childname == "and" ) {
                eq += '(';
                for(size_t ic = 1; ic < children.getCount(); ++ic) {
                    eq += _ExtractMathML(proot, pkinbody, children[ic]);
                    if( ic+1 < children.getCount() ) {
                        eq += '&';
                    }
                }
                eq += ')';
            }
            else if( childname == "or" ) {
                eq += '(';
                for(size_t ic = 1; ic < children.getCount(); ++ic) {
                    eq += _ExtractMathML(proot, pkinbody, children[ic]);
                    if( ic+1 < children.getCount() ) {
                        eq += '|';
                    }
                }
                eq += ')';
            }
            else if( childname == "not" ) {
                COLLADA_ASSERT(children.getCount()==2);
                eq += str(boost::format("(!%s)")%_ExtractMathML(proot,pkinbody,children[1]));
            }
            else if( childname == "floor" ) {
                COLLADA_ASSERT(children.getCount()==2);
                eq += str(boost::format("floor(%s)")%_ExtractMathML(proot,pkinbody,children[1]));
            }
            else if( childname == "ceiling" ) {
                COLLADA_ASSERT(children.getCount()==2);
                eq += str(boost::format("ceil(%s)")%_ExtractMathML(proot,pkinbody,children[1]));
            }
            else if( childname == "eq" ) {
                COLLADA_ASSERT(children.getCount()==3);
                eq += str(boost::format("(%s=%s)")%_ExtractMathML(proot,pkinbody,children[1])%_ExtractMathML(proot,pkinbody,children[2]));
            }
            else if( childname == "neq" ) {
                COLLADA_ASSERT(children.getCount()==3);
                eq += str(boost::format("(%s!=%s)")%_ExtractMathML(proot,pkinbody,children[1])%_ExtractMathML(proot,pkinbody,children[2]));
            }
            else if( childname == "gt" ) {
                COLLADA_ASSERT(children.getCount()==3);
                eq += str(boost::format("(%s>%s)")%_ExtractMathML(proot,pkinbody,children[1])%_ExtractMathML(proot,pkinbody,children[2]));
            }
            else if( childname == "lt" ) {
                COLLADA_ASSERT(children.getCount()==3);
                eq += str(boost::format("(%s<%s)")%_ExtractMathML(proot,pkinbody,children[1])%_ExtractMathML(proot,pkinbody,children[2]));
            }
            else if( childname == "geq" ) {
                COLLADA_ASSERT(children.getCount()==3);
                eq += str(boost::format("(%s>=%s)")%_ExtractMathML(proot,pkinbody,children[1])%_ExtractMathML(proot,pkinbody,children[2]));
            }
            else if( childname == "leq" ) {
                COLLADA_ASSERT(children.getCount()==3);
                eq += str(boost::format("(%s<=%s)")%_ExtractMathML(proot,pkinbody,children[1])%_ExtractMathML(proot,pkinbody,children[2]));
            }
            else if( childname == "ln" ) {
                COLLADA_ASSERT(children.getCount()==2);
                eq += str(boost::format("log(%s)")%_ExtractMathML(proot,pkinbody,children[1]));
            }
            else if( childname == "log" ) {
                COLLADA_ASSERT(children.getCount()==2 || children.getCount()==3);
                string sbase="10", snum;
                for(size_t ic = 1; ic < children.getCount(); ++ic) {
                    if( _getElementName(children[ic]) == string("logbase") ) {
                        sbase = _ExtractMathML(proot,pkinbody,children[ic]->getChildren()[0]);
                    }
                    else {
                        snum = _ExtractMathML(proot,pkinbody,children[ic]);
                    }
                }
                try {
                    int base = boost::lexical_cast<int>(sbase);
                    if( base == 10 ) {
                        eq += str(boost::format("log10(%s)")%snum);
                    }
                    else if( base == 2 ) {
                        eq += str(boost::format("log2(%s)")%snum);
                    }
                    else {
                        eq += str(boost::format("(log(%s)/log(%s))")%snum%sbase);
                    }
                }
                catch(const boost::bad_lexical_cast&) {
                    eq += str(boost::format("(log(%s)/log(%s))")%snum%sbase);
                }
            }
            else if( childname == "arcsin" ) {
                COLLADA_ASSERT(children.getCount()==2);
                eq += str(boost::format("asin(%s)")%_ExtractMathML(proot,pkinbody,children[1]));
            }
            else if( childname == "arccos" ) {
                COLLADA_ASSERT(children.getCount()==2);
                eq += str(boost::format("acos(%s)")%_ExtractMathML(proot,pkinbody,children[1]));
            }
            else if( childname == "arctan" ) {
                COLLADA_ASSERT(children.getCount()==2);
                eq += str(boost::format("atan(%s)")%_ExtractMathML(proot,pkinbody,children[1]));
            }
            else if( childname == "arccosh" ) {
                COLLADA_ASSERT(children.getCount()==2);
                eq += str(boost::format("acosh(%s)")%_ExtractMathML(proot,pkinbody,children[1]));
            }
            else if( childname == "arccot" ) {
                COLLADA_ASSERT(children.getCount()==2);
                eq += str(boost::format("acot(%s)")%_ExtractMathML(proot,pkinbody,children[1]));
            }
            else if( childname == "arccoth" ) {
                COLLADA_ASSERT(children.getCount()==2);
                eq += str(boost::format("acoth(%s)")%_ExtractMathML(proot,pkinbody,children[1]));
            }
            else if( childname == "arccsc" ) {
                COLLADA_ASSERT(children.getCount()==2);
                eq += str(boost::format("acsc(%s)")%_ExtractMathML(proot,pkinbody,children[1]));
            }
            else if( childname == "arccsch" ) {
                COLLADA_ASSERT(children.getCount()==2);
                eq += str(boost::format("acsch(%s)")%_ExtractMathML(proot,pkinbody,children[1]));
            }
            else if( childname == "arcsec" ) {
                COLLADA_ASSERT(children.getCount()==2);
                eq += str(boost::format("asec(%s)")%_ExtractMathML(proot,pkinbody,children[1]));
            }
            else if( childname == "arcsech" ) {
                COLLADA_ASSERT(children.getCount()==2);
                eq += str(boost::format("asech(%s)")%_ExtractMathML(proot,pkinbody,children[1]));
            }
            else if( childname == "arcsinh" ) {
                COLLADA_ASSERT(children.getCount()==2);
                eq += str(boost::format("asinh(%s)")%_ExtractMathML(proot,pkinbody,children[1]));
            }
            else if( childname == "arctanh" ) {
                COLLADA_ASSERT(children.getCount()==2);
                eq += str(boost::format("atanh(%s)")%_ExtractMathML(proot,pkinbody,children[1]));
            }
            else if( childname == "implies" || childname == "forall" || childname == "exists" || childname == "conjugate" || childname == "arg" || childname == "real" || childname == "imaginary" || childname == "lcm" || childname == "factorial" || childname == "xor") {
                throw ModelLoader::ModelLoaderException(str(boost::format("_ExtractMathML: %s function in <apply> tag not supported")%childname).c_str());
            }
            else if( childname == "csymbol" ) {
                if( children[0]->getAttribute("encoding")==string("text/xml") ) {
                    domFormulaRef pformula;
                    string functionname;
                    if( children[0]->hasAttribute("definitionURL") ) {
                        // search for the formula in library_formulas
                        string formulaurl = children[0]->getAttribute("definitionURL");
                        if( formulaurl.size() > 0 ) {
                            daeElementRef pelt = _getElementFromUrl(daeURI(*children[0],formulaurl));
                            pformula = daeSafeCast<domFormula>(pelt);
                            if( !pformula ) {
                                COLLADALOG_WARN(str(boost::format("could not find csymbol %s formula")%children[0]->getAttribute("definitionURL")));
                            }
                            else {
                                COLLADALOG_DEBUG(str(boost::format("csymbol formula %s found")%pformula->getId()));
                            }
                        }
                    }
                    if( !pformula ) {
                        if( children[0]->hasAttribute("type") ) {
                            if( children[0]->getAttribute("type") == "function" ) {
                                functionname = children[0]->getCharData();
                            }
                        }
                    }
                    else {
                        if( !!pformula->getName() ) {
                            functionname = pformula->getName();
                        }
                        else {
                            functionname = children[0]->getCharData();
                        }
                    }

                    if( functionname == "INRANGE" ) {
                        COLLADA_ASSERT(children.getCount()==4);
                        string a = _ExtractMathML(proot,pkinbody,children[1]), b = _ExtractMathML(proot,pkinbody,children[2]), c = _ExtractMathML(proot,pkinbody,children[3]);
                        eq += str(boost::format("((%s>=%s)&(%s<=%s))")%a%b%a%c);
                    }
                    else if( functionname == "SSSA" || functionname == "SASA" || functionname == "SASS" ) {
                        COLLADA_ASSERT(children.getCount()==4);
                        eq += str(boost::format("%s(%s,%s,%s)")%functionname%_ExtractMathML(proot,pkinbody,children[1])%_ExtractMathML(proot,pkinbody,children[2])%_ExtractMathML(proot,pkinbody,children[3]));
                    }
                    else if( functionname == "atan2") {
                        COLLADA_ASSERT(children.getCount()==3);
                        eq += str(boost::format("atan2(%s,%s)")%_ExtractMathML(proot,pkinbody,children[1])%_ExtractMathML(proot,pkinbody,children[2]));
                    }
                    else {
                        COLLADALOG_WARN(str(boost::format("csymbol %s not implemented")%functionname));
                        eq += "1";
                    }
                }
                else if( children[0]->getAttribute("encoding")!=string("COLLADA") ) {
                    COLLADALOG_WARN(str(boost::format("_ExtractMathML: csymbol '%s' has unknown encoding '%s'")%children[0]->getCharData()%children[0]->getAttribute("encoding")));
                }
                else {
                    eq += _ExtractMathML(proot,pkinbody,children[0]);
                }
            }
            else {
                // make a function call
                eq += childname;
                eq += '(';
                for(size_t ic = 1; ic < children.getCount(); ++ic) {
                    eq += _ExtractMathML(proot, pkinbody, children[ic]);
                    if( ic+1 < children.getCount() ) {
                        eq += ',';
                    }
                }
                eq += ')';
            }
        }
        else if( name == "csymbol" ) {
            if( !pelt->hasAttribute("encoding") ) {
                COLLADALOG_WARN(str(boost::format("_ExtractMathML: csymbol '%s' does not have any encoding")%pelt->getCharData()));
            }
            else if( pelt->getAttribute("encoding")!=string("COLLADA") ) {
                COLLADALOG_WARN(str(boost::format("_ExtractMathML: csymbol '%s' has unknown encoding '%s'")%pelt->getCharData()%pelt->getAttribute("encoding")));
            }
            boost::shared_ptr<LinkInfo> pjoint = _getJointFromRef(pelt->getCharData().c_str(),proot,pkinbody).first;
            if( !pjoint ) {
                COLLADALOG_WARN(str(boost::format("_ExtractMathML: failed to find csymbol: %s")%pelt->getCharData()));
                eq = pelt->getCharData();
            }
            string jointname(CORBA::String_var(pjoint->name));
            //if( pjoint->GetDOF() > 1 ) {
            //COLLADALOG_WARN(str(boost::format("formulas do not support using joints with > 1 DOF yet (%s)")%pjoint->GetName()));
            if( _mapJointUnits.find(pjoint) != _mapJointUnits.end() && _mapJointUnits[pjoint].at(0) != 1 ) {
                eq = str(boost::format("(%f*%s)")%(1/_mapJointUnits[pjoint].at(0))%jointname);
            }
            else {
                eq = jointname;
            }
        }
        else if( name == "cn" ) {
            eq = pelt->getCharData();
        }
        else if( name == "ci" ) {
            eq = pelt->getCharData();
        }
        else if( name == "pi" ) {
            eq = "3.14159265358979323846";
        }
        else {
            COLLADALOG_WARN(str(boost::format("mathml unprocessed tag: %s")));
        }
        return eq;
    }

    static inline bool _IsValidCharInName(char c) { return isalnum(c) || c == '_' || c == '.' || c == '-' || c == '/'; }
    static inline bool _IsValidName(const std::string& s) {
        if( s.size() == 0 ) {
            return false;
        }
        return std::count_if(s.begin(), s.end(), _IsValidCharInName) == (int)s.size();
    }

    inline std::string _ConvertToValidName(const std::string& name) {
        if( name.size() == 0 ) {
            return str(boost::format("__dummy%d")%_nGlobalIndex++);
        }
        if( _IsValidName(name) ) {
            return name;
        }
        std::string newname = name;
        for(size_t i = 0; i < newname.size(); ++i) {
            if( !_IsValidCharInName(newname[i]) ) {
                newname[i] = '_';
            }
        }
        COLLADALOG_WARN(str(boost::format("name '%s' is not a valid name, converting to '%s'")%name%newname));
        return newname;
    }

    inline static dReal _GetUnitScale(daeElementRef pelt, dReal startscale)
    {
        // getChild could be optimized since asset tag is supposed to appear as the first element
        domExtraRef pextra = daeSafeCast<domExtra> (pelt->getChild("extra"));
        if( !!pextra && !!pextra->getAsset() && !!pextra->getAsset()->getUnit() ) {
            return pextra->getAsset()->getUnit()->getMeter();
        }
        if( !!pelt->getParent() ) {
            return _GetUnitScale(pelt->getParent(),startscale);
        }
        return startscale;
    }
        
    boost::shared_ptr<DAE> _dae;
    bool _bOpeningZAE;
    domCOLLADA* _dom;
    dReal _fGlobalScale;
    std::map<boost::shared_ptr<LinkInfo> , std::vector<dReal> > _mapJointUnits;
    std::map<std::string,boost::shared_ptr<LinkInfo> > _mapJointIds;
    std::map<std::string,boost::shared_ptr<LinkInfo> > _mapLinkNames;
    std::vector<boost::shared_ptr<LinkInfo> > _veclinks;
    int _nGlobalSensorId, _nGlobalActuatorId, _nGlobalManipulatorId, _nGlobalIndex;
    std::string _filename;
};

BodyInfoCollada_impl::BodyInfoCollada_impl(PortableServer::POA_ptr poa) :
    ShapeSetInfo_impl(poa)
{
    lastUpdate_ = 0;
}


BodyInfoCollada_impl::~BodyInfoCollada_impl()
{   
}

const std::string& BodyInfoCollada_impl::topUrl()
{
    return url_;
}


char* BodyInfoCollada_impl::name()
{
    return CORBA::string_dup(name_.c_str());
}


char* BodyInfoCollada_impl::url()
{
    return CORBA::string_dup(url_.c_str());
}


StringSequence* BodyInfoCollada_impl::info()
{
    return new StringSequence(info_);
}


LinkInfoSequence* BodyInfoCollada_impl::links()
{
    return new LinkInfoSequence(links_);
}


AllLinkShapeIndexSequence* BodyInfoCollada_impl::linkShapeIndices()
{
    return new AllLinkShapeIndexSequence(linkShapeIndices_);
}

void BodyInfoCollada_impl::loadModelFile(const std::string& url)
{
    ColladaReader reader;
    if( !reader.InitFromURL(url) ) {
        throw ModelLoader::ModelLoaderException("The model file cannot be found.");
    }
    if( !reader.Extract(this) ) {
        throw ModelLoader::ModelLoaderException("The model file cannot be loaded.");
    }
}

void BodyInfoCollada_impl::setParam(std::string param, bool value)
{
    if(param == "readImage") {
        readImage_ = value;
    }
}

bool BodyInfoCollada_impl::getParam(std::string param)
{
    if(param == "readImage") {
        return readImage_;
    }
    return false;
}

void BodyInfoCollada_impl::setParam(std::string param, int value)
{
    if(param == "AABBType") {
        AABBdataType_ = (OpenHRP::ModelLoader::AABBdataType)value;
    }
}

void BodyInfoCollada_impl::setColdetModel(ColdetModelPtr& coldetModel, TransformedShapeIndexSequence shapeIndices, const Matrix44& Tparent, int& vertexIndex, int& triangleIndex){
    for(unsigned int i=0; i < shapeIndices.length(); i++){
        setColdetModelTriangles(coldetModel, shapeIndices[i], Tparent, vertexIndex, triangleIndex);
    }
}

void BodyInfoCollada_impl::setColdetModelTriangles
(ColdetModelPtr& coldetModel, const TransformedShapeIndex& tsi, const Matrix44& Tparent, int& vertexIndex, int& triangleIndex)
{
    short shapeIndex = tsi.shapeIndex;
    const DblArray12& M = tsi.transformMatrix;;
    Matrix44 T, Tlocal;
    Tlocal << M[0], M[1], M[2],  M[3],
             M[4], M[5], M[6],  M[7],
             M[8], M[9], M[10], M[11],
             0.0,  0.0,  0.0,   1.0;
    T = Tparent * Tlocal;
    
    const ShapeInfo& shapeInfo = shapes_[shapeIndex];
    
    const FloatSequence& vertices = shapeInfo.vertices;
    const LongSequence& triangles = shapeInfo.triangles;
    const int numTriangles = triangles.length() / 3;
    
    coldetModel->setNumTriangles(coldetModel->getNumTriangles()+numTriangles);
    int numVertices = numTriangles * 3;
    coldetModel->setNumVertices(coldetModel->getNumVertices()+numVertices);
    for(int j=0; j < numTriangles; ++j){
        int vertexIndexTop = vertexIndex;
        for(int k=0; k < 3; ++k){
            long orgVertexIndex = shapeInfo.triangles[j * 3 + k];
            int p = orgVertexIndex * 3;
            Vector4 v(T * Vector4(vertices[p+0], vertices[p+1], vertices[p+2], 1.0));
            coldetModel->setVertex(vertexIndex++, (float)v[0], (float)v[1], (float)v[2]);
        }
        coldetModel->setTriangle(triangleIndex++, vertexIndexTop, vertexIndexTop + 1, vertexIndexTop + 2);
    }
    
}

void BodyInfoCollada_impl::changetoBoundingBox(unsigned int* inputData){
    const double EPS = 1.0e-6;
    createAppearanceInfo();
    std::vector<Vector3> boxSizeMap;
    std::vector<Vector3> boundingBoxData;
    
    for(int i=0; i<links_.length(); i++){
        int _depth;
        if( AABBdataType_ == OpenHRP::ModelLoader::AABB_NUM )
            _depth = linkColdetModels[i]->numofBBtoDepth(inputData[i]);
        else
            _depth = inputData[i];
        if( _depth >= links_[i].AABBmaxDepth)
            _depth = links_[i].AABBmaxDepth-1;
        if(_depth >= 0 ){
            linkColdetModels[i]->getBoundingBoxData(_depth, boundingBoxData);
            std::vector<TransformedShapeIndex> tsiMap;
            links_[i].shapeIndices.length(0);
            SensorInfoSequence& sensors = links_[i].sensors;
            for (unsigned int j=0; j<sensors.length(); j++){
                SensorInfo& sensor = sensors[j];
                sensor.shapeIndices.length(0);
            }

            for(int j=0; j<boundingBoxData.size()/2; j++){

                bool flg=false;
                int k=0;
                for( ; k<boxSizeMap.size(); k++)
                    if((boxSizeMap[k] - boundingBoxData[j*2+1]).norm() < EPS)
                        break;
                if( k<boxSizeMap.size() )
                    flg=true;
                else{
                    boxSizeMap.push_back(boundingBoxData[j*2+1]);
                    setBoundingBoxData(boundingBoxData[j*2+1],k);
                }

                if(flg){
                    int l=0;
                    for( ; l<tsiMap.size(); l++){
                        Vector3 p(tsiMap[l].transformMatrix[3],tsiMap[l].transformMatrix[7],tsiMap[l].transformMatrix[11]);
                        if((p - boundingBoxData[j*2]).norm() < EPS && tsiMap[l].shapeIndex == k)
                            break;
                    }
                    if( l==tsiMap.size() )
                        flg=false;
                }

                if(!flg){
                    int num = links_[i].shapeIndices.length();
                    links_[i].shapeIndices.length(num+1);
                    TransformedShapeIndex& tsi = links_[i].shapeIndices[num];
                    tsi.inlinedShapeTransformMatrixIndex = -1;
                    tsi.shapeIndex = k;
                    Matrix44 T(Matrix44::Identity());
                    for(int p = 0,row=0; row < 3; ++row)
                       for(int col=0; col < 4; ++col)
                            if(col==3){
                                switch(row){
                                    case 0:
                                        tsi.transformMatrix[p++] = boundingBoxData[j*2][0];
                                        break;
                                     case 1:
                                        tsi.transformMatrix[p++] = boundingBoxData[j*2][1];
                                        break;
                                     case 2:
                                        tsi.transformMatrix[p++] = boundingBoxData[j*2][2];
                                        break;
                                     default:
                                        ;
                                }
                            }else
                                tsi.transformMatrix[p++] = T(row, col);

                    tsiMap.push_back(tsi);
                }
            }
        }   
        linkShapeIndices_[i] = links_[i].shapeIndices;
    }
}

bool BodyInfoCollada_impl::checkInlineFileUpdateTime()
{
    bool ret=true;
    for( std::map<std::string, time_t>::iterator it=fileTimeMap.begin(); it != fileTimeMap.end(); ++it){
        struct stat statbuff;
        time_t mtime = 0;
        if( stat( it->first.c_str(), &statbuff ) == 0 ){
            mtime = statbuff.st_mtime;
        }  
        if(it->second!=mtime){
            ret=false;
            break;
        }
    }
    return ret;
}
