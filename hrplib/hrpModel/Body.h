/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */
/** \file
    \author Shin'ichiro Nakaoka
*/

#ifndef HRPMODEL_BODY_H_INCLUDED
#define HRPMODEL_BODY_H_INCLUDED

#include <map>
#include <vector>
#include <ostream>
#include <boost/shared_ptr.hpp>
#include <hrpUtil/Referenced.h>
#include <hrpUtil/Tvmet3d.h>
#include <hrpUtil/uBlasCommonTypes.h>
#include "LinkTraverse.h"
#include "Config.h"

namespace hrp {
    class Sensor;
    class Body;
    class JointPath;
    typedef boost::shared_ptr<JointPath> JointPathPtr;
}

namespace hrp {

    struct BodyHandleEntity {
        Body* body;
    };

    struct BodyInterface;
    struct BodyCustomizerInterface;
    typedef void* BodyHandle;
    typedef void* BodyCustomizerHandle;

    class HRPMODEL_API Body : public Referenced
    {

      public:

        static BodyInterface* bodyInterface();

        Body();
        Body(const Body& org);

        virtual ~Body();

        const std::string& name() {
            return name_;
        }

        void setName(const std::string& name) {
            name_ = name;
        }

        const std::string& modelName() {
            return modelName_;
        }
        
        void setModelName(const std::string& name) {
            modelName_ = name;
        }

        void setRootLink(Link* link);

        /**
           This function must be called when the structure of the link tree is changed.
        */
        void updateLinkTree();

        /**
           The number of the links that work as a joint.
           Note that the acutal value is the maximum joint ID plus one.
           Thus there may be a case where the value does not correspond
           to the actual number of the joint-links.
           In other words, the value represents the size of the link sequence
           obtained by joint() function.
        */
        inline int numJoints() const {
            return jointIdToLinkArray.size();
        }

        /**
           This function returns a link that has a given joint ID.
           If there is no link that has a given joint ID,
           the function returns a dummy link object whose ID is minus one.
           The maximum id can be obtained by numJoints().
        */
        inline Link* joint(int id) const {
            return jointIdToLinkArray[id];
        }

        inline const std::vector<Link*>& joints() const {
            return jointIdToLinkArray;
        }

        /**
           The number of all the links the body has.
           The value corresponds to the size of the sequence obtained by link() function.
        */
        inline int numLinks() const {
            return linkTraverse_.numLinks();
        }

        /**
           This function returns the link of a given index in the whole link sequence.
           The order of the sequence corresponds to a link-tree traverse from the root link.
           The size of the sequence can be obtained by numLinks().
        */
        inline Link* link(int index) const {
            return linkTraverse_.link(index);
        }

        inline const LinkTraverse& links() const {
            return linkTraverse_;
        }

        /**
           LinkTraverse object that traverses all the links from the root link
        */
        inline const LinkTraverse& linkTraverse() const {
            return linkTraverse_;
        }

        /**
           This function returns a link that has a given name.
        */
        Link* link(const std::string& name) const;

        /**
           The root link of the body
        */
        inline Link* rootLink() const {
            return rootLink_;
        }

        // sensor access methods
        Sensor* createSensor(Link* link, int sensorType, int id, const std::string& name);

        inline Sensor* sensor(int sensorType, int sensorId) const {
            return allSensors[sensorType][sensorId];
        }

        inline int numSensors(int sensorType) const {
            return allSensors[sensorType].size();
        }

        inline int numSensorTypes() const {
            return allSensors.size();
        }

        void clearSensorValues();

        template <class TSensor> inline TSensor* sensor(int id) const {
            return static_cast<TSensor*>(allSensors[TSensor::TYPE][id]);
        }

        template <class TSensor> inline TSensor* sensor(const std::string& name) const {
            TSensor* sensor = 0;
            NameToSensorMap::const_iterator p = nameToSensorMap.find(name);
            if(p != nameToSensorMap.end()){
                sensor = dynamic_cast<TSensor*>(p->second);
            }
            return sensor;
        }

        /**
           This function returns true when the whole body is a static, fixed object like a floor.
        */
        inline bool isStaticModel() {
            return isStaticModel_;
        }

        double calcTotalMass();

        inline double totalMass() {
            return totalMass_;
        }

        Vector3 calcCM();

        /*
          The motion equation for calcMassMatrix()
          |       |   | dv   |   |    |   | fext      |
          | out_M | * | dw   | + | b1 | = | tauext    |
          |       |   |ddq   |   |    |   | u         |
        */
        void calcMassMatrix(dmatrix& out_M);

        void setColumnOfMassMatrix(dmatrix& M, int column);

        void calcInverseDynamics(Link* link, Vector3& out_f, Vector3& out_tau);

        void calcTotalMomentum(Vector3& out_P, Vector3& out_L);

        void setDefaultRootPosition(const Vector3& p, const Matrix33& R);

        void getDefaultRootPosition(Vector3& out_p, Matrix33& out_R);

        void initializeConfiguration();

        void calcForwardKinematics(bool calcVelocity = false, bool calcAcceleration = false);

        void clearExternalForces();

        JointPathPtr getJointPath(Link* baseLink, Link* targetLink);

        inline void setVirtualJointForces(){
            if(customizerInterface){
                setVirtualJointForcesSub();
            }
        }

        /**
           This function must be called before the collision detection.
           It updates the positions and orientations of the models
           for detecting collisions between links.
        */
        void updateLinkColdetModelPositions();

        void putInformation(std::ostream &out);

        bool installCustomizer();
        bool installCustomizer(BodyCustomizerInterface* customizerInterface);

        struct LinkConnection {
            Link* link[2];
            Vector3 point[2];
            int numConstraintAxes;
            Vector3 constraintAxes[3];
        };
        typedef std::vector<LinkConnection> LinkConnectionArray;

        LinkConnectionArray linkConnections;

      private:

        bool isStaticModel_;
        Link* rootLink_;

        typedef std::vector<Link*> LinkArray;

        LinkArray jointIdToLinkArray;

        LinkTraverse linkTraverse_;

        std::string name_;
        std::string modelName_;

        typedef std::map<std::string, Link*> NameToLinkMap;
        NameToLinkMap nameToLinkMap;

        // sensor = sensors[type][sensorId]
        typedef std::vector<Sensor*> SensorArray;
        std::vector<SensorArray> allSensors;

        typedef std::map<std::string, Sensor*> NameToSensorMap;
        NameToSensorMap nameToSensorMap;

        double totalMass_;

        Vector3 defaultRootPosition;
        Matrix33 defaultRootAttitude;

        // Members for customizer
        BodyCustomizerHandle customizerHandle;
        BodyCustomizerInterface* customizerInterface;
        BodyHandleEntity bodyHandleEntity;
        BodyHandle bodyHandle;

        void initialize();
        Link* createEmptyJoint(int jointId);
        void setVirtualJointForcesSub();

        friend class CustomizedJointPath;
    };

    typedef boost::intrusive_ptr<Body> BodyPtr;

};


HRPMODEL_API std::ostream &operator<< (std::ostream& out, hrp::Body& body);


#endif
