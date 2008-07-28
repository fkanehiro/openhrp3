/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */

/*! @file
  @author Shin'ichiro Nakaoka
*/

#ifndef OPENHRP_PARSER_MODEL_NODE_SET_H_INCLUDED
#define OPENHRP_PARSER_MODEL_NODE_SET_H_INCLUDED

#include "ModelParserConfig.h"

#include <vector>
#include <bitset>
#include <boost/shared_ptr.hpp>
#include <boost/signals.hpp>

#include "VrmlNodes.h"


namespace OpenHRP {

    class VRMLParser;

    class JointNodeSet;
    typedef boost::shared_ptr<JointNodeSet> JointNodeSetPtr;

    class JointNodeSet
    {
    public:
        VrmlProtoInstancePtr jointNode;
        VrmlProtoInstancePtr segmentNode;
        std::vector<JointNodeSetPtr> childJointNodeSets;
        std::vector<VrmlProtoInstancePtr> sensorNodes;
    };
    
    typedef std::vector<JointNodeSetPtr> JointNodeSetArray;
    

    class MODELPARSER_EXPORT ModelNodeSet
    {
      public:

        ModelNodeSet();

        bool loadModelFile(const std::string& filename);
		
        int numJointNodes() { return numJointNodes_; }
        VrmlProtoInstancePtr humanoidNode() { return humanoidNode_; }
        JointNodeSetPtr rootJointNodeSet() { return rootJointNodeSet_; }

        /**
           @if jp
           読み込み進行状況のメッセージを出力するためのシグナル.
           @note エラー発生時のメッセージはこのシグナルではなく例外によって処理される。
           @endif
        */
        boost::signal<void(const std::string& message)> signalOnStatusMessage;

        bool setMessageOutput( bool val ) { return( flgMessageOutput_ = val ); }

        struct Exception {
            Exception(const std::string& message) : message(message) { }
            std::string message;
        };

      private:
        
        int numJointNodes_;
        VrmlProtoInstancePtr humanoidNode_;
        JointNodeSetPtr rootJointNodeSet_;
        int messageIndent_;
        bool flgMessageOutput_;

        VrmlProtoPtr protoToCheck;

        enum {
            PROTO_UNDEFINED = 0,
            PROTO_HUMANOID,
            PROTO_JOINT,
            PROTO_SEGMENT,
            PROTO_SENSOR,
            PROTO_HARDWARECOMPONENT,
            NUM_PROTOS
        };

        typedef std::bitset<NUM_PROTOS> ProtoIdSet;
    
        void extractHumanoidNode(VRMLParser& parser);

        void throwExceptionOfIllegalField(const std::string& name, VrmlFieldTypeId typeId);
        void requireField(const std::string& name, VrmlFieldTypeId type);
        void checkFieldType(const std::string& name, VrmlFieldTypeId type);
        VrmlVariantField* addField(const std::string& name, VrmlFieldTypeId type);
        void addFloatField(const std::string& name, double defaultValue);
		
        void checkHumanoidProto();
        void checkJointProto();
        void checkSegmentProto();
        void checkSensorProtoCommon();
        void checkHardwareComponentProto();
        void extractJointNodes();
        JointNodeSetPtr addJointNodeSet(VrmlProtoInstancePtr jointNode);
        void extractChildNodes
            (JointNodeSetPtr jointNodeSet, MFNode& childNodes, const ProtoIdSet acceptableProtoIds);

        void putMessage(const std::string& message);
		
    };

    typedef boost::shared_ptr<ModelNodeSet> ModelNodeSetPtr;
};
    

#endif
